package org.elasticsearch.search.fetch.termvectors;

import com.google.common.collect.ImmutableMap;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchParseElement;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TermVectorsFetchSubPhase implements FetchSubPhase {

    public static final ContextFactory<TermVectorsFetchContext> CONTEXT_FACTORY = new ContextFactory<TermVectorsFetchContext>() {

        @Override
        public String getName() {
            return NAMES[0];
        }

        @Override
        public TermVectorsFetchContext newContextInstance() {
            return new TermVectorsFetchContext();
        }
    };

    public TermVectorsFetchSubPhase() {
    }

    public static final String[] NAMES = {"term_vectors_fetch"};

    @Override
    public Map<String, ? extends SearchParseElement> parseElements() {
        return ImmutableMap.of("term_vectors_fetch", new TermVectorsFetchParseElement());
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
        String field = context.getFetchSubPhaseContext(CONTEXT_FACTORY).getField();

        if (hitContext.hit().fieldsOrNull() == null) {
            hitContext.hit().fields(new HashMap<String, SearchHitField>());
        }
        SearchHitField hitField = hitContext.hit().fields().get(NAMES[0]);
        if (hitField == null) {
            hitField = new InternalSearchHitField(NAMES[0], new ArrayList<>(1));
            hitContext.hit().fields().put(NAMES[0], hitField);
        }
        TermVectorsResponse termVector = context.indexShard().termVectorsService().getTermVectors(new TermVectorsRequest(context.indexShard().indexService().index().getName(), hitContext.hit().type(), hitContext.hit().id()), context.indexShard().indexService().index().getName());
        try {
            Map<String, Integer> tv = new HashMap<>();
            TermsEnum terms = termVector.getFields().terms(field).iterator();
            BytesRef term;
            while ((term = terms.next()) != null) {
                tv.put(term.utf8ToString(), terms.postings(null, null, PostingsEnum.ALL).freq());
            }
            hitField.values().add(tv);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
