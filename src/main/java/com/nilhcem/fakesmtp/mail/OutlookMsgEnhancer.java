package com.nilhcem.fakesmtp.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.StringUtil;

import com.nilhcem.fakesmtp.core.Texts;

/**
 * Patches a jotlmsg-written {@code .msg} so Outlook shows From/To/CC and HTML body.
 * jotlmsg 1.x omits {@code DISPLAY_TO}/{@code DISPLAY_CC}, splits sender poorly, and has no HTML body prop.
 *
 * @since 2.1
 */
public final class OutlookMsgEnhancer {

	private static final long PROP_FLAGS = 6L; // readable | writeable

	private OutlookMsgEnhancer() {
		throw new UnsupportedOperationException();
	}

	public static void enhance(File msgFile, ParsedEmail parsed) throws IOException {
		if (msgFile == null || !msgFile.isFile() || parsed == null) {
			return;
		}
		List<EmailAddress> from = EmailAddresses.resolveFrom(parsed);
		List<EmailAddress> to = EmailAddresses.resolveTo(parsed);
		List<EmailAddress> cc = EmailAddresses.resolveCc(parsed);
		List<EmailAddress> bcc = EmailAddresses.resolveBcc(parsed);
		EmailAddress sender = EmailAddresses.first(from);
		String senderName = sender == null ? "" : EmailAddresses.displayName(sender);
		String senderEmail = sender == null ? "" : EmailAddresses.emailOnly(sender);
		if (Texts.isBlank(senderName) && !Texts.isBlank(senderEmail)) {
			senderName = senderEmail;
		}
		String displayTo = EmailAddresses.displayNames(to);
		String displayCc = EmailAddresses.displayNames(cc);
		String displayBcc = EmailAddresses.displayNames(bcc);
		String html = !Texts.isBlank(parsed.getHtml()) ? parsed.getHtml() : null;

		POIFSFileSystem fs = new POIFSFileSystem(msgFile, false);
		try {
			DirectoryNode root = fs.getRoot();
			List<MAPIProperty> added = new ArrayList<MAPIProperty>();
			putUnicode(root, MAPIProperty.DISPLAY_TO, displayTo, added);
			putUnicode(root, MAPIProperty.DISPLAY_CC, displayCc, added);
			putUnicode(root, MAPIProperty.DISPLAY_BCC, displayBcc, added);
			if (!Texts.isBlank(senderName)) {
				replaceUnicode(root, MAPIProperty.SENDER_NAME, senderName);
				putUnicode(root, MAPIProperty.SENT_REPRESENTING_NAME, senderName, added);
			}
			if (!Texts.isBlank(senderEmail)) {
				replaceUnicode(root, MAPIProperty.SENDER_EMAIL_ADDRESS, senderEmail);
				putUnicode(root, MAPIProperty.SENT_REPRESENTING_EMAIL_ADDRESS, senderEmail, added);
			}
			putUnicode(root, MAPIProperty.SENDER_ADDRTYPE, "SMTP", added);
			putUnicode(root, MAPIProperty.SENT_REPRESENTING_ADDRTYPE, "SMTP", added);
			if (html != null) {
				putUnicode(root, MAPIProperty.BODY_HTML, html, added);
			}
			appendProperties(root, added);
			fs.writeFilesystem();
		} finally {
			fs.close();
		}
	}

	private static void putUnicode(DirectoryNode root, MAPIProperty property, String value,
			List<MAPIProperty> added) throws IOException {
		if (property == null) {
			return;
		}
		String name = streamName(property);
		boolean existed = root.hasEntry(name);
		if (existed) {
			root.getEntry(name).delete();
		}
		byte[] data = StringUtil.getToUnicodeLE(value == null ? "" : value);
		root.createDocument(name, new ByteArrayInputStream(data));
		if (!existed) {
			added.add(property);
		}
	}

	private static void replaceUnicode(DirectoryNode root, MAPIProperty property, String value) throws IOException {
		String name = streamName(property);
		if (root.hasEntry(name)) {
			root.getEntry(name).delete();
		}
		byte[] data = StringUtil.getToUnicodeLE(value == null ? "" : value);
		root.createDocument(name, new ByteArrayInputStream(data));
	}

	private static void appendProperties(DirectoryNode root, List<MAPIProperty> extras) throws IOException {
		if (extras == null || extras.isEmpty() || !root.hasEntry("__properties_version1.0")) {
			return;
		}
		DocumentEntry props = (DocumentEntry) root.getEntry("__properties_version1.0");
		DocumentInputStream in = new DocumentInputStream(props);
		byte[] old;
		try {
			old = IOUtils.toByteArray(in);
		} finally {
			in.close();
		}
		props.delete();
		ByteArrayOutputStream out = new ByteArrayOutputStream(old.length + extras.size() * 16);
		out.write(old);
		for (MAPIProperty property : extras) {
			int tag = (property.id << 16) | 0x001F;
			LittleEndian.putUInt(tag, out);
			LittleEndian.putUInt(PROP_FLAGS, out);
			DocumentEntry doc = (DocumentEntry) root.getEntry(streamName(property));
			LittleEndian.putUInt(doc.getSize(), out);
			LittleEndian.putUInt(0, out);
		}
		root.createDocument("__properties_version1.0", new ByteArrayInputStream(out.toByteArray()));
	}

	private static String streamName(MAPIProperty property) {
		return String.format("__substg1.0_%04X001F", property.id);
	}
}
