/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.mule.runtime.core.message.ErrorTypeBuilder.GENERAL;
import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.MuleMessage;

public class DefaultErrorBuilder {

  private Exception exception;
  private String description;
  private String detailedDescription;
  private ErrorType errorType;
  private MuleMessage errorMessage;

  public DefaultErrorBuilder(Exception e) {
    this.exception = e;
    String exceptionDescription = e.getMessage() != null ? e.getMessage() : "unknown description";
    this.description = exceptionDescription;
    this.detailedDescription = exceptionDescription;
    this.errorType = GENERAL;
  }

  public DefaultErrorBuilder setException(Exception exception) {
    this.exception = exception;
    return this;
  }

  public DefaultErrorBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public DefaultErrorBuilder setDetailedDescription(String detailedDescription) {
    this.detailedDescription = detailedDescription;
    return this;
  }

  public DefaultErrorBuilder setErrorType(ErrorType errorType) {
    this.errorType = errorType;
    return this;
  }

  public void setErrorMessage(MuleMessage errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Error build() {
    checkState(exception != null, "error exception cannot be null");
    checkState(description != null, "description exception cannot be null");
    checkState(detailedDescription != null, "detailed description exception cannot be null");
    checkState(errorType != null, "errorType exception cannot be null");
    return new ErrorImplementation(exception, description, detailedDescription, errorType, errorMessage);
  }

  public static class ErrorImplementation implements Error {

    private Exception exception;
    private String description;
    private String detailedDescription;
    private ErrorType errorType;
    private MuleMessage muleMessage;

    private ErrorImplementation(Exception exception, String description, String detailedDescription, ErrorType errorType,
                                MuleMessage errorMessage) {
      this.exception = exception;
      this.description = description;
      this.detailedDescription = detailedDescription;
      this.errorType = errorType;
      this.muleMessage = errorMessage;
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public String getDetailedDescription() {
      return detailedDescription;
    }

    @Override
    public ErrorType getErrorType() {
      return errorType;
    }

    @Override
    public Throwable getException() {
      return exception;
    }

    @Override
    public MuleMessage getErrorMessage() {
      return muleMessage;
    }
  }

}
