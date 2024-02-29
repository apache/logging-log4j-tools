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

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.docgen.PluginSet;
import org.apache.logging.log4j.docgen.io.stax.PluginBundleStaxReader;

final class BaseTypes {

    static final Set<PluginSet> PLUGIN_SETS = readBaseTypes();

    private static Set<PluginSet> readBaseTypes() {
        return Stream.of("base-log4j-types.xml").map(BaseTypes::readBaseTypes).collect(Collectors.toSet());
    }

    private static PluginSet readBaseTypes(final String resourceName) {
        try (final InputStream inputStream = BaseTypes.class.getResourceAsStream(resourceName)) {
            return new PluginBundleStaxReader().read(inputStream);
        } catch (final Exception error) {
            final String message = String.format("failed reading plugins from `%s` resource", resourceName);
            throw new RuntimeException(message, error);
        }
    }
}
