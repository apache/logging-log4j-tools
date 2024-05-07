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

import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;

/**
 * Example plugin to demonstrate the case where a plugin subclasses another plugin.
 *
 * @see <a href="https://github.com/apache/logging-log4j-tools/issues/117">apache/logging-log4j-tools#117</a>
 */
@Plugin
@Namespace("namespace")
public final class MyAppenderSubclassingAppender extends MyAppender {

    /**
     * The canonical constructor.
     */
    @Factory
    public static MyAppenderSubclassingAppender newLayout(
            final @PluginAttribute(value = "awesomenessEnabled", defaultBoolean = true) boolean awesomenessEnabled) {
        return null;
    }
}
