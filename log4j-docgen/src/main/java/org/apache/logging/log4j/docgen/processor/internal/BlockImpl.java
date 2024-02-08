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
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentNode;

public class BlockImpl extends StructuralNodeImpl implements Block {

    public static final String PARAGRAPH_CONTEXT = "paragraph";
    public static final String LISTING_CONTEXT = "listing";

    private List<String> lines = new ArrayList<>();

    public BlockImpl(final ContentNode parent) {
        super(parent);
        setContext(PARAGRAPH_CONTEXT);
    }

    @Override
    protected void formatTo(final StringBuilder buffer) {
        if (getStyle() != null) {
            buffer.append('[').append(getStyle()).append("]\n");
        }
        if (LISTING_CONTEXT.equals(getContext())) {
            buffer.append("----\n");
        }
        lines.forEach(line -> buffer.append(line).append('\n'));
        if (LISTING_CONTEXT.equals(getContext())) {
            buffer.append("----\n");
        }
    }

    @Override
    public List<String> getLines() {
        return lines;
    }

    @Override
    public void setLines(final List<String> lines) {
        this.lines = lines;
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public String getSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSource(final String source) {
        throw new UnsupportedOperationException();
    }
}
