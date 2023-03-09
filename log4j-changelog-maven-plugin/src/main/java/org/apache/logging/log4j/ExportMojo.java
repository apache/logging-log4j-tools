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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal exporting the changelog directory.
 *
 * @see ChangelogExporter
 */
@Mojo(name = "export", defaultPhase = LifecyclePhase.PRE_SITE)
public class ExportMojo extends AbstractMojo {

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = "log4j.changelog.directory",
            required = true)
    private File changelogDirectory;

    /**
     * Directory to write generated changelog files.
     */
    @Parameter(
            defaultValue = "${project.build.directory}/generated-sources/site/asciidoc/changelog",
            property = "log4j.changelog.outputDirectory",
            required = true)
    private File outputDirectory;

    public void execute() {
        ChangelogExporterArgs args = new ChangelogExporterArgs(
                changelogDirectory.toPath(),
                outputDirectory.toPath());
        ChangelogExporter.performExport(args);
    }

}
