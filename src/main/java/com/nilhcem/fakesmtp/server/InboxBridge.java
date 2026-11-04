package com.nilhcem.fakesmtp.server;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nilhcem.fakesmtp.core.ArgsHandler;
import com.nilhcem.fakesmtp.core.Configuration;
import com.nilhcem.fakesmtp.core.UISettings;
import com.nilhcem.fakesmtp.gui.info.ClearAllButton;
import com.nilhcem.fakesmtp.mail.EmailParser;
import com.nilhcem.fakesmtp.mail.ParsedEmail;
import com.nilhcem.fakesmtp.model.EmailModel;
import com.nilhcem.fakesmtp.model.InboxMessage;
import com.nilhcem.fakesmtp.model.InboxModel;
import com.nilhcem.fakesmtp.model.UIModel;
import com.nilhcem.fakesmtp.notification.NewMailNotifier;

/**
 * Connects the existing {@link MailSaver} events to the inbox.
 * <p>
 * Parsing happens off the SMTP thread. The inbox is updated on the Swing thread.
 * A failure while parsing or notifying is logged and does not fail the SMTP transaction,
 * because this observer does not throw.
 * </p>
 *
 * @since 2.1
 */
public final class InboxBridge implements Observer {

	private static final Logger LOGGER = LoggerFactory.getLogger(InboxBridge.class);

	private final InboxModel inbox;
	private final NewMailNotifier notifier;
	private final ExecutorService executor;

	public InboxBridge(InboxModel inbox, NewMailNotifier notifier) {
		this.inbox = inbox;
		this.notifier = notifier;
		executor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "fakesmtp-mail");
			thread.setDaemon(true);
			return thread;
		});
	}

	@Override
	public void update(Observable observable, Object argument) {
		try {
			if (observable instanceof ClearAllButton) {
				SwingUtilities.invokeLater(inbox::clear);
				return;
			}
			if (!(observable instanceof MailSaver) || !(argument instanceof EmailModel)) {
				return;
			}
			EmailModel email = (EmailModel) argument;
			executor.execute(() -> accept(email));
		} catch (RuntimeException e) {
			LOGGER.warn("Could not queue a received email for the inbox", e);
		}
	}

	/**
	 * Reads {@code .eml} files already stored in the output directory.
	 * Reloaded messages are marked read and do not raise notifications.
	 *
	 * @param directory the output directory. Ignored in memory mode.
	 */
	public void loadSaved(File directory) {
		if (ArgsHandler.INSTANCE.memoryModeEnabled() || directory == null) {
			return;
		}
		executor.execute(() -> loadDirectory(directory));
	}

	public void shutdown() {
		executor.shutdownNow();
	}

	private void accept(EmailModel email) {
		try {
			boolean remote = UISettings.INSTANCE.getBoolean(UISettings.EXTERNAL_IMAGES);
			ParsedEmail parsed = EmailParser.parse(email, remote);
			SwingUtilities.invokeLater(() -> publish(email, parsed));
		} catch (RuntimeException e) {
			LOGGER.warn("Could not parse a received email", e);
		}
	}

	private void publish(EmailModel email, ParsedEmail parsed) {
		try {
			InboxMessage message = inbox.add(new InboxMessage(email, parsed, false));
			if (message != null) {
				notifier.announce(message);
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Could not display a received email", e);
		}
	}

	private void loadDirectory(File directory) {
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		}
		String suffix = Configuration.INSTANCE.get("emails.suffix");
		boolean remote = UISettings.INSTANCE.getBoolean(UISettings.EXTERNAL_IMAGES);
		for (File file : files) {
			if (!file.isFile() || !file.getName().endsWith(suffix)) {
				continue;
			}
			loadFile(file, remote);
		}
	}

	private void loadFile(File file, boolean remote) {
		try {
			String content = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
			EmailModel email = new EmailModel();
			email.setEmailStr(content);
			email.setFilePath(file.getAbsolutePath());
			email.setReceivedDate(new Date(file.lastModified()));
			email.setFrom("");
			email.setTo("");
			email.setSubject("");
			ParsedEmail parsed = EmailParser.parse(email, remote);
			SwingUtilities.invokeLater(() -> inbox.add(new InboxMessage(email, parsed, true)));
		} catch (Exception e) {
			LOGGER.warn("Could not load saved email {}", file.getName(), e);
		}
	}

	public void refreshSaved() {
		loadSaved(new File(UIModel.INSTANCE.getSavePath()));
	}
}
