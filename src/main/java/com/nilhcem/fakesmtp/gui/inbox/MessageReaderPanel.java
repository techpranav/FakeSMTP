package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
 * The reading pane: message header, attachments, and HTML, text, header, or raw body.
 *
 * @since 2.1
 */
public final class MessageReaderPanel extends JPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageReaderPanel.class);
	private static final int PREVIEW_MAX = 480;
	private static final int ATTACHMENTS_MAX_HEIGHT = 100;
	private static final Color TOOLBAR_BG = new Color(0xF8F9FA);
	private static final Color DIVIDER = new Color(0xE0E3E7);

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
	private InboxMessage current;

	public MessageReaderPanel(MessageActions actions) {
		super(new BorderLayout());
		this.actions = actions;
		setOpaque(true);
		setBackground(Color.WHITE);
		setMinimumSize(new Dimension(280, 200));
		attachmentBox.setLayout(new BoxLayout(attachmentBox, BoxLayout.Y_AXIS));
		attachmentBox.setOpaque(false);
		attachmentScroll = new JScrollPane(attachmentBox);
		attachmentScroll.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		attachmentScroll.setOpaque(false);
		attachmentScroll.getViewport().setOpaque(false);
		attachmentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		attachmentScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		attachmentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, ATTACHMENTS_MAX_HEIGHT));
		attachmentScroll.setPreferredSize(new Dimension(100, 40));
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
		readButton.addActionListener(event -> toggleRead());
		flagButton.addActionListener(event -> {
			if (current != null) {
				actions.toggleFlag(current);
			}
		});
		JButton delete = new JButton(InboxText.get("reader.delete"));
		delete.addActionListener(event -> {
			if (current != null) {
				actions.deleteMessage(current);
			}
		});
		JButton open = new JButton(InboxText.get("reader.open"));
		open.addActionListener(event -> {
			if (current != null) {
				actions.openStoredFile(current);
			}
		});
		JButton copy = new JButton(InboxText.get("reader.copy"));
		copy.setToolTipText(InboxText.get("reader.copy.tip"));
		copy.addActionListener(event -> copyCurrent());
		JButton folder = new JButton(InboxText.get("reader.folder"));
		folder.addActionListener(event -> actions.openEmailsFolder());
		JButton markAll = new JButton(InboxText.get("reader.markAllRead"));
		markAll.addActionListener(event -> actions.markAllRead());
		JButton saveMsg = new JButton(InboxText.get("reader.saveMsg"));
		saveMsg.setToolTipText(InboxText.get("reader.saveMsg.tip"));
		saveMsg.addActionListener(event -> {
			if (current != null) {
				actions.saveAsMsg(current);
			}
		});
		JButton saveEml = new JButton(InboxText.get("reader.saveEml"));
		saveEml.setToolTipText(InboxText.get("reader.saveEml.tip"));
		saveEml.addActionListener(event -> {
			if (current != null) {
				actions.saveAsEml(current);
			}
		});
		JButton screenshot = new JButton(InboxText.get("reader.screenshot"));
		screenshot.setToolTipText(InboxText.get("reader.screenshot.tip"));
		screenshot.addActionListener(event -> {
			if (current != null) {
				actions.saveScreenshot(current);
			}
		});

		JPanel viewGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		viewGroup.setOpaque(false);
		viewGroup.add(htmlButton);
		viewGroup.add(textButton);
		viewGroup.add(headerButton);
		viewGroup.add(rawButton);
		JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actionGroup.setOpaque(false);
		actionGroup.add(readButton);
		actionGroup.add(flagButton);
		actionGroup.add(markAll);
		actionGroup.add(copy);
		actionGroup.add(saveMsg);
		actionGroup.add(saveEml);
		actionGroup.add(screenshot);
		actionGroup.add(folder);
		actionGroup.add(open);
		actionGroup.add(delete);
		JPanel tools = new JPanel(new BorderLayout());
		tools.setOpaque(true);
		tools.setBackground(TOOLBAR_BG);
		tools.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
				BorderFactory.createEmptyBorder(6, 8, 6, 8)));
		tools.add(viewGroup, BorderLayout.WEST);
		tools.add(actionGroup, BorderLayout.EAST);

		body.setOpaque(true);
		body.setBackground(Color.WHITE);
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

		JPanel top = new JPanel(new BorderLayout(0, 0));
		top.setOpaque(false);
		top.add(tools, BorderLayout.NORTH);
		top.add(heading, BorderLayout.CENTER);

		JPanel content = new JPanel(new BorderLayout(0, 0));
		content.setOpaque(true);
		content.setBackground(Color.WHITE);
		content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		JPanel headingPad = new JPanel(new BorderLayout());
		headingPad.setOpaque(false);
		headingPad.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
		headingPad.add(heading, BorderLayout.CENTER);
		top.removeAll();
		top.add(tools, BorderLayout.NORTH);
		top.add(headingPad, BorderLayout.CENTER);
		content.add(top, BorderLayout.NORTH);
		content.add(body, BorderLayout.CENTER);
		outerCards.add(content, "message");
		outerCards.add(new EmptyState(InboxText.get("list.empty.selection.title"), InboxText.get("list.empty.selection.text")), "empty");
		add(outerCards, BorderLayout.CENTER);
		showNone();
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
			attachmentBox.revalidate();
			return;
		}
		attachmentScroll.setVisible(true);
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		JLabel title = new JLabel(InboxText.get("attach.title"));
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		row.add(title);
		JButton saveAll = new JButton(InboxText.get("attach.saveAll"));
		saveAll.addActionListener(event -> saveAll(parsed));
		row.add(saveAll);
		for (MailAttachment attachment : parsed.getAttachments()) {
			row.add(attachmentChip(attachment));
		}
		attachmentBox.add(row);
		attachmentBox.revalidate();
	}

	private JPanel attachmentChip(MailAttachment attachment) {
		JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		chip.setOpaque(true);
		chip.setBackground(new Color(0xF1F3F4));
		chip.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(0xDADCE0)),
				BorderFactory.createEmptyBorder(2, 6, 2, 4)));
		chip.add(new JLabel(attachment.getFileName() + " (" + attachment.getSize() + " B)"));
		JButton preview = new JButton(InboxText.get("attach.preview"));
		preview.addActionListener(event -> previewAttachment(attachment));
		JButton save = new JButton(InboxText.get("attach.save"));
		save.addActionListener(event -> saveOne(attachment));
		JButton open = new JButton(InboxText.get("attach.open"));
		open.addActionListener(event -> openAttachment(attachment));
		chip.add(preview);
		chip.add(save);
		chip.add(open);
		return chip;
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
		return area;
	}

	private static JScrollPane scroll(JTextArea area) {
		JScrollPane pane = new JScrollPane(area);
		pane.setBorder(null);
		pane.getVerticalScrollBar().setUnitIncrement(16);
		return pane;
	}
}
