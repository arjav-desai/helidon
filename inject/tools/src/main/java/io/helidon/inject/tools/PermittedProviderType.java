/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

package io.helidon.inject.tools;

import io.helidon.inject.api.InjectionPointProvider;

/**
 * Defines how the generator should allow the presence of {@link jakarta.inject.Provider}'s or
 * {@link InjectionPointProvider}'s. Since providers add a level of non-deterministic behavior
 * to the system it is required for the application to explicitly define whether this feature should be permitted.
 * @deprecated Helidon inject is deprecated and will be replaced in a future version
 */
@Deprecated(forRemoval = true, since = "4.0.8")
public enum PermittedProviderType {

    /**
     * No provider types are permitted.
     */
    NONE,

    /**
     * Each individual provider needs to be allow-listed.
     */
    NAMED,

    /**
     * Allows all/any provider type the system recognizes.
     */
    ALL

}
