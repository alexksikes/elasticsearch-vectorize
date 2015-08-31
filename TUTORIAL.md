# Machine Learning Fun with Elasticsearch - Part 1

The first part of this tutorial covers how to use the [Vectorize plugin][vectorize] in order to obtain a dataset from data stored in Elasticsearch. That dataset can then be used by external data analytics tools such as [Panda][panda] or [R][r] or by machine learning packages such as [scikit-learn][scikit] or [MLlib][mllib]. The second part of this tutorial is focused on storing the trained model back in Elasticsearch, and then on using the Vectorize plugin again to evaluate the model in scripts or in aggregations.

The Vectorize plugin is a mechanism to extract data as a [document-term matrix][docterm] in a consistent manner. What we mean by consistent is that if new features are added later on, then our specification will ensure that the same features will occupy the same column in the matrix. In order to show how the plugin works and why it is useful, we will train a classifier to distinguish between negative and positive sentiments. To this effect we will be using the [sentiment140][sentiment140] dataset.

This tutorial is merely a more detailed explanation of the heavily commented Python [example][tutorial] that ships with the plugin. We can already take a quick look at the main steps involved:

```python
# 1) let's get a good set of features using significant terms
features = get_features(3000)

# 2) now let's create a vectorizer with these features
vectorizer = get_vectorizer_body(features)

# 3) generate a dataset with this vectorizer
dataset = generate_dataset(vectorizer, batch_size=100000, cutoff=-1)

# 4.a) generate a training and a test set
train_data, test_data, train_target, test_target = get_train_test_split(dataset, test_size=0.33)

# 4.b) use scikit-learn to train a model on the train set
model = train_model(train_data, train_target)

# 5.a) evaluate the model on the test set
y_pred = evaluate(model, test_data)

# 5.b) finally report the accuracy
print 'accuracy: %s' % metrics.accuracy_score(test_target, y_pred)
```

So to summarize first we generate a good set of features. Second, we create a vectorizer to obtain a document-term matrix from the indexed data. Third, using the vectorizer a dataset is generated. Fourth, a model is trained on this dataset. And fifth, the model is evaluated and we report on its accuracy. Each of these steps will be explained in greater details, but first let's get some necessary prerequisites. So let's get started!

## Prerequisites

First, make sure you have Python with [SciPy][scipy] and [scikit-learn][scikit] installed. I will pass on the setup instructions, but basically it boils down to installing [Python][python] if not already done so and `pip install scipy scikit-learn`.

Second, you will also need to install the [Vectorize plugin][vectorize]. The Vectorize plugin is still very early on in its development. At the moment it is only supported by the latest [Elasticsearch 2.0.0-beta1][elasticsearch]. Also you will need to build it from source. Please follow the Maven instructions which are included with the plugin.

## Dataset Preparation

Next we need to index the [sentiment140][sentiment140] dataset. The dataset comes in a CSV format, but thankfully we have an already prepared JSON copy of this dataset.

Please feel free to download it:

> wget http://data.elasticsearch.org/sentiment140/sentiment140.bulk.tar.gz

De-compress it:

> tar xvf sentiment140.bulk.tar.gz

Then create the index with the given mapping:

```javascript
curl -XPUT localhost:9200/sentiment140 -d '
{
  "mappings": {
    "tweets": {
      "properties": {
        "polarity": {
          "type": "short"
        },
        "tweet_id": {
          "type": "string",
          "index": "not_analyzed"
        },
        "date": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss zzz yyyy"
        },
        "query": {
          "type": "string"
        },
        "user": {
          "type": "string",
          "index": "not_analyzed"
        },
        "text": {
          "type": "string",
          "analyzer": "my_analyzer",
          "term_vector": "with_positions_offsets_payloads"
        }
      }
    }
  }
}'
```

And finally index each bulk:

> curl -s -XPOST http://localhost:9200/sentiment140/_bulk\?pretty=true --data-binary @sentiment140.bulk.{bulk_number} >> sentiment140.indexed.log

Now you should be all set!

## Generating a Good Set of Features

Before training our model it is crucial to come up with a good set of features. We could be using all the tokenized keywords in the `text` field. However, it is desirable to select a smaller but more interesting set of features out of the gate. This will help reduce computation time and also improve the model’s performance. Since we want to discriminate between negative and positive tweets, we can use a significant terms aggregation against each of the classes.

Such an aggregation on the `1` class (positive tweets) looks like this:

```javascript
curl -XGET 'http://localhost:9200/sentiment140/tweets/_search?search_type=count&pretty' -d '
{
  "query": {
    "term": {
      "polarity": {
        "value": 1
      }
    }
  },
  "aggs": {
    "top_keywords": {
      "significant_terms": {
        "field": "text",
        "size": 10
      }
    }
  }
}'
```

And the response is full of happy keywords:

```javascript
{
  ...
  "aggregations" : {
    "top_keywords" : {
      "doc_count" : 800000,
      "buckets" : [ {
        "key" : "you",
        "doc_count" : 147254,
        "score" : 0.05561462321085083,
        "bg_count" : 226172
      }, {
        "key" : "thanks",
        "doc_count" : 34066,
        "score" : 0.03029631391819203,
        "bg_count" : 39809
      }, {
        "key" : "love",
        "doc_count" : 45060,
        "score" : 0.02648242218479298,
        "bg_count" : 61299
      }, {
        "key" : "good",
        "doc_count" : 59137,
        "score" : 0.026119569585454378,
        "bg_count" : 87394
      }, {
        "key" : "your",
        "doc_count" : 41396,
        "score" : 0.018360462657628574,
        "bg_count" : 61109
      }, {
        "key" : "great",
        "doc_count" : 24374,
        "score" : 0.015445221205247653,
        "bg_count" : 32349
      }, {
        "key" : "thank",
        "doc_count" : 15378,
        "score" : 0.014315483861158737,
        "bg_count" : 17628
      }, {
        "key" : "happy",
        "doc_count" : 19707,
        "score" : 0.012650349278892475,
        "bg_count" : 26041
      }, {
        "key" : "awesome",
        "doc_count" : 14199,
        "score" : 0.01031993171287668,
        "bg_count" : 17957
      }, {
        "key" : "haha",
        "doc_count" : 20813,
        "score" : 0.009722417393085084,
        "bg_count" : 30302
      } ]
    }
  }
}
```

Unsurprisingly, all these keywords relate to positive feelings, and therefore should be good features in helping the classifier choose between positive or negative tweets. This is essentially what is done in the first step of the Python code, only that 3000 features of each class are requested, and then the union of these keywords is the resulting feature set.

```python
# 1) let's get a good set of features using significant terms
def get_features(size):
    # use significant terms on the negative and positive class ...'
    features = []
    for i in (0, 1):
        resp = client.search(_index, _type, body=get_features_body(i, size), params={'search_type': 'count'})
        features.extend(r['key'] for r in resp['aggregations']['top_keywords']['buckets'])

    # return the union of both sets as features
    return features

def get_features_body(_class, size):
    return {
        "query": {
            "term": {
                "polarity": {
                    "value": _class
                }
            }
        },
        "aggs": {
            "top_keywords": {
                "significant_terms": {
                    "field": "text",
                    "size": size
                }
            }
        }
    }
features = get_features(3000)
```

Later on, we will see that we could have selected fewer features with minimal hit on the accuracy of the model. However, for the machine learning purists out there, reducing the number of features at this point would already be cheating.

Let's now move on to specifying how a [document-term matrix][docterm] should be generated from the index. As we have already mentioned, this matrix will then be used as the input of our classifier. This is when the [Vectorize plugin][vectorize] joins the party.

## Creating a Vectorizer

The next step consists of extracting a matrix from the sentiment140 index. We have already extracted a good set of features from the `text` field. The label on which to train is to be found in the `polarity` field. We can create a vectorizer for the top 10 positive keywords like so:

```javascript
{
  "vectorizer": [
    {
      "field": "text",
      "span": ["you", "thanks", "love", "good", "your", "great", "thank", "happy", "awesome", "haha"],
      "value": "binary"
    },
    {
      "field": "polarity",
      "span": 1
    }
  ]
}
```

With this vectorizer, 10 columns are reserved for the features found in the `text` field. These features are listed in order by an array of keywords following the `span` parameter. If the document has one or more of the given keywords, the `value` parameter specifies what to do with it.

There are a couple of possible options here, such as extracting the term frequencies or the document frequencies. However, since the tweets are rather short sized pieces of text, we ask for `binary` features. This means that a `1` is returned at this column if the document has the given feature, or `0` otherwise.

Finally, the last column is occupied by the polarity of the tweet. Here the `span` parameter takes an integer, say `n`, which indicates to use the first `n` values found in the given field as is. Here, we specify to use the first (and only) value found in the `polarity` field. These values will serve as labels for supervised machine learning.

We can now try this vectorizer on a given tweet. The Vectorize plugin registers two endpoints. The first one, `_vectorize`, is used to generated the vector of a single document. While the second one, `_search_vectorize`, as we will see later, is used on a set of documents prescribed by a query.

On this given tweet:

```javascript
{
  "_index": "sentiment140",
  "_type": "tweets",
  "_id": "2012313853",
  "_score": 1.0522405,
  "_source": {
    "date": "Tue Jun 02 20:26:01 PDT 2009",
    "polarity": 1,
    "query": "NO_QUERY",
    "text": "i love a great conversation...thank you. your a babe. ",
    "tweet_id": "2012313853",
    "user": "alex1234"
  }
}
```

Using `_vectorize` with the vectorizer previously created:

```javascript
GET sentiment140/tweets/2012313853/_vectorize
{
  "vectorizer": [
    {
      "field": "text",
      "span": ["you", "thanks", "love", "good", "your", "great", "thank", "happy", "awesome", "haha"],
      "value": "binary"
    },
    {
      "field": "polarity",
      "span": 1
    }
  ]
}
```

Gives the following response:

```javascript
{
  "_index": "sentiment140",
  "_type": "tweets",
  "_id": "2012313853",
  "_version": 0,
  "found": true,
  "took": 45,
  "shape": [1, 11],
  "matrix": [{"0": 1, "2": 1, "4": 1, "5": 1, "6": 1, "10": 1}]
}
```

The response is composed of a `shape` together with `matrix`. The shape describes the size of the matrix obtained. Here, we have a vector of size `11`. The `matrix` is the actual vector returned in sparse format, meaning that zeros are omitted from the matrix. When working with text features, the vectors are usually very sparse, and therefore returning such a sparse response is quite desirable. Looking at the vector returned, we see that this tweet has the keywords "you" (column 0), "love" (column 2), "good" (column 4), "your" (column 5), "great" (column 6) and a polarity of "1" (column 10).

Now we can come back to the second step of the Python tutorial file. What we do is essentially to create a vectorizer on all the features previously returned. That is on the roughly 6000 thousands negative and positive keywords.

```python
# 2) now let's create a vectorizer with these features
def get_vectorizer_body(features):
    return {
        "vectorizer": [
            {
                "field": "text",
                "span": features,
                "value": "binary"
            },
            {
                "field": "polarity",
                "span": 1
            }
        ]
    }
vectorizer = get_vectorizer_body(features)
```

Next let's see how to use `_search_vectorize` to generate the actual dataset. At that point, we will be in position to train a model and evaluate on its accuracy.

## Generating a Dataset

Let's start by taking a look at the response returned by using `_search_vectorize` on the small vectorizer made of the top 10 positive keywords:

```javascript
GET sentiment140/tweets/_search_vectorize?sparse_format=coo
{
  "query": {
    "terms": {
      "text": [
        "you", "thanks", "love", "good", "your", "great", "thank", "happy", "awesome", "haha"
      ]
    }
  },
  "vectorizer": [
    {
      "field": "text",
      "span": [
        "you", "thanks", "love", "good", "your", "great", "thank", "happy", "awesome", "haha"
      ],
      "value": "binary"
    },
    {
      "field": "polarity",
      "span": 1
    }
  ],
  "size": 3
}
```

And the response:

```javascript
{
  "took": 25,
  "timed_out": false,
  "shape": [
    3,
    11
  ],
  "matrix": {
    "row": [
      0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2
    ],
    "col": [
      0, 2, 3, 4, 5, 7, 9, 10, 0, 2, 4, 6, 8, 9, 10, 0, 2, 4, 5, 7, 8, 10
    ],
    "data": [
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
    ]
  }
}
```

Something to note here is that we asked to return the matrix in `COO` format, instead of an array `DICT` format. The COO sparse matrix format is better suited for the SciPy sparse package, as we can directly use its output to form a sparse matrix object. A COO matrix has a `row`, `col` and `data` so that each non zero entry in the matrix corresponds to a coordinate of the form (row_*i*, col_*i*, data_*i*). For example, the entry at (2, 0) is non-zero and of value 1.

The `_search_vectorize` endpoint supports every option that the traditional `_search` endpoint supports, including scan and scroll. The third step of Python tutorial file makes a scan request, and then concatenate each matrix from the batch.

```python
# 3) generate a dataset with this vectorizer
def generate_dataset(vectorizer, batch_size=100000, cutoff=-1):
    # perform the request with scan and scroll
    vectorizer['size'] = batch_size
    resp = vectorizer_client.scan(_index, _type, body=vectorizer, params={'scroll': '5m', 'sparse_format': 'coo'})

    # process the data into a dataset
    row = np.array([], dtype=np.int8)
    col, data, shape = [], [], [0, 0]
    for i, r in enumerate(resp):
        if i == cutoff or 'matrix' not in r:
            break
        print 'processing set #%s' % i

        # we simply append to the existing data and columns
        data.extend(r['matrix']['data'])
        col.extend(r['matrix']['col'])

        # we need to add the last coord to the current row
        current_row = np.array(r['matrix']['row'], dtype=np.long)
        if i == 0:
            row = current_row
        else:
            row = np.concatenate([row, row[-1] + current_row])

        # and increase the number of lines
        current_shape = r['shape']
        if i == 0:
            shape = current_shape
        else:
            shape[0] += current_shape[0]

    return csc_matrix((data, (row, col)), shape=shape, dtype=np.long)
dataset = generate_dataset(vectorizer, batch_size=100000, cutoff=-1)
```

Each of the returned matrices are concatenated. For COO matrices, this boils down to appending the columns and data values with their previous values. The rows must be incremented by the previous row value returned, as well as the shape values. Finally, we can directly make the sparse matrix object with the data, row, col and shape values obtained.

## Training the Model

At this point, we now have the dataset in memory. We could perform some analytics on this dataset, but let's keep it focused to using scikit-learn to train the model. First, we need to slice the matrix column wise to obtain the examples data on one side and the target labels on the other side. Second, we make use of scikit-learn's utility function to split the dataset into a training set and a test set. The training set uses 77% of the example data, while the test uses the rest.

```python
# 4.a) split as training and a test set
def get_train_test_split(dataset, test_size=0.33):
    # convert data to csr for faster row slicing
    data = dataset[:, :-1].tocsr()
    target = dataset[:, -1].toarray().ravel()
    return cross_validation.train_test_split(data, target, test_size=test_size)
train_data, test_data, train_target, test_target = get_train_test_split(dataset, test_size=0.33)
```

Our machine learning algorithm is a Linear SVC. We choose this method because SVMs are known to perform well in a high dimensional sparse feature space. Also we don't bother performing any parameter tuning whatsoever which would require using another validation set.

```python
# 4.b) use scikit-learn to train a model on the train set
def train_model(train_data, train_target):
    # we use a linear SVC for the sake of this example
    model = svm.LinearSVC()
    print 'training the model ...'
    model.fit(train_data, train_target)
    return model
model = train_model(train_data, train_target)
```

We are now ready to evaluate the model on the test set and compare our results with the ones given in the [sentiment140 paper][paper].

## Evaluating the Model

The last steps of the Python tutorial consists of evaluating the model and to report on its accuracy.

```python
# 5.a) evaluate the model on the test set
def evaluate(model, test_data):
    print 'evaluating the model ...'
    return model.predict(test_data)
y_pred = evaluate(model, test_data)

# 5.b) finally report the accuracy
print 'accuracy: %s' % metrics.accuracy_score(test_target, y_pred)
```

With roughly 6000 features obtained from the significant terms aggregation, our model performs an accuracy of 79.1% which isn't far from the 82.9% accuracy reported by the paper. If we had selected only the top 600 features, the accuracy of the model drops to only 76% accuracy. This indicates that significant terms are pretty good at generating a robust set of features, at least for this dataset.

## Final Thoughts

We could imagine having a server which would hold the models in memory. Then the client could request either to retrain a given model or to evaluate a given one. Under the hood the server would be pulling data from Elasticsearch using the Vectorize plugin, under a certain choice of vectorizers.

However, it would be nice if the model could be directly stored in Elasticsearch, so that it can be called, for example, in a script or in an aggregation. The second part of this tutorial covers this use case. Again, we will be using the Vectorize plugin to ensure that the features used for training are the same as the ones used for evaluation.

[vectorize]: https://github.com/alexksikes/elasticsearch-vectorize/
[docterm]: https://en.wikipedia.org/wiki/Document-term_matrix
[sentiment140]: http://help.sentiment140.com/for-students
[tutorial]: https://github.com/alexksikes/elasticsearch-vectorize/blob/master/tools/tutorial.py
[scipy]: https://www.scipy.org/
[scikit]: http://scikit-learn.org/
[paper]: http://cs.stanford.edu/people/alecmgo/papers/TwitterDistantSupervision09.pdf
[panda]: http://pandas.pydata.org/
[r]: https://www.r-project.org/
[mllib]: https://spark.apache.org/mllib/
[python]: https://www.python.org
[elasticsearch]: https://www.elastic.co/downloads/elasticsearch
