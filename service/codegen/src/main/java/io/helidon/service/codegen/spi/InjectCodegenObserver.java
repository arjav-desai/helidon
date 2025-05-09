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

package io.helidon.service.codegen.spi;

import java.util.Set;

import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.codegen.RegistryRoundContext;

/**
 * Processes events from inject extension.
 */
public interface InjectCodegenObserver {

    /**
     * Called after a processing event that occurred in the codegen extension.
     *
     * @param roundContext context of the current processing round
     * @param elements     all elements of interest
     */
    default void onProcessingEvent(RegistryRoundContext roundContext, Set<TypedElementInfo> elements) {
    }

    /**
     * Called for each injection point.
     * In case the injection point is a field, the {@code element} and {@code argument} are the same instance.
     *
     * @param roundContext context of the current processing round
     * @param service      the service being processed
     * @param element      element that owns the injection point (constructor, method, field)
     * @param argument     element that is the injection point (constructor/method parameter, field)
     */
    default void onInjectionPoint(RegistryRoundContext roundContext,
                                  TypeInfo service,
                                  TypedElementInfo element,
                                  TypedElementInfo argument) {

    }
}
