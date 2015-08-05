/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.rest.action.vectorize;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 *
 */
public class RestSearchVectorizeAction extends BaseRestHandler {

    static final class Fields {
        static final XContentBuilderString _SCROLL_ID = new XContentBuilderString("_scroll_id");
        static final XContentBuilderString TOOK = new XContentBuilderString("took");
        static final XContentBuilderString TIMED_OUT = new XContentBuilderString("timed_out");
        static final XContentBuilderString TERMINATED_EARLY = new XContentBuilderString("terminated_early");
    }

    @Inject
    public RestSearchVectorizeAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(GET, "/{index}/{type}/_search_vectorize", this);
        controller.registerHandler(POST, "/{index}/{type}/_search_vectorize", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) throws IOException {
        // we just mock a typical search request
        SearchRequest searchRequest;
        searchRequest = RestSearchAction.parseSearchRequest(request, parseFieldMatcher);
        searchRequest.extraSource("{\"_source\": false}");

        client.search(searchRequest, new RestBuilderListener<SearchResponse>(channel) {
            @Override
            public RestResponse buildResponse(SearchResponse resp, XContentBuilder builder) throws Exception {
                // and build a matrix from the response
                builder.startObject();
                if (resp.getScrollId() != null) {
                    builder.field(Fields._SCROLL_ID, resp.getScrollId());
                }
                builder.field(Fields.TOOK, resp.getTookInMillis());
                builder.field(Fields.TIMED_OUT, resp.isTimedOut());
                if (resp.isTerminatedEarly() != null) {
                    builder.field(Fields.TERMINATED_EARLY, resp.isTerminatedEarly());
                }
                buildDictMatrix(resp.getHits(), builder);
                builder.endObject();
                return new BytesRestResponse(OK, builder);
            }
        });
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
}
