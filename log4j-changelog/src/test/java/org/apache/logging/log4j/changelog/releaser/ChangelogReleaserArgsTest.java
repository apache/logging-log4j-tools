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
package org.apache.logging.log4j.changelog.releaser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChangelogReleaserArgsTest {

    @ParameterizedTest
    @CsvSource({
        "0.0.1,0,0,1",
        "0.1.0,0,1,0",
        "1.2.3-alpha1,1,2,3-alpha1",
        "1.2.3-beta1,1,2,3-beta1",
        "1.2.3-rc1,1,2,3-rc1",
        "1.2.3-SNAPSHOT,1,2,3-SNAPSHOT"
    })
    void valid_releaseVersion_should_match(
            final String version, final String major, final String minor, final String patch) {
        Matcher matcher = ChangelogReleaserArgs.DEFAULT_VERSION_PATTERN.matcher(version);
        assertThat(matcher).matches();
        assertThat(matcher.group("major")).isEqualTo(major);
        assertThat(matcher.group("minor")).isEqualTo(minor);
        assertThat(matcher.group("patch")).isEqualTo(patch);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1", "0.1.2.3", "0.1-SNAPSHOT", "1.2-rc1", "1.2.3-4"})
    void invalid_releaseVersion_should_not_match(final String releaseVersion) {
        Matcher matcher = ChangelogReleaserArgs.DEFAULT_VERSION_PATTERN.matcher(releaseVersion);
        assertThat(matcher.matches()).isFalse();
    }
}
