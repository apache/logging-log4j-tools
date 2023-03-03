<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements. See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License. You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
The Apache Log4j ${release.version} release is now available for voting.

This release contains minor enhancements and bug fixes.

Source repository: https://github.com/apache/logging-log4j2
Tag: rel/${release.version}-rc.1

Please download, test, and cast your votes on the Log4j developers list.

[ ] +1, release the artifacts
[ ] -1, don't release, because...

The vote will remain open for 24 hours (or more if required). All votes are welcome and we encourage everyone to test the release, but only the Logging Services PMC votes are officially counted. At least 3 +1 votes and more positive than negative votes are required.

<#if entriesByType?size gt 0>== Changes
<#list entriesByType as entryType, entries>

=== ${entryType?capitalize}

<#list entries as entry>
* ${entry.description.text?replace("\\s+", " ", "r")} (<#list entry.issues as issue>${issue.id}<#if issue?has_next>, </#if></#list>)
</#list>
</#list>
</#if>
