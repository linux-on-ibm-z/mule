/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.extension.api.dsql.DsqlQuery;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.dsql.Field;

import java.util.List;

final class DsqlQueryMetadataResolver implements MetadataOutputResolver<DsqlQuery> {

  private final QueryEntityResolver entityResolver;

  DsqlQueryMetadataResolver(RuntimeComponentModel component) {
    entityResolver = component.getQueryEntityResolverFactory().getQueryEntityResolver();
  }

  @Override
  public MetadataType getOutputMetadata(MetadataContext context, DsqlQuery key)
      throws MetadataResolvingException, ConnectionException {

    MetadataType entityMetadata = entityResolver.getEntityMetadata(context, key.getType().getName());

    final List<Field> fields = key.getFields();
    if (fields.size() == 1 && fields.get(0).getName().equals("*")) {
      return entityMetadata;
    }

    BaseTypeBuilder<?> builder = BaseTypeBuilder.create(JAVA);
    entityMetadata.accept(new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        builder.anyType();
      }

      @Override
      public void visitObject(ObjectType objectType) {
        ObjectTypeBuilder<?> objectTypeBuilder = builder.objectType();
        objectType.getFields()
            .stream()
            .filter(p -> fields.stream().anyMatch(f -> f.getName().equalsIgnoreCase(p.getKey().getName().getLocalPart())))
            .forEach(p -> {
              ObjectFieldTypeBuilder<?> field = objectTypeBuilder.addField();
              field.key(p.getKey().getName());
              field.value(p.getValue());
            });
      }
    });

    return builder.build();
  }
}
