package com.nilhcem.fakesmtp.mail;

import com.nilhcem.fakesmtp.core.Texts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Writes attachment bytes to a name that cannot escape the chosen folder.
 *
 * @since 2.1
 */
public final class AttachmentFiles {

	private static final int MAX_NAME_LENGTH = 120;
	private static final int MAX_ATTEMPTS = 1000;

	private AttachmentFiles() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param name the name supplied by the message.
	 * @return a single path segment with control characters and separators removed.
	 */
	public static String safeName(String name) {
		String value = "attachment";
		if (!Texts.isBlank(name)) {
			value = name.trim();
		}
		value = value.replace('\\', '/');
		int slash = value.lastIndexOf('/');
		if (slash >= 0 && slash < value.length() - 1) {
			value = value.substring(slash + 1);
		}
		value = value.replace('/', '_')
				.replace(':', '_').replace('*', '_').replace('?', '_')
				.replace('"', '_').replace('<', '_').replace('>', '_').replace('|', '_');
		value = value.replaceAll("\\p{Cntrl}", "_").trim();
		while (value.startsWith(".")) {
			value = value.substring(1);
		}
		if (value.isEmpty() || ".".equals(value) || "..".equals(value)) {
			value = "attachment";
		}
		if (value.length() > MAX_NAME_LENGTH) {
			value = value.substring(value.length() - MAX_NAME_LENGTH);
		}
		return value;
	}

	public static File unique(File directory, String name) {
		File file = new File(directory, name);
		if (!file.exists()) {
			return file;
		}
		String base = name;
		String extension = "";
		int dot = name.lastIndexOf('.');
		if (dot > 0) {
			base = name.substring(0, dot);
			extension = name.substring(dot);
		}
		for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
			file = new File(directory, base + "-" + attempt + extension);
			if (!file.exists()) {
				return file;
			}
		}
		return new File(directory, base + "-" + System.currentTimeMillis() + extension);
	}

	public static void write(File file, byte[] data) throws IOException {
		File parent = file.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			throw new IOException("Could not create " + parent.getAbsolutePath());
		}
		try (FileOutputStream output = new FileOutputStream(file)) {
			if (data != null) {
				output.write(data);
			}
		}
	}

	/**
	 * Copies an attachment into the temp directory so the operating system can open it.
	 * The file is marked for deletion when the application exits and is never launched from here.
	 *
	 * @param attachment the attachment to copy.
	 * @return the temp file.
	 * @throws IOException when the file cannot be written.
	 */
	public static File temporaryCopy(MailAttachment attachment) throws IOException {
		File directory = new File(System.getProperty("java.io.tmpdir"), "mailbox-attachments");
		File file = unique(directory, safeName(attachment.getFileName()));
		write(file, attachment.getData());
		file.deleteOnExit();
		return file;
	}

	/**
	 * Deletes the saved {@code .eml} file.
	 *
	 * @param path the file path, or {@code null} when the message was not stored.
	 * @return {@code true} when there is nothing left to delete.
	 */
	public static boolean deleteStoredMessage(String path) {
		if (Texts.isBlank(path)) {
			return true;
		}
		File file = new File(path);
		if (!file.exists()) {
			return true;
		}
		return file.delete();
	}
}
