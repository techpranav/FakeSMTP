package com.nilhcem.fakesmtp.model;

import com.nilhcem.fakesmtp.core.Texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The messages currently shown by the inbox, filtered by folder and search.
 * <p>
 * Callers that update Swing should use this from the event dispatch thread.
 * </p>
 *
 * @since 2.1
 */
public final class InboxModel {

	private final List<InboxMessage> messages = new ArrayList<InboxMessage>();
	private final List<InboxListener> listeners = new ArrayList<InboxListener>();
	private InboxFolder folder = InboxFolder.INBOX;
	private String query = "";
	private SortMode sortMode = SortMode.NEWEST_FIRST;
	private String selectedId;

	public void addListener(InboxListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public synchronized InboxFolder getFolder() {
		return folder;
	}

	public synchronized void setFolder(InboxFolder folder) {
		this.folder = folder;
		fire();
	}

	public synchronized String getQuery() {
		return query;
	}

	public synchronized void setQuery(String query) {
		if (query == null) {
			this.query = "";
		} else {
			this.query = query;
		}
		fire();
	}

	public synchronized boolean isNewestFirst() {
		return sortMode == SortMode.NEWEST_FIRST;
	}

	public synchronized void setNewestFirst(boolean newestFirst) {
		if (newestFirst) {
			setSortMode(SortMode.NEWEST_FIRST);
		} else {
			setSortMode(SortMode.OLDEST_FIRST);
		}
	}

	public synchronized SortMode getSortMode() {
		return sortMode;
	}

	public synchronized void setSortMode(SortMode sortMode) {
		if (sortMode == null) {
			this.sortMode = SortMode.NEWEST_FIRST;
		} else {
			this.sortMode = sortMode;
		}
		fire();
	}

	public synchronized String getSelectedId() {
		return selectedId;
	}

	public synchronized void select(String id) {
		selectedId = id;
		fire();
	}

	public synchronized InboxMessage getSelected() {
		return find(selectedId);
	}

	/**
	 * Adds a message that just arrived. A message with a file path already in the inbox is ignored.
	 *
	 * @param message the new message.
	 * @return the stored message, or {@code null} when it was a duplicate.
	 */
	public synchronized InboxMessage add(InboxMessage message) {
		if (message == null) {
			return null;
		}
		String path = message.getFilePath();
		if (path != null && !path.isEmpty() && findByPath(path) != null) {
			return null;
		}
		messages.add(message);
		fire();
		return message;
	}

	public synchronized void markAllRead() {
		boolean changed = false;
		for (InboxMessage message : messages) {
			if (!message.isRead()) {
				message.setRead(true);
				changed = true;
			}
		}
		if (changed) {
			fire();
		}
	}

	public synchronized void setRead(String id, boolean read) {
		InboxMessage message = find(id);
		if (message == null || message.isRead() == read) {
			return;
		}
		message.setRead(read);
		fire();
	}

	public synchronized void setFlagged(String id, boolean flagged) {
		InboxMessage message = find(id);
		if (message == null || message.isFlagged() == flagged) {
			return;
		}
		message.setFlagged(flagged);
		fire();
	}

	public synchronized InboxMessage remove(String id) {
		InboxMessage message = find(id);
		if (message == null) {
			return null;
		}
		messages.remove(message);
		if (id.equals(selectedId)) {
			selectedId = null;
		}
		fire();
		return message;
	}

	public synchronized void clear() {
		messages.clear();
		selectedId = null;
		fire();
	}

	public synchronized int getTotalCount() {
		return messages.size();
	}

	public synchronized int getUnreadCount() {
		int count = 0;
		for (InboxMessage message : messages) {
			if (!message.isRead()) {
				count++;
			}
		}
		return count;
	}

	public synchronized int getFlaggedCount() {
		int count = 0;
		for (InboxMessage message : messages) {
			if (message.isFlagged()) {
				count++;
			}
		}
		return count;
	}

	public synchronized int getAttachmentCount() {
		int count = 0;
		for (InboxMessage message : messages) {
			if (message.getParsed() != null && message.getParsed().hasAttachments()) {
				count++;
			}
		}
		return count;
	}

	public synchronized List<InboxMessage> visibleMessages() {
		List<InboxMessage> visible = new ArrayList<InboxMessage>();
		for (InboxMessage message : messages) {
			if (inFolder(message) && matchesQuery(message)) {
				visible.add(message);
			}
		}
		Collections.sort(visible, messageOrder());
		return visible;
	}

	private InboxMessage find(String id) {
		if (id == null) {
			return null;
		}
		for (InboxMessage message : messages) {
			if (id.equals(message.getId())) {
				return message;
			}
		}
		return null;
	}

	private InboxMessage findByPath(String path) {
		for (InboxMessage message : messages) {
			if (path.equals(message.getFilePath())) {
				return message;
			}
		}
		return null;
	}

	private boolean inFolder(InboxMessage message) {
		if (folder == InboxFolder.UNREAD) {
			return !message.isRead();
		}
		if (folder == InboxFolder.FLAGGED) {
			return message.isFlagged();
		}
		if (folder == InboxFolder.ATTACHMENTS) {
			return message.getParsed() != null && message.getParsed().hasAttachments();
		}
		return true;
	}

	private boolean matchesQuery(InboxMessage message) {
		if (Texts.isBlank(query)) {
			return true;
		}
		if (message.getParsed() == null) {
			return false;
		}
		String haystack = message.getParsed().searchText().toLowerCase(Locale.ROOT);
		return haystack.contains(query.trim().toLowerCase(Locale.ROOT));
	}

	private Comparator<InboxMessage> messageOrder() {
		return new Comparator<InboxMessage>() {
			@Override
			public int compare(InboxMessage left, InboxMessage right) {
				if (sortMode == SortMode.SUBJECT) {
					return compareText(subjectOf(left), subjectOf(right));
				}
				if (sortMode == SortMode.SENDER) {
					return compareText(senderOf(left), senderOf(right));
				}
				if (sortMode == SortMode.FLAGGED_FIRST) {
					int flags = Boolean.compare(right.isFlagged(), left.isFlagged());
					if (flags != 0) {
						return flags;
					}
					return compareDate(left, right, true);
				}
				return compareDate(left, right, sortMode == SortMode.NEWEST_FIRST);
			}
		};
	}

	private static int compareDate(InboxMessage left, InboxMessage right, boolean newestFirst) {
		Date first = left.getReceivedDate();
		Date second = right.getReceivedDate();
		if (first == null && second == null) {
			return 0;
		}
		if (first == null) {
			return 1;
		}
		if (second == null) {
			return -1;
		}
		int order = first.compareTo(second);
		if (!newestFirst) {
			return order;
		}
		return -order;
	}

	private static int compareText(String left, String right) {
		return left.compareToIgnoreCase(right);
	}

	private static String subjectOf(InboxMessage message) {
		if (message.getParsed() == null || Texts.isBlank(message.getParsed().getSubject())) {
			return "";
		}
		return message.getParsed().getSubject();
	}

	private static String senderOf(InboxMessage message) {
		if (message.getParsed() == null) {
			return "";
		}
		return message.getParsed().senderLine();
	}

	private void fire() {
		for (InboxListener listener : new ArrayList<InboxListener>(listeners)) {
			listener.inboxChanged();
		}
	}
}
