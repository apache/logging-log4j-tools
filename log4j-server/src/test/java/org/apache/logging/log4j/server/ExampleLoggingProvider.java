/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Example log event provider you can run provided a TCPSocketServer is running first.
 * 
 * Example JVM command line:
 * 
 * <pre>
 * -Dlog4j2.configurationFile=src/test/resources/org/apache/logging/log4j/server/log4j-socket-ser-options.xml -DSocketAppender.port=4664
 * </pre>
 */
public class ExampleLoggingProvider {

    public static void main(String[] args) {
        Logger logger = LogManager.getLogger();
        long i = 0;
        while (true) {
            logger.info("i = {}", i);
            i++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
