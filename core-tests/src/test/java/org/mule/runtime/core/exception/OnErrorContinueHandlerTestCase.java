/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.util.StreamCloserService;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnErrorContinueHandlerTestCase extends AbstractMuleContextTestCase {

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS.get());
  @Mock
  private MessagingException mockException;

  private MuleEvent mockMuleEvent;

  private MuleMessage muleMessage = MuleMessage.builder().payload("").build();
  @Mock
  private StreamCloserService mockStreamCloserService;
  @Spy
  private TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
  @Spy
  private TestTransaction mockXaTransaction = new TestTransaction(mockMuleContext, true);

  private Flow flow;

  private MessageContext context;

  private OnErrorContinueHandler onErrorContinueHandler;

  @Before
  public void before() throws Exception {
    Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
    if (currentTransaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
    }
    flow = getTestFlow();
    onErrorContinueHandler = new OnErrorContinueHandler();
    onErrorContinueHandler.setMuleContext(mockMuleContext);
    onErrorContinueHandler.setFlowConstruct(flow);
    when(mockMuleContext.getStreamCloserService()).thenReturn(mockStreamCloserService);

    context = DefaultMessageContext.create(flow, TEST_CONNECTOR);
    mockMuleEvent = new DefaultMuleEvent(context, muleMessage, flow);
  }

  @Test
  public void testHandleExceptionWithNoConfig() throws Exception {
    configureXaTransactionAndSingleResourceTransaction();

    MuleEvent resultEvent = onErrorContinueHandler.handleException(mockException, mockMuleEvent);
    assertThat(resultEvent, is(mockMuleEvent));

    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
    verify(mockStreamCloserService).closeStream(any(Object.class));
  }

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected MuleContext muleContext;

  @Test
  public void testHandleExceptionWithConfiguredMessageProcessors() throws Exception {
    mockMuleEvent = spy(mockMuleEvent);

    onErrorContinueHandler
        .setMessageProcessors(asList(createSetStringMessageProcessor("A"), createSetStringMessageProcessor("B")));
    onErrorContinueHandler.initialise();
    final MuleEvent result = onErrorContinueHandler.handleException(mockException, mockMuleEvent);

    verify(mockMuleEvent, times(1)).setMessage(argThat(new ArgumentMatcher<MuleMessage>() {

      @Override
      public boolean matches(Object argument) {
        return ((MuleMessage) argument).getPayload().equals("A");
      }
    }));
    assertThat(result.getMessage().getPayload(), is("B"));
    assertThat(result.getError(), is(nullValue()));
  }

  @Test
  public void testHandleExceptionWithMessageProcessorsChangingEvent() throws Exception {
    MuleEvent lastEventCreated = new DefaultMuleEvent(context, muleMessage, flow);
    onErrorContinueHandler
        .setMessageProcessors(asList(createChagingEventMessageProcessor(new DefaultMuleEvent(context,
                                                                                             muleMessage, flow)),
                                     createChagingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.initialise();
    MuleEvent exceptionHandlingResult = onErrorContinueHandler.handleException(mockException, mockMuleEvent);
    assertThat(exceptionHandlingResult.getId(), is(lastEventCreated.getId()));
    assertThat(exceptionHandlingResult.getCorrelationId(), is(lastEventCreated.getCorrelationId()));
  }

  /**
   *  On fatal error, the exception strategies are not supposed to use MuleMessage.toString() as it could
   * potentially log sensible data.
   */
  @Test
  public void testMessageToStringNotCalledOnFailure() throws Exception {
    mockMuleEvent.setMessage(spy(muleMessage));
    mockMuleEvent = spy(mockMuleEvent);

    MuleEvent lastEventCreated = new DefaultMuleEvent(context, muleMessage, flow);
    onErrorContinueHandler
        .setMessageProcessors(asList(createFailingEventMessageProcessor(new DefaultMuleEvent(context,
                                                                                             muleMessage,
                                                                                             flow)),
                                     createFailingEventMessageProcessor(lastEventCreated)));
    onErrorContinueHandler.initialise();
    when(mockMuleEvent.getMessage().toString()).thenThrow(new RuntimeException("MuleMessage.toString() should not be called"));

    MuleEvent exceptionHandlingResult =
        exceptionHandlingResult = onErrorContinueHandler.handleException(mockException, mockMuleEvent);
  }

  private MessageProcessor createChagingEventMessageProcessor(final MuleEvent lastEventCreated) {
    return event -> lastEventCreated;
  }

  private MessageProcessor createFailingEventMessageProcessor(final MuleEvent lastEventCreated) {
    return event -> {
      throw new DefaultMuleException(mockException);
    };
  }

  private MessageProcessor createSetStringMessageProcessor(final String appendText) {
    return event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload(appendText).build());
      return event;
    };
  }

  private void configureXaTransactionAndSingleResourceTransaction() throws TransactionException {
    TransactionCoordination.getInstance().bindTransaction(mockXaTransaction);
    TransactionCoordination.getInstance().suspendCurrentTransaction();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
  }
}
