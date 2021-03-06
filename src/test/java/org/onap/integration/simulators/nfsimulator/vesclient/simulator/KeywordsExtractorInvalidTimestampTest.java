/*
 * ============LICENSE_START=======================================================
 * PNF-REGISTRATION-HANDLER
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class KeywordsExtractorInvalidTimestampTest {

    private final String keyword;
    private KeywordsExtractor keywordsExtractor;

    private static final Collection INVALID_TIMESTAMP_KEYWORDS = Arrays.asList(new Object[][]{
        {"#Timesamp"},
        {"#Timestamp(10)"},
        {"#timestamp"},
        {"#Timestamp(11,13)"},
        {"Timestamp"}
    });

    public KeywordsExtractorInvalidTimestampTest(String keyword) {
        this.keyword = keyword;
    }

    @Before
    public void setUp() {
        this.keywordsExtractor = new KeywordsExtractor();
    }

    @Parameterized.Parameters
    public static Collection data() {
        return INVALID_TIMESTAMP_KEYWORDS;
    }

    @Test
    public void checkValidRandomStringKeyword() {
        assertEquals(keywordsExtractor.substituteStringKeyword(this.keyword, 1), this.keyword);
    }

}
