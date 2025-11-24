/*-
 * ============LICENSE_START=======================================================
 * Simulator
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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

package org.onap.integration.simulators.nfsimulator.vesclient.rest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.onap.integration.simulators.nfsimulator.vesclient.db.Storage;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.TemplateRequest;
import org.onap.integration.simulators.nfsimulator.vesclient.template.Template;
import org.onap.integration.simulators.nfsimulator.vesclient.template.search.IllegalJsonValueException;
import org.onap.integration.simulators.nfsimulator.vesclient.rest.model.SearchExp;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/template")
@Api(tags = "Template controller", value = "Template controller")
public class TemplateController {
    static final String TEMPLATE_NOT_FOUND_MSG = "A template with given name does not exist";
    static final String CANNOT_OVERRIDE_TEMPLATE_MSG = "Cannot overwrite existing template. Use override=true to override";
    private final Storage<Template> service;

    public TemplateController(Storage<Template> service) {
        this.service = service;
    }

    @GetMapping("list")
    @ApiOperation(value = "Fetch all templates supported by Ves client")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "It returns list of supported templates.")
    })
    public ResponseEntity<List<Template>> list() {
        return new ResponseEntity<>(service.getAll(), HttpStatus.OK);
    }

    @GetMapping("get/{templateName}")
    @ApiOperation(value = "Fetch details about selected template")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "It returns an information about selected template.")
    })
    public ResponseEntity<String> get(@PathVariable String templateName) {
        Optional<Template> template = service.get(templateName);
        return template
            .map(this::createTemplateResponse)
            .orElse(this.createTemplateNotFoundResponse());
    }

    private ResponseEntity<String> createTemplateResponse(Template template) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(template),headers,  HttpStatus.OK);
    }

    private ResponseEntity<String> createTemplateNotFoundResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(TEMPLATE_NOT_FOUND_MSG, headers, HttpStatus.NOT_FOUND);
    }

    @PostMapping("upload")
    @ApiOperation(value = "Upload a new template")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Template uploaded")
    })
    public ResponseEntity<String> upload(
            @RequestBody @Valid TemplateRequest templateRequest,
            @RequestParam(required = false) boolean override) {
        String msg = "";
        HttpStatus status = HttpStatus.CREATED;
        Template template = new Template(templateRequest.getName(), templateRequest.getTemplate(), Instant.now().getNano());
        if (!service.tryPersistOrOverwrite(template, override)) {
            status = HttpStatus.CONFLICT;
            msg = CANNOT_OVERRIDE_TEMPLATE_MSG;
        }
        return new ResponseEntity<>(msg, status);
    }

    @PostMapping("search")
    @ApiOperation(value = "Fetch templates which fit to query")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns list of templates fitted to query.")
    })
    public ResponseEntity<List<String>> searchByCriteria(@RequestBody SearchExp queryJson) {
        try {
            List<String> templateNames = service.getIdsByContentCriteria(queryJson.getSearchExpr());
            return new ResponseEntity<>(templateNames, HttpStatus.OK);
        } catch (IllegalJsonValueException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Try again with correct parameters. Cause: %s", ex.getMessage()), ex);
        }

    }


}
