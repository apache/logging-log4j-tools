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
package example;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

/**
 * Example plugin without a builder.
 */
@Plugin
public final class MyOldLayout implements Layout {

    /**
     * @param boolAttr A {@code boolean} attribute.
     * @param byteAttr A {@code byte} attribute.
     * @param charAttr A {@code char} attribute.
     * @param doubleAttr A {@code double} attribute.
     * @param floatAttr A {@code float} attribute.
     * @param intAttr An {@code int} attribute.
     * @param longAttr A {@code long} attribute.
     * @param shortAttr A {@code short} attribute.
     * @param stringAttr A {@link String} attribute.
     * @param origName An attribute with overwritten name.
     * @param enumAttr An {@code enum} attribute.
     * @param nestedLayout An element with multiplicity {@code 1}.
     * @param filters An element with multiplicity {@code n}.
     */
    @Factory
    public static MyOldLayout newLayout(
            final @PluginAttribute(defaultBoolean = false) boolean boolAttr,
            final @PluginAttribute(defaultByte = 'L') byte byteAttr,
            final @PluginAttribute(defaultChar = 'L') char charAttr,
            final @PluginAttribute(defaultDouble = 42.0) double doubleAttr,
            final @PluginAttribute(defaultFloat = 42.0f) float floatAttr,
            final @PluginAttribute(defaultInt = 424242) int intAttr,
            final @PluginAttribute(defaultLong = 42424242L) long longAttr,
            final @PluginAttribute(defaultShort = 4242) short shortAttr,
            final @PluginAttribute @Required String stringAttr,
            final @PluginAttribute("otherName") String origName,
            final @PluginAttribute MyEnum enumAttr,
            final @PluginElement Layout nestedLayout,
            final @PluginElement Filter[] filters) {
        return null;
    }
}
