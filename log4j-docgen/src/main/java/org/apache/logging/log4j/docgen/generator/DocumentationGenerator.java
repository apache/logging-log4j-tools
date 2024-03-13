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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.generator.internal.ArtifactSourcedType;
import org.apache.logging.log4j.docgen.generator.internal.TypeLookup;
import org.jspecify.annotations.Nullable;

public final class DocumentationGenerator {

    private DocumentationGenerator() {}

    public static void generateDocumentation(final DocumentationGeneratorArgs args) {
        requireNonNull(args, "args");
        final List<PluginSet> extendedSets = Stream.concat(BaseTypes.PLUGIN_SETS.stream(), args.pluginSets.stream())
                .collect(Collectors.toList());
        final TypeLookup lookup = TypeLookup.of(extendedSets, args.classNameFilter);
        lookup.values()
                .forEach(sourcedType -> renderType(sourcedType, lookup, args.templateDirectory, args.typeTemplate));
        renderIndex(lookup, args.templateDirectory, args.indexTemplate);
    }

    private static void renderType(
            final ArtifactSourcedType sourcedType,
            final TypeLookup lookup,
            final Path templateDirectory,
            final DocumentationTemplate template) {
        final Map<String, Object> templateData = Map.of(
                "sourcedType", sourcedType,
                "lookup", lookup);
        final Path targetPath = createTypeTargetPath(sourcedType, template.targetPath);
        render(templateDirectory, template.name, templateData, targetPath);
    }

    private static Path createTypeTargetPath(final ArtifactSourcedType sourcedType, final String targetPathPattern) {
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

    private static void renderIndex(
            final TypeLookup lookup, final Path templateDirectory, final DocumentationTemplate template) {
        final Map<String, Object> templateData = Map.of("lookup", lookup);
        render(templateDirectory, template.name, templateData, Path.of(template.targetPath));
    }
}
