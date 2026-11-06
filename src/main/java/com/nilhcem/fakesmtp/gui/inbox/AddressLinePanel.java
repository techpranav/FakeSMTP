package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.EmailAddress;

/**
 * Renders a label plus individually clickable addresses (From / To / CC / BCC).
 *
 * @since 2.1
 */
public final class AddressLinePanel extends JPanel {

	private final JLabel caption = new JLabel();
	private final JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

	public AddressLinePanel() {
		super(new FlowLayout(FlowLayout.LEFT, 4, 0));
		setOpaque(false);
		links.setOpaque(false);
		caption.setFont(UiTokens.compactFont(caption.getFont()));
		add(caption);
		add(links);
	}

	public void setAddresses(String label, List<EmailAddress> addresses) {
		caption.setText(label + ":");
		links.removeAll();
		if (addresses == null || addresses.isEmpty()) {
			JLabel empty = new JLabel(InboxText.get("reader.none"));
			empty.setFont(UiTokens.compactFont(empty.getFont()));
			empty.setForeground(UiTokens.TEXT_SECONDARY);
			links.add(empty);
		} else {
			boolean first = true;
			for (EmailAddress address : addresses) {
				if (address == null || Texts.isBlank(address.getAddress())) {
					continue;
				}
				if (!first) {
					JLabel sep = new JLabel("; ");
					sep.setForeground(UiTokens.TEXT_SECONDARY);
					sep.setFont(UiTokens.compactFont(sep.getFont()));
					links.add(sep);
				}
				AddressLinkLabel link = new AddressLinkLabel();
				link.setFont(UiTokens.compactFont(link.getFont()));
				link.setAddress(address);
				links.add(link);
				first = false;
			}
			if (first) {
				JLabel empty = new JLabel(InboxText.get("reader.none"));
				empty.setFont(UiTokens.compactFont(empty.getFont()));
				empty.setForeground(UiTokens.TEXT_SECONDARY);
				links.add(empty);
			}
		}
		applyTheme();
		revalidate();
		repaint();
	}

	public void applyTheme() {
		caption.setForeground(UiTokens.TEXT_SECONDARY);
		for (java.awt.Component child : links.getComponents()) {
			if (child instanceof AddressLinkLabel) {
				((AddressLinkLabel) child).applyTheme();
			} else if (child instanceof JLabel) {
				child.setForeground(UiTokens.TEXT_SECONDARY);
			}
		}
	}
}
