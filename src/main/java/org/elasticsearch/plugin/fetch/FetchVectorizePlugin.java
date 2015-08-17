package org.elasticsearch.plugin.fetch;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.fetch.vectorize.VectorizeFetchSubPhase;

public class FetchVectorizePlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "vectorize-fetch";
    }

    @Override
    public String description() {
        return "fetch plugin to get a vectorized output";
    }

    public void onModule(SearchModule searchModule) {
        searchModule.registerFetchSubPhase(VectorizeFetchSubPhase.class);
    }
}
