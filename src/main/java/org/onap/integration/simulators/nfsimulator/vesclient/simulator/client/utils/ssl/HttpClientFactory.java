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

package org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.utils.ssl;

import io.vavr.control.Try;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

class HttpClientFactory {
    private static final int CONNECTION_TIMEOUT = 1000;
    private static final RequestConfig CONFIG = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT)
            .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
            .setSocketTimeout(CONNECTION_TIMEOUT)
            .build();
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientFactory.class);
    private final SSLContextFactory sslContextFactory;

    HttpClientFactory(SSLContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
    }

    HttpClient create(String url, SslAuthenticationHelper sslAuthenticationHelper) throws GeneralSecurityException, IOException {
        HttpClient client;
        if (!sslAuthenticationHelper.isClientCertificateEnabled()) {
            client = "https".equals(new URL(url).getProtocol()) ? createForHttps() : createBasic();
        } else if (sslAuthenticationHelper.isStrictHostnameVerification()) {
            client = createSecured(sslContextFactory.create(sslAuthenticationHelper), new DefaultHostnameVerifier());
        } else {
            client = createSecured(sslContextFactory.create(sslAuthenticationHelper), new NoopHostnameVerifier());
        }
        return client;
    }

    private HttpClient createForHttps() {
        return Try.of(this::createSecuredTrustAlways)
                .onFailure(this::logErrorMessage)
                .getOrElse(createBasic());
    }

    private void logErrorMessage(Throwable e) {
        String message = String.format(
                "Could not initialize client due to SSL exception: %s. " +
                        "Default client without SSL support will be used instead." +
                        "\nCause: %s",
                e.getMessage(),
                e.getCause()
        );
        LOGGER.error(message, e);
    }


    private HttpClient createBasic() {
        return HttpClientBuilder
                .create()
                .setDefaultRequestConfig(CONFIG)
                .build();
    }

    private HttpClient createSecuredTrustAlways() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return createSecured(sslContextFactory.createTrustAlways(), new NoopHostnameVerifier());
    }

    private HttpClient createSecured(SSLContext trustAlways, HostnameVerifier hostnameVerifier) {
        return HttpClients.custom()
                .setSSLContext(trustAlways)
                .setDefaultRequestConfig(CONFIG)
                .setSSLHostnameVerifier(hostnameVerifier)
                .build();
    }
}
