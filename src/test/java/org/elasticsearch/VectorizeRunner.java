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

package org.elasticsearch;

import org.elasticsearch.bootstrap.ElasticsearchF;
import org.elasticsearch.plugin.fetchtermvectors.FetchTermVectorsPlugin;
import org.elasticsearch.plugin.vectorize.VectorizePlugin;

/**
 * 
 */
public class VectorizeRunner {

    public static void main(String[] args) throws Throwable {
        System.setProperty("es.http.cors.enabled", "true");
        System.setProperty("es.script.inline", "on");
        System.setProperty("es.shield.enabled", "false");
        System.setProperty("es.security.manager.enabled", "false");
        System.setProperty("es.plugins.load_classpath_plugins", "false");
        System.setProperty("es.plugin.types", VectorizePlugin.class.getName() + "," + FetchTermVectorsPlugin.class.getName());
        System.setProperty("es.cluster.name", VectorizeRunner.class.getSimpleName());
        
        ElasticsearchF.main(new String[]{"start"});
    }
}
