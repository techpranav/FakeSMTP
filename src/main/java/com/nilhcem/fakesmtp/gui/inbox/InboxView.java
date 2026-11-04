package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.ArgsHandler;
import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.mail.AttachmentFiles;
import com.nilhcem.fakesmtp.mail.EmailExport;
import com.nilhcem.fakesmtp.mail.HtmlPreprocessor;
import com.nilhcem.fakesmtp.model.InboxFolder;
import com.nilhcem.fakesmtp.model.InboxListener;
import com.nilhcem.fakesmtp.model.InboxMessage;
import com.nilhcem.fakesmtp.model.InboxModel;
import com.nilhcem.fakesmtp.model.UIModel;
import com.nilhcem.fakesmtp.notification.InAppToast;
import com.nilhcem.fakesmtp.server.InboxBridge;
import com.nilhcem.fakesmtp.server.SMTPServerHandler;
import com.nilhcem.fakesmtp.server.TestEmailSender;

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
	private final Runnable testNotification;

	public InboxView(LegacyControls controls, InboxModel model, InboxBridge bridge, InAppToast toast, WindowRef window,
			Runnable testNotification) {
		super(new BorderLayout());
		this.controls = controls;
		this.model = model;
		this.bridge = bridge;
		this.toast = toast;
		this.window = window;
		this.testNotification = testNotification;
		model.setNewestFirst(UISettings.INSTANCE.getBoolean(UISettings.SORT_NEWEST_FIRST));
		header = new InboxHeader(controls);
		sidebar = new InboxSidebar(model);
		reader = new MessageReaderPanel(this);
		list = new MessageListPanel(model, reader);
		header.setSettingsAction(this::openSettings);
		header.setThemeChanged(this::changeTheme);
		header.setCopyConnectionAction(this::copyConnection);
		header.setTestEmailAction(this::sendTestEmail);
		installShortcuts();
		sidebar.setLogSelected(this::showLog);
		sidebar.setFolderSelected(this::showMail);
		list.setRefreshAction(bridge::refreshSaved);
		model.addListener(this);
		controls.getServer().setBeforeToggle(this::showServerTransition);
		controls.getServer().addObserver((observable, argument) -> header.syncServerState());
		list.setMinimumSize(new Dimension(220, 160));
		reader.setMinimumSize(new Dimension(280, 160));
		sidebar.setMinimumSize(new Dimension(140, 160));
		JSplitPane messages = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, reader);
		messages.setDividerLocation(LIST_WIDTH);
		messages.setResizeWeight(0.35);
		messages.setContinuousLayout(true);
		messages.setOneTouchExpandable(true);
		centerCards.add(messages, "mail");
		centerCards.add(controls.getLogs().get(), "log");
		JSplitPane body = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, centerCards);
		body.setDividerLocation(SIDEBAR_WIDTH);
		body.setResizeWeight(0);
		body.setContinuousLayout(true);
		body.setOneTouchExpandable(true);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				SwingUtilities.invokeLater(() -> {
					body.revalidate();
					messages.revalidate();
					reader.revalidate();
					list.revalidate();
					revalidate();
					repaint();
				});
			}
		});
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
		if (Texts.isBlank(path) || !new File(path).isFile()) {
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

	@Override
	public void openEmailsFolder() {
		String path = UIModel.INSTANCE.getSavePath();
		if (Texts.isBlank(path)) {
			JOptionPane.showMessageDialog(this, InboxText.get("reader.folder.missing"));
			return;
		}
		File directory = new File(path);
		if (!directory.isDirectory()) {
			directory.mkdirs();
		}
		try {
			if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop.getDesktop().open(directory);
				return;
			}
		} catch (IOException e) {
			LOGGER.warn("Could not open the emails folder", e);
		}
		JOptionPane.showMessageDialog(this, InboxText.get("reader.folder.failed"));
	}

	@Override
	public void markAllRead() {
		model.markAllRead();
	}

	@Override
	public void toggleFlag(InboxMessage message) {
		if (message == null) {
			return;
		}
		model.setFlagged(message.getId(), !message.isFlagged());
		InboxMessage selected = model.getSelected();
		if (selected != null && selected.getId().equals(message.getId())) {
			reader.showMessage(selected);
		}
	}

	@Override
	public void saveAsMsg(InboxMessage message) {
		exportFile(message, "msg", "Outlook message (*.msg)", new Exporter() {
			@Override
			public void export(InboxMessage source, File file) throws IOException {
				EmailExport.saveAsMsg(source, file);
			}
		});
	}

	@Override
	public void saveAsEml(InboxMessage message) {
		exportFile(message, "eml", "Email message (*.eml)", new Exporter() {
			@Override
			public void export(InboxMessage source, File file) throws IOException {
				EmailExport.saveAsEml(source, file);
			}
		});
	}

	@Override
	public void saveScreenshot(InboxMessage message) {
		exportFile(message, "png", "PNG image (*.png)", new Exporter() {
			@Override
			public void export(InboxMessage source, File file) throws IOException {
				EmailExport.saveScreenshot(source, file);
			}
		});
	}

	@Override
	public void sendTestEmail() {
		try {
			TestEmailSender.sendSample();
			toast.showToast(InboxText.get("header.testEmail"), InboxText.get("reader.test.sent"), "info");
			((CardLayout) centerCards.getLayout()).show(centerCards, "mail");
			sidebar.showInbox();
			model.setFolder(InboxFolder.INBOX);
		} catch (IOException e) {
			LOGGER.warn("Could not inject the sample email", e);
			JOptionPane.showMessageDialog(this, InboxText.get("reader.test.failed"));
		}
	}

	private void exportFile(InboxMessage message, String extension, String filterLabel, Exporter exporter) {
		if (message == null || message.getParsed() == null) {
			return;
		}
		String base = safeFileBase(message);
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(base + "." + extension));
		chooser.setFileFilter(new FileNameExtensionFilter(filterLabel, extension));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = chooser.getSelectedFile();
		if (file == null) {
			return;
		}
		String name = file.getName();
		if (!name.toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)) {
			file = new File(file.getParentFile(), name + "." + extension);
		}
		try {
			exporter.export(message, file);
			toast.showToast(InboxText.get("reader.export.saved").replace("{0}", file.getName()),
					file.getAbsolutePath(), "info");
		} catch (IOException e) {
			LOGGER.warn("Could not export the message", e);
			JOptionPane.showMessageDialog(this, InboxText.get("reader.export.failed") + "\n" + e.getMessage());
		}
	}

	private static String safeFileBase(InboxMessage message) {
		String subject = message.getParsed() == null ? null : message.getParsed().getSubject();
		if (Texts.isBlank(subject)) {
			subject = "email";
		}
		String cleaned = subject.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
		if (cleaned.length() > 60) {
			cleaned = cleaned.substring(0, 60).trim();
		}
		if (Texts.isBlank(cleaned)) {
			cleaned = "email";
		}
		return cleaned;
	}

	private interface Exporter {
		void export(InboxMessage source, File file) throws IOException;
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
		if (!SettingsDialog.show(this, testNotification)) {
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

	private void copyConnection() {
		String port = controls.getPort().get().getText();
		if (Texts.isBlank(port)) {
			port = "25";
		}
		String value = "localhost:" + port.trim();
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new java.awt.datatransfer.StringSelection(value), null);
		toast.showToast(InboxText.get("notify.copied.title"), InboxText.format("notify.copied.connection", value), "info");
	}

	private void installShortcuts() {
		javax.swing.InputMap inputs = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		javax.swing.ActionMap actions = getActionMap();
		inputs.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "refresh");
		actions.put("refresh", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				bridge.refreshSaved();
			}
		});
		inputs.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "delete");
		actions.put("delete", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				if (model.getSelected() != null) {
					deleteMessage(model.getSelected());
				}
			}
		});
		inputs.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK
				| java.awt.event.InputEvent.SHIFT_DOWN_MASK), "markAllRead");
		actions.put("markAllRead", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				model.markAllRead();
			}
		});
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
