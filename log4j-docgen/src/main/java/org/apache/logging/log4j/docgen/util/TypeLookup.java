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
package org.apache.logging.log4j.docgen.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.apache.logging.log4j.docgen.AbstractType;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.PluginType;
import org.apache.logging.log4j.docgen.ScalarType;
import org.apache.logging.log4j.docgen.Type;

public class TypeLookup extends TreeMap<String, Type> {

    private static final Predicate<PluginType> HAS_CORE_NAMESPACE = p -> "Core".equals(p.getNamespace());

    private final Map<String, PluginType> pluginTypeMap = new TreeMap<>();

    public static TypeLookup of(final Iterable<PluginSet> sets) {
        return new TypeLookup(sets);
    }

    private TypeLookup(final Iterable<PluginSet> sets) {
        final Map<String, ScalarType> scalarTypeMap = new TreeMap<>();
        final Map<String, AbstractType> abstractTypeMap = new TreeMap<>();
        // Round 1: Merge all the information from the sets
        sets.forEach(set -> {
            set.getScalars().forEach(scalar -> put(scalar.getClassName(), scalar));
            set.getAbstractTypes().forEach(abstractType -> put(abstractType.getClassName(), abstractType));
            set.getPlugins().stream().filter(HAS_CORE_NAMESPACE).forEach(plugin -> put(plugin.getClassName(), plugin));
        });
        // Round 2: fill in the set of abstract types used in elements and the list of their possible implementations
        final Set<String> requiredAbstractTypes = new HashSet<>();
        sets.forEach(set -> {
            set.getPlugins().stream().filter(HAS_CORE_NAMESPACE).forEach(plugin -> {
                plugin.getSupertypes().forEach(supertype -> ((AbstractType)
                                computeIfAbsent(supertype, TypeLookup::createAbstractType))
                        .addImplementation(plugin.getClassName()));
                plugin.getElements().forEach(element -> {
                    final String type = element.getType();
                    if (!scalarTypeMap.containsKey(type) && !pluginTypeMap.containsKey(type)) {
                        requiredAbstractTypes.add(type);
                    }
                });
            });
        });
        // Round 3: remove the types that are not required and do not have a description
        values().removeIf(
                        type -> !requiredAbstractTypes.contains(type.getClassName()) && type.getDescription() == null);
    }

    private static AbstractType createAbstractType(final String className) {
        final AbstractType abstractType = new AbstractType();
        abstractType.setClassName(className);
        return abstractType;
    }
}
