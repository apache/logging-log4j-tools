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

import java.io.File;
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
        final List<Integer> changelogEntryCounts = releaseDirectories
                .stream()
                .map(ignored -> 0)
                .collect(Collectors.toList());

        // Export releases
        if (releaseDirectoryCount > 0) {

            // Export each release directory
            for (int releaseIndex = 0; releaseIndex < releaseDirectories.size(); releaseIndex++) {
                final Path releaseDirectory = releaseDirectories.get(releaseIndex);
                final ChangelogRelease changelogRelease = changelogReleases.get(releaseIndex);
                final int changelogEntryCount;
                try {
                    changelogEntryCount = exportRelease(
                            args.outputDirectory,
                            args.changelogDirectory,
                            releaseDirectory,
                            changelogRelease,
                            args.changelogTemplates);
                } catch (final Exception error) {
                    final String message =
                            String.format("failed exporting release from directory `%s`", releaseDirectory);
                    throw new RuntimeException(message, error);
                }
                changelogEntryCounts.set(releaseIndex, changelogEntryCount);
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
                    System.out.format("exporting upcoming release directory: `%s`%n", upcomingReleaseDirectory);
                    final int changelogEntryCount = exportRelease(
                            args.outputDirectory,
                            args.changelogDirectory,
                            upcomingReleaseDirectory,
                            upcomingRelease,
                            args.changelogTemplates);
                    changelogReleases.add(upcomingRelease);
                    changelogEntryCounts.add(changelogEntryCount);
                });

        // Export the release index
        exportIndex(
                args.outputDirectory,
                args.changelogDirectory,
                args.indexTemplates,
                changelogReleases,
                changelogEntryCounts);

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

    private static int exportRelease(
            final Path outputDirectory,
            final Path changelogDirectory,
            final Path releaseDirectory,
            final ChangelogRelease changelogRelease,
            final Set<ChangelogExporterTemplate> changelogTemplates) {
        final Map<ChangelogEntry.Type, List<ChangelogEntry>> changelogEntriesByType =
                readChangelogEntriesByType(releaseDirectory);
        try {
            exportRelease(
                    outputDirectory,
                    changelogDirectory,
                    releaseDirectory,
                    changelogRelease,
                    changelogEntriesByType,
                    changelogTemplates);
        } catch (final Exception error) {
            final String message = String.format("failed exporting release from directory `%s`", releaseDirectory);
            throw new RuntimeException(message, error);
        }
        return changelogEntriesByType
                .values()
                .stream()
                .mapToInt(Collection::size)
                .sum();
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
            final Path changelogDirectory,
            final Path releaseDirectory,
            final ChangelogRelease release,
            final Map<ChangelogEntry.Type, List<ChangelogEntry>> entriesByType,
            final Set<ChangelogExporterTemplate> changelogTemplates) {
        final Map<String, Object> changelogTemplateData = new LinkedHashMap<>();
        changelogTemplateData.put("release", release);
        changelogTemplateData.put("entriesByType", entriesByType);
        for (final ChangelogExporterTemplate changelogTemplate : changelogTemplates) {
            final Path changelogTemplateSourceFile = releaseDirectory.resolve(changelogTemplate.sourceFileName);
            if (Files.exists(changelogTemplateSourceFile)) {
                final String changelogTemplateName = templateName(changelogDirectory, changelogTemplateSourceFile);
                final String changelogTemplateTargetFileName =
                        changelogTemplate.targetFileName.replaceAll("%v", release.version);
                final Path changelogTemplateTargetFile = outputDirectory.resolve(changelogTemplateTargetFileName);
                FreeMarkerUtils.render(
                        changelogDirectory,
                        changelogTemplateName,
                        changelogTemplateData,
                        changelogTemplateTargetFile);
            } else if (changelogTemplate.failIfNotFound) {
                final String message = String.format("could not find template file: `%s`", changelogTemplateSourceFile);
                throw new IllegalStateException(message);
            }
        }
    }

    private static ChangelogRelease upcomingRelease(final int versionMajor) {
        final String releaseVersion = versionMajor + ".x.x";
        return new ChangelogRelease(releaseVersion, null);
    }

    private static void exportIndex(
            final Path outputDirectory,
            final Path changelogDirectory,
            final Set<ChangelogExporterTemplate> indexTemplates,
            final List<ChangelogRelease> changelogReleases,
            final List<Integer> changelogEntryCounts) {
        final Object indexTemplateData = Collections.singletonMap(
                "releases", IntStream
                        .range(0, changelogReleases.size())
                        .boxed()
                        .sorted(Comparator.reverseOrder())
                        .map(releaseIndex -> {
                            final ChangelogRelease changelogRelease = changelogReleases.get(releaseIndex);
                            final int changelogEntryCount = changelogEntryCounts.get(releaseIndex);
                            Map<String, Object> changelogReleaseData = new LinkedHashMap<>();
                            changelogReleaseData.put("version", changelogRelease.version);
                            changelogReleaseData.put("date", changelogRelease.date);
                            changelogReleaseData.put("changelogEntryCount", changelogEntryCount);
                            return (Object) changelogReleaseData;
                        })
                        .collect(Collectors.toList()));
        indexTemplates.forEach(indexTemplate -> {
            final Path indexTemplateSourceFile = changelogDirectory.resolve(indexTemplate.sourceFileName);
            if (Files.exists(indexTemplateSourceFile)) {
                final String indexTemplateSourceName = templateName(changelogDirectory, indexTemplateSourceFile);
                final Path indexTemplateTargetFile = outputDirectory.resolve(indexTemplate.targetFileName);
                FreeMarkerUtils.render(
                        changelogDirectory,
                        indexTemplateSourceName,
                        indexTemplateData,
                        indexTemplateTargetFile);
            } else if (indexTemplate.failIfNotFound) {
                final String message = String.format("could not find template file: `%s`", indexTemplateSourceFile);
                throw new IllegalStateException(message);
            }
        });
    }

    /**
     * Creates a FreeMarker template name from the given path, assuming that the provided changelog directory is the template folder.
     * <p>
     * {@link freemarker.cache.FileTemplateLoader} works against a template folder, hence the path relativization required.
     * </p>
     */
    private static String templateName(final Path changelogDirectory, final Path path) {
        final Path relativePath = changelogDirectory.relativize(path);
        return File.pathSeparatorChar == '/'
                ? relativePath.toString()
                : relativePath.toString().replace('\\', '/');
    }

}
