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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.apache.logging.log4j.docgen.ScalarType;
import org.apache.logging.log4j.docgen.Type;
import org.apache.logging.log4j.docgen.freemarker.FreeMarkerGenerator;
import org.apache.logging.log4j.docgen.freemarker.FreeMarkerGeneratorRequest;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.apache.logging.log4j.docgen.util.TypeLookup;

public class DefaultFreeMarkerGenerator implements FreeMarkerGenerator {

    private static final String CHARSET = "UTF-8";

    public void generateDocumentation(final FreeMarkerGeneratorRequest request) throws IOException {
        final PluginSet configurationSet;
        try {
            configurationSet = new PluginBundleStaxReader().read(getClass().getResourceAsStream("configuration.xml"));
        } catch (XMLStreamException e) {
            throw new RuntimeException("Internal error: unable to parse resource `configuration.xml`.", e);
        }
        final Collection<PluginSet> extendedSets = new ArrayList<>(request.getPluginSets());
        extendedSets.add(configurationSet);

        final Configuration configuration = createConfiguration(request.getTemplateDirectory());
        final TypeLookup lookup = TypeLookup.of(extendedSets);
        final Collection<ScalarType> scalarTypes = new TreeSet<>(Comparator.comparing(ScalarType::getClassName));
        for (final Type type : lookup.values()) {
            if (type instanceof AbstractType) {
                final String template =
                        type instanceof PluginType ? request.getPluginTemplate() : request.getInterfaceTemplate();
                documentAbstractType(
                        (AbstractType) type, lookup, configuration, template, request.getOutputDirectory());
            }
            if (type instanceof ScalarType) {
                scalarTypes.add((ScalarType) type);
            }
        }
        documentScalarTypes(scalarTypes, configuration, request.getScalarsTemplate(), request.getOutputDirectory());
    }

    private static void documentAbstractType(
            final AbstractType abstractType,
            final TypeLookup lookup,
            final Configuration configuration,
            final String template,
            final Path outputDir) {
        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("type", abstractType);
        templateData.put("lookup", lookup);

        final Path outputFile = createOutputFile(abstractType, outputDir);
        render(configuration, template, templateData, outputFile);
    }

    private static void documentScalarTypes(
            final Collection<ScalarType> scalarTypes,
            final Configuration configuration,
            final String template,
            final Path outputDir) {
        final Map<String, Object> templateData = new HashMap<>();
        templateData.put("scalars", scalarTypes);

        final Path outputFile = outputDir.resolve("scalars.adoc");
        render(configuration, template, templateData, outputFile);
    }

    private static Path createOutputFile(final Type type, final Path outputDir) {
        final Path outputFile = outputDir.resolve("Core").resolve(type.getClassName() + ".adoc");
        if (!outputFile.startsWith(outputDir)) {
            throw new RuntimeException("Class name '" + type.getClassName() + "' caused a path traversal attempt.");
        }
        return outputFile;
    }

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static Configuration createConfiguration(final Path templateDirectory) {
        final Version configurationVersion = Configuration.VERSION_2_3_29;
        final Configuration configuration = new Configuration(configurationVersion);
        configuration.setDefaultEncoding(CHARSET);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(templateDirectory.toFile()));
        } catch (final IOException error) {
            throw new UncheckedIOException(error);
        }
        final DefaultObjectWrapperBuilder objectWrapperBuilder = new DefaultObjectWrapperBuilder(configurationVersion);
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
            final Configuration configuration,
            final String templateName,
            final Object templateData,
            final Path outputFile) {
        try {
            final Template template = configuration.getTemplate(templateName);
            final Path outputFileParent = outputFile.getParent();
            if (outputFileParent != null) {
                Files.createDirectories(outputFileParent);
            }
            try (final BufferedWriter outputFileWriter = Files.newBufferedWriter(
                    outputFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                template.process(templateData, outputFileWriter);
            }
        } catch (final Exception error) {
            final String message = String.format(
                    "failed rendering template `%s` in directory `%s` to file `%s`",
                    templateName, configuration, outputFile);
            throw new RuntimeException(message, error);
        }
    }
}
