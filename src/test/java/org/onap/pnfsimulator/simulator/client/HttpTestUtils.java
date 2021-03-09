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
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class HttpTestUtils {

    private HttpTestUtils() {
    }

    public static final String HTTP_MESSAGE_ACCEPTER = "Accepted";
    public static final String HTTP_MESSAGE_FORBIDDEN = "Forbidden";

    static HttpEntity createMockedHttpEntity(String responseBody) throws IOException {
        HttpEntity httpEntity = mock(HttpEntity.class);
        doReturn(new ByteArrayInputStream(responseBody.getBytes())).when(httpEntity).getContent();
        return httpEntity;
    }

    static BasicStatusLine createStatusLine(int responseCode) {
        return new BasicStatusLine(
            new ProtocolVersion("1.0.0", 1, 0),
            responseCode,
            ""
        );
    }

}
