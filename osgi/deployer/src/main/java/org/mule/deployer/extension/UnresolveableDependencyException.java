/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.deployer.extension;

/**
 * Thrown to indicate that a mule core extension dependency was not
 * successfully resolved.
 */
public class UnresolveableDependencyException extends RuntimeException
{

    public UnresolveableDependencyException(String message)
    {
        super(message);
    }
}