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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.NonParameterKeywordPatterns.$nonParameterKeyword;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.SingleParameterKeywordPatterns.$singleParameterKeyword;
import static org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.TwoParameterKeywordPatterns.$twoParameterKeyword;

import io.vavr.API.Match.Pattern1;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.Keyword;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.NonParameterKeyword;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.SingleParameterKeyword;
import org.onap.integration.simulators.nfsimulator.vesclient.simulator.keywords.TwoParameterKeyword;
import org.springframework.stereotype.Component;

@Component
public class KeywordsExtractor {

    String substituteStringKeyword(String text, int increment) {
        return Match(text).of(
            Case(isRandomStringParamKeyword(),
                spk -> spk.substituteKeyword(KeywordsValueProvider.getRandomString().apply(spk.getAdditionalParameter()))
            ),
            Case(isRandomStringNonParamKeyword(),
                npk -> npk.substituteKeyword(KeywordsValueProvider.getRandomLimitedString().apply())
            ),
            Case(isRandomIntegerParamKeyword(),
                tpk -> tpk.substituteKeyword(KeywordsValueProvider.getRandomInteger().apply(
                    tpk.getAdditionalParameter1(),
                    tpk.getAdditionalParameter2()
                    )
                )
            ),
            Case(isRandomIntegerNonParamKeyword(),
                npk -> npk.substituteKeyword(KeywordsValueProvider.getRandomLimitedInteger().apply())
            ),
            Case(isIncrementKeyword(),
                ik -> ik.substituteKeyword(String.valueOf(increment))
            ),
            Case(isTimestampNonParamKeyword(),
                npk -> npk.substituteKeyword(KeywordsValueProvider.getEpochSecond().apply())
            ),
            Case(
                $(),
                () -> text
            ));
    }

    Long substitutePrimitiveKeyword(String text) {
        return Match(text).of(
            Case(isRandomPrimitiveIntegerParamKeyword(),
                tpk ->
                    KeywordsValueProvider.getRandomPrimitiveInteger().apply(tpk.getAdditionalParameter1(), tpk.getAdditionalParameter2())),
            Case(isTimestampPrimitiveNonParamKeyword(),
                tpk ->
                    KeywordsValueProvider.getTimestampPrimitive().apply()),
            Case(
                $(),
                () -> 0L
            ));
    }

    boolean isPrimitive(String text) {
        return Match(text).of(
            Case(isRandomPrimitiveIntegerParamKeyword(), () -> true),
            Case(isTimestampPrimitiveNonParamKeyword(), () -> true),
            Case($(), () -> false));
    }

    private Pattern1<String, SingleParameterKeyword> isRandomStringParamKeyword() {
        return $singleParameterKeyword($(spk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(spk, "RandomString")));
    }

    private Pattern1<String, NonParameterKeyword> isRandomStringNonParamKeyword() {
        return $nonParameterKeyword($(npk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(npk, "RandomString")));
    }

    private Pattern1<String, NonParameterKeyword> isIncrementKeyword() {
        return $nonParameterKeyword($(npk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(npk, "Increment")));
    }

    private Pattern1<String, TwoParameterKeyword> isRandomIntegerParamKeyword() {
        return $twoParameterKeyword($(tpk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(tpk, "RandomInteger")));
    }

    private Pattern1<String, TwoParameterKeyword> isRandomPrimitiveIntegerParamKeyword() {
        return $twoParameterKeyword($(tpk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(tpk, "RandomPrimitiveInteger")));
    }

    private Pattern1<String, NonParameterKeyword> isTimestampPrimitiveNonParamKeyword() {
        return $nonParameterKeyword($(npk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(npk, "TimestampPrimitive")));
    }

    private Pattern1<String, NonParameterKeyword> isRandomIntegerNonParamKeyword() {
        return $nonParameterKeyword($(npk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(npk, "RandomInteger")));
    }

    private Pattern1<String, NonParameterKeyword> isTimestampNonParamKeyword() {
        return $nonParameterKeyword($(npk -> Keyword.IS_MATCHING_KEYWORD_NAME.apply(npk, "Timestamp")));
    }

}
