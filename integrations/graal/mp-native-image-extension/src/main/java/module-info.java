/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
 * Extension for Graal VM native image to correctly build Helidon MicroProfile applications.
 */
module io.helidon.graal.nativeimage.mp {
    requires svm;
    requires io.helidon.graal.nativeimage;
    requires org.graalvm.sdk;
    requires weld.core.impl;
    requires jakarta.enterprise.cdi.api;
    requires java.json;
    requires jboss.classfilewriter;

    exports io.helidon.integrations.graal.mp.nativeimage.extension;
}