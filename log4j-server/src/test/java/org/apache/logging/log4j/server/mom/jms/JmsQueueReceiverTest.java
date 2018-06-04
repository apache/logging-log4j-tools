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
package org.apache.logging.log4j.server.mom.jms;

import org.apache.logging.log4j.server.mom.activemq.ActiveMqBrokerServiceRule;
import org.apache.logging.log4j.test.AvailablePortSystemPropertyTestRule;
import org.apache.logging.log4j.test.RuleChainFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class JmsQueueReceiverTest {

    private static final AvailablePortSystemPropertyTestRule portRule = AvailablePortSystemPropertyTestRule
            .create(ActiveMqBrokerServiceRule.PORT_PROPERTY_NAME);

    private static final ActiveMqBrokerServiceRule activeMqBrokerServiceRule = new ActiveMqBrokerServiceRule(
            JmsQueueReceiverTest.class.getName(), portRule.getName());

    @ClassRule
    public static RuleChain ruleChain = RuleChainFactory.create(portRule, activeMqBrokerServiceRule);

    @Test
    public void testMain() throws Exception {
        new JmsQueueReceiver().doMain(false, new String[] { "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
                "tcp://localhost:" + portRule.getPort(), "testq", "admin", "admin", "queue.testq", "testq" });
    }
}
