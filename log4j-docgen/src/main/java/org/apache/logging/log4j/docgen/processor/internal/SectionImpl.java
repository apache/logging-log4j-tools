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
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;

public class SectionImpl extends StructuralNodeImpl implements Section {

    private final List<StructuralNode> content = new ArrayList<>();

    public SectionImpl(final ContentNode parent) {
        super(parent);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        final String title = getTitle();
        if (title != null) {
            buffer.append("=".repeat(computeSectionLevel(this)))
                    .append(' ')
                    .append(title)
                    .append("\n\n");
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

    private static int computeSectionLevel(final StructuralNode section) {
        int level = 0;
        StructuralNode currentNode = section;
        while (currentNode != null) {
            if (currentNode instanceof Section || currentNode instanceof Document) {
                level++;
            }
            currentNode = (StructuralNode) currentNode.getParent();
        }
        return level;
    }

    @Override
    public String getSectionName() {
        return getTitle();
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public int getIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNumeral() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSpecial() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNumbered() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSectnum() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSectnum(final String delimiter) {
        throw new UnsupportedOperationException();
    }
}
