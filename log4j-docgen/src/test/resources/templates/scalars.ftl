<#ftl output_format="plainText" strip_whitespace=true>
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
<#-- @ftlvariable name="scalars" type="org.apache.logging.log4j.docgen.model.ScalarType[]" -->
<#include "license.ftl">
= Type converters

Type converter plugins are used to convert simple `String` values into other types.

== Available converters

[cols="1m,2"]
|===
|Type|Description

<#list scalars as scalar>
|[[${scalar.className}]]
${scalar.className?replace('org.apache.logging.log4j', 'o.a.l.l')}
a|${scalar.description.text}
    <#if scalar.values?size != 0>

Possible values:

        <#list scalar.values as value>
* `${value.name}`: ${value.description.text}
        </#list>
    </#if>
</#list>
