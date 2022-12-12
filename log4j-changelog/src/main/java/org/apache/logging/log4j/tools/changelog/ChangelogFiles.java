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
package org.apache.logging.log4j.tools.changelog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ChangelogFiles {

    private ChangelogFiles() {}

    public static Path unreleasedDirectory(final Path changelogDirectory, final int versionMajor) {
        final String filename = String.format(".%d.x.x", versionMajor);
        return changelogDirectory.resolve(filename);
    }

    public static Set<Integer> unreleasedDirectoryVersionMajors(final Path changelogDirectory) {
        try {
            return Files
                    .walk(changelogDirectory, 1)
                    .flatMap(path -> {

                        // Skip the directory itself.
                        if (path.equals(changelogDirectory)) {
                            return Stream.empty();
                        }

                        // Only select directories matching with the `^\.(\d+)\.x\.x$` pattern.
                        final Pattern versionPattern = Pattern.compile("^\\.(\\d+)\\.x\\.x$");
                        final Matcher versionMatcher = versionPattern.matcher(path.getFileName().toString());
                        if (!versionMatcher.matches()) {
                            return Stream.empty();
                        }
                        final String versionMajorString = versionMatcher.group(1);
                        final int versionMajor = Integer.parseInt(versionMajorString);
                        return Stream.of(versionMajor);

                    })
                    .collect(Collectors.toSet());
        } catch (final IOException error) {
            final String message = String.format("failed walking directory: `%s`", changelogDirectory);
            throw new UncheckedIOException(message, error);
        }
    }

    public static Path releaseDirectory(final Path changelogDirectory, final String releaseVersion) {
        return changelogDirectory.resolve(releaseVersion);
    }

    public static Path releaseXmlFile(final Path releaseDirectory) {
        return releaseDirectory.resolve(".release.xml");
    }

    public static Path introAsciiDocFile(final Path releaseDirectory) {
        return releaseDirectory.resolve(".intro.adoc");
    }

}
