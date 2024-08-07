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

package io.helidon.service.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import io.helidon.codegen.ClassCode;
import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenFiler;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.ModuleInfo;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.spi.CodegenExtension;
import io.helidon.common.Weighted;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.metadata.DescriptorMetadata;

import static io.helidon.service.codegen.ServiceCodegenTypes.SERVICE_ANNOTATION_DESCRIPTOR;

/**
 * Handles processing of all extensions, creates context and writes types.
 */
class ServiceRegistryCodegenExtension implements CodegenExtension {
    private final Map<TypeName, List<RegistryCodegenExtension>> typeToExtensions = new HashMap<>();
    private final Map<RegistryCodegenExtension, Predicate<TypeName>> extensionPredicates = new IdentityHashMap<>();
    private final Set<DescriptorMetadata> generatedServiceDescriptors = new HashSet<>();
    private final RegistryCodegenContext ctx;
    private final List<RegistryCodegenExtension> extensions;
    private final String module;

    private ServiceRegistryCodegenExtension(CodegenContext ctx, TypeName generator) {
        this.ctx = RegistryCodegenContext.create(ctx);
        this.module = ctx.moduleName().orElse(null);

        ServiceExtension serviceExtension = new ServiceExtension(this.ctx);
        this.extensions = List.of(serviceExtension);
        this.typeToExtensions.put(ServiceCodegenTypes.SERVICE_ANNOTATION_PROVIDER, List.of(serviceExtension));
    }

    static ServiceRegistryCodegenExtension create(CodegenContext ctx, TypeName generator) {
        return new ServiceRegistryCodegenExtension(ctx, generator);
    }

    @Override
    public void process(io.helidon.codegen.RoundContext roundContext) {
        Collection<TypeInfo> allTypes = roundContext.types();
        if (allTypes.isEmpty()) {
            extensions.forEach(it -> it.process(createRoundContext(List.of(), it)));
            return;
        }

        // type info list will contain all mapped annotations, so this is the state we can do annotation processing on
        List<TypeInfoAndAnnotations> annotatedTypes = annotatedTypes(allTypes);

        // and now for each extension, we discover types that contain annotations supported by that extension
        // and create a new round context for each extension

        // for each extension, create a RoundContext with just the stuff it wants
        for (RegistryCodegenExtension extension : extensions) {
            extension.process(createRoundContext(annotatedTypes, extension));
        }

        writeNewTypes();

        for (TypeInfo typeInfo : roundContext.annotatedTypes(SERVICE_ANNOTATION_DESCRIPTOR)) {
            // add each declared descriptor in source code
            Annotation descriptorAnnot = typeInfo.annotation(SERVICE_ANNOTATION_DESCRIPTOR);

            double weight = descriptorAnnot.doubleValue("weight").orElse(Weighted.DEFAULT_WEIGHT);
            Set<TypeName> contracts = descriptorAnnot.typeValues("contracts")
                    .map(Set::copyOf)
                    .orElseGet(Set::of);

            String registryType = descriptorAnnot.stringValue("registryType").orElse("core");

            // predefined service descriptor
            generatedServiceDescriptors.add(DescriptorMetadata.create(registryType,
                                                                      typeInfo.typeName(),
                                                                      weight,
                                                                      contracts));
        }

        if (roundContext.availableAnnotations().size() == 1 && roundContext.availableAnnotations()
                .contains(TypeNames.GENERATED)) {

            // no other types generated by Helidon annotation processors, we can generate module component (unless already done)
            if (!generatedServiceDescriptors.isEmpty()) {
                addDescriptorsToServiceMeta();
                generatedServiceDescriptors.clear();
            }
        }
    }

    @Override
    public void processingOver(io.helidon.codegen.RoundContext roundContext) {
        // do processing over in each extension
        extensions.forEach(RegistryCodegenExtension::processingOver);

        // if there was any type generated, write it out (will not trigger next round)
        writeNewTypes();

        if (!generatedServiceDescriptors.isEmpty()) {
            // re-check, maybe we run from a tool that does not generate anything except for the module component,
            // so let's create it now anyway (if created above, the set of descriptors is empty, so it is not attempted again
            // if somebody adds a service descriptor when processingOver, than it is wrong anyway
            addDescriptorsToServiceMeta();
            generatedServiceDescriptors.clear();
        }
    }

    private void addDescriptorsToServiceMeta() {
        // and write the module component
        Optional<ModuleInfo> currentModule = ctx.module();

        // generate module
        String moduleName = this.module == null ? currentModule.map(ModuleInfo::name).orElse(null) : module;
        String packageName = CodegenOptions.CODEGEN_PACKAGE.findValue(ctx.options())
                .orElseGet(() -> topLevelPackage(generatedServiceDescriptors));
        boolean hasModule = moduleName != null && !"unnamed module".equals(moduleName);
        if (!hasModule) {
            moduleName = "unnamed/" + packageName + (ctx.scope().isProduction() ? "" : "/" + ctx.scope().name());
        }
        HelidonMetaInfServices services = HelidonMetaInfServices.create(ctx.filer(),
                                                                        moduleName);

        services.addAll(generatedServiceDescriptors);
        services.write();
    }

    private void writeNewTypes() {
        // after each round, write all generated types
        CodegenFiler filer = ctx.filer();

        // generate all code
        var descriptors = ctx.descriptors();
        for (var descriptor : descriptors) {
            ClassCode classCode = descriptor.classCode();
            ClassModel classModel = classCode.classModel().build();
            generatedServiceDescriptors.add(DescriptorMetadata.create(descriptor.registryType(),
                                                                      classCode.newType(),
                                                                      descriptor.weight(),
                                                                      descriptor.contracts()));
            filer.writeSourceFile(classModel, classCode.originatingElements());
        }
        descriptors.clear();

        var otherTypes = ctx.types();
        for (var classCode : otherTypes) {
            ClassModel classModel = classCode.classModel().build();
            filer.writeSourceFile(classModel, classCode.originatingElements());
        }
        otherTypes.clear();
    }

    private List<TypeInfoAndAnnotations> annotatedTypes(Collection<TypeInfo> allTypes) {
        List<TypeInfoAndAnnotations> result = new ArrayList<>();

        for (TypeInfo typeInfo : allTypes) {
            result.add(new TypeInfoAndAnnotations(typeInfo, annotations(typeInfo)));
        }
        return result;
    }

    private RegistryRoundContext createRoundContext(List<TypeInfoAndAnnotations> annotatedTypes,
                                                    RegistryCodegenExtension extension) {
        Set<TypeName> extAnnots = new HashSet<>();
        Map<TypeName, List<TypeInfo>> extAnnotToType = new HashMap<>();
        Map<TypeName, TypeInfo> extTypes = new HashMap<>();

        for (TypeInfoAndAnnotations annotatedType : annotatedTypes) {
            for (TypeName typeName : annotatedType.annotations()) {
                boolean added = false;
                List<RegistryCodegenExtension> validExts = this.typeToExtensions.get(typeName);
                if (validExts != null) {
                    for (RegistryCodegenExtension validExt : validExts) {
                        if (validExt == extension) {
                            extAnnots.add(typeName);
                            extAnnotToType.computeIfAbsent(typeName, key -> new ArrayList<>())
                                    .add(annotatedType.typeInfo());
                            extTypes.put(annotatedType.typeInfo().typeName(), annotatedType.typeInfo);
                            added = true;
                        }
                    }
                }
                if (!added) {
                    Predicate<TypeName> predicate = this.extensionPredicates.get(extension);
                    if (predicate != null && predicate.test(typeName)) {
                        extAnnots.add(typeName);
                        extAnnotToType.computeIfAbsent(typeName, key -> new ArrayList<>())
                                .add(annotatedType.typeInfo());
                        extTypes.put(annotatedType.typeInfo().typeName(), annotatedType.typeInfo);
                    }
                }
            }
        }

        return new RoundContextImpl(
                Set.copyOf(extAnnots),
                Map.copyOf(extAnnotToType),
                List.copyOf(extTypes.values()));
    }

    private Set<TypeName> annotations(TypeInfo theTypeInfo) {
        Set<TypeName> result = new HashSet<>();

        // on type
        theTypeInfo.annotations()
                .stream()
                .map(Annotation::typeName)
                .forEach(result::add);

        // on fields, methods etc.
        theTypeInfo.elementInfo()
                .stream()
                .map(TypedElementInfo::annotations)
                .flatMap(List::stream)
                .map(Annotation::typeName)
                .forEach(result::add);

        // on parameters
        theTypeInfo.elementInfo()
                .stream()
                .map(TypedElementInfo::parameterArguments)
                .flatMap(List::stream)
                .map(TypedElementInfo::annotations)
                .flatMap(List::stream)
                .map(Annotation::typeName)
                .forEach(result::add);

        return result;
    }

    private String topLevelPackage(Set<DescriptorMetadata> typeNames) {
        String thePackage = typeNames.iterator().next().descriptorType().packageName();

        for (DescriptorMetadata typeName : typeNames) {
            String nextPackage = typeName.descriptorType().packageName();
            if (nextPackage.length() < thePackage.length()) {
                thePackage = nextPackage;
            }
        }

        return thePackage;
    }

    private record TypeInfoAndAnnotations(TypeInfo typeInfo, Set<TypeName> annotations) {
    }
}
