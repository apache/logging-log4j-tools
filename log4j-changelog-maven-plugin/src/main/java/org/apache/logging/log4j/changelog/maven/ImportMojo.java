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
package org.apache.logging.log4j.changelog.maven;

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
@Mojo(name = "import", threadSafe = true)
public final class ImportMojo extends AbstractMojo {

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = "log4j.changelog.directory",
            required = true)
    private File changelogDirectory;

    /**
     * <a href="https://maven.apache.org/plugins/maven-changes-plugin/">maven-changes-plugin</a> source XML, {@code changes.xml}, location.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changes/changes.xml",
            property = "log4j.changelog.changesXmlFile",
            required = true)
    private File changesXmlFile;

    /**
     * The upcoming release version major number, e.g., {@code 2} for {@code 2.x.x} releases.
     */
    @Parameter(property = "log4j.changelog.releaseVersionMajor", required = true)
    private int releaseVersionMajor;

    @Override
    public synchronized void execute() {
        final MavenChangesImporterArgs args =
                new MavenChangesImporterArgs(changelogDirectory.toPath(), changesXmlFile.toPath(), releaseVersionMajor);
        MavenChangesImporter.performImport(args);
    }
}
