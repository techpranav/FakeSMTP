package com.nilhcem.fakesmtp.gui.inbox;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Central MailBox chrome tokens. Do not apply these colors to rendered email HTML.
 *
 * @since 2.1
 */
public final class UiTokens {

	public static final Color APP_BG = new Color(0xF5F7FA);
	public static final Color SURFACE = new Color(0xFFFFFF);
	public static final Color SIDEBAR_BG = new Color(0xF5F7FA);
	public static final Color TEXT = new Color(0x202939);
	public static final Color TEXT_SECONDARY = new Color(0x687386);
	public static final Color ACCENT = new Color(0x3976D2);
	public static final Color SELECTED_BG = new Color(0xEAF2FF);
	public static final Color SELECTED_FG = new Color(0x1B4F9C);
	public static final Color BORDER = new Color(0xE5E9F0);
	public static final Color SUCCESS_BG = new Color(0xE8F7F0);
	public static final Color SUCCESS_FG = new Color(0x249B69);
	public static final Color WARNING_BG = new Color(0xFFF6E5);
	public static final Color WARNING_FG = new Color(0xD99A28);
	public static final Color ERROR_BG = new Color(0xFDECEC);
	public static final Color ERROR_FG = new Color(0xD94C4C);
	public static final Color MUTED_BG = new Color(0xEEF1F5);
	public static final Color STAR = new Color(0xD99A28);
	public static final Color AVATAR_BG = new Color(0xD7E6FB);
	public static final Color AVATAR_FG = new Color(0x1B4F9C);

	public static final int SPACE_XS = 4;
	public static final int SPACE_SM = 8;
	public static final int SPACE_MD = 12;
	public static final int SPACE_LG = 16;
	public static final int HEADER_HEIGHT = 56;
	public static final int SIDEBAR_WIDTH = 200;
	public static final int LIST_WIDTH = 300;
	public static final int LIST_ROW_HEIGHT = 76;
	public static final int AVATAR_SIZE = 32;
	public static final int RADIUS = 8;

	private UiTokens() {
		throw new UnsupportedOperationException();
	}

	public static Font brandFont(Font base) {
		return base.deriveFont(Font.BOLD, 17f);
	}

	public static Font subjectFont(Font base) {
		return base.deriveFont(Font.BOLD, 17f);
	}

	public static Font secondaryFont(Font base) {
		return base.deriveFont(Font.PLAIN, 12f);
	}

	public static Border bottomHairline() {
		return BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER);
	}

	public static Border rightHairline() {
		return BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER);
	}

	public static Border cardBorder() {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER),
				BorderFactory.createEmptyBorder(6, 10, 6, 10));
	}
}
