package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.nilhcem.fakesmtp.model.EmailModel;
import com.nilhcem.fakesmtp.model.InboxMessage;

public class EmailExportTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void writesMsgEmlAndScreenshot() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				folder.newFile("sample.eml").getAbsolutePath(), false);
		EmailModel email = new EmailModel();
		email.setFrom("tester@mailbox.local");
		email.setTo("developer@example.com");
		email.setSubject(parsed.getSubject());
		email.setReceivedDate(new Date());
		email.setEmailStr(raw);
		email.setFilePath("memory-export-test");
		InboxMessage message = new InboxMessage(email, parsed, false);

		File msg = folder.newFile("sample.msg");
		File eml = folder.newFile("sample-out.eml");
		File png = folder.newFile("sample.png");
		EmailExport.saveAsMsg(message, msg);
		EmailExport.saveAsEml(message, eml);
		EmailExport.saveScreenshot(message, png);

		assertTrue(msg.length() > 100);
		assertTrue(eml.length() > 100);
		assertTrue(png.length() > 100);
	}

	@Test
	public void sampleFactoryIncludesAttachmentsAndHtml() {
		String raw = TestEmailFactory.buildRaw();
		assertTrue(raw.contains("text/html"));
		assertTrue(raw.contains("sample.txt"));
		assertTrue(raw.contains("sample.csv"));
		assertTrue(raw.contains("https://"));
		assertTrue(raw.contains("<table"));
	}
}
