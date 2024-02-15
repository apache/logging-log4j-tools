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

import java.nio.file.Path;
import java.util.Set;
import org.apache.logging.log4j.docgen.PluginSet;
import org.jspecify.annotations.Nullable;

public final class DocumentationGeneratorArgs {

    final Set<PluginSet> pluginSets;

    final Path templateDirectory;

    final String scalarsTemplateName;

    final String pluginTemplateName;

    final String interfaceTemplateName;

    final Path outputDirectory;

    public DocumentationGeneratorArgs(
            final Set<PluginSet> pluginSets,
            final Path templateDirectory,
            final String scalarsTemplateName,
            final String pluginTemplateName,
            final String interfaceTemplateName,
            final Path outputDirectory) {
        this.pluginSets = requireNonNull(pluginSets, "pluginSets");
        this.templateDirectory = requireNonNull(templateDirectory, "templateDirectory");
        this.scalarsTemplateName = requireValidTemplateName(scalarsTemplateName, "scalarsTemplateName");
        this.pluginTemplateName = requireValidTemplateName(pluginTemplateName, "pluginTemplateName");
        this.interfaceTemplateName = requireValidTemplateName(interfaceTemplateName, "interfaceTemplateName");
        this.outputDirectory = requireNonNull(outputDirectory, "outputDirectory");
    }

    private static String requireValidTemplateName(@Nullable final String value, final String name) {
        final String extension = '.' + DocumentationFiles.templateFileNameExtension();
        if (value == null || !value.endsWith(extension)) {
            final String message =
                    String.format("was expecting `%s` to end with a `%s` suffix: `%s`", name, extension, value);
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
