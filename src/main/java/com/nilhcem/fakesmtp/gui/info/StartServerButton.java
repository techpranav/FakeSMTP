package com.nilhcem.fakesmtp.gui.info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.nilhcem.fakesmtp.core.Configuration;
import com.nilhcem.fakesmtp.core.I18n;
import com.nilhcem.fakesmtp.core.exception.BindPortException;
import com.nilhcem.fakesmtp.core.exception.InvalidHostException;
import com.nilhcem.fakesmtp.core.exception.InvalidPortException;
import com.nilhcem.fakesmtp.core.exception.OutOfRangePortException;
import com.nilhcem.fakesmtp.model.UIModel;

/**
 * Button to start the SMTP server.
 *
 * @author Nilhcem
 * @since 1.0
 */
public final class StartServerButton extends Observable implements Observer {
	private final I18n i18n = I18n.INSTANCE;

	private final JButton button = new JButton(i18n.get("startsrv.start"));
	private final JButton stopButton = new JButton(i18n.get("startsrv.stop"));
	private boolean startFailed;
	private boolean busy;
	private Runnable beforeToggle;

	/**
	 * Creates a start button to start the SMTP server.
	 * <p>
	 * If the user selects a wrong port before starting the server, the method will display an error message.
	 * </p>
	 */
	public StartServerButton() {
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleButton();
			}
		});
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleButton();
			}
		});
	}

	/**
	 * Switches the text inside the button and calls the PortTextField observer to enable/disable the port field.
	 *
	 * @see PortTextField
	 */
	public void setBeforeToggle(Runnable beforeToggle) {
		this.beforeToggle = beforeToggle;
	}

	public void toggleButton() {
		if (busy) {
			return;
		}
		busy = true;
		button.setEnabled(false);
		stopButton.setEnabled(false);
		if (beforeToggle != null) {
			beforeToggle.run();
		}
		startFailed = false;
		Thread worker = new Thread(this::runToggle, "fakesmtp-server-toggle");
		worker.setDaemon(true);
		worker.start();
	}

	private void runToggle() {
		Exception error = null;
		try {
			UIModel.INSTANCE.toggleButton();
		} catch (Exception e) {
			error = e;
		}
		final Exception finalError = error;
		SwingUtilities.invokeLater(() -> finishToggle(finalError));
	}

	private void finishToggle(Exception error) {
		busy = false;
		if (error instanceof InvalidHostException) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.invalidHost"),
					((InvalidHostException) error).getHost()));
		} else if (error instanceof InvalidPortException) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.invalidPort")));
		} else if (error instanceof BindPortException) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.bound"),
					((BindPortException) error).getPort()));
		} else if (error instanceof OutOfRangePortException) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.range"),
					((OutOfRangePortException) error).getPort()));
		} else if (error != null) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.default"), error.getMessage()));
		}

		boolean running = UIModel.INSTANCE.isStarted();
		button.setText(i18n.get("startsrv.start"));
		button.setEnabled(!running);
		stopButton.setEnabled(running);
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the JButton object.
	 *
	 * @return the JButton object.
	 */
	public JButton get() {
		return button;
	}

	/**
	 * @return the button that stops a running server.
	 */
	public JButton getStopButton() {
		return stopButton;
	}

	/**
	 * @return {@code true} when the last attempt to start the server failed.
	 */
	public boolean isStartFailed() {
		return startFailed;
	}

	/**
	 * Displays a message dialog displaying the error specified in parameter.
	 *
	 * @param error a string representing the error which will be displayed in a message dialog.
	 */
	private void displayError(String error) {
		JOptionPane.showMessageDialog(button.getParent(), error,
			String.format(i18n.get("startsrv.err.title"), Configuration.INSTANCE.get("application.name")),
			JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof PortTextField) {
			toggleButton();
		}
	}
}
