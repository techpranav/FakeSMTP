package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.InboxMessage;

import net.miginfocom.swing.MigLayout;

/**
 * Compact message header chrome. Does not alter HTML body styling.
 *
 * @since 2.1
 */
public final class MessageHeaderPanel extends JPanel {

	private static final int MAX_ADDRESS_HEIGHT = 52;

	private final JLabel subject = new JLabel(" ");
	private final AvatarLabel avatar = new AvatarLabel();
	private final JLabel fromValue = valueLabel();
	private final JLabel toValue = valueLabel();
	private final JLabel ccValue = valueLabel();
	private final JLabel bccValue = valueLabel();
	private final JLabel replyValue = valueLabel();
	private final JLabel dateValue = valueLabel();
	private final JPanel fromRow;
	private final JPanel toRow;
	private final JPanel ccRow;
	private final JPanel bccRow;
	private final JPanel replyRow;
	private final JPanel dateRow;

	public MessageHeaderPanel() {
		super(new MigLayout("insets 0, fillx, gap 0 4", "[grow,fill]", "[]8[]2[]2[]2[]2[]2[]"));
		setOpaque(false);
		subject.setFont(UiTokens.subjectFont(subject.getFont()));
		subject.setForeground(UiTokens.TEXT);
		add(subject, "growx, wrap");

		JPanel identity = new JPanel(new BorderLayout(10, 0));
		identity.setOpaque(false);
		identity.add(avatar, BorderLayout.WEST);
		JPanel identityText = new JPanel(new MigLayout("insets 0, fillx, gap 0 2", "[grow,fill]", "[][]"));
		identityText.setOpaque(false);
		fromRow = fieldRow(InboxText.get("reader.from"), fromValue, true);
		dateRow = fieldRow(InboxText.get("reader.date"), dateValue, false);
		identityText.add(fromRow, "growx, wrap");
		identityText.add(dateRow, "growx, wrap");
		identity.add(identityText, BorderLayout.CENTER);
		add(identity, "growx, wrap");

		toRow = addField(InboxText.get("reader.to"), toValue, true);
		ccRow = addField(InboxText.get("reader.cc"), ccValue, true);
		bccRow = addField(InboxText.get("reader.bcc"), bccValue, true);
		replyRow = addField(InboxText.get("reader.replyTo"), replyValue, false);
		setBorder(BorderFactory.createCompoundBorder(
				UiTokens.bottomHairline(),
				BorderFactory.createEmptyBorder(0, 0, 10, 0)));
	}

	public void showMessage(InboxMessage message) {
		ParsedEmail parsed = message.getParsed();
		subject.setText("<html><div style='width:100%'>" + escape(subjectOf(parsed)) + "</div></html>");
		avatar.setIdentity(parsed.senderLine());
		fromValue.setText(htmlValue(valueOrUnknown(parsed.senderLine())));
		toValue.setText(htmlValue(valueOrNone(parsed.recipientLine())));
		String cc = ParsedEmail.join(parsed.getCc());
		String bcc = ParsedEmail.join(parsed.getBcc());
		String reply = ParsedEmail.join(parsed.getReplyTo());
		fromRow.setVisible(true);
		toRow.setVisible(true);
		dateRow.setVisible(true);
		ccRow.setVisible(!Texts.isBlank(cc));
		bccRow.setVisible(!Texts.isBlank(bcc));
		replyRow.setVisible(!Texts.isBlank(reply));
		ccValue.setText(htmlValue(cc));
		bccValue.setText(htmlValue(bcc));
		replyValue.setText(htmlValue(reply));
		dateValue.setText(htmlValue(MailDates.format(message.getReceivedDate())));
		revalidate();
		repaint();
	}

	private JPanel addField(String labelText, JLabel value, boolean scrollable) {
		JPanel row = fieldRow(labelText, value, scrollable);
		add(row, "growx, wrap");
		return row;
	}

	private static JPanel fieldRow(String labelText, JLabel value, boolean scrollable) {
		JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 0 10", "[56!][grow,fill]", "[]"));
		row.setOpaque(false);
		JLabel label = new JLabel(labelText);
		label.setForeground(UiTokens.TEXT_SECONDARY);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		label.setVerticalAlignment(SwingConstants.TOP);
		row.add(label, "top");
		if (scrollable) {
			JScrollPane scroll = new JScrollPane(value);
			scroll.setBorder(null);
			scroll.setOpaque(false);
			scroll.getViewport().setOpaque(false);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, MAX_ADDRESS_HEIGHT));
			row.add(scroll, "growx");
		} else {
			row.add(value, "growx");
		}
		return row;
	}

	private static JLabel valueLabel() {
		JLabel label = new JLabel(" ");
		label.setForeground(UiTokens.TEXT);
		label.setVerticalAlignment(SwingConstants.TOP);
		return label;
	}

	private static String subjectOf(ParsedEmail parsed) {
		if (Texts.isBlank(parsed.getSubject())) {
			return InboxText.get("reader.nosubject");
		}
		return parsed.getSubject();
	}

	private static String valueOrUnknown(String value) {
		if (Texts.isBlank(value)) {
			return InboxText.get("reader.unknown");
		}
		return value;
	}

	private static String valueOrNone(String value) {
		if (Texts.isBlank(value)) {
			return InboxText.get("reader.none");
		}
		return value;
	}

	private static String htmlValue(String value) {
		return "<html><div style='width:100%'>" + escape(value) + "</div></html>";
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
