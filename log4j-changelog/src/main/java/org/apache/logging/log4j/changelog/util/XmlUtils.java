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
package org.apache.logging.log4j.changelog.util;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

final class XmlUtils {

    static final String XML_NAMESPACE = "http://logging.apache.org/log4j/changelog";

    static final String XML_SCHEMA_LOCATION = "https://logging.apache.org/log4j/changelog-0.1.2.xsd";

    private XmlUtils() {}

    static DocumentBuilderFactory createDocumentBuilderFactory() {
        final DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
        final Schema schema = readSchema();
        dbf.setSchema(schema);
        dbf.setValidating(true);
        return dbf;
    }

    /**
     * @return a {@link DocumentBuilderFactory} instance configured with certain XXE protection measures
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxp-documentbuilderfactory-saxparserfactory-and-dom4j">XML External Entity Prevention Cheat Sheet</a>
     */
    private static DocumentBuilderFactory createSecureDocumentBuilderFactory() {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String feature = null;
        try {

            // This is the PRIMARY defense.
            // If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented.
            // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
            feature = "http://apache.org/xml/features/disallow-doctype-decl";
            dbf.setFeature(feature, true);

            // If you can't completely disable DTDs, then at least do the following:
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            // JDK7+ - http://xml.org/sax/features/external-general-entities
            // This feature has to be used together with the following one, otherwise it will not protect you from XXE for sure.
            feature = "http://xml.org/sax/features/external-general-entities";
            dbf.setFeature(feature, false);

            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
            // JDK7+ - http://xml.org/sax/features/external-parameter-entities
            // This feature has to be used together with the previous one, otherwise it will not protect you from XXE for sure.
            feature = "http://xml.org/sax/features/external-parameter-entities";
            dbf.setFeature(feature, false);

            // Disable external DTDs as well
            feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dbf.setFeature(feature, false);

            // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

        } catch (final ParserConfigurationException error) {
            final String message = String.format("`%s` is probably not supported by your XML processor", feature);
            throw new RuntimeException(message, error);
        }
        return dbf;
    }

    private static Schema readSchema() {

        // Read the schema file resource
        final String schemaFileName = "/log4j-changelog.xsd";
        final InputStream schemaInputStream = XmlUtils.class.getResourceAsStream(schemaFileName);
        if (schemaInputStream == null) {
            final String message = String.format("could not find the schema file resource: `%s`", schemaFileName);
            throw new RuntimeException(message);
        }

        // Read the schema
        try {
            final StreamSource schemaSource = new StreamSource(schemaInputStream);
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return schemaFactory.newSchema(schemaSource);
        } catch (final Exception error) {
            final String message = String.format("failed to load schema from file resource: `%s`", schemaFileName);
            throw new RuntimeException(message, error);
        }

    }

}
