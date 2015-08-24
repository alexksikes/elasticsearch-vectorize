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

package org.elasticsearch.plugin.vectorize;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.vectorize.TransportVectorizeAction;
import org.elasticsearch.action.vectorize.VectorizeAction;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.vectorize.RestSearchVectorizeAction;
import org.elasticsearch.rest.action.vectorize.RestSearchVectorizeScrollAction;
import org.elasticsearch.rest.action.vectorize.RestVectorizeAction;

public class VectorizePlugin extends Plugin {

    public static final String NAME = "vectorize";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Elasticsearch Vectorize Plugin";
    }

    public void onModule(ActionModule actionModule) {
        actionModule.registerAction(VectorizeAction.INSTANCE, TransportVectorizeAction.class);
    }

    public void onModule(RestModule restModule) {
        restModule.addRestAction(RestVectorizeAction.class);
        restModule.addRestAction(RestSearchVectorizeAction.class);
        restModule.addRestAction(RestSearchVectorizeScrollAction.class);
    }
}
