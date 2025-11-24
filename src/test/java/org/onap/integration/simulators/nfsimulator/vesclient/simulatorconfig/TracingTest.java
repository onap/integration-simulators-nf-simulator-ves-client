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

package org.onap.integration.simulators.nfsimulator.vesclient.simulatorconfig;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.integration.simulators.nfsimulator.vesclient.Main;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.SimulatorService;
import org.onap.integration.simulators.nfsimulator.vesclient.template.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

// @DirtiesContext is needed because the SimulatorService mock bean
// would replace the real bean beyond the test context of this class otherwise
@DirtiesContext
@SpringBootTest(classes = {Main.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 9411)
@TestPropertySource(properties = {
    "spring.zipkin.base-url=http://localhost:${wiremock.server.port}",
    "spring.sleuth.enabled=true",
    "spring.sleuth.sampler.probability=1.0",
})
public class TracingTest {

    @MockBean
    private TemplateRepository templateRepository;

    @MockBean
    private SimulatorService simulatorService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldExportTraceWhenMakingRequest() throws InterruptedException {
        Mockito.when(simulatorService.getConfiguration()).thenReturn(new SimulatorConfig());

        WireMock.stubFor(post(urlEqualTo("/api/v2/spans"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.ACCEPTED.value())));

        restTemplate.getForEntity("/simulator/config", String.class);

        Thread.sleep(2000);
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v2/spans")));
    }
}
