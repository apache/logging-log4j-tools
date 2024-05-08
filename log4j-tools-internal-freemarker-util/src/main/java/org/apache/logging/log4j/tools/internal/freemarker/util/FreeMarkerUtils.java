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
package org.apache.logging.log4j.tools.internal.freemarker.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class FreeMarkerUtils {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String CHARSET_NAME = CHARSET.name();

    private static final Version CONFIGURATION_VERSION = Configuration.VERSION_2_3_29;

    private FreeMarkerUtils() {}

    private static final class StopMethod implements TemplateMethodModelEx {

        private static final StopMethod INSTANCE = new StopMethod();

        private static final TemplateModelException SIGNAL = new TemplateModelException();

        @Override
        public Object exec(final List arguments) throws TemplateModelException {
            throw SIGNAL;
        }

        private static boolean invoked(final Throwable throwable) {

            // Keep a second pointer that slowly walks the causal chain.
            // If the fast pointer ever catches the slower pointer, then there's a loop.
            Throwable slowPointer = throwable;
            boolean advanceSlowPointer = false;

            Throwable parent = throwable;
            while (true) {

                // Check the exception
                if (parent == StopMethod.SIGNAL) {
                    return true;
                }

                // Advance the cause
                final Throwable cause = parent.getCause();
                if (cause == null) {
                    break;
                }

                // Advance pointers
                parent = cause;
                if (parent == slowPointer) {
                    throw new IllegalArgumentException("loop in causal chain");
                }
                if (advanceSlowPointer) {
                    slowPointer = slowPointer.getCause();
                }
                advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
            }
            return false;
        }
    }

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static Configuration createConfiguration(final Path templateDirectory) {
        final Configuration configuration = new Configuration(CONFIGURATION_VERSION);
        configuration.setDefaultEncoding(CHARSET_NAME);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(templateDirectory.toFile()));
        } catch (final IOException error) {
            throw new UncheckedIOException(error);
        }
        final DefaultObjectWrapperBuilder objectWrapperBuilder = new DefaultObjectWrapperBuilder(CONFIGURATION_VERSION);
        objectWrapperBuilder.setExposeFields(true);
        final DefaultObjectWrapper objectWrapper = objectWrapperBuilder.build();
        configuration.setObjectWrapper(objectWrapper);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
        configuration.setSharedVariable("stop", StopMethod.INSTANCE);
        return configuration;
    }

    @SuppressFBWarnings("TEMPLATE_INJECTION_FREEMARKER")
    public static void render(
            final Path templateDirectory, final String templateName, final Object templateData, final Path outputFile) {
        try {

            // Render the template
            final Configuration configuration = createConfiguration(templateDirectory);
            final Template template = configuration.getTemplate(templateName);
            final StringWriter templateOutputWriter = new StringWriter();
            template.process(templateData, templateOutputWriter);
            final String templateOutput = templateOutputWriter.toString();

            // Write the template output to the target file
            final Path outputFileParent = outputFile.getParent();
            if (outputFileParent != null) {
                Files.createDirectories(outputFileParent);
            }
            try (final BufferedWriter outputFileWriter = Files.newBufferedWriter(
                    outputFile, CHARSET, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                outputFileWriter.write(templateOutput);
            }
        } catch (final Exception error) {
            final boolean stopMethodInvoked = StopMethod.invoked(error);
            if (!stopMethodInvoked) {
                final String message = String.format(
                        "failed rendering template `%s` in directory `%s` to file `%s`",
                        templateName, templateDirectory, outputFile);
                throw new RuntimeException(message, error);
            }
        }
    }

    @SuppressFBWarnings("TEMPLATE_INJECTION_FREEMARKER")
    public static String renderString(final String templateString, final Object templateData) {
        final Configuration configuration = new Configuration(CONFIGURATION_VERSION);
        final DefaultObjectWrapperBuilder objectWrapperBuilder = new DefaultObjectWrapperBuilder(CONFIGURATION_VERSION);
        objectWrapperBuilder.setExposeFields(true);
        final DefaultObjectWrapper objectWrapper = objectWrapperBuilder.build();
        configuration.setObjectWrapper(objectWrapper);
        try {
            final Template template =
                    new Template("ephemeralInlineTemplate", new StringReader(templateString), configuration);
            final Writer templateWriter = new StringWriter();
            template.process(templateData, templateWriter);
            return templateWriter.toString();
        } catch (final Exception error) {
            final String message = String.format("failed rendering template `%s`", templateString);
            throw new RuntimeException(message, error);
        }
    }
}
