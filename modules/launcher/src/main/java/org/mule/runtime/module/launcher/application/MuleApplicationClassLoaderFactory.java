/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.container.api.MuleFoldersUtil.getAppClassesFolder;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.launcher.MuleApplicationClassLoader;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.nativelib.NativeLibraryFinderFactory;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements DeployableArtifactClassLoaderFactory<ApplicationDescriptor> {

  public static final String CLASS_EXTENSION = ".class";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

  public MuleApplicationClassLoaderFactory(NativeLibraryFinderFactory nativeLibraryFinderFactory) {
    this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
  }

  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, ApplicationDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactPluginClassLoaders) {
    List<URL> urls = getApplicationResourceUrls(descriptor);

    //TODO(pablo.kraan): isolation - need to extend the lookup policy including the packages exported by the plugins
    return new MuleApplicationClassLoader(descriptor.getName(), parent.getClassLoader(),
                                          nativeLibraryFinderFactory.create(descriptor.getName()), urls,
                                          parent.getClassLoaderLookupPolicy(), artifactPluginClassLoaders);
  }

  private List<URL> getApplicationResourceUrls(ApplicationDescriptor descriptor) {
    List<URL> urls = new LinkedList<>();
    try {
      urls.add(getAppClassesFolder(descriptor.getName()).toURI().toURL());

      for (URL url : descriptor.getRuntimeLibs()) {
        urls.add(url);
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to create classloader for application", e);
    }

    if (!urls.isEmpty() && logger.isInfoEnabled()) {
      logArtifactRuntimeUrls(descriptor, urls);
    }

    return urls;
  }

  private void logArtifactRuntimeUrls(ApplicationDescriptor descriptor, List<URL> urls) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s] Loading the following jars:%n", descriptor.getName()));
    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

    for (URL url : urls) {
      sb.append(url).append(SystemUtils.LINE_SEPARATOR);
    }

    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);
    logger.info(sb.toString());
  }
}
