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
package org.apache.logging.log4j.changelog.importer;

import static org.apache.logging.log4j.changelog.util.StringUtils.isBlank;
import static org.apache.logging.log4j.changelog.util.StringUtils.trimNullable;
import static org.apache.logging.log4j.changelog.util.XmlReader.failureAtXmlNode;
import static org.apache.logging.log4j.changelog.util.XmlReader.readXmlFileRootElement;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.changelog.util.XmlReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class MavenChanges {

    final List<Release> releases;

    private MavenChanges(final List<Release> releases) {
        this.releases = releases;
    }

    static MavenChanges readFromFile(final Path file) {

        // Read the root element
        final Element documentElement = readXmlFileRootElement(file, "document");

        // Read the `body` element
        final Element bodyElement = XmlReader.requireChildElementMatchingName(documentElement, "body");

        // Read releases
        final List<Release> releases = new ArrayList<>();
        final NodeList releaseNodes = bodyElement.getChildNodes();
        final int releaseNodeCount = releaseNodes.getLength();
        for (int releaseNodeIndex = 0; releaseNodeIndex < releaseNodeCount; releaseNodeIndex++) {
            final Node releaseNode = releaseNodes.item(releaseNodeIndex);
            if ("release".equals(releaseNode.getNodeName()) && Node.ELEMENT_NODE == releaseNode.getNodeType()) {
                final Element releaseElement = (Element) releaseNode;
                final Release release = Release.fromElement(releaseElement);
                releases.add(release);
            }
        }

        // Create the instance
        return new MavenChanges(releases);
    }

    static final class Release {

        final String version;

        final String date;

        final List<Action> actions;

        private Release(final String version, final String date, final List<Action> actions) {
            this.version = version;
            this.date = date;
            this.actions = actions;
        }

        private static Release fromElement(final Element element) {

            // Read `version`
            final String version = trimNullable(element.getAttribute("version"));
            if (isBlank(version)) {
                throw XmlReader.failureAtXmlNode(element, "blank attribute: `version`");
            }

            // Read `date`
            final String date = trimNullable(element.getAttribute("date"));
            final String datePattern = "^(TBD|[0-9]{4}-[0-9]{2}-[0-9]{2})$";
            if (!date.matches(datePattern)) {
                throw XmlReader.failureAtXmlNode(
                        element, "`date` doesn't match with the `%s` pattern: `%s`", datePattern, date);
            }

            // Read actions
            final List<Action> actions = new ArrayList<>();
            final NodeList actionNodes = element.getChildNodes();
            final int actionNodeCount = actionNodes.getLength();
            for (int actionNodeIndex = 0; actionNodeIndex < actionNodeCount; actionNodeIndex++) {
                final Node actionNode = actionNodes.item(actionNodeIndex);
                if ("action".equals(actionNode.getNodeName()) && Node.ELEMENT_NODE == actionNode.getNodeType()) {
                    Element actionElement = (Element) actionNode;
                    Action action = Action.fromElement(actionElement);
                    actions.add(action);
                }
            }

            // Create the instance
            return new Release(version, date, actions);
        }

        @Override
        public String toString() {
            return version + " @ " + date;
        }
    }

    static final class Action {

        @Nullable
        final String issue;

        final Type type;

        final String dev;

        @Nullable
        final String dueTo;

        final String description;

        enum Type {
            ADD,
            FIX,
            UPDATE,
            REMOVE
        }

        private Action(
                @Nullable final String issue,
                final Type type,
                final String dev,
                @Nullable final String dueTo,
                final String description) {
            this.issue = issue;
            this.type = type;
            this.dev = dev;
            this.dueTo = dueTo;
            this.description = description;
        }

        private static Action fromElement(final Element element) {

            // Read `issue`
            @Nullable String issue = trimNullable(element.getAttribute("issue"));
            final String issuePattern = "^LOG4J2-[0-9]+$";
            if (isBlank(issue)) {
                issue = null;
            } else if (!issue.matches(issuePattern)) {
                throw XmlReader.failureAtXmlNode(
                        element, "`issue` doesn't match with the `%s` pattern: `%s`", issuePattern, issue);
            }

            // Read `type`
            @Nullable final String typeString = trimNullable(element.getAttribute("type"));
            final Type type;
            if (isBlank(typeString)) {
                type = Type.UPDATE;
            } else {
                try {
                    type = Type.valueOf(typeString.toUpperCase(Locale.US));
                } catch (IllegalArgumentException error) {
                    throw failureAtXmlNode(error, element, "invalid type: `%s`", typeString);
                }
            }

            // Read `dev`
            @Nullable final String dev = trimNullable(element.getAttribute("dev"));
            if (isBlank(dev)) {
                throw XmlReader.failureAtXmlNode(element, "blank attribute: `dev`");
            }

            // Read `dueTo`
            @Nullable String dueTo = trimNullable(element.getAttribute("due-to"));
            if (isBlank(dueTo)) {
                dueTo = null;
            }

            // Read `description`
            @Nullable final String description = trimNullable(element.getTextContent());
            if (isBlank(description)) {
                throw XmlReader.failureAtXmlNode(element, "blank `description`");
            }

            // Create the instance
            return new Action(issue, type, dev, dueTo, description);
        }

        @Override
        public String toString() {
            return issue != null ? issue : "unknown";
        }
    }
}
