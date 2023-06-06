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
package org.apache.logging.log4j.changelog.releaser;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("REDOS")
public final class ChangelogReleaserArgs {

    static final Pattern DEFAULT_VERSION_PATTERN = Pattern.compile(
            "^(?<major>0|[1-9]\\d*)\\." +
                    "(?<minor>0|[1-9]\\d*)\\." +
                    "(?<patch>0|[1-9]\\d*(-[a-zA-Z][0-9a-zA-Z-]*)?)$");

    final Path changelogDirectory;

    final String releaseVersion;

    final int releaseVersionMajor;

    final LocalDate releaseDate;

    public ChangelogReleaserArgs(
            final Path changelogDirectory,
            final String releaseVersion,
            final Pattern versionPattern,
            final LocalDate releaseDate) {
        this.changelogDirectory = Objects.requireNonNull(changelogDirectory, "changelogDirectory");
        this.releaseVersion = Objects.requireNonNull(releaseVersion, "releaseVersion");
        final Pattern effectiveVersionPattern = versionPattern != null ? versionPattern : DEFAULT_VERSION_PATTERN;
        this.releaseVersionMajor = readReleaseVersionMajor(releaseVersion, effectiveVersionPattern);
        this.releaseDate = Objects.requireNonNull(releaseDate, "releaseDate");
    }

    private static int readReleaseVersionMajor(final String releaseVersion, final Pattern versionPattern) {

        // Match the version string
        final Matcher releaseVersionMatcher = versionPattern.matcher(releaseVersion);
        if (!releaseVersionMatcher.matches()) {
            final String message = String.format(
                    "provided version `%s` does not match the expected pattern `%s`",
                    releaseVersion, versionPattern);
            throw new IllegalArgumentException(message);
        }

        // Extract the version major
        final String releaseVersionMajorString = releaseVersionMatcher.group("major");
        if (releaseVersionMajorString == null) {
            final String message = String.format(
                    "was expecting version pattern `%s` to provide a `major`-named group matching against the given version `%s`",
                    versionPattern, releaseVersion);
            throw new IllegalArgumentException(message);
        }
        return Integer.parseInt(releaseVersionMajorString);

    }

}
