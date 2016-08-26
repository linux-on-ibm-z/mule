/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
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

public class SelectInputMetadataTestCase extends AbstractDbIntegrationTestCase {

  public SelectInputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/pending/select/select-input-metadata-config.xml"};
  }

  @Test
  public void returnsNullSelectMetadataUnParameterizedQuery() throws Exception {
    doUnresolvedMetadataTest("selectMetadata", "select * from PLANET");
  }

  @Test
  public void returnsNullSelectInputMetadataFromNotSupportedParameterizedQuery() throws Exception {
    /*
    <db:select>
    <db:sql>select * from PLANET where id = :id and name = :name</db:sql>
    <db:input-parameters>
    	<db:input-parameter paramName="id" value="#[payload.id]" />
    	<db:input-parameter paramName="name" value="#[payload.name]" />
    </db:input-parameters>
    </db:select>
     */
    doUnresolvedMetadataTest("selectMetadata",
                             "select * from PLANET where id = #[payload.id] and name =#[message.outboundProperties.updateCount]");
  }

  @Test
  public void returnsSelectInputMetadataFromBeanParameterizedQuery() throws Exception {
    /*
    <db:select>
    <db:sql>select * from PLANET where id = :id and name = :name</db:sql>
    <db:input-parameters>
    	<db:input-parameter paramName="id" value="#[payload.id]" />
    	<db:input-parameter paramName="name" value="#[payload.name]" />
    </db:input-parameters>
    </db:select>
     */
    //TODO input-parameter values can't be used to resolve the input types, thus inputList is
    doUnresolvedMetadataTest("selectMetadata", "select * from PLANET where id = :id and name = :name");
  }

  @Test
  public void returnsSelectInputMetadataFromMapParameterizedQuery() throws Exception {
    /*
    <db:select>
    <db:sql>select * from PLANET where id = :id and name = :name</db:sql>
    <db:input-parameters>
    	<db:input-parameter paramName="id" value="#[payload.id]" />
    	<db:input-parameter paramName="name" value="#[payload.name]" />
    </db:input-parameters>
    </db:select>
     */
    doUnresolvedMetadataTest("selectMetadata", "select * from PLANET where id = :id and name = :name");
  }

  private void doUnresolvedMetadataTest(String flowName, String query) throws RegistrationException {

    MetadataManager metadataManager = muleContext.getRegistry().lookupObject(MuleMetadataManager.class);
    MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager
        .getMetadata(new ProcessorId(flowName, "0"), MetadataKeyBuilder.newKey(query).build());

    //Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
    //List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
    //AbstractSingleQueryDbMessageProcessor queryMessageProcessor =
    //    (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);
    //Result<MetaData> inputMetaData = queryMessageProcessor.getInputMetaData();

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getContentMetadata().isPresent(), is(true));
    assertThat(metadata.get().getContentMetadata().get().isSuccess(), is(true));
    assertThat(metadata.get().getContentMetadata().get().get().getType(), is(typeBuilder.anyType().build()));
  }

  private void doResolvedMetadataTest(String flowName, String query) throws RegistrationException {
    //Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);

    //List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
    //AbstractSingleQueryDbMessageProcessor queryMessageProcessor =
    //    (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);
    //Result<MetaData> inputMetaData = queryMessageProcessor.getInputMetaData();

    //DefinedMapMetaDataModel mapDataModel = (DefinedMapMetaDataModel) inputMetaData.get().getPayload();
    //assertThat(mapDataModel.getKeys().size(), equalTo(2));
    //MetaDataModel id = mapDataModel.getValueMetaDataModel("id");
    //assertThat(id.getDataType(), equalTo(testDatabase.getIdFieldInputMetaDataType()));
    //MetaDataModel data = mapDataModel.getValueMetaDataModel("name");
    //assertThat(data.getDataType(), equalTo(DataType.STRING));

    MetadataManager metadataManager = muleContext.getRegistry().lookupObject(MuleMetadataManager.class);
    MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager
        .getMetadata(new ProcessorId(flowName, "0"), MetadataKeyBuilder.newKey(query).build());

    assertThat(metadata.isSuccess(), is(true));
    assertThat(metadata.get().getContentMetadata().isPresent(), is(true));
    assertThat(metadata.get().getContentMetadata().get().isSuccess(), is(true));
    ObjectType type = (ObjectType) metadata.get().getContentMetadata().get().get().getType();
    assertThat(type.getFields().size(), equalTo(2));
    Optional<ObjectFieldType> id = type.getFieldByName("id");
    assertThat(id.isPresent(), is(true));
    assertThat(id.get().getValue(), equalTo(testDatabase.getIdFieldInputMetaDataType()));
    Optional<ObjectFieldType> name = type.getFieldByName("name");
    assertThat(name.isPresent(), is(true));
    assertThat(name.get().getValue(), equalTo(typeBuilder.stringType().build()));
  }
}
