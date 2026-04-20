package calculations;

import geometricTools.MatrixFunctions;

/**
 * Computes the angle (theta) of the major axis principal component.
 * Characterizes cell orientation via eigenvalue decomposition.
 * Returns angle in radians relative to the horizontal axis.
 */
public class MajorAxisAngle extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Major Axis Angle is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the angle of the major axis principal component.
	 * Uses eigenvalue decomposition of the cell perimeter to determine orientation.
	 *
	 * @return major axis angle in radians
	 */
	@Override
	public double calculate() {
		MatrixFunctions functions = new MatrixFunctions();
		// Compute major axis but only retrieve the theta angle, not the vector
		functions.getMajorAxis(segments[0].getCenterPoint(), segments[0].getInternalPerimeter());
		return functions.getTheta();
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Major Axis Angle"
	 */
	@Override
	public String getName() {
		return "Major Axis Angle";
	}
}
