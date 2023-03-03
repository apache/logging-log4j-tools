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

import org.apache.logging.log4j.changelog.exporter.ChangelogExporter;
import org.apache.logging.log4j.changelog.exporter.ChangelogExporterArgs;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal exporting a release changelog directory against a single template.
 *
 * @see ChangelogExporter
 */
@Mojo(name = "export-single")
public class ExportSingleMojo extends AbstractMojo {

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = ChangelogExporterArgs.CHANGELOG_DIRECTORY_PROPERTY_NAME,
            required = true)
    private File changelogDirectory;

    /**
     * The version to be released, e.g., {@code 2.19.0}.
     */
    @Parameter(
            property = ChangelogExporterArgs.RELEASE_VERSION_PROPERTY_NAME,
            required = true)
    private String releaseVersion;

    /**
     * The template file, e.g., {@code src/changelog/changelog-email.txt.ftl}.
     */
    @Parameter(
            property = ChangelogExporterArgs.TEMPLATE_FILE_PROPERTY_NAME,
            required = true)
    private File templateFile;

    /**
     * The output file, e.g., {@code /tmp/changelog-email.txt}.
     */
    @Parameter(
            property = ChangelogExporterArgs.OUTPUT_FILE_PROPERTY_NAME,
            required = true)
    private File outputFile;

    public void execute() {
        ChangelogExporterArgs args = ChangelogExporterArgs.ofSingle(
                changelogDirectory.toPath(),
                releaseVersion,
                templateFile.toPath(),
                outputFile.toPath());
        ChangelogExporter.performExport(args);
    }

}
