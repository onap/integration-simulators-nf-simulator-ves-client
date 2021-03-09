/*-
 * ============LICENSE_START=======================================================
 * Simulator
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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

package org.onap.pnfsimulator.integration;

import com.google.gson.JsonObject;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.UnknownHostException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.onap.pnfsimulator.integration.TestUtils.PATCHED;
import static org.onap.pnfsimulator.integration.TestUtils.SINGLE_EVENT_URL;
import static org.onap.pnfsimulator.integration.TestUtils.findSourceNameInMongoDB;
import static org.onap.pnfsimulator.integration.TestUtils.getCurrentIpAddress;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Main.class, TestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SingleEventTest {

    @Autowired
    private VesSimulatorService vesSimulatorService;

    private String currentVesSimulatorIp;

    @Before
    public void setUp() throws Exception {
        currentVesSimulatorIp = getCurrentIpAddress();
    }

    @After
    public void tearDown() {
        Mockito.reset(vesSimulatorService);
    }

    @Test
    public void whenTriggeredSimulatorWithWrongVesAddressInformationShouldBeReturned() {
        //given
        String body = "{\n"
            + "\"vesServerUrl\": \"https://" + currentVesSimulatorIp + ":8080/ves-simulator/eventListener/v5\",\n"
            + "\"event\": { \n"
            + "\"commonEventHeader\": {\n"
            + "\"sourceName\": \"HistoricalEvent\",\n"
            + "\"version\": 3"
            + "}\n"
            + "}\n"
            + "}";

        //when
        given()
            .contentType("application/json")
            .body(body)
            .when()
            .post(SINGLE_EVENT_URL)
            .then()
            .statusCode(421)
            .body("message",
                equalTo(
                    "Fail to connect with ves: Connect to "+currentVesSimulatorIp+":8080 " +
                        "[/"+currentVesSimulatorIp+"] " +
                        "failed: Connection refused (Connection refused)"));
    }

    @Test
    public void whenTriggeredSimulatorWithWrongEventShouldReturnedError() {
        //given
        String body = "{\n"
            + "\"vesServerUrl\": \"https://" + currentVesSimulatorIp + ":9443/ves-simulator/eventListener/v5\",\n"
            + "\"event\": { \n"
            + "this is not JSON {}"
            + "}\n"
            + "}";

        //when
        given()
            .contentType("application/json")
            .body(body)
            .when()
            .post(SINGLE_EVENT_URL)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message",
                stringContainsInOrder(List.of("JSON parse error:","Unexpected character ('t' (code 116)):"))
            );
    }

    @Test
    public void whenTriggeredSimulatorWithUsernameAndPasswordInUrlVesShouldAcceptRequest() throws UnknownHostException {
        //given
        String body = "{\n"
            + "\"vesServerUrl\": \"https://user1:pass1@" + currentVesSimulatorIp + ":9443/ves-simulator/eventListener/v5\",\n"
            + "\"event\": { \n"
            + "\"commonEventHeader\": {\n"
            + "\"sourceName\": \"HistoricalEvent\",\n"
            + "\"version\": 3"
            + "}\n"
            + "}\n"
            + "}";
        ArgumentCaptor<JsonObject> parameterCaptor = ArgumentCaptor.forClass(JsonObject.class);

        //when
        given()
            .contentType("application/json")
            .body(body)
            .when()
            .post(SINGLE_EVENT_URL)
            .then()
            .statusCode(HttpStatus.ACCEPTED.value())
            .body("message", equalTo("Accepted"));

        //then
        Mockito.verify(vesSimulatorService,
            Mockito.timeout(3000))
            .sendEventToDmaapV5(parameterCaptor.capture());

        Document sourceNameInMongoDB = findSourceNameInMongoDB();
        Assertions.assertThat(sourceNameInMongoDB.get(PATCHED))
            .isEqualTo("{\"commonEventHeader\":{\"sourceName\":\"HistoricalEvent\",\"version\":3}}");
    }
}
