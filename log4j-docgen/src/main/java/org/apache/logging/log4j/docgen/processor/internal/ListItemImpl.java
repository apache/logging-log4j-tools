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

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.ListItem;

public final class ListItemImpl extends StructuralNodeImpl implements ListItem {

    public ListItemImpl(final ContentNode parent) {
        super(parent);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        formatNodeCollection(getBlocks(), "+\n", buffer);
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public String getMarker() {
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
    public boolean hasText() {
        throw new UnsupportedOperationException();
    }
}
