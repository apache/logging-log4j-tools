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
package org.apache.logging.log4j.docgen.processor.internal;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.asciidoctor.ast.Author;
import org.asciidoctor.ast.Catalog;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.RevisionInfo;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Title;

public final class DocumentImpl extends StructuralNodeImpl implements Document {

    public DocumentImpl() {
        super(null);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (!StringUtils.isBlank(getTitle())) {
            buffer.append("= ").append(getTitle()).append("\n\n");
        }
        boolean first = true;
        for (final StructuralNode node : getBlocks()) {
            if (!first) {
                buffer.append('\n');
            } else {
                first = false;
            }
            if (node instanceof final StringBuilderFormattable formattable) {
                formattable.formatTo(buffer);
            } else {
                buffer.append(node.convert());
            }
        }
    }

    @Override
    public String getDoctitle() {
        return getTitle();
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public Title getStructuredDoctitle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Author> getAuthors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getSourceLines() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBasebackend(final String backend) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Object, Object> getOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getAndIncrementCounter(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getAndIncrementCounter(final String name, final int initialValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSourcemap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourcemap(final boolean state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Catalog getCatalog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RevisionInfo getRevisionInfo() {
        throw new UnsupportedOperationException();
    }
}
