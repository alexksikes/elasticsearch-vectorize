package org.elasticsearch.search.fetch.termvectors;

import org.elasticsearch.search.fetch.FetchSubPhaseContext;

public class TermVectorsFetchContext extends FetchSubPhaseContext {

    private String[] field = null;

    public TermVectorsFetchContext() {
    }

    public void setFields(String[] field) {
        this.field = field;
    }

    public String[] getFields() {
        return field;
    }
}
