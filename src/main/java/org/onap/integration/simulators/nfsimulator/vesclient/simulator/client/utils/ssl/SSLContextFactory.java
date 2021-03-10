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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;

class SSLContextFactory {
    private static final TrustStrategy TRUST_STRATEGY_ALWAYS = new TrustAllStrategy();

    private final CertAuthSslContextFactory certAuthSslContextFactory;

    SSLContextFactory(CertAuthSslContextFactory certAuthSslContextFactory) {
        this.certAuthSslContextFactory = certAuthSslContextFactory;
    }
    SSLContext create(SslAuthenticationHelper sslAuthenticationHelper) throws GeneralSecurityException, IOException {
        return certAuthSslContextFactory.createSslContext(sslAuthenticationHelper);
    }

    SSLContext createTrustAlways() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return SSLContextBuilder.create().loadTrustMaterial(TRUST_STRATEGY_ALWAYS).build();
    }

}
