package com.nilhcem.fakesmtp.model;

/**
 * Notified on the calling thread after the inbox changes.
 *
 * @since 2.1
 */
public interface InboxListener {
	void inboxChanged();
}
