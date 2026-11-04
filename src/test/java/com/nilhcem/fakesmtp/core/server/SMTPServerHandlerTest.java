package com.nilhcem.fakesmtp.core.server;

import static org.junit.Assert.*;

import java.net.InetAddress;

import org.junit.Test;

import com.nilhcem.fakesmtp.core.exception.BindPortException;
import com.nilhcem.fakesmtp.core.exception.OutOfRangePortException;
import com.nilhcem.fakesmtp.server.SMTPServerHandler;

public class SMTPServerHandlerTest {
	@Test
	public void uniqueInstance() {
		SMTPServerHandler a = SMTPServerHandler.INSTANCE;
		SMTPServerHandler b = SMTPServerHandler.INSTANCE;
		assertSame(a, b);
	}

	@Test(expected = OutOfRangePortException.class)
	public void testOutOfRangePort() throws BindPortException, OutOfRangePortException {
		SMTPServerHandler.INSTANCE.startServer(9999999, null);
	}

	@Test
	public void stopShouldDoNothingIfServerIsAlreadyStopped() {
		SMTPServerHandler.INSTANCE.stopServer();
		SMTPServerHandler.INSTANCE.stopServer();
		SMTPServerHandler.INSTANCE.stopServer();
	}

	@Test
	public void serverCanBeStoppedAndStartedAgain() throws Exception {
		InetAddress local = InetAddress.getByName("127.0.0.1");
		try {
			SMTPServerHandler.INSTANCE.startServer(2631, local);
			assertTrue(SMTPServerHandler.INSTANCE.isRunning());
			SMTPServerHandler.INSTANCE.stopServer();
			assertFalse(SMTPServerHandler.INSTANCE.isRunning());
			SMTPServerHandler.INSTANCE.startServer(2631, local);
			assertTrue(SMTPServerHandler.INSTANCE.isRunning());
		} finally {
			SMTPServerHandler.INSTANCE.stopServer();
		}
	}
}
