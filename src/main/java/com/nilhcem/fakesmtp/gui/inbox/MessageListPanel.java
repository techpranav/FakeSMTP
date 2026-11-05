package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

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
	private boolean updating;
	private Runnable refreshAction;

	public MessageListPanel(InboxModel model, MessageReaderPanel reader) {
		super(new BorderLayout(0, UiTokens.SPACE_SM));
		this.model = model;
		this.reader = reader;
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_MD, UiTokens.SPACE_MD, UiTokens.SPACE_SM, UiTokens.SPACE_SM));
		search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, InboxText.get("list.search"));
		search.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
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
		sort.setToolTipText(InboxText.get("list.sort.tip"));
		sort.setSelectedIndex(indexOf(model.getSortMode()));
		sort.addActionListener(event -> model.setSortMode(modeAt(sort.getSelectedIndex())));
		JButton refresh = new JButton(InboxText.get("list.refresh"));
		refresh.putClientProperty("JButton.buttonType", "toolBarButton");
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
		list.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				onSelection();
			}
		});
		listScroll = new JScrollPane(list);
		listScroll.getVerticalScrollBar().setUnitIncrement(16);
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

			JPanel row = new JPanel(new BorderLayout(8, 0));
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createCompoundBorder(
							BorderFactory.createMatteBorder(0, selected ? 3 : 0, 0, 0, UiTokens.ACCENT),
							BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.BORDER)),
					BorderFactory.createEmptyBorder(7, selected ? 5 : 8, 7, 6)));
			row.setBackground(selected ? UiTokens.SELECTED_BG : UiTokens.LIST_BG);
			row.setOpaque(true);

			ParsedEmail parsed = message.getParsed();
			String sender = InboxText.get("reader.unknown");
			String subject = InboxText.get("reader.nosubject");
			String preview = "";
			boolean attachments = false;
			if (parsed != null) {
				if (!Texts.isBlank(parsed.senderLine())) {
					sender = shorten(parsed.senderLine(), 34);
				}
				if (!Texts.isBlank(parsed.getSubject())) {
					subject = shorten(parsed.getSubject(), 42);
				}
				preview = shorten(parsed.getPreview(), 58);
				attachments = parsed.hasAttachments();
			}

			AvatarLabel avatar = new AvatarLabel();
			avatar.setIdentity(parsed == null ? sender : parsed.senderLine());
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

			JPanel text = new JPanel(new BorderLayout(0, 1));
			text.setOpaque(false);
			JPanel top = new JPanel(new BorderLayout(4, 0));
			top.setOpaque(false);
			JLabel senderLabel = new JLabel(sender);
			senderLabel.setForeground(UiTokens.TEXT);
			senderLabel.setFont(senderLabel.getFont().deriveFont(message.isRead() ? Font.PLAIN : Font.BOLD, 12.5f));
			JLabel dateLabel = new JLabel(MailDates.format(message.getReceivedDate()));
			dateLabel.setFont(UiTokens.compactFont(dateLabel.getFont()));
			dateLabel.setForeground(UiTokens.TEXT_SECONDARY);
			JLabel subjectLabel = new JLabel(subject);
			subjectLabel.setForeground(UiTokens.TEXT);
			subjectLabel.setFont(subjectLabel.getFont().deriveFont(message.isRead() ? Font.PLAIN : Font.BOLD, 12.5f));
			JLabel previewLabel = new JLabel(preview);
			previewLabel.setFont(UiTokens.compactFont(previewLabel.getFont()));
			previewLabel.setForeground(UiTokens.PREVIEW_MUTED);
			if (selected) {
				senderLabel.setForeground(UiTokens.SELECTED_FG);
				subjectLabel.setForeground(UiTokens.SELECTED_FG);
			}
			StringBuilder trailing = new StringBuilder();
			if (message.isFlagged()) {
				trailing.append("\u2605 ");
			}
			if (attachments) {
				trailing.append("\u2709 ");
			}
			trailing.append(MailDates.format(message.getReceivedDate()));
			dateLabel.setText(trailing.toString().trim());
			if (message.isFlagged()) {
				dateLabel.setForeground(UiTokens.STAR);
			}
			top.add(senderLabel, BorderLayout.CENTER);
			top.add(dateLabel, BorderLayout.EAST);
			text.add(top, BorderLayout.NORTH);
			text.add(subjectLabel, BorderLayout.CENTER);
			text.add(previewLabel, BorderLayout.SOUTH);
			row.add(text, BorderLayout.CENTER);
			return row;
		}

		private static String shorten(String value, int max) {
			if (value == null) {
				return "";
			}
			if (value.length() <= max) {
				return value;
			}
			return value.substring(0, max - 1) + "\u2026";
		}
	}
}
