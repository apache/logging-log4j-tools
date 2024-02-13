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
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

public class AsciidocConverterTest {

    private static final Path LICENSE_PATH = Paths.get("src/test/resources/templates/license.ftl");

    private static final String TEST_CLASS_NAME = "JavadocExample";

    private static final Charset CHARSET = UTF_8;

    @Test
    void convertToAsciidoc(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir) throws Exception {

        // Instantiate the tooling
        final DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        final StandardJavaFileManager fileManager = tool.getStandardFileManager(null, Locale.ROOT, CHARSET);

        // Populate sources (aka. compilation units)
        final Path sourcePath =
                Paths.get("src/test/resources/AsciiDocConverterTest-input/example/" + TEST_CLASS_NAME + ".java");
        final Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjects(sourcePath);

        // Set the target path used by `Javadoc2AsciiDocDoclet` to dump the generated files
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Set.of(outputDir));

        // Compile the sources
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final DocumentationTask task =
                tool.getTask(null, fileManager, diagnosticCollector, Javadoc2AsciiDocDoclet.class, null, sources);
        task.call();

        // Verify successful compilation
        List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticCollector.getDiagnostics();
        assertThat(diagnostics).noneMatch(diagnostic -> diagnostic.getKind() != Diagnostic.Kind.NOTE);

        // Verify the generated AsciiDoc
        final Path expectedPath =
                Paths.get("src/test/resources/AsciiDocConverterTest-reference-output/" + TEST_CLASS_NAME + ".adoc");
        assertThat(outputDir.resolve(TEST_CLASS_NAME + ".adoc")).hasSameTextualContentAs(expectedPath, CHARSET);
    }

    /**
     * A {@link Doclet} converting Javadoc to AsciiDoc only for the class called {@value #TEST_CLASS_NAME}.
     */
    public static final class Javadoc2AsciiDocDoclet implements Doclet {

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
                    if (ElementKind.CLASS.equals(element.getKind())
                            && TEST_CLASS_NAME.equals(element.getSimpleName().toString())) {
                        final FileObject output = fileManager.getFileForOutput(
                                StandardLocation.CLASS_OUTPUT, "", "JavadocExample.adoc", null);
                        final String asciiDoc = converter.toAsciiDoc(element);
                        assertThat(asciiDoc).isNotNull();
                        try (final OutputStream os = output.openOutputStream()) {
                            Files.copy(LICENSE_PATH, os);
                            os.write(asciiDoc.getBytes(CHARSET));
                        }
                    }
                }
            } catch (final IOException error) {
                throw new UncheckedIOException(error);
            }
            return true;
        }
    }
}
