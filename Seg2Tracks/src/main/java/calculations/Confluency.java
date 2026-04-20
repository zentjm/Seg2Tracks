package calculations;

import dataStructure.Segment;

/**
 * Computes cell confluency (total cell area coverage) for a FrameSet.
 * Sums the areas of all cells in a frame to determine overall cell confluence.
 * Useful metric for monitoring culture density and monolayer formation.
 */
public class Confluency extends FrameSetCalculation {

	/**
	 * Indicates whether this is a statistic.
	 *
	 * @return true (confluency is computed per frame as a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Cell Confluency"
	 */
	@Override
	public String getName() {
		return "Cell Confluency";
	}

	/**
	 * Calculate total cell area (confluency) by summing all segment areas in the frame.
	 * Depends on Area calculation being available for each segment.
	 * //TODO: some sort of automatic finding of the "Area" name.
	 *
	 * @return sum of all cell areas in the frame
	 */
	@Override
	public double calculate() {

		double total = 0;
		for (Segment seg : frameSet) {
			total += seg.getCalculation("Area"); //TODO: some sort of automatic finding of the "Area" name.
		}

		return total;
	}
}
