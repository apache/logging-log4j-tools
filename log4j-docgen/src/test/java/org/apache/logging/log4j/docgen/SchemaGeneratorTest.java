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
package org.apache.logging.log4j.docgen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import org.apache.logging.log4j.docgen.internal.DefaultSchemaGenerator;
import org.apache.logging.log4j.docgen.model.PluginBundle;
import org.apache.logging.log4j.docgen.model.io.stax.PluginBundleStaxReader;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.Input;

public class SchemaGeneratorTest {

    @Test
    void schemaGeneration() throws Exception {
        final URL xmlSchemaLocation = SchemaGenerator.class.getResource("/xsd/XMLSchema.xsd");
        final Source xmlSchema = Input.fromURL(xmlSchemaLocation).build();
        final URL expectedSchema = SchemaGenerator.class.getResource("/xsd/log4j.xsd");
        final String actualSchema = assertDoesNotThrow(() -> generateSchema());
        XmlAssert.assertThat(actualSchema)
                .isValidAgainst(xmlSchema)
                .and(expectedSchema)
                .ignoreComments()
                .ignoreWhitespace()
                .areIdentical();
    }

    private static String generateSchema() throws XMLStreamException, IOException {
        final PluginBundleStaxReader reader = new PluginBundleStaxReader();
        final PluginBundle bundle =
                reader.read(SchemaGeneratorTest.class.getResourceAsStream("/META-INF/log4j/plugins.xml"));
        final SchemaGenerator generator = new DefaultSchemaGenerator();
        final XMLOutputFactory factory = XMLOutputFactory.newInstance();
        final StringWriter writer = new StringWriter();
        final XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(writer);
        generator.generateSchema(Collections.singleton(bundle), xmlWriter);
        xmlWriter.close();
        return writer.toString();
    }
}
