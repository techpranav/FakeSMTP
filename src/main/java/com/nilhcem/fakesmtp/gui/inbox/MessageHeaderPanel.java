package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.EmailAddress;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.InboxMessage;

import net.miginfocom.swing.MigLayout;

/**
 * Compact Outlook-style header with clickable From / To / CC / BCC addresses.
 *
 * @since 2.1
 */
public final class MessageHeaderPanel extends JPanel {

	private final JLabel subject = new JLabel(" ");
	private final AvatarLabel avatar = new AvatarLabel();
	private final AddressLinkLabel sender = new AddressLinkLabel();
	private final JLabel date = new JLabel(" ");
	private final AddressLinePanel toLine = new AddressLinePanel();
	private final AddressLinePanel ccLine = new AddressLinePanel();
	private final AddressLinePanel bccLine = new AddressLinePanel();
	private final AddressLinePanel replyLine = new AddressLinePanel();
	private final JLabel detailsToggle = new JLabel(InboxText.get("reader.details.show"));
	private final JPanel details = new JPanel();
	private boolean detailsOpen;

	public MessageHeaderPanel() {
		super(new MigLayout("insets 0, fillx, gap 0 2", "[grow,fill]", "[]2[]1[]0[]"));
		setOpaque(false);

		subject.setFont(UiTokens.subjectFont(subject.getFont()));
		subject.setMinimumSize(new java.awt.Dimension(0, 0));
		add(subject, "growx, wmin 0, wrap");

		JPanel identity = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		identity.setOpaque(false);
		avatar.setPreferredSize(new java.awt.Dimension(UiTokens.AVATAR_SIZE, UiTokens.AVATAR_SIZE));
		identity.add(avatar, BorderLayout.WEST);

		sender.setFont(sender.getFont().deriveFont(Font.BOLD, 13f));
		date.setFont(UiTokens.compactFont(date.getFont()));
		date.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel senderRow = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		senderRow.setOpaque(false);
		senderRow.add(sender, BorderLayout.CENTER);
		senderRow.add(date, BorderLayout.EAST);
		identity.add(senderRow, BorderLayout.CENTER);
		add(identity, "growx, wrap");

		JPanel recipientSummary = new JPanel(new BorderLayout(UiTokens.SPACE_SM, 0));
		recipientSummary.setOpaque(false);
		toLine.setOpaque(false);
		recipientSummary.add(toLine, BorderLayout.CENTER);
		detailsToggle.setFont(UiTokens.compactFont(detailsToggle.getFont()));
		detailsToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		detailsToggle.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				toggleDetails();
			}
		});
		recipientSummary.add(detailsToggle, BorderLayout.EAST);
		add(recipientSummary, "growx, wrap");

		details.setOpaque(false);
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		details.setVisible(false);
		details.add(ccLine);
		details.add(bccLine);
		details.add(replyLine);
		add(details, "growx, wrap, hidemode 3");
		applyTheme();
	}

	public void applyTheme() {
		subject.setForeground(UiTokens.TEXT);
		sender.applyTheme();
		date.setForeground(UiTokens.TEXT_SECONDARY);
		detailsToggle.setForeground(UiTokens.ACCENT);
		toLine.applyTheme();
		ccLine.applyTheme();
		bccLine.applyTheme();
		replyLine.applyTheme();
		setBorder(BorderFactory.createCompoundBorder(
				UiTokens.bottomHairline(),
				BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_SM, 0)));
	}

	public void showMessage(InboxMessage message) {
		ParsedEmail parsed = message.getParsed();
		String subjectText = subjectOf(parsed);
		subject.setText(subjectText);
		subject.setToolTipText(subjectText);
		avatar.setIdentity(parsed.senderLine());
		EmailAddress from = first(parsed.getFrom());
		if (from == null && !Texts.isBlank(parsed.getEnvelopeFrom())) {
			from = new EmailAddress(null, parsed.getEnvelopeFrom());
		}
		sender.setAddress(from);
		date.setText(MailDates.format(message.getReceivedDate()));

		List<EmailAddress> to = parsed.getTo();
		if ((to == null || to.isEmpty()) && !Texts.isBlank(parsed.getEnvelopeTo())) {
			to = Collections.singletonList(new EmailAddress(null, parsed.getEnvelopeTo()));
		}
		toLine.setAddresses(InboxText.get("reader.to"), to);

		boolean hasCc = hasAddresses(parsed.getCc());
		boolean hasBcc = hasAddresses(parsed.getBcc());
		boolean hasReply = hasAddresses(parsed.getReplyTo());
		ccLine.setVisible(hasCc);
		bccLine.setVisible(hasBcc);
		replyLine.setVisible(hasReply);
		if (hasCc) {
			ccLine.setAddresses(InboxText.get("reader.cc"), parsed.getCc());
		}
		if (hasBcc) {
			bccLine.setAddresses(InboxText.get("reader.bcc"), parsed.getBcc());
		}
		if (hasReply) {
			replyLine.setAddresses(InboxText.get("reader.replyTo"), parsed.getReplyTo());
		}
		detailsToggle.setVisible(hasCc || hasBcc || hasReply);
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

	private static boolean hasAddresses(List<EmailAddress> addresses) {
		if (addresses == null) {
			return false;
		}
		for (EmailAddress address : addresses) {
			if (address != null && !Texts.isBlank(address.getAddress())) {
				return true;
			}
		}
		return false;
	}

	private static EmailAddress first(List<EmailAddress> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}
		return addresses.get(0);
	}

	private static String subjectOf(ParsedEmail parsed) {
		if (Texts.isBlank(parsed.getSubject())) {
			return InboxText.get("reader.nosubject");
		}
		return parsed.getSubject();
	}
}
