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
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.internal.SchemaGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal generating an XSD from a given plugin descriptor, e.g., {@code plugins.xml}.
 *
 * @see org.apache.logging.log4j.docgen.internal.SchemaGenerator
 */
@Mojo(name = "generate-schema", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class SchemaGeneratorMojo extends AbstractMojo {

    /**
     * The paths of the plugin descriptor XML files
     */
    @Parameter(property = "log4j.docgen.descriptorFiles", required = true)
    private File[] descriptorFiles;

    /**
     * The path of the XSD file to write to
     */
    @Parameter(property = "log4j.docgen.schemaFile", required = true)
    private File schemaFile;

    @Override
    public void execute() throws MojoExecutionException {
        final Set<PluginSet> pluginSets = PluginSets.ofDescriptorFiles(descriptorFiles);
        try {
            SchemaGenerator.generateSchema(pluginSets, schemaFile.toPath());
        } catch (final XMLStreamException error) {
            final String message = String.format("failed generating the schema file `%s`", schemaFile);
            throw new MojoExecutionException(message, error);
        }
    }
}
