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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

public final class FileUtils {

    private FileUtils() {}

    /**
     * Finds files non-recursively in the given directory.
     * <p>
     * Given directory itself and hidden files are filtered out.
     * </p>
     */
    @SuppressWarnings("RedundantIfStatement")
    public static <V> V findAdjacentFiles(
            final Path directory,
            final boolean dotFilesSkipped,
            final Function<Stream<Path>, V> consumer) {
        try (final Stream<Path> paths = Files.walk(directory, 1)) {
            final Stream<Path> filteredPaths = paths.filter(path -> {

                // Skip the directory itself
                if (path.equals(directory)) {
                    return false;
                }

                // Skip hidden files
                boolean hiddenFile = dotFilesSkipped && path.getFileName().toString().startsWith(".");
                if (hiddenFile) {
                    return false;
                }

                // Accept the rest
                return true;

            });
            return consumer.apply(filteredPaths);
        } catch (final IOException error) {
            final String message = String.format("failed walking directory: `%s`", directory);
            throw new UncheckedIOException(message, error);
        }
    }

}
