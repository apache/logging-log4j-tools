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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.changelog.ChangelogFiles;
import org.apache.logging.log4j.changelog.exporter.ChangelogExporter;
import org.apache.logging.log4j.changelog.exporter.ChangelogExporterArgs;
import org.apache.logging.log4j.changelog.exporter.ChangelogExporterTemplate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal exporting the changelog directory.
 *
 * @see ChangelogExporter
 */
@Mojo(name = "export", defaultPhase = LifecyclePhase.PRE_SITE, threadSafe = true)
public final class ExportMojo extends AbstractMojo {

    private static final String SOURCE_TARGET_TEMPLATE_PATTERN =
            "^\\.(.*)\\." + ChangelogFiles.templateFileNameExtension() + '$';

    /**
     * Directory containing release folders composed of changelog entry XML files.
     */
    @Parameter(
            defaultValue = "${project.basedir}/src/changelog",
            property = "log4j.changelog.directory",
            required = true)
    private File changelogDirectory;

    /**
     * Templates that will be rendered with the release information of all releases, e.g., to generate an index page.
     */
    @Parameter(required = true)
    private List<TemplateMojo> indexTemplates;

    /**
     * Templates that will be rendered with the release and changelog information of a particular release, e.g., to generate the release notes.
     */
    @Parameter(required = true)
    private List<TemplateMojo> changelogTemplates;

    /**
     * Directory to write rendered templates.
     */
    @Parameter(
            defaultValue = "${project.build.directory}/generated-sources/site/changelog",
            property = "log4j.changelog.outputDirectory",
            required = true)
    private File outputDirectory;

    @Override
    public void execute() {
        final Set<ChangelogExporterTemplate> translatedIndexTemplates = toExporterTemplates(indexTemplates);
        final Set<ChangelogExporterTemplate> translatedReleaseChangelogTemplates =
                toExporterTemplates(changelogTemplates);
        final ChangelogExporterArgs args = new ChangelogExporterArgs(
                changelogDirectory.toPath(),
                translatedIndexTemplates,
                translatedReleaseChangelogTemplates,
                outputDirectory.toPath());
        ChangelogExporter.performExport(args);
    }

    private static Set<ChangelogExporterTemplate> toExporterTemplates(Collection<TemplateMojo> templateMojos) {
        return templateMojos.stream().map(ExportMojo::toExporterTemplate).collect(Collectors.toSet());
    }

    private static ChangelogExporterTemplate toExporterTemplate(TemplateMojo templateMojo) {
        final String target = templateMojo.target == null
                ? templateMojo.source.replaceAll(SOURCE_TARGET_TEMPLATE_PATTERN, "$1")
                : templateMojo.target;
        return new ChangelogExporterTemplate(templateMojo.source, target, templateMojo.failIfNotFound);
    }
}
