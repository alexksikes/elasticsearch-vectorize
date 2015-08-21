package org.elasticsearch.action.vectorize;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.vectorize.Vectorizer.FieldStrings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private VectorizeRequest.Format format;

    public SearchVectorizeResponse(SearchResponse searchResponse) {
        this.searchResponse = searchResponse;
    }

    public VectorizeRequest.Format getFormat() {
        return this.format;
    }

    public void setFormat(VectorizeRequest.Format format) {
        this.format = format;
    }

    public void setFormat(String format) {
        this.format = VectorizeRequest.Format.valueOf(format.toUpperCase());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
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
            buildShape(searchResponse.getHits(), builder);
            if (format == VectorizeRequest.Format.COO) {
                buildCOOMatrix(searchResponse.getHits(), builder);
            } else {
                buildDictMatrix(searchResponse.getHits(), builder);
            }
        }
        return builder.endObject();
    }

    private void buildDictMatrix(SearchHits hits, XContentBuilder builder) throws IOException {
        builder.startArray(FieldStrings.MATRIX);
        for (SearchHit searchHitFields : hits) {
            SearchHitField hitField = searchHitFields.field("matrix");
            if (hitField == null) {
                continue;
            } else {
                builder.startObject();
                Map<Integer, Integer> value = hitField.getValue();
                for (Map.Entry<Integer, Integer> entry : value.entrySet()) {
                    builder.field(entry.getKey().toString(), entry.getValue());
                }
                builder.endObject();
            }
        }
        builder.endArray();
    }

    private void buildCOOMatrix(SearchHits hits, XContentBuilder builder) throws IOException {
        // NOTE: it would be easier if the sub-matrices could come up already in coo
        List<Integer> row = new ArrayList<>();
        List<Object> col = new ArrayList<>();
        List<Object> data = new ArrayList<>();

        int i = 0;
        for (SearchHit searchHitFields : hits) {
            SearchHitField hitField = searchHitFields.field("matrix");
            if (hitField == null) {
                continue;
            } else {
                Map<Integer, Integer> value = hitField.getValue();
                col.addAll(value.keySet());
                data.addAll(value.values());

                Integer[] currentRow = new Integer[value.size()];
                Arrays.fill(currentRow, i);
                row.addAll(Arrays.asList(currentRow));
            }
            i++;
        }

        builder.startObject(FieldStrings.MATRIX);
        builder.field(FieldStrings.ROW, row);
        builder.field(FieldStrings.COL, col);
        builder.field(FieldStrings.DATA, data);
        builder.endObject();
    }

    private void buildShape(SearchHits hits, XContentBuilder builder) throws IOException {
        int numCols = (int) hits.getAt(0).field("shape").values().get(0);
        builder.field("shape", new int[]{hits.getHits().length, numCols});
    }

    private boolean hasHits() {
        return searchResponse.getHits().getHits().length != 0;
    }
}
