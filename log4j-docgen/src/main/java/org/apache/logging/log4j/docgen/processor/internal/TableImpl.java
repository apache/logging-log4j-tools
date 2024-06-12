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
import org.asciidoctor.ast.Column;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.Table;

public final class TableImpl extends StructuralNodeImpl implements Table {

    private final List<Row> body = new ArrayList<>();

    public TableImpl(final ContentNode parent) {
        super(parent);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        final int colCount = body.isEmpty() ? 1 : body.get(0).getCells().size();

        final String title = getTitle();
        if (title != null) {
            buffer.append(".").append(title).append("\n");
        }
        buffer.append("[cols=\"");
        formatColSpecifier(colCount, buffer);
        buffer.append("\"]\n").append("|===\n");
        getBody().forEach(row -> formatRow(row, buffer));
        buffer.append("\n|===\n");
    }

    private static void formatColSpecifier(final int colCount, final StringBuilder buffer) {
        if (colCount > 0) {
            buffer.append("1");
        }
        for (int i = 1; i < colCount; i++) {
            buffer.append(",1");
        }
    }

    private static void formatRow(final Row row, final StringBuilder buffer) {
        buffer.append('\n');
        formatNodeCollection(row.getCells(), "", buffer);
    }

    @Override
    public List<Row> getBody() {
        return body;
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public List<Row> getHeader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Row> getFooter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasHeaderOption() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Column> getColumns() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFrame() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFrame(final String frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGrid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGrid(final String grid) {
        throw new UnsupportedOperationException();
    }
}
