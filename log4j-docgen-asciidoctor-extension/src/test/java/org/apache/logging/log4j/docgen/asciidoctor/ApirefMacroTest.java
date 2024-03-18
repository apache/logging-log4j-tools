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
package org.apache.logging.log4j.docgen.asciidoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ApirefMacroTest {

    private static final String TEST_NAME = ApirefMacroTest.class.getSimpleName();

    private static final Asciidoctor ASCII_DOCTOR = Asciidoctor.Factory.create();

    private final AttributesBuilder attributesBuilder = Attributes.builder()
            .attribute("log4j-docgen-descriptor-directory", "src/test/resources/" + TEST_NAME + "/plugins.xml")
            .attribute("log4j-docgen-type-template-target", "%g/%a/%c.html");

    @Test
    void unlabeled_unknown_class() {
        assertConversion("apiref:java.lang.String[]", "<code>java.lang.String</code>");
    }

    @Test
    void unlabeled_unknown_class_with_package_name_stripped() {
        attributesBuilder.attribute("log4j-docgen-package-name-stripped", "true");
        assertConversion("apiref:java.lang.String[]", "<code>String</code>");
    }

    @Test
    void labeled_unknown_class() {
        assertConversion("apiref:java.lang.String[foo]", "<em>foo</em>");
    }

    @Test
    void unlabeled_known_class() {
        assertConversion(
                "apiref:example.MyAppender[]",
                "<a href=\"com.example.groupId/example-artifactId/example.MyAppender.html\">MyAppender</a>");
    }

    @Test
    void unlabeled_known_excluded_class() {
        attributesBuilder.attribute("log4j-docgen-type-filter-exclude-pattern", "^example\\.MyAppender$");
        assertConversion("apiref:example.MyAppender[]", "<code>example.MyAppender</code>");
    }

    @Test
    void labeled_known_class() {
        assertConversion(
                "apiref:example.MyAppender[foo]",
                "<a href=\"com.example.groupId/example-artifactId/example.MyAppender.html\">foo</a>");
    }

    private void assertConversion(final String asciiDoc, final String expectedOutput) {
        final String actualOutput = convert(asciiDoc);
        final String expectedOutputInParagraph =
                String.format("<div class=\"paragraph\"><p>%s</p></div>", expectedOutput);
        Assertions.assertThat(actualOutput).as("AsciiDoc: `%s`", asciiDoc).isEqualTo(expectedOutputInParagraph);
    }

    private String convert(final String asciiDoc) {
        final Attributes attributes = attributesBuilder.build();
        final Options options = Options.builder().attributes(attributes).build();
        return ASCII_DOCTOR.convert(asciiDoc, options).replaceAll("(?s)\\r?\n", "");
    }
}
