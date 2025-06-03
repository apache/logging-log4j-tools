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
package org.apache.logging.log4j.docgen.generator;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.Description;
import org.apache.logging.log4j.docgen.PluginAttribute;
import org.apache.logging.log4j.docgen.PluginElement;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.apache.logging.log4j.docgen.ScalarType;
import org.apache.logging.log4j.docgen.ScalarValue;
import org.apache.logging.log4j.docgen.Type;
import org.apache.logging.log4j.docgen.generator.internal.ArtifactSourcedType;
import org.apache.logging.log4j.docgen.generator.internal.TypeLookup;
import org.jspecify.annotations.Nullable;

@Singleton
@Named("default")
public final class SchemaGenerator {

    private static final String LOG4J_PREFIX = "log4j";

    private static final String LOG4J_NAMESPACE = "https://logging.apache.org/xml/ns";

    private static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    private static final String MULTIPLICITY_UNBOUNDED = "*";

    private static final String CHARSET_NAME = "UTF-8";

    private static final String PROPERTY_SUBSTITUTION_TYPE = "property-substitution";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final String STRING_TYPE = "string";
    private static final ScalarType BOOLEAN_SCALAR_TYPE = new ScalarType();

    static {
        BOOLEAN_SCALAR_TYPE.setClassName(BOOLEAN_TYPE);
        final Description description = new Description();
        description.setText(
                "A custom boolean type that allows `true`, `false`, or a property substitution expression.");
        BOOLEAN_SCALAR_TYPE.setDescription(description);
        for (final Boolean value : new Boolean[] {true, false}) {
            final ScalarValue scalarValue = new ScalarValue();
            scalarValue.setName(value.toString());
            BOOLEAN_SCALAR_TYPE.addValue(scalarValue);
        }
    }

    private static final Map<String, String> XML_BUILTIN_TYPES = Map.ofEntries(
            entry(BigDecimal.class.getName(), "decimal"),
            entry(BigInteger.class.getName(), "integer"),
            entry(boolean.class.getName(), "boolean"),
            entry(Boolean.class.getName(), "boolean"),
            entry(byte.class.getName(), "byte"),
            entry(Byte.class.getName(), "byte"),
            entry(double.class.getName(), "double"),
            entry(Double.class.getName(), "double"),
            entry(float.class.getName(), "float"),
            entry(Float.class.getName(), "float"),
            entry(int.class.getName(), "int"),
            entry(Integer.class.getName(), "int"),
            entry(short.class.getName(), "short"),
            entry(Short.class.getName(), "short"),
            entry(String.class.getName(), "string"),
            entry(long.class.getName(), "long"),
            entry(Long.class.getName(), "long"),
            entry(URI.class.getName(), "anyURI"),
            entry(URL.class.getName(), "anyURI"));

    private SchemaGenerator() {}

    public static void generateSchema(final SchemaGeneratorArgs args) throws XMLStreamException {
        requireNonNull(args, "args");
        try {
            final List<PluginSet> extendedSets = Stream.concat(BaseTypes.PLUGIN_SETS.stream(), args.pluginSets.stream())
                    .collect(Collectors.toList());
            final TypeLookup lookup = TypeLookup.of(extendedSets, args.classNameFilter);
            final XMLOutputFactory factory = XMLOutputFactory.newFactory();
            @Nullable final Path schemaFileParent = args.schemaFile.getParent();
            if (schemaFileParent != null) {
                Files.createDirectories(schemaFileParent);
            }
            try (final OutputStream schemaPathOutputStream = Files.newOutputStream(args.schemaFile)) {
                final XMLStreamWriter writer = factory.createXMLStreamWriter(schemaPathOutputStream, CHARSET_NAME);
                try {
                    writeSchema(args.schemaVersion, lookup, writer);
                } finally {
                    writer.close();
                }
            }
        } catch (final IOException error) {
            throw new XMLStreamException(error);
        }
    }

    private static void writeSchema(final String version, final TypeLookup lookup, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartDocument(CHARSET_NAME, "1.0");
        writer.setDefaultNamespace(XSD_NAMESPACE);
        writer.writeStartElement(XSD_NAMESPACE, "schema");
        writer.writeDefaultNamespace(XSD_NAMESPACE);
        writer.writeNamespace(LOG4J_PREFIX, LOG4J_NAMESPACE);
        writer.writeAttribute("elementFormDefault", "qualified");
        writer.writeAttribute("targetNamespace", LOG4J_NAMESPACE);
        writer.writeAttribute("version", version);

        // The root element
        writer.writeEmptyElement(XSD_NAMESPACE, "element");
        writer.writeAttribute("type", LOG4J_PREFIX + ":org.apache.logging.log4j.core.config.Configuration");
        writer.writeAttribute("name", "Configuration");

        // Write all types in alphabetical order
        writeTypes(lookup, writer);

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private static void writeTypes(final TypeLookup lookup, final XMLStreamWriter writer) throws XMLStreamException {
        writePropertySubstitutionType(writer);
        // A union with member types `xsd:boolean` and `log4j:property-substitution` does not allow auto-completion
        // in IDEs. This is why we define a `log4j:boolean` type from scratch.
        writeScalarType(BOOLEAN_SCALAR_TYPE, writer);
        writeUnionBuiltinTypes(writer);

        for (final ArtifactSourcedType sourcedType : lookup.values()) {
            final Type type = sourcedType.type;
            if (isBuiltinXmlType(type.getClassName())) {
                continue;
            }
            if (type instanceof ScalarType) {
                writeScalarType((ScalarType) type, writer);
            }
            if (type instanceof PluginType) {
                final PluginType pluginType = (PluginType) type;
                writePluginType(lookup, pluginType, writer);
                /*
                 * If a plugin extends another plugin or has multiple aliases
                 * we also need a <group> element.
                 */
                if (!pluginType.getAliases().isEmpty()
                        || !pluginType.getImplementations().isEmpty()) {
                    writeAbstractType(lookup, pluginType, writer);
                }
            } else if (type instanceof AbstractType) {
                writeAbstractType(lookup, (AbstractType) type, writer);
            }
        }
    }

    private static boolean isBuiltinXmlType(final String className) {
        return XML_BUILTIN_TYPES.containsKey(className);
    }

    /**
     * A restriction of {@code string} that requires at least one property substitution expression {@code ${...}}.
     */
    private static void writePropertySubstitutionType(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "simpleType");
        writer.writeAttribute("name", PROPERTY_SUBSTITUTION_TYPE);

        writeDocumentation("A string with a property substitution expression.", writer);

        writer.writeStartElement(XSD_NAMESPACE, "restriction");
        writer.writeAttribute("base", "string");

        writer.writeEmptyElement(XSD_NAMESPACE, "pattern");
        writer.writeAttribute("value", ".*\\$\\{.*\\}.*");

        writer.writeEndElement();
        writer.writeEndElement();
    }

    /**
     * Define types that are the union of a builtin type and {@value PROPERTY_SUBSTITUTION_TYPE}.
     * <p>
     *     IDEs don't propose auto-completion for these types.
     * </p>
     */
    private static void writeUnionBuiltinTypes(final XMLStreamWriter writer) throws XMLStreamException {
        final Collection<String> types = new TreeSet<>(XML_BUILTIN_TYPES.values());
        // `xsd:string` is a superset of PROPERTY_SUBSTITUTION_TYPE, so no union is needed.
        types.remove(STRING_TYPE);
        // The union of `xsd:boolean` with PROPERTY_SUBSTITUTION_TYPE does not show auto-completion in IDEs.
        // `log4j:boolean` will be generated from an _ad-hoc_ ScalarType definition in `base-log4j-types.xml`.
        types.remove(BOOLEAN_TYPE);
        for (final String type : types) {
            writeUnionBuiltinType(type, writer);
        }
    }

    private static void writeUnionBuiltinType(final String type, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "simpleType");
        writer.writeAttribute("name", type);

        writeDocumentation("Union of `xsd:" + type + "` and ` " + PROPERTY_SUBSTITUTION_TYPE + "`.", writer);

        writer.writeEmptyElement(XSD_NAMESPACE, "union");
        writer.writeAttribute("memberTypes", type + " log4j:" + PROPERTY_SUBSTITUTION_TYPE);

        writer.writeEndElement();
    }

    private static void writeScalarType(final ScalarType type, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "simpleType");
        writer.writeAttribute("name", type.getClassName());

        writeDocumentation(type.getDescription(), writer);

        writer.writeStartElement(XSD_NAMESPACE, "union");
        writer.writeAttribute("memberTypes", "log4j:" + PROPERTY_SUBSTITUTION_TYPE);
        writer.writeStartElement(XSD_NAMESPACE, "simpleType");

        writer.writeStartElement(XSD_NAMESPACE, "restriction");
        writer.writeAttribute("base", "string");

        for (final ScalarValue value : type.getValues()) {
            writeScalarValue(value, writer);
        }

        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writePluginType(
            final TypeLookup lookup, final PluginType pluginType, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "complexType");
        writer.writeAttribute("name", pluginType.getClassName());

        writeDocumentation(pluginType.getDescription(), writer);

        final boolean hasComplexContent = !pluginType.getElements().isEmpty();

        if (hasComplexContent) {
            writer.writeStartElement(XSD_NAMESPACE, "sequence");
            for (final PluginElement element : pluginType.getElements()) {
                writePluginElement(lookup, element, writer);
            }
            writer.writeEndElement();
        }

        for (final PluginAttribute attribute : pluginType.getAttributes()) {
            writePluginAttribute(lookup, attribute, writer);
        }

        writer.writeEndElement();
    }

    private static void writeAbstractType(
            final TypeLookup lookup, final AbstractType abstractType, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "group");
        writer.writeAttribute("name", abstractType.getClassName());
        writeDocumentation(abstractType.getDescription(), writer);
        writer.writeStartElement(XSD_NAMESPACE, "choice");

        final Set<String> implementations = abstractType.getImplementations();
        if (abstractType instanceof PluginType) {
            implementations.add(abstractType.getClassName());
        }
        for (final String implementation : implementations) {
            @Nullable final ArtifactSourcedType sourcedType = lookup.get(implementation);
            if (sourcedType != null && sourcedType.type instanceof PluginType) {
                final PluginType pluginType = (PluginType) sourcedType.type;
                for (final String key : getKeyAndAliases(pluginType)) {
                    writer.writeEmptyElement(XSD_NAMESPACE, "element");
                    writer.writeAttribute("name", key);
                    writer.writeAttribute("type", LOG4J_PREFIX + ":" + pluginType.getClassName());
                }
            }
        }

        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeDocumentation(@Nullable final Description description, final XMLStreamWriter writer)
            throws XMLStreamException {
        if (description != null) {
            writeDocumentation(description.getText(), writer);
        }
    }

    private static void writeDocumentation(final String text, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "annotation");
        writer.writeStartElement(XSD_NAMESPACE, "documentation");
        writer.writeCharacters(text);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeScalarValue(final ScalarValue value, final XMLStreamWriter writer)
            throws XMLStreamException {
        final Description description = value.getDescription();
        if (description != null) {
            writer.writeStartElement(XSD_NAMESPACE, "enumeration");
            writer.writeAttribute("value", value.getName());
            writeDocumentation(value.getDescription(), writer);
            writer.writeEndElement();
        } else {
            writer.writeEmptyElement(XSD_NAMESPACE, "enumeration");
            writer.writeAttribute("value", value.getName());
        }
    }

    private static void writePluginElement(
            final TypeLookup lookup, final PluginElement element, final XMLStreamWriter writer)
            throws XMLStreamException {
        final String type = element.getType();
        @Nullable final String xmlType = getXmlType(lookup, type);
        if (xmlType == null) {
            return;
        }
        final ArtifactSourcedType sourcedType = lookup.get(type);
        if (sourcedType == null) {
            return;
        }
        final Type rawType = sourcedType.type;
        if (!(rawType instanceof AbstractType)) {
            return;
        }
        final AbstractType abstractType = (AbstractType) rawType;
        final PluginType pluginType = abstractType instanceof PluginType ? (PluginType) abstractType : null;
        /*
         * If a plugin extends another plugin or has multiple aliases
         * we use a <group> element.
         */
        if (pluginType == null
                || !pluginType.getAliases().isEmpty()
                || !pluginType.getImplementations().isEmpty()) {
            writer.writeStartElement(XSD_NAMESPACE, "group");
            writer.writeAttribute("ref", xmlType);
            writeMultiplicity(element.isRequired(), element.getMultiplicity(), writer);
            writeDocumentation(element.getDescription(), writer);
            writer.writeEndElement();
        } else {
            for (final String key : getKeyAndAliases(pluginType)) {
                writer.writeStartElement(XSD_NAMESPACE, "element");
                writer.writeAttribute("name", key);
                writer.writeAttribute("type", xmlType);
                writeMultiplicity(element.isRequired(), element.getMultiplicity(), writer);
                writeDocumentation(element.getDescription(), writer);
                writer.writeEndElement();
            }
        }
    }

    private static void writePluginAttribute(
            final TypeLookup lookup, final PluginAttribute attribute, final XMLStreamWriter writer)
            throws XMLStreamException {
        final String xmlType = getXmlType(lookup, attribute.getType());
        final Description description = attribute.getDescription();
        if (description != null) {
            writer.writeStartElement(XSD_NAMESPACE, "attribute");
        } else {
            writer.writeEmptyElement(XSD_NAMESPACE, "attribute");
        }
        writer.writeAttribute("name", attribute.getName());
        // If the type is unknown, use `string`
        writer.writeAttribute("type", xmlType != null ? xmlType : "string");
        if (description != null) {
            writeDocumentation(description, writer);
            writer.writeEndElement();
        }
    }

    @Nullable
    private static String getXmlType(final TypeLookup lookup, final String className) {
        final String builtinType = XML_BUILTIN_TYPES.get(className);
        if (builtinType != null) {
            // Use the union types for all built-in types, except `string`.
            return STRING_TYPE.equals(builtinType) ? STRING_TYPE : LOG4J_PREFIX + ":" + builtinType;
        }
        final ArtifactSourcedType type = lookup.get(className);
        return type != null ? LOG4J_PREFIX + ":" + className : null;
    }

    private static void writeMultiplicity(
            final boolean required, final String multiplicity, final XMLStreamWriter writer) throws XMLStreamException {
        if (!required) {
            writer.writeAttribute("minOccurs", "0");
        }
        if (MULTIPLICITY_UNBOUNDED.equals(multiplicity)) {
            writer.writeAttribute("maxOccurs", "unbounded");
        }
    }

    private static Collection<String> getKeyAndAliases(final PluginType pluginType) {
        final Collection<String> keys = new ArrayList<>();
        keys.add(pluginType.getName());
        keys.addAll(pluginType.getAliases());
        return keys;
    }
}
