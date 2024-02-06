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

import java.util.EmptyStackException;
import java.util.function.Function;
import org.apache.logging.log4j.docgen.processor.internal.BlockImpl;
import org.apache.logging.log4j.docgen.processor.internal.DocumentImpl;
import org.apache.logging.log4j.docgen.processor.internal.SectionImpl;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

final class AsciidocData {
    private final Document document;
    private int currentSectionLevel;
    private StructuralNode currentNode;
    // not attached to the current node
    private Block currentParagraph;
    private final StringBuilder currentLine;

    public AsciidocData() {
        document = new DocumentImpl();
        currentSectionLevel = 1;
        currentNode = document;
        currentParagraph = new BlockImpl(currentNode);
        currentLine = new StringBuilder();
    }

    public void newLine() {
        // Remove trailing space
        final String line = currentLine.toString().stripTrailing();
        // Ignore leading empty lines
        if (!currentParagraph.getLines().isEmpty() || !line.isEmpty()) {
            currentParagraph.getLines().add(line);
        }
        currentLine.setLength(0);
    }

    public AsciidocData append(final String text) {
        final String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            currentLine.append(lines[i]);
            if (i != lines.length - 1) {
                newLine();
            }
        }
        return this;
    }

    public void appendWords(final String words) {
        if (words.isBlank()) {
            return;
        }
        // Separate text from previous words
        if (!currentLine.isEmpty() && Character.isAlphabetic(words.codePointAt(0))) {
            currentLine.append(" ");
        }
        currentLine.append(words);
    }

    public void newParagraph() {
        newLine();
        final java.util.List<String> lines = currentParagraph.getLines();
        // Remove trailing empty lines
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).isEmpty()) {
                lines.remove(i);
            }
        }
        if (!currentParagraph.getLines().isEmpty()) {
            currentNode.append(currentParagraph);
            currentParagraph = new BlockImpl(currentNode);
        }
    }

    public StructuralNode getCurrentNode() {
        return currentNode;
    }

    public Block getCurrentParagraph() {
        return currentParagraph;
    }

    public StringBuilder getCurrentLine() {
        return currentLine;
    }

    public Document getDocument() {
        return document;
    }

    public void setCurrentSectionLevel(final int sectionLevel) {
        while (sectionLevel < currentSectionLevel) {
            popNode();
            currentSectionLevel--;
        }
        while (sectionLevel > currentSectionLevel) {
            pushChildNode(SectionImpl::new);
            currentSectionLevel++;
        }
    }

    /**
     * Creates and appends a new child to the current node.
     *
     * @param supplier a function to create a new node that takes its parent node a parameter.
     */
    public StructuralNode pushChildNode(final Function<? super StructuralNode, ? extends StructuralNode> supplier) {
        // Flushes the current paragraph
        newParagraph();

        final StructuralNode child = supplier.apply(currentNode);
        // Creates a new current paragraph
        currentParagraph = new BlockImpl(child);

        currentNode.append(child);
        return currentNode = child;
    }

    public void popNode() {
        final StructuralNode currentNode = this.currentNode;
        // Flushes the current paragraph
        newParagraph();

        final StructuralNode parent = (StructuralNode) currentNode.getParent();
        if (parent == null) {
            throw new EmptyStackException();
        }
        // Creates a new current paragraph
        currentParagraph = new BlockImpl(parent);

        this.currentNode = parent;
    }
}
