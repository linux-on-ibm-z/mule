/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classloader;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.util.Collections.emptySet;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.functional.api.classloading.isolation.ArtifactClassLoaderHolder;
import org.mule.functional.api.classloading.isolation.ArtifactUrlClassification;
import org.mule.functional.api.classloading.isolation.PluginUrlClassification;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.MuleModule;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.CompositeClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates a class loader hierarchy to emulate the one used in a mule standalone container.
 * <p/>
 * The class loaders created have the following hierarchy:
 * <ul>
 * <li>Container: all the provided scope dependencies plus their dependencies (non test dependencies) and java</li>
 * <li>Plugins (optional): for each plugin a class loader will be created with all the compile scope dependencies and their
 * transitive dependencies (only the ones with scope compile)</li>
 * <li>Application: all the test scope dependencies and their dependencies if they are not defined to be excluded, plus their
 * transitive dependencies (again if they are not excluded).</li>
 * </ul>
 *
 * @since 4.0
 */
public class IsolatedClassLoaderFactory {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private ClassLoaderFilterFactory classLoaderFilterFactory = new ArtifactClassLoaderFilterFactory();
  private DefaultExtensionManager extensionManager = new DefaultExtensionManager();

  /**
   * Creates a {@link ArtifactClassLoaderHolder} containing the container, plugins and application {@link ArtifactClassLoader}s
   *
   * @param extraBootPackages {@link Set} of {@link String}s of extra boot packages to be appended to the container
   *        {@link ClassLoader}
   * @param artifactUrlClassification the {@link ArtifactUrlClassification} that defines the different {@link URL}s for each
   *        {@link ClassLoader}
   * @return a {@link ArtifactClassLoaderHolder} that would be used to run the test
   */
  public ArtifactClassLoaderHolder createArtifactClassLoader(Set<String> extraBootPackages,
                                                             ArtifactUrlClassification artifactUrlClassification) {
    final TestContainerClassLoaderFactory testContainerClassLoaderFactory =
        new TestContainerClassLoaderFactory(extraBootPackages, artifactUrlClassification.getContainerUrls().toArray(new URL[0]));

    ArtifactClassLoader containerClassLoader =
        createContainerArtifactClassLoader(testContainerClassLoaderFactory, artifactUrlClassification);

    ClassLoaderLookupPolicy childClassLoaderLookupPolicy = testContainerClassLoaderFactory.getContainerClassLoaderLookupPolicy();

    RegionClassLoader regionClassLoader =
        new RegionClassLoader("Region", new URL[0], containerClassLoader.getClassLoader(), childClassLoaderLookupPolicy);


    List<ArtifactClassLoader> filteredPluginsArtifactClassLoaders = new ArrayList<>();
    final List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters = new ArrayList<>();
    final List<ArtifactClassLoader> pluginsArtifactClassLoaders = new ArrayList<>();
    if (!artifactUrlClassification.getPluginClassificationUrls().isEmpty()) {
      filteredPluginsArtifactClassLoaders =
          createPluginClassLoaders(regionClassLoader, childClassLoaderLookupPolicy, artifactUrlClassification,
                                   pluginsArtifactClassLoaders, pluginArtifactClassLoaderFilters);
    }

    // TODO(pablo.kraan): isolation - the real factory passes the list of plugin classLoaders as is a param of the classlaoder
    // constructor
    ArtifactClassLoader appClassLoader =
        createApplicationArtifactClassLoader(regionClassLoader, childClassLoaderLookupPolicy, artifactUrlClassification);

    regionClassLoader.addClassLoader(appClassLoader, emptySet(), emptySet());

    for (int i = 0; i < filteredPluginsArtifactClassLoaders.size(); i++) {

      final ArtifactClassLoaderFilter classLoaderFilter = pluginArtifactClassLoaderFilters.get(i);
      regionClassLoader.addClassLoader(filteredPluginsArtifactClassLoaders.get(i), classLoaderFilter.getExportedClassPackages(),
                                       classLoaderFilter.getExportedResources());
    }

    return new ArtifactClassLoaderHolder(containerClassLoader, pluginsArtifactClassLoaders, regionClassLoader);
  }

  /**
   * Creates an {@link ArtifactClassLoader} for the container. The difference between a mule container {@link ArtifactClassLoader}
   * in standalone mode and this one is that it has to be aware that the parent class loader has all the URLs loaded in launcher
   * app class loader so it has to create a particular look policy to resolve classes as CHILD_FIRST.
   * <p/>
   * In order to do that a {@link FilteringArtifactClassLoader} resolve is created with and empty look policy (meaning that
   * CHILD_FIRST strategy will be used) for the {@link URL}s that are going to be exposed from the container class loader. This
   * would be the parent class loader for the container so instead of going directly the launcher application class loader that
   * has access to the whole classpath this filtering class loader will resolve only the classes for the {@link URL}s defined to
   * be in the container.
   *
   * @param testContainerClassLoaderFactory {@link TestContainerClassLoaderFactory} that has the logic to create a container class
   *        loader
   * @param artifactUrlClassification the classifications to get plugins {@link URL}s
   * @return an {@link ArtifactClassLoader} for the container
   */
  protected ArtifactClassLoader createContainerArtifactClassLoader(TestContainerClassLoaderFactory testContainerClassLoaderFactory,
                                                                   ArtifactUrlClassification artifactUrlClassification) {
    logClassLoaderUrls("CONTAINER", artifactUrlClassification.getContainerUrls());
    MuleArtifactClassLoader launcherArtifact =
        new MuleArtifactClassLoader("launcher", new URL[0], IsolatedClassLoaderFactory.class.getClassLoader(),
                                    new MuleClassLoaderLookupPolicy(Collections.emptyMap(), Collections.<String>emptySet()));
    final List<MuleModule> muleModules = Collections.<MuleModule>emptyList();
    ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory()
        .create(testContainerClassLoaderFactory.getBootPackages(), muleModules);

    return testContainerClassLoaderFactory
        .createContainerClassLoader(new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher));
  }

  /**
   * For each plugin defined in the classification it will create an {@link ArtifactClassLoader} with the name defined in
   * classification. For extension plugins it will also create the filter based on the extension manifest file. For a plain plugin
   * it will collect the exported packages and resources for creating the filter from its mule-module.properties file. <pr/> It
   * also creates a sharedLibs plugin without any library so far, in order to be a similar representation as mule's container has
   * and also to allow the hierarchy process to look for classes from its parent (see {@link CompositeClassLoader} that doesn't
   * delegate to its parent). <pr/> With the given list of created class loader plugins it will finally create a
   * {@link CompositeClassLoader} and return it.
   *
   * @param parent the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactUrlClassification the url classifications to get plugins {@link URL}s
   * @param pluginsArtifactClassLoaders a list where it would append each {@link ArtifactClassLoader} created for a plugin in
   *        order to allow access them later
   * @param pluginArtifactClassLoaderFilters
   * @return a {@link CompositeClassLoader} that represents the plugin class loaders.
   */
  // TODO(pablo.kraan): isolation - add javadoc for the new param
  // TODO(pablo.kraan): isolation - this method sucks. Need to separate creation of the different elements needed after the method
  // return (CL, filtered CL and filters)
  protected List<ArtifactClassLoader> createPluginClassLoaders(ClassLoader parent,
                                                               ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                               ArtifactUrlClassification artifactUrlClassification,
                                                               List<ArtifactClassLoader> pluginsArtifactClassLoaders,
                                                               List<ArtifactClassLoaderFilter> pluginArtifactClassLoaderFilters) {
    final List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();

    for (PluginUrlClassification pluginUrlClassification : artifactUrlClassification.getPluginClassificationUrls()) {
      logClassLoaderUrls("PLUGIN (" + pluginUrlClassification.getName() + ")", pluginUrlClassification.getUrls());
      MuleArtifactClassLoader pluginCL =
          new MuleArtifactClassLoader(pluginUrlClassification.getName(), pluginUrlClassification.getUrls().toArray(new URL[0]),
                                      parent, childClassLoaderLookupPolicy);
      pluginsArtifactClassLoaders.add(pluginCL);

      Collection<String> exportedPackagesProperty;
      Collection<String> exportedResourcesProperty;
      URL manifestUrl = pluginCL.findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
      if (manifestUrl != null) {
        logger.debug("Plugin '{}' has extension descriptor therefore it will be handled as an extension",
                     pluginUrlClassification.getName());
        ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
        exportedPackagesProperty = extensionManifest.getExportedPackages();
        exportedResourcesProperty = extensionManifest.getExportedResources();
      } else {
        logger.debug("Plugin '{}' will be handled as standard plugin, it is not an extension", pluginUrlClassification.getName());
        ClassLoader pluginArtifactClassLoaderToDiscoverModules =
            new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null);
        List<MuleModule> modules =
            withContextClassLoader(pluginArtifactClassLoaderToDiscoverModules,
                                   () -> new ClasspathModuleDiscoverer(pluginArtifactClassLoaderToDiscoverModules).discover());
        MuleModule module = validatePluginModule(pluginUrlClassification.getName(), modules);

        exportedPackagesProperty = module.getExportedPackages();
        exportedResourcesProperty = module.getExportedPackages();
      }
      String exportedPackages = exportedPackagesProperty.stream().collect(Collectors.joining(", "));
      final String exportedResources = exportedResourcesProperty.stream().collect(Collectors.joining(", "));
      ArtifactClassLoaderFilter filter = classLoaderFilterFactory.create(exportedPackages, exportedResources);
      if (!pluginUrlClassification.getExportClasses().isEmpty()) {
        filter = new TestArtifactClassLoaderFilter(filter, pluginUrlClassification.getExportClasses());
      }
      pluginArtifactClassLoaderFilters.add(filter);
      pluginClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter));
    }
    return pluginClassLoaders;
  }

  /**
   * Creates an {@link ArtifactClassLoader} for the application.
   *
   * @param parent the parent class loader to be assigned to the new one created here
   * @param childClassLoaderLookupPolicy look policy to be used
   * @param artifactUrlClassification the url classifications to get plugins urls
   * @return the {@link ArtifactClassLoader} to be used for running the test
   */
  protected ArtifactClassLoader createApplicationArtifactClassLoader(ClassLoader parent,
                                                                     ClassLoaderLookupPolicy childClassLoaderLookupPolicy,
                                                                     ArtifactUrlClassification artifactUrlClassification) {
    logClassLoaderUrls("APP", artifactUrlClassification.getApplicationUrls());
    return new MuleArtifactClassLoader("app", artifactUrlClassification.getApplicationUrls().toArray(new URL[0]), parent,
                                       childClassLoaderLookupPolicy);
  }

  /**
   * Logs the {@link List} of {@link URL}s for the classLoaderName
   *
   * @param classLoaderName the name of the {@link ClassLoader} to be logged
   * @param urls {@link List} of {@link URL}s that are going to be used for the {@link ClassLoader}
   */
  protected void logClassLoaderUrls(final String classLoaderName, final List<URL> urls) {
    StringBuilder builder = new StringBuilder(classLoaderName).append(" classloader urls: [");
    urls.stream().forEach(e -> builder.append("\n").append(" ").append(e.getFile()));
    builder.append("\n]");
    logClassLoadingTrace(builder.toString());
  }

  /**
   * Logs the message with info severity if {@link org.mule.runtime.core.api.config.MuleProperties#MULE_LOG_VERBOSE_CLASSLOADING}
   * is set or trace severity
   *
   * @param message the message to be logged
   */
  private void logClassLoadingTrace(String message) {
    if (isVerboseClassLoading()) {
      logger.info(message);
    } else if (logger.isTraceEnabled()) {
      logger.trace(message);
    }
  }

  /**
   * @return true if {@link org.mule.runtime.core.api.config.MuleProperties#MULE_LOG_VERBOSE_CLASSLOADING} is set to true
   */
  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  /**
   * Validates that only one module should be discovered. A plugin cannot have inside another plugin that holds a
   * {@code mule-module.properties} for the time being.
   *
   * @param pluginName the plugin name
   * @param discoveredModules {@link MuleModule} discovered
   * @return the first Module from the list due to there should be only one module.
   */
  private MuleModule validatePluginModule(String pluginName, List<MuleModule> discoveredModules) {
    if (discoveredModules.size() == 0) {
      throw new IllegalStateException(pluginName
          + " doesn't have in its classpath a mule-module.properties to define what packages and resources should expose");
    }
    if (discoveredModules.size() > 1) {
      throw new IllegalStateException(pluginName + " has more than one mule-module.properties, composing plugins is not allowed");
    }
    return discoveredModules.get(0);
  }

}
