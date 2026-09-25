/*-
 * ============LICENSE_START=======================================================
 * Simulator
 * ================================================================================
 * Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.client.utils.ssl.SslAuthenticationHelper;
import org.springframework.beans.factory.ObjectProvider;

class HttpClientAdapterFactoryTest {

    private static final String TRACE_ID_HEADER = "X-B3-TraceId";

    private final AtomicReference<Headers> receivedHeaders = new AtomicReference<>();
    private HttpServer vesServer;
    private String vesUrl;

    @BeforeEach
    void setUp() throws IOException {
        vesServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        vesServer.createContext("/", exchange -> {
            receivedHeaders.set(exchange.getRequestHeaders());
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        vesServer.start();
        vesUrl = "http://localhost:" + vesServer.getAddress().getPort() + "/eventListener/v7";
    }

    @AfterEach
    void tearDown() {
        vesServer.stop(0);
    }

    @Test
    void shouldPropagateCurrentTraceToVesWhenTracingIsEnabled() throws IOException, GeneralSecurityException {
        Tracing tracing = Tracing.newBuilder().build();
        HttpClientAdapter adapter = createFactory(HttpTracing.create(tracing)).create(vesUrl);
        Span span = tracing.tracer().newTrace().start();

        try (Tracer.SpanInScope ignored = tracing.tracer().withSpanInScope(span)) {
            adapter.send("{}");
        } finally {
            span.finish();
            tracing.close();
        }

        assertThat(receivedHeaders.get().getFirst(TRACE_ID_HEADER)).isEqualTo(span.context().traceIdString());
    }

    @Test
    void shouldSendWithoutTraceHeadersWhenTracingIsDisabled() throws IOException, GeneralSecurityException {
        HttpClientAdapter adapter = createFactory(null).create(vesUrl);

        adapter.send("{}");

        assertThat(receivedHeaders.get()).isNotNull();
        assertThat(receivedHeaders.get().containsKey(TRACE_ID_HEADER)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private static HttpClientAdapterFactory createFactory(HttpTracing httpTracing) {
        ObjectProvider<HttpTracing> httpTracingProvider = mock(ObjectProvider.class);
        when(httpTracingProvider.getIfAvailable()).thenReturn(httpTracing);
        return new HttpClientAdapterFactory(new SslAuthenticationHelper(), httpTracingProvider);
    }
}
