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

package org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import brave.Tracing;
import brave.propagation.TraceContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.KeywordsExtractor;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.KeywordsHandler;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpClientAdapter;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import zipkin2.Span;

class EventJobTest {

    @Test
    void shouldSendEventWhenExecuteCalled() {
        //given
        EventJob eventJob = new EventJob();
        String templateName = "template name";
        String vesUrl = "http://someurl:80/";
        String eventId = "1";
        JsonParser parser = new JsonParser();
        JsonObject body = parser.parse("{\"a\": \"A\"}").getAsJsonObject();
        HttpClientAdapter clientAdapter = mock(HttpClientAdapter.class);
        JobExecutionContext jobExecutionContext =
            createMockJobExecutionContext(templateName, eventId, vesUrl, body, clientAdapter);

        ArgumentCaptor<String> vesUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        //when
        eventJob.execute(jobExecutionContext);

        //then
        verify(clientAdapter).send(bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).isEqualTo(body.toString());
    }

    @Test
    void shouldSendEventInChildSpanOfSchedulingTrace() {
        //given
        List<Span> reportedSpans = new ArrayList<>();
        Tracing tracing = Tracing.newBuilder().spanReporter(reportedSpans::add).build();
        TraceContext schedulingContext = tracing.tracer().newTrace().context();
        HttpClientAdapter clientAdapter = mock(HttpClientAdapter.class);
        AtomicReference<TraceContext> sendContext = captureTraceContextOnSend(tracing, clientAdapter);
        JobExecutionContext jobExecutionContext = createMockJobExecutionContext("template name", "1",
            "http://someurl:80/", new JsonObject(), clientAdapter);
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        jobDataMap.put(EventJob.TRACER, tracing.tracer());
        jobDataMap.put(EventJob.PARENT_TRACE_CONTEXT, schedulingContext);

        //when
        new EventJob().execute(jobExecutionContext);
        tracing.close();

        //then
        assertThat(sendContext.get().traceId()).isEqualTo(schedulingContext.traceId());
        assertThat(sendContext.get().parentIdAsLong()).isEqualTo(schedulingContext.spanId());
        assertThat(reportedSpans).extracting(Span::name).containsExactly("send-event");
    }

    @Test
    void shouldSendEventInNewTraceWhenScheduledWithoutTrace() {
        //given
        Tracing tracing = Tracing.newBuilder().build();
        HttpClientAdapter clientAdapter = mock(HttpClientAdapter.class);
        AtomicReference<TraceContext> sendContext = captureTraceContextOnSend(tracing, clientAdapter);
        JobExecutionContext jobExecutionContext = createMockJobExecutionContext("template name", "1",
            "http://someurl:80/", new JsonObject(), clientAdapter);
        jobExecutionContext.getJobDetail().getJobDataMap().put(EventJob.TRACER, tracing.tracer());

        //when
        new EventJob().execute(jobExecutionContext);
        tracing.close();

        //then
        assertThat(sendContext.get()).isNotNull();
        assertThat(sendContext.get().parentIdAsLong()).isZero();
    }

    private AtomicReference<TraceContext> captureTraceContextOnSend(Tracing tracing, HttpClientAdapter clientAdapter) {
        AtomicReference<TraceContext> sendContext = new AtomicReference<>();
        doAnswer(invocation -> {
            sendContext.set(tracing.currentTraceContext().get());
            return null;
        }).when(clientAdapter).send(anyString());
        return sendContext;
    }

    private JobExecutionContext createMockJobExecutionContext(String templateName, String eventId, String vesUrl,
        JsonObject body, HttpClientAdapter clientAdapter) {

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(EventJob.TEMPLATE_NAME, templateName);
        jobDataMap.put(EventJob.KEYWORDS_HANDLER, new KeywordsHandler(new KeywordsExtractor(), (id) -> 1));
        jobDataMap.put(EventJob.EVENT_ID, eventId);
        jobDataMap.put(EventJob.VES_URL, vesUrl);
        jobDataMap.put(EventJob.BODY, body);
        jobDataMap.put(EventJob.CLIENT_ADAPTER, clientAdapter);

        JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDetail.getKey()).thenReturn(new JobKey("jobId", "group"));
        return jobExecutionContext;
    }
}
