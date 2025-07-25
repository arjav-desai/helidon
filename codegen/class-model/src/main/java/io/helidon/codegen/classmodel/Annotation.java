/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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
package io.helidon.codegen.classmodel;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.common.types.AnnotationProperty;
import io.helidon.common.types.TypeName;

/**
 * Model of the annotation.
 */
public final class Annotation extends CommonComponent {

    private final List<AnnotationParameter> parameters;
    private final io.helidon.common.types.Annotation commonAnnotation;

    private Annotation(Builder builder) {
        super(builder);
        this.parameters = List.copyOf(builder.parameters.values());
        this.commonAnnotation = builder.commonAnntation;
    }

    /**
     * New {@link Builder} instance.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * New {@link Annotation} instance based on the type.
     *
     * @param type class type
     * @return new annotation instance
     */
    public static Annotation create(Class<?> type) {
        return builder().type(type).build();
    }

    /**
     * Parse new Annotation object out of the String.
     * <p>
     * This method works as expected for the following types:
     * <ul>
     *     <li>java.lang.String - expecting a quoted value, escapes of inner quotes is possible</li>
     *     <li>boolean - if the value is not quoted and is equal either to {code true} or {@code false}</li>
     *     <li>integer - if the value is not quoted and is a number</li>
     *     <li>double - if the value is not quoted, and ends with {@code D} (capital letter d)</li>
     *     <li>long - if the value is not quoted and ends with {@code L} (capital letter l)</li>
     *     <li>float - if the value is not quoted and ends with {@code F} (capital letter f)</li>
     *     <li>char - if the value is in single quotes {@code '}</li>
     *     <li>arrays - if the value is surrounded by curly braces {@code {}} it is an array</li>
     *     <li>annotations - if the value starts with at sign ({@code @})</li>
     * </ul>
     *
     * The following types have special handling and require explicit configuration:
     * <ul>
     *     <li>byte - unquoted value ending with {@code B}, such as {@code 49B}</li>
     *     <li>short - unquoted value ending with {@code S}, such as {@code 49S}</li>
     *     <li>class - unquoted value, in format {@code class::fq-name}, such as {@code class::java.lang.String}</li>
     *     <li>enum - unquoted value, in format {@code enum::fq-name.NAME}, such as
     *      {@code enum::java.lang.annotation.ElementType.CONSTRUCTOR}</li>
     * </ul>
     *
     * If the annotation only has a single value and the property name is {@code value}, the property name can be omitted
     * (same as when declaring the annotation in Java).
     * <p>
     * If the annotation does not need to declare any value, braces can be omitted (same as when declaring the annotation in Java)
     *
     * @param annotationDefinition annotation definition
     * @return new annotation instance
     */
    public static Annotation parse(String annotationDefinition) {
        return AnnotationParser.parse(annotationDefinition);
    }

    /**
     * Create a class model annotation from common types annotation.
     *
     * @param annotation annotation to process
     * @return a new class model annotation
     */
    public static Annotation create(io.helidon.common.types.Annotation annotation) {
        return builder().from(annotation).build();
    }

    /**
     * Convert class model annotation to Helidon Common Types annotation.
     *
     * @return common types annotation
     */
    public io.helidon.common.types.Annotation toTypesAnnotation() {
        if (this.commonAnnotation != null) {
            return commonAnnotation;
        }
        var builder = io.helidon.common.types.Annotation.builder()
                .typeName(type().genericTypeName());

        for (AnnotationParameter parameter : parameters) {
            var value = parameter.value();
            if (value instanceof Annotation annot) {
                builder.putValue(parameter.name(), annot.toTypesAnnotation());
            } else {
                builder.putValue(parameter.name(), parameter.value());
            }
        }

        return builder.build();
    }

    @Override
    void writeComponent(ModelWriter writer, Set<String> declaredTokens, ImportOrganizer imports, ClassType classType)
            throws IOException {
        writer.write("@" + imports.typeName(type(), includeImport()));
        if (!parameters.isEmpty()) {
            writer.write("(");
            if (parameters.size() == 1) {
                AnnotationParameter parameter = parameters.get(0);
                if (parameter.name().equals("value")) {
                    parameter.writeValue(writer, imports);
                } else {
                    parameter.writeComponent(writer, declaredTokens, imports, classType);
                }
            } else {
                boolean first = true;
                for (AnnotationParameter parameter : parameters) {
                    if (first) {
                        first = false;
                    } else {
                        writer.write(", ");
                    }
                    parameter.writeComponent(writer, declaredTokens, imports, classType);
                }
            }
            writer.write(")");
        }
    }

    @Override
    void addImports(ImportOrganizer.Builder imports) {
        super.addImports(imports);
        parameters.forEach(parameter -> parameter.addImports(imports));
    }

    /**
     * Fluent API builder for {@link Annotation}.
     */
    public static final class Builder extends CommonComponent.Builder<Builder, Annotation> {

        private final Map<String, AnnotationParameter> parameters = new LinkedHashMap<>();
        private io.helidon.common.types.Annotation commonAnntation;

        private Builder() {
        }

        @Override
        public Annotation build() {
            if (type() == null) {
                throw new ClassModelException("Annotation type needs to be set");
            }
            return new Annotation(this);
        }

        @Override
        public Builder type(TypeName type) {
            return super.type(type);
        }

        @Override
        public Builder type(String type) {
            return super.type(type);
        }

        @Override
        public Builder type(Class<?> type) {
            return super.type(type);
        }

        /**
         * Adds annotation parameter.
         *
         * @param name  annotation parameter name
         * @param value parameter value
         * @return updated builder instance
         */
        public Builder addParameter(String name, Object value) {
            Objects.requireNonNull(value);

            return addParameter(name, AnnotationProperty.create(value));
        }

        /**
         * Adds annotation parameter.
         *
         * @param name  annotation parameter name
         * @param property annotation property
         * @return updated builder instance
         */
        public Builder addParameter(String name, AnnotationProperty property) {
            Objects.requireNonNull(property);

            Object value = property.value();
            Class<?> paramType = value instanceof TypeName
                    ? Class.class
                    : value.getClass();

            return addParameter(builder -> builder.name(name)
                    .type(paramType)
                    .value(value)
                    .update(it -> property.constantValue().ifPresent(it::constantValue))
            );
        }

        /**
         * Adds annotation parameter.
         *
         * @param consumer annotation parameter builder consumer
         * @return updated builder instance
         */
        public Builder addParameter(Consumer<AnnotationParameter.Builder> consumer) {
            Objects.requireNonNull(consumer);
            AnnotationParameter.Builder builder = AnnotationParameter.builder();
            consumer.accept(builder);
            return addParameter(builder.build());
        }

        /**
         * Adds annotation parameter.
         *
         * @param builder annotation parameter builder
         * @return updated builder instance
         */
        public Builder addParameter(AnnotationParameter.Builder builder) {
            Objects.requireNonNull(builder);
            return addParameter(builder.build());
        }

        /**
         * Adds annotation parameter.
         *
         * @param parameter annotation parameter
         * @return updated builder instance
         */
        public Builder addParameter(AnnotationParameter parameter) {
            Objects.requireNonNull(parameter);
            parameters.put(parameter.name(), parameter);
            return this;
        }

        Builder from(io.helidon.common.types.Annotation annotation) {
            this.commonAnntation = annotation;
            type(annotation.typeName());
            annotation.properties()
                    .forEach(this::addParameter);
            return this;
        }
    }

}
