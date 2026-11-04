package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.model.InboxMessage;

/**
 * Actions the reading pane can ask the window to perform.
 *
 * @since 2.1
 */
public interface MessageActions {

	void deleteMessage(InboxMessage message);

	void markRead(InboxMessage message, boolean read);

	void openStoredFile(InboxMessage message);

	void loadExternalImages(InboxMessage message);

	void openEmailsFolder();

	void markAllRead();
}
