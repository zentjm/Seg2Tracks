package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

/**
 * Computes the length of the external (outer) perimeter boundary of a segmented cell.
 * Measures the true geometric arc length along the external boundary (sum of Euclidean
 * distances between consecutive perimeter points), not the point count — the point count
 * is an incidental artifact of how the boundary was simplified, not a length measurement.
 * Used to characterize cell margin properties and boundary complexity.
 */
public class ExternalPerimeter extends SegmentCalculation {

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "External Perimeter"
	 */
	@Override
	public String getName() {
		return "External Perimeter";
	}

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (External Perimeter is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the external perimeter length in pixels.
	 *
	 * @return geometric arc length of the external perimeter boundary
	 */
	@Override
	public double calculate() {
		return GeometricCalculations.arcLength(segment.getExternalPerimeter());
	}
}
