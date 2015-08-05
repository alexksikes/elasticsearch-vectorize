package org.elasticsearch.search.fetch.vectorize;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.FetchSubPhaseParseElement;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.vectorize.Vectorizer;

public class VectorizeFetchParseElement extends FetchSubPhaseParseElement<VectorizeContext> {

    @Override
    protected void innerParse(XContentParser parser, VectorizeContext vectorizeContext, SearchContext searchContext) throws Exception {
        Vectorizer vectorizer = Vectorizer.parse(parser);
        vectorizeContext.setVectorizer(vectorizer);
    }

    @Override
    protected FetchSubPhase.ContextFactory getContextFactory() {
        return VectorizeFetchSubPhase.CONTEXT_FACTORY;
    }
}
