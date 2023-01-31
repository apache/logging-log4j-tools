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
package org.apache.logging.log4j.changelog.util;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class XmlWriter {

    private static final String LS = System.lineSeparator();

    private XmlWriter() {}

    public static void toFile(
            final Path filepath,
            final String rootElementName,
            final BiConsumer<Document, Element> documentConsumer) {
        try {
            final String xml = toString(rootElementName, documentConsumer);
            final byte[] xmlBytes = xml.getBytes(CharsetUtils.CHARSET);
            @Nullable
            final Path filepathParent = filepath.getParent();
            if (filepathParent != null) {
                Files.createDirectories(filepathParent);
            }
            Files.write(filepath, xmlBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final Exception error) {
            final String message = String.format("failed writing XML to file `%s`", filepath);
            throw new RuntimeException(message, error);
        }
    }

    public static String toString(final String rootElementName, final BiConsumer<Document, Element> documentConsumer) {
        try {

            // Create the document
            final DocumentBuilderFactory documentBuilderFactory = XmlUtils.createDocumentBuilderFactory();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            // Append the license comment
            final Document document = documentBuilder.newDocument();
            document.setXmlStandalone(true);
            final Comment licenseComment = document.createComment(LS +
                    "  Licensed to the Apache Software Foundation (ASF) under one or more" + LS +
                    "  contributor license agreements. See the NOTICE file distributed with" + LS +
                    "  this work for additional information regarding copyright ownership." + LS +
                    "  The ASF licenses this file to You under the Apache License, Version 2.0" + LS +
                    "  (the \"License\"); you may not use this file except in compliance with" + LS +
                    "  the License. You may obtain a copy of the License at" + LS +
                    LS +
                    "      https://www.apache.org/licenses/LICENSE-2.0" + LS +
                    LS +
                    "  Unless required by applicable law or agreed to in writing, software" + LS +
                    "  distributed under the License is distributed on an \"AS IS\" BASIS," + LS +
                    "  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." + LS +
                    "  See the License for the specific language governing permissions and" + LS +
                    "  limitations under the License." + LS);
            document.appendChild(licenseComment);

            // Create the root element
            final Element rootElement = document.createElement(rootElementName);
            document.appendChild(rootElement);

            // Apply requested changes
            documentConsumer.accept(document, rootElement);

            // Serialize the document
            return serializeXmlDocument(document, rootElementName);

        } catch (final Exception error) {
            throw new RuntimeException("failed writing XML", error);
        }
    }

    @SuppressFBWarnings({"XXE_DTD_TRANSFORM_FACTORY", "XXE_XSLT_TRANSFORM_FACTORY"})
    private static String serializeXmlDocument(final Document document, final String rootElementName) throws Exception {

        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        final StreamResult result = new StreamResult(new StringWriter());
        final DOMSource source = new DOMSource(document);
        transformer.setOutputProperty(OutputKeys.ENCODING, CharsetUtils.CHARSET_NAME);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);

        // Life is too short to solve DOM transformer issues decently
        final String xml = result.getWriter().toString();
        final String padding = StringUtils.repeat(" ", rootElementName.length() + 2);
        return xml
                .replace("?><!--", "?>" + LS + "<!--")
                .replace("--><", "-->" + LS + "<")
                .replaceFirst(
                        '<' + rootElementName + " (.+>" + LS + ")",
                        ('<' + rootElementName + " xmlns=\"" + XmlUtils.XML_NAMESPACE + "\"" + LS +
                                padding + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + LS +
                                padding + "xsi:schemaLocation=\"" + XmlUtils.XML_NAMESPACE + " "+ XmlUtils.XML_SCHEMA_LOCATION + "\"" + LS +
                                padding + "$1"));

    }

}
