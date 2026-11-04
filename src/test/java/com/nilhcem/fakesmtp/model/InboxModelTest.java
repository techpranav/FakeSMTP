package com.nilhcem.fakesmtp.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.nilhcem.fakesmtp.mail.MailAttachment;
import com.nilhcem.fakesmtp.mail.ParsedEmail;

public class InboxModelTest {

	@Test
	public void filtersSearchUnreadSortAndDelete() {
		InboxModel model = new InboxModel();
		InboxMessage older = message("Ada <ada@example.com>", "bob@example.com", "Weekly report", hoursAgo(2), false, false);
		InboxMessage newer = message("Cara <cara@example.com>", "ada@example.com", "Build failed", hoursAgo(1), true, true);
		model.add(older);
		model.add(newer);

		assertEquals(2, model.getTotalCount());
		assertEquals(1, model.getUnreadCount());
		assertEquals(1, model.getAttachmentCount());
		assertEquals(newer.getId(), model.visibleMessages().get(0).getId());

		model.setNewestFirst(false);
		assertEquals(older.getId(), model.visibleMessages().get(0).getId());

		model.setQuery("weekly");
		List<InboxMessage> found = model.visibleMessages();
		assertEquals(1, found.size());
		assertEquals(older.getId(), found.get(0).getId());

		model.setQuery("");
		model.setFolder(InboxFolder.UNREAD);
		assertEquals(1, model.visibleMessages().size());
		assertFalse(model.visibleMessages().get(0).isRead());

		model.setFolder(InboxFolder.ATTACHMENTS);
		assertEquals(newer.getId(), model.visibleMessages().get(0).getId());

		model.setFolder(InboxFolder.INBOX);
		model.setRead(newer.getId(), true);
		assertEquals(0, model.getUnreadCount());

		model.remove(newer.getId());
		assertEquals(1, model.getTotalCount());
		model.clear();
		assertEquals(0, model.getTotalCount());
		assertNull(model.getSelected());
	}

	@Test
	public void ignoresADuplicateSavedFile() {
		InboxModel model = new InboxModel();
		InboxMessage first = message("a@example.com", "b@example.com", "One", new Date(), false, false);
		InboxMessage duplicate = message("a@example.com", "b@example.com", "One", new Date(), false, false);
		assertEquals(first, model.add(first));
		assertNull(model.add(duplicate));
		assertEquals(1, model.getTotalCount());
	}

	private static InboxMessage message(String from, String to, String subject, Date received, boolean unread, boolean attachment) {
		EmailModel email = new EmailModel();
		email.setFrom(from);
		email.setTo(to);
		email.setSubject(subject);
		email.setReceivedDate(received);
		email.setEmailStr("Subject: " + subject);
		email.setFilePath("memory-" + subject + "-" + unread);
		ParsedEmail.Builder builder = ParsedEmail.builder()
				.subject(subject)
				.preview(subject)
				.receivedDate(received)
				.filePath(email.getFilePath())
				.envelopeFrom(from)
				.envelopeTo(to)
				.plainText(subject)
				.readable(true);
		if (attachment) {
			builder.attachments(java.util.Collections.singletonList(
					new MailAttachment("notes.txt", "text/plain", null, new byte[] {1, 2, 3}, false)));
		}
		return new InboxMessage(email, builder.build(), !unread);
	}

	private static Date hoursAgo(int hours) {
		return new Date(System.currentTimeMillis() - hours * 60L * 60L * 1000L);
	}
}
