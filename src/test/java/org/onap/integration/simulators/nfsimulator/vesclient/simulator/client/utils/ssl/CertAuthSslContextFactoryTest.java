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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CertAuthSslContextFactoryTest {

    private static final String CERTIFICATES_DIRECTORY = "src/test/resources/certificates/";

    private static final String KEYSTORE_FILENAME = "client.p12";
    private static final String VALID_KEYSTORE_PASSWORD_FILENAME = "client.pass";
    private static final String INVALID_KEYSTORE_PASSWORD_FILENAME = "client_invalid.pass";

    private static final String TRUSTSTORE_FILENAME = "truststore";
    private static final String VALID_TRUSTSTORE_PASSWORD_FILENAME = "truststore.pass";
    private static final String INVALID_TRUSTSTORE_PASSWORD_FILENAME = "truststore_invalid.pass";

    private static final String NON_EXISTING_PASSWORD_FILENAME = "non_existing.pass";
    private static final String PASSWORD_INCORRECT = "password was incorrect";

    private CertAuthSslContextFactory certAuthSslContextFactory;

    @Before
    public void setup() {
        this.certAuthSslContextFactory = new CertAuthSslContextFactory(new CertificateReader());
    }

    @Test
    public void shouldCreateSslContextSuccessfully_whenValidPasswordsUsed()
        throws GeneralSecurityException, IOException {
        // Given
        final SslAuthenticationHelper sslAuthenticationHelper = mockSslAuthenticationHelperWithFiles(
            VALID_KEYSTORE_PASSWORD_FILENAME, VALID_TRUSTSTORE_PASSWORD_FILENAME);

        // When
        final SSLContext sslContext = certAuthSslContextFactory.createSslContext(sslAuthenticationHelper);

        // Then
        assertNotNull(sslContext);
    }

    @Test
    public void shouldThrowIOException_whenInvalidKeystorePasswordUsed() {
        // Given
        final SslAuthenticationHelper sslAuthenticationHelper = mockSslAuthenticationHelperWithFiles(
            INVALID_KEYSTORE_PASSWORD_FILENAME, VALID_TRUSTSTORE_PASSWORD_FILENAME);

        // When
        final IOException exception = assertThrows(IOException.class,
            () -> certAuthSslContextFactory.createSslContext(sslAuthenticationHelper));

        // Then
        assertThat(exception.getMessage(), CoreMatchers.containsString(PASSWORD_INCORRECT));
    }

    @Test
    public void shouldThrowIOException_whenInvalidTruststorePasswordUsed() {
        // Given
        final SslAuthenticationHelper sslAuthenticationHelper = mockSslAuthenticationHelperWithFiles(
            VALID_KEYSTORE_PASSWORD_FILENAME, INVALID_TRUSTSTORE_PASSWORD_FILENAME);

        // When
        final IOException exception = assertThrows(IOException.class,
            () -> certAuthSslContextFactory.createSslContext(sslAuthenticationHelper));

        // Then
        assertThat(exception.getMessage(), CoreMatchers.containsString(PASSWORD_INCORRECT));
    }

    @Test
    public void shouldThrowNoSuchFileException_whenInvalidKeystoreFilePath() {
        final SslAuthenticationHelper sslAuthenticationHelper = mockSslAuthenticationHelperWithFiles(
            NON_EXISTING_PASSWORD_FILENAME, INVALID_TRUSTSTORE_PASSWORD_FILENAME);

        // When, Then
        assertThrows(NoSuchFileException.class,
            () -> certAuthSslContextFactory.createSslContext(sslAuthenticationHelper));
    }

    @Test
    public void shouldThrowNoSuchFileException_whenInvalidTruststoreFilePath() {
        // Given
        final SslAuthenticationHelper sslAuthenticationHelper = mockSslAuthenticationHelperWithFiles(
            VALID_KEYSTORE_PASSWORD_FILENAME, NON_EXISTING_PASSWORD_FILENAME);

        // When, Then
        assertThrows(NoSuchFileException.class,
            () -> certAuthSslContextFactory.createSslContext(sslAuthenticationHelper));
    }

    private SslAuthenticationHelper mockSslAuthenticationHelperWithFiles(String keystorePasswordFilename,
        String truststorePasswordFilename) {
        final SslAuthenticationHelper sslAuthenticationHelper = Mockito.mock(SslAuthenticationHelper.class);

        when(sslAuthenticationHelper.getClientCertificatePath())
            .thenReturn(getPath(KEYSTORE_FILENAME));
        when(sslAuthenticationHelper.getClientCertificatePasswordPath())
            .thenReturn(getPath(keystorePasswordFilename));
        when(sslAuthenticationHelper.getTrustStorePath())
            .thenReturn(getPath(TRUSTSTORE_FILENAME));
        when(sslAuthenticationHelper.getTrustStorePasswordPath())
            .thenReturn(getPath(truststorePasswordFilename));

        return sslAuthenticationHelper;
    }

    private String getPath(String fileName) {
        return CERTIFICATES_DIRECTORY + fileName;
    }
}
