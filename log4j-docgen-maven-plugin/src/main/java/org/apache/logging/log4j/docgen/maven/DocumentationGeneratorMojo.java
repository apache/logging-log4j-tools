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
package org.apache.logging.log4j.docgen.maven;

import java.io.File;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.generator.DocumentationGenerator;
import org.apache.logging.log4j.docgen.generator.DocumentationGeneratorArgs;
import org.apache.logging.log4j.docgen.generator.DocumentationTemplate;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal generating documentation by feeding the configuration collected from the provided plugin descriptors (e.g., {@code plugins.xml}) to the FreeMarker templates provided.
 *
 * @see DocumentationGenerator
 */
@Mojo(name = "generate-documentation", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class DocumentationGeneratorMojo extends AbstractDocgenMojo {

    /**
     * The path to the FreeMarker template directory.
     */
    @Parameter(property = "log4j.docgen.templateDirectory", required = true)
    private File templateDirectory;

    /**
     * The template that will be used to document all types.
     */
    @Parameter(required = true)
    private DocumentationTemplateMojo indexTemplate;

    /**
     * The template that will be used to document types.
     */
    @Parameter(required = true)
    private DocumentationTemplateMojo typeTemplate;

    @Override
    public void execute() {
        if (skip) {
            getLog().info("Skipping documentation generation");
            return;
        }
        final Set<PluginSet> pluginSets =
                PluginSets.ofDescriptorFilesAndFileMatchers(descriptorFiles, descriptorFileMatchers);
        final Predicate<String> classNameFilter = typeFilter != null ? typeFilter.createPredicate() : ignored -> true;
        final DocumentationGeneratorArgs generatorArgs = new DocumentationGeneratorArgs(
                pluginSets,
                classNameFilter,
                templateDirectory.toPath(),
                toApiModel(indexTemplate),
                toApiModel(typeTemplate));
        DocumentationGenerator.generateDocumentation(generatorArgs);
    }

    private static DocumentationTemplate toApiModel(final DocumentationTemplateMojo mojo) {
        return new DocumentationTemplate(mojo.source, mojo.target);
    }
}
