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

import java.util.Objects;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.List;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.StructuralNode;

public class ListImpl extends StructuralNodeImpl implements List {

    public static final String ORDERED_LIST_CONTEXT = "olist";
    private static final char ORDERED_LIST_MARKER = '.';
    public static final String UNORDERED_LIST_CONTEXT = "ulist";
    private static final char UNORDERED_LIST_MARKER = '*';

    public ListImpl(final ContentNode parent) {
        super(parent);
        setContext(ORDERED_LIST_CONTEXT);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        final String prefix = computeItemPrefix();
        getBlocks().forEach(node -> {
            buffer.append(prefix);
            if (node instanceof final StringBuilderFormattable formattable) {
                formattable.formatTo(buffer);
            } else {
                buffer.append(node.convert());
            }
        });
    }

    /**
     * Computes the appropriate prefix for the list.
     */
    private String computeItemPrefix() {
        final StringBuilder sb = new StringBuilder();
        ContentNode currentNode = this;
        while (currentNode instanceof List) {
            // If the type of list changes stop.
            if (!Objects.equals(getContext(), currentNode.getContext())) {
                break;
            }
            sb.append(
                    ORDERED_LIST_CONTEXT.equals(currentNode.getContext())
                            ? ORDERED_LIST_MARKER
                            : UNORDERED_LIST_MARKER);
            currentNode = currentNode.getParent();
            if (!(currentNode instanceof ListItem)) {
                break;
            }
            currentNode = currentNode.getParent();
        }
        return sb.reverse().append(' ').toString();
    }

    @Override
    public java.util.List<StructuralNode> getItems() {
        return getBlocks();
    }

    @Override
    public boolean hasItems() {
        return !getBlocks().isEmpty();
    }

    @Override
    public void setContext(final String context) {
        switch (context) {
            case ORDERED_LIST_CONTEXT:
            case UNORDERED_LIST_CONTEXT:
                break;
            default:
                throw new RuntimeException("Unknown list context " + context);
        }
        super.setContext(context);
    }
}
