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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.SocketException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Main.class, TestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class EventContentTest {

    private static final String START_URL = "http://0.0.0.0:5000/simulator/start";
    private static final int VERIFICATION_TIMEOUT_MILLIS = 10000;

    @Autowired
    private VesSimulatorService vesSimulatorService;

    private String vesUrl;
    private String marker;

    @Before
    public void setUp() throws SocketException {
        vesUrl = "https://" + TestUtils.getCurrentIpAddress() + ":9443/ves-simulator/eventListener/v5";
        marker = UUID.randomUUID().toString();
    }

    @After
    public void tearDown() {
        Mockito.reset(vesSimulatorService);
    }

    @Test
    public void whenPatchIsGivenItShouldBeMergedIntoTemplate() {
        startJob("notification.json",
            "{\"event\": {\"commonEventHeader\": {\"sourceName\": \"" + marker + "\", \"addedField\": \"added\"}}}", 1);

        JsonObject header = receiveEvents("sourceName", 1).get(0)
            .getAsJsonObject("event").getAsJsonObject(TestUtils.COMMON_EVENT_HEADER);
        assertThat(header.get("addedField").getAsString()).isEqualTo("added");
        assertThat(header.get("domain").getAsString()).isEqualTo("notification");
        assertThat(header.get("eventName").getAsString()).isEqualTo("vFirewallBroadcastPackets");
    }

    @Test
    public void whenJobRepeatsKeywordsShouldBeEvaluatedForEveryEvent() {
        startJob("notification.json",
            "{\"event\": {\"commonEventHeader\": {\"sourceName\": \"" + marker + "\","
                + "\"eventId\": \"#RandomString(20)\", \"sequence\": \"#Increment\"}}}", 3);

        List<JsonObject> events = receiveEvents("sourceName", 3);
        assertThat(events).extracting(event -> TestUtils.getHeaderField(event, "sequence"))
            .containsExactlyInAnyOrder("1", "2", "3");
        assertThat(events).extracting(event -> TestUtils.getHeaderField(event, "eventId"))
            .doesNotHaveDuplicates()
            .allSatisfy(eventId -> assertThat(eventId).hasSize(20));
    }

    @Test
    public void whenMeasurementTemplateIsSentItsKeywordsShouldBeReplaced() {
        long before = Instant.now().getEpochSecond();
        startJob("measurement.json",
            "{\"event\": {\"commonEventHeader\": {\"reportingEntityName\": \"" + marker + "\"}}}", 1);

        JsonObject event = receiveEvents("reportingEntityName", 1).get(0).getAsJsonObject("event");
        JsonObject header = event.getAsJsonObject(TestUtils.COMMON_EVENT_HEADER);
        assertThat(header.get("startEpochMicrosec").getAsLong()).isBetween(before, Instant.now().getEpochSecond());
        JsonObject fields = event.getAsJsonObject("measurementsForVfScalingFields");
        assertThat(fields.get("requestRate").getAsInt()).isBetween(50, 100);
        assertThat(fields.get("meanRequestLatency").getAsInt()).isBetween(1, 1000);
        for (JsonElement cpuUsage : fields.getAsJsonArray("cpuUsageArray")) {
            assertThat(cpuUsage.getAsJsonObject().get("percentUsage").getAsInt()).isBetween(1, 100);
        }
    }

    private void startJob(String templateName, String patch, int repeatCount) {
        String body = "{\"templateName\": \"" + templateName + "\", \"patch\": " + patch + ","
            + "\"simulatorParams\": {\"vesServerUrl\": \"" + vesUrl + "\", \"repeatInterval\": 1, "
            + "\"repeatCount\": " + repeatCount + "}}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
            .post(START_URL)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private List<JsonObject> receiveEvents(String markerField, int expectedCount) {
        ArgumentCaptor<JsonObject> eventCaptor = ArgumentCaptor.forClass(JsonObject.class);
        Mockito.verify(vesSimulatorService, Mockito.timeout(VERIFICATION_TIMEOUT_MILLIS).atLeast(expectedCount))
            .sendEventToDmaapV5(argThat(event -> marker.equals(TestUtils.getHeaderField(event, markerField))));
        Mockito.verify(vesSimulatorService, Mockito.atLeast(expectedCount)).sendEventToDmaapV5(eventCaptor.capture());
        return eventCaptor.getAllValues().stream()
            .filter(event -> marker.equals(TestUtils.getHeaderField(event, markerField)))
            .collect(Collectors.toList());
    }
}
