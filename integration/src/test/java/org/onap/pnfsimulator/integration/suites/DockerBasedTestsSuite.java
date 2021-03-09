/*-
 * ============LICENSE_START=======================================================
 * Simulator
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights reserved.
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

package org.onap.pnfsimulator.integration.suites;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.onap.pnfsimulator.integration.BasicAvailabilityTest;
import org.onap.pnfsimulator.integration.OptionalTemplatesTest;
import org.onap.pnfsimulator.integration.SearchInTemplatesTest;
import org.onap.pnfsimulator.integration.SingleEventTest;
import org.onap.pnfsimulator.integration.TemplatesManagementTest;
import org.onap.pnfsimulator.integration.VariablesReplacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

@RunWith(Suite.class)
@SuiteClasses({BasicAvailabilityTest.class, TemplatesManagementTest.class, OptionalTemplatesTest.class,
    SearchInTemplatesTest.class, VariablesReplacement.class, SingleEventTest.class})
public class DockerBasedTestsSuite {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerBasedTestsSuite.class);

    private static final String HEALTH_CHECK_ADDRESS = "http://0.0.0.0:5000/health";
    private static final int RETRY_COUNT = 10;
    private static final int RETRY_INTERVAL = 1000;

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("../docker-compose.yml")
        .waitingForService("pnf-simulator", HealthChecks.toHaveAllPortsOpen())
        .waitingForService("mongo", HealthChecks.toHaveAllPortsOpen())
        .build();

    @BeforeClass
    public static void waitForPnfSimulatorToBeHealthy() throws InterruptedException {
        boolean isHealthy = false;
        int retry = 0;
        while (!isHealthy && retry < RETRY_COUNT) {
            retry++;
            LOGGER.info("Checking PNF health, try {} out of {}", retry, RETRY_COUNT);
            isHealthy = performHealthCheck();
            if (isHealthy) {
                LOGGER.info("PNF is healthy");
            } else {
                LOGGER.info("PNF no healthy retrying in  {}", RETRY_COUNT);
                Thread.sleep(RETRY_INTERVAL);
            }
        }
    }

    private static boolean performHealthCheck() {
        boolean isUp = false;
        try {
            int statusCode = given().get(HEALTH_CHECK_ADDRESS).getStatusCode();
            if (statusCode == HttpStatus.OK.value()) {
                isUp = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isUp;
    }

}
