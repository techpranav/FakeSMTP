package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Central MailBox chrome tokens. Do not apply these colors to rendered email HTML.
 * <p>
 * Call {@link #apply(boolean)} when the light/dark theme changes, then refresh chrome
 * components so backgrounds and borders pick up the new values.
 * </p>
 *
 * @since 2.1
 */
public final class UiTokens {

	public static Color APP_BG;
	public static Color SURFACE;
	public static Color SURFACE_ALT;
	public static Color SIDEBAR_BG;
	public static Color LIST_BG;
	public static Color READER_CHROME_BG;
	public static Color TEXT;
	public static Color TEXT_SECONDARY;
	public static Color TEXT_MUTED;
	public static Color ACCENT;
	public static Color SELECTED_BG;
	public static Color SELECTED_FG;
	public static Color HOVER_BG;
	public static Color BORDER;
	public static Color SUCCESS_BG;
	public static Color SUCCESS_FG;
	public static Color WARNING_BG;
	public static Color WARNING_FG;
	public static Color ERROR_BG;
	public static Color ERROR_FG;
	public static Color MUTED_BG;
	public static Color STAR;
	public static Color AVATAR_BG;
	public static Color AVATAR_FG;
	public static Color PREVIEW_MUTED;
	public static Color TOAST_BG;
	public static Color TOAST_BORDER;
	public static Color TOAST_TITLE;
	public static Color TOAST_BODY;
	/** Always white — email HTML viewport, independent of app theme. */
	public static final Color EMAIL_SURFACE = Color.WHITE;

	public static final int SPACE_XS = 4;
	public static final int SPACE_SM = 8;
	public static final int SPACE_MD = 12;
	public static final int SPACE_LG = 16;
	public static final int HEADER_HEIGHT = 52;
	public static final int SIDEBAR_WIDTH = 200;
	public static final int LIST_WIDTH = 300;
	public static final int LIST_ROW_HEIGHT = 74;
	public static final int AVATAR_SIZE = 30;
	public static final int ATTACH_CARD_WIDTH = 220;
	public static final int ATTACH_CARD_HEIGHT = 62;
	public static final int CONTROL_HEIGHT = 28;
	public static final int SORT_WIDTH = 136;
	public static final int TOAST_WIDTH = 340;
	public static final int RADIUS = 8;

	private static boolean dark;

	static {
		apply(false);
	}

	private UiTokens() {
		throw new UnsupportedOperationException();
	}

	public static boolean isDark() {
		return dark;
	}

	/**
	 * Updates semantic tokens for light or dark chrome. Does not touch email HTML.
	 *
	 * @param useDark {@code true} for the dark palette.
	 */
	public static void apply(boolean useDark) {
		dark = useDark;
		if (useDark) {
			APP_BG = new Color(0x171A21);
			SIDEBAR_BG = new Color(0x1D222B);
			LIST_BG = new Color(0x202631);
			READER_CHROME_BG = new Color(0x202631);
			SURFACE = new Color(0x252C37);
			SURFACE_ALT = new Color(0x29313D);
			SELECTED_BG = new Color(0x293C55);
			HOVER_BG = new Color(0x303B4A);
			BORDER = new Color(0x343C48);
			TEXT = new Color(0xE8EDF5);
			TEXT_SECONDARY = new Color(0xA5B0C0);
			TEXT_MUTED = new Color(0x8793A5);
			PREVIEW_MUTED = new Color(0x8793A5);
			ACCENT = new Color(0x5B9BE6);
			SELECTED_FG = new Color(0xD7E8FF);
			SUCCESS_BG = new Color(0x1C3A2E);
			SUCCESS_FG = new Color(0x35B879);
			WARNING_BG = new Color(0x3A3320);
			WARNING_FG = new Color(0xE5B454);
			ERROR_BG = new Color(0x3A2424);
			ERROR_FG = new Color(0xE36B6B);
			MUTED_BG = new Color(0x29313D);
			STAR = new Color(0xE5B454);
			AVATAR_BG = new Color(0x334155);
			AVATAR_FG = new Color(0xD7E8FF);
			TOAST_BG = new Color(0x29313D);
			TOAST_BORDER = new Color(0x3D4A5C);
			TOAST_TITLE = new Color(0xD7E8FF);
			TOAST_BODY = new Color(0xA5B0C0);
		} else {
			APP_BG = new Color(0xF5F7FA);
			SIDEBAR_BG = new Color(0xF5F7FA);
			LIST_BG = new Color(0xFFFFFF);
			READER_CHROME_BG = new Color(0xFFFFFF);
			SURFACE = new Color(0xFFFFFF);
			SURFACE_ALT = new Color(0xF5F7FA);
			SELECTED_BG = new Color(0xEAF2FF);
			HOVER_BG = new Color(0xF0F3F8);
			BORDER = new Color(0xE5E9F0);
			TEXT = new Color(0x202939);
			TEXT_SECONDARY = new Color(0x687386);
			TEXT_MUTED = new Color(0x8A93A3);
			PREVIEW_MUTED = new Color(0x8A93A3);
			ACCENT = new Color(0x3976D2);
			SELECTED_FG = new Color(0x1B4F9C);
			SUCCESS_BG = new Color(0xE8F7F0);
			SUCCESS_FG = new Color(0x249B69);
			WARNING_BG = new Color(0xFFF6E5);
			WARNING_FG = new Color(0xD99A28);
			ERROR_BG = new Color(0xFDECEC);
			ERROR_FG = new Color(0xD94C4C);
			MUTED_BG = new Color(0xEEF1F5);
			STAR = new Color(0xD99A28);
			AVATAR_BG = new Color(0xD7E6FB);
			AVATAR_FG = new Color(0x1B4F9C);
			TOAST_BG = new Color(0xEEF4FF);
			TOAST_BORDER = new Color(0xB7D0F5);
			TOAST_TITLE = new Color(0x174EA6);
			TOAST_BODY = new Color(0x202939);
		}
	}

	public static String toHex(Color color) {
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	public static Font brandFont(Font base) {
		return base.deriveFont(Font.BOLD, 17f);
	}

	public static Font subjectFont(Font base) {
		return base.deriveFont(Font.BOLD, 16f);
	}

	public static Font secondaryFont(Font base) {
		return base.deriveFont(Font.PLAIN, 12f);
	}

	public static Font compactFont(Font base) {
		return base.deriveFont(Font.PLAIN, 11f);
	}

	public static Font statusFont(Font base) {
		return base.deriveFont(Font.PLAIN, 12f);
	}

	public static Border bottomHairline() {
		return BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER);
	}

	public static Border rightHairline() {
		return BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER);
	}

	public static Border topHairline() {
		return BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER);
	}

	public static Border cardBorder() {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER),
				BorderFactory.createEmptyBorder(6, 10, 6, 10));
	}
}
