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
	private static final int SIDEBAR_WIDTH = UiTokens.SIDEBAR_WIDTH;
	private static final int LIST_WIDTH = UiTokens.LIST_WIDTH;

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
	private final JPanel statusBar = new JPanel();
	private final JLabel statusStorageLabel = new JLabel(InboxText.get("status.storage"));
	private final JLabel statusPathLabel = new JLabel(" ");
	private final JLabel statusCountLabel = new JLabel(" ");
	private final JLabel logTitle = new JLabel(InboxText.get("folder.log"));
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
		list.setMinimumSize(new Dimension(200, 160));
		reader.setMinimumSize(new Dimension(360, 160));
		sidebar.setMinimumSize(new Dimension(160, 160));
		JSplitPane messages = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, list, reader);
		messages.setDividerLocation(LIST_WIDTH);
		// Prefer the reading pane when the window grows.
		messages.setResizeWeight(0.28);
		messages.setContinuousLayout(true);
		messages.setOneTouchExpandable(true);
		centerCards.setBackground(UiTokens.APP_BG);
		centerCards.add(messages, "mail");
		centerCards.add(wrapLogs(controls.getLogs().get()), "log");
		JSplitPane body = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, centerCards);
		body.setDividerLocation(SIDEBAR_WIDTH);
		body.setResizeWeight(0);
		body.setContinuousLayout(true);
		body.setOneTouchExpandable(true);
		setBackground(UiTokens.APP_BG);
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
		south.setOpaque(false);
		JPanel toastTray = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 4));
		toastTray.setOpaque(false);
		toastTray.add(toast);
		south.add(toastTray, BorderLayout.NORTH);
		south.add(buildStatusBar(), BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);
		applyTheme();
	}

	public void applyTheme() {
		setBackground(UiTokens.APP_BG);
		centerCards.setBackground(UiTokens.APP_BG);
		header.applyTheme();
		sidebar.applyTheme();
		list.applyTheme();
		reader.applyTheme();
		toast.applyTheme();
		statusBar.setBackground(UiTokens.SURFACE_ALT);
		statusBar.setBorder(UiTokens.topHairline());
		statusStorageLabel.setForeground(UiTokens.TEXT_SECONDARY);
		statusPathLabel.setForeground(UiTokens.TEXT);
		statusCountLabel.setForeground(UiTokens.TEXT_SECONDARY);
		logTitle.setForeground(UiTokens.TEXT);
		revalidate();
		repaint();
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
		Thread thread = new Thread(() -> reprepare(message, true), "mailbox-images");
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

	private JPanel buildStatusBar() {
		statusBar.setLayout(new MigLayout("insets 3 12 3 12, gapx 6",
				"[][][][][][grow][][]", "[]"));
		statusBar.setOpaque(true);
		statusBar.setPreferredSize(new Dimension(10, 32));

		javax.swing.JTextField pathField = controls.getSaveDirectory().get();
		pathField.setVisible(false);
		pathField.setFocusable(false);

		javax.swing.Icon directoryIcon = javax.swing.UIManager.getIcon("FileView.directoryIcon");
		JLabel folderIcon = directoryIcon == null ? new JLabel("\u25A1") : new JLabel(directoryIcon);
		folderIcon.setToolTipText(pathField.getToolTipText());

		statusStorageLabel.setFont(UiTokens.statusFont(statusStorageLabel.getFont()));
		statusStorageLabel.setBorder(null);
		statusPathLabel.setText(shortStoragePath(pathField.getText()));
		statusPathLabel.setOpaque(false);
		statusPathLabel.setBorder(null);
		statusPathLabel.setFont(UiTokens.statusFont(statusPathLabel.getFont()));
		statusPathLabel.setToolTipText(pathField.getText());
		pathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent event) {
				syncPathLabel(pathField, statusPathLabel);
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent event) {
				syncPathLabel(pathField, statusPathLabel);
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent event) {
				syncPathLabel(pathField, statusPathLabel);
			}
		});

		javax.swing.JButton openFolder = chromeButton(InboxText.get("status.openFolder"));
		openFolder.setToolTipText(InboxText.get("reader.folder"));
		openFolder.addActionListener(event -> openEmailsFolder());

		javax.swing.JButton changeFolder = chromeButton(InboxText.get("status.changeFolder"));
		changeFolder.setToolTipText(pathField.getToolTipText());
		changeFolder.setEnabled(!ArgsHandler.INSTANCE.memoryModeEnabled());
		changeFolder.addActionListener(event -> controls.getSaveDirectory().requestChooseDirectory());

		JLabel received = controls.getReceived().get();
		received.setVisible(false);
		statusCountLabel.setText(emailCountText(received.getText()));
		statusCountLabel.setOpaque(false);
		statusCountLabel.setBorder(null);
		statusCountLabel.setFont(UiTokens.statusFont(statusCountLabel.getFont()));
		received.addPropertyChangeListener("text", event ->
				statusCountLabel.setText(emailCountText(received.getText())));

		javax.swing.JButton clear = controls.getClearAll().get();
		clear.putClientProperty("JButton.buttonType", "toolBarButton");
		clear.setFont(UiTokens.statusFont(clear.getFont()));

		statusBar.add(folderIcon);
		statusBar.add(statusStorageLabel);
		statusBar.add(statusPathLabel);
		statusBar.add(openFolder);
		if (!ArgsHandler.INSTANCE.memoryModeEnabled()) {
			statusBar.add(changeFolder);
		}
		statusBar.add(statusCountLabel, "gapbefore push");
		statusBar.add(clear);
		return statusBar;
	}

	private static String emailCountText(String raw) {
		int count = 0;
		try {
			count = Integer.parseInt(raw == null ? "0" : raw.trim());
		} catch (NumberFormatException ignored) {
			count = 0;
		}
		if (count == 1) {
			return InboxText.format("status.emails", Integer.valueOf(count));
		}
		return InboxText.format("status.emails.plural", Integer.valueOf(count));
	}

	private static void syncPathLabel(javax.swing.JTextField pathField, JLabel pathLabel) {
		String path = pathField.getText();
		pathLabel.setText(shortStoragePath(path));
		pathLabel.setToolTipText(path);
	}

	private static String shortStoragePath(String path) {
		if (Texts.isBlank(path)) {
			return InboxText.get("status.memory");
		}
		File file = new File(path);
		String name = file.getName();
		if (Texts.isBlank(name)) {
			return path;
		}
		File parent = file.getParentFile();
		if (parent == null) {
			return name;
		}
		String parentName = parent.getName();
		if (Texts.isBlank(parentName)) {
			return name;
		}
		return parentName + File.separator + name;
	}

	private static javax.swing.JButton chromeButton(String text) {
		javax.swing.JButton button = new javax.swing.JButton(text);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.setFont(UiTokens.statusFont(button.getFont()));
		return button;
	}

	private JPanel wrapLogs(javax.swing.JComponent logs) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(UiTokens.SURFACE);
		logTitle.setFont(logTitle.getFont().deriveFont(java.awt.Font.BOLD, 14f));
		logTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 8, 16));
		panel.add(logTitle, BorderLayout.NORTH);
		panel.add(logs, BorderLayout.CENTER);
		return panel;
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
			Thread thread = new Thread(() -> reprepare(selected, remote), "mailbox-images");
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
		applyTheme();
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
