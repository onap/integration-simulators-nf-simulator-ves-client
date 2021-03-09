/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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

package org.onap.pnfsimulator.simulator.client;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpApacheResponseAdapterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpApacheResponseAdapterFactory.class);

    public HttpResponseAdapter create(HttpResponse response) {
        String message;
        try {
            message = EntityUtils.toString(response.getEntity());
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.warn("Response from VES was empty");
            message = "";
        }
        return new HttpResponseAdapter(response.getStatusLine().getStatusCode(), message);
    }

}
