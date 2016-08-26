/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.Map;

public class InsertMetadataResolver extends AbstractMetadataResolver {

  private BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  private Map<Integer, MetadataType> dbToMetaDataType;
  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  public static final String DUPLICATE_COLUMN_LABEL_ERROR =
      "Query metadata contains multiple columns with the same label. Define column aliases to resolve this problem";

}
