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

public final class DocumentationGenerator {

    private DocumentationGenerator() {}

    public static void generateDocumentation(final DocumentationGeneratorArgs args) {
        requireNonNull(args, "args");
        final List<PluginSet> extendedSets = Stream.concat(BaseTypes.PLUGIN_SETS.stream(), args.pluginSets.stream())
                .collect(Collectors.toList());
        final TypeLookup lookup = TypeLookup.of(extendedSets);
        final Collection<ScalarType> scalarTypes = new TreeSet<>();
        lookup.forEach((typeKey, type) -> {
            if (type instanceof PluginType) {
                documentAbstractType((AbstractType) type, lookup, args.templateDirectory, args.pluginTemplate);
            } else if (type instanceof AbstractType) {
                documentAbstractType((AbstractType) type, lookup, args.templateDirectory, args.interfaceTemplate);
            } else if (type instanceof ScalarType) {
                scalarTypes.add((ScalarType) type);
            } else {
                throw new RuntimeException("unknown type: `" + type + "`");
            }
        });
        documentScalarTypes(scalarTypes, args.templateDirectory, args.scalarsTemplate);
    }

    private static void documentAbstractType(
            final AbstractType abstractType,
            final TypeLookup lookup,
            final Path templateDirectory,
            final DocumentationTemplate template) {
        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("type", abstractType);
        templateData.put("lookup", lookup);
        final Path targetPath = createAbstractTypeTargetPath(abstractType, template.targetPath);
        render(templateDirectory, template.name, templateData, targetPath);
    }

    private static Path createAbstractTypeTargetPath(final AbstractType abstractType, final String targetPathPattern) {
        final String targetPath = targetPathPattern.replaceAll("%c", abstractType.getClassName());
        return Path.of(targetPath);
    }

    private static void documentScalarTypes(
            final Collection<ScalarType> scalarTypes,
            final Path templateDirectory,
            final DocumentationTemplate template) {
        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("scalars", scalarTypes);
        render(templateDirectory, template.name, templateData, Path.of(template.targetPath));
    }
}
