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


import com.google.gson.JsonObject;
import brave.Tracing;
import brave.propagation.TraceContext;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.KeywordsHandler;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpClientAdapterFactory;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.BODY;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.CLIENT_ADAPTER;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.EVENT_ID;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.KEYWORDS_HANDLER;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.PARENT_TRACE_CONTEXT;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.TEMPLATE_NAME;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.TRACER;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.scheduler.EventJob.VES_URL;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

@Component
public class EventScheduler {


    private final Scheduler scheduler;
    private final KeywordsHandler keywordsHandler;
    private final HttpClientAdapterFactory httpClientAdapterFactory;
    private final ObjectProvider<Tracing> tracing;

    @Autowired
    public EventScheduler(Scheduler scheduler, KeywordsHandler keywordsHandler,
                          HttpClientAdapterFactory httpClientAdapterFactory, ObjectProvider<Tracing> tracing) {
        this.scheduler = scheduler;
        this.keywordsHandler = keywordsHandler;
        this.httpClientAdapterFactory = httpClientAdapterFactory;
        this.tracing = tracing;
    }

    public String scheduleEvent(String vesUrl, Integer repeatInterval, Integer repeatCount,
                                String templateName, String eventId, JsonObject body)
            throws SchedulerException, IOException, GeneralSecurityException {

        JobDetail jobDetail = createJobDetail(vesUrl, templateName, eventId, body);
        SimpleTrigger trigger = createTrigger(repeatInterval, repeatCount);

        scheduler.scheduleJob(jobDetail, trigger);
        return jobDetail.getKey().getName();
    }

    public boolean cancelAllEvents() throws SchedulerException {
        List<JobKey> jobKeys = getActiveJobsKeys();
        return scheduler.deleteJobs(jobKeys);
    }

    public boolean cancelEvent(String jobName) throws SchedulerException {
        Optional<JobKey> activeJobKey = getActiveJobsKeys().stream().filter(e -> e.getName().equals(jobName)).findFirst();
        return activeJobKey.isPresent() && scheduler.deleteJob(activeJobKey.get());
    }

    private SimpleTrigger createTrigger(int interval, int repeatCount) {
        return TriggerBuilder.newTrigger()
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(interval)
                        .withRepeatCount(repeatCount - 1))
                .build();
    }

    private JobDetail createJobDetail(String vesUrl, String templateName, String eventId, JsonObject body) throws IOException, GeneralSecurityException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TEMPLATE_NAME, templateName);
        jobDataMap.put(VES_URL, vesUrl);
        jobDataMap.put(EVENT_ID, eventId);
        jobDataMap.put(KEYWORDS_HANDLER, keywordsHandler);
        jobDataMap.put(BODY, body);
        jobDataMap.put(CLIENT_ADAPTER, httpClientAdapterFactory.create(vesUrl));
        Tracing currentTracing = tracing.getIfAvailable();
        if (currentTracing != null) {
            jobDataMap.put(TRACER, currentTracing.tracer());
            TraceContext parentContext = currentTracing.currentTraceContext().get();
            if (parentContext != null) {
                jobDataMap.put(PARENT_TRACE_CONTEXT, parentContext);
            }
        }

        return JobBuilder
                .newJob(EventJob.class)
                .withDescription(templateName)
                .usingJobData(jobDataMap)
                .build();
    }

    private List<JobKey> getActiveJobsKeys() throws SchedulerException {
        return scheduler.getCurrentlyExecutingJobs()
                .stream()
                .map(JobExecutionContext::getJobDetail)
                .map(JobDetail::getKey)
                .collect(Collectors.toList());
    }
}
