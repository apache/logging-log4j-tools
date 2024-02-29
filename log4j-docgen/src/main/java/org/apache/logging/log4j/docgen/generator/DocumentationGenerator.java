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
import org.jspecify.annotations.Nullable;

public final class DocumentationGenerator {

    private DocumentationGenerator() {}

    public static void generateDocumentation(final DocumentationGeneratorArgs args) {
        requireNonNull(args, "args");
        final List<PluginSet> extendedSets = Stream.concat(BaseTypes.PLUGIN_SETS.stream(), args.pluginSets.stream())
                .collect(Collectors.toList());
        final TypeLookup lookup = TypeLookup.of(extendedSets, args.classNameFilter);
        final Collection<ArtifactSourcedType> scalarTypes = new TreeSet<>();
        lookup.forEach((className, sourcedType) -> {
            final Type type = sourcedType.type;
            if (type instanceof PluginType) {
                documentAbstractType(sourcedType, lookup, args.templateDirectory, args.pluginTemplate);
            } else if (type instanceof AbstractType) {
                documentAbstractType(sourcedType, lookup, args.templateDirectory, args.interfaceTemplate);
            } else if (type instanceof ScalarType) {
                scalarTypes.add(sourcedType);
            } else {
                throw new RuntimeException("unknown type: `" + type + "`");
            }
        });
        documentScalarTypes(scalarTypes, args.templateDirectory, args.scalarsTemplate);
    }

    private static void documentAbstractType(
            final ArtifactSourcedType sourcedType,
            final TypeLookup lookup,
            final Path templateDirectory,
            final DocumentationTemplate template) {
        final Map<String, Object> templateData = Map.of(
                "sourcedType", sourcedType,
                "lookup", lookup);
        final Path targetPath = createAbstractTypeTargetPath(sourcedType, template.targetPath);
        render(templateDirectory, template.name, templateData, targetPath);
    }

    private static Path createAbstractTypeTargetPath(
            final ArtifactSourcedType sourcedType, final String targetPathPattern) {
        final String groupId = or(sourcedType.groupId, "unknown-groupId");
        final String artifactId = or(sourcedType.artifactId, "unknown-artifactId");
        final String version = or(sourcedType.version, "unknown-version");
        final String targetPath = targetPathPattern
                .replaceAll("%g", groupId)
                .replaceAll("%a", artifactId)
                .replaceAll("%v", version)
                .replaceAll("%c", sourcedType.type.getClassName());
        return Path.of(targetPath);
    }

    private static String or(@Nullable final String value, final String fallback) {
        return value != null ? value : fallback;
    }

    private static void documentScalarTypes(
            final Collection<ArtifactSourcedType> scalarTypes,
            final Path templateDirectory,
            final DocumentationTemplate template) {
        final Map<String, Object> templateData = Map.of("sourcedTypes", scalarTypes);
        render(templateDirectory, template.name, templateData, Path.of(template.targetPath));
    }
}
