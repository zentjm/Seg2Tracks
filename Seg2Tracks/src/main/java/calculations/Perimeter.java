package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

/**
 * Computes the pixel perimeter length of a segmented cell.
 * Measures the true geometric arc length of the cell's internal boundary (sum of Euclidean
 * distances between consecutive perimeter points), not the point count — the point count is
 * an incidental artifact of how the boundary was simplified, not a length measurement.
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
	 * Calculate the perimeter length in pixels.
	 *
	 * @return geometric arc length of the internal perimeter boundary
	 */
	@Override
	public double calculate() {
		return GeometricCalculations.arcLength(segment.getInternalPerimeter());
	}
}
