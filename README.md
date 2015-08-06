Elasticsearch Vectorize Plugin
==============================

The Vectorize Plugin is used to return a
[document-term matrix](https://en.wikipedia.org/wiki/Document-term_matrix)
according to some user given specification. In a document-term, the rows
correspond to documents in an index and the columns correspond to terms, or
more precisely to some numerical value associated with each term such as tf or
tf-idf. Such a matrix could then be used as a dataset and loaded in your
favorite statistical environment.

Example of Usage
----------------

```js
GET /index/type/_search_vectorize
{
  "query": {
    "function_score": {
      "functions": [
        {
          "random_score": {}
        }
      ]
    }
  },
  "size": 1000,
  "vectorizer": [
    {
      "field": "text",
      "span": [... list of terms ...],
      "value": "term_freq"
    },
    {
      "field": "field_numeric_1",
      "span": 1
    },
    {
      "field": "field_numeric_2",
      "span": 1
    },
    {
      "field": "field_numeric_3",
      "span": 5
    },
    {
      "field": "label",
      "span": 1
    }
  ]
}
```

and the response:

```js
{
  "shape": [1000, 9],
  "vector": [
    {"3": 5, "5": 2, "6": 0.55, "7": 1},
    {"0": 2, "3": 1, "6": 0.21, "7": 1, "8": 1},
    {"3": 5, "3": 5, "6": 0.45, "7": 0},
    {"3": 5, "5": 3, "6": 0.56, "7": 0, "8": 1},
    ...
  ]
}
```

or in [COO](https://en.wikipedia.org/wiki/Sparse_matrix) sparse format:

```js
GET /index/type/_search_vectorize?sparse_format=coo
{
    ...
}
```

and the response:

```json
{
  "shape": [1000, 9],
  "matrix": {
    "row": [0, 0, 0, 0, 1, 1, 1, 1, 1, ...],
    "col": [3, 5, 6, 7, 0, 3, 6, 7, 8, ...],
    "data": [5, 2, 0.55, 1, 2, 1, 0.21, 1, 1, ...]
  }
}
```

It supports all options that `search` supports including scan and scroll.
There is also a `_vectorize` endpoint to get a single example with a Java API.
For more performance (but requires more client side parsing), you can use the
`vectorize-fetch` fetch sub-phase directly.

Installation
------------

NOTE: This is still **very** alpha.

If you'd still like to play with it, you can do so by building a `SNAPSHOT`
version with Maven:

```bash
mvn clean install -DskipTests
bin/plugin install vectorize \
       --url file:target/releases/elasticsearch-vectorize-X.X.X-SNAPSHOT.zip
```

License
-------

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2015 Elasticsearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
