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
package org.apache.logging.log4j.changelog;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.logging.log4j.tools.internal.test.util.FileTestUtils.assertDirectoryContentMatches;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import org.apache.logging.log4j.changelog.releaser.ChangelogReleaser;
import org.apache.logging.log4j.changelog.releaser.ChangelogReleaserArgs;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class ChangelogReleaserTest {

    @Test
    @Order(1)
    void _1st_time_release_output_should_match(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path changelogDirectory)
            throws Exception {
        verifyReleaseOutput(changelogDirectory, "src/test/resources/3-enriched", "src/test/resources/5-released");
    }

    @Test
    @Order(2)
    void _2nd_time_release_output_should_match(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path changelogDirectory)
            throws Exception {
        verifyReleaseOutput(
                changelogDirectory, "src/test/resources/6-enriched-again", "src/test/resources/7-released-again");
    }

    @Test
    @Order(3)
    void _3rd_time_release_without_unreleased_directories_should_match(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path changelogDirectory) throws Exception {
        // We verify the `8-no-unreleased-directories` directory against itself.
        // This is valid, since we indeed expect no changes due to missing `.2.x.x`, etc. (i.e., unreleased) folders.
        verifyReleaseOutput(
                changelogDirectory,
                "src/test/resources/8-no-unreleased-directories",
                "src/test/resources/8-no-unreleased-directories");
    }

    private static void verifyReleaseOutput(
            final Path changelogDirectory,
            final String referenceChangelogDirectory,
            final String expectedOutputDirectory)
            throws Exception {

        // Clone the directory to avoid `copy`/`move` operations cluttering the reference folder
        copyDirectory(new File(referenceChangelogDirectory), changelogDirectory.toFile());

        // Invoke the releaser
        final ChangelogReleaserArgs args =
                new ChangelogReleaserArgs(changelogDirectory, "2.19.0", null, LocalDate.parse("2023-01-25"));
        ChangelogReleaser.performRelease(args);

        // Compare the output
        assertDirectoryContentMatches(changelogDirectory, Paths.get(expectedOutputDirectory));
    }
}
