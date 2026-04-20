package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

/**
 * Computes the pixel perimeter length of a segmented cell.
 * Measures the boundary length of the cell using the internal perimeter representation.
 * Useful for characterizing cell shape complexity and size.
 */
public class Perimeter extends SegmentCalculation {

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Perimeter"
	 */
	@Override
	public String getName() {
		return "Perimeter";
	}

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Perimeter is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the perimeter by counting pixels along the internal boundary.
	 * //TODO: Move calculations here
	 *
	 * @return perimeter length in pixels
	 */
	@Override //TODO: Move calculations here
	public double calculate() {
		return (double) segments[0].getInternalPerimeter().length;
	}
}
