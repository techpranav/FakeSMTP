package com.nilhcem.fakesmtp.model;

/**
 * How the inbox list is ordered.
 *
 * @since 2.1
 */
public enum SortMode {
	NEWEST_FIRST,
	OLDEST_FIRST,
	SENDER_ASC,
	SENDER_DESC,
	SUBJECT_ASC,
	SUBJECT_DESC,
	/** @deprecated Prefer {@link #SUBJECT_ASC}. Kept for older preferences/tests. */
	@Deprecated
	SUBJECT,
	/** @deprecated Prefer {@link #SENDER_ASC}. Kept for older preferences/tests. */
	@Deprecated
	SENDER,
	FLAGGED_FIRST
}
