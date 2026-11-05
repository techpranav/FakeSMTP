package com.nilhcem.fakesmtp.gui.inbox;

import com.nilhcem.fakesmtp.gui.info.ClearAllButton;
import com.nilhcem.fakesmtp.gui.info.NbReceivedLabel;
import com.nilhcem.fakesmtp.gui.info.PortTextField;
import com.nilhcem.fakesmtp.gui.info.SaveMsgField;
import com.nilhcem.fakesmtp.gui.info.StartServerButton;
import com.nilhcem.fakesmtp.gui.tab.LogsPane;

/**
 * The original window controls, kept so server start, storage, and the SMTP log still work.
 *
 * @since 2.1
 */
public final class LegacyControls {

	private final PortTextField port;
	private final StartServerButton server;
	private final SaveMsgField saveDirectory;
	private final NbReceivedLabel received;
	private final ClearAllButton clearAll;
	private final LogsPane logs;

	public LegacyControls(PortTextField port, StartServerButton server, SaveMsgField saveDirectory,
			NbReceivedLabel received, ClearAllButton clearAll, LogsPane logs) {
		this.port = port;
		this.server = server;
		this.saveDirectory = saveDirectory;
		this.received = received;
		this.clearAll = clearAll;
		this.logs = logs;
	}

	public PortTextField getPort() {
		return port;
	}

	public StartServerButton getServer() {
		return server;
	}

	public SaveMsgField getSaveDirectory() {
		return saveDirectory;
	}

	public NbReceivedLabel getReceived() {
		return received;
	}

	public ClearAllButton getClearAll() {
		return clearAll;
	}

	public LogsPane getLogs() {
		return logs;
	}
}
