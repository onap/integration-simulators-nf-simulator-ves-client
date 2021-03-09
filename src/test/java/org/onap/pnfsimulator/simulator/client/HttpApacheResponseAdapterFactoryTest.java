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

package org.onap.pnfsimulator.simulator.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.onap.pnfsimulator.simulator.client.HttpTestUtils.createMockedHttpEntity;
import static org.onap.pnfsimulator.simulator.client.HttpTestUtils.createStatusLine;

class HttpApacheResponseAdapterFactoryTest {

    private HttpResponse httpResponse;

    @BeforeEach
    void setup() {
        httpResponse = mock(HttpResponse.class);
    }

    @Test
    void shouldCreateCorrectHttpResponseAdapterFromApacheHttpAcceptedResponse() throws IOException {
        // given
        final int responseCode = HttpStatus.SC_ACCEPTED;
        final String responseBody = HttpTestUtils.HTTP_MESSAGE_ACCEPTER;
        prepareHttpResponseMock(responseCode, createMockedHttpEntity(responseBody));

        // when
        HttpResponseAdapter httpResponseAdapter = new HttpApacheResponseAdapterFactory().create(httpResponse);

        // then
        assertHttpResponseIsCorrect(responseCode, responseBody, httpResponseAdapter);
    }


    @Test
    void shouldCreateCorrectHttpResponseAdapterFromApacheHttpForbiddenResponse() throws IOException {
        // given
        final int responseCode = HttpStatus.SC_FORBIDDEN;
        final String responseBody = HttpTestUtils.HTTP_MESSAGE_FORBIDDEN;
        prepareHttpResponseMock(responseCode, createMockedHttpEntity(responseBody));

        // when
        HttpResponseAdapter httpResponseAdapter = new HttpApacheResponseAdapterFactory().create(httpResponse);

        // then
        assertHttpResponseIsCorrect(responseCode, responseBody, httpResponseAdapter);
    }

    @Test
    void shouldCreateCorrectHttpResponseAdapterFromApacheHttpResponseWithEmptyEntity() {
        // given
        final int responseCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        prepareHttpResponseMock(responseCode, null);

        // when
        HttpResponseAdapter httpResponseAdapter = new HttpApacheResponseAdapterFactory().create(httpResponse);


        assertHttpResponseIsCorrect(responseCode, "", httpResponseAdapter);
    }

    private void prepareHttpResponseMock(int responseCode, HttpEntity httpEntity) {
        doReturn(createStatusLine(responseCode)).when(httpResponse).getStatusLine();
        doReturn(httpEntity).when(httpResponse).getEntity();
    }

    private void assertHttpResponseIsCorrect(int responseCode, String responseBody, HttpResponseAdapter httpResponseAdapter) {
        assertEquals(responseCode, httpResponseAdapter.getCode());
        assertEquals(responseBody, httpResponseAdapter.getMessage());
    }

}
