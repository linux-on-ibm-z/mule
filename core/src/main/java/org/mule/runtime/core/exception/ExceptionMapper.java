/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.MuleRuntimeException;

public class ExceptionMapper {

  private Set<ExceptionMapping> exceptionMappings = new TreeSet<>();

  private ExceptionMapper(Set<ExceptionMapping> exceptionMappings) {
    this.exceptionMappings = exceptionMappings;
  }

  public Optional<ErrorType> resolveErrorType(Exception exception) {
    return exceptionMappings.stream()
        .filter(exceptionMapping -> exceptionMapping.matches(exception))
        .findFirst().map(matchingExceptionMapping -> matchingExceptionMapping.getErrorType());
  }


  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Builder() {}

    private Set<ExceptionMapping> exceptionMappings = new TreeSet<>();

    public Builder addExceptionMapping(Class<? extends Exception> exceptionType, ErrorType errorType) {
      if (!exceptionMappings.add(new ExceptionMapping(exceptionType, errorType))) {
        throw new MuleRuntimeException(createStaticMessage(format("Cannot build an %s with a repeated mapping for exception %s",
                                                                  ExceptionMapper.class.getName(),
                                                                  exceptionType.getClass().getName())));
      }
      return this;
    }

    public ExceptionMapper build() {
      return new ExceptionMapper(exceptionMappings);
    }
  }
}
