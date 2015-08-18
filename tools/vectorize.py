from elasticsearch.client.utils import AddonClient, query_params, _make_path, \
    SKIP_IN_PATH


class VectorizeClient(AddonClient):
    namespace = 'vectorize'

    @query_params()
    def vectorize(self, index, doc_type, id, body, params=None):
        """
        :arg index: The name of the index
        :arg doc_type: The type of the document
        :arg id: Document ID
        :arg body: the vectorizer
        """
        for param in (index, doc_type, id, body):
            if param in SKIP_IN_PATH:
                raise ValueError("Empty value passed for a required argument.")
        _, data = self.transport.perform_request('GET', _make_path(index, doc_type, id, '_vectorize'),
            params=params, body=body)
        return data

    @query_params('scroll', 'search_type', 'size')
    def search_vectorize(self, index, doc_type, body, params=None):
        """
        :arg index: The name of the index
        :arg doc_type: The type of the document
        :arg body: the search request with a vectorizer
        """
        for param in (index, doc_type, body):
            if param in SKIP_IN_PATH:
                raise ValueError("Empty value passed for a required argument.")
        _, data = self.transport.perform_request('GET', _make_path(index, doc_type, '_search_vectorize'),
            params=params, body=body)
        return data

    @query_params('scroll')
    def scroll_vectorize(self, scroll_id=None, body=None, params=None):
        """
        :arg scroll_id: The scroll ID
        :arg body: The scroll ID if not passed by URL or query parameter
        :arg scroll: Specify how long a consistent view of the index should be
            maintained for scrolled search
        """
        if scroll_id in SKIP_IN_PATH and body in SKIP_IN_PATH:
            raise ValueError("You need to supply scroll_id or body.")
        elif scroll_id and not body:
            body = scroll_id
        elif scroll_id:
            params['scroll_id'] = scroll_id

        _, data = self.transport.perform_request('GET', '/_search_vectorize/scroll',
            params=params, body=body)
        return data
