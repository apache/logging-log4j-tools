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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.apache.logging.log4j.changelog.releaser.ChangelogReleaser;
import org.apache.logging.log4j.changelog.releaser.ChangelogReleaserArgs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.logging.log4j.changelog.FileTestUtils.assertDirectoryContentMatches;

import static org.apache.commons.io.FileUtils.copyDirectory;

class ChangelogReleaserTest {

    @Test
    void output_should_match(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path changelogDirectory)
            throws Exception {

        // Clone the directory to avoid `move` operations cluttering the reference folder
        copyDirectory(new File("src/test/resources/3-enriched"), changelogDirectory.toFile());

        // Invoke the releaser
        final ChangelogReleaserArgs args = new ChangelogReleaserArgs(
                changelogDirectory,
                "2.19.0",
                null,
                LocalDate.parse("2023-01-25"));
        ChangelogReleaser.performRelease(args);

        // Empty folders are not tracked by git, though created by `ChangelogReleaser`.
        // Create the `.2.x.x` folder to match the actual output.
        final Path expectedChangelogDirectory = Paths.get("src/test/resources/5-released");
        final Path emptyFolder = expectedChangelogDirectory.resolve(".2.x.x");
        if (!Files.exists(emptyFolder)) {
            Files.createDirectories(emptyFolder);
        }

        // Compare the output
        assertDirectoryContentMatches(changelogDirectory, expectedChangelogDirectory);

    }

}
