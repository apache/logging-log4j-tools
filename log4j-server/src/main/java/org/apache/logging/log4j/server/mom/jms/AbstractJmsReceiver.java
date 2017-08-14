/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.server.mom.jms;

import java.util.Properties;

import org.apache.logging.log4j.server.JmsServer;

/**
 * Common JMS server functionality.
 *
 * @since 2.6
 */
public abstract class AbstractJmsReceiver {

    class CommandLineArgs {

    }

    /**
     * Prints out usage information to {@linkplain System#err standard error}.
     */
    protected abstract void usage();

    /**
     * Executes a JmsServer with the given command line arguments.
     *
     * @param interactive
     *            Whether or not this is an interactive application by providing a command line and exit on error.
     * @param args
     *            command line arguments
     *
     * @throws Exception
     */
    protected void doMain(boolean interactive, final String... args) throws Exception {
        // TODO Too many args, Use picocli
        if (args.length < 5) {
            usage();
            if (interactive) {
                System.exit(1);
            }
        }
        final Properties properties = new Properties();
        for (int index = 5; index < args.length; index += 2) {
            properties.put(args[index], args[index + 1]);
        }
        final JmsServer server = new JmsServer(args[0], "ConnectionFactory", args[1], args[2], args[3],
                args[4].toCharArray(), properties);
        server.start();
        if (interactive) {
            server.commandLineLoop();
        }
    }
}
