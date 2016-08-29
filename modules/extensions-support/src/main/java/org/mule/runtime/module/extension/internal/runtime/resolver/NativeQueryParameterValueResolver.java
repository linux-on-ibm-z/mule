/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.extension.api.dsql.DsqlParser.isDsqlQuery;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.extension.api.dsql.DsqlParser;
import org.mule.runtime.extension.api.dsql.DsqlQuery;
import org.mule.runtime.extension.api.introspection.dsql.QueryTranslator;

/**
 * Base class for a {@link ValueResolver} which needs to create instances of {@link NestedProcessor}, regardless of it being the
 * main return type or not
 *
 */
public final class NativeQueryParameterValueResolver implements ValueResolver<String> {

  private static final DsqlParser PARSER = DsqlParser.getInstance();

  private final String query;
  private final QueryTranslator translator;

  public NativeQueryParameterValueResolver(String query, QueryTranslator translator) {
    this.query = query;
    this.translator = translator;
  }

  @Override
  public String resolve(MuleEvent event) throws MuleException {
    return doTranslate();
  }

  public String doTranslate() {
    if (!isDsqlQuery(query)) {
      return query;
    }
    DsqlQuery parse = PARSER.parse(query);
    return parse.translate(translator);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
