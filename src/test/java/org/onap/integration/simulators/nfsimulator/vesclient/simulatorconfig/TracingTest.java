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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.integration.simulators.nfsimulator.vesclient.Main;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.SimulatorService;
import org.onap.integration.simulators.nfsimulator.vesclient.template.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// @DirtiesContext is needed because the SimulatorService mock bean
// would replace the real bean beyond the test context of this class otherwise
@DirtiesContext
@SpringBootTest(classes = {Main.class})
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
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
    private MockMvc mockMvc;

    @Test
    public void shouldExportTraceWhenMakingRequest() throws Exception {
        Mockito.when(simulatorService.getConfiguration()).thenReturn(new SimulatorConfig());

        WireMock.stubFor(post(urlEqualTo("/api/v2/spans"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK.value())));

        mockMvc.perform(get("/simulator/config"))
            .andExpect(status().isOk());

        Thread.sleep(2000);
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v2/spans")));
    }
}
