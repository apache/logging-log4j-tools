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
package org.apache.logging.log4j.changelog;

import static org.apache.logging.log4j.tools.internal.test.utils.FileTestUtils.assertDirectoryContentMatches;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.changelog.importer.MavenChangesImporter;
import org.apache.logging.log4j.changelog.importer.MavenChangesImporterArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class MavenChangesImporterTest {

    @Test
    void output_should_match(@TempDir(cleanup = CleanupMode.ON_SUCCESS) final Path changelogDirectory) {
        final MavenChangesImporterArgs args =
                new MavenChangesImporterArgs(changelogDirectory, Paths.get("src/test/resources/1-changes.xml"), 2);
        MavenChangesImporter.performImport(args);
        assertDirectoryContentMatches(changelogDirectory, Paths.get("src/test/resources/2-imported"));
    }
}
