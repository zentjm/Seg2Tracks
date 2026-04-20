package calculations;

import java.util.HashMap;
import dataStructure.LinkSet;
import dataStructure.Segment;

/**
 * Abstract base class for calculations performed at the LinkSet level.
 * A LinkSet represents a single cell tracked across all frames.
 * Computes metrics that characterize cell behavior or properties across its lifetime.
 */
public abstract class LinkSetCalculation extends Data {


	boolean statistic; ///TODO: if this can be abstracted for dataset statistics

	String name;
	double solution;
	// Sentinel value to detect uncomputed calculations
	final double flag = Double.MIN_VALUE;
	LinkSet linkSet;

	/**
	 * Constructor initializing the calculation with cached solution sentinel.
	 */
	//For Statistic
	public LinkSetCalculation() {
		solution = flag;
		name = getName();
		statistic = isStatistic();
	}

	/**
	 * Set the LinkSet that this calculation will operate on.
	 *
	 * @param linkSet the LinkSet representing one cell's track across frames
	 */
	//access link/segments from here
	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
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
	 * @return DataType.LINKSET_CALCULATION
	 */
	public DataType getType() {
		return DataType.LINKSET_CALCULATION;
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
	 * Perform the calculation on the LinkSet.
	 * Must be implemented by subclasses.
	 *
	 * @return computed value
	 */
	public abstract double calculate();

}


	/*
 	 * FOR ACCESS TO Seg2tracks MEAN:
 	 * --> For each SegmentModel
 	 * 	--> go linkset, access segment model, access calculations
 	 * 		--> for each calculation set to "Statistic"
 	 * 			--> get calculation. If no calculation, force calculation then return
 	 * 			--> add calculation results to hashmap of calculation
 	 *
 	 * FOR SINGLE STAT generation for a LINKSET
 	 * --> For each segmentModel
 	 * --> Go linkset, access segment model OR segmentModel calculation
 	 * 		--> Do calculation.
 	 * 		--> Get data.
 	 * 		--> Store in solution.
 	 *
 	 * get calculations of all segments
 	 *
 	 *
 	 *
 	 *
 	 */
