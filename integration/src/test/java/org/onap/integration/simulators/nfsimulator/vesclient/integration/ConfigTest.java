/*-
 * ============LICENSE_START=======================================================
 * Simulator
 * ================================================================================
 * Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.argThat;

import io.restassured.response.Response;
import java.net.SocketException;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Main.class, TestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ConfigTest {

    private static final String CONFIG_URL = "http://0.0.0.0:5000/simulator/config";
    private static final String VES_SERVER_URL_PATH = "simulatorConfig.vesServerUrl";

    @Autowired
    private VesSimulatorService vesSimulatorService;

    private String vesUrl;
    private String originalVesUrl;

    @Before
    public void setUp() throws SocketException {
        vesUrl = "https://" + TestUtils.getCurrentIpAddress() + ":9443/ves-simulator/eventListener/v5";
        originalVesUrl = when().get(CONFIG_URL).then().statusCode(HttpStatus.OK.value()).extract().path(VES_SERVER_URL_PATH);
    }

    @After
    public void tearDown() {
        updateVesUrl(originalVesUrl);
        Mockito.reset(vesSimulatorService);
    }

    @Test
    public void whenVesUrlIsUpdatedItShouldBeReturnedByConfig() {
        updateVesUrl(vesUrl)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body(VES_SERVER_URL_PATH, equalTo(vesUrl));

        when()
            .get(CONFIG_URL)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body(VES_SERVER_URL_PATH, equalTo(vesUrl));
    }

    @Test
    public void whenRequestHasNoVesUrlConfiguredVesUrlShouldBeUsed() {
        updateVesUrl(vesUrl).then().statusCode(HttpStatus.OK.value());
        String sourceName = UUID.randomUUID().toString();

        given()
            .contentType("application/json")
            .body("{\"templateName\": \"notification.json\","
                + "\"patch\": {\"event\": {\"commonEventHeader\": {\"sourceName\": \"" + sourceName + "\"}}},"
                + "\"simulatorParams\": {\"repeatInterval\": 1, \"repeatCount\": 1}}")
            .when()
            .post("http://0.0.0.0:5000/simulator/start")
            .then()
            .statusCode(HttpStatus.OK.value());

        Mockito.verify(vesSimulatorService, Mockito.timeout(10000))
            .sendEventToDmaapV5(argThat(event -> sourceName.equals(TestUtils.getSourceName(event))));
    }

    @Test
    public void whenVesUrlIsMalformedConfigShouldBeRejected() {
        updateVesUrl("not a url")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        when()
            .get(CONFIG_URL)
            .then()
            .body(VES_SERVER_URL_PATH, equalTo(originalVesUrl));
    }

    private Response updateVesUrl(String url) {
        return given()
            .contentType("application/json")
            .body("{\"vesServerUrl\": \"" + url + "\"}")
            .when()
            .put(CONFIG_URL);
    }
}
