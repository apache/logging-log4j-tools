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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Cursor;
import org.asciidoctor.ast.StructuralNode;

public abstract class StructuralNodeImpl extends ContentNodeImpl implements StructuralNode {

    private int level;
    private String title;
    private String style;

    private final List<StructuralNode> blocks = new ArrayList<>();

    public StructuralNodeImpl(final ContentNode parent) {
        super(parent);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public String getStyle() {
        return style;
    }

    @Override
    public void setStyle(final String style) {
        this.style = style;
    }

    @Override
    public List<StructuralNode> getBlocks() {
        return blocks;
    }

    @Override
    public void append(final StructuralNode structuralNode) {
        blocks.add(structuralNode);
    }

    @Override
    public final String convert() {
        final StringBuilder sb = new StringBuilder();
        formatTo(sb);
        return sb.toString();
    }

    protected abstract void formatTo(final StringBuilder buffer);

    protected static void formatNode(final StructuralNode node, final StringBuilder buffer) {
        if (node instanceof final StructuralNodeImpl impl) {
            impl.formatTo(buffer);
        } else {
            buffer.append(node.convert());
        }
    }

    protected static void formatNodeCollection(
            final Iterable<? extends StructuralNode> nodes, final String separator, final StringBuilder buffer) {
        boolean first = true;
        for (final StructuralNode node : nodes) {
            if (!first) {
                buffer.append(separator);
            } else {
                first = false;
            }
            formatNode(node, buffer);
        }
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(final int level) {
        this.level = level;
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public List<StructuralNode> findBy(final Map<Object, Object> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCaption() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCaption(final String caption) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentModel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor getSourceLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getSubstitutions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSubstitutionEnabled(final String substitutionEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSubstitution(final String substitution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSubstitution(final String substitution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prependSubstitution(final String substitution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSubstitutions(final String... substitutions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getContent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInline() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBlock() {
        throw new UnsupportedOperationException();
    }
}
