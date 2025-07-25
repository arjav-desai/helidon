/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
 * Code generation for Langchain4j AI services.
 */
module io.helidon.integrations.langchain4j.codegen {
    requires transitive io.helidon.codegen;
    requires io.helidon.service.codegen;
    requires io.helidon.common.types;
    requires io.helidon.codegen.classmodel;
    requires io.helidon.common;

    exports io.helidon.integrations.langchain4j.codegen;

    provides io.helidon.codegen.spi.CodegenExtensionProvider
            with io.helidon.integrations.langchain4j.codegen.AiServiceCodegenProvider,
                    io.helidon.integrations.langchain4j.codegen.ModelFactoryCodegenProvider,
                    io.helidon.integrations.langchain4j.codegen.ModelConfigCodegenProvider;

    provides io.helidon.codegen.spi.TypeMapperProvider
            with io.helidon.integrations.langchain4j.codegen.LcToolsMapperProvider;
}