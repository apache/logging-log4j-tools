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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.jspecify.annotations.Nullable;

final class TypeLookup extends TreeMap<String, ArtifactSourcedType> {

    private static final long serialVersionUID = 1L;

    static TypeLookup of(final Iterable<? extends PluginSet> pluginSets, final Predicate<String> classNameFilter) {
        return new TypeLookup(pluginSets, classNameFilter);
    }

    private TypeLookup(final Iterable<? extends PluginSet> pluginSets, final Predicate<String> classNameFilter) {
        mergeDescriptors(pluginSets);
        populateTypeHierarchy(pluginSets);
        filterTypes(classNameFilter);
    }

    private void mergeDescriptors(Iterable<? extends PluginSet> pluginSets) {
        pluginSets.forEach(pluginSet -> {
            pluginSet.getScalars().forEach(scalar -> {
                final ArtifactSourcedType sourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, scalar);
                putIfAbsent(scalar.getClassName(), sourcedType);
            });
            pluginSet.getAbstractTypes().forEach(abstractType -> {
                final ArtifactSourcedType sourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, abstractType);
                putIfAbsent(abstractType.getClassName(), sourcedType);
            });
            pluginSet.getPlugins().forEach(pluginType -> {
                final ArtifactSourcedType sourcedType = ArtifactSourcedType.ofPluginSet(pluginSet, pluginType);
                putIfAbsent(pluginType.getClassName(), sourcedType);
            });
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
