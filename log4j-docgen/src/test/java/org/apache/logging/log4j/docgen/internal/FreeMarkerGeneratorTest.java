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

import static org.apache.logging.log4j.tools.internal.test.utils.FileTestUtils.assertDirectoryContentMatches;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.freemarker.FreeMarkerGenerator;
import org.apache.logging.log4j.docgen.freemarker.FreeMarkerGeneratorRequest;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

public class FreeMarkerGeneratorTest {

    @Test
    void generatePluginDocumentation(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir) throws Exception {

        // Read plugins
        final PluginBundleStaxReader reader = new PluginBundleStaxReader();
        final PluginSet pluginSet = reader.read("src/test/resources/example-plugins.xml");

        // Generate the documentation
        final FreeMarkerGenerator generator = new DefaultFreeMarkerGenerator();
        final FreeMarkerGeneratorRequest request = new FreeMarkerGeneratorRequest();
        request.addPluginSet(pluginSet);
        final Path templateDirectory = Paths.get("src/test/resources/templates");
        request.setTemplateDirectory(templateDirectory);
        request.setOutputDirectory(outputDir);
        generator.generateDocumentation(request);

        // Verify the output
        final Path expectedDir = Paths.get("src/test/resources/plugins-to-freemarker-output");
        assertDirectoryContentMatches(outputDir, expectedDir);
    }
}
