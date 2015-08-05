package org.elasticsearch.plugin.fetchtermvectors;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.search.fetch.FetchSubPhaseModule;
import org.elasticsearch.search.fetch.vectorize.VectorizeFetchSubPhase;

public class FetchTermVectorsPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "vectorize-fetch";
    }

    @Override
    public String description() {
        return "fetch plugin to test if the plugin mechanism works";
    }

    public void onModule(FetchSubPhaseModule fetchSubPhaseModule) {
        fetchSubPhaseModule.registerFetchSubPhase(VectorizeFetchSubPhase.class);
    }
}
