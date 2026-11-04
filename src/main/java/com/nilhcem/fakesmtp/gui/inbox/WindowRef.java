package com.nilhcem.fakesmtp.gui.inbox;

import javax.swing.JFrame;

/**
 * Holds the main window so notifications can tell whether it is focused.
 *
 * @since 2.1
 */
public final class WindowRef {

	private JFrame frame;

	public void set(JFrame frame) {
		this.frame = frame;
	}

	public JFrame get() {
		return frame;
	}

	public boolean isFocused() {
		return frame != null && frame.isFocused();
	}
}
