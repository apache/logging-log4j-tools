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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.junit.jupiter.api.Test;

public class AsciidocConverterTest {

    private static final Path LICENSE_PATH;

    static {
        try {
            LICENSE_PATH = Paths.get(AsciidocConverterTest.class
                    .getResource("/templates/license.ftl")
                    .toURI());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void convertToAsciidoc() throws Exception {
        final DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        final DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = tool.getStandardFileManager(null, Locale.ROOT, UTF_8);

        final Path basePath = Paths.get(System.getProperty("basedir", "."));
        final Path sourcePath = Paths.get(AsciidocConverterTest.class
                .getResource("/processor/asciidoc/example/JavadocExample.java")
                .toURI());
        final Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjects(sourcePath);

        final Path destPath = basePath.resolve("target/test-site");
        Files.createDirectories(destPath);
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Set.of(destPath));

        final DocumentationTask task = tool.getTask(null, fileManager, ds, TestDoclet.class, null, sources);
        task.call();

        final List<String> warnings = ds.getDiagnostics().stream()
                .filter(d -> d.getKind() != Diagnostic.Kind.NOTE)
                .map(d -> d.getMessage(Locale.ROOT))
                .collect(Collectors.toList());
        assertThat(warnings).isEmpty();
        final Path expectedPath = Paths.get(AsciidocConverterTest.class
                .getResource("/expected/processor/JavadocExample.adoc")
                .toURI());
        assertThat(destPath.resolve("processor/JavadocExample.adoc")).hasSameTextualContentAs(expectedPath, UTF_8);
    }

    public static class TestDoclet implements Doclet {

        @Override
        public void init(final Locale locale, final Reporter reporter) {}

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public Set<? extends Option> getSupportedOptions() {
            return Set.of();
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean run(final DocletEnvironment environment) {
            final AsciidocConverter converter = new AsciidocConverter(environment.getDocTrees());
            final JavaFileManager fileManager = environment.getJavaFileManager();
            try {
                for (final Element element : environment.getIncludedElements()) {
                    if ("JavadocExample".equals(element.getSimpleName().toString())) {
                        final FileObject output = fileManager.getFileForOutput(
                                StandardLocation.CLASS_OUTPUT, "processor", "JavadocExample.adoc", null);
                        final String asciiDoc = converter.toAsciiDoc(element);
                        try (final OutputStream os = output.openOutputStream()) {
                            Files.copy(LICENSE_PATH, os);
                            os.write(asciiDoc.getBytes(UTF_8));
                        }
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }
}
