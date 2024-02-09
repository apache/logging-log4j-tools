/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.docgen.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.Types;

final class Annotations {

    private static final Collection<String> FACTORY_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory",
            "org.apache.logging.log4j.core.config.plugins.PluginFactory",
            "org.apache.logging.log4j.plugins.Factory",
            "org.apache.logging.log4j.plugins.PluginFactory");
    private static final String NAMESPACE_ANNOTATION_NAME = "org.apache.logging.log4j.plugins.Namespace";
    private static final String PLUGIN_V2_ANNOTATION_NAME = "org.apache.logging.log4j.core.config.plugins.Plugin";
    private static final String PLUGIN_V3_ANNOTATION_NAME = "org.apache.logging.log4j.plugins.Plugin";
    private static final Collection<String> PLUGIN_ALIAS_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.PluginAliases",
            "org.apache.logging.log4j.plugins.PluginAliases");
    private static final Collection<String> PLUGIN_ATTRIBUTE_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute",
            "org.apache.logging.log4j.core.config.plugins.PluginAttribute",
            "org.apache.logging.log4j.plugins.PluginAttribute",
            "org.apache.logging.log4j.plugins.PluginBuilderAttribute");
    private static final Collection<String> PLUGIN_ELEMENT_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.PluginElement",
            "org.apache.logging.log4j.plugins.PluginElement");
    private static final Collection<String> REQUIRED_CONSTRAINT_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.validation.constraints.Required",
            "org.apache.logging.log4j.plugins.validation.constraints.Required");

    private final Elements elements;
    private final Types types;

    private final Name category;
    private final Name name;
    private final Name value;

    private final TypeElement[] pluginAnnotationElements;
    // Contain the namespace of the plugin
    private final DeclaredType namespaceAnnotation;
    private final DeclaredType pluginV2Annotation;
    private final DeclaredType pluginV3Annotation;

    private final Collection<DeclaredType> factoryAnnotations = new HashSet<>();
    private final Collection<DeclaredType> pluginAliasAnnotations = new HashSet<>();
    private final Collection<DeclaredType> pluginAttributeAnnotations = new HashSet<>();
    private final Collection<DeclaredType> pluginAttributeAndElementAnnotations = new HashSet<>();
    private final Collection<DeclaredType> requiredConstraintAnnotations = new HashSet<>();

    Annotations(final Elements elements, final Types types) {
        this.elements = elements;
        this.types = types;

        this.category = elements.getName("category");
        this.name = elements.getName("name");
        this.value = elements.getName("value");

        final Collection<TypeElement> pluginAnnotations = new ArrayList<>();
        final TypeElement pluginV2Annotation = elements.getTypeElement(PLUGIN_V2_ANNOTATION_NAME);
        this.pluginV2Annotation = pluginV2Annotation != null ? (DeclaredType) pluginV2Annotation.asType() : null;
        if (pluginV2Annotation != null) {
            pluginAnnotations.add(pluginV2Annotation);
        }
        final TypeElement pluginV3Annotation = elements.getTypeElement(PLUGIN_V3_ANNOTATION_NAME);
        this.pluginV3Annotation = pluginV3Annotation != null ? (DeclaredType) pluginV3Annotation.asType() : null;
        if (pluginV3Annotation != null) {
            pluginAnnotations.add(pluginV3Annotation);
        }
        final TypeElement namespaceAnnotation = elements.getTypeElement(NAMESPACE_ANNOTATION_NAME);
        this.namespaceAnnotation = namespaceAnnotation != null ? (DeclaredType) namespaceAnnotation.asType() : null;
        this.pluginAnnotationElements = pluginAnnotations.toArray(new TypeElement[0]);

        FACTORY_ANNOTATION_NAMES.forEach(name -> addDeclaredTypeIfExists(name, factoryAnnotations));
        PLUGIN_ALIAS_ANNOTATION_NAMES.forEach(name -> addDeclaredTypeIfExists(name, pluginAliasAnnotations));
        PLUGIN_ATTRIBUTE_ANNOTATION_NAMES.forEach(name -> addDeclaredTypeIfExists(name, pluginAttributeAnnotations));
        pluginAttributeAndElementAnnotations.addAll(pluginAttributeAnnotations);
        PLUGIN_ELEMENT_ANNOTATION_NAMES.forEach(
                name -> addDeclaredTypeIfExists(name, pluginAttributeAndElementAnnotations));
        REQUIRED_CONSTRAINT_ANNOTATION_NAMES.forEach(
                name -> addDeclaredTypeIfExists(name, requiredConstraintAnnotations));
    }

    public TypeElement[] getPluginAnnotations() {
        return pluginAnnotationElements;
    }

    public Optional<String> getAttributeSpecifiedName(final AnnotationMirror annotation) {
        return Optional.ofNullable(getValueAsString(annotation, value));
    }

    public Optional<String> getPluginSpecifiedName(final AnnotatedConstruct element) {
        return getAnnotationValue(element, pluginV2Annotation, name, pluginV3Annotation, value);
    }

    public Optional<String> getPluginSpecifiedNamespace(final AnnotatedConstruct element) {
        return getAnnotationValue(element, pluginV2Annotation, category, namespaceAnnotation, value);
    }

    public boolean hasFactoryAnnotation(final Element element) {
        return hasAnyDirectAnnotation(element, factoryAnnotations);
    }

    public boolean hasRequiredConstraint(final Element element) {
        return hasAnyDirectAnnotation(element, requiredConstraintAnnotations);
    }

    public boolean isAttributeAnnotation(final AnnotationMirror annotation) {
        return contains(pluginAttributeAnnotations, annotation.getAnnotationType());
    }

    /**
     * Find all plugin element and attribute annotations on the element and its children.
     */
    public Collection<? extends AnnotationMirror> findAttributeAndPropertyAnnotations(final Element element) {
        final Collection<AnnotationMirror> annotations = new HashSet<>();
        element.accept(
                new SimpleElementVisitor8<Void, Collection<? super AnnotationMirror>>() {
                    @Override
                    protected Void defaultAction(
                            final Element e, final Collection<? super AnnotationMirror> annotations) {
                        for (final AnnotationMirror annotation : e.getAnnotationMirrors()) {
                            if (contains(pluginAttributeAndElementAnnotations, annotation.getAnnotationType())) {
                                annotations.add(annotation);
                            }
                        }
                        return null;
                    }

                    @Override
                    public Void visitExecutable(
                            final ExecutableElement e, final Collection<? super AnnotationMirror> annotations) {
                        for (final VariableElement param : e.getParameters()) {
                            param.accept(this, annotations);
                        }
                        return super.visitExecutable(e, annotations);
                    }
                },
                annotations);
        return annotations;
    }

    private String getValueAsString(final AnnotationMirror annotation, final Name property) {
        final Object value = getValue(annotation, property);
        return value != null ? value.toString() : null;
    }

    private boolean hasAnyDirectAnnotation(
            final Element element, final Iterable<? extends DeclaredType> annotationTypes) {
        return elements.getAllAnnotationMirrors(element).stream()
                .anyMatch(mirror -> contains(annotationTypes, mirror.getAnnotationType()));
    }

    private Object getValue(final AnnotationMirror annotation, final Name property) {
        for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                annotation.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().equals(property)) {
                return entry.getValue().getValue();
            }
        }
        return null;
    }

    /**
     * Necessary, since TypeMirror does not implement a valid equals.
     */
    private <T extends TypeMirror> boolean contains(final Iterable<? extends T> collection, final T type) {
        for (final T t : collection) {
            if (types.isSameType(t, type)) {
                return true;
            }
        }
        return false;
    }

    private void addDeclaredTypeIfExists(
            final CharSequence className, final Collection<? super DeclaredType> collection) {
        final TypeElement element = elements.getTypeElement(className);
        if (element != null) {
            final TypeMirror type = element.asType();
            if (type instanceof DeclaredType) {
                collection.add((DeclaredType) type);
            }
        }
    }

    private Optional<String> getAnnotationValue(
            final AnnotatedConstruct element,
            final TypeMirror v2Annotation,
            final Name v2Property,
            final TypeMirror v3Annotation,
            final Name v3Property) {
        return element.getAnnotationMirrors().stream()
                .map(annotation -> {
                    final DeclaredType annotationType = annotation.getAnnotationType();
                    if (v2Annotation != null && types.isSameType(v2Annotation, annotationType)) {
                        return getValueAsString(annotation, v2Property);
                    }
                    return v3Annotation != null && types.isSameType(v3Annotation, annotationType)
                            ? getValueAsString(annotation, v3Property)
                            : null;
                })
                .filter(Objects::nonNull)
                .findAny();
    }
}
