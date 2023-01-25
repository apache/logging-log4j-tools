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
package org.apache.logging.log4j;

import java.io.File;
import java.time.LocalDate;

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
@Mojo(name = "release", defaultPhase = LifecyclePhase.VALIDATE)
public class ReleaseMojo extends AbstractMojo {

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = ChangelogReleaserArgs.CHANGELOG_DIRECTORY_PROPERTY_NAME,
            required = true)
    private File changelogDirectory;

    /**
     * The version to be released, e.g., {@code 2.19.0}.
     */
    @Parameter(
            property = ChangelogReleaserArgs.RELEASE_VERSION_PROPERTY_NAME,
            required = true)
    private String releaseVersion;

    public void execute() {
        ChangelogReleaserArgs args = new ChangelogReleaserArgs(
                changelogDirectory.toPath(),
                releaseVersion,
                LocalDate.now());
        ChangelogReleaser.performRelease(args);
    }

}
