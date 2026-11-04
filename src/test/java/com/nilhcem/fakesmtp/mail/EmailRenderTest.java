package com.nilhcem.fakesmtp.mail;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import javax.imageio.ImageIO;

import org.junit.Test;

public class EmailRenderTest {

	@Test
	public void rendersTheOverdueApprovalEmail() throws Exception {
		String html = new String(resource("emails/overdue.html"), StandardCharsets.UTF_8);
		String raw = "From: ENOVIA USER <Enovia@technia.com>\r\n"
				+ "To: Approver 02 <KTApprove02@kla-tencor.com>\r\n"
				+ "Cc: DEREK HARTL <derek.hartl@technia.com>\r\n"
				+ "Subject: Overdue CR/CO/CCN/ISO Document(s) Approval\r\n"
				+ "Message-ID: <overdue@example>\r\n"
				+ "MIME-Version: 1.0\r\n"
				+ "Content-Type: text/html; charset=UTF-8\r\n"
				+ "Content-Transfer-Encoding: 8bit\r\n\r\n"
				+ html.replace("\n", "\r\n");
		ParsedEmail email = EmailParser.parse(raw, "Enovia@technia.com", "KTApprove02@kla-tencor.com", new Date(), null, false);

		assertTrue(email.isReadable());
		assertTrue(email.getSubject().contains("Overdue"));
		assertTrue(email.senderLine().contains("Enovia@technia.com"));
		assertTrue(email.getPreparedHtml().contains("E00827"));
		assertTrue(email.getPreparedHtml().contains("Task Assignee"));
		assertTrue(email.getPreparedHtml().contains("data:image/png;base64,"));
		assertFalse(email.getPreparedHtml().toLowerCase().contains("<script"));
		assertFalse(email.getPreparedHtml().contains("example.invalid"));
		assertTrue(email.getBlockedImages() > 0);
		assertTrue(email.getPreview().contains("auto-generated") || email.getPlainText().contains("auto-generated")
				|| email.getHtml().contains("auto-generated"));

		BufferedImage image = HtmlRenderer.render(email, 980, 1700);
		Path output = java.nio.file.Paths.get("target", "overdue-approval.png");
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());

		assertTrue(containsColor(image, 217, 226, 239));
		assertTrue(containsColor(image, 76, 120, 168) || containsColor(image, 245, 133, 24));
		assertNotNull(image);
	}

	@Test
	public void plainTextAndMalformedMessagesStayReadable() {
		ParsedEmail plain = EmailParser.parse("Subject: Hello\r\n\r\nHow are you?\r\n", "a@b.c", "d@e.f", new Date(), null, false);
		assertTrue(plain.getPlainText().contains("How are you?"));

		ParsedEmail broken = EmailParser.parse("this is not a mime message", "a@b.c", "d@e.f", new Date(), null, false);
		assertNotNull(broken.getRaw());
		assertTrue(broken.getRaw().contains("not a mime"));
	}

	@Test
	public void resolvesCidAndBlocksRemoteImages() {
		String raw = "From: me@example.org\r\nTo: you@example.org\r\nSubject: CID\r\n"
				+ "MIME-Version: 1.0\r\nContent-Type: multipart/related; boundary=BOUND\r\n\r\n"
				+ "--BOUND\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n"
				+ "<html><body><img src=\"cid:logo@example\"><img src=\"https://example.invalid/a.png\"></body></html>\r\n"
				+ "--BOUND\r\nContent-Type: image/gif\r\nContent-ID: <logo@example>\r\n"
				+ "Content-Transfer-Encoding: base64\r\nContent-Disposition: inline\r\n\r\n"
				+ "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7\r\n"
				+ "--BOUND--\r\n";
		ParsedEmail email = EmailParser.parse(raw, "me@example.org", "you@example.org", new Date(), null, false);
		assertTrue(email.getInlineImages().containsKey("logo@example"));
		assertTrue(email.getBlockedImages() > 0);
		MailUserAgent agent = new MailUserAgent();
		agent.setInlineImages(email.getInlineImages());
		assertNotNull(agent.resolveAndOpenStream("cid:logo@example"));
		assertNotNull(agent.resolveAndOpenStream("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"));
		org.junit.Assert.assertNull(agent.resolveAndOpenStream("file:///c:/windows/win.ini"));
		org.junit.Assert.assertNull(agent.resolveAndOpenStream("https://example.invalid/a.png"));
		String embedded = CidEmbedder.embed(email.getPreparedHtml(), email.getInlineImages());
		assertTrue(embedded.contains("data:image/gif;base64,"));
		assertFalse(embedded.toLowerCase().contains("cid:logo@example"));
	}

	private static byte[] resource(String name) throws Exception {
		try (InputStream input = EmailRenderTest.class.getClassLoader().getResourceAsStream(name)) {
			return org.apache.commons.io.IOUtils.toByteArray(input);
		}
	}

	private static boolean containsColor(BufferedImage image, int red, int green, int blue) {
		int tolerance = 36;
		for (int y = 0; y < image.getHeight(); y += 2) {
			for (int x = 0; x < image.getWidth(); x += 2) {
				int rgb = image.getRGB(x, y);
				int dr = Math.abs(((rgb >> 16) & 0xff) - red);
				int dg = Math.abs(((rgb >> 8) & 0xff) - green);
				int db = Math.abs((rgb & 0xff) - blue);
				if (dr < tolerance && dg < tolerance && db < tolerance) {
					return true;
				}
			}
		}
		return false;
	}
}
