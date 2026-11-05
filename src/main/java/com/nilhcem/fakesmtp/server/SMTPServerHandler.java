package com.nilhcem.fakesmtp.server;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import com.nilhcem.fakesmtp.core.exception.BindPortException;
import com.nilhcem.fakesmtp.core.exception.OutOfRangePortException;

/**
 * Starts and stops the SMTP server.
 * <p>
 * A SubEthaSMTP {@code SMTPServer} can only be started once, so a new instance is created
 * when the server is started again after having been stopped. The same {@link MailSaver}
 * and {@link MailListener} are shared by every instance.
 * </p>
 *
 * @author Nilhcem
 * @since 1.0
 */
public enum SMTPServerHandler {
	INSTANCE;

	private static final Logger LOGGER = LoggerFactory.getLogger(SMTPServerHandler.class);
	private final MailSaver mailSaver = new MailSaver();
	private final MailListener myListener = new MailListener(mailSaver);
	private volatile SMTPServer smtpServer = createSmtpServer();
	private boolean serverUsed = false;

	SMTPServerHandler() {
	}

	/**
	 * Starts the server on the port and address specified in parameters.
	 *
	 * @param port the SMTP port to be opened.
	 * @param bindAddress the address to bind to. null means bind to all.
	 * @throws BindPortException when the port can't be opened.
	 * @throws OutOfRangePortException when port is out of range.
	 * @throws IllegalArgumentException when port is out of range.
	 */
	public synchronized void startServer(int port, InetAddress bindAddress) throws BindPortException, OutOfRangePortException {
		LOGGER.debug("Starting server on port {}", port);
		if (serverUsed && !smtpServer.isRunning()) {
			smtpServer = createSmtpServer();
			serverUsed = false;
		}
		try {
			smtpServer.setBindAddress(bindAddress);
			smtpServer.setPort(port);
			smtpServer.start();
			serverUsed = true;
		} catch (RuntimeException exception) {
			String message = String.valueOf(exception.getMessage());
			if (message.contains("BindException")) { // Can't open port
				LOGGER.error("{}. Port {}", message, port);
				throw new BindPortException(exception, port);
			} else if (message.contains("out of range")) { // Port out of range
				LOGGER.error("Port {} out of range.", port);
				throw new OutOfRangePortException(exception, port);
			} else { // Unknown error
				LOGGER.error("", exception);
				throw exception;
			}
		}
	}

	/**
	 * Stops the server.
	 * <p>
	 * If the server is not started, does nothing special.
	 * </p>
	 */
	public synchronized void stopServer() {
		if (smtpServer.isRunning()) {
			LOGGER.debug("Stopping server");
			smtpServer.stop();
		}
	}

	/**
	 * Returns whether the SMTP server is currently accepting connections.
	 *
	 * @return {@code true} if the server is running.
	 */
	public boolean isRunning() {
		return smtpServer.isRunning();
	}

	/**
	 * Returns the {@code MailSaver} object.
	 *
	 * @return the {@code MailSaver} object.
	 */
	public MailSaver getMailSaver() {
		return mailSaver;
	}

	/**
	 * Returns the {@code SMTPServer} object.
     *
     * @return the {@code SMTPServer} object.
	 */
	public SMTPServer getSmtpServer() {
		return smtpServer;
	}

	private SMTPServer createSmtpServer() {
		return new SMTPServer(new SimpleMessageListenerAdapter(myListener), new SMTPAuthHandlerFactory());
	}
}
