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
package org.apache.logging.log4j;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * The template file definition.
 */
public final class TemplateMojo {

    /**
     * The source template file name, e.g., {@code .index.adoc.ftl}, {@code .release-notes.adoc.ftl}.
     */
    @Parameter(required = true)
    String source;

    /**
     * The target file name to export, e.g., {@code index.adoc}, {@code release-notes.adoc}, or, if the file is in a release changelog directory, {@code %v.adoc}, where {@code %v} will be replaced with the release version.
     * <p>
     * If not provided, the {@code source} will be used after dropping the dot prefix and {@code .ftl} suffix.
     * That is, for source {@code .index.adoc.ftl}, {@code index.adoc} target will be used.
     * </p>
     *
     */
    @Parameter
    String target;

    /**
     * Indicates if export should fail when the source cannot be found.
     */
    @Parameter(defaultValue = "false")
    boolean failIfNotFound;
}
