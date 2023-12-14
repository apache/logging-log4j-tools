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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.logging.log4j.docgen.SchemaGenerator;
import org.apache.logging.log4j.docgen.model.Description;
import org.apache.logging.log4j.docgen.model.EnumType;
import org.apache.logging.log4j.docgen.model.EnumValue;
import org.apache.logging.log4j.docgen.model.PluginAttribute;
import org.apache.logging.log4j.docgen.model.PluginBundle;
import org.apache.logging.log4j.docgen.model.PluginElement;
import org.apache.logging.log4j.docgen.model.PluginEntry;
import org.apache.logging.log4j.docgen.model.io.stax.PluginBundleStaxReader;

@Singleton
@Named("default")
public class DefaultSchemaGenerator implements SchemaGenerator {

    private static final String PLUGIN_NAMESPACE = "Core";
    private static final String LOG4J_PREFIX = "log4j";
    private static final String LOG4J_NAMESPACE = "http://logging.apache.org/log4j/2.0/config";
    private static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String MULTIPLICITY_UNBOUNDED = "*";

    @Override
    public void generateSchema(Collection<PluginBundle> bundles, XMLStreamWriter writer) throws XMLStreamException {
        try {
            final PluginBundle configurationBundle =
                    new PluginBundleStaxReader().read(getClass().getResourceAsStream("configuration.xml"));
            final Set<PluginBundle> extendedBundles = new HashSet<>(bundles);
            extendedBundles.add(configurationBundle);
            final TypeLookup lookup = new TypeLookup(extendedBundles);
            writeSchema(lookup, writer);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void writeSchema(final TypeLookup lookup, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument("UTF-8", "1.0");
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

        // Simple types
        writeEnumTypes(lookup.getRequiredEnums(), writer);

        // Plugins
        writePluginEntries(lookup, writer);

        // Interfaces
        writeGroups(lookup, lookup.getRequiredGroups(), writer);

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void writePluginEntries(final TypeLookup lookup, final XMLStreamWriter writer) throws XMLStreamException {
        for (final PluginEntry entry : lookup.getPlugins()) {
            writePluginEntry(lookup, entry, writer);
        }
    }

    private void writePluginEntry(final TypeLookup lookup, final PluginEntry entry, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "complexType");
        writer.writeAttribute("name", entry.getClassName());

        writeDocumentation(entry.getDescription(), writer);

        final boolean hasSimpleContent = entry.getElements().isEmpty();

        if (!hasSimpleContent) {
            writer.writeStartElement(XSD_NAMESPACE, "sequence");
            for (final PluginElement element : entry.getElements()) {
                writePluginElement(lookup, element, writer);
            }
            writer.writeEndElement();
        }

        for (final PluginAttribute attribute : entry.getAttributes()) {
            writePluginAttribute(lookup, attribute, writer);
        }

        writer.writeEndElement();
    }

    private void writePluginElement(final TypeLookup lookup, final PluginElement element, final XMLStreamWriter writer)
            throws XMLStreamException {
        final String type = element.getType();
        final String xmlType = lookup.getXmlType(type, false);
        final PluginEntry entry = lookup.getPluginByType(type);
        /*
         * If some plugins have `type` as super type or if the type is unknown,
         * we use a <group> element.
         */
        if (lookup.getPluginsByGroup(type) != null || entry == null) {
            writer.writeStartElement(XSD_NAMESPACE, "group");
            writer.writeAttribute("ref", xmlType);
            writeMultiplicity(element.isRequired(), element.getMultiplicity(), writer);
            writeDocumentation(element.getDescription(), writer);
            writer.writeEndElement();
        } else {
            final Set<String> actualKeys = new TreeSet<>(entry.getKeys());
            actualKeys.add(entry.getName());
            for (final String key : actualKeys) {
                writer.writeStartElement(XSD_NAMESPACE, "element");
                writer.writeAttribute("name", key);
                writer.writeAttribute("type", xmlType);
                writeMultiplicity(element.isRequired(), element.getMultiplicity(), writer);
                writeDocumentation(element.getDescription(), writer);
                writer.writeEndElement();
            }
        }
    }

    private void writeMultiplicity(final boolean required, final String multiplicity, final XMLStreamWriter writer)
            throws XMLStreamException {
        if (!required) {
            writer.writeAttribute("minOccurs", "0");
        }
        if (MULTIPLICITY_UNBOUNDED.equals(multiplicity)) {
            writer.writeAttribute("maxOccurs", "unbounded");
        }
    }

    private void writePluginAttribute(
            final TypeLookup lookup, final PluginAttribute attribute, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "attribute");
        writer.writeAttribute("name", attribute.getName());
        writer.writeAttribute("type", lookup.getXmlType(attribute.getType(), true));
        final Description description = attribute.getDescription();
        if (description != null) {
            writeDocumentation(description, writer);
        }
        writer.writeEndElement();
    }

    private void writeDocumentation(final Description description, final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "annotation");
        writer.writeStartElement(XSD_NAMESPACE, "documentation");
        writer.writeCharacters(normalizeAsciidoc(description.getText()));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private String normalizeAsciidoc(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (final String line : text.split("\r?\n", -1)) {
            sb.append(line.trim()).append('\n');
        }
        final int length = sb.length();
        if (length > 0) {
            sb.setLength(length - 1);
        }
        return sb.toString();
    }

    private void writeEnumTypes(final Collection<EnumType> types, final XMLStreamWriter writer)
            throws XMLStreamException {
        for (final EnumType type : types) {
            writeEnumType(type, writer);
        }
    }

    private void writeGroups(final TypeLookup lookup, final Collection<String> groups, final XMLStreamWriter writer)
            throws XMLStreamException {
        for (final String group : groups) {
            writeGroup(
                    lookup,
                    group,
                    Optional.ofNullable(lookup.getPluginsByGroup(group)).orElse(Collections.emptySet()),
                    writer);
        }
    }

    private void writeGroup(
            final TypeLookup lookup,
            final String groups,
            final Collection<PluginEntry> entries,
            final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "group");
        writer.writeAttribute("name", groups + ".group");
        writer.writeStartElement(XSD_NAMESPACE, "choice");

        for (final PluginEntry entry : entries) {
            final Set<String> actualKeys = new TreeSet<>(entry.getKeys());
            actualKeys.add(entry.getName());
            for (final String key : actualKeys) {
                writer.writeEmptyElement(XSD_NAMESPACE, "element");
                writer.writeAttribute("name", key);
                writer.writeAttribute("type", LOG4J_PREFIX + ":" + entry.getClassName());
            }
        }

        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeEnumType(final EnumType type, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "simpleType");
        writer.writeAttribute("name", type.getClassName());

        writeDocumentation(type.getDescription(), writer);

        writer.writeStartElement(XSD_NAMESPACE, "restriction");
        writer.writeAttribute("base", "string");

        for (final EnumValue value : type.getValues()) {
            writeEnumValue(value, writer);
        }

        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeEnumValue(final EnumValue value, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(XSD_NAMESPACE, "enumeration");
        writer.writeAttribute("value", value.getName());

        writeDocumentation(value, writer);

        writer.writeEndElement();
    }

    private static final class TypeLookup {

        private final Map<String, EnumType> enumByName = new HashMap<>();
        private final Map<String, PluginEntry> pluginsByName = new TreeMap<>();
        private final Map<String, Set<PluginEntry>> pluginsByGroup = new HashMap<>();
        private final Set<EnumType> requiredEnums = new TreeSet<>(Comparator.comparing(EnumType::getClassName));
        private final Set<String> requiredGroups = new TreeSet<>();

        public TypeLookup(final Collection<PluginBundle> bundles) {
            bundles.forEach(bundle -> {
                bundle.getPlugins().forEach(entry -> {
                    if (PLUGIN_NAMESPACE.equals(entry.getNamespace())) {
                        pluginsByName.put(entry.getClassName(), entry);
                        entry.getSupertypes().forEach(type -> pluginsByGroup
                                .computeIfAbsent(
                                        type, ignored -> new TreeSet<>(Comparator.comparing(PluginEntry::getName)))
                                .add(entry));
                    }
                });
                bundle.getEnums().forEach(enumType -> enumByName.put(enumType.getClassName(), enumType));
            });
            // Validates the required types.
            validateTypes();
        }

        private void validateTypes() {
            getPlugins().forEach(entry -> {
                entry.getAttributes().forEach(attribute -> getXmlType(attribute.getType(), true));
                entry.getElements().forEach(element -> getXmlType(element.getType(), false));
            });
        }

        public Collection<EnumType> getRequiredEnums() {
            return requiredEnums;
        }

        public Collection<String> getRequiredGroups() {
            return requiredGroups;
        }

        public Set<PluginEntry> getPluginsByGroup(final String group) {
            return pluginsByGroup.get(group);
        }

        public Collection<PluginEntry> getPlugins() {
            return pluginsByName.values();
        }

        public PluginEntry getPluginByType(final String javaType) {
            return pluginsByName.get(javaType);
        }

        public String getXmlType(final String javaType, final boolean simple) {
            switch (javaType) {
                case "boolean":
                case "byte":
                case "double":
                case "float":
                case "int":
                case "short":
                case "long":
                    return javaType;
                case "java.lang.String":
                    return "string";
            }
            if (enumByName.containsKey(javaType)) {
                requiredEnums.add(enumByName.get(javaType));
                return LOG4J_PREFIX + ":" + javaType;
            }
            if (!simple) {
                final PluginEntry entry = pluginsByName.get(javaType);
                if (pluginsByGroup.get(javaType) == null
                        && entry != null
                        && entry.getKeys().isEmpty()) {
                    return LOG4J_PREFIX + ":" + javaType;
                }
                requiredGroups.add(javaType);
                // Add also the base class to the group
                if (entry != null) {
                    pluginsByGroup
                            .computeIfAbsent(javaType, ignored -> new HashSet<>())
                            .add(entry);
                }
                return LOG4J_PREFIX + ":" + javaType + ".group";
            }
            throw new IllegalArgumentException("Unknown simple Java type '" + javaType + "'.");
        }
    }
}
