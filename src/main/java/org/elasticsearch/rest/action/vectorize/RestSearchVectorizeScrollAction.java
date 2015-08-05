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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.vectorize.SearchVectorizeResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.search.RestSearchScrollAction;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.search.Scroll;

import static org.elasticsearch.common.unit.TimeValue.parseTimeValue;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 *
 */
public class RestSearchVectorizeScrollAction extends BaseRestHandler {

    @Inject
    public RestSearchVectorizeScrollAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);

        controller.registerHandler(GET, "/_search_vectorize/scroll", this);
        controller.registerHandler(POST, "/_search_vectorize/scroll", this);
        controller.registerHandler(GET, "/_search_vectorize/scroll/{scroll_id}", this);
        controller.registerHandler(POST, "/_search_vectorize/scroll/{scroll_id}", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        String scrollId = request.param("scroll_id");
        SearchScrollRequest searchScrollRequest = new SearchScrollRequest();
        searchScrollRequest.scrollId(scrollId);
        String scroll = request.param("scroll");
        if (scroll != null) {
            searchScrollRequest.scroll(new Scroll(parseTimeValue(scroll, null, "scroll")));
        }

        if (RestActions.hasBodyContent(request)) {
            XContentType type = XContentFactory.xContentType(RestActions.getRestContent(request));
            if (type == null) {
                if (scrollId == null) {
                    scrollId = RestActions.getRestContent(request).toUtf8();
                    searchScrollRequest.scrollId(scrollId);
                }
            } else {
                // NOTE: if rest request with xcontent body has request parameters, these parameters override xcontent values
                RestSearchScrollAction.buildFromContent(RestActions.getRestContent(request), searchScrollRequest);
            }
        }
        client.searchScroll(searchScrollRequest, new RestBuilderListener<SearchResponse>(channel) {
            @Override
            public RestResponse buildResponse(SearchResponse resp, XContentBuilder builder) throws Exception {
                new SearchVectorizeResponse(resp).toXContent(builder, ToXContent.EMPTY_PARAMS);
                return new BytesRestResponse(OK, builder);
            }
        });
    }
}
