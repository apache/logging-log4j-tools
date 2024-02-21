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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@ServiceProvider(value = Processor.class, resolution = Resolution.OPTIONAL)
@SupportedAnnotationTypes({"org.apache.logging.log4j.core.config.plugins.*", "org.apache.logging.log4j.plugins.*"})
@SupportedOptions({
    DescriptorGenerator.DESCRIPTOR_FILE_PATH_OPTION_KEY,
    DescriptorGenerator.GROUP_ID_OPTION_KEY,
    DescriptorGenerator.ARTIFACT_ID_OPTION_KEY,
    DescriptorGenerator.VERSION_OPTION_KEY,
    DescriptorGenerator.DESCRIPTION_OPTION_KEY
})
@NullMarked
public class DescriptorGenerator extends AbstractProcessor {

    static final String DESCRIPTOR_FILE_PATH_OPTION_KEY = "log4j.docgen.descriptorFilePath";

    static final String GROUP_ID_OPTION_KEY = "log4j.docgen.groupId";

    static final String ARTIFACT_ID_OPTION_KEY = "log4j.docgen.artifactId";

    static final String VERSION_OPTION_KEY = "log4j.docgen.version";

    static final String DESCRIPTION_OPTION_KEY = "log4j.docgen.description";

    private static final String MULTIPLICITY_UNBOUNDED = "*";

    private static final CharSequence[] GETTER_SETTER_PREFIXES = {"get", "is", "set"};

    /**
     * Reference types from the {@code java.*} namespace that are described in {@code org/apache/logging/log4j/docgen/generator/base-java-types.xml}.
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

    // Abstract types to process
    private final Collection<TypeElement> abstractTypesToDocument = new HashSet<>();

    // Scalar types to process
    private final Collection<TypeElement> scalarTypesToDocument = new HashSet<>();

    private PluginSet pluginSet;

    private Path descriptorFilePath;

    private DocTrees docTrees;

    private Elements elements;

    private Messager messager;

    private Types types;

    private AsciiDocConverter converter;

    private Annotations annotations;

    // Type corresponding to `java.util.Collection`
    private DeclaredType collectionType;

    // Type corresponding to `java.lang.Enum`
    private DeclaredType enumType;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        pluginSet = createPluginSet(processingEnv);
        descriptorFilePath = Path.of(requireOption(processingEnv, DESCRIPTOR_FILE_PATH_OPTION_KEY));
        docTrees = DocTrees.instance(processingEnv);
        elements = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        types = processingEnv.getTypeUtils();
        converter = new AsciiDocConverter(docTrees);
        annotations = new Annotations(elements, types);
        collectionType = getDeclaredType(processingEnv, "java.util.Collection");
        enumType = getDeclaredType(processingEnv, "java.lang.Enum");
    }

    private static PluginSet createPluginSet(final ProcessingEnvironment processingEnv) {
        final PluginSet pluginSet = new PluginSet();
        final String groupId = requireOption(processingEnv, GROUP_ID_OPTION_KEY);
        final String artifactId = requireOption(processingEnv, ARTIFACT_ID_OPTION_KEY);
        @Nullable final String version = getOption(processingEnv, VERSION_OPTION_KEY);
        @Nullable final String description = getOption(processingEnv, DESCRIPTION_OPTION_KEY);
        pluginSet.setGroupId(groupId);
        pluginSet.setArtifactId(artifactId);
        pluginSet.setVersion(version);
        if (description != null) {
            final Description descriptionModel = new Description();
            descriptionModel.setText(description);
            pluginSet.setDescription(descriptionModel);
        }
        return pluginSet;
    }

    private static String requireOption(final ProcessingEnvironment processingEnv, final String key) {
        @Nullable final String value = getOption(processingEnv, key);
        if (value == null) {
            final String message = String.format("missing option: `%s`", key);
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    @Nullable
    private static String getOption(final ProcessingEnvironment processingEnv, final String key) {
        @Nullable final String value = processingEnv.getOptions().get(key);
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private static DeclaredType getDeclaredType(final ProcessingEnvironment processingEnv, final String className) {
        final Types typeUtils = processingEnv.getTypeUtils();
        final Elements elementUtils = processingEnv.getElementUtils();
        return (DeclaredType)
                typeUtils.erasure(elementUtils.getTypeElement(className).asType());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> unused, final RoundEnvironment roundEnv) {
        // First step: document plugins
        roundEnv.getElementsAnnotatedWithAny(annotations.getPluginAnnotations()).forEach(this::addPluginDocumentation);
        // Second step: document abstract types
        abstractTypesToDocument.forEach(this::addAbstractTypeDocumentation);
        // Second step: document scalars
        scalarTypesToDocument.forEach(this::addScalarTypeDocumentation);
        // Write the result file
        if (roundEnv.processingOver()) {
            writePluginDescriptor();
        }
        return false;
    }

    private void addPluginDocumentation(final Element element) {
        try {
            if (element instanceof TypeElement) {
                final PluginType pluginType = new PluginType();
                pluginType.setName(annotations.getPluginSpecifiedName(element).orElseGet(() -> element.getSimpleName()
                        .toString()));
                pluginType.setNamespace(
                        annotations.getPluginSpecifiedNamespace(element).orElse("Core"));
                populatePlugin((TypeElement) element, pluginType);
                pluginSet.addPlugin(pluginType);
            } else {
                messager.printMessage(
                        Diagnostic.Kind.WARNING, "Found @Plugin annotation on unexpected element.", element);
            }
        } catch (final Exception error) {
            final String message = String.format("failed to process element `%s`", element);
            throw new RuntimeException(message, error);
        }
    }

    private void addAbstractTypeDocumentation(final QualifiedNameable element) {
        try {
            final AbstractType abstractType = new AbstractType();
            populateType(element, abstractType);
            if (!abstractType.getClassName().startsWith("java.")) {
                pluginSet.addAbstractType(abstractType);
            }
        } catch (final Exception error) {
            final String message = String.format("failed to process element `%s`", element);
            throw new RuntimeException(message, error);
        }
    }

    private void addScalarTypeDocumentation(final TypeElement element) {
        try {
            final ScalarType scalarType = new ScalarType();
            populateScalarType(element, scalarType);
            pluginSet.addScalar(scalarType);
        } catch (final Exception error) {
            final String message = String.format("failed to process element `%s`", element);
            throw new RuntimeException(message, error);
        }
    }

    private void writePluginDescriptor() {
        try {
            try (final Writer writer = Files.newBufferedWriter(descriptorFilePath)) {
                new PluginBundleStaxWriter().write(writer, pluginSet);
            }
        } catch (final IOException | XMLStreamException error) {
            final String message = String.format("failed to write to `%s`", descriptorFilePath);
            throw new RuntimeException(message, error);
        }
    }

    private void populateType(final QualifiedNameable element, final Type docgenType) {
        // Class name
        docgenType.setClassName(element.getQualifiedName().toString());
        // Description
        docgenType.setDescription(createDescription(element, null));
    }

    private void populateScalarType(final TypeElement element, final ScalarType scalarType) {
        populateType(element, scalarType);
        if (types.isSubtype(element.asType(), enumType)) {
            for (final Element member : element.getEnclosedElements()) {
                if (member instanceof VariableElement
                        && member.getModifiers().contains(Modifier.STATIC)
                        && types.isSameType(member.asType(), element.asType())) {
                    final VariableElement field = (VariableElement) member;
                    final ScalarValue value = new ScalarValue();
                    value.setDescription(createDescription(field, null));
                    value.setName(field.getSimpleName().toString());
                    scalarType.addValue(value);
                }
            }
        }
    }

    private Map<String, String> getParameterDescriptions(final Element element) {
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

    private void populatePlugin(final TypeElement element, final PluginType pluginType) {
        populateType(element, pluginType);
        // Supertypes
        registerSupertypes(element).forEach(pluginType::addSupertype);
        // Plugin factory
        for (final Element member : element.getEnclosedElements()) {
            if (annotations.hasFactoryAnnotation(member) && member instanceof ExecutableElement) {
                final ExecutableElement executable = (ExecutableElement) member;
                final Map<String, String> descriptions = getParameterDescriptions(executable);
                final List<? extends VariableElement> parameters = executable.getParameters();
                if (parameters.isEmpty()) {
                    // We have a builder
                    final TypeElement returnType = getReturnType(executable);
                    if (returnType != null) {
                        populateConfigurationProperties(getAllMembers(returnType), descriptions, pluginType);
                    } else {
                        messager.printMessage(
                                Diagnostic.Kind.WARNING,
                                "The return type of a @PluginFactory annotated method should be a concrete class.",
                                member);
                    }
                } else {
                    // Old style factory method
                    populateConfigurationProperties(parameters, descriptions, pluginType);
                }
            }
        }
    }

    private void populateConfigurationProperties(
            final Iterable<? extends Element> members,
            final Map<? super String, String> descriptions,
            final PluginType pluginType) {
        final Collection<PluginAttribute> pluginAttributes =
                new TreeSet<>(Comparator.comparing(a -> defaultString(a.getName())));
        final Collection<PluginElement> pluginElements =
                new TreeSet<>(Comparator.comparing(e -> defaultString(e.getType())));
        // Gather documentation, which can be on any member.
        for (final Element member : members) {
            final String name = getAttributeOrPropertyName(member);
            final String asciiDoc = converter.toAsciiDoc(member);
            descriptions.compute(name, (key, value) -> Stream.of(value, asciiDoc)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.joining("\n")));
        }
        // Creates attributes and elements
        for (final Element member : members) {
            final String description = descriptions.get(getAttributeOrPropertyName(member));
            for (final AnnotationMirror annotation : annotations.findAttributeAndPropertyAnnotations(member)) {
                if (annotations.isAttributeAnnotation(annotation)) {
                    pluginAttributes.add(createPluginAttribute(
                            member,
                            description,
                            annotations
                                    .getAttributeSpecifiedName(annotation)
                                    .orElseGet(() -> getAttributeOrPropertyName(member))));
                } else {
                    pluginElements.add(createPluginElement(member, description));
                }
            }
        }
        pluginAttributes.forEach(pluginType::addAttribute);
        pluginElements.forEach(pluginType::addElement);
    }

    @Nullable
    private Description createDescription(final Element element, final @Nullable String fallbackDescriptionText) {
        @Nullable String descriptionText = converter.toAsciiDoc(element);
        if (StringUtils.isBlank(descriptionText)) {
            if (StringUtils.isBlank(fallbackDescriptionText)) {
                return null;
            } else {
                descriptionText = fallbackDescriptionText;
            }
        }
        descriptionText = descriptionText.trim();
        final Description description = new Description();
        description.setText(descriptionText);
        return description;
    }

    private PluginAttribute createPluginAttribute(
            final Element element, final String description, final String specifiedName) {
        final PluginAttribute attribute = new PluginAttribute();
        // Name
        attribute.setName(specifiedName.isEmpty() ? getAttributeOrPropertyName(element) : specifiedName);
        // Type
        final TypeMirror type = getMemberType(element);
        final String className = getClassName(type);
        // If type is not a well-known declared type, add it to the scanning queue.
        if (className != null && !KNOWN_SCALAR_TYPES.contains(className) && type instanceof DeclaredType) {
            scalarTypesToDocument.add(asTypeElement((DeclaredType) type));
        }
        attribute.setType(className);
        // Description
        attribute.setDescription(createDescription(element, description));
        // Required
        attribute.setRequired(annotations.hasRequiredConstraint(element));
        // Default value
        final Object defaultValue =
                element instanceof VariableElement ? ((VariableElement) element).getConstantValue() : null;
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
        pluginElement.setRequired(annotations.hasRequiredConstraint(element));
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
                        if (type instanceof DeclaredType) {
                            final TypeElement element = asTypeElement((DeclaredType) type);
                            final String className = element.getQualifiedName().toString();
                            abstractTypesToDocument.add(element);
                            if (supertypes.add(className)) {
                                element.accept(this, supertypes);
                            }
                        }
                    }
                },
                supertypes);
        return supertypes;
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
                        switch (parameters.size()) {
                                // A getter
                            case 0:
                                return returnType;
                                // A setter
                            case 1:
                                return parameters.get(0).asType();
                                // Invalid property
                            default:
                                return super.visitExecutable(element, unused);
                        }
                    }
                },
                null);
    }

    private String getAttributeOrPropertyName(final Element element) {
        return element.accept(
                new SimpleElementVisitor8<String, @Nullable Void>() {
                    @Override
                    protected String defaultAction(final Element e, @Nullable final Void unused) {
                        return e.getSimpleName().toString();
                    }

                    @Override
                    public String visitExecutable(final ExecutableElement e, final Void unused) {
                        final Name name = e.getSimpleName();
                        if (StringUtils.startsWithAny(name, GETTER_SETTER_PREFIXES)) {
                            final int prefixLen = StringUtils.startsWith(name, "is") ? 2 : 3;
                            if (name.length() > prefixLen) {
                                return Character.toLowerCase(name.charAt(prefixLen))
                                        + name.toString().substring(prefixLen + 1);
                            }
                        }
                        return super.visitExecutable(e, unused);
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
        return superclass instanceof DeclaredType ? asTypeElement((DeclaredType) superclass) : null;
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
                                        switch (t.getKind()) {
                                            case BOOLEAN:
                                                return "boolean";
                                            case BYTE:
                                                return "byte";
                                            case SHORT:
                                                return "short";
                                            case INT:
                                                return "int";
                                            case LONG:
                                                return "long";
                                            case CHAR:
                                                return "char";
                                            case FLOAT:
                                                return "float";
                                            case DOUBLE:
                                                return "double";
                                            default:
                                                throw new IllegalArgumentException();
                                        }
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
}
