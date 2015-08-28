# Machine Learning Fun with Elasticsearch - Part 1

The first part of this tutorial covers how to use the [Vectorize plugin](https://github.com/alexksikes/elasticsearch-vectorize/) in order to obtain a dataset from data stored in Elasticsearch. That dataset can then be used by external data analytics tools such as Panda or R or by machine learning packages such as scikit-learn or MLlib. The second part of this tutorial is focused on storing the trained model back in Elasticsearch, and then on using the Vectorize plugin again to evaluate the model in scripts or in aggregations.

The Vectorize plugin is simply a mechanism to extract data as a [document-term matrix](https://en.wikipedia.org/wiki/Document-term_matrix) in a consistent manner. What we mean by consistent is that if new features are added later on, then our specification will ensure that the same features will occupy the same column in the matrix. In order to show how the plugin works and why it is useful, we will train a classifier to distinguish between negative and positive sentiments. To this effect we will be using the [sentiment140](http://help.sentiment140.com/for-students) dataset.

This tutorial is mainly a more detailed explanation of the heavily commented Python [example](https://github.com/alexksikes/elasticsearch-vectorize/blob/master/tools/tutorial.py) that ships with the plugin. We can already take a quick look at the main steps involved:

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

So to summarize first we get a good set of features. Second, we create a vectorizer to obtain a document-term matrix from the indexed data. Third, using the vectorizer a dataset is generated. Fourth, a model is trained on this dataset. And fifth, the model is evaluated and we report on its accuracy. Each of these steps will be explained in greater details, but first let's get some necessary prerequisites. So let's get started!

## Prerequisites

First, make sure you have Python with [scipy](https://www.scipy.org/) and [scikit-learn](http://scikit-learn.org/) installed. I will pass on the setup instructions, but basically it boils down to installing Python if not already done so and `pip install scipy scikit-learn`.

Second, you will also need to install the [Vectorize plugin](https://github.com/alexksikes/elasticsearch-vectorize). The Vectorize plugin is still very early on in its development. At the moment it is only supported by the latest Elasticsearch beta-1 release. You will need to build it from source also. Please follow the Maven instructions which are included with the plugin.

## Dataset Preparation

Next we need to index the sentiment140 dataset. The datastet comes in a csv format. Thankfully, we have an already prepared json copy of this dataset.

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

Now we should be all set!

## Getting a Good Set of Features

Before training our model it is crucial to come up with a good set of features. We could be using all the tokenized keywords in the `text` field. However, it would more desirable to select a smaller but more interesting set of features out of the gate. This would help reduce computation time but also probably improve on the performance of our model. Since we want to discriminate between negative and positive tweets, we could use a significant terms aggregation against each of the classes.

Such an aggregation on the `1` class (positive tweets) would look like this:

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

Unsurprisingly, all these keywords relate to positive feelings, and therefore should be good features in helping the classifier choose between positive or negative tweets. This is essentially what we do in the first step of the Python code, , only that we get 3000 features for each of the classes, and then take the union of these keywords as out feature set.

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

Later on, we will see that we could have selected fewer features with very minimal hit on the accuracy of the model. However, for the machine learning purists out there, reducing the number of features at this point would already be cheating.

Let's now move on to specifying how a document-term matrix should be generated from the index. As we have already mentioned, this matrix will then be used as the input of our classifier. This is when the Vectorize plugin joins the party.

## Creating a Vectorizer

The next step consists of extracting a matrix from the indexed sentiment140 data. For that purpose we now have a good set of features which are to be found in the `text` field. We also know that the label of our data is to be found in the `polarity` field. As previously mentioned, a Vectorizer is simply a way of specifying a document-term matrix. As an illustrative example, let's see how such a vectorizer would look like for the top 10 positive keywords previously returned.

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

What this means is that we reserve 10 columns for the features found in the `text` field. These features are specified in order by an array of keywords following the `span` parameter. The values which are extracted if the document has the given keywords is provided by the `value` parameter. There are a couple of possible options here such as extracting the term frequency or the document frequency of the term. However, since the tweets are rather short sized, we could more simply ask for `binary` features. This means that a 1 is returned at this column if the document has the given feature, or 0 otherwise. Finally, the last column is occupied by the polarity of the tweet. Here the `span` parameter takes an integer, say n, which indicates to use the first n values found in the given field as is. Here we specify to use the first (and only) value found in the `polarity` field. This will serve as the label for our supervised machine learning method.

Let's take a look as to how such a vector would look like on a tweet we know contains many of such terms. For that purpose we will make use of the `_vectorize` endpoint of the Vectorize plugin.

So for example on this tweet:

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
    "user": "tylerceerius"
  }
}
```

Using the `_vectorize` endpoint with the vectorizer previously created:

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

The response is composed of a `shape` field together with a `matrix` field. The shape describes the size of the matrix obtained. Here, we have a vector of size `11`, because the vectorizer has implicitly specified 11 columns. The `matrix` field is the actual returned vector in a sparse format, meaning that zeros are omitted from the response. While working with text features, the vectors are usually very sparse, and therefore returning such a sparse response is quite desirable. Now looking at the vector returned, we see that this tweet has the keywords "you" (column 0), "love" (column 2), "good" (column 4), "your" (column 5), "great" (column 6) and a polarity of "1" (column 10).

Now we can come back to the second step of the Python tutorial file. What we do is essentially creating a vectorizer but not on the top 10 positive keywords, but on all the features previously returned, that is on the ~6000 negative and positive keywords.

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

As we will see next, we can apply the vectorizer not just on a single document but rather on to a set of document in order to generate a dataset. At this point, we will be in position to train a model and evaluate its accuracy.

## Generating a Dataset

## Training the Model

## Evaluating the Model

## Final Thoughts
