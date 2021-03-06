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

package org.onap.integration.simulators.nfsimulator.vesclient.simulator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.FullEvent;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.SimulatorParams;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.SimulatorRequest;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpClientAdapter;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpResponseAdapter;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.utils.ssl.SslAuthenticationHelper;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventScheduler;
import org.onap.integration.simulators.nfsimulator.vesclient.simulatorconfig.SimulatorConfig;
import org.onap.integration.simulators.nfsimulator.vesclient.simulatorconfig.SimulatorConfigService;
import org.onap.integration.simulators.nfsimulator.vesclient.event.EventData;
import org.onap.integration.simulators.nfsimulator.vesclient.event.EventDataService;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpTestUtils;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class SimulatorServiceTest {

    private static final String VES_URL = "http://0.0.0.0:8080";
    private static final String IN_DB_VES_URL = "http://0.0.0.0:8080/eventListener/v6";
    private static final Gson GSON = new Gson();
    private static final JsonObject VALID_PATCH = GSON.fromJson("{\"event\": {\n"
        + "    \"commonEventHeader\": {\n"
        + "      \"sourceName\": \"SomeCustomSource\"}}}\n", JsonObject.class);
    private static final JsonObject VALID_FULL_EVENT = GSON.fromJson("{\"event\": {\n"
        + "    \"commonEventHeader\": {\n"
        + "      \"domain\": \"notification\",\n"
        + "      \"eventName\": \"vFirewallBroadcastPackets\"\n"
        + "    },\n"
        + "    \"notificationFields\": {\n"
        + "      \"arrayOfNamedHashMap\": [{\n"
        + "        \"name\": \"A20161221.1031-1041.bin.gz\",\n"
        + "        \"hashMap\": {\n"
        + "          \"fileformatType\": \"org.3GPP.32.435#measCollec\"}}]}}}", JsonObject.class);
    private static final JsonObject FULL_EVENT_WITH_KEYWORDS = GSON.fromJson("{\"event\":{  \n"
        + "      \"commonEventHeader\":{  \n"
        + "         \"domain\":\"notification\",\n"
        + "         \"eventName\":\"#RandomString(20)\",\n"
        + "         \"eventOrderNo\":\"#Increment\"}}}", JsonObject.class);
    private static final JsonObject VALID_VARIABLES = GSON.fromJson("{\"dn\": \"TestDN-1\", \"measurement\":{\n"
        + "              \"name\": \"AdditionalM\",\n"
        + "              \"value\": \"1.5\"\n"
        + "            }}", JsonObject.class);
    private static final String SOME_CUSTOM_SOURCE = "SomeCustomSource";
    private static final String CLOSED_LOOP_VNF = "ClosedLoopVNF";
    private static final String SAMPLE_ID = "sampleId";
    private static final EventData SAMPLE_EVENT = EventData.builder().id("1").build();
    private static URL inDbVesUrl;
    private final ArgumentCaptor<JsonObject> bodyCaptor = ArgumentCaptor.forClass(JsonObject.class);
    private final ArgumentCaptor<Integer> intervalCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Integer> repeatCountCaptor = ArgumentCaptor
        .forClass(Integer.class);
    private final ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> vesUrlCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> eventContentCaptor = ArgumentCaptor.forClass(String.class);
    private final SslAuthenticationHelper sslAuthenticationHelper = new SslAuthenticationHelper();
    private final TemplatePatcher templatePatcher = new TemplatePatcher();
    private final TemplateReader templateReader = new FilesystemTemplateReader(
        "src/test/resources/org/onap/integration/simulators/nfsimulator/vesclient/simulator/", GSON);

    private SimulatorService simulatorService;
    private EventDataService eventDataService;
    private EventScheduler eventScheduler;
    private SimulatorConfigService simulatorConfigService;

    @BeforeEach
    void setUp() throws MalformedURLException {
        inDbVesUrl = new URL(IN_DB_VES_URL);
        eventDataService = mock(EventDataService.class);
        eventScheduler = mock(EventScheduler.class);
        simulatorConfigService = mock(SimulatorConfigService.class);

        simulatorService = new SimulatorService(templatePatcher, templateReader,
            eventScheduler, eventDataService, simulatorConfigService,
            new TemplateVariablesReplacer(),new SslAuthenticationHelper());
    }

    @Test
    void shouldTriggerEventWithGivenParams() throws IOException, SchedulerException, GeneralSecurityException {
        String templateName = "validExampleMeasurementEvent.json";
        SimulatorParams simulatorParams = new SimulatorParams(VES_URL, 1, 1);
        SimulatorRequest simulatorRequest = new SimulatorRequest(simulatorParams,
            templateName, VALID_PATCH, VALID_VARIABLES);

        doReturn(SAMPLE_EVENT).when(eventDataService).persistEventData(any(JsonObject.class), any(JsonObject.class), any(JsonObject.class), any(JsonObject.class));

        simulatorService.triggerEvent(simulatorRequest);

        assertEventHasExpectedStructure(VES_URL, templateName, SOME_CUSTOM_SOURCE);
        assertEventHasReplacedVariables();
    }

    @Test
    void shouldTriggerEventWithDefaultVesUrlWhenNotProvidedInRequest() throws IOException, SchedulerException, GeneralSecurityException {
        String templateName = "validExampleMeasurementEvent.json";
        SimulatorRequest simulatorRequest = new SimulatorRequest(
            new SimulatorParams("", 1, 1),
            templateName, VALID_PATCH, new JsonObject());

        doReturn(SAMPLE_EVENT).when(eventDataService).persistEventData(any(JsonObject.class), any(JsonObject.class), any(JsonObject.class), any(JsonObject.class));
        when(simulatorConfigService.getConfiguration()).thenReturn(new SimulatorConfig(SAMPLE_ID, inDbVesUrl));

        simulatorService.triggerEvent(simulatorRequest);

        assertEventHasExpectedStructure(inDbVesUrl.toString(), templateName, SOME_CUSTOM_SOURCE);
    }

    @Test
    void shouldThrowJsonSyntaxWhenInvalidJson() {
        //given
        JsonObject patch = GSON.fromJson("{\n"
            + "  \"event\": {\n"
            + "    \"commonEventHeader\": {\n"
            + "      \"sourceName\": \""
            + SOME_CUSTOM_SOURCE + "\"\n"
            + "    }\n"
            + "  }\n"
            + "}\n", JsonObject.class);
        EventData eventData = EventData.builder().id("1").build();

        SimulatorParams simulatorParams = new SimulatorParams(VES_URL, 1, 1);
        SimulatorRequest simulatorRequest = new SimulatorRequest(simulatorParams,
            "invalidJsonStructureEvent.json", patch, new JsonObject());
        doReturn(eventData).when(eventDataService).persistEventData(any(JsonObject.class), any(JsonObject.class), any(JsonObject.class), any(JsonObject.class));

        //when
        assertThrows(JsonSyntaxException.class,
            () -> simulatorService.triggerEvent(simulatorRequest));
    }

    @Test
    void shouldHandleNonExistingPatchSection() throws IOException, SchedulerException, GeneralSecurityException {
        String templateName = "validExampleMeasurementEvent.json";
        SimulatorRequest simulatorRequest = new SimulatorRequest(
            new SimulatorParams("", 1, 1),
            templateName, null, new JsonObject());

        doReturn(SAMPLE_EVENT).when(eventDataService).persistEventData(any(JsonObject.class), any(JsonObject.class), any(JsonObject.class), any(JsonObject.class));
        doReturn(new SimulatorConfig(SAMPLE_ID, inDbVesUrl)).when(simulatorConfigService).getConfiguration();

        simulatorService.triggerEvent(simulatorRequest);

        assertEventHasExpectedStructure(inDbVesUrl.toString(), templateName, CLOSED_LOOP_VNF);
    }

    @Test
    void shouldSuccessfullySendOneTimeEventWithVesUrlWhenPassed() throws IOException, GeneralSecurityException {
        SimulatorService spiedTestedService = spy(new SimulatorService(
            templatePatcher, templateReader, eventScheduler,
            eventDataService, simulatorConfigService,
            new TemplateVariablesReplacer(),
            new SslAuthenticationHelper()));

        HttpClientAdapter adapterMock = mock(HttpClientAdapter.class);
        prepareMocksWithAcceptedResponse(spiedTestedService, adapterMock);
        FullEvent event = new FullEvent(VES_URL, VALID_FULL_EVENT);

        spiedTestedService.triggerOneTimeEvent(event);

        assertThat(eventContentCaptor.getValue()).isEqualTo(VALID_FULL_EVENT.toString());
        verify(eventDataService, times(1)).persistEventData(any(JsonObject.class), any(JsonObject.class), any(JsonObject.class), any(JsonObject.class));
        verify(adapterMock, times(1)).send(VALID_FULL_EVENT.toString());
    }

    @Test
    void shouldSubstituteKeywordsAndSuccessfullySendOneTimeEvent() throws IOException, GeneralSecurityException {
        SimulatorService spiedTestedService = spy(new SimulatorService(
            templatePatcher, templateReader, eventScheduler,
            eventDataService, simulatorConfigService,
            new TemplateVariablesReplacer(),
            new SslAuthenticationHelper())
        );

        HttpClientAdapter adapterMock = mock(HttpClientAdapter.class);
        prepareMocksWithAcceptedResponse(spiedTestedService, adapterMock);
        FullEvent event = new FullEvent(VES_URL, FULL_EVENT_WITH_KEYWORDS);

        spiedTestedService.triggerOneTimeEvent(event);

        JsonObject sentContent = GSON.fromJson(eventContentCaptor.getValue(), JsonElement.class).getAsJsonObject();
        assertThat(sentContent.getAsJsonObject("event").getAsJsonObject("commonEventHeader").get("eventOrderNo").getAsString()).isEqualTo("1");
        assertThat(sentContent.getAsJsonObject("event").getAsJsonObject("commonEventHeader").get("eventName").getAsString()).hasSize(20);
    }

    @Test
    void shouldGetSimulatorConfiguration() {
        SimulatorConfig simulatorConfig = getSimulatorConfig();

        when(simulatorConfigService.getConfiguration()).thenReturn(simulatorConfig);

        Assertions.assertEquals(simulatorService.getConfiguration(), simulatorConfig);
    }

    @Test
    void shouldUpdateSimulatorConfiguration() {
        SimulatorConfig simulatorConfig = getSimulatorConfig();

        when(simulatorConfigService.updateConfiguration(simulatorConfig)).thenReturn(simulatorConfig);

        Assertions.assertEquals(simulatorService.updateConfiguration(simulatorConfig), simulatorConfig);
    }

    @Test
    void shouldCancelAllEvents() throws SchedulerException {
        when(eventScheduler.cancelAllEvents()).thenReturn(true);

        assertTrue(simulatorService.cancelAllEvents());
    }

    @Test
    void shouldCancelSingleEvent() throws SchedulerException {
        final String jobName = "testJobName";
        when(eventScheduler.cancelEvent(jobName)).thenReturn(true);

        assertTrue(simulatorService.cancelEvent(jobName));
    }

    private void prepareMocksWithAcceptedResponse(SimulatorService spiedTestedService, HttpClientAdapter adapterMock) throws IOException, GeneralSecurityException {
        HttpResponseAdapter response = new HttpResponseAdapter(HttpStatus.SC_ACCEPTED, HttpTestUtils.HTTP_MESSAGE_ACCEPTER);
        doReturn(response).when(adapterMock).send(eventContentCaptor.capture());
        doReturn(adapterMock).when(spiedTestedService).createHttpClientAdapter(any(String.class));
    }

    private void assertEventHasExpectedStructure(String expectedVesUrl, String templateName, String sourceNameString) throws SchedulerException, IOException, GeneralSecurityException {
        verify(eventScheduler, times(1)).scheduleEvent(vesUrlCaptor.capture(), intervalCaptor.capture(),
            repeatCountCaptor.capture(), templateNameCaptor.capture(), eventIdCaptor.capture(), bodyCaptor.capture());
        assertThat(vesUrlCaptor.getValue()).isEqualTo(expectedVesUrl);
        assertThat(intervalCaptor.getValue()).isEqualTo(1);
        assertThat(repeatCountCaptor.getValue()).isEqualTo(1);
        assertThat(templateNameCaptor.getValue()).isEqualTo(templateName);
        String actualSourceName = GSON.fromJson(bodyCaptor.getValue(), JsonObject.class)
            .get("event").getAsJsonObject()
            .get("commonEventHeader").getAsJsonObject()
            .get("sourceName").getAsString();
        assertThat(actualSourceName).isEqualTo(sourceNameString);
        verify(eventDataService)
            .persistEventData(any(JsonObject.class), any(JsonObject.class), any(JsonObject.class),
                any(JsonObject.class));
    }

    private SimulatorConfig getSimulatorConfig() {
        return new SimulatorConfig(SAMPLE_ID, inDbVesUrl);
    }

    private void assertEventHasReplacedVariables() {
        String measurementName = GSON.fromJson(bodyCaptor.getValue(), JsonObject.class)
            .get("event").getAsJsonObject()
            .get("measurementsForVfScalingFields").getAsJsonObject()
            .get("additionalMeasurements").getAsJsonArray().get(0).getAsJsonObject()
            .get("arrayOfFields").getAsJsonArray().get(0).getAsJsonObject()
            .get("name").getAsString();

        String reportingEntityName = GSON.fromJson(bodyCaptor.getValue(), JsonObject.class)
            .get("event").getAsJsonObject()
            .get("commonEventHeader").getAsJsonObject()
            .get("reportingEntityName").getAsString();

        assertThat(measurementName).isEqualTo("AdditionalM");
        assertThat(reportingEntityName).isEqualTo("TestDN-1");
    }

}
