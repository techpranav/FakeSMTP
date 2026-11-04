package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A short explanation shown when a list or the reading pane has nothing to display.
 *
 * @since 2.1
 */
public final class EmptyState extends JPanel {

	private static final int TEXT_WIDTH = 280;
	private static final float TITLE_SIZE = 18f;

	private final JLabel title = new JLabel();
	private final JLabel text = new JLabel();

	public EmptyState(String titleText, String body) {
		super(new BorderLayout());
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setOpaque(false);
		title.setFont(title.getFont().deriveFont(Font.BOLD, TITLE_SIZE));
		title.setAlignmentX(CENTER_ALIGNMENT);
		text.setAlignmentX(CENTER_ALIGNMENT);
		box.add(Box.createVerticalGlue());
		box.add(title);
		box.add(Box.createVerticalStrut(8));
		box.add(text);
		box.add(Box.createVerticalGlue());
		add(box, BorderLayout.CENTER);
		setMessage(titleText, body);
	}

	public void setMessage(String titleText, String body) {
		title.setText(titleText);
		text.setText("<html><div style='text-align:center;width:" + TEXT_WIDTH + "px'>" + body + "</div></html>");
	}
}
