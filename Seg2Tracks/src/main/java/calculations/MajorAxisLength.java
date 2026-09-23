package calculations;

import geometricTools.MatrixFunctions;

/**
 * Computes the length of the major axis of a segmented cell.
 * Characterizes cell size and aspect ratio via eigenvalue decomposition.
 * Returns the longest axis length of the cell's elliptical approximation.
 */
public class MajorAxisLength extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Major Axis Length is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the length of the major axis principal component.
	 * Uses eigenvalue decomposition to determine the longest axis.
	 *
	 * @return major axis length in pixels
	 */
	@Override
	public double calculate() {
		MatrixFunctions functions = new MatrixFunctions();
		return functions.getMajorAxis(segment.getCenterPoint(), segment.getInternalPerimeter()).length;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Major Axis Length"
	 */
	@Override
	public String getName() {
		return "Major Axis Length";
	}
}
