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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
import org.jspecify.annotations.Nullable;

@Singleton
@Named("default")
public final class SchemaGenerator {

    private static final String LOG4J_PREFIX = "log4j";

    private static final String LOG4J_NAMESPACE = "https://logging.apache.org/xml/ns";

    private static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    private static final String VERSION = new PluginSet().getSchemaVersion();

    private static final String MULTIPLICITY_UNBOUNDED = "*";

    private static final String CHARSET_NAME = "UTF-8";

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
                    writeSchema(lookup, writer);
                } finally {
                    writer.close();
                }
            }
        } catch (final IOException error) {
            throw new XMLStreamException(error);
        }
    }

    private static void writeSchema(final TypeLookup lookup, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument(CHARSET_NAME, "1.0");
        writer.setDefaultNamespace(XSD_NAMESPACE);
        writer.writeStartElement(XSD_NAMESPACE, "schema");
        writer.writeDefaultNamespace(XSD_NAMESPACE);
        writer.writeNamespace(LOG4J_PREFIX, LOG4J_NAMESPACE);
        writer.writeAttribute("elementFormDefault", "qualified");
        writer.writeAttribute("targetNamespace", LOG4J_NAMESPACE);
        writer.writeAttribute("version", VERSION);

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
        switch (className) {
            case "boolean":
            case "byte":
            case "double":
            case "float":
            case "int":
            case "short":
            case "long":
            case "java.lang.String":
                return true;
            default:
                return false;
        }
    }

    private static void writeScalarType(final ScalarType type, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "simpleType");
        writer.writeAttribute("name", type.getClassName());

        writeDocumentation(type.getDescription(), writer);

        writer.writeStartElement(XSD_NAMESPACE, "restriction");
        writer.writeAttribute("base", "string");

        for (final ScalarValue value : type.getValues()) {
            writeScalarValue(value, writer);
        }

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
            writer.writeStartElement(XSD_NAMESPACE, "annotation");
            writer.writeStartElement(XSD_NAMESPACE, "documentation");
            writer.writeCharacters(description.getText());
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private static void writeScalarValue(final ScalarValue value, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "enumeration");
        writer.writeAttribute("value", value.getName());

        writeDocumentation(value.getDescription(), writer);

        writer.writeEndElement();
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
        @Nullable final String xmlType = getXmlType(lookup, attribute.getType());
        if (xmlType == null) {
            return;
        }
        writer.writeStartElement(XSD_NAMESPACE, "attribute");
        writer.writeAttribute("name", attribute.getName());
        writer.writeAttribute("type", xmlType);
        final Description description = attribute.getDescription();
        if (description != null) {
            writeDocumentation(description, writer);
        }
        writer.writeEndElement();
    }

    @Nullable
    private static String getXmlType(final TypeLookup lookup, final String className) {
        switch (className) {
            case "boolean":
            case "byte":
            case "double":
            case "float":
            case "int":
            case "short":
            case "long":
                return className;
            case "java.lang.String":
                return "string";
        }
        final ArtifactSourcedType type = lookup.get(className);
        if (type != null) {
            return LOG4J_PREFIX + ":" + className;
        }
        return null;
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
