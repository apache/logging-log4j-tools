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

import java.util.Comparator;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.Type;
import org.jspecify.annotations.Nullable;

public final class ArtifactSourcedType implements Comparable<ArtifactSourcedType> {

    private static final Comparator<ArtifactSourcedType> COMPARATOR = Comparator.<ArtifactSourcedType, String>comparing(
                    sourcedType -> sourcedType.type.getClassName())
            .thenComparing(sourcedType -> na(sourcedType.groupId))
            .thenComparing(sourcedType -> na(sourcedType.artifactId))
            .thenComparing(sourcedType -> na(sourcedType.version));

    private static String na(@Nullable final String value) {
        return value != null ? value : "N/A";
    }

    @Nullable
    public final String groupId;

    @Nullable
    public final String artifactId;

    @Nullable
    public final String version;

    public final Type type;

    private ArtifactSourcedType(
            @Nullable final String groupId,
            @Nullable final String artifactId,
            @Nullable final String version,
            final Type type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = requireNonNull(type, "type");
    }

    static ArtifactSourcedType ofPluginSet(final PluginSet pluginSet, final Type type) {
        return new ArtifactSourcedType(pluginSet.getGroupId(), pluginSet.getArtifactId(), pluginSet.getVersion(), type);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int compareTo(final ArtifactSourcedType sourcedType) {
        return COMPARATOR.compare(this, sourcedType);
    }
}
