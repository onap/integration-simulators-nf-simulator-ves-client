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

package org.onap.integration.simulators.nfsimulator.vesclient.integration;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class TemplateSyncTest {

    private static final long SYNC_TIMEOUT_MILLIS = 10000;
    private static final long POLL_INTERVAL_MILLIS = 200;

    private String templateName;
    private Path templateFile;

    @Before
    public void setUp() {
        templateName = UUID.randomUUID() + ".json";
        templateFile = Paths.get("..", "templates", templateName);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(templateFile);
    }

    @Test
    public void whenTemplateFileIsModifiedTemplateShouldBeUpdated() throws Exception {
        writeTemplateFile("{\"field\": \"initial\"}");
        assertThat(awaitTemplate(response -> hasField(response, "initial"))).isTrue();

        writeTemplateFile("{\"field\": \"modified\"}");

        assertThat(awaitTemplate(response -> hasField(response, "modified"))).isTrue();
    }

    @Test
    public void whenTemplateFileIsDeletedTemplateShouldBeRemoved() throws Exception {
        writeTemplateFile("{\"field\": \"initial\"}");
        assertThat(awaitTemplate(response -> hasField(response, "initial"))).isTrue();

        Files.delete(templateFile);

        assertThat(awaitTemplate(response -> response.statusCode() == HttpStatus.NOT_FOUND.value())).isTrue();
    }

    private static boolean hasField(Response response, String value) {
        return response.statusCode() == HttpStatus.OK.value() && value.equals(response.path("content.field"));
    }

    private void writeTemplateFile(String content) throws IOException {
        Files.write(templateFile, content.getBytes(StandardCharsets.UTF_8));
    }

    private boolean awaitTemplate(Predicate<Response> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + SYNC_TIMEOUT_MILLIS;
        while (!condition.test(getTemplate())) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        return true;
    }

    private Response getTemplate() {
        return when().get("http://0.0.0.0:5000/template/get/" + templateName);
    }
}
