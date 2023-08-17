/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

/**
 * Injection maven-plugin module.
 */
module io.helidon.inject.maven.plugin {
    requires maven.plugin.annotations;
    requires maven.plugin.api;
    requires maven.project;
    requires maven.artifact;
    requires maven.model;
    requires io.helidon.builder.api;
    requires io.helidon.inject.configdriven.runtime;
    requires io.helidon.common;
    requires io.helidon.config;
    requires transitive io.helidon.inject.tools;

    exports io.helidon.inject.maven.plugin;

    uses io.helidon.inject.tools.spi.ActivatorCreator;
    uses io.helidon.inject.tools.spi.ApplicationCreator;
    uses io.helidon.inject.tools.spi.ExternalModuleCreator;
}