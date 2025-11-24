/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
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

package org.onap.integration.simulators.nfsimulator.vesclient.rest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.FullEvent;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.SimulatorParams;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.SimulatorRequest;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.SimulatorService;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpResponseAdapter;
import org.onap.integration.simulators.nfsimulator.vesclient.simulatorconfig.SimulatorConfig;
import org.onap.integration.simulators.nfsimulator.vesclient.event.EventData;
import org.onap.integration.simulators.nfsimulator.vesclient.event.EventDataService;
import org.quartz.SchedulerException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SimulatorControllerTest {

    private static final String START_ENDPOINT = "/simulator/start";
    private static final String CONFIG_ENDPOINT = "/simulator/config";
    private static final String EVENT_ENDPOINT = "/simulator/event";
    private static final String CANCEL_JOB_ENDPOINT = "/simulator/cancel/";
    private static final String ALL_EVENTS_ENDPOINT = "/simulator/all-events";
    private static final String TEST_ENDPOINT = "/simulator/test";

    private static final String JSON_MSG_EXPRESSION = "$.message";
    private static final String EVENT_WAS_CANCELLED = "Event(s) was cancelled";
    private static final String EVENT_WAS_NOT_CANCELLED = "Simulator was not able to cancel event(s)";

    private static final String NEW_URL = "http://0.0.0.0:8090/eventListener/v7";
    private static final String UPDATE_SIM_CONFIG_VALID_JSON = "{\"vesServerUrl\": \""
            + NEW_URL + "\"}";
    private static final String SAMPLE_ID = "sampleId";
    private static final Gson GSON_OBJ = new Gson();
    private static final String JOB_NAME = "testJobName";
    private static final HttpResponseAdapter TEST_HTTP_ACCEPTED_RESPONSE = new HttpResponseAdapter(202,"");
    private static String simulatorRequestBody;
    private MockMvc mockMvc;
    @InjectMocks
    private SimulatorController controller;
    @Mock
    private EventDataService eventDataService;
    @Mock
    private SimulatorService simulatorService;

    @BeforeAll
    static void beforeAll() {
        SimulatorParams simulatorParams = new SimulatorParams("http://0.0.0.0:8080", 1, 1);
        SimulatorRequest simulatorRequest = new SimulatorRequest(simulatorParams,
                "testTemplate.json", new JsonObject(), new JsonObject());

        simulatorRequestBody = GSON_OBJ.toJson(simulatorRequest);
    }

    @BeforeEach
    void setup() throws IOException, SchedulerException, GeneralSecurityException {
        MockitoAnnotations.initMocks(this);
        when(simulatorService.triggerEvent(any())).thenReturn("jobName");
        when(simulatorService.triggerOneTimeEvent(any())).thenReturn(TEST_HTTP_ACCEPTED_RESPONSE);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void shouldStartSimulatorProperly() throws Exception {
        startSimulator();
        SimulatorRequest simulatorRequest = new Gson().fromJson(simulatorRequestBody, SimulatorRequest.class);

        verify(simulatorService).triggerEvent(eq(simulatorRequest));
    }

    @Test
    void testShouldGetConfigurationWhenRequested() throws Exception {
        String newUrl = "http://localhost:8090/eventListener/v7";
        SimulatorConfig expectedConfig = new SimulatorConfig(SAMPLE_ID, new URL(newUrl));
        when(simulatorService.getConfiguration()).thenReturn(expectedConfig);

        MvcResult getResult = mockMvc
                .perform(get(CONFIG_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_SIM_CONFIG_VALID_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String expectedVesUrlJsonPart = createStringReprOfJson("vesServerUrl", newUrl);
        assertThat(getResult.getResponse().getContentAsString()).contains(expectedVesUrlJsonPart);
    }

    @Test
    void testShouldSuccessfullyUpdateConfigurationWithNewVesUrl() throws Exception {
        String oldUrl = "http://localhost:8090/eventListener/v7";
        SimulatorConfig expectedConfigBeforeUpdate = new SimulatorConfig(SAMPLE_ID, new URL(oldUrl));
        SimulatorConfig expectedConfigAfterUpdate = new SimulatorConfig(SAMPLE_ID, new URL(NEW_URL));

        when(simulatorService.getConfiguration()).thenReturn(expectedConfigBeforeUpdate);
        when(simulatorService.updateConfiguration(any(SimulatorConfig.class))).thenReturn(expectedConfigAfterUpdate);

        MvcResult postResult = mockMvc
                .perform(put(CONFIG_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_SIM_CONFIG_VALID_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String expectedVesUrlJsonPart = createStringReprOfJson("vesServerUrl", expectedConfigAfterUpdate.getVesServerUrl().toString());
        assertThat(postResult.getResponse().getContentAsString()).contains(expectedVesUrlJsonPart);
    }

    @Test
    void testShouldRaiseExceptionWhenUpdateConfigWithIncorrectPayloadWasSent() throws Exception {
        mockMvc
                .perform(put(CONFIG_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vesUrl\": \""
                                + NEW_URL + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testShouldRaiseExceptionWhenUrlInInvalidFormatIsSent() throws Exception {
        mockMvc
                .perform(put(CONFIG_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vesUrl\": \"http://0.0.0.0:VES-PORT/eventListener/v7\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testShouldSendEventDirectly() throws Exception {
        String contentAsString = mockMvc
                .perform(post(EVENT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"vesServerUrl\":\"http://0.0.0.0:8080/simulator/v7\",\n"
                                + "      \"event\":{  \n"
                                + "         \"commonEventHeader\":{  \n"
                                + "            \"domain\":\"notification\",\n"
                                + "            \"eventName\":\"vFirewallBroadcastPackets\"\n"
                                + "         },\n"
                                + "         \"notificationFields\":{  \n"
                                + "            \"arrayOfNamedHashMap\":[  \n"
                                + "               {  \n"
                                + "                  \"name\":\"A20161221.1031-1041.bin.gz\",\n"
                                + "                  \"hashMap\":{  \n"
                                + "                     \"fileformatType\":\"org.3GPP.32.435#measCollec\"}}]}}}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(contentAsString).contains("One-time direct event sent successfully");
    }

    @Test
    void testShouldReplaceKeywordsAndSendEventDirectly() throws Exception {
        String contentAsString = mockMvc
                .perform(post(EVENT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"vesServerUrl\": \"http://localhost:9999/eventListener\",\n"
                                + "    \"event\": {\n"
                                + "        \"commonEventHeader\": {\n"
                                + "            \"eventId\": \"#RandomString(20)\",\n"
                                + "            \"sourceName\": \"PATCHED_sourceName\",\n"
                                + "            \"version\": 3.0\n}}}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertThat(contentAsString).contains("One-time direct event sent successfully");

        verify(simulatorService, Mockito.times(1)).triggerOneTimeEvent(any(FullEvent.class));
    }

    @Test
    void shouldUseTestEndpointThenReceiveProperMessage() throws Exception {
        String contentAsString = mockMvc
                .perform(post(TEST_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"simulatorParams\": {\n" +
                                "        \"vesServerUrl\": \"http://localhost:9999/eventListener\"\n" +
                                "    },\n" +
                                "    \"templateName\": \"testTemplateName\"\n" +
                                "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(contentAsString).contains("message1234");
    }

    @Test
    void shouldSuccessfullyCancelJobThenReturnProperMessage() throws Exception {
        when(simulatorService.cancelEvent(JOB_NAME)).thenReturn(true);

        String contentAsString = mockMvc
                .perform(post(CANCEL_JOB_ENDPOINT + JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(contentAsString).contains(EVENT_WAS_CANCELLED);
    }

    @Test
    void shouldFailWhileCancelingJobThenReturnProperMessage() throws Exception {
        when(simulatorService.cancelEvent(JOB_NAME)).thenReturn(false);

        String contentAsString = mockMvc
                .perform(post(CANCEL_JOB_ENDPOINT + JOB_NAME)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(""))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        assertThat(contentAsString).contains(EVENT_WAS_NOT_CANCELLED);
    }

    @Test
    void shouldSuccessfullyCancelAllJobsThenReturnsProperMessage() throws Exception {
        when(simulatorService.cancelAllEvents()).thenReturn(true);

        String contentAsString = mockMvc
                .perform(post(CANCEL_JOB_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(contentAsString).contains(EVENT_WAS_CANCELLED);
    }

    @Test
    void shouldSuccessfullyCancelJobWhenSendingJobNameWithBreakingCharactersThenReturnProperMessage() throws SchedulerException {
        final String lineBreakingJobName = "test\tJob\nName\r";
        when(simulatorService.cancelEvent(lineBreakingJobName)).thenReturn(true);

        Object actualResponseBody = Objects.requireNonNull(controller.cancelEvent(lineBreakingJobName).getBody());

        assertThat(actualResponseBody.toString()).contains(EVENT_WAS_CANCELLED);
    }

    @Test
    void shouldReturnAllEvents() throws Exception {
        List<EventData> events = getEventDatas();
        String expectedMessage = events.stream()
                .map(EventData::toString)
                .collect(Collectors.joining("\\n"));

        when(eventDataService.getAllEvents()).thenReturn(events);

        String contentAsString = mockMvc
                .perform(get(ALL_EVENTS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(contentAsString).contains(expectedMessage);
    }


    private List<EventData> getEventDatas() {
        return Arrays.asList(
                getEventData("id1", "keywords1", "input1", "patched1", "template1", 0),
                getEventData("id2", "keywords2", "input2", "patched2", "template2", 1)
        );
    }

    private EventData getEventData(String id, String keywords, String input, String patched, String template, int incrementValue) {
        return EventData.builder()
                .id(id)
                .keywords(keywords)
                .input(input)
                .patched(patched)
                .template(template)
                .incrementValue(incrementValue)
                .build();
    }

    private void startSimulator() throws Exception {
        mockMvc
                .perform(post(START_ENDPOINT)
                        .content(simulatorRequestBody)
                        .contentType(MediaType.APPLICATION_JSON).characterEncoding("utf-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_MSG_EXPRESSION).value("Request started"));

    }

    private String createStringReprOfJson(String key, String value) {
        return GSON_OBJ.toJson(ImmutableMap.of(key, value));
    }
}
