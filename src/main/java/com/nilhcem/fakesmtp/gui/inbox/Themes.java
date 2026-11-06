package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.nilhcem.fakesmtp.core.UISettings;

/**
 * Installs the light or dark window theme and updates MailBox chrome tokens.
 *
 * @since 2.1
 */
public final class Themes {

	private static final Logger LOGGER = LoggerFactory.getLogger(Themes.class);

	private Themes() {
		throw new UnsupportedOperationException();
	}

	public static void installSaved() {
		install(UISettings.INSTANCE.isDarkTheme(), null);
	}

	public static void install(boolean dark, Window window) {
		try {
			UiTokens.apply(dark);
			if (dark) {
				FlatDarkLaf.setup();
				UISettings.INSTANCE.set(UISettings.THEME, UISettings.THEME_DARK);
			} else {
				FlatLightLaf.setup();
				UISettings.INSTANCE.set(UISettings.THEME, UISettings.THEME_LIGHT);
			}
			UIManager.put("Component.arc", Integer.valueOf(UiTokens.RADIUS));
			UIManager.put("Button.arc", Integer.valueOf(UiTokens.RADIUS));
			UIManager.put("TextComponent.arc", Integer.valueOf(UiTokens.RADIUS));
			UIManager.put("Component.focusWidth", Integer.valueOf(1));
			UIManager.put("ScrollBar.thumbArc", Integer.valueOf(999));
			UIManager.put("ScrollBar.trackArc", Integer.valueOf(999));
			UIManager.put("SplitPaneDivider.gripDotCount", Integer.valueOf(3));
			if (window != null) {
				SwingUtilities.updateComponentTreeUI(window);
				window.invalidate();
				window.repaint();
			}
		} catch (RuntimeException e) {
			LOGGER.warn("Could not install the selected theme", e);
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
				LOGGER.warn("Could not fall back to the system theme", ignored);
			}
		}
	}
}
