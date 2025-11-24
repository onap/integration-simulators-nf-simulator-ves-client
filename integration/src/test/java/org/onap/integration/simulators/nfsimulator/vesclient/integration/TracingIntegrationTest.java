/*-
 * ============LICENSE_START=======================================================
 * Simulator
 * ================================================================================
 * Copyright (C) 2025 Deutsche Telekom. All rights reserved.
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Main.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 9411)
@TestPropertySource(properties = {
    "spring.zipkin.base-url=http://localhost:${wiremock.server.port}",
    "spring.sleuth.enabled=true",
    "spring.sleuth.sampler.probability=1.0",
})
public class TracingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldExportTraceWhenMakingRequest() {
        WireMock.stubFor(post(urlEqualTo("/api/v2/spans"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.ACCEPTED.value())));

        ResponseEntity<String> response = restTemplate.getForEntity("/simulator/status", String.class);

        await()
            .atMost(20, TimeUnit.SECONDS)
            .untilAsserted(() -> WireMock.verify(postRequestedFor(urlEqualTo(ZIPKIN_SPANS_PATH))));
    }
}
