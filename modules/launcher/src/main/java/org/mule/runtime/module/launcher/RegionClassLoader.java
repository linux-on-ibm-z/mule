/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher;

import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionClassLoader extends MuleArtifactClassLoader {

  private final List<ArtifactClassLoader> classLoaders = new ArrayList<>();
  private final Map<String, ArtifactClassLoader> packageMapping = new HashMap<>();
  private final Map<String, ArtifactClassLoader> resourceMapping = new HashMap<>();

  public RegionClassLoader(String name, URL[] urls, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy) {
    super(name, urls, parent, lookupPolicy);
  }

  public void addClassLoader(ArtifactClassLoader artifactClassLoader, Set<String> exportedPackages,
                             Set<String> exportedResources) {
    classLoaders.add(artifactClassLoader);

    exportedPackages.forEach(p -> packageMapping.put(p, artifactClassLoader));
    exportedResources.forEach(r -> resourceMapping.put(r, artifactClassLoader));
  }

  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    final String packageName = ClassUtils.getPackageName(name);

    final ArtifactClassLoader artifactClassLoader = packageMapping.get(packageName);
    if (artifactClassLoader != null) {
      return artifactClassLoader.findClass(name);
    } else {
      // TODO(pablo.kraan): isolation - add a new policy map if found
      return classLoaders.get(0).findClass(name);
    }
  }

  @Override
  public final URL findResource(final String name) {
    URL resource;
    final ArtifactClassLoader artifactClassLoader = resourceMapping.get(name);
    if (artifactClassLoader != null) {
      resource = artifactClassLoader.getClassLoader().getResource(name);
    } else {
      resource = classLoaders.get(0).findResource(name);
    }
    return resource;
  }

  @Override
  public final Enumeration<URL> findResources(final String name) throws IOException {
    final Enumeration<URL> resources;
    final ArtifactClassLoader artifactClassLoader = resourceMapping.get(name);
    if (artifactClassLoader != null) {
      resources = artifactClassLoader.findResources(name);
    } else {
      resources = classLoaders.get(0).findResources(name);
    }
    return resources;
  }
}
