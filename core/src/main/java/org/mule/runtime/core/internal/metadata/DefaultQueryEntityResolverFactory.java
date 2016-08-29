/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.getClassName;
import static org.mule.runtime.core.util.ClassUtils.instanciateClass;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.introspection.metadata.QueryEntityResolverFactory;


/**
 * @since 4.0
 */
public final class DefaultQueryEntityResolverFactory implements QueryEntityResolverFactory {

  private final static String CREATION_ERROR_MASK = "Could not create QueryEntityMetadataResolver of type %s";
  private final QueryEntityResolver resolver;

  public DefaultQueryEntityResolverFactory(Class<? extends QueryEntityResolver> resolver) {
    try {
      this.resolver = instanciateClass(resolver);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(CREATION_ERROR_MASK, getClassName(resolver)), e);
    }
  }

  @Override
  public QueryEntityResolver getQueryEntityResolver() {
    return resolver;
  }
}
