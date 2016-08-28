/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

public class DefaultErrorTypeRepositoryFactory {

  public static ErrorTypeRepository createDefaultErrorTypeRepository() {
    ErrorTypeRepository errorTypeRepository = new ErrorTypeRepository();
    errorTypeRepository.addErrorType("mule", "CONNECTIVITY", errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType("mule", "TRANSFORMATION", errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType("mule", "EXPRESSION", errorTypeRepository.getAnyErrorType());
    errorTypeRepository.addErrorType("mule", "UNKNOWN", errorTypeRepository.getAnyErrorType());
    return errorTypeRepository;
  }

}
