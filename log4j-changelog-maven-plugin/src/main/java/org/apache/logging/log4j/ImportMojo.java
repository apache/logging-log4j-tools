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

import org.apache.logging.log4j.changelog.importer.MavenChangesImporter;
import org.apache.logging.log4j.changelog.importer.MavenChangesImporterArgs;
import org.apache.logging.log4j.changelog.releaser.ChangelogReleaser;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal populating a changelog directory from <a href="https://maven.apache.org/plugins/maven-changes-plugin/">maven-changes-plugin</a> source XML.
 *
 * @see ChangelogReleaser
 */
@Mojo(name = "import")
public class ImportMojo extends AbstractMojo {

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = MavenChangesImporterArgs.CHANGELOG_DIRECTORY_PROPERTY_NAME,
            required = true)
    private File changelogDirectory;

    /**
     * <a href="https://maven.apache.org/plugins/maven-changes-plugin/">maven-changes-plugin</a> source XML, {@code changes.xml}, location.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changes/changes.xml",
            property = MavenChangesImporterArgs.CHANGES_XML_FILE_PROPERTY_NAME,
            required = true)
    private File changesXmlFile;

    /**
     * The upcoming release version major number, e.g., {@code 2} for {@code 2.x.x} releases.
     */
    @Parameter(
            property = MavenChangesImporterArgs.RELEASE_VERSION_MAJOR_PROPERTY_NAME,
            required = true)
    private int releaseVersionMajor;

    public void execute() {
        MavenChangesImporterArgs args = new MavenChangesImporterArgs(
                changelogDirectory.toPath(),
                changesXmlFile.toPath(),
                releaseVersionMajor);
        MavenChangesImporter.performImport(args);
    }

}
