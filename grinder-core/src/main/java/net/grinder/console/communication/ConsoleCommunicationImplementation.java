// Copyright (C) 2000 - 2013 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.communication;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.annotation.PreDestroy;

import net.grinder.common.TimeAuthority;
import net.grinder.communication.Acceptor;
import net.grinder.communication.Address;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.ServerReceiver;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.translation.Translations;
import net.grinder.util.thread.BooleanCondition;

/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 */
public final class ConsoleCommunicationImplementation
  implements ConsoleCommunication {

  private final Translations m_translations;

  private final ConsoleProperties m_properties;

  private final ErrorHandler m_errorHandler;

  private final TimeAuthority m_timeAuthority;

  private final long m_idlePollDelay;

  private final long m_inactiveClientTimeOut;

  private final MessageDispatchSender m_messageDispatcher =
      new MessageDispatchSender();

  private final BooleanCondition m_processing = new BooleanCondition();

  private final BooleanCondition m_shutdown = new BooleanCondition();

  private Acceptor m_acceptor = null;

  private ServerReceiver m_receiver = null;

  private FanOutServerSender m_sender = null;

  /**
   * Constructor that uses a default idlePollDelay.
   *
   * @param translations
   *          Translation service.
   * @param properties
   *          Console properties.
   * @param errorHandler
   *          Error handler.
   * @param timeAuthority
   *          Knows the time
   * @throws DisplayMessageConsoleException
   *           If properties are invalid.
   */
  public ConsoleCommunicationImplementation(final Translations translations,
                                            final ConsoleProperties properties,
                                            final ErrorHandler errorHandler,
                                            final TimeAuthority timeAuthority)
      throws DisplayMessageConsoleException {
    this(translations, properties, errorHandler, timeAuthority, 500, 30000);
  }

  /**
   * Constructor.
   *
   * @param translations
   *          Translation service.
   * @param properties
   *          Console properties.
   * @param errorHandler
   *          Error handler.
   * @param timeAuthority
   *          Knows the time
   * @param idlePollDelay
   *          Time in milliseconds that our ServerReceiver threads should sleep
   *          for if there's no incoming messages.
   * @param inactiveClientTimeOut
   *          How long before we consider a client connection that presents no
   *          data to be inactive.
   * @throws DisplayMessageConsoleException
   *           If properties are invalid.
   */
  public ConsoleCommunicationImplementation(final Translations translations,
                                            final ConsoleProperties properties,
                                            final ErrorHandler errorHandler,
                                            final TimeAuthority timeAuthority,
                                            final long idlePollDelay,
                                            final long inactiveClientTimeOut)
      throws DisplayMessageConsoleException {

    m_translations = translations;
    m_properties = properties;
    m_errorHandler = errorHandler;
    m_timeAuthority = timeAuthority;
    m_idlePollDelay = idlePollDelay;
    m_inactiveClientTimeOut = inactiveClientTimeOut;

    properties.addPropertyChangeListener(
        new PropertyChangeListener() {
          @Override
          public void propertyChange(final PropertyChangeEvent event) {
            final String property = event.getPropertyName();

            if (property.equals(ConsoleProperties.CONSOLE_HOST_PROPERTY) ||
                property.equals(ConsoleProperties.CONSOLE_PORT_PROPERTY)) {
              reset();
            }
          }
        });

    reset();
  }

  private void reset() {
    try {
      if (m_acceptor != null) {
        m_acceptor.shutdown();
      }
    }
    catch (final CommunicationException e) {
      m_errorHandler.handleException(e);
      return;
    }

    if (m_sender != null) {
      m_sender.shutdown();
    }

    if (m_receiver != null) {
      m_receiver.shutdown();

      // Wait until we're deaf. This requires that some other thread executes
      // processOneMessage(). We can't suck on m_receiver ourself as there may
      // be valid pending messages queued up.

      m_processing.await(false);
    }

    if (m_shutdown.get()) {
      return;
    }

    try {
      m_acceptor = new Acceptor(m_properties.getConsoleHost(),
        m_properties.getConsolePort(),
        1,
        m_timeAuthority);
    }
    catch (final CommunicationException e) {
      m_errorHandler.handleException(
          new DisplayMessageConsoleException(
            m_translations.translate("communication/local-bind-error"), e));

      // Wake up any threads waiting in processOneMessage().
      m_processing.wakeUpAllWaiters();

      return;
    }

    final Thread acceptorProblemListener =
        new Thread("Acceptor problem listener") {
          @Override
          public void run() {
            while (true) {
              final Exception exception = m_acceptor.getPendingException();

              if (exception == null) {
                // Acceptor is shutting down.
                break;
              }

              m_errorHandler.handleException(exception);
            }
          }
        };

    acceptorProblemListener.setDaemon(true);
    acceptorProblemListener.start();

    m_receiver = new ServerReceiver();

    try {
      m_receiver.receiveFrom(m_acceptor,
        new ConnectionType[] {
                              ConnectionType.AGENT,
                              ConnectionType.CONSOLE_CLIENT,
                              ConnectionType.WORKER,
        },
        5,
        m_idlePollDelay,
        m_inactiveClientTimeOut);
    }
    catch (final CommunicationException e) {
      throw new AssertionError(e);
    }

    try {
      m_sender = new FanOutServerSender(m_acceptor, ConnectionType.AGENT, 3);
    }
    catch (final Acceptor.ShutdownException e) {
      // I am tempted to make this an assertion.
      // Currently, this condition can only happen if the accept() call throws
      // an exception. I guess this might reasonably happen if a network i/f
      // goes away immediately after we create the Acceptor. It's not easy for
      // us to reset ourselves at this point (I certainly don't want to
      // recurse), so we notify the user. Users could get going again by
      // reseting new console address info, but most likely they'll just restart
      // the console.
      m_processing.wakeUpAllWaiters();
      m_errorHandler.handleException(e);
      return;
    }

    m_processing.set(true);
  }

  /**
   * Returns the message dispatch registry which callers can use to register new
   * message handlers.
   *
   * @return The registry.
   */
  @Override
  public MessageDispatchRegistry getMessageDispatchRegistry() {
    return m_messageDispatcher;
  }

  /**
   * Shut down communication.
   */
  @Override
  @PreDestroy
  public void shutdown() {
    m_shutdown.set(true);
    m_processing.set(false);
    reset();
  }

  /**
   * Wait to receive a message, then process it.
   *
   * @return <code>true</code> if we processed a message successfully;
   *         <code>false</code> if we've been shut down.
   * @see #shutdown()
   */
  @Override
  public boolean processOneMessage() {
    while (true) {
      if (m_shutdown.get()) {
        return false;
      }

      if (m_processing.await(true)) {
        try {
          final Message message = m_receiver.waitForMessage();

          if (message == null) {
            // Current receiver has been shut down.
            m_processing.set(false);
          }
          else {
            m_messageDispatcher.send(message);
            return true;
          }
        }
        catch (final CommunicationException e) {
          // The receive or send failed. We only set m_processing to false when
          // our receiver has been shut down.
          m_errorHandler.handleException(e);
        }
      }
    }
  }

  /**
   * The number of connections that have been accepted and are still active.
   * Used by the unit tests.
   *
   * @return The number of accepted connections.
   */
  public int getNumberOfConnections() {
    return m_acceptor == null ? 0 : m_acceptor.getNumberOfConnections();
  }

  /**
   * Send the given message to the agent processes (which may pass it on to
   * their workers).
   *
   * <p>
   * Any errors that occur will be handled with the error handler.
   * </p>
   *
   * @param message
   *          The message to send.
   */
  @Override
  public void sendToAgents(final Message message) {

    final String errorText =
        m_translations.translate("communication/send-error");

    if (m_sender == null) {
      m_errorHandler.handleErrorMessage(errorText);
    }
    else {
      try {
        m_sender.send(message);
      }
      catch (final CommunicationException e) {
        m_errorHandler
            .handleException(new DisplayMessageConsoleException(errorText, e));
      }
    }
  }

  /**
   * Send the given message to the given agent processes (which may pass it on
   * to its workers).
   *
   * <p>
   * Any errors that occur will be handled with the error handler.
   * </p>
   *
   * @param address
   *          The address to which the message should be sent.
   * @param message
   *          The message to send.
   */
  @Override
  public void sendToAddressedAgents(
    final Address address,
    final Message message) {

    final String errorText =
        m_translations.translate("communication/send-error");

    if (m_sender == null) {
      m_errorHandler.handleErrorMessage(errorText);
    }
    else {
      try {
        m_sender.send(address, message);
      }
      catch (final CommunicationException e) {
        m_errorHandler
            .handleException(new DisplayMessageConsoleException(errorText, e));
      }
    }
  }
}
