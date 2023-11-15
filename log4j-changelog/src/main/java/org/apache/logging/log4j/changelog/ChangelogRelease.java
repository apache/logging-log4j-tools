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
package org.apache.logging.log4j.changelog;

import java.nio.file.Path;
import org.apache.logging.log4j.changelog.util.XmlReader;
import org.apache.logging.log4j.changelog.util.XmlWriter;
import org.w3c.dom.Element;

public final class ChangelogRelease {

    public final String version;

    public final String date;

    public ChangelogRelease(final String version, final String date) {
        this.version = version;
        this.date = date;
    }

    public void writeToXmlFile(final Path path) {
        XmlWriter.toFile(path, "release", (document, releaseElement) -> {
            releaseElement.setAttribute("version", version);
            releaseElement.setAttribute("date", date);
        });
    }

    public static ChangelogRelease readFromXmlFile(final Path path) {
        final Element releaseElement = XmlReader.readXmlFileRootElement(path, "release");
        final String version = XmlReader.requireAttribute(releaseElement, "version");
        final String date = XmlReader.requireAttribute(releaseElement, "date");
        return new ChangelogRelease(version, date);
    }
}
