package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.ArgsHandler;
import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.mail.AttachmentFiles;
import com.nilhcem.fakesmtp.mail.HtmlPreprocessor;
import com.nilhcem.fakesmtp.model.InboxFolder;
import com.nilhcem.fakesmtp.model.InboxListener;
import com.nilhcem.fakesmtp.model.InboxMessage;
import com.nilhcem.fakesmtp.model.InboxModel;
import com.nilhcem.fakesmtp.model.UIModel;
import com.nilhcem.fakesmtp.notification.InAppToast;
import com.nilhcem.fakesmtp.server.InboxBridge;
import com.nilhcem.fakesmtp.server.SMTPServerHandler;

import net.miginfocom.swing.MigLayout;

/**
 * The mail-client layout hosted inside the main window.
 *
 * @since 2.1
 */
public final class InboxView extends JPanel implements InboxListener, MessageActions {

	private static final Logger LOGGER = LoggerFactory.getLogger(InboxView.class);
	private static final int SIDEBAR_WIDTH = 220;
	private static final int LIST_WIDTH = 380;

	private final LegacyControls controls;
	private final InboxModel model;
	private final InboxBridge bridge;
	private final InAppToast toast;
	private final WindowRef window;
	private final InboxHeader header;
	private final InboxSidebar sidebar;
	private final MessageReaderPanel reader;
	private final MessageListPanel list;
	private final JPanel centerCards = new JPanel(new CardLayout());

	public InboxView(LegacyControls controls, InboxModel model, InboxBridge bridge, InAppToast toast, WindowRef window) {
		super(new BorderLayout());
		this.controls = controls;
		this.model = model;
		this.bridge = bridge;
		this.toast = toast;
		this.window = window;
		model.setNewestFirst(UISettings.INSTANCE.getBoolean(UISettings.SORT_NEWEST_FIRST));
		header = new InboxHeader(controls);
		sidebar = new InboxSidebar(model);
		reader = new MessageReaderPanel(this);
		list = new MessageListPanel(model, reader);
		header.setSettingsAction(this::openSettings);
		header.setThemeChanged(this::changeTheme);
		sidebar.setLogSelected(this::showLog);
		sidebar.setFolderSelected(this::showMail);
		list.setRefreshAction(bridge::refreshSaved);
		model.addListener(this);
		controls.getServer().setBeforeToggle(this::showServerTransition);
		controls.getServer().addObserver((observable, argument) -> header.syncServerState());
		JSplitPane messages = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, reader);
		messages.setDividerLocation(LIST_WIDTH);
		messages.setResizeWeight(0.32);
		messages.setContinuousLayout(true);
		centerCards.add(messages, "mail");
		centerCards.add(controls.getLogs().get(), "log");
		JSplitPane body = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, centerCards);
		body.setDividerLocation(SIDEBAR_WIDTH);
		body.setResizeWeight(0);
		body.setContinuousLayout(true);
		add(header, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);
		JPanel south = new JPanel(new BorderLayout());
		toast.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 12, 4, 12));
		south.add(toast, BorderLayout.NORTH);
		south.add(statusBar(), BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);
	}

	public InAppToast getToast() {
		return toast;
	}

	public void attachWindow(javax.swing.JFrame frame) {
		window.set(frame);
	}

	public void reveal(String messageId) {
		((CardLayout) centerCards.getLayout()).show(centerCards, "mail");
		sidebar.showInbox();
		model.setFolder(InboxFolder.INBOX);
		model.select(messageId);
		list.selectId(messageId);
		if (window.get() != null) {
			window.get().setVisible(true);
			window.get().setExtendedState(window.get().getExtendedState() & ~Frame.ICONIFIED);
			window.get().toFront();
			window.get().requestFocus();
		}
	}

	@Override
	public void inboxChanged() {
		sidebar.refreshCounts();
		list.reload();
	}

	@Override
	public void deleteMessage(InboxMessage message) {
		int answer = JOptionPane.showConfirmDialog(this, InboxText.get("reader.delete.confirm"),
				InboxText.get("reader.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.YES_OPTION) {
			return;
		}
		synchronized (SMTPServerHandler.INSTANCE.getMailSaver().getLock()) {
			String path = message.getFilePath();
			if (!AttachmentFiles.deleteStoredMessage(path)) {
				JOptionPane.showMessageDialog(this, InboxText.get("attach.save.failed"));
				return;
			}
			if (path != null) {
				Iterator<Map.Entry<Integer, String>> entries = UIModel.INSTANCE.getListMailsMap().entrySet().iterator();
				while (entries.hasNext()) {
					if (path.equals(entries.next().getValue())) {
						entries.remove();
					}
				}
			}
			model.remove(message.getId());
		}
	}

	@Override
	public void markRead(InboxMessage message, boolean read) {
		model.setRead(message.getId(), read);
		reader.showMessage(model.getSelected() == null ? message : model.getSelected());
	}

	@Override
	public void openStoredFile(InboxMessage message) {
		String path = message.getFilePath();
		if (path == null || path.isBlank() || !new File(path).isFile()) {
			JOptionPane.showMessageDialog(this, InboxText.get("reader.open.missing"));
			return;
		}
		File file = new File(path);
		try {
			String viewer = ArgsHandler.INSTANCE.getEmlViewer();
			if (viewer != null) {
				Runtime.getRuntime().exec(viewer + " " + file.getAbsolutePath());
			} else if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop.getDesktop().open(file);
			} else {
				JOptionPane.showMessageDialog(this, InboxText.get("reader.open.failed"));
			}
		} catch (IOException e) {
			LOGGER.warn("Could not open the saved email", e);
			JOptionPane.showMessageDialog(this, InboxText.get("reader.open.failed"));
		}
	}

	@Override
	public void loadExternalImages(InboxMessage message) {
		Thread thread = new Thread(() -> reprepare(message, true), "fakesmtp-images");
		thread.setDaemon(true);
		thread.start();
	}

	private JPanel statusBar() {
		JPanel bar = new JPanel(new MigLayout("insets 6 12 6 12", "[][grow,fill]push[][]", "[]"));
		bar.add(new JLabel(InboxText.get("status.saving")));
		bar.add(controls.getSaveDirectory().get(), "wmin 180");
		bar.add(controls.getReceived().get());
		bar.add(new JLabel(InboxText.get("status.received")));
		bar.add(controls.getClearAll().get());
		return bar;
	}

	private void showLog() {
		((CardLayout) centerCards.getLayout()).show(centerCards, "log");
	}

	private void showMail() {
		((CardLayout) centerCards.getLayout()).show(centerCards, "mail");
	}

	private void showServerTransition() {
		if (UIModel.INSTANCE.isStarted()) {
			header.showTransientStatus("header.status.stopping");
		} else {
			header.showTransientStatus("header.status.starting");
		}
	}

	private void openSettings() {
		if (!SettingsDialog.show(this)) {
			return;
		}
		header.refreshNotificationSwitch();
		model.setNewestFirst(UISettings.INSTANCE.getBoolean(UISettings.SORT_NEWEST_FIRST));
		if (model.getSelected() != null) {
			InboxMessage selected = model.getSelected();
			boolean remote = UISettings.INSTANCE.getBoolean(UISettings.EXTERNAL_IMAGES);
			Thread thread = new Thread(() -> reprepare(selected, remote), "fakesmtp-images");
			thread.setDaemon(true);
			thread.start();
		}
	}

	private void changeTheme() {
		java.awt.Window frame = window.get();
		Themes.install(header.isDarkSelected(), frame);
		try {
			UISettings.INSTANCE.save();
		} catch (IOException e) {
			LOGGER.warn("Could not save the theme", e);
		}
		header.syncServerState();
	}

	private void reprepare(InboxMessage message, boolean remote) {
		try {
			if (message.getParsed().getHtml() == null) {
				return;
			}
			message.getParsed().applyPrepared(HtmlPreprocessor.prepare(message.getParsed().getHtml(), remote));
			javax.swing.SwingUtilities.invokeLater(() -> {
				if (model.getSelected() != null && message.getId().equals(model.getSelected().getId())) {
					reader.showMessage(message);
				}
			});
		} catch (RuntimeException e) {
			LOGGER.warn("Could not prepare HTML for display", e);
		}
	}
}
