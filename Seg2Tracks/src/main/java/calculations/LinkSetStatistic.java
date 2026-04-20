package calculations;

import java.util.HashMap;

import dataStructure.LinkSet;


/**
 * Abstract base class for aggregate statistics computed at the LinkSet level.
 * A LinkSet represents a single cell tracked across all frames.
 * A statistic operates on multiple segment-level calculations to produce summary metrics across the track.
 * Caches results in a HashMap keyed by calculation name.
 */
public abstract class LinkSetStatistic extends Data {

	String name;
	boolean statistic;
	LinkSet linkSet;
	String [] calculationNames;

	// HashMap caching computed statistic values keyed by calculation name
	//for STATISTICS data
	HashMap <String, Double> solutionMap;

	/**
	 * Constructor initializing the statistic with empty solution cache.
	 */
	public LinkSetStatistic() {
		name = getName();
		statistic = isStatistic();
		solutionMap = new HashMap <String, Double>(); //STATISTIC
	}

	/**
	 * Set the LinkSet that this statistic will operate on.
	 *
	 * @param linkSet the LinkSet representing one cell's track across all frames
	 */
	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
	}

	/**
	 * Set the list of available segment calculation names.
	 * Used to populate the cache for multiple calculations.
	 *
	 * @param calculationNames array of segment calculation names to compute statistics for
	 */
	public void setSegmentCalculations(String[] calculationNames) {
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
	 * @return DataType.LINKSET_STATISTIC
	 */
	public DataType getType() {
		return DataType.LINKSET_STATISTIC;
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
	 * @return true (all LinkSetStatistic subclasses are statistics)
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
