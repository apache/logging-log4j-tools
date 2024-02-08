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

import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Column;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Table;

public class CellImpl extends StructuralNodeImpl implements Cell {

    public static final String HEADER_CONTEXT = "header";
    public static final String BODY_CONTEXT = "body";

    public CellImpl(final ContentNode parent) {
        super(parent);
        setContext(BODY_CONTEXT);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (getBlocks().size() > 1) {
            buffer.append('a');
        } else if (HEADER_CONTEXT.equals(getContext())) {
            buffer.append('h');
        }
        buffer.append("| ");
        getBlocks().forEach(node -> {
            if (node instanceof final StringBuilderFormattable formattable) {
                formattable.formatTo(buffer);
            } else {
                buffer.append(node.convert());
            }
        });
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public Column getColumn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getColspan() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRowspan() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getText() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSource(final String source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Table.HorizontalAlignment getHorizontalAlignment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHorizontalAlignment(final Table.HorizontalAlignment halign) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Table.VerticalAlignment getVerticalAlignment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVerticalAlignment(final Table.VerticalAlignment valign) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getInnerDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInnerDocument(final Document document) {
        throw new UnsupportedOperationException();
    }
}
