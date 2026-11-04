package com.nilhcem.fakesmtp.model;

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
	private boolean newestFirst = true;
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
		return newestFirst;
	}

	public synchronized void setNewestFirst(boolean newestFirst) {
		this.newestFirst = newestFirst;
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

	public synchronized void setRead(String id, boolean read) {
		InboxMessage message = find(id);
		if (message == null || message.isRead() == read) {
			return;
		}
		message.setRead(read);
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
		if (folder == InboxFolder.ATTACHMENTS) {
			return message.getParsed() != null && message.getParsed().hasAttachments();
		}
		return true;
	}

	private boolean matchesQuery(InboxMessage message) {
		if (query == null || query.isBlank()) {
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
		};
	}

	private void fire() {
		for (InboxListener listener : new ArrayList<InboxListener>(listeners)) {
			listener.inboxChanged();
		}
	}
}
