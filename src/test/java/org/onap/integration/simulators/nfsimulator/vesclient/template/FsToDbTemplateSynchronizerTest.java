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
package org.onap.integration.simulators.nfsimulator.vesclient.template;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FsToDbTemplateSynchronizerTest {


    @Test
    public void shouldReturnErrorSynchronizedMessage() {
        //given
        FsToDbTemplateSynchronizer fsToDbTemplateSynchronizer = new FsToDbTemplateSynchronizer("someInvalidValue", null);
        Logger logger = (Logger) LoggerFactory.getLogger(FsToDbTemplateSynchronizer.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        //when
        fsToDbTemplateSynchronizer.synchronize();
        List<ILoggingEvent> logsList = listAppender.list;
        //then
        assertEquals("Cannot synchronize templates. Check whether the proper folder exists.", logsList.get(0)
                .getMessage());
        assertEquals(Level.ERROR, logsList.get(0)
                .getLevel());
    }
}
