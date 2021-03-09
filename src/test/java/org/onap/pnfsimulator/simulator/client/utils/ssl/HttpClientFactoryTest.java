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

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpClientFactoryTest {
    private static final String HTTPS_URL = "https://example.com";
    private static final String HTTP_URL = "http://example.com";

    private SSLContextFactory sslContextFactoryMock;
    private HttpClientFactory httpClientFactory;
    private SslAuthenticationHelper sslAuthenticationHelper;

    @BeforeEach
    public void setup() {
        sslContextFactoryMock = mock(SSLContextFactory.class);
        httpClientFactory = new HttpClientFactory(sslContextFactoryMock);
        sslAuthenticationHelper = new SslAuthenticationHelper();
    }

    @Test
    void shouldCreateHttpsClient_whenClientCertificationDisabled() throws GeneralSecurityException, IOException {
        // given
        sslAuthenticationHelper.setClientCertificateEnabled(false);

        // when
        final var httpClient = httpClientFactory.create(HTTPS_URL, sslAuthenticationHelper);

        // then
        assertNotNull(httpClient);
        verifySslContextFactoryMockCalls(0, 1);
    }

    @Test
    void shouldCreateHttpsClient_whenClientCertificationDisabled_AndCannotCreateTrustAlwaysSslContext() throws GeneralSecurityException, IOException {
        // given
        sslAuthenticationHelper.setClientCertificateEnabled(false);
        when(sslContextFactoryMock.createTrustAlways()).thenThrow(KeyStoreException.class);

        // when
        final var httpClient = httpClientFactory.create(HTTPS_URL, sslAuthenticationHelper);

        // then
        assertNotNull(httpClient);
        verifySslContextFactoryMockCalls(0, 1);
    }

    @Test
    void shouldCreateHttpClient_whenClientCertificationDisabled() throws GeneralSecurityException, IOException {
        // given
        sslAuthenticationHelper.setClientCertificateEnabled(false);

        // when
        final var httpClient = httpClientFactory.create(HTTP_URL, sslAuthenticationHelper);

        // then
        assertNotNull(httpClient);
        verifySslContextFactoryMockCalls(0, 0);
    }


    @Test
    void shouldCreateHttpClient_whenClientCertificationAndStrictHostnameVerificationAreEnabled() throws GeneralSecurityException, IOException {
        // given
        sslAuthenticationHelper.setClientCertificateEnabled(true);
        sslAuthenticationHelper.setStrictHostnameVerification(true);

        // when
        final var httpClient = httpClientFactory.create(HTTP_URL, sslAuthenticationHelper);

        // then
        assertNotNull(httpClient);
        verifySslContextFactoryMockCalls(1, 0);
    }

    @Test
    void shouldCreateHttpClient_whenClientCertificationEnabledAndStrictHostnameVerificationDisabled() throws GeneralSecurityException, IOException {
        // given
        sslAuthenticationHelper.setClientCertificateEnabled(true);
        sslAuthenticationHelper.setStrictHostnameVerification(false);

        // when
        final var httpClient = httpClientFactory.create(HTTP_URL, sslAuthenticationHelper);

        // then
        assertNotNull(httpClient);
        verifySslContextFactoryMockCalls(1, 0);
    }

    @Test
    void shouldThrowMalformedURLException_whenInvalidUrl() throws GeneralSecurityException, IOException {
        // given
        var invalidUrl = "invalid";

        // when
        final var exception = assertThrows(MalformedURLException.class,
                () -> httpClientFactory.create(invalidUrl, sslAuthenticationHelper));

        // then
        assertThat(exception.getMessage(), CoreMatchers.containsString("invalid"));
    }

    private void verifySslContextFactoryMockCalls(int createCalls, int createTrustAlwaysCalls) throws GeneralSecurityException, IOException {
        verify(sslContextFactoryMock, times(createCalls)).create(any());
        verify(sslContextFactoryMock, times(createTrustAlwaysCalls)).createTrustAlways();
    }

}
