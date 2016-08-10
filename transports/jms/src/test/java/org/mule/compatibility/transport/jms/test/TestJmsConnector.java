/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.test;

import org.mule.compatibility.transport.jms.JmsConnector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

public class TestJmsConnector extends JmsConnector
{
    public TestJmsConnector(MuleContext context) throws InitialisationException
    {
        super(context);
        setConnectionFactory(new TestConnectionFactory());
    }
}
