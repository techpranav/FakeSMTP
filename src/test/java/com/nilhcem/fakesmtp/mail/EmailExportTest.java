package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

import org.apache.poi.hsmf.MAPIMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.nilhcem.fakesmtp.model.EmailModel;
import com.nilhcem.fakesmtp.model.InboxMessage;

import ch.astorm.jotlmsg.OutlookMessage;

public class EmailExportTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void writesGenuineMsgWithRecipientsAndHtmlAttachment() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				folder.newFile("sample.eml").getAbsolutePath(), false);
		InboxMessage message = message(parsed, raw);

		File msg = folder.newFile("sample.msg");
		EmailExport.saveAsMsg(message, msg);
		assertTrue(msg.length() > 500);

		OutlookMessage loaded = new OutlookMessage(msg);
		assertTrue(loaded.getSubject().contains("MailBox sample") || loaded.getSubject().length() > 0);
		assertNotNull(loaded.getPlainTextBody());
		assertTrue(loaded.getAttachments().size() >= 2);
		for (ch.astorm.jotlmsg.OutlookMessageAttachment attachment : loaded.getAttachments()) {
			assertTrue("HTML must be message body, not attachment: " + attachment.getName(),
					attachment.getName() == null || !attachment.getName().equalsIgnoreCase("message-body.html"));
		}
		assertEquals(1, loaded.getRecipients(ch.astorm.jotlmsg.OutlookMessageRecipient.Type.TO).size());
		assertEquals(1, loaded.getRecipients(ch.astorm.jotlmsg.OutlookMessageRecipient.Type.CC).size());
		MAPIMessage poi = new MAPIMessage(msg.getAbsolutePath());
		assertTrue(poi.getDisplayFrom().toLowerCase().contains("tester")
				|| poi.getDisplayFrom().toLowerCase().contains("mailbox"));
		assertTrue(poi.getDisplayTo().toLowerCase().contains("developer"));
		assertTrue(poi.getDisplayCC().toLowerCase().contains("qa"));
		assertTrue("MSG must contain binary HTML body stream",
				hasStream(msg, "__substg1.0_10130102"));
		assertTrue("MSG must contain RTF compressed body",
				hasStream(msg, "__substg1.0_10090102"));
	}

	private static boolean hasStream(File msg, String name) throws Exception {
		org.apache.poi.poifs.filesystem.POIFSFileSystem fs =
				new org.apache.poi.poifs.filesystem.POIFSFileSystem(msg, true);
		try {
			return fs.getRoot().hasEntry(name);
		} finally {
			fs.close();
		}
	}

	@Test
	public void overdueApprovalMsgOpensWithFullHeaders() throws Exception {
		String raw = TestEmailFactory.buildOverdueRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "Enovia@technia.com",
				"KTApprove02@kla-tencor.com", new Date(), null, false);
		InboxMessage message = message(parsed, raw);
		File msg = folder.newFile("overdue.msg");
		EmailExport.saveAsMsg(message, msg);
		MAPIMessage poi = new MAPIMessage(msg.getAbsolutePath());
		assertTrue(poi.getSubject().contains("Overdue"));
		assertTrue(poi.getDisplayFrom().toLowerCase().contains("enovia"));
		assertTrue(poi.getDisplayTo().toLowerCase().contains("approver"));
		assertTrue(poi.getDisplayCC().toLowerCase().contains("derek")
				|| poi.getDisplayCC().toLowerCase().contains("manager"));
	}

	@Test
	public void writesOriginalEmlBytes() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				null, false);
		InboxMessage message = message(parsed, raw);
		File eml = folder.newFile("sample-out.eml");
		EmailExport.saveAsEml(message, eml);
		String saved = new String(Files.readAllBytes(eml.toPath()), StandardCharsets.UTF_8);
		assertTrue(saved.contains("multipart/mixed") || saved.contains("text/html"));
		assertTrue(saved.contains("sample.txt"));
	}

	@Test
	public void writesFullScreenshotWithMetadata() throws Exception {
		String raw = TestEmailFactory.buildRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "tester@mailbox.local", "developer@example.com", new Date(),
				null, false);
		InboxMessage message = message(parsed, raw);
		File png = folder.newFile("sample.png");
		EmailExport.saveScreenshot(message, png);
		assertTrue(png.length() > 1000);
		String base = EmailExport.suggestedFileBase(message, "MailBox_Email");
		assertTrue(base.startsWith("MailBox_Email_"));
		assertTrue(base.matches(".*_\\d{8}_\\d{6}$"));
	}

	@Test
	public void overdueScreenshotIncludesTableAndCharts() throws Exception {
		String raw = TestEmailFactory.buildOverdueRaw();
		ParsedEmail parsed = EmailParser.parse(raw, "Enovia@technia.com",
				"KTApprove02@kla-tencor.com", new Date(), null, false);
		String page = ScreenshotDocument.compose(parsed);
		assertTrue(page.contains("From:"));
		assertTrue(page.contains("Enovia@technia.com"));
		assertTrue(page.contains("To:"));
		assertTrue(page.contains("KTApprove02@kla-tencor.com"));
		assertTrue(page.contains("CC:"));
		assertTrue(page.contains("E00827"));
		assertTrue(page.contains("Assignment distribution"));
		java.awt.image.BufferedImage image = EmailScreenshot.capture(parsed);
		assertTrue("overdue full capture height=" + image.getHeight(), image.getHeight() > 800);
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

	private static InboxMessage message(ParsedEmail parsed, String raw) {
		EmailModel email = new EmailModel();
		email.setFrom("tester@mailbox.local");
		email.setTo("developer@example.com");
		email.setSubject(parsed.getSubject());
		email.setReceivedDate(new Date());
		email.setEmailStr(raw);
		email.setFilePath("memory-export-test-" + System.nanoTime());
		return new InboxMessage(email, parsed, false);
	}
}
