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

import io.restassured.specification.RequestSpecification;
import java.net.SocketException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Main.class, TestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TracingTest {

    private static final String TRACE_ID = "463ac35c9f6413ad48485a3953bb6124";
    private static final String SPAN_ID = "a2fb4a1d1a96d312";
    private static final String TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String SPAN_ID_HEADER = "X-B3-SpanId";
    private static final int VERIFICATION_TIMEOUT_MILLIS = 10000;

    @Autowired
    private VesSimulatorService vesSimulatorService;

    private String vesUrl;

    @Before
    public void setUp() throws SocketException {
        vesUrl = "https://" + TestUtils.getCurrentIpAddress() + ":9443/ves-simulator/eventListener/v5";
    }

    @After
    public void tearDown() {
        Mockito.reset(vesSimulatorService);
    }

    @Test
    public void whenOneTimeEventIsSentTraceContextShouldBePropagatedToVes() {
        String body = "{\"vesServerUrl\": \"" + vesUrl + "\","
            + "\"event\": {\"commonEventHeader\": {\"sourceName\": \"TracedEvent\"}}}";

        givenTracedRequest()
            .body(body)
            .when()
            .post(TestUtils.SINGLE_EVENT_URL)
            .then()
            .statusCode(HttpStatus.ACCEPTED.value());

        assertThatVesReceivedEventInTrace();
    }

    @Test
    public void whenPeriodicEventIsSentTraceContextShouldBePropagatedToVes() {
        String body = "{\"templateName\": \"notification.json\","
            + "\"simulatorParams\": {\"vesServerUrl\": \"" + vesUrl + "\", \"repeatInterval\": 1, \"repeatCount\": 1}}";

        givenTracedRequest()
            .body(body)
            .when()
            .post("http://0.0.0.0:5000/simulator/start")
            .then()
            .statusCode(HttpStatus.OK.value());

        assertThatVesReceivedEventInTrace();
    }

    private RequestSpecification givenTracedRequest() {
        return given()
            .contentType("application/json")
            .header(TRACE_ID_HEADER, TRACE_ID)
            .header(SPAN_ID_HEADER, SPAN_ID)
            .header("X-B3-Sampled", "1");
    }

    private void assertThatVesReceivedEventInTrace() {
        ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
        Mockito.verify(vesSimulatorService, Mockito.timeout(VERIFICATION_TIMEOUT_MILLIS).atLeastOnce())
            .receiveHeaders(headersCaptor.capture());
        assertThat(headersCaptor.getAllValues()).anySatisfy(headers -> {
            assertThat(headers.getFirst(TRACE_ID_HEADER)).isEqualTo(TRACE_ID);
            assertThat(headers.getFirst(SPAN_ID_HEADER)).isNotNull().isNotEqualTo(SPAN_ID);
        });
    }
}
