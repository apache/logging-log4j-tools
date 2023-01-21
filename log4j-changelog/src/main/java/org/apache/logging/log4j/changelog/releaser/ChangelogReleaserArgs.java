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

import static org.apache.logging.log4j.changelog.util.PropertyUtils.requireNonBlankPathProperty;
import static org.apache.logging.log4j.changelog.util.PropertyUtils.requireNonBlankStringProperty;
import static org.apache.logging.log4j.changelog.util.VersionUtils.requireSemanticVersioning;

public final class ChangelogReleaserArgs {

    final Path changelogDirectory;

    final String releaseVersion;

    private ChangelogReleaserArgs(final Path changelogDirectory, final String releaseVersion) {
        this.changelogDirectory = changelogDirectory;
        this.releaseVersion = releaseVersion;
    }

    static ChangelogReleaserArgs fromSystemProperties() {
        final Path changelogDirectory = requireNonBlankPathProperty("log4j.changelog.directory");
        final String releaseVersionProperty = "log4j.changelog.releaseVersion";
        final String releaseVersion = requireNonBlankStringProperty(releaseVersionProperty);
        requireSemanticVersioning(releaseVersion, releaseVersionProperty);
        return new ChangelogReleaserArgs(changelogDirectory, releaseVersion);
    }

    public static ChangelogReleaserArgs fromArgs(final Path changelogDirectory, final String releaseVersion) {
        return new ChangelogReleaserArgs(changelogDirectory, releaseVersion);
    }

}
