/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.MetadataManager;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectOutputMetadataTestCase extends AbstractDbIntegrationTestCase {

  public SelectOutputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/pending/select/select-output-metadata-config.xml"};
  }

  @Test
  public void returnsSelectOutputMetadata() throws Exception {
    doSelectMetadataTest("selectMetadata");
  }

  @Test
  public void returnsSelectStreamingOutputMetadata() throws Exception {
    doSelectMetadataTest("selectStreamingMetadata");
  }

  private void doSelectMetadataTest(String flowName) throws RegistrationException {
    MetadataManager metadataManager = muleContext.getRegistry().lookupObject(MuleMetadataManager.class);
    MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager
        .getMetadata(new ProcessorId(flowName, "0"), newKey("select * from PLANET").build());

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getOutputMetadata().isSuccess(), is(true));
    assertThat(metadata.get().getOutputMetadata().get().getPayloadMetadata().isSuccess(), is(true));
    ArrayType output = (ArrayType) metadata.get().getOutputMetadata().get().getPayloadMetadata().get().getType();
    ObjectType record = (ObjectType) output.getType();

    assertThat(record.getFields().size(), equalTo(3));
    Optional<ObjectFieldType> id = record.getFieldByName("ID");
    assertThat(id.isPresent(), is(true));
    assertThat(id.get().getValue(), equalTo(testDatabase.getIdFieldOutputMetaDataType()));

    Optional<ObjectFieldType> position = record.getFieldByName("POSITION");
    assertThat(position.isPresent(), is(true));
    assertThat(position.get().getValue(), equalTo(testDatabase.getPositionFieldOutputMetaDataType()));

    Optional<ObjectFieldType> name = record.getFieldByName("NAME");
    assertThat(name.isPresent(), is(true));
    assertThat(name.get().getValue(), equalTo(typeBuilder.stringType().build()));
  }
}
