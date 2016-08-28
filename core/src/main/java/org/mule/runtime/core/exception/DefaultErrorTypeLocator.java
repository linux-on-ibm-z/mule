/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;

public class DefaultErrorTypeLocator {

  public static ErrorTypeLocator createDefaultComponentErrorTypeLocator(ErrorTypeRepository errorTypeRepository) {
    return ErrorTypeLocator.builder(errorTypeRepository)
        .defaultExceptionMapper(ExceptionMapper.builder()
            .addExceptionMapping(TransformerMessagingException.class,
                                 errorTypeRepository.lookupErrorType("mule", "TRANSFORMATION"))
            .addExceptionMapping(ExpressionRuntimeException.class, errorTypeRepository.lookupErrorType("mule", "EXPRESSION"))
            .addExceptionMapping(Exception.class, errorTypeRepository.lookupErrorType("mule", "UNKNOWN"))
            .build())
        .build();
  }

}
