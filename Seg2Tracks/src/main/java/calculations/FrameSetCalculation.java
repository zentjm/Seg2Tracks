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
	double solution;
	// Sentinel value to detect uncomputed calculations
	final double flag = Double.MIN_VALUE;
	FrameSet frameSet;

	/**
	 * Constructor initializing the calculation with cached solution sentinel.
	 */
	public FrameSetCalculation() {
		solution = flag;
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
		if (solution != flag) return solution;
		solution = calculate();
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
	 * Indicates whether this is a statistic or a basic calculation.
	 *
	 * @return true if statistic, false if basic calculation
	 */
	public abstract boolean isStatistic();

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
