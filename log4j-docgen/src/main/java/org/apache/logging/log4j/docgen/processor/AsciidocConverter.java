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
package org.apache.logging.log4j.docgen.processor;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.ParamTree;
import com.sun.source.util.DocTrees;
import javax.lang.model.element.Element;

/**
 * Converts a {@link DocCommentTree} into AsciiDoc text.
 */
final class AsciidocConverter {

    private static final DocTreeVisitor<Void, AsciidocData> DOC_COMMENT_TREE_VISITOR = new DocCommentTreeVisitor();
    private static final DocTreeVisitor<Void, AsciidocData> PARAM_TREE_VISITOR = new ParamTreeVisitor();

    private final DocTrees docTrees;

    AsciidocConverter(final DocTrees docTrees) {
        this.docTrees = docTrees;
    }

    public String toAsciiDoc(final Element element) {
        final DocCommentTree tree = docTrees.getDocCommentTree(element);
        return tree != null ? toAsciiDoc(tree) : null;
    }

    public String toAsciiDoc(final DocCommentTree tree) {
        final AsciidocData data = new AsciidocData();
        tree.accept(DOC_COMMENT_TREE_VISITOR, data);
        return data.getDocument().convert();
    }

    public String toAsciiDoc(final ParamTree tree) {
        final AsciidocData data = new AsciidocData();
        tree.accept(PARAM_TREE_VISITOR, data);
        return data.getDocument().convert();
    }

    private static final class DocCommentTreeVisitor extends AbstractAsciidocTreeVisitor {
        @Override
        public Void visitDocComment(final DocCommentTree node, final AsciidocData data) {
            // Summary block wrapped in a new paragraph.
            for (final DocTree docTree : node.getFirstSentence()) {
                docTree.accept(this, data);
            }
            data.newParagraph();
            // Body
            for (final DocTree docTree : node.getBody()) {
                docTree.accept(this, data);
            }
            // Flushes the last paragraph
            data.newParagraph();
            return super.visitDocComment(node, data);
        }
    }

    private static final class ParamTreeVisitor extends AbstractAsciidocTreeVisitor {
        @Override
        public Void visitParam(final ParamTree node, final AsciidocData data) {
            for (final DocTree docTree : node.getDescription()) {
                docTree.accept(this, data);
            }
            // Flushes the last paragraph
            data.newParagraph();
            return super.visitParam(node, data);
        }
    }
}
