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
import static org.apache.logging.log4j.tools.internal.freemarker.util.FreeMarkerUtils.render;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.apache.logging.log4j.docgen.ScalarType;
import org.apache.logging.log4j.docgen.Type;

public final class DocumentationGenerator {

    private DocumentationGenerator() {}

    public static void generateDocumentation(final DocumentationGeneratorArgs args) {
        requireNonNull(args, "args");
        final List<PluginSet> extendedSets = Stream.concat(
                        Stream.of(ConfigurationXml.PLUGIN_SET), args.pluginSets.stream())
                .collect(Collectors.toList());
        final TypeLookup lookup = TypeLookup.of(extendedSets);
        final Collection<ScalarType> scalarTypes = new TreeSet<>(Comparator.comparing(ScalarType::getClassName));
        for (final Type type : lookup.values()) {
            if (type instanceof AbstractType) {
                final String template =
                        type instanceof PluginType ? args.pluginTemplateName : args.interfaceTemplateName;
                documentAbstractType(
                        (AbstractType) type, lookup, args.templateDirectory, template, args.outputDirectory);
            }
            if (type instanceof ScalarType) {
                scalarTypes.add((ScalarType) type);
            }
        }
        documentScalarTypes(scalarTypes, args.templateDirectory, args.scalarsTemplateName, args.outputDirectory);
    }

    private static void documentAbstractType(
            final AbstractType abstractType,
            final TypeLookup lookup,
            final Path templateDirectory,
            final String templateName,
            final Path outputDir) {
        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("type", abstractType);
        templateData.put("lookup", lookup);
        final Path outputFile = createOutputFile(abstractType, outputDir);
        render(templateDirectory, templateName, templateData, outputFile);
    }

    private static void documentScalarTypes(
            final Collection<ScalarType> scalarTypes,
            final Path templateDirectory,
            final String templateName,
            final Path outputDir) {
        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("scalars", scalarTypes);
        final Path outputFile = outputDir.resolve("scalars.adoc");
        render(templateDirectory, templateName, templateData, outputFile);
    }

    private static Path createOutputFile(final Type type, final Path outputDir) {
        final Path outputFile = outputDir.resolve("Core").resolve(type.getClassName() + ".adoc");
        if (!outputFile.startsWith(outputDir)) {
            throw new RuntimeException("Class name '" + type.getClassName() + "' caused a path traversal attempt.");
        }
        return outputFile;
    }
}
