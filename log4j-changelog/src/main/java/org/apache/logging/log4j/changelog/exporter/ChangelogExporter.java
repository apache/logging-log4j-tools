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
package org.apache.logging.log4j.changelog.exporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.changelog.ChangelogEntry;
import org.apache.logging.log4j.changelog.ChangelogFiles;
import org.apache.logging.log4j.changelog.ChangelogRelease;
import org.apache.logging.log4j.changelog.util.FileUtils;

public final class ChangelogExporter {

    private ChangelogExporter() {}

    public static void main(final String[] mainArgs) {
        final ChangelogExporterArgs args = ChangelogExporterArgs.fromSystemProperties();
        performExport(args);
    }

    public static void performExport(final ChangelogExporterArgs args) {

        // Find release directories
        final List<Path> releaseDirectories = findReleaseDirectories(args);
        final int releaseDirectoryCount = releaseDirectories.size();

        // Read the release information files
        final List<ChangelogRelease> changelogReleases = releaseDirectories
                .stream()
                .map(releaseDirectory -> {
                    final Path releaseXmlFile = ChangelogFiles.releaseXmlFile(releaseDirectory);
                    return ChangelogRelease.readFromXmlFile(releaseXmlFile);
                })
                .collect(Collectors.toList());

        // Export releases
        if (releaseDirectoryCount > 0) {

            // Export each release directory
            for (int releaseIndex = 0; releaseIndex < releaseDirectories.size(); releaseIndex++) {
                final Path releaseDirectory = releaseDirectories.get(releaseIndex);
                final ChangelogRelease changelogRelease = changelogReleases.get(releaseIndex);
                final Path releaseChangelogTemplateFile = ChangelogFiles.releaseChangelogTemplateFile(releaseDirectory);
                try {
                    exportRelease(
                            args.outputDirectory,
                            releaseDirectory,
                            changelogRelease,
                            releaseChangelogTemplateFile);
                } catch (final Exception error) {
                    final String message =
                            String.format("failed exporting release from directory `%s`", releaseDirectory);
                    throw new RuntimeException(message, error);
                }
            }

            // Report the operation
            if (releaseDirectoryCount == 1) {
                System.out.format("exported a single release directory: `%s`%n", releaseDirectories.get(0));
            } else {
                System.out.format(
                        "exported %d release directories: ..., `%s`%n",
                        releaseDirectories.size(),
                        releaseDirectories.get(releaseDirectoryCount - 1));
            }

        }

        // Export unreleased
        ChangelogFiles
                .unreleasedDirectoryVersionMajors(args.changelogDirectory)
                .stream()
                .sorted()
                .forEach(upcomingReleaseVersionMajor -> {
                    final Path upcomingReleaseDirectory =
                            ChangelogFiles.unreleasedDirectory(args.changelogDirectory, upcomingReleaseVersionMajor);
                    final ChangelogRelease upcomingRelease = upcomingRelease(upcomingReleaseVersionMajor);
                    final Path upcomingReleaseChangelogTemplateFile =
                            ChangelogFiles.releaseChangelogTemplateFile(upcomingReleaseDirectory);
                    System.out.format("exporting upcoming release directory: `%s`%n", upcomingReleaseDirectory);
                    exportRelease(
                            args.outputDirectory,
                            upcomingReleaseDirectory,
                            upcomingRelease,
                            upcomingReleaseChangelogTemplateFile);
                    changelogReleases.add(upcomingRelease);
                });

        // Export the release index
        final Path changelogIndexTemplateFile = ChangelogFiles.indexTemplateFile(args.changelogDirectory);
        exportIndex(args.outputDirectory, changelogReleases, changelogIndexTemplateFile);

    }

    private static List<Path> findReleaseDirectories(ChangelogExporterArgs args) {
        return FileUtils.findAdjacentFiles(
                args.changelogDirectory, true,
                paths -> paths
                        .filter(ChangelogExporter::isNonEmptyDirectory)
                        .sorted(Comparator.comparing(releaseDirectory -> {
                            final Path releaseXmlFile = ChangelogFiles.releaseXmlFile(releaseDirectory);
                            final ChangelogRelease changelogRelease =
                                    ChangelogRelease.readFromXmlFile(releaseXmlFile);
                            return changelogRelease.date;
                        }))
                        .collect(Collectors.toList()));
    }

    private static boolean isNonEmptyDirectory(final Path path) {
        return Files.isDirectory(path) &&
                FileUtils.findAdjacentFiles(path, false, paths -> paths.findFirst().isPresent());
    }

    private static void exportRelease(
            final Path outputDirectory,
            final Path releaseDirectory,
            final ChangelogRelease changelogRelease,
            final Path releaseChangelogTemplateFile) {
        final Map<ChangelogEntry.Type, List<ChangelogEntry>> changelogEntriesByType = readChangelogEntriesByType(releaseDirectory);
        try {
            exportRelease(outputDirectory, changelogRelease, changelogEntriesByType, releaseChangelogTemplateFile);
        } catch (final IOException error) {
            final String message = String.format("failed exporting release from directory `%s`", releaseDirectory);
            throw new UncheckedIOException(message, error);
        }
    }

    private static Map<ChangelogEntry.Type, List<ChangelogEntry>> readChangelogEntriesByType(
            final Path releaseDirectory) {
        return FileUtils.findAdjacentFiles(releaseDirectory, true, stream -> stream
                // Sorting is needed to generate the same output between different runs
                .sorted()
                .map(ChangelogEntry::readFromXmlFile)
                .collect(Collectors.groupingBy(
                        changelogEntry -> changelogEntry.type,
                        // A sorted map is needed to generate the same output between different runs
                        TreeMap::new,
                        Collectors.toList())));
    }

    private static void exportRelease(
            final Path outputDirectory,
            final ChangelogRelease release,
            final Map<ChangelogEntry.Type, List<ChangelogEntry>> entriesByType,
            final Path releaseChangelogTemplateFile)
            throws IOException {
        final String releaseChangelogFileName = releaseChangelogFileName(release);
        final Path releaseChangelogFile = outputDirectory.resolve(releaseChangelogFileName);
        final Map<String, Object> releaseChangelogTemplateData = new LinkedHashMap<>();
        releaseChangelogTemplateData.put("release", release);
        releaseChangelogTemplateData.put("entriesByType", entriesByType);
        FreeMarkerUtils.render(releaseChangelogTemplateFile, releaseChangelogTemplateData, releaseChangelogFile);
    }

    private static ChangelogRelease upcomingRelease(final int versionMajor) {
        final String releaseVersion = versionMajor + ".x.x";
        return new ChangelogRelease(releaseVersion, null);
    }

    private static void exportIndex(
            final Path outputDirectory,
            final List<ChangelogRelease> changelogReleases,
            final Path indexTemplateFile) {
        final Object indexTemplateData = Collections.singletonMap(
                "releases", IntStream
                        .range(0, changelogReleases.size())
                        .boxed()
                        .sorted(Comparator.reverseOrder())
                        .map(releaseIndex -> {
                            final ChangelogRelease changelogRelease = changelogReleases.get(releaseIndex);
                            Map<String, Object> changelogReleaseData = new LinkedHashMap<>();
                            changelogReleaseData.put("version", changelogRelease.version);
                            changelogReleaseData.put("date", changelogRelease.date);
                            changelogReleaseData.put("changelogFileName", releaseChangelogFileName(changelogRelease));
                            return (Object) changelogReleaseData;
                        })
                        .collect(Collectors.toList()));
        final Path indexFile = outputDirectory.resolve("index.adoc");
        FreeMarkerUtils.render(indexTemplateFile, indexTemplateData, indexFile);
    }

    private static String releaseChangelogFileName(final ChangelogRelease changelogRelease) {
        // Using only the version (that is, avoiding the date) in the filename so that one can determine the link to the changelog of a particular release with only version information
        return String.format("%s.adoc", changelogRelease.version);
    }

}
