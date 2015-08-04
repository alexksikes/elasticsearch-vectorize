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

package org.elasticsearch.action.vectorize;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.vectorize.Vectorizer;

/**
 * 
 */
public class VectorizeRequestBuilder extends ActionRequestBuilder<VectorizeRequest, VectorizeResponse, VectorizeRequestBuilder> {

    public VectorizeRequestBuilder(ElasticsearchClient client, VectorizeAction action) {
        super(client, action, new VectorizeRequest());
    }

    public VectorizeRequestBuilder(ElasticsearchClient client, VectorizeAction action, String index, String type, String id) {
        super(client, action, new VectorizeRequest(index, type, id));
    }

    public VectorizeRequestBuilder setIndex(String index) {
        request.index(index);
        return this;
    }

    public VectorizeRequestBuilder setType(String type) {
        request.type(type);
        return this;
    }

    public VectorizeRequestBuilder setId(String id) {
        request.id(id);
        return this;
    }

    public VectorizeRequestBuilder setVectorizer(Vectorizer vectorizer) {
        request.vectorizer(vectorizer);
        return this;
    }

    public VectorizeRequestBuilder setFormat(VectorizeRequest.Format format) {
        request.format(format);
        return this;
    }

    public VectorizeRequestBuilder setFormat(String format) {
        request.format(format);
        return this;
    }

    public VectorizeRequestBuilder setRouting(String routing) {
        request.routing(routing);
        return this;
    }

    public VectorizeRequestBuilder setRealtime(Boolean realtime) {
        request.realtime(realtime);
        return this;
    }

    public VectorizeRequestBuilder setVersion(long version) {
        request.version(version);
        return this;
    }

    public VectorizeRequestBuilder setVersionType(VersionType versionType) {
        request.versionType(versionType);
        return this;
    }

    public VectorizeRequestBuilder setParent(String parent) {
        request.parent(parent);
        return this;
    }

    public VectorizeRequestBuilder setPreference(String preference) {
        request.preference(preference);
        return this;
    }

    public VectorizeRequestBuilder setDfs(boolean dfs) {
        request.dfs(dfs);
        return this;
    }

}
