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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpClientAdapterFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.ObjectProvider;

class EventSchedulerTest {

    @InjectMocks
    EventScheduler eventScheduler;

    @Mock
    Scheduler quartzScheduler;

    @Mock
    HttpClientAdapterFactory httpClientAdapterFactory;

    @Mock
    ObjectProvider<Tracing> tracingProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldTriggerEventWithGivenConfiguration() throws SchedulerException, IOException, GeneralSecurityException {
        //given
        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<SimpleTrigger> triggerCaptor = ArgumentCaptor.forClass(SimpleTrigger.class);

        String vesUrl = "http://some:80/";
        int repeatInterval = 1;
        int repeatCount = 4;
        String testName = "testName";
        String eventId = "1";
        JsonObject body = new JsonObject();

        //when
        eventScheduler.scheduleEvent(vesUrl, repeatInterval, repeatCount, testName, eventId, body);

        //then
        verify(quartzScheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        JobDataMap actualJobDataMap = jobDetailCaptor.getValue().getJobDataMap();
        assertThat(actualJobDataMap.get(EventJob.BODY)).isEqualTo(body);
        assertThat(actualJobDataMap.get(EventJob.TEMPLATE_NAME)).isEqualTo(testName);
        assertThat(actualJobDataMap.get(EventJob.VES_URL)).isEqualTo(vesUrl);

        SimpleTrigger actualTrigger = triggerCaptor.getValue();
        // repeat count adds 1 to given value
        assertThat(actualTrigger.getRepeatCount()).isEqualTo(repeatCount - 1);

        //getRepeatInterval returns interval in ms
        assertThat(actualTrigger.getRepeatInterval()).isEqualTo(repeatInterval * 1000);
    }

    @Test
    void shouldPassSchedulingTraceContextToJob() throws SchedulerException, IOException, GeneralSecurityException {
        //given
        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        Tracing tracing = Tracing.newBuilder().build();
        when(tracingProvider.getIfAvailable()).thenReturn(tracing);
        Span schedulingSpan = tracing.tracer().newTrace().start();

        //when
        try (Tracer.SpanInScope ignored = tracing.tracer().withSpanInScope(schedulingSpan)) {
            eventScheduler.scheduleEvent("http://some:80/", 1, 1, "testName", "1", new JsonObject());
        } finally {
            schedulingSpan.finish();
            tracing.close();
        }

        //then
        verify(quartzScheduler).scheduleJob(jobDetailCaptor.capture(), any(SimpleTrigger.class));
        JobDataMap actualJobDataMap = jobDetailCaptor.getValue().getJobDataMap();
        assertThat(actualJobDataMap.get(EventJob.TRACER)).isSameAs(tracing.tracer());
        assertThat(actualJobDataMap.get(EventJob.PARENT_TRACE_CONTEXT)).isEqualTo(schedulingSpan.context());
    }

    @Test
    void shouldNotPassTracerToJobWhenTracingIsDisabled() throws SchedulerException, IOException, GeneralSecurityException {
        //given
        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);

        //when
        eventScheduler.scheduleEvent("http://some:80/", 1, 1, "testName", "1", new JsonObject());

        //then
        verify(quartzScheduler).scheduleJob(jobDetailCaptor.capture(), any(SimpleTrigger.class));
        JobDataMap actualJobDataMap = jobDetailCaptor.getValue().getJobDataMap();
        assertThat(actualJobDataMap.containsKey(EventJob.TRACER)).isFalse();
        assertThat(actualJobDataMap.containsKey(EventJob.PARENT_TRACE_CONTEXT)).isFalse();
    }

    @Test
    void shouldCancelAllScheduledEvents() throws SchedulerException {
        //given
        Set<JobKey> jobsKeys = new HashSet<>(Arrays.asList(new JobKey("jobName1"), new JobKey("jobName2"),
            new JobKey("jobName3"), new JobKey("jobName4")));
        when(quartzScheduler.getJobKeys(GroupMatcher.anyJobGroup())).thenReturn(jobsKeys);
        when(quartzScheduler.deleteJobs(new ArrayList<>(jobsKeys))).thenReturn(true);

        //when
        boolean isCancelled = eventScheduler.cancelAllEvents();

        //then
        assertThat(isCancelled).isTrue();
    }

    @Test
    void shouldCancelSingleScheduledEvent() throws SchedulerException {
        //given
        JobKey jobToRemove = new JobKey("jobName3");
        Set<JobKey> jobsKeys = new HashSet<>(Arrays.asList(new JobKey("jobName1"), new JobKey("jobName2"),
            jobToRemove, new JobKey("jobName4")));
        when(quartzScheduler.getJobKeys(GroupMatcher.anyJobGroup())).thenReturn(jobsKeys);
        when(quartzScheduler.deleteJob(jobToRemove)).thenReturn(true);

        //when
        boolean isCancelled = eventScheduler.cancelEvent("jobName3");

        //then
        assertThat(isCancelled).isTrue();
    }

    @Test
    void shouldNotCancelUnknownEvent() throws SchedulerException {
        //given
        when(quartzScheduler.getJobKeys(GroupMatcher.anyJobGroup()))
            .thenReturn(new HashSet<>(Arrays.asList(new JobKey("jobName1"))));

        //when
        boolean isCancelled = eventScheduler.cancelEvent("unknownJob");

        //then
        assertThat(isCancelled).isFalse();
        verify(quartzScheduler, never()).deleteJob(any(JobKey.class));
    }
}
