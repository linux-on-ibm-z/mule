/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;

import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMetadataResolver implements MetadataContentResolver<String> {

  private BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  private Map<Integer, MetadataType> dbToMetaDataType;
  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  public static final String DUPLICATE_COLUMN_LABEL_ERROR =
      "Query metadata contains multiple columns with the same label. Define column aliases to resolve this problem";

  @Override
  public MetadataType getContentMetadata(MetadataContext context, String query)
      throws MetadataResolvingException, ConnectionException {

    QueryTemplate queryTemplate = parseQuery(query);
    List<InputQueryParam> inputParams = queryTemplate.getInputParams();
    // No metadata when no input parameters
    if (inputParams.size() == 0) {
      return typeBuilder.anyType().build();
    }

    PreparedStatement statement = getStatement(context, queryTemplate);

    List<String> fieldNames = new ArrayList<String>();
    for (InputQueryParam inputParam : inputParams) {
      String field = getReferencedField(inputParam);
      if (field == null) {
        return typeBuilder.anyType().build();
      }
      fieldNames.add(field);
    }

    try {
      return getInputMetadataUsingStatementMetadata(statement, fieldNames);
    } catch (SQLException e) {
      return getStaticInputMetadata(fieldNames);
    }

  }

  protected QueryTemplate parseQuery(String query) {
    return new SimpleQueryTemplateParser().parse(query);
  }

  protected PreparedStatement getStatement(MetadataContext context, QueryTemplate query)
      throws ConnectionException, MetadataResolvingException {
    Optional<DbConnection> c = context.getConnection();
    PreparedStatement statement;
    try {
      statement = c.get().getJdbcConnection().prepareStatement(query.getSqlText());
    } catch (SQLException e) {
      e.printStackTrace();
      throw new MetadataResolvingException(e.getMessage(), FailureCode.UNKNOWN, e);
    }
    return statement;
  }

  private boolean isValidIdentifier(String value) {
    for (char c : value.toCharArray()) {
      if (!Character.isJavaIdentifierPart(c)) {
        return false;
      }
    }

    return true;
  }

  private MetadataType getStaticInputMetadata(List<String> fieldNames) {
    Map<String, MetadataType> recordModels = new HashMap<>();

    for (String fieldName : fieldNames) {
      recordModels.put(fieldName, getDataTypeMetadataModel(Types.VARCHAR));
    }

    ObjectTypeBuilder record = typeBuilder.objectType().id("recordModel");
    recordModels.entrySet().forEach(e -> record.addField().key(e.getKey()).value(e.getValue()));
    return record.build();
  }

  private MetadataType getInputMetadataUsingStatementMetadata(PreparedStatement statement, List<String> fieldNames)
      throws SQLException {
    ParameterMetaData parameterMetaData = statement.getParameterMetaData();

    Map<String, MetadataType> recordModels = new HashMap<>();
    int i = 1;
    for (String fieldName : fieldNames) {
      int dataType = parameterMetaData.getParameterType(i++);
      recordModels.put(fieldName, getDataTypeMetadataModel(dataType));
    }

    ObjectTypeBuilder record = typeBuilder.objectType().id("recordModel");
    recordModels.entrySet().forEach(e -> record.addField().key(e.getKey()).value(e.getValue()));
    return record.build();
  }

  private String getReferencedField(InputQueryParam inputParam) {
    if (inputParam.getValue() == null || !(inputParam.getValue() instanceof String)) {
      return null;
    }
    String value = (String) inputParam.getValue();

    if (value.startsWith("#[") && value.endsWith("]")) {
      value = value.substring(2, value.length() - 1);

      if (value.startsWith("payload.")) {
        value = value.substring(8);
        if (isValidIdentifier(value)) {
          return value;
        }
      } else if ((value.startsWith("payload['") && value.endsWith("']"))
          || (value.startsWith("payload[\"") && value.endsWith("\"]"))) {
        value = value.substring(9, value.length() - 2);
        if (isValidIdentifier(value)) {
          return value;
        }
      }

    }

    return null;
  }

  protected MetadataType getDataTypeMetadataModel(int columnTypeName, String columnClassName) {
    if (columnTypeName == Types.JAVA_OBJECT) {
      return typeLoader.load(columnClassName).orElse(typeBuilder.anyType().build());
    }

    return getDataTypeMetadataModel(columnTypeName);
  }

  protected MetadataType getDataTypeMetadataModel(int columnTypeName) {
    if (dbToMetaDataType == null) {
      synchronized (this) {
        if (dbToMetaDataType == null) {
          initializeDbToMetaDataType();
        }
      }
    }

    return dbToMetaDataType.getOrDefault(columnTypeName, typeBuilder.anyType().build());
  }

  private void initializeDbToMetaDataType() {
    dbToMetaDataType = new HashMap<>();

    dbToMetaDataType.put(Types.BIT, typeBuilder.booleanType().build());
    dbToMetaDataType.put(Types.TINYINT, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.SMALLINT, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.INTEGER, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.BIGINT, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.FLOAT, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.REAL, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.DOUBLE, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.NUMERIC, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.DECIMAL, typeBuilder.numberType().build());
    dbToMetaDataType.put(Types.CHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.VARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.LONGNVARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.DATE, typeLoader.load(Date.class));
    dbToMetaDataType.put(Types.TIME, typeLoader.load(Time.class));
    dbToMetaDataType.put(Types.TIMESTAMP, typeLoader.load(Timestamp.class));
    dbToMetaDataType.put(Types.BINARY, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.VARBINARY, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.LONGVARBINARY, typeBuilder.binaryType().build());
    dbToMetaDataType.put(Types.NULL, typeBuilder.nullType().build());
    dbToMetaDataType.put(Types.OTHER, typeBuilder.anyType().build());
    dbToMetaDataType.put(Types.DISTINCT, typeBuilder.anyType().build());
    dbToMetaDataType.put(Types.STRUCT, typeLoader.load(Struct.class));
    dbToMetaDataType.put(Types.ARRAY, typeBuilder.arrayType().of().anyType().build());
    dbToMetaDataType.put(Types.BLOB, typeLoader.load(Blob.class));
    dbToMetaDataType.put(Types.CLOB, typeLoader.load(Clob.class));
    dbToMetaDataType.put(Types.REF, typeLoader.load(Ref.class));
    dbToMetaDataType.put(Types.DATALINK, typeLoader.load(URL.class));
    dbToMetaDataType.put(Types.BOOLEAN, typeBuilder.booleanType().build());
    dbToMetaDataType.put(Types.ROWID, typeLoader.load(RowId.class));
    dbToMetaDataType.put(Types.NCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.NVARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.LONGNVARCHAR, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.NCLOB, typeBuilder.stringType().build());
    dbToMetaDataType.put(Types.SQLXML, typeLoader.load(SQLXML.class));
  }
}
