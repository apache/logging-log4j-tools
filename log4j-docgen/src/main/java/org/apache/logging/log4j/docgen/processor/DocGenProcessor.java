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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.Description;
import org.apache.logging.log4j.docgen.PluginAttribute;
import org.apache.logging.log4j.docgen.PluginElement;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.apache.logging.log4j.docgen.ScalarType;
import org.apache.logging.log4j.docgen.ScalarValue;
import org.apache.logging.log4j.docgen.Type;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxWriter;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@ServiceProvider(value = Processor.class, resolution = Resolution.OPTIONAL)
@SupportedAnnotationTypes("org.apache.logging.log4j.plugins.*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@NullMarked
public class DocGenProcessor extends AbstractProcessor {

    private static final String MULTIPLICITY_UNBOUNDED = "*";
    private static final CharSequence[] GETTER_SETTER_PREFIXES = {"get", "is", "set"};
    private static final List<Class<? extends Annotation>> PROPERTY_ANNOTATION_TYPES = List.of(
            PluginBuilderAttribute.class,
            org.apache.logging.log4j.plugins.PluginAttribute.class,
            org.apache.logging.log4j.plugins.PluginElement.class);
    private static final List<Class<? extends Annotation>> FACTORY_ANNOTATION_TYPES =
            List.of(Factory.class, PluginFactory.class);
    /**
     * Reference types from the {@code java.*} namespace that are described
     * in {@code org/apache/logging/log4j/docgen/internal/configuration.xml}
     */
    private static final Set<String> KNOWN_SCALAR_TYPES = Set.of(
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.String");

    // Plugins
    private final Map<String, PluginType> pluginTypes = new TreeMap<>();
    // Other interfaces
    private final Map<String, TypeElement> abstractTypes = new TreeMap<>();
    // Scalar types
    private final Map<String, TypeElement> scalarTypes = new TreeMap<>();

    private AsciidocConverter converter;
    private DocTrees docTrees;
    private Elements elements;
    private Types types;
    private Messager messager;
    // Type corresponding to java.util.Collection
    private DeclaredType collectionType;
    // Type corresponding to java.lang.Enum
    private DeclaredType enumType;

    @SuppressWarnings({"DataFlowIssue", "unused"})
    public DocGenProcessor() {
        converter = null;
        docTrees = null;
        elements = null;
        types = null;
        messager = null;
        collectionType = null;
        enumType = null;
    }

    // For testing and silencing nullability warnings
    @SuppressWarnings("unused")
    DocGenProcessor(final ProcessingEnvironment processingEnv) {
        docTrees = DocTrees.instance(processingEnv);
        converter = new AsciidocConverter(docTrees);
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        collectionType = (DeclaredType)
                types.erasure(elements.getTypeElement("java.util.Collection").asType());
        enumType = (DeclaredType)
                types.erasure(elements.getTypeElement("java.lang.Enum").asType());
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        docTrees = DocTrees.instance(processingEnv);
        converter = new AsciidocConverter(docTrees);
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        collectionType = (DeclaredType)
                types.erasure(elements.getTypeElement("java.util.Collection").asType());
        enumType = (DeclaredType)
                types.erasure(elements.getTypeElement("java.lang.Enum").asType());
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final PluginSet set = new PluginSet();
        // First step: document plugins
        roundEnv.getElementsAnnotatedWith(Plugin.class).forEach(element -> {
            if (element instanceof final TypeElement typeElement) {
                final PluginType pluginType = new PluginType();
                final Plugin pluginAnnotation = element.getAnnotation(Plugin.class);
                processPlugin(typeElement, pluginAnnotation, pluginType);
                pluginTypes.put(pluginType.getClassName(), pluginType);
            }
        });
        pluginTypes.values().forEach(set::addPlugin);
        // Second step: document abstract types
        abstractTypes.values().forEach(type -> {
            final AbstractType abstractType = new AbstractType();
            processAbstractType(type, abstractType);
            if (!abstractType.getDescription().getText().isEmpty()) {
                set.addAbstractType(abstractType);
            }
        });
        // Second step: document scalars
        scalarTypes.values().forEach(type -> {
            final ScalarType scalarType = new ScalarType();
            processScalarType(type, scalarType);
            set.addScalar(scalarType);
        });
        // Write the result file
        if (roundEnv.processingOver()) {
            try {
                final FileObject output = processingEnv
                        .getFiler()
                        .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/log4j/plugins.xml");

                try (final Writer writer = output.openWriter()) {
                    new PluginBundleStaxWriter().write(writer, set);
                }
            } catch (final IOException | XMLStreamException e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "An error occurred while writing to `META-INF/log4j/plugins.xml`: " + e.getMessage());
            }
        }
        return false;
    }

    private void processType(final QualifiedNameable element, final Type docgenType) {
        // Class name
        docgenType.setClassName(element.getQualifiedName().toString());
        // Description
        docgenType.setDescription(createDescription(element, null));
    }

    private void processAbstractType(final QualifiedNameable element, final AbstractType abstractType) {
        processType(element, abstractType);
    }

    private void processScalarType(final TypeElement element, final ScalarType scalarType) {
        processType(element, scalarType);
        if (types.isSubtype(element.asType(), enumType)) {
            for (final Element member : element.getEnclosedElements()) {
                if (member instanceof final VariableElement field
                        && field.getModifiers().contains(Modifier.STATIC)
                        && types.isSameType(field.asType(), element.asType())) {
                    final ScalarValue value = new ScalarValue();
                    value.setDescription(createDescription(field, null));
                    value.setName(field.getSimpleName().toString());
                    scalarType.addValue(value);
                }
            }
        }
    }

    private Map<String, String> processParameterDescriptions(final Element element) {
        final Map<String, String> descriptions = new HashMap<>();
        final DocCommentTree docCommentTree = docTrees.getDocCommentTree(element);
        if (docCommentTree != null) {
            docCommentTree.accept(
                    new SimpleDocTreeVisitor<Void, Map<String, String>>() {
                        @Override
                        public Void visitDocComment(final DocCommentTree node, final Map<String, String> descriptions) {
                            for (final DocTree docTree : node.getBlockTags()) {
                                docTree.accept(this, descriptions);
                            }
                            return null;
                        }

                        @Override
                        public Void visitParam(final ParamTree paramTree, final Map<String, String> descriptions) {
                            final String name = paramTree.getName().getName().toString();
                            descriptions.put(name, defaultString(converter.toAsciiDoc(paramTree)));
                            return null;
                        }
                    },
                    descriptions);
        }
        return descriptions;
    }

    private void processPlugin(final TypeElement element, final Plugin pluginAnnotation, final PluginType pluginType) {
        processAbstractType(element, pluginType);
        // Name
        pluginType.setName(getPluginName(element, pluginAnnotation));
        // Namespace
        final Namespace namespace = getAnnotation(element, Namespace.class);
        pluginType.setNamespace(namespace != null ? namespace.value() : "Core");
        // Supertypes
        registerSupertypes(element).forEach(pluginType::addSupertype);
        // Plugin factory
        for (final Element member : element.getEnclosedElements()) {
            if (!(member instanceof final ExecutableElement executable)) {
                continue;
            }
            if (!findAllAnnotationsOnMember(member, FACTORY_ANNOTATION_TYPES).isEmpty()) {
                final Map<String, String> descriptions = processParameterDescriptions(executable);
                final List<? extends VariableElement> parameters = executable.getParameters();
                if (parameters.isEmpty()) {
                    // We have a builder
                    final TypeElement returnType = getReturnType(executable);
                    if (returnType != null) {
                        processProperties(getAllMembers(returnType), descriptions, pluginType);
                    } else {
                        messager.printMessage(
                                Diagnostic.Kind.WARNING,
                                "The return type of a @PluginFactory annotated method should be a concrete class.",
                                member);
                    }
                } else {
                    // Old style factory method
                    processProperties(parameters, descriptions, pluginType);
                }
            }
        }
    }

    private void processProperties(
            final Iterable<? extends Element> members,
            final Map<? super String, String> descriptions,
            final PluginType pluginType) {
        final Collection<PluginAttribute> pluginAttributes =
                new TreeSet<>(Comparator.comparing(a -> defaultString(a.getName())));
        final Collection<PluginElement> pluginElements =
                new TreeSet<>(Comparator.comparing(e -> defaultString(e.getType())));
        // Gather documentation, which can be on any member.
        for (final Element member : members) {
            final String name = getPropertyName(member);
            final String asciidoc = converter.toAsciiDoc(member);
            descriptions.compute(name, (key, value) -> Stream.of(value, asciidoc)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.joining("\n")));
        }
        // Creates attributes and elements
        for (final Element member : members) {
            final String description = descriptions.get(getPropertyName(member));
            for (final Annotation annotation : findAllAnnotationsOnMember(member, PROPERTY_ANNOTATION_TYPES)) {
                if (annotation instanceof final PluginBuilderAttribute attribute) {
                    pluginAttributes.add(createPluginAttribute(member, description, attribute.value()));
                }
                if (annotation instanceof final org.apache.logging.log4j.plugins.PluginAttribute attribute) {
                    pluginAttributes.add(createPluginAttribute(member, description, attribute.value()));
                }
                if (annotation instanceof org.apache.logging.log4j.plugins.PluginElement) {
                    pluginElements.add(createPluginElement(member, description));
                }
            }
        }
        pluginAttributes.forEach(pluginType::addAttribute);
        pluginElements.forEach(pluginType::addElement);
    }

    private Description createDescription(final String asciidoc) {
        final Description description = new Description();
        description.setText(StringUtils.stripToEmpty(asciidoc));
        return description;
    }

    private Description createDescription(final Element element, final @Nullable String fallback) {
        return createDescription(defaultIfEmpty(converter.toAsciiDoc(element), defaultString(fallback)));
    }

    private PluginAttribute createPluginAttribute(
            final Element element, final String description, final String specifiedName) {
        final PluginAttribute attribute = new PluginAttribute();
        // Name
        attribute.setName(specifiedName.isEmpty() ? getPropertyName(element) : specifiedName);
        // Type
        final TypeMirror type = getMemberType(element);
        final String className = getClassName(type);
        // If type is not a well-known declared type, process it for documentation.
        if (className != null
                && !KNOWN_SCALAR_TYPES.contains(className)
                && type instanceof final DeclaredType declaredType) {
            scalarTypes.putIfAbsent(className, asTypeElement(declaredType));
        }
        attribute.setType(className);
        // Description
        attribute.setDescription(createDescription(element, description));
        // Required
        if (getAnnotation(element, Required.class) != null) {
            attribute.setRequired(true);
        }
        // Default value
        final Object defaultValue = element instanceof final VariableElement field ? field.getConstantValue() : null;
        if (defaultValue != null) {
            attribute.setDefaultValue(elements.getConstantExpression(defaultValue));
        }
        // TODO: add the value of the property used, when we add it to the annotation.
        return attribute;
    }

    private PluginElement createPluginElement(final Element element, final String description) {
        final PluginElement pluginElement = new PluginElement();
        // Type and multiplicity
        final TypeMirror elementType = getMemberType(element);
        if (elementType == null) {
            messager.printMessage(Diagnostic.Kind.WARNING, "Unable to determine type of plugin element.", element);
        } else {
            pluginElement.setType(getComponentClassName(elementType));
            pluginElement.setMultiplicity(getMultiplicity(elementType));
        }
        // Required
        if (getAnnotation(element, Required.class) != null) {
            pluginElement.setRequired(true);
        }
        // Description
        pluginElement.setDescription(createDescription(element, description));
        return pluginElement;
    }

    /**
     * Register all the supertypes of the given type for doc processing.
     * @param element a plugin class,
     * @return the set of FQCN of all supertypes.
     */
    private Set<String> registerSupertypes(final TypeElement element) {
        final Set<String> supertypes = new TreeSet<>();
        element.accept(
                new SimpleElementVisitor8<Void, Set<String>>() {
                    @Override
                    public Void visitType(final TypeElement element, final Set<String> supertypes) {
                        registerAndVisit(element.getSuperclass(), supertypes);
                        element.getInterfaces().forEach(iface -> registerAndVisit(iface, supertypes));
                        return null;
                    }

                    private void registerAndVisit(final TypeMirror type, final Set<String> supertypes) {
                        if (type instanceof final DeclaredType declaredType) {
                            final TypeElement element = asTypeElement(declaredType);
                            final String className = element.getQualifiedName().toString();
                            abstractTypes.putIfAbsent(className, element);
                            if (supertypes.add(className)) {
                                element.accept(this, supertypes);
                            }
                        }
                    }
                },
                supertypes);
        return supertypes;
    }

    private String getPluginName(final Element element, final Plugin annotation) {
        final String value = annotation.value();
        return value.isEmpty() ? element.getSimpleName().toString() : value;
    }

    private Collection<? extends Annotation> findAllAnnotationsOnMember(
            final Element element, final Iterable<Class<? extends Annotation>> annotationTypes) {
        final Collection<Annotation> annotations = new HashSet<>();
        element.accept(
                new SimpleElementVisitor8<Void, Collection<? super Annotation>>() {
                    @Override
                    protected Void defaultAction(final Element e, final Collection<? super Annotation> annotations) {
                        annotationTypes.forEach(annotationType -> {
                            final Annotation annotation = getAnnotation(e, annotationType);
                            if (annotation != null) {
                                annotations.add(annotation);
                            }
                        });
                        return null;
                    }

                    @Override
                    public Void visitExecutable(
                            final ExecutableElement e, final Collection<? super Annotation> annotations) {
                        for (final VariableElement param : e.getParameters()) {
                            param.accept(this, annotations);
                        }
                        return super.visitExecutable(e, annotations);
                    }
                },
                annotations);
        return annotations;
    }

    private @Nullable TypeMirror getMemberType(final Element element) {
        return element.accept(
                new SimpleElementVisitor8<@Nullable TypeMirror, @Nullable Void>() {
                    @Override
                    protected @Nullable TypeMirror defaultAction(final Element element, final Void unused) {
                        messager.printMessage(
                                Diagnostic.Kind.WARNING,
                                "Unexpected plugin annotation on element of type "
                                        + element.getKind().name(),
                                element);
                        return null;
                    }

                    @Override
                    public TypeMirror visitVariable(final VariableElement element, final Void unused) {
                        return element.asType();
                    }

                    @Override
                    public @Nullable TypeMirror visitExecutable(final ExecutableElement element, final Void unused) {
                        final TypeMirror returnType = element.getReturnType();
                        final List<? extends VariableElement> parameters = element.getParameters();
                        return switch (parameters.size()) {
                                // A getter
                            case 0 -> returnType;
                                // A setter
                            case 1 -> parameters.get(0).asType();
                                // Invalid property
                            default -> super.visitExecutable(element, unused);
                        };
                    }
                },
                null);
    }

    private @Nullable String getPropertyName(final Element element) {
        return element.accept(
                new SimpleElementVisitor8<@Nullable String, @Nullable Void>() {
                    @Override
                    public String visitVariable(final VariableElement e, final Void unused) {
                        return e.getSimpleName().toString();
                    }

                    @Override
                    public @Nullable String visitExecutable(final ExecutableElement e, final Void unused) {
                        final Name name = e.getSimpleName();
                        if (StringUtils.startsWithAny(name, GETTER_SETTER_PREFIXES)) {
                            final int prefixLen = StringUtils.startsWith(name, "is") ? 2 : 3;
                            if (name.length() > prefixLen) {
                                return Character.toLowerCase(name.charAt(prefixLen))
                                        + name.toString().substring(prefixLen + 1);
                            }
                        }
                        return null;
                    }
                },
                null);
    }

    /**
     * Returns the appropriate type element for the return type of this method.
     * <p>
     *     If the return type is a type variable, returns its upper bound.
     * </p>
     * <p>
     *     If the return type is {@code void} or primitive, {@code null} is returned.
     * </p>
     */
    private @Nullable TypeElement getReturnType(final ExecutableElement method) {
        return method.getReturnType()
                .accept(
                        new SimpleTypeVisitor8<@Nullable TypeElement, @Nullable Void>() {
                            @Override
                            public TypeElement visitDeclared(final DeclaredType t, final Void unused) {
                                return asTypeElement(t);
                            }

                            @Override
                            public @Nullable TypeElement visitTypeVariable(final TypeVariable t, final Void unused) {
                                // If the return type is a variable, try the upper bound
                                return t.getUpperBound().accept(this, unused);
                            }
                        },
                        null);
    }

    /**
     * Returns all the members of this type or its ancestors.
     */
    private Collection<? extends Element> getAllMembers(final TypeElement element) {
        final Collection<Element> members = new HashSet<>();
        TypeElement currentElement = element;
        while (currentElement != null) {
            members.addAll(currentElement.getEnclosedElements());
            currentElement = getSuperclass(currentElement);
        }
        return members;
    }

    private @Nullable TypeElement getSuperclass(final TypeElement element) {
        final TypeMirror superclass = element.getSuperclass();
        return superclass instanceof final DeclaredType declaredType ? asTypeElement(declaredType) : null;
    }

    // TODO: Can the element associated to a declared type be anything else than a type element?
    private TypeElement asTypeElement(final DeclaredType type) {
        return (TypeElement) type.asElement();
    }

    /**
     * Gets the class name of the erasure of this type.
     * <p>
     *     If this is an array type, {@code null} is returned.
     * </p>
     */
    private @Nullable String getClassName(final @Nullable TypeMirror type) {
        return type != null
                ? types.erasure(type)
                        .accept(
                                new SimpleTypeVisitor8<String, @Nullable Void>() {

                                    @Override
                                    public String visitDeclared(final DeclaredType t, final Void unused) {
                                        return asTypeElement(t)
                                                .getQualifiedName()
                                                .toString();
                                    }

                                    @Override
                                    public String visitPrimitive(final PrimitiveType t, final Void unused) {
                                        return switch (t.getKind()) {
                                            case BOOLEAN -> "boolean";
                                            case BYTE -> "byte";
                                            case SHORT -> "short";
                                            case INT -> "int";
                                            case LONG -> "long";
                                            case CHAR -> "char";
                                            case FLOAT -> "float";
                                            case DOUBLE -> "double";
                                            default -> throw new IllegalArgumentException();
                                        };
                                    }

                                    @Override
                                    public String visitNoType(final NoType t, final Void unused) {
                                        return "void";
                                    }
                                },
                                null)
                : null;
    }

    /**
     * If this is an array or collection type, returns the class name of its component.
     * <p>
     *     If this is not an array or collection type, {@link #getClassName(TypeMirror)} is returned.
     * </p>
     */
    private @Nullable String getComponentClassName(final TypeMirror type) {
        return type.accept(
                new SimpleTypeVisitor8<@Nullable String, @Nullable Void>() {
                    @Override
                    protected @Nullable String defaultAction(final TypeMirror e, final Void unused) {
                        return getClassName(e);
                    }

                    @Override
                    public @Nullable String visitArray(final ArrayType t, final Void unused) {
                        return getClassName(t.getComponentType());
                    }

                    @Override
                    public @Nullable String visitDeclared(final DeclaredType t, final Void unused) {
                        if (types.isAssignable(t, collectionType)) {
                            // Bind T in Collection
                            final DeclaredType asCollection = findCollectionSupertype(t);
                            if (asCollection != null) {
                                final List<? extends TypeMirror> typeArguments = asCollection.getTypeArguments();
                                if (!typeArguments.isEmpty()) {
                                    return getClassName(typeArguments.get(0));
                                }
                            }
                        }
                        return super.visitDeclared(t, unused);
                    }

                    private @Nullable DeclaredType findCollectionSupertype(final TypeMirror type) {
                        if (types.isSameType(types.erasure(type), collectionType)) {
                            return (DeclaredType) type;
                        }
                        for (final TypeMirror supertype : types.directSupertypes(type)) {
                            final DeclaredType result = findCollectionSupertype(supertype);
                            if (result != null) {
                                return result;
                            }
                        }
                        return null;
                    }
                },
                null);
    }

    private @Nullable String getMultiplicity(final TypeMirror type) {
        return type.accept(
                new SimpleTypeVisitor8<@Nullable String, @Nullable Void>() {
                    @Override
                    public String visitArray(final ArrayType t, final Void unused) {
                        return MULTIPLICITY_UNBOUNDED;
                    }

                    @Override
                    public @Nullable String visitDeclared(final DeclaredType t, final Void unused) {
                        return types.isAssignable(t, collectionType) ? MULTIPLICITY_UNBOUNDED : null;
                    }
                },
                null);
    }

    <A extends Annotation> @Nullable A getAnnotation(final Element element, final Class<A> annotationType) {
        return element.getAnnotation(annotationType);
    }
}
