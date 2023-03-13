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
import java.util.Set;

public final class ChangelogExporterArgs {

    final Path changelogDirectory;

    final Set<ChangelogExporterTemplate> indexTemplates;

    final Set<ChangelogExporterTemplate> changelogTemplates;

    final Path outputDirectory;

    public ChangelogExporterArgs(
            final Path changelogDirectory,
            final Set<ChangelogExporterTemplate> indexTemplates,
            final Set<ChangelogExporterTemplate> changelogTemplates,
            final Path outputDirectory) {
        this.changelogDirectory = Objects.requireNonNull(changelogDirectory, "changelogDirectory");
        this.indexTemplates = Objects.requireNonNull(indexTemplates, "indexTemplates");
        this.changelogTemplates = Objects.requireNonNull(changelogTemplates, "changelogTemplates");
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
    }

}
