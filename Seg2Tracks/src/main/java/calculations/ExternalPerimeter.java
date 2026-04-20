package calculations;

import dataStructure.Segment;

/**
 * Computes the length of the external (outer) perimeter boundary of a segmented cell.
 * Measures the pixel count along the external boundary.
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
	 * //TODO: Move calculations here
	 *
	 * @return length of external perimeter boundary
	 */
	@Override //TODO: Move calculations here
	public double calculate() {
		return (double) segments[0].getExternalPerimeter().length;
	}
}
