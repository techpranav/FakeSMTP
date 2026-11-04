package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.formdev.flatlaf.FlatClientProperties;
import com.nilhcem.fakesmtp.model.InboxFolder;
import com.nilhcem.fakesmtp.model.InboxModel;

import net.miginfocom.swing.MigLayout;

/**
 * Flat navigation for mail folders and SMTP log.
 *
 * @since 2.1
 */
public final class InboxSidebar extends JPanel {

	private final InboxModel model;
	private final JToggleButton inbox = item(InboxText.get("folder.inbox"));
	private final JToggleButton unread = item(InboxText.get("folder.unread"));
	private final JToggleButton flagged = item(InboxText.get("folder.flagged"));
	private final JToggleButton all = item(InboxText.get("folder.all"));
	private final JToggleButton attachments = item(InboxText.get("folder.attachments"));
	private final JToggleButton log = item(InboxText.get("folder.log"));
	private Runnable logSelected;
	private Runnable folderSelected;
	private boolean updating;

	public InboxSidebar(InboxModel model) {
		super(new MigLayout("insets 14 10 14 10, wrap 1, gapy 2", "[grow,fill]", "[]"));
		this.model = model;
		setOpaque(true);
		setBackground(UiTokens.SIDEBAR_BG);
		setBorder(UiTokens.rightHairline());
		ButtonGroup group = new ButtonGroup();
		group.add(inbox);
		group.add(unread);
		group.add(flagged);
		group.add(all);
		group.add(attachments);
		group.add(log);
		inbox.setSelected(true);
		inbox.addActionListener(event -> selectFolder(InboxFolder.INBOX));
		unread.addActionListener(event -> selectFolder(InboxFolder.UNREAD));
		flagged.addActionListener(event -> selectFolder(InboxFolder.FLAGGED));
		all.addActionListener(event -> selectFolder(InboxFolder.ALL));
		attachments.addActionListener(event -> selectFolder(InboxFolder.ATTACHMENTS));
		log.addActionListener(event -> {
			if (!updating && logSelected != null) {
				logSelected.run();
			}
		});
		add(section(InboxText.get("nav.mail")));
		add(inbox);
		add(unread);
		add(flagged);
		add(all);
		add(attachments);
		add(section(InboxText.get("nav.developer")), "gaptop 14");
		add(log);
		refreshCounts();
	}

	public void setLogSelected(Runnable logSelected) {
		this.logSelected = logSelected;
	}

	public void setFolderSelected(Runnable folderSelected) {
		this.folderSelected = folderSelected;
	}

	public void showInbox() {
		updating = true;
		inbox.setSelected(true);
		updating = false;
	}

	public void refreshCounts() {
		inbox.setText(label(InboxText.get("folder.inbox"), model.getTotalCount()));
		unread.setText(label(InboxText.get("folder.unread"), model.getUnreadCount()));
		flagged.setText(label(InboxText.get("folder.flagged"), model.getFlaggedCount()));
		all.setText(label(InboxText.get("folder.all"), model.getTotalCount()));
		attachments.setText(label(InboxText.get("folder.attachments"), model.getAttachmentCount()));
		log.setText(InboxText.get("folder.log"));
	}

	private void selectFolder(InboxFolder folder) {
		if (updating) {
			return;
		}
		model.setFolder(folder);
		if (folderSelected != null) {
			folderSelected.run();
		}
	}

	private static JLabel section(String text) {
		JLabel label = new JLabel(text.toUpperCase(java.util.Locale.ROOT));
		label.setForeground(UiTokens.TEXT_SECONDARY);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
		label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 4, 0));
		return label;
	}

	private static JToggleButton item(String text) {
		JToggleButton button = new JToggleButton(text);
		button.setHorizontalAlignment(JToggleButton.LEFT);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
		button.putClientProperty(FlatClientProperties.STYLE,
				"arc: 8; focusWidth: 0; background: null; borderWidth: 0;"
						+ "selectedBackground: #EAF2FF; selectedForeground: #1B4F9C;"
						+ "hoverBackground: #EEF1F5");
		return button;
	}

	private static String label(String name, int count) {
		return name + "   " + count;
	}
}
