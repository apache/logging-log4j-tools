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
package org.apache.logging.log4j.docgen.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.apache.logging.log4j.docgen.util.TypeLookup;
import org.apache.logging.log4j.docgen.xsd.SchemaGenerator;
import org.apache.logging.log4j.docgen.xsd.SchemaGeneratorRequest;

@Singleton
@Named("default")
public class DefaultSchemaGenerator implements SchemaGenerator {

    private static final String LOG4J_PREFIX = "log4j";
    private static final String LOG4J_NAMESPACE = "http://logging.apache.org/log4j/2.0/config";
    private static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String MULTIPLICITY_UNBOUNDED = "*";
    private static final String UTF_8 = "UTF-8";

    @Override
    public void generateSchema(final SchemaGeneratorRequest request) throws XMLStreamException {
        try {
            final PluginSet configurationSet =
                    new PluginBundleStaxReader().read(getClass().getResourceAsStream("configuration.xml"));
            final List<PluginSet> extendedSets = new ArrayList<>(request.getPluginSets());
            extendedSets.add(configurationSet);
            final TypeLookup lookup = TypeLookup.of(extendedSets);
            final XMLOutputFactory factory = XMLOutputFactory.newFactory();
            final Path schemaPath = request.getOutputDirectory().resolve(request.getFileName());
            final XMLStreamWriter writer = factory.createXMLStreamWriter(Files.newOutputStream(schemaPath), UTF_8);
            try {
                writeSchema(lookup, writer);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private static void writeSchema(final TypeLookup lookup, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument(UTF_8, "1.0");
        writer.setDefaultNamespace(XSD_NAMESPACE);
        writer.writeStartElement(XSD_NAMESPACE, "schema");
        writer.writeDefaultNamespace(XSD_NAMESPACE);
        writer.writeNamespace(LOG4J_PREFIX, LOG4J_NAMESPACE);
        writer.writeAttribute("elementFormDefault", "qualified");
        writer.writeAttribute("targetNamespace", LOG4J_NAMESPACE);

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
        for (final Type type : lookup.values()) {
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
        final Description description = abstractType.getDescription();
        if (description != null) {
            writeDocumentation(description, writer);
        }
        writer.writeStartElement(XSD_NAMESPACE, "choice");

        final Set<String> implementations = new TreeSet<>(abstractType.getImplementations());
        if (abstractType instanceof PluginType) {
            implementations.add(abstractType.getClassName());
        }
        for (final String implementation : implementations) {
            final PluginType pluginType = (PluginType) lookup.get(implementation);
            final Collection<String> keys = new TreeSet<>(pluginType.getAliases());
            keys.add(pluginType.getName());
            for (final String key : keys) {
                writer.writeEmptyElement(XSD_NAMESPACE, "element");
                writer.writeAttribute("name", key);
                writer.writeAttribute("type", LOG4J_PREFIX + ":" + pluginType.getClassName());
            }
        }

        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeDocumentation(final Description description, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "annotation");
        writer.writeStartElement(XSD_NAMESPACE, "documentation");
        writer.writeCharacters(description.getText());
        writer.writeEndElement();
        writer.writeEndElement();
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
        final String xmlType = getXmlType(lookup, type);
        final AbstractType abstractType = (AbstractType) lookup.get(type);
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
            final Collection<String> keys = new TreeSet<>(pluginType.getAliases());
            keys.add(pluginType.getName());
            for (final String key : keys) {
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
        writer.writeStartElement(XSD_NAMESPACE, "attribute");
        writer.writeAttribute("name", attribute.getName());
        writer.writeAttribute("type", getXmlType(lookup, attribute.getType()));
        final Description description = attribute.getDescription();
        if (description != null) {
            writeDocumentation(description, writer);
        }
        writer.writeEndElement();
    }

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
        final Type type = lookup.get(className);
        if (type != null) {
            return LOG4J_PREFIX + ":" + className;
        }
        throw new IllegalArgumentException("Unknown Java type '" + className + "'.");
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
}
