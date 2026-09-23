package calculations;

import dataStructure.FrameSet;

/**
 * Abstract base class for calculations performed at the FrameSet level.
 * A FrameSet contains all segments in a single frame/time point.
 * Computes metrics that aggregate or characterize all cells within a frame.
 */
public abstract class FrameSetCalculation extends Data {

	String name;
	boolean statistic;
	private double solution;
	private boolean computed = false;
	FrameSet frameSet;

	/**
	 * Constructor — caches name and statistic flag.
	 */
	public FrameSetCalculation() {
		name = getName();
		statistic = isStatistic();
	}

	/**
	 * Set the FrameSet that this calculation will operate on.
	 *
	 * @param frameSet the FrameSet containing all segments in a single frame
	 */
	//access link/segments from here
	public void setFrameSet(FrameSet frameSet) {
		this.frameSet = frameSet;
	}

	/**
	 * Get the cached or computed calculation result.
	 * Computes the result if not already cached.
	 *
	 * @return calculated value, cached if previously computed
	 */
	public double get() {
		if (computed) return solution;
		solution = calculate();
		computed = true;
		return solution;
	}

	/**
	 * Returns the DataType for this calculation.
	 *
	 * @return DataType.FRAMESET_CALCULATION
	 */
	public DataType getType() {
		return DataType.FRAMESET_CALCULATION;
	}

	/**
	 * Whether this calculation's value should be included in DataSet-level aggregate
	 * statistics when that tier is implemented.  Returns {@code true} by default;
	 * override to return {@code false} to exclude.
	 *
	 * @return true if this calculation participates in aggregate statistics
	 */
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return calculation name as String
	 */
	public abstract String getName();

	/**
	 * Perform the calculation on the FrameSet.
	 * Must be implemented by subclasses.
	 *
	 * @return computed value
	 */
	public abstract double calculate();

}
