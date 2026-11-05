package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.mail.AttachmentFiles;
import com.nilhcem.fakesmtp.mail.HeaderField;
import com.nilhcem.fakesmtp.mail.MailAttachment;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.InboxMessage;

/**
 * Reading pane: chrome toolbar + metadata + untouched HTML body.
 *
 * @since 2.1
 */
public final class MessageReaderPanel extends JPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageReaderPanel.class);
	private static final int PREVIEW_MAX = 480;

	private final MessageActions actions;
	private final CardLayout outer = new CardLayout();
	private final JPanel outerCards = new JPanel(outer);
	private final CardLayout bodyCards = new CardLayout();
	private final JPanel body = new JPanel(bodyCards);
	private final MessageHeaderPanel headerPanel = new MessageHeaderPanel();
	private final JPanel attachmentBox = new JPanel();
	private final JScrollPane attachmentScroll;
	private final HtmlEmailView htmlView = new HtmlEmailView();
	private final JTextArea plain = area();
	private final JTextArea headers = area();
	private final JTextArea raw = area();
	private final JToggleButton htmlButton = new JToggleButton(InboxText.get("reader.html"));
	private final JToggleButton textButton = new JToggleButton(InboxText.get("reader.text"));
	private final JToggleButton headerButton = new JToggleButton(InboxText.get("reader.headers"));
	private final JToggleButton rawButton = new JToggleButton(InboxText.get("reader.raw"));
	private final JButton readButton = new JButton(InboxText.get("reader.markUnread"));
	private final JButton flagButton = new JButton(InboxText.get("reader.flag"));
	private final JPanel tools = new JPanel(new BorderLayout());
	private final JPanel headingPad = new JPanel(new BorderLayout());
	private final JPanel content = new JPanel(new BorderLayout());
	private final EmptyState emptyState = new EmptyState(InboxText.get("list.empty.selection.title"),
			InboxText.get("list.empty.selection.text"));
	private InboxMessage current;
	private int lastAttachmentWidth;

	public MessageReaderPanel(MessageActions actions) {
		super(new BorderLayout());
		this.actions = actions;
		setOpaque(true);
		setMinimumSize(new Dimension(320, 200));
		attachmentBox.setLayout(new BoxLayout(attachmentBox, BoxLayout.Y_AXIS));
		attachmentBox.setOpaque(false);
		attachmentScroll = new JScrollPane(attachmentBox);
		attachmentScroll.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		attachmentScroll.setOpaque(false);
		attachmentScroll.getViewport().setOpaque(false);
		attachmentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		attachmentScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		attachmentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
		attachmentScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				int width = attachmentScroll.getViewport().getWidth();
				if (Math.abs(width - lastAttachmentWidth) < 12) {
					return;
				}
				if (current != null && current.getParsed() != null && current.getParsed().hasAttachments()) {
					fillAttachments(current.getParsed());
				}
			}
		});
		htmlView.setLoadImages(() -> {
			if (current != null) {
				actions.loadExternalImages(current);
			}
		});
		ButtonGroup views = new ButtonGroup();
		views.add(htmlButton);
		views.add(textButton);
		views.add(headerButton);
		views.add(rawButton);
		htmlButton.addActionListener(event -> bodyCards.show(body, "html"));
		textButton.addActionListener(event -> bodyCards.show(body, "text"));
		headerButton.addActionListener(event -> bodyCards.show(body, "headers"));
		rawButton.addActionListener(event -> bodyCards.show(body, "raw"));
		htmlButton.setSelected(true);
		styleSegment(htmlButton);
		styleSegment(textButton);
		styleSegment(headerButton);
		styleSegment(rawButton);
		readButton.addActionListener(event -> toggleRead());
		flagButton.addActionListener(event -> {
			if (current != null) {
				actions.toggleFlag(current);
			}
		});

		JPopupMenu more = new JPopupMenu();
		more.add(menuItem(InboxText.get("reader.markAllRead"), () -> actions.markAllRead()));
		more.add(menuItem(InboxText.get("reader.copy"), this::copyCurrent));
		more.add(menuItem(InboxText.get("reader.saveMsg"), () -> {
			if (current != null) {
				actions.saveAsMsg(current);
			}
		}));
		more.add(menuItem(InboxText.get("reader.saveEml"), () -> {
			if (current != null) {
				actions.saveAsEml(current);
			}
		}));
		more.add(menuItem(InboxText.get("reader.screenshot"), () -> {
			if (current != null) {
				actions.saveScreenshot(current);
			}
		}));
		more.add(menuItem(InboxText.get("reader.folder"), actions::openEmailsFolder));
		more.add(menuItem(InboxText.get("reader.open"), () -> {
			if (current != null) {
				actions.openStoredFile(current);
			}
		}));
		more.addSeparator();
		more.add(menuItem(InboxText.get("reader.delete"), () -> {
			if (current != null) {
				actions.deleteMessage(current);
			}
		}));
		JButton moreButton = new JButton(InboxText.get("reader.more"));
		moreButton.putClientProperty("JButton.buttonType", "toolBarButton");
		moreButton.addActionListener(event -> more.show(moreButton, 0, moreButton.getHeight()));

		JPanel viewGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		viewGroup.setOpaque(false);
		viewGroup.add(htmlButton);
		viewGroup.add(textButton);
		viewGroup.add(headerButton);
		viewGroup.add(rawButton);
		JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actionGroup.setOpaque(false);
		readButton.putClientProperty("JButton.buttonType", "toolBarButton");
		flagButton.putClientProperty("JButton.buttonType", "toolBarButton");
		actionGroup.add(readButton);
		actionGroup.add(flagButton);
		actionGroup.add(moreButton);
		tools.setOpaque(true);
		tools.add(viewGroup, BorderLayout.WEST);
		tools.add(actionGroup, BorderLayout.EAST);

		body.setOpaque(true);
		body.setBackground(UiTokens.EMAIL_SURFACE);
		body.setMinimumSize(new Dimension(200, 120));
		body.add(htmlView, "html");
		body.add(scroll(plain), "text");
		body.add(scroll(headers), "headers");
		body.add(scroll(raw), "raw");

		JPanel heading = new JPanel();
		heading.setOpaque(false);
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		headerPanel.setAlignmentX(LEFT_ALIGNMENT);
		attachmentScroll.setAlignmentX(LEFT_ALIGNMENT);
		heading.add(headerPanel);
		heading.add(attachmentScroll);

		headingPad.setOpaque(true);
		headingPad.setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_SM, UiTokens.SPACE_MD, UiTokens.SPACE_XS, UiTokens.SPACE_MD));
		headingPad.add(heading, BorderLayout.CENTER);

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(tools, BorderLayout.NORTH);
		top.add(headingPad, BorderLayout.CENTER);

		content.setOpaque(true);
		content.add(top, BorderLayout.NORTH);
		content.add(body, BorderLayout.CENTER);
		outerCards.add(content, "message");
		outerCards.add(emptyState, "empty");
		add(outerCards, BorderLayout.CENTER);
		applyTheme();
		showNone();
	}

	public void applyTheme() {
		setBackground(UiTokens.READER_CHROME_BG);
		content.setBackground(UiTokens.READER_CHROME_BG);
		tools.setBackground(UiTokens.SURFACE_ALT);
		tools.setBorder(BorderFactory.createCompoundBorder(
				UiTokens.bottomHairline(),
				BorderFactory.createEmptyBorder(6, 12, 6, 12)));
		headingPad.setBackground(UiTokens.READER_CHROME_BG);
		headerPanel.applyTheme();
		emptyState.applyTheme();
		styleTextArea(plain);
		styleTextArea(headers);
		styleTextArea(raw);
		// HTML email surface stays independent of the app theme.
		body.setBackground(UiTokens.EMAIL_SURFACE);
		if (current != null && current.getParsed() != null) {
			fillAttachments(current.getParsed());
		}
		revalidate();
		repaint();
	}

	private static void styleTextArea(JTextArea area) {
		area.setBackground(UiTokens.SURFACE);
		area.setForeground(UiTokens.TEXT);
		area.setCaretColor(UiTokens.TEXT);
	}

	public void showNone() {
		current = null;
		outer.show(outerCards, "empty");
	}

	public void showMessage(InboxMessage message) {
		if (message == null || message.getParsed() == null) {
			showNone();
			return;
		}
		boolean switched = current == null || !current.getId().equals(message.getId());
		current = message;
		ParsedEmail parsed = message.getParsed();
		headerPanel.showMessage(message);
		plain.setText(parsed.getPlainText());
		plain.setCaretPosition(0);
		headers.setText(headerText(parsed));
		headers.setCaretPosition(0);
		raw.setText(message.getRaw());
		raw.setCaretPosition(0);
		htmlView.showEmail(parsed);
		fillAttachments(parsed);
		if (message.isRead()) {
			readButton.setText(InboxText.get("reader.markUnread"));
		} else {
			readButton.setText(InboxText.get("reader.markRead"));
		}
		if (message.isFlagged()) {
			flagButton.setText(InboxText.get("reader.unflag"));
		} else {
			flagButton.setText(InboxText.get("reader.flag"));
		}
		if (switched) {
			if (UISettings.VIEW_TEXT.equals(UISettings.INSTANCE.get(UISettings.DEFAULT_VIEW))) {
				textButton.setSelected(true);
				bodyCards.show(body, "text");
			} else {
				htmlButton.setSelected(true);
				bodyCards.show(body, "html");
			}
		}
		outer.show(outerCards, "message");
		revalidate();
		repaint();
	}

	private void toggleRead() {
		if (current == null) {
			return;
		}
		actions.markRead(current, !current.isRead());
	}

	private void copyCurrent() {
		if (current == null || current.getParsed() == null) {
			return;
		}
		ParsedEmail parsed = current.getParsed();
		StringBuilder text = new StringBuilder();
		text.append(InboxText.get("reader.from")).append(": ").append(blank(parsed.senderLine(), InboxText.get("reader.unknown"))).append('\n');
		text.append(InboxText.get("reader.to")).append(": ").append(blank(parsed.recipientLine(), InboxText.get("reader.none"))).append('\n');
		text.append(InboxText.get("reader.date")).append(": ").append(MailDates.format(current.getReceivedDate())).append('\n');
		text.append("Subject: ").append(blank(parsed.getSubject(), InboxText.get("reader.nosubject"))).append('\n');
		text.append('\n');
		if (!Texts.isBlank(parsed.getPlainText())) {
			text.append(parsed.getPlainText());
		} else if (!Texts.isBlank(parsed.getHtml())) {
			text.append(parsed.getHtml());
		} else {
			text.append(current.getRaw());
		}
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new java.awt.datatransfer.StringSelection(text.toString()), null);
	}

	private void fillAttachments(ParsedEmail parsed) {
		attachmentBox.removeAll();
		if (!parsed.hasAttachments()) {
			attachmentScroll.setVisible(false);
			attachmentScroll.setPreferredSize(new Dimension(10, 0));
			attachmentBox.revalidate();
			return;
		}
		attachmentScroll.setVisible(true);
		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setOpaque(false);
		titleRow.setAlignmentX(LEFT_ALIGNMENT);
		titleRow.setBorder(BorderFactory.createEmptyBorder(UiTokens.SPACE_XS, 0, 0, 0));
		JLabel title = new JLabel(InboxText.get("attach.title"));
		title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
		title.setForeground(UiTokens.TEXT);
		title.setOpaque(false);
		JButton saveAll = tiny(InboxText.get("attach.saveAll"));
		saveAll.addActionListener(event -> saveAll(parsed));
		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(saveAll, BorderLayout.EAST);
		attachmentBox.add(titleRow);
		attachmentBox.add(Box.createVerticalStrut(UiTokens.SPACE_XS));
		int available = attachmentScroll.getViewport().getWidth();
		if (available < 180) {
			available = Math.max(180, getWidth() - 40);
		}
		lastAttachmentWidth = available;
		int gap = UiTokens.SPACE_SM;
		int perRow = Math.max(1, (available + gap) / (UiTokens.ATTACH_CARD_WIDTH + gap));
		int cardWidth = Math.min(UiTokens.ATTACH_CARD_WIDTH,
				Math.max(160, (available - gap * (perRow - 1)) / perRow));
		JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, UiTokens.SPACE_XS));
		cards.setOpaque(false);
		cards.setAlignmentX(LEFT_ALIGNMENT);
		for (MailAttachment attachment : parsed.getAttachments()) {
			cards.add(attachmentCard(attachment, cardWidth));
		}
		attachmentBox.add(cards);
		attachmentBox.revalidate();
		int rows = (parsed.getAttachments().size() + perRow - 1) / perRow;
		int height = Math.min(160, 28 + rows * (UiTokens.ATTACH_CARD_HEIGHT + UiTokens.SPACE_XS + 4));
		attachmentScroll.setPreferredSize(new Dimension(10, height));
	}

	private JPanel attachmentCard(MailAttachment attachment, int cardWidth) {
		JPanel card = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		card.setOpaque(true);
		card.setBackground(UiTokens.SURFACE_ALT);
		card.setBorder(UiTokens.cardBorder());
		card.setPreferredSize(new Dimension(cardWidth, UiTokens.ATTACH_CARD_HEIGHT));
		card.setMaximumSize(new Dimension(cardWidth, UiTokens.ATTACH_CARD_HEIGHT));
		card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		card.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent event) {
				card.setBackground(UiTokens.HOVER_BG);
			}

			@Override
			public void mouseExited(MouseEvent event) {
				card.setBackground(UiTokens.SURFACE_ALT);
			}
		});

		String type = extensionLabel(attachment.getFileName());
		JLabel icon = new JLabel(type);
		icon.setOpaque(true);
		icon.setBackground(UiTokens.AVATAR_BG);
		icon.setForeground(UiTokens.AVATAR_FG);
		icon.setFont(icon.getFont().deriveFont(Font.BOLD, 10f));
		icon.setBorder(BorderFactory.createEmptyBorder(8, 7, 8, 7));
		icon.setHorizontalAlignment(JLabel.CENTER);
		icon.setPreferredSize(new Dimension(36, 36));
		card.add(icon, BorderLayout.WEST);

		JPanel meta = new JPanel(new BorderLayout(0, 1));
		meta.setOpaque(false);
		String rawName = attachment.getFileName();
		int nameBudget = Math.max(8, (cardWidth - 56) / 7);
		String displayName = TextFit.ellipsizeChars(rawName == null ? "attachment" : rawName, nameBudget);
		JLabel name = new JLabel(displayName == null ? "attachment" : displayName);
		name.setToolTipText(rawName);
		name.setForeground(UiTokens.TEXT);
		name.setFont(name.getFont().deriveFont(Font.BOLD, 12f));
		JLabel size = new JLabel(formatSize(attachment.getSize()));
		size.setForeground(UiTokens.TEXT_SECONDARY);
		size.setFont(UiTokens.compactFont(size.getFont()));
		JPanel title = new JPanel(new BorderLayout(0, 0));
		title.setOpaque(false);
		title.add(name, BorderLayout.NORTH);
		title.add(size, BorderLayout.SOUTH);
		meta.add(title, BorderLayout.NORTH);

		JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		actionsRow.setOpaque(false);
		JButton preview = tiny(InboxText.get("attach.preview"));
		preview.addActionListener(event -> previewAttachment(attachment));
		JButton save = tiny(InboxText.get("attach.save"));
		save.addActionListener(event -> saveOne(attachment));
		JButton open = tiny(InboxText.get("attach.open"));
		open.addActionListener(event -> openAttachment(attachment));
		actionsRow.add(preview);
		actionsRow.add(save);
		actionsRow.add(open);
		meta.add(actionsRow, BorderLayout.SOUTH);
		card.add(meta, BorderLayout.CENTER);
		return card;
	}

	private static String formatSize(long bytes) {
		if (bytes < 1024L) {
			return bytes + " B";
		}
		if (bytes < 1024L * 1024L) {
			return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
		}
		return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
	}

	private static String extensionLabel(String name) {
		if (Texts.isBlank(name) || !name.contains(".")) {
			return "FILE";
		}
		String ext = name.substring(name.lastIndexOf('.') + 1).toUpperCase(java.util.Locale.ROOT);
		if (ext.length() > 4) {
			ext = ext.substring(0, 4);
		}
		return ext;
	}

	private static JButton tiny(String text) {
		JButton button = new JButton(text);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.setFont(button.getFont().deriveFont(11f));
		return button;
	}

	private static void styleSegment(JToggleButton button) {
		button.putClientProperty("JButton.buttonType", "toolBarButton");
	}

	private static JMenuItem menuItem(String text, Runnable action) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(event -> action.run());
		return item;
	}

	private void saveOne(MailAttachment attachment) {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(AttachmentFiles.safeName(attachment.getFileName())));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		writeAttachment(chooser.getSelectedFile(), attachment);
	}

	private void saveAll(ParsedEmail parsed) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File directory = chooser.getSelectedFile();
		for (MailAttachment attachment : parsed.getAttachments()) {
			File file = AttachmentFiles.unique(directory, AttachmentFiles.safeName(attachment.getFileName()));
			writeAttachment(file, attachment);
		}
	}

	private void writeAttachment(File file, MailAttachment attachment) {
		try {
			AttachmentFiles.write(file, attachment.getData());
		} catch (IOException e) {
			LOGGER.warn("Could not save an attachment", e);
			JOptionPane.showMessageDialog(this, InboxText.get("attach.save.failed"));
		}
	}

	private void openAttachment(MailAttachment attachment) {
		try {
			File file = AttachmentFiles.temporaryCopy(attachment);
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().open(file);
				return;
			}
		} catch (Exception e) {
			LOGGER.warn("Could not open an attachment", e);
		}
		JOptionPane.showMessageDialog(this, InboxText.get("attach.open.failed"));
	}

	private void previewAttachment(MailAttachment attachment) {
		try {
			Image image = ImageIO.read(new ByteArrayInputStream(attachment.getData()));
			if (image == null) {
				JOptionPane.showMessageDialog(this, InboxText.get("attach.preview.none"));
				return;
			}
			int width = image.getWidth(null);
			if (width > PREVIEW_MAX) {
				int height = image.getHeight(null) * PREVIEW_MAX / width;
				image = image.getScaledInstance(PREVIEW_MAX, height, Image.SCALE_SMOOTH);
			}
			JOptionPane.showMessageDialog(this, new JLabel(new javax.swing.ImageIcon(image)),
					attachment.getFileName(), JOptionPane.PLAIN_MESSAGE);
		} catch (Exception e) {
			LOGGER.warn("Could not preview an attachment", e);
			JOptionPane.showMessageDialog(this, InboxText.get("attach.preview.none"));
		}
	}

	private static String headerText(ParsedEmail parsed) {
		StringBuilder text = new StringBuilder();
		for (HeaderField field : parsed.getHeaders()) {
			text.append(field.getName()).append(": ").append(field.getValue()).append('\n');
		}
		text.append("\nContent-Type: ").append(parsed.getContentType());
		text.append("\nContent-Transfer-Encoding: ").append(parsed.getTransferEncoding());
		text.append("\nMessage-ID: ").append(parsed.getMessageId());
		return text.toString();
	}

	private static String blank(String value, String fallback) {
		if (Texts.isBlank(value)) {
			return fallback;
		}
		return value;
	}

	private static JTextArea area() {
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
		styleTextArea(area);
		return area;
	}

	private static JScrollPane scroll(JTextArea area) {
		JScrollPane pane = new JScrollPane(area);
		pane.setBorder(null);
		pane.getVerticalScrollBar().setUnitIncrement(16);
		return pane;
	}
}
