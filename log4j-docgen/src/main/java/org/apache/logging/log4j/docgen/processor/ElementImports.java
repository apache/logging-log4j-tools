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

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * A map associating simple class name keys with qualified class names that is collected from {@link com.sun.source.tree.ImportTree} of an {@link javax.lang.model.element.Element}.
 */
final class ElementImports extends TreeMap<String, String> {

    private static final ElementImports EMPTY = new ElementImports();

    private static final long serialVersionUID = 0L;

    private ElementImports() {}

    private ElementImports(Map<String, String> imports) {
        super(imports);
    }

    static ElementImportsFactory factory(final Trees trees) {
        return element -> {
            final ImportCollectingTreeScanner scanner = new ImportCollectingTreeScanner();
            @Nullable final TreePath treePath = trees.getPath(element);
            if (treePath == null) {
                return EMPTY;
            }
            scanner.scan(treePath.getCompilationUnit(), null);
            return new ElementImports(scanner.imports);
        };
    }

    private static final class ImportCollectingTreeScanner extends TreeScanner<Object, Trees> {

        private final Map<String, String> imports = new HashMap<>();

        private ImportCollectingTreeScanner() {}

        @Override
        public Object visitImport(final ImportTree importTree, final Trees trees) {
            final Tree qualifiedIdentifier = importTree.getQualifiedIdentifier();
            final String qualifiedClassName = qualifiedIdentifier.toString();
            final String simpleClassName = qualifiedClassName.substring(qualifiedClassName.lastIndexOf('.') + 1);
            imports.put(simpleClassName, qualifiedClassName);
            return super.visitImport(importTree, trees);
        }
    }
}
