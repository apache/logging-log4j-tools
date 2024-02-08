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

import static org.apache.commons.lang3.StringUtils.substringBefore;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.docgen.processor.internal.BlockImpl;
import org.apache.logging.log4j.docgen.processor.internal.CellImpl;
import org.apache.logging.log4j.docgen.processor.internal.ListImpl;
import org.apache.logging.log4j.docgen.processor.internal.ListItemImpl;
import org.apache.logging.log4j.docgen.processor.internal.RowImpl;
import org.apache.logging.log4j.docgen.processor.internal.TableImpl;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.List;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.Section;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;

abstract class AbstractAsciidocTreeVisitor extends SimpleDocTreeVisitor<Void, AsciidocData> {

    private static final String SOURCE_STYLE = "source";
    // These are not supported by AsciiDoctor and are only used internally
    private static final String CODE_STYLE = "code";
    private static final String CODE_DELIM = "`";
    private static final String EMPHASIS_STYLE = "em";
    private static final String EMPHASIS_DELIM = "_";
    private static final String STRONG_EMPHASIS_STYLE = "strong";
    private static final String STRONG_EMPHASIS_DELIM = "*";

    private static void appendSentences(final String text, final AsciidocData data) {
        final String[] sentences = text.split("(?<=\\w{2}[.!?])", -1);
        // Full sentences
        for (int i = 0; i < sentences.length - 1; i++) {
            data.appendAdjustingSpace(sentences[i]).newLine();
        }
        // Partial sentence
        data.appendAdjustingSpace(sentences[sentences.length - 1]);
    }

    @Override
    public Void visitStartElement(final StartElementTree node, final AsciidocData data) {
        final String elementName = node.getName().toString();
        switch (elementName) {
            case "p":
                data.newParagraph();
                break;
            case "ol":
                // Nested list without a first paragraph
                if (data.getCurrentNode() instanceof ListItem) {
                    data.newParagraph();
                }
                data.pushChildNode(ListImpl::new).setContext(ListImpl.ORDERED_LIST_CONTEXT);
                break;
            case "ul":
                // Nested list without a first paragraph
                if (data.getCurrentNode() instanceof ListItem) {
                    data.newParagraph();
                }
                data.pushChildNode(ListImpl::new).setContext(ListImpl.UNORDERED_LIST_CONTEXT);
                break;
            case "li":
                if (!(data.getCurrentNode() instanceof List)) {
                    throw new IllegalArgumentException("A <li> tag must be a child of a <ol> or <ul> tag.");
                }
                data.pushChildNode(ListItemImpl::new);
                break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                // Flush the current paragraph
                data.newParagraph();
                StructuralNode currentNode;
                // Remove other types of nodes from stack
                while ((currentNode = data.getCurrentNode()) != null
                        && !(currentNode instanceof Section || currentNode instanceof Document)) {
                    data.popNode();
                }
                break;
            case "table":
                data.pushChildNode(TableImpl::new);
                break;
            case "tr":
                break;
            case "th":
                data.pushChildNode(CellImpl::new).setContext(CellImpl.HEADER_CONTEXT);
                break;
            case "td":
                data.pushChildNode(CellImpl::new);
                break;
            case "pre":
                data.newParagraph();
                data.getCurrentParagraph().setContext(BlockImpl.LISTING_CONTEXT);
                data.getCurrentParagraph().setStyle(SOURCE_STYLE);
                break;
            case "code":
                data.newTextSpan(CODE_STYLE);
                break;
            case "em":
            case "i":
                data.newTextSpan(EMPHASIS_STYLE);
                break;
            case "strong":
            case "b":
                data.newTextSpan(STRONG_EMPHASIS_STYLE);
                break;
            default:
        }
        return super.visitStartElement(node, data);
    }

    @Override
    public Void visitEndElement(final EndElementTree node, final AsciidocData data) {
        final String elementName = node.getName().toString();
        switch (elementName) {
            case "p":
                // Ignore closing tags.
                break;
            case "ol":
            case "ul":
            case "li":
            case "table":
            case "th":
            case "td":
                data.popNode();
                break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                // Only flush the current line
                if (!data.getCurrentLine().isEmpty()) {
                    data.newLine();
                }
                // The current paragraph contains the title
                // We retrieve the text and empty the paragraph
                final Block currentParagraph = data.getCurrentParagraph();
                final String title = StringUtils.normalizeSpace(currentParagraph.convert());
                currentParagraph.setLines(new ArrayList<>());

                // There should be no <h1> tags
                final int newLevel = "h1".equals(elementName) ? 2 : elementName.charAt(1) - '0';
                data.setCurrentSectionLevel(newLevel);
                data.getCurrentNode().setTitle(title);
                break;
            case "pre":
                data.newParagraph();
                break;
            case "tr":
                // We group the new cells into a row
                final Table table = (Table) data.getCurrentNode();
                final java.util.List<StructuralNode> cells = table.getBlocks();
                // First index of the row
                int idx = 0;
                for (final Row row : table.getHeader()) {
                    idx += row.getCells().size();
                }
                for (final Row row : table.getBody()) {
                    idx += row.getCells().size();
                }
                final Row row = new RowImpl();
                String context = CellImpl.BODY_CONTEXT;
                for (int i = idx; i < table.getBlocks().size(); i++) {
                    final StructuralNode cell = cells.get(i);
                    context = cell.getContext();
                    if (cell instanceof Cell) {
                        row.getCells().add((Cell) cell);
                    }
                }
                if (CellImpl.HEADER_CONTEXT.equals(context)) {
                    table.getHeader().add(row);
                } else {
                    table.getBody().add(row);
                }
                break;
            case "code":
                appendSpan(data, CODE_DELIM);
                break;
            case "em":
            case "i":
                appendSpan(data, EMPHASIS_DELIM);
                break;
            case "strong":
            case "b":
                appendSpan(data, STRONG_EMPHASIS_DELIM);
                break;
            default:
        }
        return super.visitEndElement(node, data);
    }

    @Override
    public Void visitLink(final LinkTree node, final AsciidocData data) {
        final String className = substringBefore(node.getReference().getSignature(), '#');
        final String simpleName = StringUtils.substringAfterLast(className, '.');
        if (!data.getCurrentLine().isEmpty()) {
            data.append(" ");
        }
        data.append("xref:")
                .append(className)
                .append(".adoc[")
                .append(simpleName)
                .append("]");
        return super.visitLink(node, data);
    }

    @Override
    public Void visitLiteral(final LiteralTree node, final AsciidocData data) {
        if (node.getKind() == DocTree.Kind.CODE) {
            data.newTextSpan(CODE_STYLE);
            node.getBody().accept(this, data);
            appendSpan(data, "`");
        } else {
            node.getBody().accept(this, data);
        }
        return super.visitLiteral(node, data);
    }

    private void appendSpan(final AsciidocData data, final String delimiter) {
        final String body = data.popTextSpan();
        data.append(delimiter);
        final boolean needsEscaping = body.contains(delimiter);
        if (needsEscaping) {
            data.append("++").append(body).append("++");
        } else {
            data.append(body);
        }
        data.append(delimiter);
    }

    @Override
    public Void visitText(final TextTree node, final AsciidocData data) {
        final Block currentParagraph = data.getCurrentParagraph();
        if (BlockImpl.PARAGRAPH_CONTEXT.equals(currentParagraph.getContext())) {
            appendSentences(node.getBody(), data);
        } else {
            data.append(node.getBody());
        }
        return super.visitText(node, data);
    }
}
