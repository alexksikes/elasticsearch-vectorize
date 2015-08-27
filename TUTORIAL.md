# Machine Learning Fun with Elasticsearch - Part 1

The first part of this tutorial covers how to use the [Vectorize plugin](https://github.com/alexksikes/elasticsearch-vectorize/) in order to obtain a dataset from data stored in Elasticsearch. That dataset can then be used by external data analytics tools such as Panda or R or by machine learning packages such as scikit-learn or MLlib. The second part of this tutorial is focused on storing a trained model back in Elasticsearch, and then on using the Vectorize plugin again to evaluate the model in scripts or in aggregations.

The Vectorize plugin is simply a mechanism to extract data as a [document-term matrix](https://en.wikipedia.org/wiki/Document-term_matrix) in a consistent manner. What we mean by consistent is that if new features are added later on, then our specification will ensure that the same features will occupy the same column in the matrix. In order to show how the plugin works and why it is useful, we will build a classifier to distinguish between negative and positive sentiments. To this effect we will be using the [sentiment140](http://help.sentiment140.com/for-students) dataset.

This tutorial is mainly a more detailed explanation of the heavily commented Python [example](https://github.com/alexksikes/elasticsearch-vectorize/blob/master/tools/tutorial.py) that ships with the plugin. We can already take a quick look at the main steps involved:

```python
# first let's get a good set of features using significant terms
features = get_features(3000)

# now let's create a vectorizer with these features
vectorizer = get_vectorizer_body(features)

# generate a dataset with this vectorizer
dataset = generate_dataset(vectorizer, batch_size=100000, cutoff=-1)

# generate a training and a test set
train_data, test_data, train_target, test_target = get_train_test_split(dataset, test_size=0.33)

# use scikit-learn to train a model on the train set
model = train_model(train_data, train_target)

# evaluate the model on the test set
y_pred = evaluate(model, test_data)

# and finally report the accuracy
print 'accuracy: %s' % metrics.accuracy_score(test_target, y_pred)
```

So to summarize first we get a good set of features, then create a vectorizer to obtain a document-term matrix. Then a model is trained on this matrix. Finally we evaluate the model and report on its accuracy. Each of these steps will be explained in greater details, but first let's get some necessary prerequisites. So let's get started!

## Prerequisites

First, make sure you have Python with [scipy](https://www.scipy.org/) and [scikit-learn](http://scikit-learn.org/) installed. I will pass on the setup instructions, but basically it boils down to installing Python if not already done so and `pip install scipy scikit-learn`.

Second, you will also need to install the [Vectorize plugin](https://github.com/alexksikes/elasticsearch-vectorize). The Vectorize plugin is still very early on in its development. At the moment it is only supported by the latest Elasticsearch beta-1 release. You will need to build it source also. Please follow the Maven instructions which are included with the plugin.

## Dataset Preparation

Next we need to index the sentiment140 dataset. The datastet comes in a csv format. However, we have an already prepared json copy of this dataset.

Please feel free to download it:

> wget http://data.elasticsearch.org/sentiment140/sentiment140.bulk.tar.gz

Decompress it:

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

Before training our model it is crucial to come up with a good set of features. We could be using all the tokenized keywords in the `text` field. However, it would more desirable to select a smaller but more interesting set of features out of the gate. This would help reduce computation time but also probably improve on the performance of our model. Since we want to discriminate between negative and positive tweets, we could use a significant terms aggregation against each of the classes, and be the features the union of that set.

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

This is what we do in the first step of the tutorial.py file, only that we get 3000 features for each class. We will see later that we could have selected fewer features and still get a pretty good accuracy. However, for the machine learning purist out there, this would already be cheating.

OK let's move on to specifying how a document-term matrix should be generated from the index. That matrix will then be used as input to our machine learning model. This is when the Vectorize plugin joins the party.

## Creating a Vectorizer

Now that we have a good set of features

## Generating a Dataset

## Training the Model

## Evaluating the Model

## Final Thoughts
