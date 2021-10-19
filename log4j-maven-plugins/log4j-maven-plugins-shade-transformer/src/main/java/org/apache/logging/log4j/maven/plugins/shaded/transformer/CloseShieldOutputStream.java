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

package org.apache.logging.log4j.maven.plugins.shaded.transformer;


import org.apache.commons.io.output.ProxyOutputStream;

import java.io.IOException;
import java.io.OutputStream;

import static org.apache.commons.io.output.ClosedOutputStream.CLOSED_OUTPUT_STREAM;

/**
 * Suppress the close of underlying output stream.
 */
final class CloseShieldOutputStream extends ProxyOutputStream {

    /**
     * @param out  the OutputStream to delegate to
     */
    /* default */ CloseShieldOutputStream(final OutputStream out) {
        super(out);
    }


    @Override
    public void close() throws IOException {
        out.flush();
        out = CLOSED_OUTPUT_STREAM;
    }
}
