package util;

/**
 * An operation or analysis cannot run because of the user's data or choices (a step was
 * skipped, the wrong dataset was chosen, ...), not because of a bug. Its message is written for
 * the end user and is shown as-is in a plain dialog; no stack trace is printed, since printing
 * one makes Fiji open its Console window.
 * <p>
 * Anything else thrown from an operation or analysis is treated as a bug and its stack trace is
 * logged for bug reports.
 */
public class UserInputException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public UserInputException(String message) {
		super(message);
	}
}
