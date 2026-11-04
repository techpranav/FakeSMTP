package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.formdev.flatlaf.FlatClientProperties;
import com.nilhcem.fakesmtp.model.InboxFolder;
import com.nilhcem.fakesmtp.model.InboxModel;

import net.miginfocom.swing.MigLayout;

/**
 * Inbox, Unread, All Emails, Attachments, and the SMTP log.
 *
 * @since 2.1
 */
public final class InboxSidebar extends JPanel {

	private static final Color SIDEBAR_BG = new Color(0xF8F9FA);
	private static final Color DIVIDER = new Color(0xE0E3E7);

	private final InboxModel model;
	private final JToggleButton inbox = item("folder.inbox");
	private final JToggleButton unread = item("folder.unread");
	private final JToggleButton all = item("folder.all");
	private final JToggleButton attachments = item("folder.attachments");
	private final JToggleButton log = item("folder.log");
	private Runnable logSelected;
	private Runnable folderSelected;
	private boolean updating;

	public InboxSidebar(InboxModel model) {
		super(new MigLayout("insets 14 10 14 10, wrap 1, gapy 4", "[grow,fill]", "[][][][]16[]"));
		this.model = model;
		setOpaque(true);
		setBackground(SIDEBAR_BG);
		setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DIVIDER));
		ButtonGroup group = new ButtonGroup();
		group.add(inbox);
		group.add(unread);
		group.add(all);
		group.add(attachments);
		group.add(log);
		inbox.setSelected(true);
		inbox.addActionListener(event -> selectFolder(InboxFolder.INBOX));
		unread.addActionListener(event -> selectFolder(InboxFolder.UNREAD));
		all.addActionListener(event -> selectFolder(InboxFolder.ALL));
		attachments.addActionListener(event -> selectFolder(InboxFolder.ATTACHMENTS));
		log.addActionListener(event -> {
			if (!updating && logSelected != null) {
				logSelected.run();
			}
		});
		add(inbox);
		add(unread);
		add(all);
		add(attachments);
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
		inbox.setText(label("folder.inbox", model.getTotalCount()));
		unread.setText(label("folder.unread", model.getUnreadCount()));
		all.setText(label("folder.all", model.getTotalCount()));
		attachments.setText(label("folder.attachments", model.getAttachmentCount()));
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

	private static JToggleButton item(String key) {
		JToggleButton button = new JToggleButton(InboxText.get(key));
		button.setHorizontalAlignment(JToggleButton.LEFT);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, button.getFont().getSize() + 1f));
		button.putClientProperty(FlatClientProperties.STYLE,
				"arc: 8; focusWidth: 0; selectedBackground: #E8F0FE; selectedForeground: #174EA6");
		return button;
	}

	private static String label(String key, int count) {
		return InboxText.get(key) + "   " + count;
	}
}
