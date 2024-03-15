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
package org.apache.logging.log4j.docgen.asciidoctor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.generator.internal.ArtifactSourcedType;
import org.apache.logging.log4j.docgen.generator.internal.TypeLookup;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Format;
import org.asciidoctor.extension.FormatType;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.PositionalAttributes;
import org.jspecify.annotations.Nullable;

@Name("apiref")
@Format(FormatType.LONG)
@PositionalAttributes({"label"})
public final class ApirefMacro extends InlineMacroProcessor {

    private static final Logger LOGGER = Logger.getLogger(ApirefMacro.class.getName());

    /**
     * A regular expression that never matches.
     */
    private static final String IMPOSSIBLE_REGEX = "(?!.*)";

    private TypeLookup lookup;

    private String typeTemplateTarget;

    private boolean packageNameStripped;

    private boolean initialized = false;

    @Override
    public PhraseNode process(final StructuralNode parent, final String target, final Map<String, Object> attributes) {
        initialize(parent);
        return createPhraseNode(parent, target, attributes);
    }

    private PhraseNode createPhraseNode(
            final StructuralNode parent, final String target, final Map<String, Object> attributes) {
        final int methodSplitterIndex = target.indexOf('#');
        final boolean methodProvided = methodSplitterIndex > 0;
        final String className = methodProvided ? target.substring(0, methodSplitterIndex) : target;
        @Nullable final String label = (String) attributes.get("label");

        // If this is a type we expect to find in an AsciiDoc file produced by `DocumentationGenerator`
        @Nullable final ArtifactSourcedType sourcedType = lookup.get(className);
        if (sourcedType != null) {
            Map<String, Object> nodeOptions = new HashMap<>();
            nodeOptions.put("type", ":xref");
            nodeOptions.put("target", createTypeTemplateTargetPath(sourcedType));
            final String effectiveLabel = label != null ? label : className.substring(className.lastIndexOf('.') + 1);
            return createPhraseNode(parent, "anchor", effectiveLabel, attributes, nodeOptions);
        }

        // Otherwise we don't know the link
        if (label != null) {
            return createPhraseNode(parent, "quoted", "<em>" + label + "</em>", attributes);
        }
        final String effectiveLabel = packageNameStripped ? target.replaceFirst("^([a-z][a-z0-9_]*\\.)*", "") : target;
        return createPhraseNode(parent, "quoted", "<code>" + effectiveLabel + "</code>", attributes);
    }

    private String createTypeTemplateTargetPath(final ArtifactSourcedType sourcedType) {
        final String groupId = or(sourcedType.groupId, "unknown-groupId");
        final String artifactId = or(sourcedType.artifactId, "unknown-artifactId");
        final String version = or(sourcedType.version, "unknown-version");
        return typeTemplateTarget
                .replaceAll("%g", groupId)
                .replaceAll("%a", artifactId)
                .replaceAll("%v", version)
                .replaceAll("%c", sourcedType.type.getClassName());
    }

    private static String or(@Nullable final String value, final String fallback) {
        return value != null ? value : fallback;
    }

    private void initialize(final StructuralNode node) {
        if (!initialized) {
            LOGGER.fine("Initializing...");
            lookup = createTypeLookup(node);
            final Map<String, Object> documentAttributes = node.getDocument().getAttributes();
            typeTemplateTarget = getStringAttribute(documentAttributes, attributeName("type-template-target"), null);
            packageNameStripped = getBooleanAttribute(documentAttributes, attributeName("package-name-stripped"));
            initialized = true;
            LOGGER.fine("Initialized.");
        }
    }

    private TypeLookup createTypeLookup(final StructuralNode node) {
        final Set<PluginSet> pluginSets = loadDescriptors(node);
        final Map<String, Object> documentAttributes = node.getDocument().getAttributes();
        final Pattern includePattern =
                getPatternAttribute(documentAttributes, attributeName("type-filter-include-pattern"), ".*");
        final Pattern excludePattern =
                getPatternAttribute(documentAttributes, attributeName("type-filter-exclude-pattern"), IMPOSSIBLE_REGEX);
        LOGGER.log(
                Level.FINE,
                "Creating type lookup using `%s` and `%s` patterns for inclusion and exclusion, respectively...",
                new Object[] {includePattern, excludePattern});
        final Predicate<String> classNameFilter =
                className -> includePattern.matcher(className).matches()
                        && !excludePattern.matcher(className).matches();
        return TypeLookup.of(pluginSets, classNameFilter);
    }

    private Set<PluginSet> loadDescriptors(final StructuralNode node) {
        final Map<String, Object> documentAttributes = node.getDocument().getAttributes();
        final String directory = getStringAttribute(documentAttributes, attributeName("descriptor-directory"), null);
        final String pathMatcher =
                getStringAttribute(documentAttributes, attributeName("descriptor-path-matcher"), "glob:**/*.xml");
        final String dotFilesExcludedString =
                getStringAttribute(documentAttributes, attributeName("descriptor-dot-files-excluded"), "true");
        final boolean dotFilesExcluded = Boolean.parseBoolean(dotFilesExcludedString);
        LOGGER.log(
                Level.FINE,
                "Loading descriptors matching `{}` pattern in `{}`... (Dot files will be {})",
                new Object[] {pathMatcher, directory, dotFilesExcluded ? "ignored." : "included!"});
        final Set<PluginSet> pluginSets = loadDescriptors(directory, pathMatcher, dotFilesExcluded);
        LOGGER.log(Level.FINE, "Loaded {} descriptors in total.", pluginSets.size());
        return pluginSets;
    }

    private static String attributeName(final String key) {
        final String attributeName = "log4j-docgen-" + key;
        if (key.matches(".*[^a-z-]+.*")) {
            final String message = String.format(
                    "Found invalid attribute name: `%s`.%n"
                            + "`node.getDocument().getAttributes()` lower cases all attribute names and replaces symbols with dashes.%n"
                            + "Hence, you should use kebab-case attribute names.",
                    attributeName);
            throw new IllegalArgumentException(message);
        }
        return attributeName;
    }

    private static Pattern getPatternAttribute(
            final Map<String, Object> documentAttributes, final String key, final String defaultValue) {
        final String regex = getStringAttribute(documentAttributes, key, defaultValue);
        try {
            return Pattern.compile(regex);
        } catch (final Exception error) {
            final String message =
                    String.format("failed compiling the regex pattern `%s` provided in attribute `%s`", regex, key);
            throw new IllegalArgumentException(message, error);
        }
    }

    private static boolean getBooleanAttribute(final Map<String, Object> documentAttributes, final String key) {
        // Boolean document attributes get transformed from `pom.xml` in an unexpected way:
        // 1. `<foo>true</foo>` gets translated to an empty string in the `documentAttributes`
        // 2. `<foo>false</foo>` doesn't even get into the `documentAttributes`
        // Hence, we only check for the existence of the key
        return documentAttributes.containsKey(key);
    }

    private static String getStringAttribute(
            final Map<String, Object> documentAttributes, final String key, @Nullable final String defaultValue) {
        final Object value = documentAttributes.get(key);
        final String textValue;
        if (!(value instanceof String) || (textValue = ((String) value).trim()).isEmpty()) {
            if (defaultValue == null) {
                final String message = String.format("blank or missing attribute: `%s`", key);
                throw new IllegalArgumentException(message);
            } else {
                return defaultValue;
            }
        }
        return textValue;
    }

    private Set<PluginSet> loadDescriptors(
            final String directory, final String pathPattern, final boolean dotFilesExcluded) {
        final Set<PluginSet> pluginSets = new LinkedHashSet<>();
        final PluginBundleStaxReader pluginSetReader = new PluginBundleStaxReader();
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pathPattern);
        try (final Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.forEach(path -> {

                // Skip directories
                if (Files.isDirectory(path)) {
                    return;
                }

                // Skip dot files
                final boolean dotFile =
                        dotFilesExcluded && path.getFileName().toString().startsWith(".");
                if (dotFile) {
                    return;
                }

                // Skip mismatching paths
                final boolean matched = pathMatcher.matches(path);
                if (!matched) {
                    return;
                }

                // Read the descriptor
                final PluginSet pluginSet;
                try {
                    pluginSet = pluginSetReader.read(path.toString());
                } catch (final Exception error) {
                    final String message = String.format("failed reading descriptor: `%s`", path);
                    throw new RuntimeException(message, error);
                }
                pluginSets.add(pluginSet);
                LOGGER.log(Level.FINE, "Loaded descriptor at `{}`.", new Object[] {path});
            });
        } catch (final IOException error) {
            final String message = String.format("failed reading descriptors from directory: `%s`", directory);
            throw new UncheckedIOException(message, error);
        }
        return pluginSets;
    }
}
