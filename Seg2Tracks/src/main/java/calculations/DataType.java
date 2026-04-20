package calculations;

/**
 * Enumeration of all calculation and statistic data types.
 * Categorizes calculations by the hierarchical level they operate on.
 * Supports both metric calculations and aggregate statistics at each level.
 */
public enum DataType {

	/**
	 * Per-segment morphological or intensity calculation.
	 */
	SEGMENT_CALCULATION(),

	/**
	 * Per-linkset (track) calculation across all frames.
	 */
	LINKSET_CALCULATION(),

	/**
	 * Per-frameset (time point) calculation across all cells.
	 */
	FRAMESET_CALCULATION(),

	/**
	 * Per-dataset (condition/channel) calculation.
	 */
	DATASET_CALCULATON(),

	/**
	 * Per-linkset aggregate statistic (e.g., mean, variance).
	 */
	LINKSET_STATISTIC(),

	/**
	 * Per-frameset aggregate statistic.
	 */
	FRAMESET_STATISTIC(),

	/**
	 * Per-dataset aggregate statistic.
	 */
	DATASET_STATISTIC(),

	/**
	 * Aggregate calculation over child void LinkSets in the context of their parent cell.
	 * Operates on a (parent LinkSet, List&lt;child LinkSet&gt;) pair via
	 * {@link RecursiveLinkSetCalculation}.
	 */
	RECURSIVE_LINKSET_CALCULATION(),

	/**
	 * Constructor for DataType enum.
	 */
	DataType () {
	}
}
