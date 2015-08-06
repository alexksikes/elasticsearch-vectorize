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

package org.elasticsearch.vectorize;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.TermStatistics;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.action.vectorize.VectorizeRequest;
import org.elasticsearch.action.vectorize.VectorizeResponse;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.IndexShard;

import java.io.IOException;
import java.util.Map;

/**
 */
public class VectorizeService extends AbstractIndexShardComponent {

    private IndexShard indexShard;

    // Unfortunately it does not seem possible to bind shard services in a plugin
    public VectorizeService(IndexShard indexShard) {
        super(indexShard.shardId(), indexShard.indexSettings());
        this.indexShard = indexShard;
    }

    public VectorizeResponse getVector(VectorizeRequest request) {
        final VectorizeResponse response = new VectorizeResponse(request.index(), request.type(), request.id());
        final Vectorizer vectorizer = request.vectorizer();

        // first fetch the term vectors
        TermVectorsResponse termVectorsResponse = null;
        if (request.fields() != null) {
            termVectorsResponse = getTermVectors(request);
        }
        if (termVectorsResponse != null && termVectorsResponse.isExists()) {
            try {
                processTermVectorsFields(vectorizer, termVectorsResponse.getFields());
            } catch (IOException e) {
                return response;  // we failed return an empty response for now
            }
        }

        // now take care of the numerical fields
        GetResult getResult = null;
        if (request.numericalFields() != null) {
            getResult = getGetResult(request);
        }
        if (getResult != null && getResult.isExists()) {
            processGetResult(vectorizer, getResult.getFields());
        }

        // now write the obtained vector
        try {
            response.setVector(vectorizer.writeVector());
        } catch (IOException e) {
            return response;
        }

        // finally return the response and set the format
        response.setExists(exists(termVectorsResponse, getResult));
        response.setFormat(request.format());
        return response;
    }

    private boolean exists(TermVectorsResponse termVectorsResponse, GetResult getResult) {
        return ((termVectorsResponse != null && termVectorsResponse.isExists()) ||
                (getResult != null && getResult.isExists()));
    }

    private TermVectorsResponse getTermVectors(VectorizeRequest request) {
        // TODO: there is no need to actually to embed a term vector request
        return indexShard.termVectorsService().getTermVectors(request.getTermVectorsRequest(), indexShard.shardId().getIndex());
    }

    private GetResult getGetResult(VectorizeRequest request) {
        return indexShard.getService().get(request.type(), request.id(), request.numericalFields(), request.realtime(),
                request.version(), request.versionType(), null, true);
    }

    private void processTermVectorsFields(Vectorizer vectorizer, Fields termVectorsFields) throws IOException {
        for (String fieldName : termVectorsFields) {
            TermsEnum termsEnum = termVectorsFields.terms(fieldName).iterator();
            while (termsEnum.next() != null) {
                Term term = new Term(fieldName, termsEnum.term());
                TermStatistics termStatistics = new TermStatistics(termsEnum.term(), termsEnum.docFreq(), termsEnum.totalTermFreq());
                int freq = termsEnum.postings(null, null, PostingsEnum.ALL).freq();
                vectorizer.add(term, termStatistics, freq);
            }
        }
    }

    private void processGetResult(Vectorizer vectorizer, Map<String, GetField> getResult) {
        for (GetField getField : getResult.values()) {
            vectorizer.add(getField.getName(), getField.getValues());
        }
    }
}
