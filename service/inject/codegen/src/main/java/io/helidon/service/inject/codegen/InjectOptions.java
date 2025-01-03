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

package io.helidon.service.inject.codegen;

import java.util.Set;

import io.helidon.codegen.Option;
import io.helidon.common.GenericType;
import io.helidon.common.types.TypeName;

/**
 * Supported options specific to Helidon Inject.
 */
public final class InjectOptions {
    /**
     * Which {@code InterceptionStrategy} to use.
     */
    public static final Option<InterceptionStrategy> INTERCEPTION_STRATEGY =
            Option.create("helidon.inject.interceptionStrategy",
                          "Which interception strategy to use (NONE, EXPLICIT, ALL_RUNTIME, ALL_RETAINED)",
                          InterceptionStrategy.EXPLICIT,
                          InterceptionStrategy::valueOf,
                          GenericType.create(InterceptionStrategy.class));

    /**
     * Additional meta annotations that mark scope annotations. This can be used to include
     * jakarta.enterprise.context.NormalScope annotated types as scopes.
     */
    public static final Option<Set<TypeName>> SCOPE_META_ANNOTATIONS =
            Option.createSet("helidon.inject.scopeMetaAnnotations",
                             "Additional meta annotations that mark scope annotations. This can be used to include"
                                     + "jakarta.enterprise.context.NormalScope annotated types as scopes.",
                             Set.of(),
                             TypeName::create,
                             new GenericType<Set<TypeName>>() { });

    private InjectOptions() {
    }
}
