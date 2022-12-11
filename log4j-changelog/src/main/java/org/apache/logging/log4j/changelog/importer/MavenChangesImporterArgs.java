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

import static org.apache.logging.log4j.PropertyUtils.requireNonBlankIntProperty;
import static org.apache.logging.log4j.PropertyUtils.requireNonBlankPathProperty;

final class MavenChangesImporterArgs {

    final Path changelogDirectory;

    final Path changesXmlFile;

    final int releaseVersionMajor;

    private MavenChangesImporterArgs(final Path changelogDirectory, final Path changesXmlFile, final int releaseVersionMajor) {
        this.changelogDirectory = changelogDirectory;
        this.changesXmlFile = changesXmlFile;
        this.releaseVersionMajor = releaseVersionMajor;
    }

    static MavenChangesImporterArgs fromSystemProperties() {
        final Path changelogDirectory = requireNonBlankPathProperty("log4j.changelog.directory");
        final Path changesXmlFile = requireNonBlankPathProperty("log4j.changelog.changesXmlFile");
        final int releaseVersion = requireNonBlankIntProperty("log4j.changelog.releaseVersionMajor");
        return new MavenChangesImporterArgs(changelogDirectory, changesXmlFile, releaseVersion);
    }

}
