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

package org.onap.integration.simulators.nfsimulator.vesclient.simulator.client;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.utils.ssl.HttpClientFactoryFacade;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.utils.ssl.SslAuthenticationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;

import static org.onap.integration.simulators.nfsimulator.vesclient.logging.MdcVariables.REQUEST_ID;
import static org.onap.integration.simulators.nfsimulator.vesclient.logging.MdcVariables.X_INVOCATION_ID;
import static org.onap.integration.simulators.nfsimulator.vesclient.logging.MdcVariables.X_ONAP_REQUEST_ID;

public class HttpClientAdapterImpl implements HttpClientAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientAdapterImpl.class);
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final Marker INVOKE = MarkerFactory.getMarker("INVOKE");
    private static final HttpApacheResponseAdapterFactory responseFactory = new HttpApacheResponseAdapterFactory();
    private final HttpClient client;
    private final String targetUrl;

    public HttpClientAdapterImpl(String targetUrl, SslAuthenticationHelper sslAuthenticationHelper)
        throws IOException, GeneralSecurityException {
        this.client = HttpClientFactoryFacade.create(targetUrl, sslAuthenticationHelper);
        this.targetUrl = targetUrl;
    }

    HttpClientAdapterImpl(HttpClient client, String targetUrl) {
        this.client = client;
        this.targetUrl = targetUrl;
    }

    @Override
    public HttpResponseAdapter send(String content) {
        HttpResponseAdapter vesResponse;
        try {
            HttpResponse response = sendAndRetrieve(content);
            LOGGER.info(INVOKE, "Message sent, ves response code: {}", response.getStatusLine());
            vesResponse = responseFactory.create(response);
            EntityUtils.consumeQuietly(response.getEntity()); //response has to be fully consumed otherwise apache won't release connection
        } catch (IOException | URISyntaxException e) {
            LOGGER.warn("Error sending message to ves: {}", e.getMessage(), e.getCause());
            vesResponse = new HttpResponseAdapter(421, String.format("Fail to connect with ves: %s", e.getMessage()));
        }
        return vesResponse;
    }

    private HttpResponse sendAndRetrieve(String content) throws IOException, URISyntaxException {
        HttpPost request = createRequest(content);
        HttpResponse httpResponse = client.execute(request);
        request.releaseConnection();
        return httpResponse;
    }

    private HttpPost createRequest(String content) throws UnsupportedEncodingException, URISyntaxException {
        LOGGER.info("sending request using address: {}", this.targetUrl);
        URI targetAddress = new URI(this.targetUrl);
        HttpPost request = new HttpPost(targetAddress);
        if(urlContainsUserInfo(targetAddress)) {
            request.addHeader(HttpHeaders.AUTHORIZATION, getAuthenticationHeaderForUser(targetAddress.getUserInfo()));
        }
        StringEntity stringEntity = new StringEntity(content);
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON);
        request.addHeader(X_ONAP_REQUEST_ID, MDC.get(REQUEST_ID));
        request.addHeader(X_INVOCATION_ID, UUID.randomUUID().toString());
        request.setEntity(stringEntity);
        return request;
    }

    private boolean urlContainsUserInfo(URI targetAddress) {
        return targetAddress.getUserInfo() != null && !targetAddress.getUserInfo().isEmpty();
    }

    private String getAuthenticationHeaderForUser(String userInfo) {
        final byte[] encodedUserInfo = Base64.encodeBase64(
            userInfo.getBytes(StandardCharsets.ISO_8859_1)
        );
        return String.format("Basic %s", new String(encodedUserInfo));
    }

}
