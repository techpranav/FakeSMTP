package com.nilhcem.fakesmtp.gui.inbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmailAddressActionsTest {

	@Test
	public void validatesCommonAddresses() {
		assertTrue(EmailAddressActions.isValidAddress("developer@example.com"));
		assertTrue(EmailAddressActions.isValidAddress("ada.lovelace+tag@example.co.uk"));
		assertFalse(EmailAddressActions.isValidAddress("not-an-email"));
		assertFalse(EmailAddressActions.isValidAddress("a@@b.com"));
		assertFalse(EmailAddressActions.isValidAddress(""));
		assertFalse(EmailAddressActions.isValidAddress(null));
	}
}
