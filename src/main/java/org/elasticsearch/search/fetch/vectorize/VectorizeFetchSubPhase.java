package org.elasticsearch.search.fetch.vectorize;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.vectorize.VectorizeRequest;
import org.elasticsearch.action.vectorize.VectorizeResponse;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.vectorize.VectorizeService;
import org.elasticsearch.vectorize.Vectorizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VectorizeFetchSubPhase implements FetchSubPhase {

    public static final String NAME = "vectorize-fetch";
    
    public static final ContextFactory<VectorizeContext> CONTEXT_FACTORY = new ContextFactory<VectorizeContext>() {

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public VectorizeContext newContextInstance() {
            return new VectorizeContext();
        }
    };

    public VectorizeFetchSubPhase() {
    }

    @Override
    public Map<String, ? extends SearchParseElement> parseElements() {
        return ImmutableMap.of("vectorizer", new VectorizeFetchParseElement());
    }

    @Override
    public boolean hitsExecutionNeeded(SearchContext context) {
        return false;
    }

    @Override
    public void hitsExecute(SearchContext context, InternalSearchHit[] hits) {
    }

    @Override
    public boolean hitExecutionNeeded(SearchContext context) {
        return context.getFetchSubPhaseContext(CONTEXT_FACTORY).hitExecutionNeeded();
    }

    @Override
    public void hitExecute(SearchContext context, HitContext hitContext) {
        Vectorizer vectorizer = context.getFetchSubPhaseContext(CONTEXT_FACTORY).getVectorizer();

        if (hitContext.hit().fieldsOrNull() == null) {
            hitContext.hit().fields(new HashMap<String, SearchHitField>());
        }
        SearchHitField hitField = hitContext.hit().fields().get("matrix");
        if (hitField == null) {
            hitField = new InternalSearchHitField("matrix", new ArrayList<>(1));
            hitContext.hit().fields().put("matrix", hitField);
        }

        String index = context.indexShard().indexService().index().getName();
        String type = hitContext.hit().type();
        String id = hitContext.hit().id();

        VectorizeResponse response = new VectorizeService(context.indexShard()).getVector(
                new VectorizeRequest(index, type, id).vectorizer(vectorizer)
        );

        Map<String, Integer> out = new HashMap<>();
        try {
            Vectorizer.SparseVector vector = response.getVector();
            while (vector.hasNext()) {
                Vectorizer.Coord coord = vector.next();
                out.put(String.valueOf(coord.x), coord.y);
            }
            hitField.values().add(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
