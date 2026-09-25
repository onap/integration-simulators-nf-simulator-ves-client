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
public class CancelEventsTest {

    private static final String SIMULATOR_URL = "http://0.0.0.0:5000/simulator/";
    private static final int FIRST_EVENT_TIMEOUT_MILLIS = 10000;
    private static final int NO_MORE_EVENTS_WINDOW_MILLIS = 3000;

    @Autowired
    private VesSimulatorService vesSimulatorService;

    private String vesUrl;
    private String sourceName;

    @Before
    public void setUp() throws SocketException {
        vesUrl = "https://" + TestUtils.getCurrentIpAddress() + ":9443/ves-simulator/eventListener/v5";
        sourceName = UUID.randomUUID().toString();
    }

    @After
    public void tearDown() {
        when().post(SIMULATOR_URL + "cancel");
        Mockito.reset(vesSimulatorService);
    }

    @Test
    public void whenRunningJobIsCancelledNoMoreEventsShouldBeSent() {
        String jobName = startLongRunningJob();
        waitForFirstEvent();

        when()
            .post(SIMULATOR_URL + "cancel/" + jobName)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("message", equalTo("Event(s) was cancelled"));

        assertNoMoreEventsAreSent();
    }

    @Test
    public void whenAllJobsAreCancelledNoMoreEventsShouldBeSent() {
        startLongRunningJob();
        waitForFirstEvent();

        when()
            .post(SIMULATOR_URL + "cancel")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("message", equalTo("Event(s) was cancelled"));

        assertNoMoreEventsAreSent();
    }

    @Test
    public void whenNonexistentJobIsCancelledNotFoundShouldBeReturned() {
        when()
            .post(SIMULATOR_URL + "cancel/nonexistent-job")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", equalTo("Simulator was not able to cancel event(s)"));
    }

    private String startLongRunningJob() {
        String body = "{\"templateName\": \"notification.json\","
            + "\"patch\": {\"event\": {\"commonEventHeader\": {\"sourceName\": \"" + sourceName + "\"}}},"
            + "\"simulatorParams\": {\"vesServerUrl\": \"" + vesUrl + "\", \"repeatInterval\": 1, \"repeatCount\": 100}}";
        return given()
            .contentType("application/json")
            .body(body)
            .when()
            .post(SIMULATOR_URL + "start")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .path("jobName");
    }

    private void waitForFirstEvent() {
        Mockito.verify(vesSimulatorService, Mockito.timeout(FIRST_EVENT_TIMEOUT_MILLIS).atLeastOnce())
            .sendEventToDmaapV5(argThat(event -> sourceName.equals(TestUtils.getSourceName(event))));
    }

    private void assertNoMoreEventsAreSent() {
        Mockito.clearInvocations(vesSimulatorService);
        Mockito.verify(vesSimulatorService, Mockito.after(NO_MORE_EVENTS_WINDOW_MILLIS).never())
            .sendEventToDmaapV5(argThat(event -> sourceName.equals(TestUtils.getSourceName(event))));
    }
}
