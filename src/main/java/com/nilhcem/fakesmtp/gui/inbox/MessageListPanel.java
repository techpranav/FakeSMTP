package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

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

/**
 * Searchable list of received messages.
 *
 * @since 2.1
 */
public final class MessageListPanel extends JPanel {

	private static final int ROW_HEIGHT = 88;
	private static final Color UNREAD_DOT = new Color(0x1A73E8);
	private static final Color PREVIEW_COLOR = new Color(0x5F6368);
	private static final Color DATE_COLOR = new Color(0x80868B);
	private static final Color DIVIDER = new Color(0xE8EAED);
	private static final Color SELECTED_BG = new Color(0xE8F0FE);
	private static final Color SELECTED_FG = new Color(0x174EA6);

	private final InboxModel model;
	private final DefaultListModel<InboxMessage> items = new DefaultListModel<InboxMessage>();
	private final JList<InboxMessage> list = new JList<InboxMessage>(items);
	private final JTextField search = new JTextField();
	private final JPanel cards = new JPanel(new CardLayout());
	private final EmptyState empty = new EmptyState("", "");
	private final MessageReaderPanel reader;
	private boolean updating;
	private Runnable refreshAction;

	public MessageListPanel(InboxModel model, MessageReaderPanel reader) {
		super(new BorderLayout(0, 8));
		this.model = model;
		this.reader = reader;
		setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 6));
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
		JComboBox<String> sort = new JComboBox<String>(new String[] {
			InboxText.get("list.newest"), InboxText.get("list.oldest")
		});
		if (!model.isNewestFirst()) {
			sort.setSelectedIndex(1);
		}
		sort.addActionListener(event -> model.setNewestFirst(sort.getSelectedIndex() == 0));
		JButton refresh = new JButton(InboxText.get("list.refresh"));
		refresh.addActionListener(event -> {
			if (refreshAction != null) {
				refreshAction.run();
			}
		});
		JPanel tools = new JPanel(new BorderLayout(6, 6));
		tools.setOpaque(false);
		tools.add(search, BorderLayout.CENTER);
		JPanel toolsRow = new JPanel(new BorderLayout(6, 0));
		toolsRow.setOpaque(false);
		toolsRow.add(sort, BorderLayout.CENTER);
		toolsRow.add(refresh, BorderLayout.EAST);
		tools.add(toolsRow, BorderLayout.SOUTH);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFixedCellHeight(ROW_HEIGHT);
		list.setCellRenderer(new RowRenderer());
		list.setBorder(null);
		list.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				onSelection();
			}
		});
		JScrollPane listScroll = new JScrollPane(list);
		listScroll.getVerticalScrollBar().setUnitIncrement(16);
		listScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));
		cards.add(listScroll, "list");
		cards.add(empty, "empty");
		setMinimumSize(new Dimension(240, 160));
		add(tools, BorderLayout.NORTH);
		add(cards, BorderLayout.CENTER);
		reload();
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
		if (model.getFolder() == InboxFolder.ATTACHMENTS) {
			return InboxText.get("list.empty.attachments.text");
		}
		return InboxText.get("list.empty.inbox.text");
	}

	/**
	 * One inbox row with sender, time, subject, and preview.
	 */
	private static final class RowRenderer implements ListCellRenderer<InboxMessage> {

		private final DefaultListCellRenderer probe = new DefaultListCellRenderer();

		@Override
		public Component getListCellRendererComponent(JList<? extends InboxMessage> list, InboxMessage message,
				int index, boolean selected, boolean focus) {
			probe.getListCellRendererComponent(list, message, index, selected, focus);

			JPanel row = new JPanel(new BorderLayout(8, 0));
			row.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
					BorderFactory.createEmptyBorder(10, 10, 10, 10)));
			if (selected) {
				row.setBackground(SELECTED_BG);
			} else if (index % 2 == 0) {
				row.setBackground(Color.WHITE);
			} else {
				row.setBackground(new Color(0xFAFBFC));
			}
			row.setOpaque(true);

			JPanel unreadMark = new JPanel(new GridBagLayout());
			unreadMark.setOpaque(false);
			unreadMark.setPreferredSize(new Dimension(10, 10));
			JLabel dot = new JLabel(message.isRead() ? " " : "\u25CF");
			dot.setForeground(UNREAD_DOT);
			dot.setFont(dot.getFont().deriveFont(10f));
			unreadMark.add(dot);
			row.add(unreadMark, BorderLayout.WEST);

			ParsedEmail parsed = message.getParsed();
			String sender = InboxText.get("reader.unknown");
			String subject = InboxText.get("reader.nosubject");
			String preview = "";
			String clip = "";
			if (parsed != null) {
				if (!Texts.isBlank(parsed.senderLine())) {
					sender = shorten(parsed.senderLine(), 42);
				}
				if (!Texts.isBlank(parsed.getSubject())) {
					subject = shorten(parsed.getSubject(), 48);
				}
				preview = shorten(parsed.getPreview(), 70);
				if (parsed.hasAttachments()) {
					clip = " \u2709";
				}
			}

			JPanel text = new JPanel(new BorderLayout(0, 2));
			text.setOpaque(false);
			JPanel top = new JPanel(new BorderLayout());
			top.setOpaque(false);
			JLabel senderLabel = new JLabel(sender + clip);
			JLabel dateLabel = new JLabel(MailDates.format(message.getReceivedDate()));
			dateLabel.setFont(dateLabel.getFont().deriveFont(11f));
			dateLabel.setForeground(DATE_COLOR);
			JLabel subjectLabel = new JLabel(subject);
			JLabel previewLabel = new JLabel(preview);
			previewLabel.setFont(previewLabel.getFont().deriveFont(11f));
			previewLabel.setForeground(PREVIEW_COLOR);
			if (!message.isRead()) {
				senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD));
				subjectLabel.setFont(subjectLabel.getFont().deriveFont(Font.BOLD));
			}
			if (selected) {
				senderLabel.setForeground(SELECTED_FG);
				subjectLabel.setForeground(SELECTED_FG);
				previewLabel.setForeground(SELECTED_FG);
				dateLabel.setForeground(SELECTED_FG);
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
