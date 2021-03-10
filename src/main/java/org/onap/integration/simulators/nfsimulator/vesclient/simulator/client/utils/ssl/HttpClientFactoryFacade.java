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

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.apache.http.client.HttpClient;

public class HttpClientFactoryFacade {

    private HttpClientFactoryFacade() {
    }

    private static final CertificateReader CERTIFICATE_READER = new CertificateReader();
    private static final CertAuthSslContextFactory CERT_AUTH_SSL_CONTEXT_FACTORY = new CertAuthSslContextFactory(CERTIFICATE_READER);
    private static final SSLContextFactory SSL_CONTEXT_FACTORY = new SSLContextFactory(CERT_AUTH_SSL_CONTEXT_FACTORY);
    private static final HttpClientFactory HTTP_CLIENT_FACTORY = new HttpClientFactory(SSL_CONTEXT_FACTORY);

    public static HttpClient create(String url, SslAuthenticationHelper sslAuthenticationHelper) throws GeneralSecurityException, IOException {
        return HTTP_CLIENT_FACTORY.create(url, sslAuthenticationHelper);
    }
}
