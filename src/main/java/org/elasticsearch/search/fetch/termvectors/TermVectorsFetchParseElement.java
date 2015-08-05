package org.elasticsearch.search.fetch.termvectors;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.FetchSubPhaseParseElement;
import org.elasticsearch.search.internal.SearchContext;

public class TermVectorsFetchParseElement extends FetchSubPhaseParseElement<TermVectorsFetchContext> {

    @Override
    protected void innerParse(XContentParser parser, TermVectorsFetchContext termVectorsFetchContext, SearchContext searchContext) throws Exception {
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.VALUE_STRING) {
            String fieldName = parser.text();
            termVectorsFetchContext.setField(fieldName);
        } else {
            throw new IllegalStateException("Expected a VALUE_STRING but got " + token);
        }
    }

    @Override
    protected FetchSubPhase.ContextFactory getContextFactory() {
        return TermVectorsFetchSubPhase.CONTEXT_FACTORY;
    }
}
