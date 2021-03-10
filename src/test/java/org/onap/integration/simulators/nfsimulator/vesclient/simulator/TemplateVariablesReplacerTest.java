/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.integration.simulators.nfsimulator.vesclient.simulator;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.skyscreamer.jsonassert.JSONAssert;

@TestInstance(Lifecycle.PER_CLASS)
class TemplateVariablesReplacerTest {

    private static final Gson GSON = new Gson();

    private TemplateVariablesReplacer replacer;

    @BeforeAll
    void setUp() {
        replacer = new TemplateVariablesReplacer();
    }

    @Test
    void shouldReplaceStringVariable() {
        val sourceAsString = "{\"test1\":\"#variable1\", \"variable1\":\"value2 #variable1\"}";
        val expectedAsString = "{\"test1\":\"valueOfVariable1\", \"variable1\":\"value2 #variable1\"}";
        val variablesAsString = "{\"variable1\":\"valueOfVariable1\"}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceStringAndNumberVariable() {
        val sourceAsString = "{\"test1\":\"#variable1\", \"test2\":\"#variable2\"}";
        val expectedAsString = "{\"test1\":\"valueOfVariable1=1\", \"test2\":2}";
        val variablesAsString = "{\"variable1\":\"valueOfVariable1=1\", \"variable2\":2}";

        val source = new Gson().fromJson(sourceAsString, JsonObject.class);
        val variables = new Gson().fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceSimpleStringVariable() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable1\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\":\"valueOfVariable1\"}";
        val variablesAsString = "{\"variable1\":\"valueOfVariable1\"}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceObjectVariable() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable1\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\":{\"replaced1\":\"valueOfVariable1\"}}";
        val variablesAsString = "{\"variable1\":{\"replaced1\":\"valueOfVariable1\"}}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceIntegerVariable() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable1\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\": 1}";
        val variablesAsString = "{\"variable1\": 1}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceBoolVariable() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable1\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\": true}";
        val variablesAsString = "{\"variable1\": true}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceDifferentVariables() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable\",  \"variable2\":\"text #variable\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\":{\"replaced1\":\"valueOfVariable1\"},  \"variable2\":\"text #variable\"}";
        val variablesAsString = "{\"variable\":{\"replaced1\":\"valueOfVariable1\"}}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceArrayVariables() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable1\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\":[1,2,3]}";
        val variablesAsString = "{\"variable1\":[1,2,3]}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceArrayWithStringVariables() {
        val sourceAsString = "{\"test1\":\"value1\", \"variable1\":\"#variable1\"}";
        val expectedAsString = "{\"test1\":\"value1\", \"variable1\":[\"1\",\"2\",\"3\"]}";
        val variablesAsString = "{\"variable1\":[\"1\",\"2\",\"3\"]}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

    @Test
    void shouldReplaceArrayAsStringVariables() {
        val sourceAsString = "{\"test1\":\"#variable1\", \"variable1\":\"Text #variable1\"}";
        val expectedAsString = "{\"test1\":[1,2,3], \"variable1\": \"Text #variable1\"}";
        val variablesAsString = "{\"variable1\":[1,2,3]}";

        val source = GSON.fromJson(sourceAsString, JsonObject.class);
        val variables = GSON.fromJson(variablesAsString, JsonObject.class);

        JsonObject result = replacer.substituteVariables(source, variables);
        JSONAssert.assertEquals(expectedAsString, result.toString(), true);
    }

}
