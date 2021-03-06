/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.agent;

import org.mule.runtime.core.AbstractAgent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.context.notification.ComponentMessageNotification;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.ManagementNotification;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.context.notification.SecurityNotification;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractNotificationLoggerAgent</code> Receives Mule server notifications and logs them and can optionally route them to
 * an endpoint. This agent will only receive notifications for notification events that are enabled. The notifications that are
 * enabled are determined by the {@link MuleContextBuilder} that is used or configuration mechanisms that may override these
 * values.
 */
public abstract class AbstractNotificationLoggerAgent extends AbstractAgent {

  /**
   * The logger used for this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private boolean ignoreManagerNotifications = false;
  private boolean ignoreConnectionNotifications = false;
  private boolean ignoreSecurityNotifications = false;
  private boolean ignoreManagementNotifications = false;
  private boolean ignoreCustomNotifications = false;
  private boolean ignoreAdminNotifications = false;
  private boolean ignoreMessageNotifications = false;
  private boolean ignoreComponentMessageNotifications = false;
  private boolean ignoreMessageProcessorNotifications = true;

  protected Set<ServerNotificationListener<? extends ServerNotification>> listeners =
      new HashSet<ServerNotificationListener<? extends ServerNotification>>();

  protected AbstractNotificationLoggerAgent(String name) {
    super(name);
  }

  @Override
  public void start() throws MuleException {
    // nothing to do
  }

  @Override
  public void stop() throws MuleException {
    // nothing to do
  }

  @Override
  public void dispose() {
    for (ServerNotificationListener<?> listener : listeners) {
      muleContext.unregisterListener(listener);
    }
  }

  public boolean isIgnoreManagerNotifications() {
    return ignoreManagerNotifications;
  }

  public void setIgnoreManagerNotifications(boolean ignoreManagerNotifications) {
    this.ignoreManagerNotifications = ignoreManagerNotifications;
  }

  public boolean isIgnoreMessageNotifications() {
    return ignoreMessageNotifications;
  }

  public void setIgnoreMessageNotifications(boolean ignoreMessageNotifications) {
    this.ignoreMessageNotifications = ignoreMessageNotifications;
  }

  public boolean isIgnoreSecurityNotifications() {
    return ignoreSecurityNotifications;
  }

  public void setIgnoreSecurityNotifications(boolean ignoreSecurityNotifications) {
    this.ignoreSecurityNotifications = ignoreSecurityNotifications;
  }

  public boolean isIgnoreManagementNotifications() {
    return ignoreManagementNotifications;
  }

  public void setIgnoreManagementNotifications(boolean ignoreManagementNotifications) {
    this.ignoreManagementNotifications = ignoreManagementNotifications;
  }

  public boolean isIgnoreCustomNotifications() {
    return ignoreCustomNotifications;
  }

  public void setIgnoreCustomNotifications(boolean ignoreCustomNotifications) {
    this.ignoreCustomNotifications = ignoreCustomNotifications;
  }

  public boolean isIgnoreAdminNotifications() {
    return ignoreAdminNotifications;
  }

  public void setIgnoreAdminNotifications(boolean ignoreAdminNotifications) {
    this.ignoreAdminNotifications = ignoreAdminNotifications;
  }

  public boolean isIgnoreConnectionNotifications() {
    return ignoreConnectionNotifications;
  }

  public void setIgnoreConnectionNotifications(boolean ignoreConnectionNotifications) {
    this.ignoreConnectionNotifications = ignoreConnectionNotifications;
  }

  public boolean isIgnoreComponentMessageNotifications() {
    return ignoreComponentMessageNotifications;
  }

  public void setIgnoreComponentMessageNotifications(boolean ignoreComponentMessageNotifications) {
    this.ignoreComponentMessageNotifications = ignoreComponentMessageNotifications;
  }

  public boolean isIgnoreMessageProcessorNotifications() {
    return ignoreMessageProcessorNotifications;
  }

  public void setIgnoreMessageProcessorNotifications(boolean ignoreMessageProcessorNotifications) {
    this.ignoreMessageProcessorNotifications = ignoreMessageProcessorNotifications;
  }

  @Override
  public final void initialise() throws InitialisationException {
    doInitialise();
    if (!ignoreManagerNotifications) {
      ServerNotificationListener<MuleContextNotification> l = new MuleContextNotificationListener<MuleContextNotification>() {

        @Override
        public void onNotification(MuleContextNotification notification) {
          logEvent(notification);
        }
      };
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }
    if (!ignoreSecurityNotifications) {
      ServerNotificationListener<SecurityNotification> l = new SecurityNotificationListener<SecurityNotification>() {

        @Override
        public void onNotification(SecurityNotification notification) {
          logEvent(notification);
        }
      };
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }

    if (!ignoreManagementNotifications) {
      ServerNotificationListener<ManagementNotification> l = new ManagementNotificationListener<ManagementNotification>() {

        @Override
        public void onNotification(ManagementNotification notification) {
          logEvent(notification);
        }
      };
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }

    if (!ignoreCustomNotifications) {
      ServerNotificationListener<?> l = new CustomNotificationListener<ServerNotification>() {

        @Override
        public void onNotification(ServerNotification notification) {
          logEvent(notification);
        }
      };
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }

    if (!ignoreConnectionNotifications) {
      ServerNotificationListener<ConnectionNotification> l = new ConnectionNotificationListener<ConnectionNotification>() {

        @Override
        public void onNotification(ConnectionNotification notification) {
          logEvent(notification);
        }
      };
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }

    if (!ignoreMessageNotifications && !ignoreComponentMessageNotifications) {
      ServerNotificationListener<ComponentMessageNotification> l = notification -> logEvent(notification);
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }

    if (!ignoreMessageNotifications && !ignoreMessageProcessorNotifications) {
      ServerNotificationListener<MessageProcessorNotification> l = notification -> logEvent(notification);
      try {
        muleContext.registerListener(l);
      } catch (NotificationException e) {
        throw new InitialisationException(e, this);
      }
      listeners.add(l);
    }
  }

  protected abstract void doInitialise() throws InitialisationException;

  protected abstract void logEvent(ServerNotification e);
}
