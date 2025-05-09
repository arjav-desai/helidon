/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.service.tests.scopes;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.service.registry.Scope;
import io.helidon.service.registry.Service;

@Service.Singleton
@Service.NamedByType(CustomScope.class)
public class CustomScopeHandler implements Service.ScopeHandler {

    private final AtomicReference<Scope> currentScope = new AtomicReference<>();

    @Override
    public Optional<Scope> currentScope() {
        return Optional.ofNullable(currentScope.get());
    }

    @Override
    public void activate(Scope scope) {
        currentScope.set(scope);
        scope.registry().activate();
    }
}
