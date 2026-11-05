package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatClientProperties;
import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.InboxFolder;
import com.nilhcem.fakesmtp.model.InboxMessage;
import com.nilhcem.fakesmtp.model.InboxModel;
import com.nilhcem.fakesmtp.model.SortMode;

/**
 * Searchable list of received messages.
 *
 * @since 2.1
 */
public final class MessageListPanel extends JPanel {

	private final InboxModel model;
	private final DefaultListModel<InboxMessage> items = new DefaultListModel<InboxMessage>();
	private final JList<InboxMessage> list = new JList<InboxMessage>(items);
	private final JTextField search = new JTextField();
	private final JComboBox<String> sort = new JComboBox<String>(new String[] {
		InboxText.get("list.newest"),
		InboxText.get("list.oldest"),
		InboxText.get("list.sender.asc"),
		InboxText.get("list.sender.desc"),
		InboxText.get("list.subject.asc"),
		InboxText.get("list.subject.desc")
	});
	private final JPanel cards = new JPanel(new CardLayout());
	private final EmptyState empty = new EmptyState("", "");
	private final JScrollPane listScroll;
	private final MessageReaderPanel reader;
	private final MessageActions actions;
	private boolean updating;
	private Runnable refreshAction;

	public MessageListPanel(InboxModel model, MessageReaderPanel reader, MessageActions actions) {
		super(new BorderLayout(0, UiTokens.SPACE_SM));
		this.model = model;
		this.reader = reader;
		this.actions = actions;
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_MD, UiTokens.SPACE_MD, UiTokens.SPACE_SM, UiTokens.SPACE_SM));
		search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, InboxText.get("list.search"));
		search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new com.formdev.flatlaf.icons.FlatSearchIcon());
		search.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
		search.setToolTipText(InboxText.get("list.search.tip"));
		search.setMinimumSize(new Dimension(96, UiTokens.CONTROL_HEIGHT));
		search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent event) {
				model.setQuery(search.getText());
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent event) {
				model.setQuery(search.getText());
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent event) {
				model.setQuery(search.getText());
			}
		});
		sort.setMaximumRowCount(8);
		sort.setPreferredSize(new Dimension(UiTokens.SORT_WIDTH, UiTokens.CONTROL_HEIGHT));
		sort.setMaximumSize(new Dimension(UiTokens.SORT_WIDTH, UiTokens.CONTROL_HEIGHT));
		sort.setToolTipText(InboxText.get("list.sort.tip"));
		sort.setSelectedIndex(indexOf(model.getSortMode()));
		sort.addActionListener(event -> model.setSortMode(modeAt(sort.getSelectedIndex())));
		search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, Boolean.TRUE);
		JButton refresh = new JButton("\u21BB");
		refresh.setToolTipText(InboxText.get("list.refresh"));
		refresh.putClientProperty("JButton.buttonType", "toolBarButton");
		refresh.setPreferredSize(new Dimension(UiTokens.CONTROL_HEIGHT + 4, UiTokens.CONTROL_HEIGHT));
		refresh.addActionListener(event -> {
			if (refreshAction != null) {
				refreshAction.run();
			}
		});
		JPanel tools = new JPanel(new BorderLayout(6, 0));
		tools.setOpaque(false);
		tools.add(search, BorderLayout.CENTER);
		JPanel trailing = new JPanel(new BorderLayout(4, 0));
		trailing.setOpaque(false);
		trailing.add(sort, BorderLayout.CENTER);
		trailing.add(refresh, BorderLayout.EAST);
		tools.add(trailing, BorderLayout.EAST);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFixedCellHeight(UiTokens.LIST_ROW_HEIGHT);
		list.setCellRenderer(new RowRenderer());
		list.setBorder(null);
		list.setPrototypeCellValue(null);
		list.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				onSelection();
			}
		});
		installContextMenu();
		listScroll = new JScrollPane(list);
		listScroll.getVerticalScrollBar().setUnitIncrement(16);
		listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		listScroll.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				int width = listScroll.getViewport().getWidth();
				if (width > 0 && list.getFixedCellWidth() != width) {
					list.setFixedCellWidth(width);
				}
				list.repaint();
			}
		});
		cards.setOpaque(false);
		cards.add(listScroll, "list");
		cards.add(empty, "empty");
		setMinimumSize(new Dimension(220, 160));
		add(tools, BorderLayout.NORTH);
		add(cards, BorderLayout.CENTER);
		applyTheme();
		reload();
	}

	public void applyTheme() {
		setBackground(UiTokens.LIST_BG);
		list.setBackground(UiTokens.LIST_BG);
		listScroll.setBackground(UiTokens.LIST_BG);
		listScroll.getViewport().setBackground(UiTokens.LIST_BG);
		listScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTokens.BORDER));
		empty.applyTheme();
		list.repaint();
	}

	public void setRefreshAction(Runnable refreshAction) {
		this.refreshAction = refreshAction;
	}

	public void reload() {
		updating = true;
		items.clear();
		InboxMessage selected = model.getSelected();
		for (InboxMessage message : model.visibleMessages()) {
			items.addElement(message);
		}
		if (selected != null) {
			list.setSelectedValue(selected, true);
		} else {
			list.clearSelection();
		}
		updating = false;
		if (items.isEmpty()) {
			empty.setMessage(emptyTitle(), emptyText());
			((CardLayout) cards.getLayout()).show(cards, "empty");
			reader.showNone();
		} else {
			((CardLayout) cards.getLayout()).show(cards, "list");
			if (list.getSelectedValue() == null) {
				reader.showNone();
			}
		}
	}

	public void selectId(String id) {
		for (int index = 0; index < items.size(); index++) {
			if (items.get(index).getId().equals(id)) {
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
				return;
			}
		}
	}

	/**
	 * Outlook-style right-click actions on an inbox row (Save as .msg, screenshot, etc.).
	 */
	private void installContextMenu() {
		if (actions == null) {
			return;
		}
		final JPopupMenu menu = new JPopupMenu();
		final JMenuItem saveMsg = new JMenuItem(InboxText.get("reader.saveMsg"));
		saveMsg.setToolTipText(InboxText.get("reader.saveMsg.tip"));
		final JMenuItem saveEml = new JMenuItem(InboxText.get("reader.saveEml"));
		final JMenuItem screenshot = new JMenuItem(InboxText.get("reader.screenshot"));
		screenshot.setToolTipText(InboxText.get("reader.screenshot.tip"));
		final JMenuItem flag = new JMenuItem(InboxText.get("reader.flag"));
		final JMenuItem markRead = new JMenuItem(InboxText.get("reader.markRead"));
		final JMenuItem openFile = new JMenuItem(InboxText.get("reader.open"));
		final JMenuItem delete = new JMenuItem(InboxText.get("reader.delete"));
		menu.add(saveMsg);
		menu.add(saveEml);
		menu.add(screenshot);
		menu.addSeparator();
		menu.add(flag);
		menu.add(markRead);
		menu.add(openFile);
		menu.addSeparator();
		menu.add(delete);

		// Select the row under the cursor before the menu opens (Windows popup = mouseReleased).
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				selectRowAt(event);
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				selectRowAt(event);
			}

			private void selectRowAt(MouseEvent event) {
				if (!SwingUtilities.isRightMouseButton(event) && !event.isPopupTrigger()) {
					return;
				}
				int index = list.locationToIndex(event.getPoint());
				if (index >= 0) {
					list.setSelectedIndex(index);
				}
			}
		});

		menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) {
				InboxMessage message = list.getSelectedValue();
				boolean enabled = message != null;
				saveMsg.setEnabled(enabled);
				saveEml.setEnabled(enabled);
				screenshot.setEnabled(enabled);
				flag.setEnabled(enabled);
				markRead.setEnabled(enabled);
				openFile.setEnabled(enabled);
				delete.setEnabled(enabled);
				if (!enabled) {
					return;
				}
				flag.setText(message.isFlagged() ? InboxText.get("reader.unflag") : InboxText.get("reader.flag"));
				markRead.setText(message.isRead() ? InboxText.get("reader.markUnread") : InboxText.get("reader.markRead"));
				final InboxMessage target = message;
				replaceAction(saveMsg, () -> actions.saveAsMsg(target));
				replaceAction(saveEml, () -> actions.saveAsEml(target));
				replaceAction(screenshot, () -> actions.saveScreenshot(target));
				replaceAction(flag, () -> actions.toggleFlag(target));
				replaceAction(markRead, () -> actions.markRead(target, !target.isRead()));
				replaceAction(openFile, () -> actions.openStoredFile(target));
				replaceAction(delete, () -> actions.deleteMessage(target));
			}

			@Override
			public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event) {
				// no-op
			}

			@Override
			public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event) {
				// no-op
			}
		});
		list.setComponentPopupMenu(menu);
	}

	private static void replaceAction(JMenuItem item, Runnable action) {
		java.awt.event.ActionListener[] listeners = item.getActionListeners();
		for (java.awt.event.ActionListener listener : listeners) {
			item.removeActionListener(listener);
		}
		item.addActionListener(event -> action.run());
	}

	private void onSelection() {
		if (updating) {
			return;
		}
		InboxMessage message = list.getSelectedValue();
		if (message == null) {
			reader.showNone();
			return;
		}
		if (!message.getId().equals(model.getSelectedId())) {
			model.select(message.getId());
		}
		if (!message.isRead()) {
			model.setRead(message.getId(), true);
		}
		reader.showMessage(message);
	}

	private String emptyTitle() {
		if (!Texts.isBlank(model.getQuery())) {
			return InboxText.get("list.empty.search.title");
		}
		if (model.getFolder() == InboxFolder.UNREAD) {
			return InboxText.get("list.empty.unread.title");
		}
		if (model.getFolder() == InboxFolder.FLAGGED) {
			return InboxText.get("list.empty.flagged.title");
		}
		if (model.getFolder() == InboxFolder.ATTACHMENTS) {
			return InboxText.get("list.empty.attachments.title");
		}
		return InboxText.get("list.empty.inbox.title");
	}

	private String emptyText() {
		if (!Texts.isBlank(model.getQuery())) {
			return InboxText.get("list.empty.search.text");
		}
		if (model.getFolder() == InboxFolder.UNREAD) {
			return InboxText.get("list.empty.unread.text");
		}
		if (model.getFolder() == InboxFolder.FLAGGED) {
			return InboxText.get("list.empty.flagged.text");
		}
		if (model.getFolder() == InboxFolder.ATTACHMENTS) {
			return InboxText.get("list.empty.attachments.text");
		}
		return InboxText.get("list.empty.inbox.text");
	}

	private static int indexOf(SortMode mode) {
		if (mode == SortMode.OLDEST_FIRST) {
			return 1;
		}
		if (mode == SortMode.SENDER || mode == SortMode.SENDER_ASC) {
			return 2;
		}
		if (mode == SortMode.SENDER_DESC) {
			return 3;
		}
		if (mode == SortMode.SUBJECT || mode == SortMode.SUBJECT_ASC) {
			return 4;
		}
		if (mode == SortMode.SUBJECT_DESC) {
			return 5;
		}
		return 0;
	}

	private static SortMode modeAt(int index) {
		if (index == 1) {
			return SortMode.OLDEST_FIRST;
		}
		if (index == 2) {
			return SortMode.SENDER_ASC;
		}
		if (index == 3) {
			return SortMode.SENDER_DESC;
		}
		if (index == 4) {
			return SortMode.SUBJECT_ASC;
		}
		if (index == 5) {
			return SortMode.SUBJECT_DESC;
		}
		return SortMode.NEWEST_FIRST;
	}

	private static final class RowRenderer implements ListCellRenderer<InboxMessage> {

		private final DefaultListCellRenderer probe = new DefaultListCellRenderer();

		@Override
		public Component getListCellRendererComponent(JList<? extends InboxMessage> list, InboxMessage message,
				int index, boolean selected, boolean focus) {
			probe.getListCellRendererComponent(list, message, index, selected, focus);

			int rowWidth = list.getWidth();
			if (rowWidth <= 0 && list.getParent() instanceof javax.swing.JViewport) {
				rowWidth = list.getParent().getWidth();
			}
			if (rowWidth <= 0) {
				rowWidth = 260;
			}

			JPanel row = new JPanel(new BorderLayout(8, 0));
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createCompoundBorder(
							BorderFactory.createMatteBorder(0, selected ? 3 : 0, 0, 0, UiTokens.ACCENT),
							BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.BORDER)),
					BorderFactory.createEmptyBorder(7, selected ? 5 : 8, 7, 6)));
			row.setBackground(selected ? UiTokens.SELECTED_BG : UiTokens.LIST_BG);
			row.setOpaque(true);
			row.setPreferredSize(new Dimension(rowWidth, UiTokens.LIST_ROW_HEIGHT));
			row.setMaximumSize(new Dimension(rowWidth, UiTokens.LIST_ROW_HEIGHT));

			ParsedEmail parsed = message.getParsed();
			String fullSender = InboxText.get("reader.unknown");
			String fullSubject = InboxText.get("reader.nosubject");
			String fullPreview = "";
			boolean attachments = false;
			if (parsed != null) {
				if (!Texts.isBlank(parsed.senderLine())) {
					fullSender = parsed.senderLine();
				}
				if (!Texts.isBlank(parsed.getSubject())) {
					fullSubject = parsed.getSubject();
				}
				fullPreview = blank(parsed.getPreview());
				attachments = parsed.hasAttachments();
			}

			AvatarLabel avatar = new AvatarLabel();
			avatar.setIdentity(parsed == null ? fullSender : parsed.senderLine());
			JPanel west = new JPanel(new BorderLayout(4, 0));
			west.setOpaque(false);
			if (!message.isRead()) {
				JLabel dot = new JLabel("\u25CF");
				dot.setForeground(UiTokens.ACCENT);
				dot.setFont(dot.getFont().deriveFont(8f));
				west.add(dot, BorderLayout.WEST);
			} else {
				west.add(javax.swing.Box.createHorizontalStrut(8), BorderLayout.WEST);
			}
			west.add(avatar, BorderLayout.CENTER);
			row.add(west, BorderLayout.WEST);

			String marks = "";
			if (message.isFlagged()) {
				marks += "\u2605 ";
			}
			if (attachments) {
				marks += "\u2709 ";
			}
			String dateText = marks + MailDates.format(message.getReceivedDate());
			Font dateFont = UiTokens.compactFont(list.getFont());
			FontMetrics dateMetrics = list.getFontMetrics(dateFont);
			int dateWidth = dateMetrics.stringWidth(dateText) + 8;
			int westWidth = UiTokens.AVATAR_SIZE + 20;
			int textWidth = Math.max(40, rowWidth - westWidth - dateWidth - 28);

			Font senderFont = list.getFont().deriveFont(message.isRead() ? Font.PLAIN : Font.BOLD, 12.5f);
			Font subjectFont = list.getFont().deriveFont(message.isRead() ? Font.PLAIN : Font.BOLD, 12.5f);
			Font previewFont = UiTokens.compactFont(list.getFont());
			String sender = TextFit.ellipsize(fullSender, list.getFontMetrics(senderFont), textWidth);
			String subject = TextFit.ellipsize(fullSubject, list.getFontMetrics(subjectFont), textWidth);
			String preview = TextFit.ellipsize(fullPreview, list.getFontMetrics(previewFont), textWidth);

			JPanel text = new JPanel(new BorderLayout(0, 1));
			text.setOpaque(false);
			JPanel top = new JPanel(new BorderLayout(4, 0));
			top.setOpaque(false);
			JLabel senderLabel = new JLabel(sender);
			senderLabel.setForeground(UiTokens.TEXT);
			senderLabel.setFont(senderFont);
			senderLabel.setToolTipText(fullSender);
			JLabel dateLabel = new JLabel(dateText.trim());
			dateLabel.setFont(dateFont);
			dateLabel.setForeground(message.isFlagged() ? UiTokens.STAR : UiTokens.TEXT_SECONDARY);
			dateLabel.setPreferredSize(new Dimension(dateWidth, dateMetrics.getHeight()));
			JLabel subjectLabel = new JLabel(subject);
			subjectLabel.setForeground(UiTokens.TEXT);
			subjectLabel.setFont(subjectFont);
			subjectLabel.setToolTipText(fullSubject);
			JLabel previewLabel = new JLabel(preview);
			previewLabel.setFont(previewFont);
			previewLabel.setForeground(UiTokens.PREVIEW_MUTED);
			if (!Texts.isBlank(fullPreview)) {
				previewLabel.setToolTipText(fullPreview);
			}
			if (selected) {
				senderLabel.setForeground(UiTokens.SELECTED_FG);
				subjectLabel.setForeground(UiTokens.SELECTED_FG);
			}
			top.add(senderLabel, BorderLayout.CENTER);
			top.add(dateLabel, BorderLayout.EAST);
			text.add(top, BorderLayout.NORTH);
			text.add(subjectLabel, BorderLayout.CENTER);
			text.add(previewLabel, BorderLayout.SOUTH);
			row.add(text, BorderLayout.CENTER);
			return row;
		}

		private static String blank(String value) {
			return value == null ? "" : value;
		}
	}
}
