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

import static org.apache.logging.log4j.changelog.util.PropertyUtils.requireNonBlankPathProperty;

public final class ChangelogExporterArgs {

    public static final String CHANGELOG_DIRECTORY_PROPERTY_NAME = "log4j.changelog.directory";

    public static final String OUTPUT_DIRECTORY_PROPERTY_NAME = "log4j.changelog.outputDirectory";

    final Path changelogDirectory;

    final Path outputDirectory;

    public ChangelogExporterArgs(final Path changelogDirectory, final Path outputDirectory) {
        this.changelogDirectory = Objects.requireNonNull(changelogDirectory, "changelogDirectory");
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
    }

    static ChangelogExporterArgs fromSystemProperties() {
        final Path changelogDirectory = requireNonBlankPathProperty(CHANGELOG_DIRECTORY_PROPERTY_NAME);
        final Path outputDirectory = requireNonBlankPathProperty(OUTPUT_DIRECTORY_PROPERTY_NAME);
        return new ChangelogExporterArgs(changelogDirectory, outputDirectory);
    }

}
