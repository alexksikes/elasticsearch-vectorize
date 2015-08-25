import elasticsearch
import numpy as np
from scipy.sparse import csc_matrix
from sklearn import cross_validation
from sklearn import svm
from sklearn import metrics

import vectorize

_index = "sentiment140"
_type = "tweets"

client = elasticsearch.Elasticsearch(timeout=100)
vectorizer_client = vectorize.VectorizeClient(client)


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


def get_train_test_split(dataset, test_size=0.33):
    # convert data to csr for faster row slicing
    data = dataset[:, :-1].tocsr()
    target = dataset[:, -1].toarray().ravel()
    return cross_validation.train_test_split(data, target, test_size=test_size)


def train_model(train_data, train_target):
    # we use a linear SVC for the sake of this example
    model = svm.LinearSVC()
    print 'training the model ...'
    model.fit(train_data, train_target)
    return model


def evaluate(model, test_data):
    print 'evaluating the model ...'
    return model.predict(test_data)


if __name__ == '__main__':
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
