/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.config.processor;

import org.mule.runtime.module.db.internal.config.domain.param.StaticQueryParamResolverFactoryBean;
import org.mule.runtime.module.db.internal.domain.executor.BulkUpdateExecutorFactory;
import org.mule.runtime.module.db.internal.domain.query.QueryType;
import org.mule.runtime.module.db.internal.metadata.PreparedBulkUpdateMetadataProvider;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class PreparedBulkUpdateProcessorBeanDefinitionParser extends AbstractSingleQueryProcessorDefinitionParser
{

    private final List<QueryType> validQueryTypes;

    public PreparedBulkUpdateProcessorBeanDefinitionParser(List<QueryType> validQueryTypes)
    {
        this.validQueryTypes = validQueryTypes;
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return BulkUpdateMessageProcessorFactoryBean.class;
    }

    @Override
    protected void doParseElement(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        super.doParseElement(element, context, builder);

        builder.addConstructorArgValue(validQueryTypes);
        parseAutoGeneratedKeys(element, builder);
        parseMetadataProvider(element, builder);
        builder.addConstructorArgValue(queryBean);
    }

    @Override
    protected BeanDefinition getParamResolverBeanDefinition()
    {
        BeanDefinitionBuilder sqlParamResolverFactory = BeanDefinitionBuilder.genericBeanDefinition(StaticQueryParamResolverFactoryBean.class);
        return sqlParamResolverFactory.getBeanDefinition();
    }

    @Override
    protected Object createExecutorFactory(Element element)
    {
        BeanDefinitionBuilder executorFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(BulkUpdateExecutorFactory.class);

        executorFactoryBean.addConstructorArgValue(parseStatementFactory(element));

        return executorFactoryBean.getBeanDefinition();
    }

    @Override
    protected Object getMetadataProvider()
    {
        BeanDefinitionBuilder metadataProviderBuilder = BeanDefinitionBuilder.genericBeanDefinition(PreparedBulkUpdateMetadataProvider.class);
        metadataProviderBuilder.addConstructorArgValue(dbConfigResolverFactoryBeanDefinition);
        metadataProviderBuilder.addConstructorArgValue(queryBean);
        metadataProviderBuilder.addConstructorArgValue(autoGeneratedKeyStrategy);

        return metadataProviderBuilder.getBeanDefinition();
    }
}
