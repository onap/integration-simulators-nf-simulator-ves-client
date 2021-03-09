/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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

package org.onap.pnfsimulator.simulator.client.utils.ssl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SSLContextFactoryTest {
    private CertificateReader certificateReaderMock;
    private CertAuthSslContextFactory certAuthSslContextFactory;
    private SSLContextFactory sslContextFactory;

    @BeforeEach
    void setup() {
        certificateReaderMock = mock(CertificateReader.class);
        certAuthSslContextFactory = new CertAuthSslContextFactory(certificateReaderMock);
        sslContextFactory = new SSLContextFactory(certAuthSslContextFactory);
    }

    @Test
    void shouldSuccessfullyCreateTrustAlwaysSSLContext() throws GeneralSecurityException, IOException {
        // given, when, then
        assertNotNull(sslContextFactory.createTrustAlways());
        verify(certificateReaderMock, times(0)).read(any(), any(), any());
    }

    @Test
    void shouldSuccessfullyCreateSSLContext() throws GeneralSecurityException, IOException {
        // given, when, then
        assertNotNull(sslContextFactory.create(new SslAuthenticationHelper()));
        verify(certificateReaderMock, times(2)).read(any(), any(), any());
    }

}

