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

import static org.apache.logging.log4j.docgen.generator.PluginSetUtils.readDescriptor;
import static org.apache.logging.log4j.docgen.generator.PluginSetUtils.readDescriptors;
import static org.apache.logging.log4j.tools.internal.test.util.FileTestUtils.assertDirectoryContentMatches;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.logging.log4j.docgen.PluginSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

public class DocumentationGeneratorTest {

    private static final Predicate<String> CLASS_NAME_MATCHER = className -> !className.startsWith("java.");

    @Test
    void simple_descriptor_should_work(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir) {
        final PluginSet pluginSet = readDescriptor("src/test/resources/example-plugins.xml");
        final Path referenceOutputDir = Paths.get("src/test/resources/plugins-to-freemarker-output");
        generateDocumentationAndVerifyOutput(outputDir, Set.of(pluginSet), referenceOutputDir);
    }

    @Test
    void multiple_descriptors_should_be_able_to_link_each_other(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir) {
        final Set<PluginSet> pluginSets = readDescriptors(
                "src/test/resources/plugins-with-dependency/descriptors/log4j-core-plugins.xml",
                "src/test/resources/plugins-with-dependency/descriptors/log4j-layout-template-json-plugins.xml");
        final Path referenceOutputDir = Paths.get("src/test/resources/plugins-with-dependency/freemarker-output");
        generateDocumentationAndVerifyOutput(outputDir, pluginSets, referenceOutputDir);
    }

    private static void generateDocumentationAndVerifyOutput(
            final Path outputDir, final Set<PluginSet> pluginSets, final Path referenceOutputDir) {

        // Generate the documentation
        final Path templateDirectory = Paths.get("src/test/resources/templates");
        final DocumentationGeneratorArgs generatorArgs = new DocumentationGeneratorArgs(
                pluginSets,
                CLASS_NAME_MATCHER,
                templateDirectory,
                new DocumentationTemplate(
                        "index.adoc.ftl", outputDir.resolve("index.adoc").toString()),
                new DocumentationTemplate(
                        "plugin.adoc.ftl", outputDir.resolve("%a/%c.adoc").toString()),
                new DocumentationTemplate(
                        "interface.adoc.ftl", outputDir.resolve("%a/%c.adoc").toString()));
        DocumentationGenerator.generateDocumentation(generatorArgs);

        // Verify the output
        assertDirectoryContentMatches(outputDir, referenceOutputDir);
    }
}
