/*-
 * ============LICENSE_START=======================================================
 * Simulator
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
package org.onap.integration.simulators.nfsimulator.vesclient.simulator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.integration.simulators.nfsimulator.vesclient.template.Template;
import org.onap.integration.simulators.nfsimulator.vesclient.template.TemplateService;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbTemplateReaderTest {

    public static final String SOME_TEMPLATE = "someTemplate";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final long LMOD = 10L;
    private TemplateService service;
    private DbTemplateReader dbTemplateReader;

    @BeforeEach
    void setUp() {
        this.service = mock(TemplateService.class);
        this.dbTemplateReader = new DbTemplateReader(this.service, new Gson());
    }

    @Test
    public void shouldReportErrorWhenTemplateDoesNotExistInTemplateService() {
        // given
        when(this.service.get(SOME_TEMPLATE)).thenReturn(Optional.empty());

        // when/then
        assertThrows(IOException.class,
            () -> this.dbTemplateReader.readTemplate(SOME_TEMPLATE)
        );
    }

    @Test
    public void shouldReturnTemplateFromService() throws IOException {
        // given
        Template template = givenTemplate(SOME_TEMPLATE);
        when(this.service.get(SOME_TEMPLATE)).thenReturn(Optional.of(template));

        // when
        final JsonObject someTemplate = this.dbTemplateReader.readTemplate(SOME_TEMPLATE);

        // then
        Assertions.assertThat(someTemplate).isNotNull();
        Assertions.assertThat(someTemplate.get(KEY).getAsString()).isEqualTo(VALUE);
    }

    private Template givenTemplate(String templateName) {
        return new Template(templateName, new Document(KEY, VALUE), LMOD);
    }
}
