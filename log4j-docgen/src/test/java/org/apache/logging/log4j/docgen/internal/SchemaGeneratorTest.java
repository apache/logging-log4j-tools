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

import static org.apache.logging.log4j.docgen.TestUtils.resourceStream;
import static org.apache.logging.log4j.docgen.TestUtils.resourceUrl;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.apache.logging.log4j.docgen.xsd.SchemaGenerator;
import org.apache.logging.log4j.docgen.xsd.SchemaGeneratorRequest;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.Input;

public class SchemaGeneratorTest {

    @Test
    void schemaGeneration() {
        final Source xmlSchema =
                Input.fromURI("https://www.w3.org/2001/XMLSchema.xsd").build();
        final URL expectedSchema = resourceUrl("/expected/xsd/log4j-config.xsd");
        final Path actualSchema = assertDoesNotThrow(SchemaGeneratorTest::generateSchema);
        XmlAssert.assertThat(actualSchema)
                .isValidAgainst(xmlSchema)
                .and(expectedSchema)
                .ignoreComments()
                .ignoreWhitespace()
                .areIdentical();
    }

    private static Path generateSchema() throws XMLStreamException, IOException {
        final PluginBundleStaxReader reader = new PluginBundleStaxReader();
        final PluginSet set = reader.read(resourceStream("/META-INF/log4j/plugins-sample.xml"));
        final SchemaGenerator generator = new DefaultSchemaGenerator();
        final SchemaGeneratorRequest request = new SchemaGeneratorRequest();
        final Path outputDirectory = Paths.get("target/test-site/xsd");
        Files.createDirectories(outputDirectory);
        request.addPluginSet(set);
        request.setOutputDirectory(outputDirectory);

        generator.generateSchema(request);
        return outputDirectory.resolve(request.getFileName());
    }
}
