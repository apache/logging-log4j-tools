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
package org.apache.logging.log4j;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

import org.apache.logging.log4j.changelog.releaser.ChangelogReleaser;
import org.apache.logging.log4j.changelog.releaser.ChangelogReleaserArgs;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal moving the contents of an unreleased changelog directory (e.g., {@code .2.x.x} to a released one (e.g., {@code 2.19.0}).
 *
 * @see ChangelogReleaser
 */
@Mojo(name = "release", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public final class ReleaseMojo extends AbstractMojo {

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = "log4j.changelog.directory",
            required = true)
    private File changelogDirectory;

    /**
     * The version to be released, e.g., {@code 2.19.0}.
     */
    @Parameter(
            property = "log4j.changelog.releaseVersion",
            required = true)
    private String releaseVersion;

    /**
     * The regular expression pattern for parsing versions.
     * <p>
     * The pattern must provide the following named groups:
     * </p>
     * <ol>
     * <li>major</li>
     * <li>minor</li>
     * <li>patch</li>
     * </ol>
     */
    @Parameter(property = "log4j.changelog.versionPattern")
    private String versionPattern;

    @Override
    public synchronized void execute() {
        Pattern compiledVersionPattern = versionPattern != null ? Pattern.compile(versionPattern) : null;
        final ChangelogReleaserArgs args = new ChangelogReleaserArgs(
                changelogDirectory.toPath(),
                releaseVersion,
                compiledVersionPattern,
                LocalDate.now(ZoneId.systemDefault()));
        ChangelogReleaser.performRelease(args);
    }

}
