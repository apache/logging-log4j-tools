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
package org.apache.logging.log4j.docgen.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;
import org.jspecify.annotations.Nullable;

final class PluginSets {

    static Set<PluginSet> ofDescriptorFilesAndFileMatchers(
            @Nullable final File[] descriptorFiles, @Nullable final PathMatcherMojo[] descriptorFileMatchers) {

        // Check arguments
        final File[] effectiveDescriptorFiles = descriptorFiles != null ? descriptorFiles : new File[0];
        final PathMatcherMojo[] effectiveDescriptorFileMatchers =
                descriptorFileMatchers != null ? descriptorFileMatchers : new PathMatcherMojo[0];
        if (effectiveDescriptorFiles.length == 0 && effectiveDescriptorFileMatchers.length == 0) {
            throw new IllegalArgumentException(
                    "at least one of the `descriptorFiles` and `descriptorFileMatchers` configurations must be provided");
        }

        // Collect paths
        final Set<Path> foundDescriptorPaths = new LinkedHashSet<>();
        Arrays.stream(effectiveDescriptorFiles).map(File::toPath).forEach(foundDescriptorPaths::add);
        Arrays.stream(effectiveDescriptorFileMatchers).forEach(matcher -> matcher.findPaths(foundDescriptorPaths::add));

        // Create plugin sets
        final PluginBundleStaxReader reader = new PluginBundleStaxReader();
        return foundDescriptorPaths.stream()
                .map(descriptorFile -> {
                    try {
                        return reader.read(descriptorFile.toString());
                    } catch (final Exception error) {
                        final String message = String.format("failed reading descriptor file `%s`", descriptorFile);
                        throw new RuntimeException(message, error);
                    }
                })
                .collect(Collectors.toSet());
    }
}
