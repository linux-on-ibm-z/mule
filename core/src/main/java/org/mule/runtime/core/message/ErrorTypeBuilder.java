/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.ErrorType;

public class ErrorTypeBuilder {

  private static final String MULE_NAMESPACE = "mule";
  private static final String ANY_STRING_REPRESENTATION = "ANY";
  //TODO re-think if unknown is needed
  private static final String UNKNOWN_STRING_REPRESENTATION = "UNKNOWN";
  private static final String GENERAL_STRING_REPRESENTATION = "GENERAL";

  public static final ErrorType ANY =
      new ErrorTypeBuilder().setNamespace(MULE_NAMESPACE).setStringRepresentation(ANY_STRING_REPRESENTATION).build();
  public static final ErrorType GENERAL = new ErrorTypeBuilder().setNamespace(MULE_NAMESPACE)
      .setStringRepresentation(GENERAL_STRING_REPRESENTATION).setParentErrorType(ANY).build();

  private String stringRepresentation;
  private String namespace;
  private ErrorType parentErrorType;

  public ErrorTypeBuilder setStringRepresentation(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
    return this;
  }

  public ErrorTypeBuilder setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public ErrorTypeBuilder setParentErrorType(ErrorType parentErrorType) {
    this.parentErrorType = parentErrorType;
    return this;
  }

  public ErrorType build() {
    checkState(stringRepresentation != null, "string representation cannot be null");
    checkState(namespace != null, "namespace representation cannot be null");
    if (!stringRepresentation.equals(ANY_STRING_REPRESENTATION)) {
      checkState(parentErrorType != null, "parent error type cannot be null");
    }
    return new ErrorTypeImplementation(stringRepresentation, namespace, parentErrorType);
  }

  public static class ErrorTypeImplementation implements ErrorType {

    private String stringRepresentation;
    private String namespace;
    private ErrorType parentErrorType;

    private ErrorTypeImplementation(String stringRepresentation, String namespace, ErrorType parentErrorType) {
      this.stringRepresentation = stringRepresentation;
      this.namespace = namespace;
      this.parentErrorType = parentErrorType;
    }

    @Override
    public String getStringRepresentation() {
      return stringRepresentation;
    }

    @Override
    public String getNamespace() {
      return namespace;
    }

    @Override
    public ErrorType getParentErrorType() {
      return parentErrorType;
    }
  }

}
