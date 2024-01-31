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
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;

public abstract class ContentNodeImpl implements ContentNode {

    private final ContentNode parent;
    private String context;

    protected ContentNodeImpl(final ContentNode parent) {
        this.parent = parent;
    }

    @Override
    public ContentNode getParent() {
        return parent;
    }

    @Override
    public String getContext() {
        return context;
    }

    @Override
    public void setContext(final String context) {
        this.context = context;
    }

    @Override
    public Document getDocument() {
        return parent != null ? parent.getDocument() : null;
    }

    //
    // All methods below this line are not implemented.
    //
    @Override
    public String getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setId(final String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNodeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(final Object name, final Object defaultValue, final boolean inherit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(final Object name, final Object defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(final Object name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAttribute(final Object name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAttribute(final Object name, final boolean inherited) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAttribute(final Object name, final Object expected) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAttribute(final Object name, final Object expected, final boolean inherit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setAttribute(final Object name, final Object value, final boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOption(final Object name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRole() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasRole(final String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRole() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getRoles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRole(final String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRole(final String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReftext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReftext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String iconUri(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String mediaUri(final String target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String imageUri(final String targetImage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String imageUri(final String targetImage, final String assetDirKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readAsset(final String path, final Map<Object, Object> opts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String normalizeWebPath(final String path, final String start, final boolean preserveUriTarget) {
        throw new UnsupportedOperationException();
    }
}
