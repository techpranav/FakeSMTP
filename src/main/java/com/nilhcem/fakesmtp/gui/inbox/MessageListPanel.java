package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
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
import javax.swing.ListSelectionModel;

import com.formdev.flatlaf.FlatClientProperties;
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

	private static final int ROW_HEIGHT = 76;

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
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
		search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, InboxText.get("list.search"));
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
		tools.add(search, BorderLayout.CENTER);
		JPanel toolsRow = new JPanel(new BorderLayout(6, 0));
		toolsRow.add(sort, BorderLayout.CENTER);
		toolsRow.add(refresh, BorderLayout.EAST);
		tools.add(toolsRow, BorderLayout.SOUTH);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFixedCellHeight(ROW_HEIGHT);
		list.setCellRenderer(new RowRenderer());
		list.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				onSelection();
			}
		});
		cards.add(new JScrollPane(list), "list");
		cards.add(empty, "empty");
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
		if (model.getQuery() != null && !model.getQuery().isBlank()) {
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
		if (model.getQuery() != null && !model.getQuery().isBlank()) {
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
	 * One inbox row: sender, subject, preview, and time.
	 */
	private static final class RowRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
			InboxMessage message = (InboxMessage) value;
			ParsedEmail parsed = message.getParsed();
			String sender = InboxText.get("reader.unknown");
			String subject = InboxText.get("reader.nosubject");
			String preview = "";
			String clip = "";
			if (parsed != null) {
				if (!parsed.senderLine().isBlank()) {
					sender = parsed.senderLine();
				}
				if (parsed.getSubject() != null && !parsed.getSubject().isBlank()) {
					subject = parsed.getSubject();
				}
				preview = parsed.getPreview();
				if (parsed.hasAttachments()) {
					clip = "  \u2709";
				}
			}
			int style = Font.PLAIN;
			if (!message.isRead()) {
				style = Font.BOLD;
			}
			label.setFont(label.getFont().deriveFont(style));
			label.setText("<html><div style='width:300px;padding:2px 4px'>"
					+ "<b>" + escape(sender) + "</b>" + escape(clip)
					+ "<span style='float:right'>" + escape(MailDates.format(message.getReceivedDate())) + "</span><br>"
					+ escape(subject) + "<br><span>" + escape(preview) + "</span></div></html>");
			label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
			return label;
		}

		private static String escape(String value) {
			if (value == null) {
				return "";
			}
			return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}
}
