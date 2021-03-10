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
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;

class CertAuthSslContextFactory {

    private final CertificateReader certificateReader;

    CertAuthSslContextFactory(CertificateReader certificateReader) {
        this.certificateReader = certificateReader;
    }

    SSLContext createSslContext(SslAuthenticationHelper sslAuthenticationHelper)
        throws GeneralSecurityException, IOException {
        final String keystorePasswordPath = sslAuthenticationHelper.getClientCertificatePasswordPath();

        final KeyStore keystore = certificateReader.read(sslAuthenticationHelper.getClientCertificatePath(),
            keystorePasswordPath, "PKCS12");
        final KeyStore truststore = certificateReader.read(sslAuthenticationHelper.getTrustStorePath(),
            sslAuthenticationHelper.getTrustStorePasswordPath(), "JKS");

        return SSLContexts.custom()
            .loadKeyMaterial(keystore, certificateReader.readPassword(keystorePasswordPath))
            .loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
            .build();
    }

}
