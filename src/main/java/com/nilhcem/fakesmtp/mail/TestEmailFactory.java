package com.nilhcem.fakesmtp.mail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;

/**
 * Builds sample multipart HTML emails for inbox preview and export validation.
 *
 * @since 2.1
 */
public final class TestEmailFactory {

	private static final String BOUNDARY = "----=_MailBox_Test_Part_7a3c";
	private static final String RELATED = "----=_MailBox_Related_9f2b";
	/** 1x1 PNG pixel used as an inline CID image. */
	private static final String TINY_PNG_BASE64 =
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

	private TestEmailFactory() {
		throw new UnsupportedOperationException();
	}

	public static InputStream createSampleMessage() {
		return new ByteArrayInputStream(buildRaw().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Real-world overdue approval mail (ENOVIA-style HTML table + SVG charts).
	 */
	public static InputStream createOverdueApprovalMessage() {
		return new ByteArrayInputStream(buildOverdueRaw().getBytes(StandardCharsets.UTF_8));
	}

	public static String buildOverdueRaw() {
		try (InputStream in = TestEmailFactory.class.getResourceAsStream("/emails/overdue-approval.eml")) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource /emails/overdue-approval.eml");
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not load overdue-approval.eml", e);
		}
	}

	public static String buildRaw() {
		String now = rfc2822(new Date());
		String html = sampleHtml();
		String plain = "MailBox sample email\r\n\r\n"
				+ "This message includes HTML formatting, a hyperlink, a table, an SVG chart,\r\n"
				+ "an inline image, and two attachments (sample.txt and sample.csv).\r\n\r\n"
				+ "Open the HTML view in MailBox to preview how real application mail will look.\r\n";
		StringBuilder raw = new StringBuilder();
		raw.append("Date: ").append(now).append("\r\n");
		raw.append("From: MailBox Tester <tester@mailbox.local>\r\n");
		raw.append("To: Developer <developer@example.com>\r\n");
		raw.append("Cc: QA <qa@example.com>\r\n");
		raw.append("Subject: MailBox sample: HTML, links and attachments\r\n");
		raw.append("Message-ID: <test.").append(System.currentTimeMillis()).append("@mailbox.local>\r\n");
		raw.append("MIME-Version: 1.0\r\n");
		raw.append("Content-Type: multipart/mixed; boundary=\"").append(BOUNDARY).append("\"\r\n");
		raw.append("\r\n");
		raw.append("--").append(BOUNDARY).append("\r\n");
		raw.append("Content-Type: multipart/related; boundary=\"").append(RELATED).append("\"\r\n\r\n");
		raw.append("--").append(RELATED).append("\r\n");
		raw.append("Content-Type: multipart/alternative; boundary=\"----=_Alt_1\"\r\n\r\n");
		raw.append("------=_Alt_1\r\n");
		raw.append("Content-Type: text/plain; charset=UTF-8\r\n");
		raw.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
		raw.append(plain).append("\r\n");
		raw.append("------=_Alt_1\r\n");
		raw.append("Content-Type: text/html; charset=UTF-8\r\n");
		raw.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
		raw.append(html).append("\r\n");
		raw.append("------=_Alt_1--\r\n");
		raw.append("--").append(RELATED).append("\r\n");
		raw.append("Content-Type: image/png; name=\"pixel.png\"\r\n");
		raw.append("Content-Transfer-Encoding: base64\r\n");
		raw.append("Content-ID: <logo@mailbox>\r\n");
		raw.append("Content-Disposition: inline; filename=\"pixel.png\"\r\n\r\n");
		raw.append(TINY_PNG_BASE64).append("\r\n");
		raw.append("--").append(RELATED).append("--\r\n");
		raw.append("--").append(BOUNDARY).append("\r\n");
		raw.append("Content-Type: text/plain; name=\"sample.txt\"\r\n");
		raw.append("Content-Transfer-Encoding: 7bit\r\n");
		raw.append("Content-Disposition: attachment; filename=\"sample.txt\"\r\n\r\n");
		raw.append("This is a MailBox sample attachment.\r\nYou can Save or Open it from the reading pane.\r\n");
		raw.append("\r\n--").append(BOUNDARY).append("\r\n");
		raw.append("Content-Type: text/csv; name=\"sample.csv\"\r\n");
		raw.append("Content-Transfer-Encoding: 7bit\r\n");
		raw.append("Content-Disposition: attachment; filename=\"sample.csv\"\r\n\r\n");
		raw.append("Task,Days overdue,Document,Project\r\n");
		raw.append("Approver 02,6,E00827,WIN\r\n");
		raw.append("ConfigAnalyst 02,12,CR-A00024,WIN\r\n");
		raw.append("\r\n--").append(BOUNDARY).append("--\r\n");
		return raw.toString();
	}

	private static String sampleHtml() {
		return "<html><body style='font-family:Calibri,Arial,sans-serif;font-size:13px;color:#202124'>"
				+ "<p><img src='cid:logo@mailbox' width='12' height='12' alt='logo'/> "
				+ "<b>MailBox sample email</b></p>"
				+ "<p>This message demonstrates how real HTML mail is shown in MailBox:</p>"
				+ "<ul>"
				+ "<li><b>Bold</b>, <i>italic</i>, and <u>underlined</u> text</li>"
				+ "<li>A <a href='https://example.com/docs'>hyperlink to example.com</a></li>"
				+ "<li>A formatted table</li>"
				+ "<li>An SVG bar chart</li>"
				+ "<li>An inline CID image and downloadable attachments</li>"
				+ "</ul>"
				+ "<table width='100%' cellpadding='6' cellspacing='0' style='border-collapse:collapse;font-size:12px'>"
				+ "<tr style='background:#d9e2ef'>"
				+ "<th style='border:1px solid #999;text-align:left'>Task Assignee</th>"
				+ "<th style='border:1px solid #999;text-align:left'>Days overdue</th>"
				+ "<th style='border:1px solid #999;text-align:left'>Document</th>"
				+ "<th style='border:1px solid #999;text-align:left'>Project</th>"
				+ "</tr>"
				+ "<tr><td style='border:1px solid #999'>Approver 02</td>"
				+ "<td style='border:1px solid #999'>6</td>"
				+ "<td style='border:1px solid #999'><a href='https://example.com/E00827'>E00827</a></td>"
				+ "<td style='border:1px solid #999'>WIN</td></tr>"
				+ "<tr><td style='border:1px solid #999'>ConfigAnalyst 02</td>"
				+ "<td style='border:1px solid #999'>12</td>"
				+ "<td style='border:1px solid #999'><a href='https://example.com/CR-A00024'>CR-A00024</a></td>"
				+ "<td style='border:1px solid #999'>WIN</td></tr>"
				+ "</table>"
				+ "<p style='margin-top:16px'><b>Assignment distribution</b><br/>"
				+ "<svg xmlns='http://www.w3.org/2000/svg' width='420' height='70' role='img'>"
				+ "<text x='0' y='18' font-size='12' font-family='Arial,sans-serif'>Approver 02</text>"
				+ "<rect x='120' y='6' width='180' height='14' fill='#4C78A8'/>"
				+ "<text x='308' y='18' font-size='12' font-family='Arial,sans-serif'>2</text>"
				+ "<text x='0' y='46' font-size='12' font-family='Arial,sans-serif'>ConfigAnalyst</text>"
				+ "<rect x='120' y='34' width='90' height='14' fill='#4C78A8'/>"
				+ "<text x='218' y='46' font-size='12' font-family='Arial,sans-serif'>1</text>"
				+ "</svg></p>"
				+ "<p>Attachments: <code>sample.txt</code> and <code>sample.csv</code>.</p>"
				+ "<p style='color:#5f6368'>Generated by MailBox for UI preview. Do not reply.</p>"
				+ "</body></html>";
	}

	private static String rfc2822(Date date) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
		format.setTimeZone(TimeZone.getDefault());
		return format.format(date);
	}

	public static byte[] tinyPng() {
		return Base64.getDecoder().decode(TINY_PNG_BASE64);
	}
}
