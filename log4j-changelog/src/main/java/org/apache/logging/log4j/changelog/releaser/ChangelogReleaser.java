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
package org.apache.logging.log4j.changelog.releaser;

import static java.time.format.DateTimeFormatter.ISO_DATE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.changelog.ChangelogFiles;
import org.apache.logging.log4j.changelog.ChangelogRelease;
import org.apache.logging.log4j.changelog.util.FileUtils;

public final class ChangelogReleaser {

    private ChangelogReleaser() {}

    public static void performRelease(final ChangelogReleaserArgs args) {

        // Read the release date and version
        final String releaseDate =
                ISO_DATE.format(args.releaseDate != null ? args.releaseDate : LocalDate.now(ZoneId.systemDefault()));
        System.out.format("using `%s` for the release date%n", releaseDate);

        try {

            // Determine released and unreleased directories
            final Path unreleasedDirectory =
                    ChangelogFiles.unreleasedDirectory(args.changelogDirectory, args.releaseVersionMajor);
            final Path releaseDirectory = ChangelogFiles.releaseDirectory(args.changelogDirectory, args.releaseVersion);

            // Short-circuit if there is nothing to be released
            if (!Files.exists(unreleasedDirectory)) {
                return;
            }

            // Populate the release changelog files
            populateReleaseChangelogEntryFiles(unreleasedDirectory, releaseDirectory);
            populateReleaseXmlFiles(releaseDate, args.releaseVersion, releaseDirectory);
            populateReleaseChangelogTemplateFiles(unreleasedDirectory, releaseDirectory);

        } catch (final IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static void populateReleaseChangelogEntryFiles(final Path unreleasedDirectory, final Path releaseDirectory)
            throws IOException {
        if (!Files.exists(releaseDirectory)) {
            Files.createDirectories(releaseDirectory);
        }
        FileUtils.findAdjacentFiles(unreleasedDirectory, true, paths -> {
            paths.forEach(unreleasedChangelogEntryFile -> {
                final String fileName =
                        unreleasedChangelogEntryFile.getFileName().toString();
                final Path releasedChangelogEntryFile = releaseDirectory.resolve(fileName);
                System.out.format(
                        "moving changelog entry file `%s` to `%s`%n",
                        unreleasedChangelogEntryFile, releasedChangelogEntryFile);
                try {
                    Files.move(unreleasedChangelogEntryFile, releasedChangelogEntryFile);
                } catch (final IOException error) {
                    throw new UncheckedIOException(error);
                }
            });
            return 1;
        });
    }

    private static void populateReleaseXmlFiles(
            final String releaseDate, final String releaseVersion, final Path releaseDirectory) throws IOException {
        final Path releaseXmlFile = ChangelogFiles.releaseXmlFile(releaseDirectory);
        System.out.format("writing release information to `%s`%n", releaseXmlFile);
        final ChangelogRelease changelogRelease = new ChangelogRelease(releaseVersion, releaseDate);
        Files.deleteIfExists(releaseXmlFile);
        changelogRelease.writeToXmlFile(releaseXmlFile);
    }

    private static void populateReleaseChangelogTemplateFiles(
            final Path unreleasedDirectory, final Path releaseDirectory) throws IOException {
        Set<String> releaseChangelogTemplateFileNames = releaseChangelogTemplateFileNames(unreleasedDirectory);
        for (final String releaseChangelogTemplateFileName : releaseChangelogTemplateFileNames) {
            final Path targetFile = releaseDirectory.resolve(releaseChangelogTemplateFileName);
            if (Files.exists(targetFile)) {
                System.out.format("keeping the existing changelog template file: `%s`%n", targetFile);
            } else {
                final Path sourceFile = unreleasedDirectory.resolve(releaseChangelogTemplateFileName);
                System.out.format("copying the changelog template file `%s` to `%s`%n", sourceFile, targetFile);
                Files.copy(sourceFile, targetFile);
            }
        }
    }

    private static Set<String> releaseChangelogTemplateFileNames(final Path releaseDirectory) {
        final String templateFileNameSuffix = '.' + ChangelogFiles.templateFileNameExtension();
        return FileUtils.findAdjacentFiles(releaseDirectory, false, paths -> paths.map(
                        path -> path.getFileName().toString())
                .filter(path -> path.endsWith(templateFileNameSuffix))
                .collect(Collectors.toSet()));
    }
}
