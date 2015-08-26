# Machine Learning Fun with Elasticsearch - Part 1

The first part of this tutorial is going to show how to use the Vectorize plugin in order to train a model on data stored in Elasticsearch. In the second part of this tutorial we will show how to store and then directly use such a model in Elasticsearch.

The Vectorize plugin is simply a mechanism to extract data as a [document-term matrix](https://en.wikipedia.org/wiki/Document-term_matrix) so that it could be used as input to our machine learning models. For the sake of this example, we will be working with the [sentiment140](http://help.sentiment140.com/for-students) dataset. First, we will index the data, then extract a document-term matrix from it, and finally train a model on this matrix. The model trained on this dataset will then be able to discriminate between negative and positive feelings. At the end tutorial we will compare the results obtained by our model with the ones given in the [sentiment140 article](http://cs.stanford.edu/people/alecmgo/papers/TwitterDistantSupervision09.pdf)

In order to follow this tutorial you will need Python with scipy and scikit-learn installed, and a copy of the sentiment140 dataset. We have an already prepared copy this dataset [here](http://data.elasticsearch.org/sentiment140/sentiment140.bulk.tar.gz). Please feel free to download and follow the instructions for indexing it. The full code used in this tutorial is available [here](https://github.com/alexksikes/elasticsearch-vectorize). Following are the main steps that we go through in order to train and evaluate our model. Each of these steps will be explained in greater details.

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

