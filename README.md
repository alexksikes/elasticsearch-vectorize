Elasticsearch Vectorize Plugin
===================================

To build a `SNAPSHOT` version, you can either build it with Maven:

```bash
mvn clean install -DskipTests
bin/plugin install vectorize \
       --url file:target/releases/elasticsearch-vectorize-X.X.X-SNAPSHOT.zip
```

Or grab the latest binary for the Elasticsearch [2.0](https://github.com/elastic/elasticsearch-vectorize/releases/download/v2.0.0-beta1/elasticsearch-vectorize-2.0.0-beta1-SNAPSHOT.zip) or for 
[1.x](https://github.com/elastic/elasticsearch-vectorize/releases/download/v1.0.1/elasticsearch-vectorize-1.0.1-SNAPSHOT.zip) and install it:

```bash
cd /path/to/elasticsearch/
bin/plugin install vectorize --url file:/path/to/downloads/elasticsearch-vectorize-X.X.X-SNAPSHOT.zip
```

Example of Usage
----------------

```js
GET /imdb/movies/111161/_vectorize
{
  "vectorizer": [
    {
      "field": "plot_keywords",
      "span": ["pie", "sdfdsf", "police"]
    },
    {
      "field": "year",
      "span": 1
    },
    {
      "field": "plot",
      "span": ["two", "dsfsdf", "imprisoned","bond", "solace"]
    }
  ]
}
```

and the response:

```js
GET /imdb/movies/111161/_vectorize
{
  "vectorizer": [
    {
      "field": "plot_keywords",
      "span": ["pie", "sdfdsf", "police"]
    },
    {
      "field": "year",
      "span": 1
    },
    {
      "field": "plot",
      "span": ["two", "dsfsdf", "imprisoned","bond", "solace"]
    }
  ]
}
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
