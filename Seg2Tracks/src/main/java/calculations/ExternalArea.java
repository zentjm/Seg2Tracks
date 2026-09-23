package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

/**
 * Computes pixel area of the external (outer boundary) region of a segmented cell.
 * Measures the area bounded by the external perimeter.
 * Useful for studying cell margin properties and boundary characteristics.
 */
public class ExternalArea extends SegmentCalculation {

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "External Area"
	 */
	@Override
	public String getName() {
		return "External Area";
	}

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (External Area is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the pixel area within the external perimeter boundary.
	 * //TODO: Move calculations here
	 *
	 * @return number of pixels within the external boundary
	 */
	@Override //TODO: Move calculations here
	public double calculate() {
		return (double) GeometricCalculations.getAreaByRoi(segment.getExternalPerimeter()).length;
	}
}
