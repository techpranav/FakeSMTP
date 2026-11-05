package com.nilhcem.fakesmtp.license;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ulm.license.core.LicenseResult;
import com.ulm.license.core.TrustedKeys;
import com.ulm.license.model.LicenseStatus;
import com.ulm.license.sdk.LicenseManager;

/**
 * Startup license gate for MailBox. The application JAR ships without a license file;
 * a valid ULM {@code MAILBOX} license must be present (or selected via the file dialog)
 * before SMTP or the GUI may start.
 */
public final class MailBoxLicenseGate {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailBoxLicenseGate.class);

	/** ULM product code — must match licenses issued by the Unified License Manager. */
	public static final String PRODUCT_CODE = "MAILBOX";

	private MailBoxLicenseGate() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Validates the MailBox license. In GUI mode, offers a file chooser when the license
	 * is missing, invalid, or expired. In background / headless mode, prints a clear
	 * error and returns {@code false} (no interactive picker).
	 *
	 * @param guiMode {@code true} when the Swing UI will run; {@code false} for {@code -s} background SMTP
	 * @return {@code true} only when a valid license is available
	 */
	public static boolean ensureLicensed(boolean guiMode) {
		LicenseManager licenses = createManager();
		LicenseResult result = licenses.validate();
		if (result.isValid()) {
			logValid(licenses);
			return true;
		}

		boolean interactive = guiMode && !GraphicsEnvironment.isHeadless();
		if (!interactive) {
			printFailure(result, licenses);
			return false;
		}

		while (!result.isValid()) {
			int choice = JOptionPane.showOptionDialog(
					null,
					buildDialogMessage(result, licenses),
					"MailBox — License required",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null,
					new Object[] {"Select license…", "Exit"},
					"Select license…");
			if (choice != 0) {
				LOGGER.warn("MailBox startup cancelled: no valid license ({})", result.getStatus());
				return false;
			}

			File chosen = chooseLicenseFile();
			if (chosen == null) {
				continue;
			}

			result = licenses.installLicense(chosen.toPath());
			if (!result.isValid()) {
				JOptionPane.showMessageDialog(
						null,
						statusHeadline(result.getStatus()) + "\n\n" + result.getMessage()
								+ "\n\nSelected file:\n" + chosen.getAbsolutePath(),
						"MailBox — License not accepted",
						JOptionPane.ERROR_MESSAGE);
			}
		}

		logValid(licenses);
		JOptionPane.showMessageDialog(
				null,
				"License accepted.\n\nInstalled for MailBox at:\n" + licenses.getDefaultLicenseDirectory(),
				"MailBox — License installed",
				JOptionPane.INFORMATION_MESSAGE);
		return true;
	}

	private static LicenseManager createManager() {
		LicenseManager.Builder builder = LicenseManager.builder(PRODUCT_CODE);
		Path sidecar = Path.of("ulm", "trusted-keys.properties");
		if (java.nio.file.Files.isRegularFile(sidecar)) {
			try {
				builder.trustedKeys(TrustedKeys.fromFile(sidecar));
			} catch (Exception e) {
				LOGGER.warn("Could not read ulm/trusted-keys.properties: {}", e.getMessage());
			}
		}
		return builder.build();
	}

	private static File chooseLicenseFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select MailBox license file (.lic)");
		chooser.setFileFilter(new FileNameExtensionFilter("ULM license (*.lic)", "lic"));
		chooser.setAcceptAllFileFilterUsed(true);
		int outcome = chooser.showOpenDialog(null);
		if (outcome != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		return chooser.getSelectedFile();
	}

	private static String buildDialogMessage(LicenseResult result, LicenseManager licenses) {
		StringBuilder sb = new StringBuilder();
		sb.append(statusHeadline(result.getStatus())).append("\n\n");
		sb.append(result.getMessage()).append("\n\n");
		sb.append("MailBox cannot be used without a valid license.\n");
		sb.append("Default license folder:\n");
		sb.append(licenses.getDefaultLicenseDirectory()).append("\n\n");
		sb.append("Select a .lic file issued for product MAILBOX, or Exit.");
		return sb.toString();
	}

	private static String statusHeadline(LicenseStatus status) {
		switch (status) {
			case MISSING_LICENSE:
				return "License not available.";
			case EXPIRED:
				return "License expired. Select a new license to continue.";
			case NOT_YET_VALID:
				return "License not yet valid.";
			case WRONG_PRODUCT:
				return "License is for a different product.";
			case MACHINE_MISMATCH:
				return "License is bound to a different machine.";
			case INVALID_SIGNATURE:
				return "License signature is invalid.";
			case INVALID_FORMAT:
				return "License file is damaged or invalid.";
			case UNSUPPORTED_SCHEMA:
				return "License format is not supported.";
			case MISSING_ENTITLEMENT:
				return "License is missing a required feature.";
			case TAMPERED:
				return "Application integrity check failed.";
			case VALIDATION_ERROR:
				return "License could not be validated.";
			default:
				return "License is not valid.";
		}
	}

	private static void printFailure(LicenseResult result, LicenseManager licenses) {
		System.err.println("ERROR: " + statusHeadline(result.getStatus()));
		System.err.println(result.getMessage());
		if (result.getTechnicalDetail() != null && !result.getTechnicalDetail().isBlank()) {
			System.err.println(result.getTechnicalDetail());
		}
		result.getSource().ifPresent(p -> System.err.println("License file: " + p));
		System.err.println("Expected product: " + PRODUCT_CODE);
		System.err.println("Default license folder: " + licenses.getDefaultLicenseDirectory());
		System.err.println("Override with -Dulm.license.file=<path> or place MAILBOX.lic in the default folder.");
		System.err.println("MailBox cannot run without a valid license.");
		LOGGER.error("MailBox license check failed: {} — {}", result.getStatus(), result.getMessage());
	}

	private static void logValid(LicenseManager licenses) {
		licenses.getLicenseFile().ifPresent(f -> LOGGER.info("MailBox license OK ({})", f));
		licenses.getExpiryDate().ifPresent(expiry ->
				LOGGER.info("MailBox license valid until {}", expiry));
	}
}
