<!-- vim: set syn=markdown : -->
<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

# Log4j Maven Shaded Plugin Support

This module provides support for [Apache Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/). 

## Overview

This module includes the transformer for [Apache Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/), that concatenates `Log4j2Plugins.dat` files,
so it must be used when there are several Log4j2Plugins.dat files in the fat jar dependencies.

For example a fat jar must be assembled with `org.apache.logging.log4j:log4j-web` that for sure requires also `org.apache.logging.log4j:log4j-core`. Still both includes `Log4j2Plugins.dat` resources the transformer must be configured. 

## Usage

The transformer configuration must augment standard [Apache Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/) configuration in `pom.xml`.

```xml

<project>
    ...
    <build>
        ...
        <plugins>
            ...
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                ...
                                <transformer
                                        implementation="org.apache.logging.log4j.maven.plugins.shaded.transformer.Log4j2PluginCacheFileTransformer">
                                </transformer>
                            </transformers>
                            ...
                        </configuration>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.logging.maven</groupId>
                        <artifactId>log4j-maven-plugins-shade-transformer</artifactId>
                        <version>${log4jVersion}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>

    </build>

</project>
```
In the above example `${log4jVersion}` placeholder should point to the same version of the fat jar dependencies of `org.apache.logging.log4j` group

# Legacy

Initially the transformer was developed in this repository  https://github.com/edwgiz/maven-shaded-log4j-transformer
