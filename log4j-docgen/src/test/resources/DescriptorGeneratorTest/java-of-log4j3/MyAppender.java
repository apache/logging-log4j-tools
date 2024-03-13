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
import javax.lang.model.element.TypeElement;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

/**
 * Example plugin
 * <p>
 *     This is an example plugin. It has the following characteristics:
 * </p>
 * <ol>
 *     <li>Plugin name: {@code MyPlugin},</li>
 *     <li>Namespace: default (i.e. {@code Core}).</li>
 * </ol>
 * <p>
 *     It also implements:
 * </p>
 * <ul>
 *     <li>{@link Appender},</li>
 *     <li>{@link BaseAppender}</li>
 * </ul>
 */
@Plugin
@Namespace("namespace")
public final class MyAppender extends AbstractAppender implements Appender {

    /**
     * Parent builder with some private fields that are not returned by
     * {@link javax.lang.model.util.Elements#getAllMembers(TypeElement)}.
     */
    public static class ParentBuilder {

        /**
         * A {@code char} attribute.
         */
        @PluginBuilderAttribute
        private char charAtt = 'L';

        /**
         * An {@code int} attribute.
         */
        @PluginBuilderAttribute
        private int intAtt = 4242;

        /**
         * An element with multiplicity 1.
         */
        @PluginElement
        private Layout layout;
    }

    public static final class Builder extends ParentBuilder
            implements org.apache.logging.log4j.plugins.util.Builder<MyAppender> {

        /**
         * A {@code short} attribute annotated on type.
         */
        private @PluginBuilderAttribute short shortAtt = 42;

        /**
         * A {@code long} attribute annotated on type.
         */
        private @PluginBuilderAttribute long longAtt = 424242L;

        /**
         * A {@code String} attribute.
         */
        @PluginBuilderAttribute
        @Required
        private String stringAtt;

        /**
         * An attribute whose name differs from the field name.
         */
        @PluginBuilderAttribute("anotherName")
        private String origName;

        /**
         * An attribute that is an enumeration annotated on type.
         */
        private @PluginBuilderAttribute MyEnum enumAtt;

        /**
         * An attribute of type {@code float}.
         */
        private @PluginBuilderAttribute float floatAtt;

        /**
         * An attribute of type {@code double}.
         */
        private @PluginBuilderAttribute double aDouble;

        private @PluginBuilderAttribute double undocumentedAttribute;

        private Object notAnAttribute;

        /**
         * A collection element.
         */
        @PluginElement
        private List<Appender2> appenderList;

        /**
         * A set of layouts
         */
        @PluginElement
        private LayoutSet layoutSet;

        /**
         * A {@code boolean} attribute with annotated type.
         */
        public Builder setBooleanAtt(final @PluginBuilderAttribute boolean booleanAtt) {
            return this;
        }

        /**
         * A {@code byte} attribute with annotated parameter.
         */
        public Builder setByteAtt(@PluginBuilderAttribute final byte byteAtt) {
            return this;
        }

        /**
         * An element with multiplicity n with annotated setter.
         */
        @PluginElement
        public Builder setFilters(final Filter[] filters) {
            return this;
        }

        /**
         * An element that is not an interface with annotated parameter.
         */
        public Builder setAbstractElement(@PluginElement final AbstractAppender abstractAppender) {
            return this;
        }

        /**
         * An element with an annotated type.
         */
        public Builder setNestedAppender(final @PluginElement Appender nestedAppender) {
            return this;
        }

        /**
         * A setter with a varargs type.
         */
        public Builder setVarargs(@PluginElement final Layout3... layouts) {
            return this;
        }

        @Override
        public MyAppender build() {
            return null;
        }
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * This method is only here to test that this <em>deprecated</em> factory will be ignored.
     *
     * @param boolAttr A {@code boolean} attribute.
     * @param byteAttr A {@code byte} attribute.
     * @param charAttr A {@code char} attribute.
     */
    @Factory
    @Deprecated
    public static MyOldLayout newAppender(
            final @PluginAttribute(defaultBoolean = false) boolean boolAttr,
            final @PluginAttribute(defaultByte = 'L') byte byteAttr,
            final @PluginAttribute(defaultChar = 'L') char charAttr) {
        return null;
    }

    public static interface Appender2 {}

    public static interface Layout2 {}

    public static interface Layout3 {}

    public abstract static class LayoutSet implements Set<Layout2> {}
}
