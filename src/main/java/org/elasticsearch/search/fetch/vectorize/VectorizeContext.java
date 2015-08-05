package org.elasticsearch.search.fetch.vectorize;

import org.elasticsearch.search.fetch.FetchSubPhaseContext;
import org.elasticsearch.vectorize.Vectorizer;

public class VectorizeContext extends FetchSubPhaseContext {

    private Vectorizer vectorizer = null;

    public VectorizeContext() {
    }

    public void setVectorizer(Vectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    public Vectorizer getVectorizer() {
        return this.vectorizer;
    }
}
