package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.InboxMessage;

import net.miginfocom.swing.MigLayout;

/**
 * Compact Outlook-style header. Keeps meta chrome tight so the email body starts sooner.
 *
 * @since 2.1
 */
public final class MessageHeaderPanel extends JPanel {

	private final JLabel subject = new JLabel(" ");
	private final AvatarLabel avatar = new AvatarLabel();
	private final JLabel sender = new JLabel(" ");
	private final JLabel date = new JLabel(" ");
	private final JLabel recipients = new JLabel(" ");
	private final JLabel detailsToggle = new JLabel(InboxText.get("reader.details.show"));
	private final JPanel details = new JPanel(new MigLayout("insets 2 0 0 0, fillx, gap 0 2", "[grow,fill]", "[]"));
	private final JLabel detailsText = new JLabel(" ");
	private boolean detailsOpen;

	public MessageHeaderPanel() {
		super(new MigLayout("insets 0, fillx, gap 0 2", "[grow,fill]", "[]2[]1[]0[]"));
		setOpaque(false);

		subject.setFont(UiTokens.subjectFont(subject.getFont()));
		subject.setForeground(UiTokens.TEXT);
		add(subject, "growx, wrap");

		JPanel identity = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		identity.setOpaque(false);
		avatar.setPreferredSize(new java.awt.Dimension(UiTokens.AVATAR_SIZE, UiTokens.AVATAR_SIZE));
		identity.add(avatar, BorderLayout.WEST);

		sender.setForeground(UiTokens.TEXT);
		sender.setFont(sender.getFont().deriveFont(Font.BOLD, 13f));
		date.setForeground(UiTokens.TEXT_SECONDARY);
		date.setFont(UiTokens.compactFont(date.getFont()));
		date.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel senderRow = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		senderRow.setOpaque(false);
		senderRow.add(sender, BorderLayout.CENTER);
		senderRow.add(date, BorderLayout.EAST);
		identity.add(senderRow, BorderLayout.CENTER);
		add(identity, "growx, wrap");

		recipients.setForeground(UiTokens.TEXT_SECONDARY);
		recipients.setFont(UiTokens.compactFont(recipients.getFont()));
		detailsToggle.setForeground(UiTokens.ACCENT);
		detailsToggle.setFont(UiTokens.compactFont(detailsToggle.getFont()));
		detailsToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		detailsToggle.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				toggleDetails();
			}
		});

		JPanel recipientRow = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		recipientRow.setOpaque(false);
		recipientRow.add(recipients, BorderLayout.CENTER);
		recipientRow.add(detailsToggle, BorderLayout.EAST);
		add(recipientRow, "growx, wrap");

		details.setOpaque(false);
		details.setVisible(false);
		detailsText.setForeground(UiTokens.TEXT_SECONDARY);
		detailsText.setFont(UiTokens.compactFont(detailsText.getFont()));
		detailsText.setVerticalAlignment(SwingConstants.TOP);
		details.add(detailsText, "growx");
		add(details, "growx, wrap, hidemode 3");

		setBorder(BorderFactory.createCompoundBorder(
				UiTokens.bottomHairline(),
				BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_SM, 0)));
	}

	public void showMessage(InboxMessage message) {
		ParsedEmail parsed = message.getParsed();
		subject.setText(subjectOf(parsed));
		avatar.setIdentity(parsed.senderLine());
		sender.setText(valueOrUnknown(parsed.senderLine()));
		date.setText(MailDates.format(message.getReceivedDate()));

		String to = valueOrNone(parsed.recipientLine());
		String cc = ParsedEmail.join(parsed.getCc());
		String bcc = ParsedEmail.join(parsed.getBcc());
		String reply = ParsedEmail.join(parsed.getReplyTo());

		StringBuilder summary = new StringBuilder();
		summary.append(InboxText.get("reader.to")).append(": ").append(shorten(to, 72));
		if (!Texts.isBlank(cc)) {
			summary.append("   ").append(InboxText.get("reader.cc")).append(": ").append(shorten(cc, 36));
		}
		recipients.setText(summary.toString());

		StringBuilder full = new StringBuilder("<html><body style='width:100%;color:#687386;font-size:11px'>");
		full.append("<b>").append(InboxText.get("reader.to")).append(":</b> ").append(escape(to)).append("<br>");
		if (!Texts.isBlank(cc)) {
			full.append("<b>").append(InboxText.get("reader.cc")).append(":</b> ").append(escape(cc)).append("<br>");
		}
		if (!Texts.isBlank(bcc)) {
			full.append("<b>").append(InboxText.get("reader.bcc")).append(":</b> ").append(escape(bcc)).append("<br>");
		}
		if (!Texts.isBlank(reply)) {
			full.append("<b>").append(InboxText.get("reader.replyTo")).append(":</b> ").append(escape(reply));
		}
		full.append("</body></html>");
		detailsText.setText(full.toString());
		detailsOpen = false;
		details.setVisible(false);
		detailsToggle.setText(InboxText.get("reader.details.show"));
		revalidate();
		repaint();
	}

	private void toggleDetails() {
		detailsOpen = !detailsOpen;
		details.setVisible(detailsOpen);
		detailsToggle.setText(detailsOpen
				? InboxText.get("reader.details.hide")
				: InboxText.get("reader.details.show"));
		revalidate();
		repaint();
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

	private static String shorten(String value, int max) {
		if (value == null) {
			return "";
		}
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max - 1) + "\u2026";
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
