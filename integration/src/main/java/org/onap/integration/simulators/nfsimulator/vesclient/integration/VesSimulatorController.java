/*-
 * ============LICENSE_START=======================================================
 * Simulator
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

package org.onap.integration.simulators.nfsimulator.vesclient.integration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("ves-simulator")
@RestController
public class VesSimulatorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VesSimulatorController.class);
    private final VesSimulatorService vesSimulatorService;
    private final Gson gson;
    private final ResponseEntity<String> response = ResponseEntity
        .status(HttpStatus.ACCEPTED)
        .body("Accepted");

    @Autowired
    public VesSimulatorController(VesSimulatorService vesSimulatorService, Gson gson) {
        this.vesSimulatorService = vesSimulatorService;
        this.gson = gson;
    }

    @PostMapping("eventListener/v5")
    public ResponseEntity<String> sendEventToDmaapV5(@RequestBody String body) {
        JsonObject jsonObject = getJsonObjectFromBody(body);
        vesSimulatorService.sendEventToDmaapV5(jsonObject);
        return response;
    }

    @PostMapping("eventListener/v7")
    public ResponseEntity<String> sendEventToDmaapV7(@RequestBody String body) {
        JsonObject jsonObject = getJsonObjectFromBody(body);
        vesSimulatorService.sendEventToDmaapV7(jsonObject);
        return response;
    }

    private JsonObject getJsonObjectFromBody(@RequestBody String body) {
        LOGGER.info(String.format("Received event: %s", body));
        return gson.fromJson(body, JsonObject.class);
    }
}
