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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.jspecify.annotations.Nullable;

abstract class AbstractAsciiDocTreeVisitor extends SimpleDocTreeVisitor<Void, AsciiDocData> {

    private static final String JAVA_SOURCE_STYLE = "source,java";
    private static final String XML_SOURCE_STYLE = "source,xml";

    private static final String CODE_DELIM = "`";
    private static final String EMPHASIS_DELIM = "_";
    private static final String STRONG_EMPHASIS_DELIM = "*";

    private static final String SPACE = " ";
    // Simple pattern to match (most) XML opening tags
    private static final Pattern XML_TAG = Pattern.compile("<\\w+\\s*(\\w+=[\"'][^\"']*[\"']\\s*)*/?>");

    @Override
    public Void visitStartElement(final StartElementTree node, final AsciiDocData data) {
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
                break;
            case "code":
            case "em":
            case "i":
            case "strong":
            case "b":
                data.newTextSpan();
                break;
            default:
        }
        return super.visitStartElement(node, data);
    }

    @Override
    public Void visitEndElement(final EndElementTree node, final AsciiDocData data) {
        final String elementName = node.getName().toString();
        switch (elementName) {
            case "p":
                // Ignore closing tags.
                break;
            case "ol":
            case "ul":
            case "li":
            case "table":
            case "td":
                data.popNode();
                break;
            case "th":
                data.popNode().setContext(CellImpl.HEADER_CONTEXT);
                break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                // Only flush the current line
                if (!isEmpty(data.getCurrentLine())) {
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
                final Block codeBlock = data.newParagraph();
                final java.util.List<String> lines = codeBlock.getLines();
                // Trim common indentation and detect language
                int commonIndentSize = Integer.MAX_VALUE;
                for (final String line : lines) {
                    final int firstNotSpace = StringUtils.indexOfAnyBut(line, SPACE);
                    if (0 <= firstNotSpace && firstNotSpace < commonIndentSize) {
                        commonIndentSize = firstNotSpace;
                    }
                }
                final boolean isXml = XML_TAG.matcher(String.join(SPACE, lines)).find();
                final java.util.List<String> newLines = new ArrayList<>(lines.size());
                for (final String line : lines) {
                    newLines.add(StringUtils.substring(line, commonIndentSize));
                }
                codeBlock.setLines(newLines);
                codeBlock.setStyle(isXml ? XML_SOURCE_STYLE : JAVA_SOURCE_STYLE);
                break;
            case "tr":
                // We group the new cells into a row
                final Table table = (Table) data.getCurrentNode();
                final java.util.List<StructuralNode> cells = table.getBlocks();
                // First index of the row
                int idx = 0;
                for (final Row row : table.getBody()) {
                    idx += row.getCells().size();
                }
                final Row row = new RowImpl();
                for (int i = idx; i < table.getBlocks().size(); i++) {
                    final StructuralNode cell = cells.get(i);
                    if (cell instanceof Cell) {
                        row.getCells().add((Cell) cell);
                    }
                }
                table.getBody().add(row);
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
    public Void visitLink(final LinkTree node, final AsciiDocData data) {
        final String referenceSignature = getReferenceSignature(node.getReference(), data);
        final String referenceLabel = linkLabelToAsciiDoc(node);
        data.appendAdjustingSpace(" apiref:")
                .append(referenceSignature)
                .append("[")
                .append(referenceLabel)
                .append("]");
        return super.visitLink(node, data);
    }

    private static String getReferenceSignature(final ReferenceTree referenceTree, final AsciiDocData data) {

        // If it is of type `{@link #foo}`
        final String referenceSignature = referenceTree.getSignature();
        if (referenceSignature.startsWith("#")) {
            return data.qualifiedClassName + referenceSignature;
        }

        // Determine class and method parts of the reference signature
        final int methodSplitterIndex = referenceSignature.indexOf('#');
        final String classSignature;
        final String methodSignature;
        if (methodSplitterIndex > 0) {
            classSignature = referenceSignature.substring(0, methodSplitterIndex);
            methodSignature = referenceSignature.substring(methodSplitterIndex);
        } else {
            classSignature = referenceSignature;
            methodSignature = "";
        }

        // If it is an imported class
        @Nullable final String importedClassFqcn = data.imports.get(classSignature);
        if (importedClassFqcn != null) {
            return importedClassFqcn + methodSignature;
        }

        // If it is a `java.lang` class
        try {
            final String qualifiedClassName = "java.lang." + classSignature;
            Class.forName(qualifiedClassName);
            return qualifiedClassName + methodSignature;
        } catch (final ClassNotFoundException ignored) {
            // Do nothing
        }

        // Otherwise it is a package-local class
        final String packageName = data.qualifiedClassName.substring(0, data.qualifiedClassName.lastIndexOf('.'));
        return packageName + '.' + classSignature + methodSignature;
    }

    private static String linkLabelToAsciiDoc(final LinkTree node) {
        int[] labelTokenIndex = {0};
        return node.getLabel().stream()
                .map(labelToken -> {
                    if (!(labelToken instanceof TextTree)) {
                        final String message = String.format(
                                "Non-text labels in links are not allowed! "
                                        + "That is, `{@link foo.bar.Baz awesome thing}` is allowed, whereas {@link foo.bar.Baz {@code doh}}` is not. "
                                        + "While visiting link `%s`, label token at index %d found to be of unexpected type: `%s`",
                                node, labelTokenIndex[0], labelToken.getClass().getCanonicalName());
                        throw new IllegalArgumentException(message);
                    }
                    labelTokenIndex[0]++;
                    final TextTree textLabel = (TextTree) labelToken;
                    return textLabel.getBody();
                })
                .collect(Collectors.joining(" "));
    }

    @Override
    public Void visitLiteral(final LiteralTree node, final AsciiDocData data) {
        if (node.getKind() == DocTree.Kind.CODE) {
            data.newTextSpan();
            node.getBody().accept(this, data);
            appendSpan(data, "`");
        } else {
            node.getBody().accept(this, data);
        }
        return super.visitLiteral(node, data);
    }

    @Override
    public Void visitEntity(final EntityTree node, final AsciiDocData asciiDocData) {
        final String text;
        switch (node.getName().toString()) {
            case "amp":
                text = "&";
                break;
            case "apos":
                text = "'";
                break;
            case "gt":
                text = ">";
                break;
            case "lt":
                text = "<";
                break;
            case "quot":
                text = "\"";
                break;
            default:
                text = null;
        }
        if (text != null) {
            asciiDocData.append(text);
        }
        return super.visitEntity(node, asciiDocData);
    }

    @Override
    public Void visitText(final TextTree node, final AsciiDocData data) {
        final Block currentParagraph = data.getCurrentParagraph();
        if (BlockImpl.PARAGRAPH_CONTEXT.equals(currentParagraph.getContext())) {
            appendSentences(node.getBody(), data);
        } else {
            data.append(node.getBody());
        }
        return super.visitText(node, data);
    }

    private static void appendSentences(final String text, final AsciiDocData data) {
        final String[] sentences = text.split("(?<=\\w{2}[.!?])", -1);
        // Full sentences
        for (int i = 0; i < sentences.length - 1; i++) {
            data.appendAdjustingSpace(sentences[i]).newLine();
        }
        // Partial sentence
        data.appendAdjustingSpace(sentences[sentences.length - 1]);
    }

    private void appendSpan(final AsciiDocData data, final String delimiter) {
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
}
