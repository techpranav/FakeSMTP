package com.nilhcem.fakesmtp.gui.info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import com.nilhcem.fakesmtp.core.Configuration;
import com.nilhcem.fakesmtp.core.I18n;
import com.nilhcem.fakesmtp.core.exception.*;
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
		if (beforeToggle != null) {
			beforeToggle.run();
		}
		startFailed = false;
		try {
			UIModel.INSTANCE.toggleButton();
		} catch (InvalidHostException ihe) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.invalidHost"), ihe.getHost()));
		} catch (InvalidPortException ipe) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.invalidPort")));
		} catch (BindPortException bpe) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.bound"), bpe.getPort()));
		} catch (OutOfRangePortException orpe) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.range"), orpe.getPort()));
		} catch (RuntimeException re) {
			startFailed = true;
			displayError(String.format(i18n.get("startsrv.err.default"), re.getMessage()));
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
