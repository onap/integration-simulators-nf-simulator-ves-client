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
package org.onap.pnfsimulator.simulator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map.Entry;

import lombok.val;
import org.springframework.stereotype.Component;

@Component
public class TemplateVariablesReplacer {
    private static final Gson GSON = new Gson();
    private static final String OBJECT_KEYWORD_MARK = "#";
    private static final String ESCAPED_QUOTE = "\"";
    private static final String STRING_KEYWORD_MARK = ESCAPED_QUOTE + OBJECT_KEYWORD_MARK + "%s" + ESCAPED_QUOTE;

    JsonObject substituteVariables(JsonObject source, JsonObject variables) {
        var result = source.toString();
        for (val variable : variables.entrySet()) {
            result = substituteVariable(result, variable);
        }
        return GSON.fromJson(result, JsonObject.class);
    }

    private String substituteVariable(String sourceAsString, Entry<String, JsonElement> variable) {
        return sourceAsString.replaceAll(String.format(STRING_KEYWORD_MARK, variable.getKey()), variable.getValue().toString());
    }

}
