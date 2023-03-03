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
package org.apache.logging.log4j.changelog.importer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.changelog.ChangelogEntry;
import org.apache.logging.log4j.changelog.ChangelogFiles;
import org.apache.logging.log4j.changelog.ChangelogRelease;

import static org.apache.logging.log4j.changelog.util.StringUtils.isBlank;

public final class MavenChangesImporter {

    private MavenChangesImporter() {}

    public static void performImport(final MavenChangesImporterArgs args) {
        Objects.requireNonNull(args, "args");
        final MavenChanges mavenChanges = MavenChanges.readFromFile(args.changesXmlFile);
        mavenChanges.releases.forEach(release -> {
            if ("TBD".equals(release.date)) {
                writeUnreleased(args.changelogDirectory, args.releaseVersionMajor, release);
            } else {
                writeReleased(args.changelogDirectory, release);
            }
        });
    }

    private static void writeUnreleased(
            final Path changelogDirectory,
            final int releaseVersionMajor,
            final MavenChanges.Release release) {
        final Path releaseDirectory = ChangelogFiles.unreleasedDirectory(changelogDirectory, releaseVersionMajor);
        release.actions.forEach(action -> writeAction(releaseDirectory, action));
    }

    private static void writeReleased(final Path changelogDirectory, final MavenChanges.Release release) {

        // Determine the directory for this particular release
        final Path releaseDirectory = ChangelogFiles.releaseDirectory(changelogDirectory, release.version);

        // Write release information
        final Path releaseFile = ChangelogFiles.releaseXmlFile(releaseDirectory);
        final ChangelogRelease changelogRelease = new ChangelogRelease(release.version, release.date);
        changelogRelease.writeToXmlFile(releaseFile);

        // Write release actions
        release.actions.forEach(action -> writeAction(releaseDirectory, action));

    }

    private static void writeAction(final Path releaseDirectory, final MavenChanges.Action action) {
        final ChangelogEntry changelogEntry = changelogEntry(action);
        final String changelogEntryFilename = changelogEntryFilename(action);
        final Path changelogEntryFile = releaseDirectory.resolve(changelogEntryFilename);
        changelogEntry.writeToXmlFile(changelogEntryFile);
    }

    private static String changelogEntryFilename(final MavenChanges.Action action) {
        final StringBuilder actionRelativeFileBuilder = new StringBuilder();
        if (action.issue != null) {
            actionRelativeFileBuilder
                    .append(action.issue)
                    .append('_');
        }
        final String sanitizedDescription = action
                .description
                .replaceAll("[^A-Za-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[^A-Za-z0-9]*", "")
                .replaceAll("[^A-Za-z0-9]*$", "");
        final String shortenedSanitizedDescription = sanitizedDescription.length() > 60
                ? sanitizedDescription.substring(0, 60)
                : sanitizedDescription;
        actionRelativeFileBuilder.append(shortenedSanitizedDescription);
        actionRelativeFileBuilder.append(".xml");
        return actionRelativeFileBuilder.toString();
    }

    private static ChangelogEntry changelogEntry(final MavenChanges.Action action) {

        // Create the `type`
        final ChangelogEntry.Type type = changelogType(action.type);

        // Create the `issue`s
        final List<ChangelogEntry.Issue> issues = new ArrayList<>(1);
        if (action.issue != null) {
            final String issueLink = String.format("https://issues.apache.org/jira/browse/%s", action.issue);
            final ChangelogEntry.Issue issue = new ChangelogEntry.Issue(action.issue, issueLink);
            issues.add(issue);
        }

        // Create the `author`s
        final List<ChangelogEntry.Author> authors = new ArrayList<>(2);
        for (final String authorId : action.dev.split("\\s*,\\s*")) {
            if (!isBlank(authorId)) {
                authors.add(new ChangelogEntry.Author(authorId, null));
            }
        }
        if (action.dueTo != null) {
            authors.add(new ChangelogEntry.Author(null, action.dueTo));
        }

        // Create the `description`
        final ChangelogEntry.Description description = new ChangelogEntry.Description("asciidoc", action.description);

        // Create the instance
        return new ChangelogEntry(type, issues, authors, description);

    }

    /**
     * Maps {@code maven-changes-plugin} action types to their {@code Keep a Changelog} equivalents.
     */
    private static ChangelogEntry.Type changelogType(final MavenChanges.Action.Type type) {
        if (MavenChanges.Action.Type.ADD.equals(type)) {
            return ChangelogEntry.Type.ADDED;
        } else if (MavenChanges.Action.Type.FIX.equals(type)) {
            return ChangelogEntry.Type.FIXED;
        } else if (MavenChanges.Action.Type.REMOVE.equals(type)) {
            return ChangelogEntry.Type.REMOVED;
        } else {
            return ChangelogEntry.Type.CHANGED;
        }
    }

}
