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
package org.apache.logging.log4j.tools;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.logging.log4j.tools.StringUtils.isBlank;

public final class PropertyUtils {

    private PropertyUtils() {}

    public static Path requireNonBlankPathProperty(final String key) {
        final String value = requireNonBlankStringProperty(key);
        try {
            return Paths.get(value);
        } catch (final InvalidPathException error) {
            final String message = String.format("system property `%s` doesn't contain a valid path: `%s`", key, value);
            throw new IllegalArgumentException(message, error);
        }
    }

    public static int requireNonBlankIntProperty(final String key) {
        final String value = requireNonBlankStringProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException error) {
            final String message = String.format("system property `%s` doesn't contain a valid integer: `%s`", key, value);
            throw new IllegalArgumentException(message, error);
        }
    }

    public static String requireNonBlankStringProperty(final String key) {
        final String value = System.getProperty(key);
        if (isBlank(value)) {
            final String message = String.format("blank system property: `%s`", key);
            throw new IllegalArgumentException(message);
        }
        return value;
    }

}
