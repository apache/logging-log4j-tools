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
package org.apache.logging.log4j.docgen.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.assertj3.XmlAssert;

class DescriptorGeneratorTest {

    private static final String TEST_CLASS_RESOURCE_PATH =
            "src/test/resources/" + DescriptorGeneratorTest.class.getSimpleName();

    @Test
    void log4j2_plugins_should_be_extracted(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir)
            throws Exception {
        test(outputDir, Paths.get(TEST_CLASS_RESOURCE_PATH + "/java-of-log4j2"));
    }

    @Test
    void log4j3_plugins_should_be_extracted(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir)
            throws Exception {
        test(outputDir, Paths.get(TEST_CLASS_RESOURCE_PATH + "/java-of-log4j3"));
    }

    @Test
    void failure_source_should_be_reported(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir) {
        assertThatThrownBy(() -> test(outputDir, Paths.get(TEST_CLASS_RESOURCE_PATH + "/java-with-invalid-javadoc")))
                .hasMessageContaining("failed to process element `example.JavadocExample`")
                .hasRootCauseMessage("A <li> tag must be a child of a <ol> or <ul> tag.");
    }

    private static void test(final Path outputDir, final Path sourceDir) throws Exception {
        final Path modelloGeneratedSchema = Paths.get("target/generated-site/resources/xsd/plugins-0.1.0.xsd");
        final Path expectedDescriptor = Paths.get(TEST_CLASS_RESOURCE_PATH + "/expected-plugins.xml");
        final Path actualDescriptor = generateDescriptor(outputDir, sourceDir);
        XmlAssert.assertThat(actualDescriptor)
                .isValidAgainst(modelloGeneratedSchema)
                .and(expectedDescriptor)
                .ignoreComments()
                .ignoreWhitespace()
                .areIdentical();
    }

    private static Path generateDescriptor(final Path outputDir, final Path sourceDir) throws Exception {

        // Instantiate the tooling
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.ROOT, UTF_8);

        // Populate sources
        final Iterable<? extends JavaFileObject> sources;
        try (final Stream<Path> files = Files.walk(sourceDir)) {
            Path[] sourceFiles = files.filter(Files::isRegularFile).toArray(Path[]::new);
            sources = fileManager.getJavaFileObjects(sourceFiles);
        }

        // Set the target path used by `DescriptorGenerator` to dump the generated files
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Set.of(outputDir));

        // Compile the sources
        final Path descriptorFilePath = outputDir.resolve("plugins.xml");
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnosticCollector,
                List.of(
                        "-proc:only",
                        "-processor",
                        DescriptorGenerator.class.getName(),
                        "-A" + DescriptorGenerator.DESCRIPTOR_FILE_PATH_OPTION_KEY + "=" + descriptorFilePath,
                        "-A" + DescriptorGenerator.GROUP_ID_OPTION_KEY + "=com.example.groupId",
                        "-A" + DescriptorGenerator.ARTIFACT_ID_OPTION_KEY + "=example-artifactId",
                        "-A" + DescriptorGenerator.VERSION_OPTION_KEY + "=1.2.3",
                        "-A" + DescriptorGenerator.DESCRIPTION_OPTION_KEY + "=example description",
                        "-A" + DescriptorGenerator.TYPE_FILTER_EXCLUDE_PATTERN_OPTION_KEY + "=^java\\..+"),
                null,
                sources);
        task.call();

        // Verify successful compilation
        List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticCollector.getDiagnostics();
        assertThat(diagnostics).noneMatch(diagnostic -> diagnostic.getKind() != Diagnostic.Kind.NOTE);

        // Resolve the descriptor file
        assertThat(descriptorFilePath).isNotEmptyFile();
        return descriptorFilePath;
    }
}
