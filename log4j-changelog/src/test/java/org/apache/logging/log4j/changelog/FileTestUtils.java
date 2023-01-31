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
package org.apache.logging.log4j.changelog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

final class FileTestUtils {

    private FileTestUtils() {}

    static void assertDirectoryContentMatches(final Path actualPath, final Path expectedPath) {

        // Compare file paths
        final Map<String, Path> actualContents = directoryContents(actualPath);
        final Map<String, Path> expectedContents = directoryContents(expectedPath);
        final Set<String> relativeFilePaths = expectedContents.keySet();
        assertThat(actualContents).containsOnlyKeys(relativeFilePaths);

        // Compare file contents
        relativeFilePaths.forEach(relativeFilePath -> {
            final Path actualFilePath = actualContents.get(relativeFilePath);
            final Path expectedFilePath = expectedContents.get(relativeFilePath);
            if (!Files.isDirectory(actualFilePath) || !Files.isDirectory(expectedFilePath)) {
                assertThat(actualFilePath).hasSameTextualContentAs(expectedFilePath);
            }
        });

    }

    private static Map<String, Path> directoryContents(final Path root) {
        final int rootPathLength = root.toAbsolutePath().toString().length();
        try (final Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(path -> !root.equals(path))
                    .collect(Collectors.toMap(
                            path -> path.toAbsolutePath().toString().substring(rootPathLength + 1),
                            Function.identity(),
                            (oldPath, newPath) -> {
                                final String message = String.format(
                                        "paths `%s` and `%s` have conflicting keys",
                                        oldPath, newPath);
                                throw new IllegalStateException(message);
                            },
                            TreeMap::new));
        } catch (final IOException error) {
            final String message = String.format("failed walking directory: `%s`", root);
            throw new UncheckedIOException(message, error);
        }
    }

}
