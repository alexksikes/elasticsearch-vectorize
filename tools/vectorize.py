from elasticsearch.client.utils import AddonClient, query_params, _make_path, \
    SKIP_IN_PATH


class VectorizeClient(AddonClient):
    namespace = 'vectorize'

    @query_params('parent', 'preference', 'realtime', 'routing', 'version',
        'version_type', 'sparse_format')
    def vectorize(self, index, doc_type, id, body, params=None):
        """
        :arg index: The name of the index
        :arg doc_type: The type of the document
        :arg id: The document ID
        :arg body: The vectorizer definition
        :arg parent: The ID of the parent document
        :arg preference: Specify the node or shard the operation should be
            performed on (default: random)
        :arg realtime: Specify whether to perform the operation in realtime or
            search mode
        :arg routing: Specific routing value
        :arg version: Explicit version number for concurrency control
        :arg version_type: Explicit version number for concurrency control
        """
        for param in (index, doc_type, id, body):
            if param in SKIP_IN_PATH:
                raise ValueError("Empty value passed for a required argument.")
        _, data = self.transport.perform_request('GET', _make_path(index, doc_type, id, '_vectorize'),
            params=params, body=body)
        return data

    @query_params('analyze_wildcard', 'analyzer', 'default_operator', 'df',
        'explain', 'fielddata_fields', 'fields', 'indices_boost', 'lenient',
        'allow_no_indices', 'expand_wildcards', 'ignore_unavailable',
        'lowercase_expanded_terms', 'from_', 'preference', 'q', 'query_cache',
        'routing', 'scroll', 'search_type', 'size', 'sort', 'source', 'stats',
        'suggest_field', 'suggest_mode', 'suggest_size', 'suggest_text',
        'terminate_after', 'timeout', 'track_scores', 'version', 'sparse_format')
    def search(self, index, doc_type, body, params=None):
        """
        :arg index: The name of the index
        :arg doc_type: The type of the document
        :arg id: The document ID
        :arg body: The search definition with a vectorizer definition
        :arg analyze_wildcard: Specify whether wildcard and prefix queries
            should be analyzed (default: false)
        :arg analyzer: The analyzer to use for the query string
        :arg default_operator: The default operator for query string query (AND
            or OR) (default: OR)
        :arg df: The field to use as default where no field prefix is given in
            the query string
        :arg explain: Specify whether to return detailed information about
            score computation as part of a hit
        :arg fielddata_fields: A comma-separated list of fields to return as the
            field data representation of a field for each hit
        :arg fields: A comma-separated list of fields to return as part of a hit
        :arg indices_boost: Comma-separated list of index boosts
        :arg lenient: Specify whether format-based query failures (such as
            providing text to a numeric field) should be ignored
        :arg allow_no_indices: Whether to ignore if a wildcard indices
            expression resolves into no concrete indices. (This includes `_all`
            string or when no indices have been specified)
        :arg expand_wildcards: Whether to expand wildcard expression to concrete
            indices that are open, closed or both., default 'open'
        :arg ignore_unavailable: Whether specified concrete indices should be
            ignored when unavailable (missing or closed)
        :arg lowercase_expanded_terms: Specify whether query terms should be lowercased
        :arg from\_: Starting offset (default: 0)
        :arg preference: Specify the node or shard the operation should be
            performed on (default: random)
        :arg q: Query in the Lucene query string syntax
        :arg query_cache: Enable or disable caching on a per-query basis
        :arg routing: A comma-separated list of specific routing values
        :arg scroll: Specify how long a consistent view of the index should be
            maintained for scrolled search
        :arg search_type: Search operation type
        :arg size: Number of hits to return (default: 10)
        :arg sort: A comma-separated list of <field>:<direction> pairs
        :arg source: The URL-encoded request definition using the Query DSL
            (instead of using request body)
        :arg stats: Specific 'tag' of the request for logging and statistical purposes
        :arg suggest_field: Specify which field to use for suggestions
        :arg suggest_mode: Specify suggest mode (default: missing)
        :arg suggest_size: How many suggestions to return in response
        :arg suggest_text: The source text for which the suggestions should be returned
        :arg terminate_after: The maximum number of documents to collect for
            each shard, upon reaching which the query execution will terminate
            early.
        :arg timeout: Explicit operation timeout
        :arg track_scores: Whether to calculate and return scores even if they
            are not used for sorting
        :arg version: Specify whether to return document version as part of a hit
        """
        # from is a reserved word so it cannot be used, use from_ instead
        if 'from_' in params:
            params['from'] = params.pop('from_')

        for param in (index, doc_type, body):
            if param in SKIP_IN_PATH:
                raise ValueError("Empty value passed for a required argument.")
        _, data = self.transport.perform_request('GET', _make_path(index, doc_type, '_search_vectorize'),
            params=params, body=body)
        return data

    @query_params('scroll')
    def scroll(self, scroll_id=None, body=None, params=None):
        """
        :arg scroll_id: The scroll ID
        :arg body: The scroll ID if not passed by URL or query parameter
        """
        if params is None:
            params = {}
        if scroll_id in SKIP_IN_PATH and body in SKIP_IN_PATH:
            raise ValueError("You need to supply scroll_id or body.")
        elif scroll_id and not body:
            body = scroll_id
        elif scroll_id:
            params['scroll_id'] = scroll_id

        _, data = self.transport.perform_request('GET', '/_search_vectorize/scroll',
            params=params, body=body)
        return data

    @query_params('sparse_format', 'scroll')
    def scan(self, index, doc_type, body, scroll='5m', params=None):
        """
        :arg index: The name of the index
        :arg doc_type: The type of the document
        :arg body: the search request with a vectorizer
        :arg scroll: Specify how long a consistent view of the index should be
            maintained for scrolled search
        """
        # initial search
        if params is None:
            params = {}
        params.update(search_type='scan', sroll=scroll)
        resp = self.search(index, doc_type, body=body, params=params)

        scroll_id = resp.get('_scroll_id')
        if scroll_id is None:
            return

        # scroll request
        while True:
            resp = self.scroll(scroll_id, params=params)
            yield resp
            scroll_id = resp.get('_scroll_id')
            if scroll_id is None or resp.get('shape') is None:
                break
