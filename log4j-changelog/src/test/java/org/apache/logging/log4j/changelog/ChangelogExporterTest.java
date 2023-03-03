/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.changelog;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.changelog.exporter.ChangelogExporter;
import org.apache.logging.log4j.changelog.exporter.ChangelogExporterArgs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.logging.log4j.changelog.FileTestUtils.assertDirectoryContentMatches;

class ChangelogExporterTest {

    @Test
    void multi_mode_output_should_match(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDirectory) {
        ChangelogExporterArgs args = ChangelogExporterArgs.of(
                Paths.get("src/test/resources/3-enriched"),
                outputDirectory);
        ChangelogExporter.performExport(args);
        assertDirectoryContentMatches(outputDirectory, Paths.get("src/test/resources/4-exported-multi"));
    }

    @Test
    void single_mode_with_releaseVersion_output_should_match(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDirectory) {
        final Path outputFile = outputDirectory.resolve(".changelog-email.txt");
        ChangelogExporterArgs args = ChangelogExporterArgs.ofSingle(
                Paths.get("src/test/resources/3-enriched"),
                "2.18.0",
                Paths.get("src/test/resources/3-enriched/.changelog-email.txt.ftl"),
                outputFile);
        ChangelogExporter.performExport(args);
        assertDirectoryContentMatches(outputDirectory, Paths.get("src/test/resources/4-exported-single"));
    }

}
