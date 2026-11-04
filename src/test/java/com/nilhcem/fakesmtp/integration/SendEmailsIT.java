package com.nilhcem.fakesmtp.integration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.nilhcem.fakesmtp.core.Configuration;
import com.nilhcem.fakesmtp.core.exception.BindPortException;
import com.nilhcem.fakesmtp.core.exception.OutOfRangePortException;
import com.nilhcem.fakesmtp.core.test.TestConfig;
import com.nilhcem.fakesmtp.server.SMTPServerHandler;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SendEmailsIT {

	private static final Logger logger = LoggerFactory.getLogger(SendEmailsIT.class);
	private static boolean startedByTest;

	@BeforeClass
	public static void displayInfo() throws BindPortException, OutOfRangePortException {
		logger.info("Launching integration tests...");
		if (smtpReady()) {
			logger.info("Using the SMTP server already listening on port {}.", TestConfig.PORT_INTEGRATION_TESTS);
			return;
		}
		logger.info("Starting an embedded SMTP server on port {}.", TestConfig.PORT_INTEGRATION_TESTS);
		SMTPServerHandler.INSTANCE.startServer(TestConfig.PORT_INTEGRATION_TESTS, InetAddress.getLoopbackAddress());
		startedByTest = true;
	}

	@AfterClass
	public static void stopEmbeddedServer() {
		if (startedByTest) {
			SMTPServerHandler.INSTANCE.stopServer();
		}
	}

	private static boolean smtpReady() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(TestConfig.HOST, TestConfig.PORT_INTEGRATION_TESTS), 500);
			socket.setSoTimeout(1000);
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
			String line = reader.readLine();
			return line != null && line.startsWith("220");
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	public void sendSimpleTestEmail() throws EmailException {
		Email email = new SimpleEmail();
		email.setHostName(TestConfig.HOST);
		email.setSmtpPort(TestConfig.PORT_INTEGRATION_TESTS);
		email.setFrom("user@gmail.com");
		email.setSubject("Simple email");
		email.setMsg("This is a simple plain text email :-)");
		email.addTo("foo@bar.com");
		email.send();
	}

	@Test
	public void sendEmailWithAttachment() throws EmailException {
		// Create the attachment
		EmailAttachment attachment = new EmailAttachment();
		attachment.setPath("src/main/resources" + Configuration.INSTANCE.get("application.icon.path"));
		attachment.setDisposition(EmailAttachment.ATTACHMENT);
		attachment.setDescription("Image file");
		attachment.setName("icon.gif");

		// Create the email message
		MultiPartEmail email = new MultiPartEmail();
		email.setHostName(TestConfig.HOST);
		email.setSmtpPort(TestConfig.PORT_INTEGRATION_TESTS);
		email.addTo("jdoe@somewhere.org", "John Doe");
		email.setFrom("me@example.org", "Me");
		email.setSubject("File attachment");
		email.setMsg("This email contains an enclosed file.");

		// Add the attachment
		email.attach(attachment);

		// Send the email
		email.send();
	}

	@Test
	public void sendHTMLFormattedEmail() throws EmailException {
		// Create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(TestConfig.HOST);
		email.setSmtpPort(TestConfig.PORT_INTEGRATION_TESTS);
		email.addTo("jdoe@somewhere.org", "John Doe");
		email.setFrom("me@example.org", "Me");
		email.setSubject("HTML email");

		// Set the HTML message
		email.setHtmlMsg("<html><body>This is an <b>HTML</b> email.<br /><br /></body></html>");

		// Set the alternative message
		email.setTextMsg("Your email client does not support HTML messages");

		// Send the email
		email.send();
	}

    @Test
    public void sendEmailWithBase64Subject() throws EmailException {
        Email email = new SimpleEmail();
        email.setHostName(TestConfig.HOST);
        email.setSmtpPort(TestConfig.PORT_INTEGRATION_TESTS);
        email.setFrom("spammy@example.org");
        email.addTo("foo@bar.com");
        email.setSubject("=?UTF-8?B?4pyIIEJvc3RvbiBhaXJmYXJlIGRlYWxzIC0gd2hpbGUgdGhleSBsYXN0IQ==?=");
        email.setMsg("Not really interesting, huh?");
        email.send();
    }

	@Test
	public void sendEmailToManyRecipientsWithTwoHeaders() throws EmailException {
		Email email = new SimpleEmail();
		email.setHostName(TestConfig.HOST);
		email.setSmtpPort(TestConfig.PORT_INTEGRATION_TESTS);
		email.setFrom("info@example.com");
		email.addTo("test1@example.com");
		email.addTo("test2@example.com");
		email.addHeader("Foo", "Bar");
		email.addHeader("Foo2", "Bar2");
		email.setSubject("Hi");
		email.setMsg("Just to check if everything is OK");
		email.send();
	}

	@Test
	public void sendEmailWithDots() throws EmailException {
		Email email = new SimpleEmail();
		email.setDebug(true);
		email.setHostName(TestConfig.HOST);
		email.setSmtpPort(TestConfig.PORT_INTEGRATION_TESTS);
		email.setFrom("user@example.com");
		email.addTo("foo@example.com");
		email.setSubject("Two dots separated with a new line");
		email.setMsg(".\n.");
		email.send();
	}
}
