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

import java.nio.file.Path;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.Nullable;

import static org.apache.logging.log4j.changelog.util.VersionUtils.requireSemanticVersioning;

public final class ChangelogExporterArgs {

    public static final String CHANGELOG_DIRECTORY_PROPERTY_NAME = "log4j.changelog.directory";

    public static final String OUTPUT_DIRECTORY_PROPERTY_NAME = "log4j.changelog.outputDirectory";

    public static final String RELEASE_VERSION_PROPERTY_NAME = "log4j.changelog.releaseVersion";

    public static final String TEMPLATE_FILE_PROPERTY_NAME = "log4j.changelog.templateFile";

    public static final String OUTPUT_FILE_PROPERTY_NAME = "log4j.changelog.outputFile";

    public enum Mode { MULTI, SINGLE }

    final Mode mode;

    final Path changelogDirectory;

    @Nullable
    final Path outputDirectory;

    @Nullable
    final String releaseVersion;

    @Nullable
    final Path templateFile;

    @Nullable
    final Path outputFile;

    private ChangelogExporterArgs(
            final Mode mode,
            final Path changelogDirectory,
            @Nullable final Path outputDirectory,
            @Nullable final String releaseVersion,
            @Nullable final Path templateFile,
            @Nullable final Path outputFile) {
        this.mode = mode;
        this.changelogDirectory = changelogDirectory;
        this.outputDirectory = outputDirectory;
        this.releaseVersion = releaseVersion;
        this.templateFile = templateFile;
        this.outputFile = outputFile;
    }

    public static ChangelogExporterArgs of(final Path changelogDirectory, final Path outputDirectory) {
        Objects.requireNonNull(changelogDirectory, "changelogDirectory");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        return new ChangelogExporterArgs(
                Mode.MULTI,
                changelogDirectory,
                outputDirectory,
                null,
                null,
                null);
    }

    public static ChangelogExporterArgs ofSingle(
            final Path changelogDirectory,
            final String releaseVersion,
            final Path templateFile,
            final Path outputFile) {
        Objects.requireNonNull(changelogDirectory, "changelogDirectory");
        Objects.requireNonNull(releaseVersion, "releaseVersion");
        requireSemanticVersioning(releaseVersion, RELEASE_VERSION_PROPERTY_NAME);
        Objects.requireNonNull(templateFile, "templateFile");
        Objects.requireNonNull(outputFile, "outputFile");
        return new ChangelogExporterArgs(
                Mode.SINGLE,
                changelogDirectory,
                null,
                releaseVersion,
                templateFile,
                outputFile);
    }

}
