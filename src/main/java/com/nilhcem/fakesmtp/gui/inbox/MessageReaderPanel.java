package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final MessageActions actions;
	private final CardLayout outer = new CardLayout();
	private final JPanel outerCards = new JPanel(outer);
	private final CardLayout bodyCards = new CardLayout();
	private final JPanel body = new JPanel(bodyCards);
	private final JLabel subject = new JLabel(" ");
	private final JTextArea details = new JTextArea();
	private final JPanel attachmentBox = new JPanel();
	private final HtmlEmailView htmlView = new HtmlEmailView();
	private final JTextArea plain = area();
	private final JTextArea headers = area();
	private final JTextArea raw = area();
	private final JToggleButton htmlButton = new JToggleButton(InboxText.get("reader.html"));
	private final JToggleButton textButton = new JToggleButton(InboxText.get("reader.text"));
	private final JToggleButton headerButton = new JToggleButton(InboxText.get("reader.headers"));
	private final JToggleButton rawButton = new JToggleButton(InboxText.get("reader.raw"));
	private final JButton readButton = new JButton(InboxText.get("reader.markUnread"));
	private InboxMessage current;

	public MessageReaderPanel(MessageActions actions) {
		super(new BorderLayout());
		this.actions = actions;
		subject.setFont(subject.getFont().deriveFont(Font.BOLD, subject.getFont().getSize() + 6f));
		details.setEditable(false);
		details.setLineWrap(true);
		details.setWrapStyleWord(true);
		details.setOpaque(false);
		attachmentBox.setLayout(new BoxLayout(attachmentBox, BoxLayout.Y_AXIS));
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
		JPanel tools = new JPanel();
		tools.add(htmlButton);
		tools.add(textButton);
		tools.add(headerButton);
		tools.add(rawButton);
		tools.add(readButton);
		tools.add(delete);
		tools.add(open);
		body.add(htmlView, "html");
		body.add(scroll(plain), "text");
		body.add(scroll(headers), "headers");
		body.add(scroll(raw), "raw");
		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
		JPanel heading = new JPanel(new BorderLayout(0, 8));
		heading.add(subject, BorderLayout.NORTH);
		heading.add(details, BorderLayout.CENTER);
		heading.add(attachmentBox, BorderLayout.SOUTH);
		JPanel top = new JPanel(new BorderLayout(0, 8));
		top.setOpaque(false);
		top.add(tools, BorderLayout.NORTH);
		top.add(heading, BorderLayout.CENTER);
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
		subject.setText("<html>" + escape(subjectOf(parsed)) + "</html>");
		details.setText(detailText(parsed, message));
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
	}

	private void toggleRead() {
		if (current == null) {
			return;
		}
		actions.markRead(current, !current.isRead());
	}

	private void fillAttachments(ParsedEmail parsed) {
		attachmentBox.removeAll();
		if (!parsed.hasAttachments()) {
			attachmentBox.add(new JLabel(InboxText.get("attach.none")));
			attachmentBox.revalidate();
			return;
		}
		JButton saveAll = new JButton(InboxText.get("attach.saveAll"));
		saveAll.setAlignmentX(LEFT_ALIGNMENT);
		saveAll.addActionListener(event -> saveAll(parsed));
		attachmentBox.add(saveAll);
		for (MailAttachment attachment : parsed.getAttachments()) {
			attachmentBox.add(Box.createVerticalStrut(4));
			attachmentBox.add(attachmentRow(attachment));
		}
		attachmentBox.revalidate();
	}

	private JPanel attachmentRow(MailAttachment attachment) {
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		String id = "";
		if (attachment.getContentId() != null) {
			id = "  " + attachment.getContentId();
		}
		row.add(new JLabel(attachment.getFileName() + "  " + attachment.getContentType()
				+ "  " + attachment.getSize() + " B" + id), BorderLayout.CENTER);
		JButton save = new JButton(InboxText.get("attach.save"));
		save.addActionListener(event -> saveOne(attachment));
		JButton open = new JButton(InboxText.get("attach.open"));
		open.addActionListener(event -> openAttachment(attachment));
		JButton preview = new JButton(InboxText.get("attach.preview"));
		preview.addActionListener(event -> previewAttachment(attachment));
		JPanel buttons = new JPanel();
		buttons.add(preview);
		buttons.add(save);
		buttons.add(open);
		row.add(buttons, BorderLayout.EAST);
		return row;
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

	private static String detailText(ParsedEmail parsed, InboxMessage message) {
		StringBuilder text = new StringBuilder();
		line(text, "reader.from", valueOrUnknown(parsed.senderLine()));
		line(text, "reader.to", valueOrNone(parsed.recipientLine()));
		line(text, "reader.cc", ParsedEmail.join(parsed.getCc()));
		line(text, "reader.bcc", ParsedEmail.join(parsed.getBcc()));
		line(text, "reader.replyTo", ParsedEmail.join(parsed.getReplyTo()));
		line(text, "reader.date", MailDates.format(message.getReceivedDate()));
		return text.toString().trim();
	}

	private static void line(StringBuilder text, String key, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		text.append(InboxText.get(key)).append(": ").append(value).append('\n');
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

	private static String subjectOf(ParsedEmail parsed) {
		if (parsed.getSubject() == null || parsed.getSubject().isBlank()) {
			return InboxText.get("reader.nosubject");
		}
		return parsed.getSubject();
	}

	private static String valueOrUnknown(String value) {
		if (value == null || value.isBlank()) {
			return InboxText.get("reader.unknown");
		}
		return value;
	}

	private static String valueOrNone(String value) {
		if (value == null || value.isBlank()) {
			return InboxText.get("reader.none");
		}
		return value;
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static JTextArea area() {
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		return area;
	}

	private static JScrollPane scroll(JTextArea area) {
		JScrollPane pane = new JScrollPane(area);
		pane.setBorder(null);
		return pane;
	}
}
