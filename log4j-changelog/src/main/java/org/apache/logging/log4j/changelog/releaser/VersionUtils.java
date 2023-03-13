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
package org.apache.logging.log4j.changelog.releaser;

import java.util.Objects;

final class VersionUtils {

    private static final String VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?$";

    private VersionUtils() {}

    static String requireSemanticVersioning(final String version, final String fieldName) {
        Objects.requireNonNull(version, fieldName);
        if (!version.matches(VERSION_PATTERN)) {
            final String message = String.format(
                    "provided version in `%s` does not match the expected `<major>.<minor>.<patch>[-SNAPSHOT]` pattern: `%s`",
                    fieldName, version);
            throw new IllegalArgumentException(message);
        }
        return version;
    }

    static int versionMajor(final String version) {
        return Integer.parseInt(version.split("\\.", 2)[0]);
    }

}
