package calculations;

import java.util.HashMap;

import dataStructure.FrameSet;

/**
 * Abstract base class for aggregate statistics computed at the FrameSet level.
 * A statistic operates on multiple segment-level calculations to produce summary metrics (e.g., mean, variance).
 * Caches results in a HashMap keyed by calculation name.
 */
public abstract class FrameSetStatistic extends Data {

	boolean statistic;
	String name;
	FrameSet frameSet;
	String [] calculationNames;

	// HashMap caching computed statistic values keyed by calculation name
	HashMap <String, Double> solutionMap;

	/**
	 * Constructor initializing the statistic with empty solution cache.
	 */
	public FrameSetStatistic() {
		name = getName();
		statistic = isStatistic();
		solutionMap = new HashMap <String, Double>(); //STATISTIC
	}

	/**
	 * Set the FrameSet that this statistic will operate on.
	 *
	 * @param frameSet the FrameSet containing all segments in a single frame
	 */
	public void setFrameSet(FrameSet frameSet) {
		this.frameSet = frameSet;
	}

	/**
	 * Set the list of available segment calculation names.
	 * Used to populate the cache for multiple calculations.
	 *
	 * @param calculationNames array of segment calculation names to compute statistics for
	 */
	public void setFrameSetCalculations(String[] calculationNames) {
		this.calculationNames = calculationNames;
	}

	/**
	 * Get the cached or computed statistic result for a calculation.
	 * Computes result if not already cached.
	 *
	 * @param calcName the name of the segment calculation to compute statistic for
	 * @return computed statistic value, cached if previously computed
	 */
	public double get(String calcName) {
		if (solutionMap.get(calcName) == null) solutionMap.put(calcName, calculate(calcName));
		return solutionMap.get(calcName);
	}

	/**
	 * Returns the DataType for this statistic.
	 *
	 * @return DataType.FRAMESET_STATISTIC
	 */
	public DataType getType() {
		return DataType.FRAMESET_STATISTIC;
	}

	/**
	 * Returns the name of this statistic.
	 *
	 * @return statistic name as String
	 */
	public abstract String getName();

	/**
	 * Indicates whether this is a statistic.
	 *
	 * @return true (all FrameSetStatistic subclasses are statistics)
	 */
	public abstract boolean isStatistic();

	/**
	 * Compute the statistic for a specific segment-level calculation.
	 * Must be implemented by subclasses.
	 *
	 * @param calcName name of the segment calculation to compute statistic for
	 * @return computed statistic value
	 */
	public abstract double calculate(String calcName);

}
