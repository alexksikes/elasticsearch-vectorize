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
import org.elasticsearch.action.vectorize.SearchVectorizeResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 *
 */
public class RestSearchVectorizeAction extends BaseRestHandler {

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

        final String sparseFormat = request.param("sparse_format", "dict");
        client.search(searchRequest, new RestBuilderListener<SearchResponse>(channel) {
            @Override
            public RestResponse buildResponse(SearchResponse resp, XContentBuilder builder) throws Exception {
                SearchVectorizeResponse searchVectorizeResponse = new SearchVectorizeResponse(resp);
                searchVectorizeResponse.setFormat(sparseFormat);
                searchVectorizeResponse.toXContent(builder, ToXContent.EMPTY_PARAMS);
                return new BytesRestResponse(OK, builder);
            }
        });
    }
}
