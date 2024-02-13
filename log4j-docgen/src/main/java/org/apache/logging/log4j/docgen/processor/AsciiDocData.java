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
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.logging.log4j.docgen.processor.internal.BlockImpl;
import org.apache.logging.log4j.docgen.processor.internal.DocumentImpl;
import org.apache.logging.log4j.docgen.processor.internal.SectionImpl;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

final class AsciiDocData {
    private static final Pattern WHITESPACE_SEQUENCE = Pattern.compile("\\s+");
    private static final String SPACE = " ";
    private static final char SPACE_CHAR = ' ';
    private static final char CODE_CHAR = '`';

    private final Document document;
    private int currentSectionLevel;
    private StructuralNode currentNode;
    // A stack of nested text blocks. Each can have a different style.
    private final Deque<Block> paragraphs = new ArrayDeque<>();
    private final Deque<StringBuilder> lines = new ArrayDeque<>();

    public AsciiDocData() {
        document = new DocumentImpl();
        currentSectionLevel = 1;
        currentNode = document;
        paragraphs.push(new BlockImpl(currentNode));
        lines.push(new StringBuilder());
    }

    public void newLine() {
        // Remove trailing space
        final String line = getCurrentLine().toString().stripTrailing();
        // Ignore leading empty lines
        if (!getCurrentParagraph().getLines().isEmpty() || !line.isEmpty()) {
            getCurrentParagraph().getLines().add(line);
        }
        getCurrentLine().setLength(0);
    }

    public AsciiDocData append(final String text) {
        final String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            getCurrentLine().append(lines[i]);
            if (i != lines.length - 1) {
                newLine();
            }
        }
        return this;
    }

    public AsciiDocData appendAdjustingSpace(final CharSequence text) {
        final String normalized = WHITESPACE_SEQUENCE.matcher(text).replaceAll(SPACE);
        if (!normalized.isEmpty()) {
            final StringBuilder currentLine = getCurrentLine();
            // Last char of current line or space
            final char lineLastChar = isEmpty(currentLine) ? SPACE_CHAR : currentLine.charAt(currentLine.length() - 1);
            // First char of test
            final char textFirstChar = normalized.charAt(0);
            if (lineLastChar == SPACE_CHAR && textFirstChar == SPACE_CHAR) {
                // Merge spaces
                currentLine.append(normalized, 1, normalized.length());
            } else if (lineLastChar == CODE_CHAR && Character.isAlphabetic(textFirstChar)) {
                currentLine.append(SPACE_CHAR).append(normalized);
            } else {
                currentLine.append(normalized);
            }
        }
        return this;
    }

    public void newTextSpan() {
        paragraphs.push(new BlockImpl(paragraphs.peek()));
        lines.push(new StringBuilder());
    }

    public String popTextSpan() {
        // Flush the paragraph
        final StringBuilder line = lines.peek();
        if (isNotEmpty(line)) {
            newLine();
        }
        lines.pop();
        return String.join(SPACE, paragraphs.pop().getLines());
    }

    public Block newParagraph() {
        return newParagraph(currentNode);
    }

    private Block newParagraph(final StructuralNode parent) {
        newLine();
        final Block currentParagraph = paragraphs.pop();
        final java.util.List<String> lines = currentParagraph.getLines();
        // Remove trailing empty lines
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).isEmpty()) {
                lines.remove(i);
            } else {
                break;
            }
        }
        if (!currentParagraph.getLines().isEmpty()) {
            currentNode.append(currentParagraph);
        }
        paragraphs.push(new BlockImpl(parent));
        return currentParagraph;
    }

    public StructuralNode getCurrentNode() {
        return currentNode;
    }

    public Block getCurrentParagraph() {
        return paragraphs.peek();
    }

    public StringBuilder getCurrentLine() {
        return lines.peek();
    }

    public Document getDocument() {
        return document;
    }

    public void setCurrentSectionLevel(final int sectionLevel) {
        if (sectionLevel < currentSectionLevel) {
            // Close all subsections
            while (sectionLevel < currentSectionLevel) {
                popNode();
                currentSectionLevel--;
            }
            // Create sibling section
            popNode();
            pushChildNode(SectionImpl::new);
        } else {
            // Create missing section levels
            while (sectionLevel > currentSectionLevel) {
                pushChildNode(SectionImpl::new);
                currentSectionLevel++;
            }
        }
    }

    /**
     * Creates and appends a new child to the current node.
     *
     * @param supplier a function to create a new node that takes its parent node a parameter.
     */
    public StructuralNode pushChildNode(final Function<? super StructuralNode, ? extends StructuralNode> supplier) {
        final StructuralNode child = supplier.apply(currentNode);

        // Flushes and reparents the current paragraph
        newParagraph(child);

        currentNode.append(child);
        return currentNode = child;
    }

    public StructuralNode popNode() {
        final StructuralNode currentNode = this.currentNode;

        final StructuralNode parent = (StructuralNode) currentNode.getParent();
        if (parent == null) {
            throw new EmptyStackException();
        }
        // Flushes and creates a new current paragraph
        newParagraph(parent);

        this.currentNode = parent;
        return currentNode;
    }
}
