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
package org.apache.logging.log4j.docgen.generator.internal;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.apache.logging.log4j.docgen.ScalarType;
import org.apache.logging.log4j.docgen.Type;
import org.jspecify.annotations.Nullable;

public final class TypeLookup extends TreeMap<String, ArtifactSourcedType> {

    private static final long serialVersionUID = 1L;

    public static TypeLookup of(
            final Iterable<? extends PluginSet> pluginSets, final Predicate<String> classNameFilter) {
        return new TypeLookup(pluginSets, classNameFilter);
    }

    private TypeLookup(final Iterable<? extends PluginSet> pluginSets, final Predicate<String> classNameFilter) {
        mergeDescriptors(pluginSets);
        populateTypeHierarchy(pluginSets);
        filterTypes(classNameFilter);
    }

    private void mergeDescriptors(Iterable<? extends PluginSet> pluginSets) {
        pluginSets.forEach(pluginSet -> {
            mergeScalarTypes(pluginSet);
            mergeAbstractTypes(pluginSet);
            mergePluginTypes(pluginSet);
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void mergeScalarTypes(PluginSet pluginSet) {
        pluginSet.getScalars().forEach(newType -> {
            final String className = newType.getClassName();
            @Nullable final ArtifactSourcedType oldSourcedType = get(className);

            // If the entry doesn't exist yet, add it
            if (oldSourcedType == null) {
                final ArtifactSourcedType newSourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, newType);
                put(className, newSourcedType);
            }

            // If the entry already exists and is of expected type, we should ideally extend it.
            // Since Modello doesn't generate `hashCode()`, `equals()`, etc. it is difficult to compare instances.
            // Hence, we will be lazy, and just assume they are the same.
            else if (oldSourcedType.type instanceof ScalarType) {
            }

            // If the entry already exists, but with an unexpected type, fail
            else {
                conflictingTypeFailure(className, oldSourcedType.type, newType);
            }
        });
    }

    private static void conflictingTypeFailure(final String className, final Type oldType, final Type newType) {
        final String message = String.format(
                "`%s` class occurs multiple times with conflicting types: `%s` and `%s`",
                className,
                oldType.getClass().getSimpleName(),
                newType.getClass().getSimpleName());
        throw new IllegalArgumentException(message);
    }

    private void mergeAbstractTypes(PluginSet pluginSet) {
        pluginSet.getAbstractTypes().forEach(newType -> {
            final String className = newType.getClassName();
            @Nullable final ArtifactSourcedType oldSourcedType = get(className);

            // If the entry doesn't exist yet, add it
            if (oldSourcedType == null) {
                final ArtifactSourcedType newSourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, newType);
                put(className, newSourcedType);
            }

            // If the entry already exists and is of expected type, extend it
            else if (oldSourcedType.type instanceof AbstractType) {
                final AbstractType oldType = (AbstractType) oldSourcedType.type;
                newType.getImplementations().forEach(oldType::addImplementation);
            }

            // If the entry already exists, but with an unexpected type, fail
            else {
                conflictingTypeFailure(className, oldSourcedType.type, newType);
            }
        });
    }

    private void mergePluginTypes(PluginSet pluginSet) {
        pluginSet.getPlugins().forEach(newType -> {
            final String className = newType.getClassName();
            @Nullable final ArtifactSourcedType oldSourcedType = get(className);

            // If the entry doesn't exist yet, add it
            if (oldSourcedType == null) {
                final ArtifactSourcedType newSourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, newType);
                put(className, newSourcedType);
            }

            // If the entry already exists, but is of `AbstractType`, promote it to `PluginType`.
            //
            // The most prominent example to this is `LoggerConfig`, which is a plugin.
            // Assume `AsyncLoggerConfig` (extending from `LoggerConfig`) is encountered first.
            // This results in `LoggerConfig` getting registered as an `AbstractType`.
            // When the actual `LoggerConfig` definition is encountered, the type needs to be promoted to `PluginType`.
            // Otherwise, `LoggerConfig` plugin definition will get skipped.
            else if (oldSourcedType.type instanceof AbstractType && !(oldSourcedType.type instanceof PluginType)) {
                final ArtifactSourcedType newSourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, newType);
                put(className, newSourcedType);
                // Preserve old implementations
                final AbstractType oldType = (AbstractType) oldSourcedType.type;
                oldType.getImplementations().forEach(newType::addImplementation);
            }

            // If the entry already exists and is of expected type, extend it
            else if (oldSourcedType.type instanceof PluginType) {
                final PluginType oldType = (PluginType) oldSourcedType.type;
                newType.getImplementations().forEach(oldType::addImplementation);
            }

            // If the entry already exists, but with an unexpected type, fail
            else {
                conflictingTypeFailure(className, oldSourcedType.type, newType);
            }
        });
    }

    private void populateTypeHierarchy(Iterable<? extends PluginSet> pluginSets) {
        pluginSets.forEach(pluginSet -> {
            final Set<PluginType> pluginTypes = pluginSet.getPlugins();
            pluginTypes.forEach(plugin -> {
                final Set<String> superTypeClassNames = plugin.getSupertypes();
                superTypeClassNames.forEach(superTypeClassName -> {
                    final AbstractType sourcedSuperType = getOrPutAbstractType(superTypeClassName, pluginSet);
                    sourcedSuperType.addImplementation(plugin.getClassName());
                });
            });
        });
    }

    /**
     * Gets the {@link AbstractType} associated with the provided class name, otherwise puts a new one sourced using the provided {@link PluginSet}.
     * <h1>Invocation order matters!</h1>
     * <p>
     * Consider we are processing {@code plugins.xml}s from following artifacts:
     * </p>
     * <ol>
     * <li>{@code log4j-core} providing {@code Layout}</li>
     * <li>{@code log4j-layout-template-json} providing {@code JsonTemplateLayout} extending from {@code Layout}
     * </ol>
     * <p>
     * If {@code plugins.xml} of {@code log4j-layout-template-json} gets processed first, {@code Layout} type will be <strong>incorrectly</strong> associated with {@code log4j-layout-template-json}.
     * Hence, {@code log4j-core} processing needs to happen before the {@code log4j-layout-template-json} processing.
     * </p>
     *
     * @param className a class name
     * @param pluginSet the descriptor sourcing the type
     * @return either the existing {@link AbstractType} associated with the provided class name, or a new one created
     */
    @Nullable
    private AbstractType getOrPutAbstractType(final String className, final PluginSet pluginSet) {

        // If there is already an entry with the same class name, return that
        @Nullable final ArtifactSourcedType foundSourcedType = get(className);
        if (foundSourcedType != null) {
            return (AbstractType) foundSourcedType.type;
        }

        // Otherwise, create a new one, and return that
        else {
            final AbstractType type = new AbstractType();
            type.setClassName(className);
            final ArtifactSourcedType sourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, type);
            put(className, sourcedType);
            return type;
        }
    }

    private void filterTypes(final Predicate<String> classNameMatcher) {
        final Iterator<String> classNameIterator = keySet().iterator();
        while (classNameIterator.hasNext()) {
            final String className = classNameIterator.next();
            final boolean matching = classNameMatcher.test(className);
            if (!matching) {
                classNameIterator.remove();
            }
        }
    }
}
