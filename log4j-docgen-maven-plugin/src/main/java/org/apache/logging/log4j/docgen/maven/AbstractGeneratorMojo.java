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

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jspecify.annotations.Nullable;

abstract class AbstractGeneratorMojo extends AbstractMojo {

    /**
     * The paths of the plugin descriptor XML files.
     * <p>
     * If you want to provide to a multitude of files, you might want to use {@link #descriptorFileMatchers} instead.
     * </p>
     */
    @Nullable
    @Parameter(property = "log4j.docgen.descriptorFiles")
    File[] descriptorFiles;

    /**
     * The {@link java.nio.file.FileSystem#getPathMatcher(String) PathMatcher}s to populate the paths of the plugin descriptor XML files.
     * <p>
     * If you want to refer to a particular file, you might want to use {@link #descriptorFiles} instead.
     * </p>
     */
    @Nullable
    @Parameter
    PathMatcherMojo[] descriptorFileMatchers;

    /**
     * Predicate for filtering types that the generator will operate on.
     */
    @Nullable
    @Parameter
    TypeFilterMojo typeFilter;
}
