#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to you under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Enable strict mode
set -euo pipefail
IFS=$'\n\t'

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

stderr() {
    echo "$*" 1>&2
}

fail_for_invalid_args() {
    stderr "Invalid arguments!"
    stderr "Expected arguments: <vote|announce> <version> <commitId> <nexusUrl>"
    exit 1
}

# Check arguments
[ $# -ne 4 ] && fail_for_invalid_args

# Constants
PROJECT_NAME="Apache Log4j Tools"
PROJECT_ID="log4j-tools"
PROJECT_SITE="https://logging.apache.org/log4j/tools"
PROJECT_STAGING_SITE="${PROJECT_SITE/apache.org/staged.apache.org}"
PROJECT_REPO="https://github.com/apache/logging-log4j-tools"
PROJECT_VERSION="$2"
COMMIT_ID="$3"
NEXUS_URL="$4"
PROJECT_DIST_URL="https://dist.apache.org/repos/dist/dev/logging/$PROJECT_ID/$PROJECT_VERSION"

# Check release notes file
RELEASE_NOTES_FILE="$SCRIPT_DIR/../target/generated-site/antora/modules/ROOT/pages/_release-notes/$PROJECT_VERSION.adoc"
[ -f "$RELEASE_NOTES_FILE" ] || {
    stderr "Couldn't find release notes file: $RELEASE_NOTES_FILE"
    exit 1
}

dump_release_notes() {
    awk "f{print} /^Release date::/{f=1}" "$RELEASE_NOTES_FILE" \
        | sed -r -e 's|'$PROJECT_REPO'/(issues|pull)/[0-9]+\[([0-9]+)\]|#\2|g
                     s|https://github.com/([^/]+)/([^/]+)/(pull|issues)/([0-9]+)\[(\1/\2#\4)\]|\5|g'
}

case $1 in

vote)
    cat <<EOF
To: dev@logging.apache.org
Title: [VOTE] Release $PROJECT_NAME \`$PROJECT_VERSION\`

This is a vote to release the $PROJECT_NAME \`$PROJECT_VERSION\`.

Website: $PROJECT_STAGING_SITE-$PROJECT_VERSION
GitHub: $PROJECT_REPO
Commit: $COMMIT_ID
Distribution: $PROJECT_DIST_URL
Nexus: $NEXUS_URL
Signing key: 0x077e8893a6dcc33dd4a4d5b256e73ba9a0b592d0
Review kit: https://s.apache.org/logging-parent-release-review-kit

Please download, test, and cast your votes on this mailing list.

[ ] +1, release the artifacts
[ ] -1, don't release, because...

This vote is open for 72 hours and will pass unless getting a
net negative vote count. All votes are welcome and we encourage
everyone to test the release, but only the Logging Services PMC
votes are officially counted. At least 3 +1 votes and more
positive than negative votes are required.

== Release notes

$(dump_release_notes)
EOF
    ;;

announce)
    cat <<EOF
To: log4j-user@logging.apache.org, dev@logging.apache.org
Title: [ANNOUNCE] $PROJECT_NAME \`$PROJECT_VERSION\` released

${PROJECT_NAME} team is pleased to announce the \`$PROJECT_VERSION\`
release. This project provides tooling internally used by the
Apache Log4j project. For further information (support, download,
etc.) see the project website[1].

[1] $PROJECT_SITE

== Release Notes

$(dump_release_notes)
EOF
    ;;

*) fail_for_invalid_args

esac
