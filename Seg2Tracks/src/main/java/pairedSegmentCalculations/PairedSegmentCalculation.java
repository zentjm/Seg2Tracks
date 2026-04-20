package pairedSegmentCalculations;

import calculations.Data;
import calculations.DataType;
import pairedDataStructure.PairedSegment;

/**
 * PairedSegmentCalculation is the abstract base for frame-level paired metrics.
 * Represents overlap/similarity measurements between a pair of Segments (automatic vs. manual).
 * Uses lazy caching: calculation is performed on first access and cached.
 */
public abstract class PairedSegmentCalculation extends Data {

	String name;
	double solution; // Cached result
	final double flag = Double.MIN_VALUE; // Sentinel value indicating no calculation yet
	PairedSegment[] pairs; // Pair(s) of segments to compare

	/**
	 * Constructs a PairedSegmentCalculation with undefined result.
	 */
	public PairedSegmentCalculation() {
		solution = flag; // Mark as uncalculated
		name = getName();
	}

	/**
	 * Sets the segment pair(s) to calculate over.
	 * @param pairs one or more PairedSegments (typically just one)
	 */
	public void setSegments(PairedSegment...pairs) {
		this.pairs = pairs;
	}

	/**
	 * Gets the cached result, computing if necessary.
	 * Implements lazy evaluation: calculates on first access.
	 * @return calculated metric value
	 */
	public double get() {
		if (solution != flag) return solution; // Return cached result
		calculate(); // Compute if not yet done
		return solution;
	}

	@Override
	public DataType getType() {
		return DataType.SEGMENT_CALCULATION;
	}

	/**
	 * Abstract calculation method.
	 * Subclasses should compute the metric and set this.solution.
	 */
	public abstract void calculate();

}
