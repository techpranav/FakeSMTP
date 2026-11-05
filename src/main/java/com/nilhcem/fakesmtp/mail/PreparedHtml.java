package com.nilhcem.fakesmtp.mail;

/**
 * HTML ready to hand to the renderer, plus how many remote images were removed.
 *
 * @since 2.1
 */
public final class PreparedHtml {

	private final String xhtml;
	private final int blockedImages;

	public PreparedHtml(String xhtml, int blockedImages) {
		this.xhtml = xhtml;
		this.blockedImages = blockedImages;
	}

	public String getXhtml() {
		return xhtml;
	}

	public int getBlockedImages() {
		return blockedImages;
	}
}
