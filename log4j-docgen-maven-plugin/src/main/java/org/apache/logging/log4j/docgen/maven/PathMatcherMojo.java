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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugins.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/**
 * Models a {@link java.nio.file.FileSystem#getPathMatcher(String) PathMatcher}-based path provider.
 */
public final class PathMatcherMojo {

    /**
     * The file path of the directory which will be scanned for matches.
     */
    @Parameter(required = true)
    private File baseDirectory;

    /**
     * Indicates if dot-prefixed paths found in {@link #baseDirectory} should be excluded.
     */
    @Parameter(defaultValue = "true")
    private boolean dotFilesExcluded = true;

    /**
     * The path patterns (e.g., <code>glob:**&#47;*.xml</code>) that will be used to filter paths found in {@link #baseDirectory}.
     * @see FileSystem#getPathMatcher(String)
     */
    @Nullable
    @Parameter
    private String[] pathPatterns;

    void findPaths(final Consumer<Path> pathConsumer) {

        // Create path matchers
        final String[] effectivePathPatterns = pathPatterns != null ? pathPatterns : new String[0];
        final FileSystem fileSystem = FileSystems.getDefault();
        final Set<PathMatcher> pathMatchers = Arrays.stream(effectivePathPatterns)
                .map(pathPattern -> {
                    try {
                        return fileSystem.getPathMatcher(pathPattern);
                    } catch (final Exception error) {
                        final String message =
                                String.format("failed to create matcher using path pattern `%s`", pathPattern);
                        throw new IllegalArgumentException(message, error);
                    }
                })
                .collect(Collectors.toSet());

        // Walk the base directory
        try (final Stream<Path> paths = Files.walk(baseDirectory.toPath())) {
            paths.filter(path -> {

                        // Skip directories
                        if (Files.isDirectory(path)) {
                            return false;
                        }

                        // Skip dot files
                        final boolean dotFile = dotFilesExcluded
                                && path.getFileName().toString().startsWith(".");
                        if (dotFile) {
                            return false;
                        }

                        // Check against path matchers
                        return pathMatchers.isEmpty()
                                || pathMatchers.stream().anyMatch(pathMatcher -> pathMatcher.matches(path));
                    })
                    .forEach(pathConsumer);
        } catch (final IOException error) {
            final String message = String.format("failed walking directory: `%s`", baseDirectory);
            throw new UncheckedIOException(message, error);
        }
    }
}
