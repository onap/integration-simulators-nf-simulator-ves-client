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

package org.onap.integration.simulators.nfsimulator.vesclient;

import javax.annotation.PostConstruct;

import org.onap.integration.simulators.nfsimulator.vesclient.template.FsToDbTemplateSynchronizer;
import org.onap.integration.simulators.nfsimulator.vesclient.filesystem.WatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Main {

    private final WatcherService watcherService;
    private final FsToDbTemplateSynchronizer fsToDbTemplateSynchronizer;

    @Autowired
    public Main(WatcherService watcherService,
                FsToDbTemplateSynchronizer fsToDbTemplateSynchronizer) {
        this.watcherService = watcherService;
        this.fsToDbTemplateSynchronizer = fsToDbTemplateSynchronizer;
    }

    // We are excluding this line in Sonar due to fact that
    // Spring is handling arguments
    public static void main(String[] args) { // NOSONAR
        SpringApplication.run(Main.class, args);
    }

    @PostConstruct
    public void createWatchers() {
        fsToDbTemplateSynchronizer.synchronize();
        watcherService.createWatcher();
    }
}


