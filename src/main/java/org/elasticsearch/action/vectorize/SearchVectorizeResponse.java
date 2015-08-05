package org.elasticsearch.action.vectorize;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;

/*
 * Just a wrapper around search response in order to render a matrix instead of search results.
 */
public class SearchVectorizeResponse extends ActionResponse implements ToXContent {

    static final class Fields {
        static final XContentBuilderString _SCROLL_ID = new XContentBuilderString("_scroll_id");
        static final XContentBuilderString TOOK = new XContentBuilderString("took");
        static final XContentBuilderString TIMED_OUT = new XContentBuilderString("timed_out");
        static final XContentBuilderString TERMINATED_EARLY = new XContentBuilderString("terminated_early");
    }

    private SearchResponse searchResponse;

    public SearchVectorizeResponse(SearchResponse searchResponse) {
        this.searchResponse = searchResponse;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        // and build a matrix from the response
        builder.startObject();
        if (searchResponse.getScrollId() != null) {
            builder.field(Fields._SCROLL_ID, searchResponse.getScrollId());
        }
        builder.field(Fields.TOOK, searchResponse.getTookInMillis());
        builder.field(Fields.TIMED_OUT, searchResponse.isTimedOut());
        if (searchResponse.isTerminatedEarly() != null) {
            builder.field(Fields.TERMINATED_EARLY, searchResponse.isTerminatedEarly());
        }
        if (hasHits()) {
            buildDictMatrix(searchResponse.getHits(), builder);
        }
        return builder.endObject();
    }

    private void buildDictMatrix(SearchHits hits, XContentBuilder builder) throws IOException {
        int numCols = (int) hits.getAt(0).field("shape").values().get(0);
        builder.field("shape", new int[]{hits.getHits().length, numCols});

        builder.startArray("matrix");
        for (SearchHit searchHitFields : hits) {
            SearchHitField hitField = searchHitFields.field("matrix");
            if (hitField == null) {
                continue;
            } else {
                builder.value(hitField.value());
            }
        }
        builder.endArray();
    }

    private boolean hasHits() {
        return searchResponse.getHits().getHits().length != 0;
    }
}
