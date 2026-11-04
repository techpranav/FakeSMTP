package com.nilhcem.fakesmtp.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.mail.TestEmailFactory;

/**
 * Injects the built-in sample email through the same save path as real SMTP mail.
 *
 * @since 2.1
 */
public final class TestEmailSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestEmailSender.class);

	private TestEmailSender() {
		throw new UnsupportedOperationException();
	}

	public static void sendSample() throws IOException {
		try {
			SMTPServerHandler.INSTANCE.getMailSaver().saveEmailAndNotify(
					"tester@mailbox.local",
					"developer@example.com",
					TestEmailFactory.createSampleMessage());
		} catch (RuntimeException e) {
			LOGGER.warn("Could not inject the sample email", e);
			throw new IOException("Could not create the sample email", e);
		}
	}
}
