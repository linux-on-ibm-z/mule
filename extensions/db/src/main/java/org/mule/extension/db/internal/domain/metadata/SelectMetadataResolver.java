/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SelectMetadataResolver extends AbstractMetadataResolver implements MetadataOutputResolver<String> {

  private BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  public static final String DUPLICATE_COLUMN_LABEL_ERROR =
      "Query metadata contains multiple columns with the same label. Define column aliases to resolve this problem";

  @Override
  public MetadataType getOutputMetadata(MetadataContext context, String query)
      throws MetadataResolvingException, ConnectionException {

    PreparedStatement statement = getStatement(context, parseQuery(query));

    ResultSetMetaData statementMetaData;
    try {
      statementMetaData = statement.getMetaData();
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), FailureCode.UNKNOWN, e);
    }

    if (statementMetaData == null) {
      throw new MetadataResolvingException("Driver did not return metadata for the provided SQL", FailureCode.UNKNOWN);
    }

    Map<String, MetadataType> recordModels = new HashMap<>();
    try {
      for (int i = 1; i <= statementMetaData.getColumnCount(); i++) {
        int columnType = statementMetaData.getColumnType(i);
        recordModels.put(statementMetaData.getColumnLabel(i),
                         getDataTypeMetadataModel(columnType, statementMetaData.getColumnClassName(i)));
      }
      if (statementMetaData.getColumnCount() != recordModels.size()) {
        throw new MetadataResolvingException(DUPLICATE_COLUMN_LABEL_ERROR, FailureCode.UNKNOWN);
      }
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), FailureCode.UNKNOWN, e);
    }


    ObjectTypeBuilder record = typeBuilder.objectType().id("recordModel");
    recordModels.entrySet().forEach(e -> record.addField().key(e.getKey()).value(e.getValue()));

    return typeBuilder.arrayType().of(record).build();
  }
}
