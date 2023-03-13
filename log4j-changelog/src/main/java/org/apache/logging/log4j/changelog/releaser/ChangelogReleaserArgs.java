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

import static org.apache.logging.log4j.changelog.util.PropertyUtils.requireNonBlankPathProperty;
import static org.apache.logging.log4j.changelog.util.PropertyUtils.requireNonBlankStringProperty;
import static org.apache.logging.log4j.changelog.util.VersionUtils.requireSemanticVersioning;

public final class ChangelogReleaserArgs {

    public static final String CHANGELOG_DIRECTORY_PROPERTY_NAME = "log4j.changelog.directory";

    public static final String RELEASE_VERSION_PROPERTY_NAME = "log4j.changelog.releaseVersion";

    final Path changelogDirectory;

    final String releaseVersion;

    final LocalDate releaseDate;

    public ChangelogReleaserArgs(
            final Path changelogDirectory,
            final String releaseVersion,
            final LocalDate releaseDate) {

        // Check arguments
        Objects.requireNonNull(changelogDirectory, "changelogDirectory");
        Objects.requireNonNull(releaseVersion, "releaseVersion");
        requireSemanticVersioning(releaseVersion, RELEASE_VERSION_PROPERTY_NAME);
        Objects.requireNonNull(releaseDate, "releaseDate");

        // Set fields
        this.changelogDirectory = changelogDirectory;
        this.releaseVersion = releaseVersion;
        this.releaseDate = releaseDate;

    }

    static ChangelogReleaserArgs fromSystemProperties() {
        final Path changelogDirectory = requireNonBlankPathProperty(CHANGELOG_DIRECTORY_PROPERTY_NAME);
        final String releaseVersion = requireNonBlankStringProperty(RELEASE_VERSION_PROPERTY_NAME);
        final LocalDate releaseDate = LocalDate.now();
        return new ChangelogReleaserArgs(changelogDirectory, releaseVersion, releaseDate);
    }

}
