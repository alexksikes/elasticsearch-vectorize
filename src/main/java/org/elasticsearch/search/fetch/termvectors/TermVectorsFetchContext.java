package org.elasticsearch.search.fetch.termvectors;

import org.elasticsearch.search.fetch.FetchSubPhaseContext;

public class TermVectorsFetchContext extends FetchSubPhaseContext {

    private String field = null;

    public TermVectorsFetchContext() {
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
