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
package org.apache.logging.log4j.tools.internal.freemarker.util;

import static java.nio.file.Files.write;
import static org.apache.logging.log4j.tools.internal.freemarker.util.FreeMarkerUtils.render;
import static org.apache.logging.log4j.tools.internal.freemarker.util.FreeMarkerUtils.renderString;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class FreeMarkerUtilsTest {

    @Test
    void render_should_work(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path tempDir) throws Exception {

        // Create the template file
        final String templateName = "test.txt.ftl";
        final Path templateFile = tempDir.resolve(templateName);
        write(
                templateFile,
                "Hello, ${name?capitalize}!".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE_NEW);

        // Render the template
        final Path outputFile = tempDir.resolve("test.txt");
        render(tempDir, templateName, Collections.singletonMap("name", "volkan"), outputFile);

        // Verify the generated content
        assertThat(outputFile).hasContent("Hello, Volkan!");
    }

    @Test
    void render_should_stop(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path tempDir) throws Exception {

        // Create the template file
        final String templateName = "test.txt.ftl";
        final Path templateFile = tempDir.resolve(templateName);
        final String excessiveContentToEnsureSuccessIsNotDueToBuffering = String.format("%010000d\n", 1);
        final String templateFileContent = excessiveContentToEnsureSuccessIsNotDueToBuffering
                + "You might expect awesome things to get rendered.\nBut I tell you: they won't.\n${stop()}";
        write(templateFile, templateFileContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

        // Render the template
        final Path outputFile = tempDir.resolve("test.txt");
        render(tempDir, templateName, Collections.singletonMap("name", "volkan"), outputFile);

        // Verify the generated content
        assertThat(outputFile).doesNotExist();
    }

    @Test
    void renderString_should_work() {
        final String output = renderString("Hello, ${name?capitalize}!", Collections.singletonMap("name", "volkan"));
        assertThat(output).isEqualTo("Hello, Volkan!");
    }
}
