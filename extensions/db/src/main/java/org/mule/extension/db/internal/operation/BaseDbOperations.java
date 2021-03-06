/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.api.param.ParameterizedStatementDefinition;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.autogeneratedkey.AutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.autogeneratedkey.ColumnIndexAutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.autogeneratedkey.ColumnNameAutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.autogeneratedkey.DefaultAutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.autogeneratedkey.NoAutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.executor.UpdateExecutor;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.query.QueryType;
import org.mule.extension.db.internal.domain.statement.QueryStatementFactory;
import org.mule.extension.db.internal.resolver.query.ParameterizedQueryResolver;
import org.mule.extension.db.internal.resolver.query.QueryResolver;

import com.google.common.base.Joiner;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class with common functionality for Database operations
 *
 * @since 4.0
 */
abstract class BaseDbOperations {

  protected static final int DEFAULT_FETCH_SIZE = 10;
  private static final Logger LOGGER = LoggerFactory.getLogger(DmlOperations.class);
  protected final QueryResolver<ParameterizedStatementDefinition> queryResolver = new ParameterizedQueryResolver<>();

  protected QueryStatementFactory getStatementFactory(StatementAttributes statementAttributes,
                                                      boolean streaming,
                                                      QuerySettings settings) {

    QueryStatementFactory statementFactory = new QueryStatementFactory();

    if (statementAttributes != null) {
      if (statementAttributes.getMaxRows() != null) {
        statementFactory.setMaxRows(statementAttributes.getMaxRows());
      }

      if (statementAttributes.getFetchSize() != null) {
        statementFactory.setFetchSize(statementAttributes.getFetchSize());
      } else if (streaming) {
        LOGGER.warn("Streaming mode needs to configure fetchSize property. Using default value: " + DEFAULT_FETCH_SIZE);
        statementFactory.setFetchSize(DEFAULT_FETCH_SIZE);
      }
    }

    statementFactory.setQueryTimeout(new Long(settings.getQueryTimeoutUnit().toSeconds(settings.getQueryTimeout())).intValue());

    return statementFactory;
  }

  protected StatementResult executeUpdate(QueryDefinition query,
                                          StatementAttributes statementAttributes,
                                          AutoGeneratedKeyAttributes autoGeneratedKeyAttributes,
                                          DbConnection connection,
                                          Query resolvedQuery)
      throws SQLException {
    QueryStatementFactory statementFactory = getStatementFactory(statementAttributes, false, query.getSettings());
    return (StatementResult) new UpdateExecutor(statementFactory)
        .execute(connection, resolvedQuery, getAutoGeneratedKeysStrategy(autoGeneratedKeyAttributes));
  }

  protected Query resolveQuery(QueryDefinition query, DbConnector connector, DbConnection connection, QueryType... validTypes) {
    final Query resolvedQuery = queryResolver.resolve(query, connector, connection);
    validateQueryType(resolvedQuery.getQueryTemplate(), asList(validTypes));

    return resolvedQuery;
  }

  protected void validateQueryType(QueryTemplate queryTemplate, List<QueryType> validTypes) {
    if (validTypes == null || !validTypes.contains(queryTemplate.getType())) {
      throw new IllegalArgumentException(format("Query type must be one of [%s] but query '%s' is of type '%s'",
                                                Joiner.on(", ").join(validTypes), queryTemplate.getSqlText(),
                                                queryTemplate.getType()));
    }
  }

  protected AutoGeneratedKeyStrategy getAutoGeneratedKeysStrategy(AutoGeneratedKeyAttributes keyAttributes) {

    if (keyAttributes == null) {
      return new NoAutoGeneratedKeyStrategy();
    }

    if (keyAttributes.isAutoGeneratedKeys()) {

      final List<Integer> columnIndexes = keyAttributes.getAutoGeneratedKeysColumnIndexes();
      final List<String> columnNames = keyAttributes.getAutoGeneratedKeysColumnNames();

      if (!isEmpty(columnIndexes)) {
        int[] indexes = new int[columnIndexes.size()];
        int i = 0;
        for (int index : columnIndexes) {
          indexes[i++] = index;
        }
        return new ColumnIndexAutoGeneratedKeyStrategy(indexes);
      } else if (!isEmpty(columnNames)) {
        return new ColumnNameAutoGeneratedKeyStrategy(columnNames.stream().toArray(String[]::new));
      } else {
        return new DefaultAutoGeneratedKeyStrategy();
      }
    } else {
      return new NoAutoGeneratedKeyStrategy();
    }
  }
}
