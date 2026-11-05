package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.nilhcem.fakesmtp.core.Texts;
import com.nilhcem.fakesmtp.mail.EmailAddress;

/**
 * Single clickable mailbox address with mailto open and a small context menu.
 *
 * @since 2.1
 */
public final class AddressLinkLabel extends JLabel {

	private EmailAddress address;
	private boolean hovered;

	public AddressLinkLabel() {
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setOpaque(false);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON3) {
					showMenu(event);
					return;
				}
				if (event.getButton() == MouseEvent.BUTTON1 && address != null) {
					EmailAddressActions.openMailto(AddressLinkLabel.this, address);
				}
			}

			@Override
			public void mousePressed(MouseEvent event) {
				if (event.isPopupTrigger()) {
					showMenu(event);
				}
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (event.isPopupTrigger()) {
					showMenu(event);
				}
			}

			@Override
			public void mouseEntered(MouseEvent event) {
				hovered = true;
				applyTheme();
			}

			@Override
			public void mouseExited(MouseEvent event) {
				hovered = false;
				applyTheme();
			}
		});
		applyTheme();
	}

	public void setAddress(EmailAddress address) {
		this.address = address;
		if (address == null || Texts.isBlank(address.getAddress())) {
			setText(" ");
			setToolTipText(null);
			setEnabled(false);
			return;
		}
		setEnabled(true);
		String visible = Texts.isBlank(address.getPersonal()) ? address.getAddress() : address.getPersonal();
		setText(visible);
		setToolTipText(address.format());
		applyTheme();
	}

	public void applyTheme() {
		if (!isEnabled() || address == null) {
			setForeground(UiTokens.TEXT_SECONDARY);
			return;
		}
		setForeground(UiTokens.ACCENT);
		if (hovered) {
			setText("<html><u>" + escape(displayText()) + "</u></html>");
		} else {
			setText(displayText());
		}
	}

	private String displayText() {
		if (address == null) {
			return " ";
		}
		if (!Texts.isBlank(address.getPersonal())) {
			return address.getPersonal();
		}
		return address.getAddress();
	}

	private void showMenu(MouseEvent event) {
		if (address == null || Texts.isBlank(address.getAddress())) {
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem open = new JMenuItem(InboxText.get("address.open"));
		open.addActionListener(e -> EmailAddressActions.openMailto(this, address));
		JMenuItem copyAddress = new JMenuItem(InboxText.get("address.copy"));
		copyAddress.addActionListener(e -> EmailAddressActions.copyAddress(address));
		menu.add(open);
		menu.add(copyAddress);
		if (!Texts.isBlank(address.getPersonal())) {
			JMenuItem copyName = new JMenuItem(InboxText.get("address.copyName"));
			copyName.addActionListener(e -> EmailAddressActions.copyDisplayName(address));
			menu.add(copyName);
		}
		menu.show(this, event.getX(), event.getY());
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
