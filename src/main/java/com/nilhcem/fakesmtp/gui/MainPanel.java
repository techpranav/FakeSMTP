package com.nilhcem.fakesmtp.gui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.util.Observable;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.nilhcem.fakesmtp.core.ArgsHandler;
import com.nilhcem.fakesmtp.core.Configuration;
import com.nilhcem.fakesmtp.core.I18n;
import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.gui.info.ClearAllButton;
import com.nilhcem.fakesmtp.gui.info.NbReceivedLabel;
import com.nilhcem.fakesmtp.gui.info.PortTextField;
import com.nilhcem.fakesmtp.gui.info.SaveMsgField;
import com.nilhcem.fakesmtp.gui.info.StartServerButton;
import com.nilhcem.fakesmtp.gui.inbox.InboxView;
import com.nilhcem.fakesmtp.gui.inbox.LegacyControls;
import com.nilhcem.fakesmtp.gui.inbox.SettingsPreferences;
import com.nilhcem.fakesmtp.gui.inbox.WindowRef;
import com.nilhcem.fakesmtp.gui.tab.LastMailPane;
import com.nilhcem.fakesmtp.gui.tab.LogsPane;
import com.nilhcem.fakesmtp.gui.tab.MailsListPane;
import com.nilhcem.fakesmtp.model.InboxModel;
import com.nilhcem.fakesmtp.model.UIModel;
import com.nilhcem.fakesmtp.notification.DesktopNotificationBackend;
import com.nilhcem.fakesmtp.notification.InAppToast;
import com.nilhcem.fakesmtp.notification.NewMailNotifier;
import com.nilhcem.fakesmtp.server.InboxBridge;
import com.nilhcem.fakesmtp.server.MailSaver;
import com.nilhcem.fakesmtp.server.SMTPServerHandler;

import net.miginfocom.swing.MigLayout;

/**
 * Provides the main panel of the application, which will contain all the components.
 *
 * @author Nilhcem
 * @since 1.0
 */
public final class MainPanel {
	private final I18n i18n = I18n.INSTANCE;
	private final MigLayout layout = new MigLayout("insets 0", "[grow,fill]", "[grow,fill]");
	private final JPanel mainPanel = new JPanel(layout);
	private final DirChooser dirChooser = new DirChooser(mainPanel);
	private final JLabel portLabel = new JLabel(i18n.get("mainpanel.listening.port"));
	private final PortTextField portText = new PortTextField();
	private final StartServerButton startServerBtn = new StartServerButton();
	private final JLabel receivedLabel = new JLabel(i18n.get("mainpanel.messages.received"));
	private final NbReceivedLabel nbReceivedLabel = new NbReceivedLabel();
	private final JLabel saveMessages = new JLabel(i18n.get("mainpanel.save.messages"));
	private final SaveMsgField saveMsgTextField = new SaveMsgField();
	private final LogsPane logsPane = new LogsPane();
	private final MailsListPane mailsListPane = new MailsListPane();
	private final LastMailPane lastMailPane = new LastMailPane();
	private final ClearAllButton clearAll = new ClearAllButton();
	private final WindowRef windowRef = new WindowRef();
	private final InAppToast toast = new InAppToast();
	private final InboxModel inboxModel = new InboxModel();
	private final InboxBridge bridge;
	private final InboxView inboxView;

	/**
	 * Creates the main panel.
	 * <p>
	 * The original SMTP controls stay in place and feed the inbox. The mails table is still updated
	 * so clearing emails can delete the saved files, and the SMTP log remains available from the sidebar.
	 * </p>
	 *
	 * @param menu the menu bar which will notify the directory file chooser.
	 * @param frame the main window, used by notifications and dialogs.
	 */
	public MainPanel(Observable menu, JFrame frame) {
		windowRef.set(frame);
		Image icon = Toolkit.getDefaultToolkit().getImage(
				getClass().getResource(Configuration.INSTANCE.get("application.icon.path")));
		DesktopNotificationBackend notificationBackend = new DesktopNotificationBackend(toast, icon);
		notificationBackend.setBringToFront(() -> {
			frame.setVisible(true);
			frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
			frame.toFront();
			frame.requestFocus();
		});
		NewMailNotifier notifier = new NewMailNotifier(new SettingsPreferences(windowRef), notificationBackend);
		bridge = new InboxBridge(inboxModel, notifier);
		LegacyControls controls = new LegacyControls(portText, startServerBtn, saveMsgTextField,
				nbReceivedLabel, clearAll, logsPane);
		inboxView = new InboxView(controls, inboxModel, bridge, toast, windowRef, () ->
				notificationBackend.show("MailBox", "Desktop notifications are working.\nNew mail will appear here.", "test"));
		toast.setHandler(messageId -> {
			frame.setVisible(true);
			frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
			frame.toFront();
			if (!"test".equals(messageId)) {
				inboxView.reveal(messageId);
			}
		});
		assignLabelsToFields();
		addObservers(menu);
		mainPanel.add(inboxView, "grow");
		checkArgs();
		if (!ArgsHandler.INSTANCE.memoryModeEnabled() && UISettings.INSTANCE.getBoolean(UISettings.LOAD_SAVED_EMAILS)) {
			bridge.loadSaved(new File(UIModel.INSTANCE.getSavePath()));
		}
	}

	/**
	 * Returns the JPanel object.
	 *
	 * @return the JPanel object.
	 */
	public JPanel get() {
		return mainPanel;
	}

	/**
	 * Stops the background parser when the window closes.
	 */
	public void shutdown() {
		bridge.shutdown();
	}

	/**
	 * Handles components interactions by adding observers to observable elements.
	 *
	 * @param menu the menu bar which will notify the directory file chooser.
	 */
	private void addObservers(Observable menu) {
		menu.addObserver(dirChooser);
		saveMsgTextField.addObserver(dirChooser);
		startServerBtn.addObserver(portText);
		portText.addObserver(startServerBtn);
		dirChooser.addObserver(saveMsgTextField);

		MailSaver mailSaver = SMTPServerHandler.INSTANCE.getMailSaver();
		mailSaver.addObserver(nbReceivedLabel);
		mailSaver.addObserver(mailsListPane);
		mailSaver.addObserver(lastMailPane);
		mailSaver.addObserver(clearAll);
		mailSaver.addObserver(bridge);

		clearAll.addObserver(nbReceivedLabel);
		clearAll.addObserver(mailsListPane);
		clearAll.addObserver(logsPane);
		clearAll.addObserver(lastMailPane);
		clearAll.addObserver(bridge);
	}

	/**
	 * Checks command line arguments and toggles components if necessary.
	 */
	private void checkArgs() {
		ArgsHandler args = ArgsHandler.INSTANCE;
		if (args.getPort() != null) {
			portText.setText(args.getPort());
		}
		if (args.shouldStartServerAtLaunch()) {
			startServerBtn.toggleButton();
		}
		if (args.memoryModeEnabled()) {
			saveMsgTextField.get().setEnabled(false);
		}
	}

	/**
	 * Assigns labels to components, for accessibility purpose.
	 */
	private void assignLabelsToFields() {
		portLabel.setLabelFor(portText.get());
		saveMessages.setLabelFor(saveMsgTextField.get());
		receivedLabel.setLabelFor(nbReceivedLabel.get());
	}

	/**
	 * Returns reference to portText field. Used for saving last values to file.
	 *
	 * @return reference to portText field.
	 */
	public PortTextField getPortText() {
		return portText;
	}

	/**
	 * Returns reference to saveMsgTextField. Used for saving last values to file.
	 *
	 * @return reference to saveMsgTextField.
	 */
	public SaveMsgField getSaveMsgTextField() {
		return saveMsgTextField;
	}
}
