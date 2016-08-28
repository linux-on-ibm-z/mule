/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Optional.empty;
import static org.mule.runtime.core.util.Preconditions.checkState;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.HashedMap;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.config.ComponentIdentifier;

public class ErrorTypeLocator {

  private final ErrorTypeRepository errorTypeRepository;
  private ExceptionMapper defaultExceptionMapper;
  private Map<ComponentIdentifier, ExceptionMapper> componentExceptionMappers;

  private ErrorTypeLocator(ExceptionMapper defaultExceptionMapper,
                           Map<ComponentIdentifier, ExceptionMapper> componentExceptionMappers,
                           ErrorTypeRepository errorTypeRepository) {
    this.defaultExceptionMapper = defaultExceptionMapper;
    this.componentExceptionMappers = componentExceptionMappers;
    this.errorTypeRepository = errorTypeRepository;
  }

  public ErrorType getAnyErrorType() {
    return errorTypeRepository.getAnyErrorType();
  }

  public ErrorType findErrorType(Exception exception) {
    return defaultExceptionMapper.resolveErrorType(exception).get();
  }

  public ErrorType findComponentErrorType(ComponentIdentifier componentIdentifier, Exception exception) {
    ExceptionMapper exceptionMapper = componentExceptionMappers.get(componentIdentifier);
    Optional<ErrorType> errorType = empty();
    if (exceptionMapper != null) {
      errorType = exceptionMapper.resolveErrorType(exception);
    }
    return errorType.orElseGet(() -> defaultExceptionMapper.resolveErrorType(exception).get());
  }

  public static Builder builder(ErrorTypeRepository errorTypeRepository) {
    return new Builder(errorTypeRepository);
  }

  public static class Builder {

    private final ErrorTypeRepository errorTypeRepository;

    public Builder(ErrorTypeRepository errorTypeRepository) {
      this.errorTypeRepository = errorTypeRepository;
    }

    private ExceptionMapper defaultExceptionMapper;
    private Map<ComponentIdentifier, ExceptionMapper> componentExceptionMappers = new HashedMap();

    public Builder defaultExceptionMapper(ExceptionMapper exceptionMapper) {
      this.defaultExceptionMapper = exceptionMapper;
      return this;
    }

    public Builder addComponentExceptionMapper(ComponentIdentifier componentIdentifier, ExceptionMapper exceptionMapper) {
      this.componentExceptionMappers.put(componentIdentifier, exceptionMapper);
      return this;
    }

    public ErrorTypeLocator build() {
      checkState(defaultExceptionMapper != null, "default exception mapper cannot not be null");
      checkState(componentExceptionMappers != null, "component exception mappers cannot not be null");
      return new ErrorTypeLocator(defaultExceptionMapper, componentExceptionMappers, errorTypeRepository);
    }
  }
}
