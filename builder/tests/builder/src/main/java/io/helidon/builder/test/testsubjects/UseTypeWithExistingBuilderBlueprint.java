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

package io.helidon.builder.test.testsubjects;

import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface UseTypeWithExistingBuilderBlueprint {
    // this should not fail, as even though there is a builder method, the returned type is not our builder
    ExistingTypeWithBuilder typeWithBuilder();

    // this should create consumer methods
    ExistingTypeWithNiceBuilder typeWithNiceBuilder();
}
