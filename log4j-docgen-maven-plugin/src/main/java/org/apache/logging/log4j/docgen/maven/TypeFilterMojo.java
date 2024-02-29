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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/**
 * Models a predicate for filtering types that a generator will operate on.
 */
public final class TypeFilterMojo {

    /**
     * The regular expressions that will be matched against the class name (e.g., {@code java.lang.String}) to determine the types that will be <em>included</em> for processing by the generator.
     * <p>
     * The type will be included if it matches one or more patterns.
     * </p><p>
     * The collection defaults to {@code [".*"]}, that is, all types are accepted.
     * </p>
     */
    @Nullable
    @Parameter
    private String[] includes;

    /**
     * The regular expressions that will be matched against the class name (e.g., {@code java.lang.String}) to determine the types that will be <em>excluded</em> for processing the generator.
     * <p>
     * The type will be excluded if it matches one or more patterns.
     * </p><p>
     * The collection is empty by default, that is, no types will be discarded.
     * </p>
     */
    @Nullable
    @Parameter
    private String[] excludes;

    Predicate<String> createPredicate() {
        final String[] effectiveIncludes = includes != null && includes.length > 0 ? includes : new String[] {".*"};
        final List<Pattern> inclusionPatterns = compilePatterns("includes", effectiveIncludes);
        final List<Pattern> exclusionPatterns = compilePatterns("excludes", excludes);
        return (final String className) -> {
            final boolean included = inclusionPatterns.stream()
                    .anyMatch(pattern -> pattern.matcher(className).matches());
            if (included) {
                final boolean excluded = exclusionPatterns.stream()
                        .anyMatch(pattern -> pattern.matcher(className).matches());
                return !excluded;
            }
            return false;
        };
    }

    private static List<Pattern> compilePatterns(final String fieldName, @Nullable final String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(patterns)
                .map(pattern -> {
                    try {
                        return Pattern.compile(pattern);
                    } catch (final Exception error) {
                        final String message = String.format("invalid `%s` pattern: `%s`", fieldName, pattern);
                        throw new IllegalArgumentException(message, error);
                    }
                })
                .collect(Collectors.toList());
    }
}
