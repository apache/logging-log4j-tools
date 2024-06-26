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
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.generator.SchemaGenerator;
import org.apache.logging.log4j.docgen.generator.SchemaGeneratorArgs;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal generating an XSD from a given plugin descriptor, e.g., {@code plugins.xml}.
 *
 * @see SchemaGenerator
 */
@Mojo(name = "generate-schema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class SchemaGeneratorMojo extends AbstractDocgenMojo {

    /**
     * The version of the XSD
     */
    @Parameter(property = "log4j.docgen.schemaVersion", required = true)
    private String schemaVersion;

    /**
     * The path of the XSD file to write to
     */
    @Parameter(property = "log4j.docgen.schemaFile", required = true)
    private File schemaFile;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping schema generation");
            return;
        }
        final Set<PluginSet> pluginSets =
                PluginSets.ofDescriptorFilesAndFileMatchers(descriptorFiles, descriptorFileMatchers);
        final Predicate<String> classNameFilter = typeFilter != null ? typeFilter.createPredicate() : ignored -> true;
        try {
            final SchemaGeneratorArgs generatorArgs =
                    new SchemaGeneratorArgs(pluginSets, classNameFilter, schemaVersion, schemaFile.toPath());
            SchemaGenerator.generateSchema(generatorArgs);
        } catch (final XMLStreamException error) {
            final String message = String.format("failed generating the schema file `%s`", schemaFile);
            throw new MojoExecutionException(message, error);
        }
    }
}
