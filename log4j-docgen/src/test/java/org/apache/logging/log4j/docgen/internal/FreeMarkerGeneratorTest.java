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
package org.apache.logging.log4j.docgen.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.freemarker.FreeMarkerGenerator;
import org.apache.logging.log4j.docgen.freemarker.FreeMarkerGeneratorRequest;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.junit.jupiter.api.Test;

public class FreeMarkerGeneratorTest {

    @Test
    void generatePluginDocumentation() throws Exception {
        final PluginBundleStaxReader reader = new PluginBundleStaxReader();
        final PluginSet set = reader.read("src/test/resources/META-INF/log4j/plugins-sample.xml");
        final Path templateDir = Paths.get("src/test/resources/templates");
        final Path expectedDirectory = Paths.get("src/test/resources/expected/freemarker");
        final Path outputDirectory = Paths.get("target/test-site/freemarker");

        final FreeMarkerGenerator generator = new DefaultFreeMarkerGenerator();
        final FreeMarkerGeneratorRequest request = new FreeMarkerGeneratorRequest();
        request.addPluginSet(set);
        request.setTemplateDirectory(templateDir);
        request.setOutputDirectory(outputDirectory);

        generator.generateDocumentation(request);

        try (final Stream<Path> stream = Files.walk(expectedDirectory)) {
            stream.forEach(expectedPath -> {
                if (Files.isRegularFile(expectedPath)) {
                    final Path path = expectedDirectory.relativize(expectedPath);
                    final Path actualPath = outputDirectory.resolve(path);
                    assertThat(actualPath)
                            .exists()
                            .usingCharset(StandardCharsets.UTF_8)
                            .hasSameTextualContentAs(expectedPath);
                }
            });
        }
    }
}
