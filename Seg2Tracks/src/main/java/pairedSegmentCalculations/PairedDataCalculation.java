package pairedSegmentCalculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import calculations.Data;
import calculations.DataType;
import pairedDataStructure.PairedDataSet;
import pairedDataStructure.PairedList;
import pairedDataStructure.PairedSegment;

/**
 * PairedDataCalculation is the abstract base for dataset-level paired metrics.
 * Aggregates segment-level and list-level calculations to dataset level (e.g., mean of means).
 */
public abstract class PairedDataCalculation extends Data {

	String name;
	PairedDataSet pairedDataSet; // Reference to dataset
	PairedSegmentCalculation[] segmentCalcs; // Available segment calculations
	PairedListCalculation listCalc; // List-level aggregation method

	/**
	 * Inner key class for caching results with both segment and list calculation components.
	 */
	class Key {

		PairedListCalculation listCalc;
		PairedSegmentCalculation segCalc;

		/**
		 * Constructs a cache key from calculation components.
		 * @param segCalc segment-level calculation
		 * @param listCalc list-level (cell-level) calculation
		 */
		public Key(PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
			this.listCalc = listCalc;
			this.segCalc = segCalc;
		}

		 @Override
	    public boolean equals(Object o) {
			if (this == o) return true;
	        if (!(o instanceof Key)) return false;
	        Key key = (Key) o;
	        return listCalc == key.listCalc && segCalc == key.segCalc;
	    }

		 // x = listCalc, y = segCalc
	    @Override
	    public int hashCode() {
	       int result = listCalc.hashCode();
	        result = 31 * result + segCalc.hashCode(); // TODO: stronger hashCode?
	        return result;
	    }
	}

	// For STATISTICS data: cache results indexed by key
	HashMap <Key, Double> solutionMap;

	/**
	 * Constructs a PairedDataCalculation with empty cache.
	 */
	public PairedDataCalculation() {
		name = getName();
		solutionMap = new HashMap <Key, Double>();
	}

	/**
	 * Sets the dataset to calculate over.
	 * @param pairedDataSet dataset containing all paired comparisons
	 */
	public void setList(PairedDataSet pairedDataSet) {
		this.pairedDataSet = pairedDataSet;
	}

	/**
	 * Sets the calculation components to use.
	 * @param segmentCalcs available segment-level calculations
	 * @param listCalc list-level aggregation method
	 */
	public void setCalculations(PairedSegmentCalculation[] segmentCalcs, PairedListCalculation listCalc) {
		this.listCalc = listCalc;
		this.segmentCalcs = segmentCalcs;
	}

	/**
	 * Retrieves or calculates dataset-level metric.
	 * @param segCalc segment-level calculation
	 * @param listCalc list-level calculation
	 * @return aggregated value
	 */
	public double get(PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
		if (solutionMap.get(new Key(segCalc,listCalc)) == null) {
			solutionMap.put(new Key(segCalc,listCalc), calculate(segCalc, listCalc));
		}
		return solutionMap.get(new Key(segCalc,listCalc));
	}

	@Override
	public DataType getType() {
		return DataType.DATASET_CALCULATON;
	}

	/**
	 * Abstract method for calculating dataset-level metrics.
	 * @param calc segment-level calculation
	 * @param listCalc list-level calculation
	 * @return aggregated result
	 */
	public abstract double calculate(PairedSegmentCalculation calc, PairedListCalculation listCalc);
	// As in the mean of means

}
