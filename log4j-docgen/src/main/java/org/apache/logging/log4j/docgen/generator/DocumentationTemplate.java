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
package org.apache.logging.log4j.docgen.generator;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public final class DocumentationTemplate {

    final String name;

    final String targetPath;

    public DocumentationTemplate(final String name, final String targetPath) {
        this.name = requireNonNull(name, "name");
        this.targetPath = requireNonNull(targetPath, "targetPath");
    }

    @Override
    public boolean equals(Object instance) {
        if (this == instance) {
            return true;
        }
        if (instance == null || getClass() != instance.getClass()) {
            return false;
        }
        DocumentationTemplate that = (DocumentationTemplate) instance;
        return Objects.equals(name, that.name) && Objects.equals(targetPath, that.targetPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, targetPath);
    }
}
