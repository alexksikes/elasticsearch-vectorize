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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.DocumentRequest;
import org.elasticsearch.action.support.single.shard.SingleShardRequest;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.vectorize.Vectorizer;

import java.io.IOException;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * 
 */
public class VectorizeRequest extends SingleShardRequest<VectorizeRequest> implements DocumentRequest<VectorizeRequest> {

    private TermVectorsRequest termVectorsRequest;  // embedded term vectors request

    private Vectorizer vectorizer;

    private Format format = Format.DICT;

    long startTime;

    public enum Format {
        DICT, COO
    }
    
    VectorizeRequest() {
    }

    public VectorizeRequest(String index, String type, String id) {
        super(index);
        this.termVectorsRequest = new TermVectorsRequest(index, type, id).termStatistics(true);
    }

    @Override
    public String type() {
        return termVectorsRequest.type();
    }

    public VectorizeRequest type(String type) {
        this.termVectorsRequest.type(type);
        return this;
    }

    @Override
    public String id() {
        return termVectorsRequest.id();
    }

    public VectorizeRequest id(String id) {
        this.termVectorsRequest.id(id);
        return this;
    }

    public TermVectorsRequest getTermVectorsRequest() {
        return this.termVectorsRequest;
    }

    public Vectorizer vectorizer() {
        return this.vectorizer;
    }

    public VectorizeRequest vectorizer(Vectorizer vectorizer) {
        this.vectorizer = vectorizer;
        this.termVectorsRequest.selectedFields(vectorizer.getFields());
        return this;
    }

    public Format format() {
        return this.format;
    }

    public VectorizeRequest format(Format format) {
        this.format = format;
        return this;
    }

    public VectorizeRequest format(String format) {
        this.format = Format.valueOf(format.toUpperCase());
        return this;
    }

    @Override
    public String routing() {
        return this.termVectorsRequest.routing();
    }

    @Override
    public VectorizeRequest routing(String routing) {
        this.termVectorsRequest.routing(routing);
        return this;
    }

    public boolean realtime() {
        return this.termVectorsRequest.realtime();
    }

    public VectorizeRequest realtime(Boolean realtime) {
        this.termVectorsRequest.realtime(realtime);
        return this;
    }

    public long version() {
        return this.termVectorsRequest.version();
    }

    public VectorizeRequest version(long version) {
        this.termVectorsRequest.version(version);
        return this;
    }

    public VersionType versionType() {
        return this.termVectorsRequest.versionType();
    }

    public VectorizeRequest versionType(VersionType versionType) {
        this.termVectorsRequest.versionType(versionType);
        return this;
    }

    public VectorizeRequest parent(String parent) {
        this.termVectorsRequest.parent(parent);
        return this;
    }

    public String preference() {
        return this.termVectorsRequest.preference();
    }

    public VectorizeRequest preference(String preference) {
        this.termVectorsRequest.preference(preference);
        return this;
    }

    public boolean dfs() {
        return this.termVectorsRequest.dfs();
    }

    public VectorizeRequest dfs(boolean dfs) {
        this.termVectorsRequest.dfs(dfs);
        return this;
    }

    public String[] fields() {
        return this.vectorizer.getFields();
    }

    public String[] numericalFields() {
        return this.vectorizer.getNumericalFields();
    }

    public static void parseRequest(VectorizeRequest vectorizeRequest, XContentParser parser) throws IOException {
        XContentParser.Token token;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (currentFieldName != null) {
                if (currentFieldName.equals("vectorizer")) {
                    vectorizeRequest.vectorizer(Vectorizer.parse(parser));
                } else {
                    throw new ElasticsearchParseException("The parameter ["+currentFieldName+"] is not a valid " +
                            "parameter of a vectorize request!");
                }
            }
        }
    }

    public long startTime() {
        return this.startTime;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = termVectorsRequest.validate();
        if (vectorizer == null) {
            validationException = addValidationError("no vectorizer has been specified", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        this.termVectorsRequest.readFrom(in);
        this.vectorizer.readFrom(in);
        this.format(in.readString());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        this.termVectorsRequest.writeTo(out);
        this.vectorizer.writeTo(out);
        out.writeString(format.name());
    }
}
