package calculations;

import dataStructure.Segment;
import dataStructure.SegmentModel;

/**
 * Computes mean value of a segment-level calculation across all segments in a frame.
 * Takes a calculation name and returns the average value across all segments.
 * Used to characterize population-level cell properties at a single time point.
 */
public class FrameSetMean extends FrameSetStatistic{

	/**
	 * Returns the name of this statistic.
	 *
	 * @return "Mean"
	 */
	@Override
	public String getName() {
		return "Mean";
	}

	/**
	 * Indicates whether this is a statistic.
	 *
	 * @return true (FrameSetMean is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate mean value of a calculation across all segments in the frame.
	 * Sums calculation values and divides by frame size.
	 * // BUG: Uses =+ instead of += for accumulation (unary plus, not addition assignment)
	 *
	 * @param calcName name of the segment calculation to aggregate
	 * @return mean value of the calculation across all frame segments
	 */
	@Override
	public double calculate(String calcName) {
		double total = 0;
		for (SegmentModel seg: frameSet) {
			total =+ seg.getCalculation(calcName);  // BUG: This is =+ (unary plus) not += (addition assignment)
		}
		return total/frameSet.size();
	}
}
