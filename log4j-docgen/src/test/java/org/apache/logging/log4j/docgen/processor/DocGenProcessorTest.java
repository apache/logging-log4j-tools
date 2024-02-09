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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.logging.log4j.docgen.xsd.SchemaGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.assertj3.XmlAssert;

public class DocGenProcessorTest {

    static Stream<String> descriptorGenerationSucceeds() {
        return Stream.of("v2", "v3");
    }

    @ParameterizedTest
    @MethodSource
    void descriptorGenerationSucceeds(final String version) {
        final Path basePath = Paths.get(System.getProperty("basedir", "."));
        final Path schema = basePath.resolve("target/generated-site/resources/xsd/plugins-0.1.0.xsd");
        final URL expected = SchemaGenerator.class.getResource("/expected/processor/META-INF/log4j/plugins.xml");
        final Path actual = assertDoesNotThrow(() -> generateDescriptor(version));
        XmlAssert.assertThat(actual)
                .isValidAgainst(schema)
                .and(expected)
                .ignoreComments()
                .ignoreWhitespace()
                .areIdentical();
    }

    private static Path generateDescriptor(final String version) throws Exception {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);

        final Path basePath = Paths.get(System.getProperty("basedir", "."));
        final Path sourcePath = Paths.get(
                DocGenProcessorTest.class.getResource("/processor/" + version).toURI());
        final Iterable<? extends JavaFileObject> sources;
        try (final Stream<Path> files = Files.walk(sourcePath)) {
            sources = fileManager.getJavaFileObjects(
                    files.filter(Files::isRegularFile).toArray(Path[]::new));
        }

        final Path destPath = basePath.resolve("target/test-site/processor/" + version);
        Files.createDirectories(destPath);
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Set.of(destPath));

        final CompilationTask task = compiler.getTask(
                null,
                fileManager,
                ds,
                Arrays.asList("-proc:only", "-processor", DocGenProcessor.class.getName()),
                null,
                sources);
        task.call();

        final List<String> warnings = ds.getDiagnostics().stream()
                .filter(d -> d.getKind() != Diagnostic.Kind.NOTE)
                .map(d -> d.getMessage(Locale.ROOT))
                .collect(Collectors.toList());
        assertThat(warnings).isEmpty();

        final Path descriptor = destPath.resolve("META-INF/log4j/plugins.xml");
        assertThat(descriptor).isNotEmptyFile();
        return descriptor;
    }
}
