package calculations;

import dataStructure.Segment;

/**
 * Computes mean value of a segment-level calculation across all frames in a LinkSet (cell track).
 * Takes a calculation name and returns the average value across all time points.
 * Used to characterize cell behavior properties that aggregate across the cell's lifetime.
 */
public class LinkSetMean extends LinkSetStatistic {

	/**
	 * Constructor initializing LinkSetMean statistic.
	 * // TODO Auto-generated constructor stub
	 */
	public LinkSetMean() {
		// TODO Auto-generated constructor stub
	}

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
	 * @return true (LinkSetMean is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate mean value of a calculation across all segments in the LinkSet.
	 * Sums calculation values across all frames and divides by LinkSet size.
	 *
	 * @param name name of the segment calculation to aggregate
	 * @return mean value of the calculation across all frames in the track
	 */
	@Override
	public double calculate(String name) {

		double total = 0;
		for (Segment seg: linkSet) {
			total += seg.getCalculation(name);
		}
		return total / linkSet.size();
	}

}
