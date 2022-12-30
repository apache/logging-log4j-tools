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
package org.apache.logging.log4j.tools.changelog.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.logging.log4j.tools.CharsetUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.*;

final class FreeMarkerUtils {

    private FreeMarkerUtils() {}

    private static final Configuration CONFIGURATION = createConfiguration();

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static Configuration createConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_29);
        configuration.setDefaultEncoding(CharsetUtils.CHARSET_NAME);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(new File("/")));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        DefaultObjectWrapperBuilder objectWrapperBuilder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_27);
        objectWrapperBuilder.setExposeFields(true);
        DefaultObjectWrapper objectWrapper = objectWrapperBuilder.build();
        configuration.setObjectWrapper(objectWrapper);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
        return configuration;
    }

    @SuppressFBWarnings("TEMPLATE_INJECTION_FREEMARKER")
    static void render(final Path templateFile, final Object templateData, final Path outputFile) {
        try {
            final Template template = CONFIGURATION.getTemplate(templateFile.toAbsolutePath().toString());
            final Path outputFileParent = outputFile.getParent();
            if (outputFileParent != null) {
                Files.createDirectories(outputFileParent);
            }
            try (final BufferedWriter outputFileWriter = Files.newBufferedWriter(
                    outputFile,
                    CharsetUtils.CHARSET,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                template.process(templateData, outputFileWriter);
            }
        } catch (final Exception error) {
            final String message = String.format(
                    "failed rendering template `%s` to file `%s`",
                    templateFile,
                    outputFile);
            throw new RuntimeException(message, error);
        }
    }

}
