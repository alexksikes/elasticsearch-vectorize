package org.elasticsearch.search.fetch.termvectors;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.fetch.FetchSubPhaseParseElement;
import org.elasticsearch.search.internal.SearchContext;

import java.util.ArrayList;
import java.util.List;

public class TermVectorsFetchParseElement extends FetchSubPhaseParseElement<TermVectorsFetchContext> {

    @Override
    protected void innerParse(XContentParser parser, TermVectorsFetchContext termVectorsFetchContext, SearchContext searchContext) throws Exception {
        List<String> fields = new ArrayList<>();
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.START_ARRAY) {
            while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                fields.add(parser.text());
            }
        } else {
            throw new IllegalStateException("Expected a START_ARRAY but got " + token);
        }
        termVectorsFetchContext.setFields(fields.toArray(new String[0]));
    }

    @Override
    protected FetchSubPhase.ContextFactory getContextFactory() {
        return TermVectorsFetchSubPhase.CONTEXT_FACTORY;
    }
}
