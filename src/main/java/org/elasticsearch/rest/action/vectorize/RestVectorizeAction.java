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

import org.elasticsearch.action.vectorize.VectorizeRequest;
import org.elasticsearch.action.vectorize.VectorizeResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestToXContentListener;

import java.io.IOException;

import static org.elasticsearch.action.vectorize.VectorizeAction.INSTANCE;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 *
 */
public class RestVectorizeAction extends BaseRestHandler {

    @Inject
    public RestVectorizeAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(GET, "/{index}/{type}/{id}/_vectorize", this);
        controller.registerHandler(POST, "/{index}/{type}/{id}/_vectorize", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) throws IOException {
        VectorizeRequest vectorizeRequest = new VectorizeRequest(request.param("index"), request.param("type"), request.param("id"));
        if (RestActions.hasBodyContent(request)) {
            try (XContentParser parser = XContentFactory.xContent(RestActions.guessBodyContentType(request)).createParser(RestActions.getRestContent(request))){
                VectorizeRequest.parseRequest(vectorizeRequest, parser);
            }
        }
        readURIParameters(vectorizeRequest, request);

        client.execute(INSTANCE, vectorizeRequest, new RestToXContentListener<VectorizeResponse>(channel));
    }

    static public void readURIParameters(VectorizeRequest vectorizeRequest, RestRequest request) {
        vectorizeRequest.routing(request.param("routing"));
        vectorizeRequest.realtime(request.paramAsBoolean("realtime", null));
        vectorizeRequest.version(RestActions.parseVersion(request, vectorizeRequest.version()));
        vectorizeRequest.versionType(VersionType.fromString(request.param("version_type"), vectorizeRequest.versionType()));
        vectorizeRequest.parent(request.param("parent"));
        vectorizeRequest.preference(request.param("preference"));
        vectorizeRequest.dfs(request.paramAsBoolean("dfs", vectorizeRequest.dfs()));
    }
}
