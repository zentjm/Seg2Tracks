package calculations;

import geometricTools.GeometricCalculations;

/**
 * Computes pixel area of a segmented cell.
 * Returns the number of pixels contained within the cell's internal perimeter boundary.
 */
public class Area extends SegmentCalculation {

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Area"
	 */
	@Override
	public String getName() {
		return "Area";
	}

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Area is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the pixel area of the segment's internal perimeter region.
	 * //TODO: move calculations here
	 *
	 * @return number of pixels within the internal boundary
	 */
	@Override //TODO: move calculations here
	public double calculate() {
		double answer = (double) GeometricCalculations.getAreaByRoi(segments[0].getInternalPerimeter()).length;
		//System.out.println("Answer is: " + answer);
		return answer;
	}
}
