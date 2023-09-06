/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.stackdrive.sonarqube;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.stackdrive.sonarqube.gson.RuntimeTypeAdapterFactory;
import org.stackdrive.report.model.tabbed.TabPane;
import org.stackdrive.report.model.views.CommentList;
import org.stackdrive.report.model.views.PlantUmlChart;
import org.stackdrive.report.model.views.ViewElement;
import org.stackdrive.report.model.views.table.DataTable;
import org.stackdrive.report.model.views.tagcloud.TagCloud;

import java.lang.reflect.Type;

public class TabPaneDeserializer implements JsonDeserializer<TabPane<?>> {

    private final Gson gson;

    private final TypeToken<ViewElement> tabPaneTypeToken;

    public TabPaneDeserializer() {
        final RuntimeTypeAdapterFactory<ViewElement> typeFactory = RuntimeTypeAdapterFactory
                .of(ViewElement.class, "type") // Here you specify which is the parent class and what field particularizes the child class.
                .registerSubtype(DataTable.class, "DataTable") // if the flag equals the class name, you can skip the second parameter. This is only necessary, when the "type" field does not equal the class name.
                .registerSubtype(PlantUmlChart.class, "PlantUmlChart")
                .registerSubtype(TagCloud.class, "TagCloud")
                .registerSubtype(CommentList.class, "CommentList");

        this.gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory).create();
        this.tabPaneTypeToken = new TypeToken<ViewElement>() {
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public TabPane deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject tabPaneObject = json.getAsJsonObject();

        final String title = tabPaneObject.getAsJsonPrimitive("title").getAsString();
        final int key = tabPaneObject.getAsJsonPrimitive("key").getAsInt();
        final ViewElement viewElement = gson.fromJson(tabPaneObject.getAsJsonObject("element"), tabPaneTypeToken.getType());

        return new TabPane(title, key, viewElement);
    }
}