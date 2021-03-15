/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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
import com.google.gson.JsonSyntaxException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.json.JSONException;
import org.onap.integration.simulators.nfsimulator.vesclient.event.EventData;
import org.onap.integration.simulators.nfsimulator.vesclient.event.EventDataService;
import org.onap.integration.simulators.nfsimulator.vesclient.logging.MdcVariables;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.FullEvent;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.SimulatorRequest;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.SimulatorService;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpResponseAdapter;
import org.onap.integration.simulators.nfsimulator.vesclient.simulatorconfig.SimulatorConfig;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.util.DateUtil;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.util.ResponseBuilder;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.onap.integration.simulators.nfsimulator.vesclient.rest.util.ResponseBuilder.MESSAGE;
import static org.onap.integration.simulators.nfsimulator.vesclient.rest.util.ResponseBuilder.TIMESTAMP;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/simulator")
@Api(tags = "Ves client", value = "Simulate Ves client")
public class SimulatorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorController.class);
    private static final Marker ENTRY = MarkerFactory.getMarker("ENTRY");
    private static final String INCORRECT_TEMPLATE_MESSAGE = "Cannot start simulator, template %s is not in valid format: %s";
    private static final String NOT_EXISTING_TEMPLATE = "Cannot start simulator - template %s not found.";
    private static final String BREAKING_CHARACTER_REGEX = "[\n|\r|\t]";
    private final DateFormat responseDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss,SSS");
    private final SimulatorService simulatorService;
    private EventDataService eventDataService;

    @Autowired
    public SimulatorController(SimulatorService simulatorService,
                               EventDataService eventDataService) {
        this.simulatorService = simulatorService;
        this.eventDataService = eventDataService;
    }

    /**
     * @deprecated
     */
    @PostMapping("test")
    @Deprecated
    public ResponseEntity<Map<String, Object>> test(@Valid @RequestBody SimulatorRequest simulatorRequest) {
        MDC.put("test", "test");
        String simulatorRequestString = simulatorRequest.toString();
        LOGGER.info(ENTRY, simulatorRequestString);
        return buildResponse(OK, ImmutableMap.of(MESSAGE, "message1234"));
    }

    @PostMapping(value = "start")
    @ApiOperation(value = "Start a job which will send a multiple events to VES")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Job scheduled successfully. It returns: {'message':'<txt>', 'jobName':'<job_name>'}"),
            @ApiResponse(code = 400, message = "Bad request: invalid json in payload"),
            @ApiResponse(code = 500, message = "Internal server error: unexpected error") })
    public ResponseEntity<Map<String, Object>> start(@RequestHeader HttpHeaders headers,
                                                     @Valid
                                                     @RequestBody
                                                     @ApiParam(
                                                             name =  "jobConfiguration",
                                                             value = "Information what to send as event and when",
                                                             required = true) SimulatorRequest triggerEventRequest) {
        logContextHeaders(headers, "/simulator/start");
        LOGGER.info(ENTRY, "Simulator started");

        try {
            return processRequest(triggerEventRequest);

        } catch (JSONException | JsonSyntaxException e) {
            MDC.put(MdcVariables.RESPONSE_CODE, BAD_REQUEST.toString());
            LOGGER.warn("Cannot trigger event, invalid json format: {}", e.getMessage());
            LOGGER.debug("Received json has invalid format", e);
            return buildResponse(BAD_REQUEST, ImmutableMap.of(MESSAGE, String
                .format(INCORRECT_TEMPLATE_MESSAGE, triggerEventRequest.getTemplateName(),
                    e.getMessage())));
        } catch (GeneralSecurityException e) {
            MDC.put(MdcVariables.RESPONSE_CODE, INTERNAL_SERVER_ERROR.toString());
            LOGGER.error("Client certificate validation failed: {}", e.getMessage());
            return buildResponse(INTERNAL_SERVER_ERROR,
                ImmutableMap.of(MESSAGE, "Invalid or misconfigured client certificate"));
        } catch (IOException e) {
            MDC.put(MdcVariables.RESPONSE_CODE, BAD_REQUEST.toString());
            LOGGER.warn("Json validation failed: {}", e.getMessage());
            return buildResponse(BAD_REQUEST,
                ImmutableMap.of(MESSAGE, String.format(NOT_EXISTING_TEMPLATE, triggerEventRequest.getTemplateName())));
        } catch (Exception e) {
            MDC.put(MdcVariables.RESPONSE_CODE, INTERNAL_SERVER_ERROR.toString());
            LOGGER.error("Cannot trigger event - unexpected exception", e);
            return buildResponse(INTERNAL_SERVER_ERROR,
                ImmutableMap.of(MESSAGE, "Unexpected exception: " + e.getMessage()));
        } finally {
            MDC.clear();
        }
    }

    /**
     * @deprecated
     */
    @GetMapping("all-events")
    @Deprecated
    public ResponseEntity<Map<String, Object>> allEvents() {
        List<EventData> eventDataList = eventDataService.getAllEvents();
        StringBuilder sb = new StringBuilder();
        eventDataList.forEach(e -> sb.append(e).append(System.lineSeparator()));

        return ResponseBuilder
            .status(OK).put(MESSAGE, sb.toString())
            .build();
    }

    @GetMapping("config")
    @ApiOperation(value = "Get simulator configuration")
    @ApiResponses(
            value = {
                    @ApiResponse(code=200, message = "Ves client configuration fetched. It returns: {'simulatorConfig': {'vesServerUrl': '<urlToVesService>'} }"),
            }
    )
    public ResponseEntity<Map<String, Object>> getConfig() {
        SimulatorConfig configToGet = simulatorService.getConfiguration();
        return buildResponse(OK, ImmutableMap.of("simulatorConfig", configToGet));
    }

    @PutMapping("config")
    @ApiOperation(value = "Update simulator configuration")
    @ApiResponses(
            value = {
                    @ApiResponse(code=200, message = "Ves client configuration updated. It returns: {'simulatorConfig': {'vesServerUrl': '<urlToVesService>'} }"),
            }
    )
    public ResponseEntity<Map<String, Object>> updateConfig(
            @Valid @RequestBody
            @ApiParam(
                name =  "clientConfiguration",
                value = "Client configuration",
                required = true) SimulatorConfig newConfig) {
        SimulatorConfig updatedConfig = simulatorService.updateConfiguration(newConfig);
        return buildResponse(OK, ImmutableMap.of("simulatorConfig", updatedConfig));
    }

    @PostMapping("cancel/{jobName}")
    @ApiOperation(value = "Cancel a single job which sends events to VES")
    @ApiResponses(
            value = {
                    @ApiResponse(code=200, message = "Job cancelled successfully"),
                    @ApiResponse(code=404, message = "Unable to find job to cancel"),
            }
    )
    public ResponseEntity<Map<String, Object>> cancelEvent(@PathVariable String jobName) throws SchedulerException {
        String jobNameNoBreakingCharacters = replaceBreakingCharacters(jobName);
        LOGGER.info(ENTRY, "Cancel called on {}.", jobNameNoBreakingCharacters);
        boolean isCancelled = simulatorService.cancelEvent(jobName);
        return createCancelEventResponse(isCancelled);
    }

    @PostMapping("cancel")
    @ApiOperation(value = "Cancel all jobs which send events to VES")
    @ApiResponses(
            value = {
                    @ApiResponse(code=200, message = "Jobs cancelled successfully"),
                    @ApiResponse(code=404, message = "Unable to find job to cancel"),
            }
    )
    public ResponseEntity<Map<String, Object>> cancelAllEvent() throws SchedulerException {
        LOGGER.info(ENTRY, "Cancel called on all jobs");
        boolean isCancelled = simulatorService.cancelAllEvents();
        return createCancelEventResponse(isCancelled);
    }

    @PostMapping("event")
    @ApiOperation(value = "Send single event to VES")
    @ApiResponses(
            value = {
                    @ApiResponse(code=200, message = "Event sent successfully")
            }
    )
    public ResponseEntity<Map<String, Object>> sendEventDirectly(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody
            @ApiParam(
                name =  "event",
                value = "Event to send",
                required = true) FullEvent event)
        throws IOException, GeneralSecurityException {
        logContextHeaders(headers, "/simulator/event");
        LOGGER.info(ENTRY, "Trying to send one-time event directly to VES Collector");
        HttpResponseAdapter response = simulatorService.triggerOneTimeEvent(event);
        return buildResponse(response);
    }

    private String replaceBreakingCharacters(String jobName) {
        return jobName.replaceAll(BREAKING_CHARACTER_REGEX, "_");
    }

    private ResponseEntity<Map<String, Object>> processRequest(SimulatorRequest triggerEventRequest)
        throws IOException, SchedulerException, GeneralSecurityException {

        String jobName = simulatorService.triggerEvent(triggerEventRequest);
        MDC.put(MdcVariables.RESPONSE_CODE, OK.toString());
        return buildResponse(OK, ImmutableMap.of(MESSAGE, "Request started", "jobName", jobName));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus endStatus, Map<String, Object> parameters) {
        ResponseBuilder builder = ResponseBuilder
            .status(endStatus)
            .put(TIMESTAMP, DateUtil.getTimestamp(responseDateFormat));
        parameters.forEach(builder::put);
        return builder.build();
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpResponseAdapter response) {
        HttpStatus status = HttpStatus.valueOf(response.getCode());
        Map<String, Object> parameters;
        if (response.getMessage().isEmpty()) {
            parameters = Map.of(MESSAGE, "One-time direct event sent successfully");
        } else {
            parameters = Map.of(MESSAGE, response.getMessage());
        }
        return buildResponse(status, parameters);
    }

    private void logContextHeaders(HttpHeaders headers, String serviceName) {
        MDC.put(MdcVariables.REQUEST_ID, headers.getFirst(MdcVariables.X_ONAP_REQUEST_ID));
        MDC.put(MdcVariables.INVOCATION_ID, headers.getFirst(MdcVariables.X_INVOCATION_ID));
        MDC.put(MdcVariables.INSTANCE_UUID, UUID.randomUUID().toString());
        MDC.put(MdcVariables.SERVICE_NAME, serviceName);
    }

    private ResponseEntity<Map<String, Object>> createCancelEventResponse(boolean isCancelled) {
        if (isCancelled) {
            return buildResponse(OK, ImmutableMap.of(MESSAGE, "Event(s) was cancelled"));
        } else {
            return buildResponse(NOT_FOUND, ImmutableMap.of(MESSAGE, "Simulator was not able to cancel event(s)"));
        }
    }
}
