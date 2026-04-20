package pairedSegmentCalculations;

import java.util.HashMap;

import calculations.Data;
import calculations.DataType;
import pairedDataStructure.PairedList;
import pairedDataStructure.PairedSegment;

/**
 * PairedListCalculation is the abstract base for cell-level paired metrics.
 * Aggregates segment-level (frame-level) calculations to cell level (e.g., mean across frames).
 */
public abstract class PairedListCalculation extends Data {

	String name;
	PairedList pairedList; // Reference to cell pairing
	PairedSegmentCalculation[] segmentCalcs; // Available segment calculations

	// For STATISTICS data: cache results indexed by segment calculation
	HashMap <PairedSegmentCalculation, Double> solutionMap;

	/**
	 * Constructs a PairedListCalculation with empty cache.
	 */
	public PairedListCalculation() {
		name = getName();
		solutionMap = new HashMap <PairedSegmentCalculation, Double>(); // STATISTIC
	}

	/**
	 * Sets the paired cell list to calculate over.
	 * @param pairedList cell pairing to analyze
	 */
	public void setList(PairedList pairedList) {
		this.pairedList = pairedList;
	}

	/**
	 * Sets the segment-level calculations available for aggregation.
	 * @param segmentCalcs array of segment-level calculations
	 */
	public void setSegmentCalculations(PairedSegmentCalculation[] segmentCalcs) {
		this.segmentCalcs = segmentCalcs;
	}

	/**
	 * Retrieves or calculates cell-level metric.
	 * @param calc segment-level calculation to aggregate
	 * @return aggregated value across all frames
	 */
	public double get(PairedSegmentCalculation calc) {
		if (solutionMap.get(calc) == null) solutionMap.put(calc,calculate(calc));
		return solutionMap.get(calc);
	}

	@Override
	public DataType getType() {
		return DataType.LINKSET_CALCULATION;
	}

	/**
	 * Abstract method for calculating cell-level metrics.
	 * @param calc segment-level calculation to aggregate
	 * @return aggregated result
	 */
	public abstract double calculate(PairedSegmentCalculation calc);

}
