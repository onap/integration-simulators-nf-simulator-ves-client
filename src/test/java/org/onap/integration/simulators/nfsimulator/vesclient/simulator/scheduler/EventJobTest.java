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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.KeywordsExtractor;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.KeywordsHandler;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.HttpClientAdapter;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

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
