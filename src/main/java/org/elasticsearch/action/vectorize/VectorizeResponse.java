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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.vectorize.Vectorizer;

import java.io.IOException;

public class VectorizeResponse extends ActionResponse implements ToXContent {

    private static class FieldStrings {
        public static final XContentBuilderString _INDEX = new XContentBuilderString("_index");
        public static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        public static final XContentBuilderString _ID = new XContentBuilderString("_id");
        public static final XContentBuilderString _VERSION = new XContentBuilderString("_version");
        public static final XContentBuilderString FOUND = new XContentBuilderString("found");
        public static final XContentBuilderString TOOK = new XContentBuilderString("took");
    }

    private String index;
    private String type;
    private String id;
    private long docVersion;
    private boolean exists = false;
    private long tookInMillis;
    private BytesReference vector;

    public VectorizeResponse() {
    }

    public VectorizeResponse(String index, String type, String id) {
        this.index = index;
        this.type = type;
        this.id = id;
    }

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Long getVersion() {
        return docVersion;
    }

    public void setDocVersion(long version) {
        this.docVersion = version;

    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public void updateTookInMillis(long startTime) {
        this.tookInMillis = Math.max(1, System.currentTimeMillis() - startTime);
    }

    public Vectorizer.SparseVector getVector() throws IOException {
        return Vectorizer.readVector(vector);
    }

    public void setVector(BytesReference output) {
        vector = output;
    }

    public TimeValue getTook() {
        return new TimeValue(tookInMillis);
    }

    public long getTookInMillis() {
        return tookInMillis;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        assert index != null;
        assert type != null;
        assert id != null;
        builder.field(FieldStrings._INDEX, index);
        builder.field(FieldStrings._TYPE, type);
        builder.field(FieldStrings._ID, id);
        builder.field(FieldStrings._VERSION, docVersion);
        builder.field(FieldStrings.FOUND, isExists());
        builder.field(FieldStrings.TOOK, tookInMillis);
        if (isExists()) {
            buildVector(builder, params);
        }
        return builder;
    }

    public void buildVector(XContentBuilder builder, Params params) throws IOException {
        getVector().toXContent(builder, params);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        index = in.readString();
        type = in.readString();
        id = in.readString();
        docVersion = in.readVLong();
        exists = in.readBoolean();
        tookInMillis = in.readVLong();
        vector = in.readBytesReference();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(type);
        out.writeString(id);
        out.writeVLong(docVersion);
        out.writeBoolean(exists);
        out.writeVLong(tookInMillis);
        out.writeBytesReference(vector);
    }
}
