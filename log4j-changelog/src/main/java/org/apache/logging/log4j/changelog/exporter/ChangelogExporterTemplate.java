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
package org.apache.logging.log4j.changelog.exporter;

import java.util.Objects;

import org.apache.logging.log4j.changelog.ChangelogFiles;

public final class ChangelogExporterTemplate {

    final String sourceFileName;

    final String targetFileName;

    final boolean failIfNotFound;

    public ChangelogExporterTemplate(
            final String sourceFileName,
            final String targetFileName,
            final boolean failIfNotFound) {
        this.sourceFileName = requireTemplateFileName(sourceFileName, "sourceFileName");
        this.targetFileName = Objects.requireNonNull(targetFileName, "targetFileName");
        this.failIfNotFound = failIfNotFound;
    }

    @SuppressWarnings("SameParameterValue")
    private static String requireTemplateFileName(final String fileName, final String fieldName) {
        Objects.requireNonNull(fileName, fieldName);
        final String templateFileNameSuffix = '.' + ChangelogFiles.templateFileNameExtension();
        if (!fileName.endsWith(templateFileNameSuffix)) {
            final String message = String.format(
                    "`%s` contains file name without a `%s` suffix: `%s`",
                    fieldName,
                    templateFileNameSuffix,
                    fileName);
            throw new IllegalArgumentException(message);
        }
        return fileName;
    }

    @Override
    public boolean equals(Object instance) {
        if (this == instance) {
            return true;
        }
        if (instance == null || getClass() != instance.getClass()) {
            return false;
        }
        ChangelogExporterTemplate template = (ChangelogExporterTemplate) instance;
        return failIfNotFound == template.failIfNotFound &&
                sourceFileName.equals(template.sourceFileName) &&
                targetFileName.equals(template.targetFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceFileName, targetFileName, failIfNotFound);
    }

    @Override
    public String toString() {
        return String.format(
                "`%s` → `%s`%s",
                sourceFileName,
                targetFileName,
                failIfNotFound ? " (failIfNotFound)" : "");
    }

}
