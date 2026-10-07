package analysisMethod;

/**
 * An analysis cannot run because of the input data (e.g. a required segmentation step was
 * skipped), not because of a bug. The message is written for the end user and is shown as-is;
 * no stack trace is printed, since printing one makes Fiji open its Console window.
 * <p>
 * Anything else thrown from an analysis is treated as a bug and its stack trace is logged.
 */
public class AnalysisInputException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public AnalysisInputException(String message) {
		super(message);
	}
}
