/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.Artifact;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

/**
 * Represents an application or domain plugin artifact.
 *
 * @since 4.0
 */
public interface ArtifactPlugin extends Artifact<ArtifactPluginDescriptor>
{
}
