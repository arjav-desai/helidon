/*
 * Copyright (c) 2019, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.tests.integration.packaging.mp1;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.helidon.security.abac.scope.ScopeValidator;

/**
 * A resource to test.
 */
@Path("/")
public class JaxRsProtectedResource {
    @GET
    @Path("/public")
    @PermitAll
    public String publicHello() {
        return "Hello anybody";
    }

    @GET
    @Path("/scope")
    @ScopeValidator.Scope("admin_scope")
    public String scope() {
        return "Hello scope";
    }

    @GET
    @Path("/role")
    @RolesAllowed("admin")
    public String role() {
        return "Hello role";
    }
}
