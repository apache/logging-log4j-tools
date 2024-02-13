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
package org.apache.logging.log4j.docgen.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.transform.Source;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.apache.logging.log4j.docgen.xsd.SchemaGenerator;
import org.apache.logging.log4j.docgen.xsd.SchemaGeneratorRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.Input;

public class SchemaGeneratorTest {

    @Test
    void schemaGeneration(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path outputDir) throws Exception {
        final Source xmlSchema =
                Input.fromURI("https://www.w3.org/2001/XMLSchema.xsd").build();
        final Path expectedSchema = Paths.get("src/test/resources/generated-plugins.xsd");
        final Path actualSchema = generateSchema(outputDir);
        XmlAssert.assertThat(actualSchema)
                .isValidAgainst(xmlSchema)
                .and(expectedSchema)
                .ignoreComments()
                .ignoreWhitespace()
                .areIdentical();
    }

    private static Path generateSchema(final Path outputDir) throws Exception {

        // Read sample plugins
        final PluginBundleStaxReader reader = new PluginBundleStaxReader();
        final PluginSet pluginSet = reader.read("src/test/resources/example-plugins.xml");

        // Generate the schema
        final SchemaGeneratorRequest request = new SchemaGeneratorRequest();
        request.addPluginSet(pluginSet);
        request.setOutputDirectory(outputDir);
        final SchemaGenerator generator = new DefaultSchemaGenerator();
        generator.generateSchema(request);

        // Return the generated XSD file path
        return outputDir.resolve(request.getFileName());
    }
}
