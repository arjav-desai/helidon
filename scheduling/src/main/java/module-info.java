/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

import io.helidon.common.features.api.Features;
import io.helidon.common.features.api.HelidonFlavor;

/**
 * Scheduling module for Helidon reactive implementation.
 */
@Features.Name("Scheduling")
@Features.Description("Scheduling of periodical tasks")
@Features.Flavor(HelidonFlavor.SE)
@Features.Path("Scheduling")
module io.helidon.scheduling {

    requires com.cronutils;
    requires io.helidon.common.config;
    requires io.helidon.common.configurable;
    requires io.helidon.builder.api;

    requires static io.helidon.common.features.api;
    requires io.helidon.service.registry;

    exports io.helidon.scheduling;

}