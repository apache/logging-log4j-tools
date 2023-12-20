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
<#-- @ftlvariable name="type" type="org.apache.logging.log4j.docgen.PluginType" -->
<#-- @ftlvariable name="lookup" type="org.apache.logging.log4j.docgen.util.TypeLookup" -->
<#include "license.ftl">
= ${type.name}

${type.description.text}

== XML snippet
[source, xml]
----
<#assign tag><${type.name} </#assign>
<#assign indent = tag?replace('.', ' ', 'r')/>
<#assign has_elements = type.elements?size != 0/>
<#if type.attributes?size == 0>
<${type.name}/>
    <#else>
        <#list type.attributes?sort_by('name') as attr>
            <#if attr?is_first>
${tag}${attr.name}="${attr.defaultValue!}"${attr?is_last?then(has_elements?then('>', '/>'), '')}
            <#else>
${indent}${attr.name}="${attr.defaultValue!}"${attr?is_last?then(has_elements?then('>', '/>'), '')}
            </#if>
        </#list>
        <#if has_elements>
            <#list type.elements as element>
                <#assign element_type = lookup[element.type]/>
<#-- @ftlvariable name="element_type" type="org.apache.logging.log4j.docgen.model.AbstractType" -->
                <#if element_type.name?? && element_type.implementations?size == 0>
<#-- @ftlvariable name="element_type" type="org.apache.logging.log4j.docgen.model.PluginType" -->
    <${element_type.name}/>
                    <#else>
    ... ${(element.multiplicity == '*')?then('multiple ' + element.type?keep_after_last('.') + ' elements', 'one ' + element.type?keep_after_last('.') + ' element')} ...
                </#if>
            </#list>
</${type.name}>
        </#if>
</#if>
----
<#if type.attributes?size != 0>

== Attributes

Required attributes are in **bold face**.

[cols="1,1,1,5m"]
|===
|Name
|Type
|Default
|Description

    <#list type.attributes?sort_by('name') as attr>
|${attr.required?then('**', '')}${attr.name}${attr.required?then('**', '')}
|xref:../scalars.adoc#${attr.type}[${attr.type?contains('.')?then(attr.type?keep_after_last('.'), attr.type)}]
|${attr.defaultValue!}
a|${attr.description.text}

    </#list>
|===
</#if>
<#if type.elements?size != 0>

== Nested Components

Required components are in **bold face**.

[cols="1,1,5m"]
|===
|Tag
|Type
|Description

    <#list type.elements?sort_by('type') as element>
|${element.required?then('**', '') + (lookup[element.type].name!'N/A') + element.required?then('**', '')}
|xref:${element.type}.adoc[${element.type?contains('.')?then(element.type?keep_after_last('.'), element.type)}]
a|${element.description.text}

    </#list>
|===
</#if>
<#if type.implementations?size != 0>

== Known implementations

    <#list type.implementations as impl>
* xref:${impl}.adoc[${impl?contains('.')?then(impl?keep_after_last('.'), impl)}]
    </#list>
</#if>
