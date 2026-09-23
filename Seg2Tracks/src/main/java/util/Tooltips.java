package util;

/**
 * Central repository for all user-facing tooltip strings.
 *
 * <p>Tooltips are grouped into nested static inner classes, one per dialog or
 * settings panel, so each constant is self-documenting at the call site
 * (e.g. {@code Tooltips.ObjectID.SIGMA}).  Add a new nested class when a new
 * panel gains tooltip coverage; do not add bare top-level constants.
 *
 * <p>All strings that contain line breaks are wrapped in {@code <html>…</html>}
 * so Swing renders them as multi-line tooltips.
 */
public final class Tooltips {

	private Tooltips() {} // non-instantiable utility class

	// ── Object ID Settings (CalibrationPanel) ────────────────────────────────

	/**
	 * Tooltips for the Object ID Settings dialog.
	 */
	public static final class ObjectID {

		private ObjectID() {}

		/** Gaussian blur sigma applied to each frame before peak detection. */
		public static final String SIGMA =
			"<html>"
			+ "Standard deviation (in pixels) of the Gaussian blur applied to each frame<br>"
			+ "before local intensity maxima are detected.<br>"
			+ "<br>"
			+ "Higher values smooth more noise but may merge nearby objects or miss small ones.<br>"
			+ "Typical range: 5–30 px."
			+ "</html>";

		/**
		 * Intensity threshold for filtering detections, entered as a percent of the
		 * frame's intensity range.
		 */
		public static final String THRESHOLD =
			"<html>"
			+ "Minimum brightness a detected point must have to be kept, expressed as a<br>"
			+ "percentage of the frame's full intensity range (0–100%).<br>"
			+ "<br>"
			+ "Each candidate is scored by the average intensity of the pixels in a small<br>"
			+ "neighbourhood around it. Candidates below this threshold are discarded.<br>"
			+ "<br>"
			+ "Higher values keep only the brightest objects. Typical range: 5–25%."
			+ "</html>";

		/**
		 * Peak-separation tolerance for recursive (subsegment) identification only,
		 * entered as a percent of the intensity range inside the masked parent segment.
		 */
		public static final String RECURSIVE_TOLERANCE =
			"<html>"
			+ "Used during recursive (subsegment) identification only — has no effect on primary<br>"
			+ "object detection.<br>"
			+ "<br>"
			+ "Sets the minimum intensity difference required to treat two nearby peaks as<br>"
			+ "separate objects, expressed as a percentage of the intensity range within the<br>"
			+ "masked parent segment region.<br>"
			+ "<br>"
			+ "Increase this value if closely spaced subsegments are being merged into one detection.<br>"
			+ "Decrease it if real subsegments are being missed. Typical range: 5–20%."
			+ "</html>";
	}

	// ── Boundary Cleanup Settings (BoundaryCleanupPanel) ─────────────────────

	/**
	 * Tooltips for the Boundary Cleanup Settings dialog.
	 */
	public static final class BoundaryCleanup {

		private BoundaryCleanup() {}

		/** How far removeLoops searches ahead for a loop/revisit artifact, as a percent
		 *  of a boundary's own point count. */
		public static final String SEARCH_FRACTION =
			"<html>"
			+ "How far ahead the loop-cleanup pass searches for a boundary point that has<br>"
			+ "drifted back close to an earlier one, expressed as a percentage of the<br>"
			+ "boundary's own point count.<br>"
			+ "<br>"
			+ "A loop artifact wider than this (measured along the boundary) will not be<br>"
			+ "detected or removed. Larger objects need a larger search window to catch<br>"
			+ "the same size of defect. Typical range: 10–25%."
			+ "</html>";

		/** Absolute upper bound (points) on the search window regardless of the percent. */
		public static final String SEARCH_CEILING =
			"<html>"
			+ "Hard upper limit (in boundary points) on the loop-cleanup search window,<br>"
			+ "regardless of the Search Distance percentage.<br>"
			+ "<br>"
			+ "Caps processing time on very large boundaries. Lower this if boundary<br>"
			+ "cleanup is slow on large objects; raise it if large loop artifacts on large<br>"
			+ "objects are not being caught."
			+ "</html>";

		/** Douglas-Peucker perpendicular-distance simplification tolerance. */
		public static final String EPSILON =
			"<html>"
			+ "Maximum distance (in pixels) a boundary point may deviate from a straightened<br>"
			+ "edge before it is kept instead of simplified away.<br>"
			+ "<br>"
			+ "Higher values produce simpler boundaries with fewer points, but can round off<br>"
			+ "small real features. Lower values preserve more detail but keep more points.<br>"
			+ "Typical range: 1–3 px."
			+ "</html>";
	}

	// ── Add further nested classes here as new panels gain tooltip coverage ──
	// Example:
	//
	// public static final class Linkage { … }
}
