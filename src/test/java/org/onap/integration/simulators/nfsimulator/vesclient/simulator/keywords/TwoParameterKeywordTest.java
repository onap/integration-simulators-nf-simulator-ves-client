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

package org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords;


import io.vavr.Tuple1;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class TwoParameterKeywordTest {
    @Test
    public void whenGivenKeywordShouldReturnTwoParameterKeywordObjectWithParsedValues() {
        //given
        final String expectedName = "TEST";
        final Integer expectedParam1 = 123;
        final Integer expectedParam2 = 456;

        String keyword = "#" + expectedName + "(" + expectedParam1 + "," + expectedParam2 + ")";

        //when
        Tuple1<TwoParameterKeyword> keywordTuple = TwoParameterKeyword.twoParameterKeyword(keyword);
        TwoParameterKeyword twoParameterKeyword = keywordTuple._1();

        //then
        assertEquals(twoParameterKeyword.getName(), expectedName);
        assertEquals(twoParameterKeyword.getAdditionalParameter1(), expectedParam1);
        assertEquals(twoParameterKeyword.getAdditionalParameter2(), expectedParam2);
    }
}
