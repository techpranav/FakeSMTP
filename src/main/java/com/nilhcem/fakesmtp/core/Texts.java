package com.nilhcem.fakesmtp.core;

/**
 * Small string helpers that stay compatible with Java 8.
 *
 * @since 2.1
 */
public final class Texts {

	private Texts() {
		throw new UnsupportedOperationException();
	}

	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
