package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AttachmentFilesTest {

	@Test
	public void stripsPathCharactersFromTheFileName() {
		assertEquals("report.pdf", AttachmentFiles.safeName("../secret/report.pdf"));
		assertEquals("b_c.txt", AttachmentFiles.safeName("a\\b:c.txt"));
		assertEquals("attachment", AttachmentFiles.safeName(".."));
		assertEquals("attachment", AttachmentFiles.safeName("   "));
		assertFalse(AttachmentFiles.safeName("nested/name").contains("/"));
		assertFalse(AttachmentFiles.safeName("nested\\name").contains("\\"));
	}
}
