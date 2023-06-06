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
package org.apache.logging.log4j.changelog.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.*;
import org.apache.logging.log4j.changelog.util.CharsetUtils;

final class FreeMarkerUtils {

    private FreeMarkerUtils() {}

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static Configuration createConfiguration(final Path templateDirectory) {
        final Version configurationVersion = Configuration.VERSION_2_3_29;
        final Configuration configuration = new Configuration(configurationVersion);
        configuration.setDefaultEncoding(CharsetUtils.CHARSET_NAME);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(templateDirectory.toFile()));
        } catch (final IOException error) {
            throw new UncheckedIOException(error);
        }
        final DefaultObjectWrapperBuilder objectWrapperBuilder =
                new DefaultObjectWrapperBuilder(configurationVersion);
        objectWrapperBuilder.setExposeFields(true);
        final DefaultObjectWrapper objectWrapper = objectWrapperBuilder.build();
        configuration.setObjectWrapper(objectWrapper);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
        return configuration;
    }

    @SuppressFBWarnings("TEMPLATE_INJECTION_FREEMARKER")
    static void render(
            final Path templateDirectory,
            final String templateName,
            final Object templateData,
            final Path outputFile) {
        try {
            final Configuration configuration = createConfiguration(templateDirectory);
            final Template template = configuration.getTemplate(templateName);
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
                    "failed rendering template `%s` in directory `%s` to file `%s`",
                    templateName,
                    templateDirectory,
                    outputFile);
            throw new RuntimeException(message, error);
        }
    }

}
