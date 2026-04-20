package calculations;

import dataStructure.Segment;
import ij.ImageStack;

/**
 * Abstract base class for calculations performed on individual segments (cells).
 * Computes morphological metrics (area, perimeter, shape) and intensity properties.
 * Caches results to avoid redundant computation across multiple statistic aggregations.
 */
public abstract class SegmentCalculation extends Data {

	boolean statistic; //TODO: determines if LinkSet values (mean, etc) will be performed on this one.
	String name;
	double solution;
	// Sentinel value to detect uncomputed calculations
	double flag = Double.MIN_VALUE;
	Segment[] segments;
	ImageStack stack;
	int slice;

	/**
	 * Constructor initializing the calculation with cached solution sentinel.
	 */
	public SegmentCalculation() {
		solution = flag;
		name = getName();
		statistic = isStatistic();
	}

	/**
	 * Set the target image stack and slice (frame number) for intensity calculations.
	 *
	 * @param stack the ImageStack containing pixel intensity data
	 * @param slice the frame/slice number to extract intensity from
	 */
	public void setTargetStackSlice(ImageStack stack, int slice) {
		this.stack = stack;
		this.slice = slice;
	}

	/**
	 * Set the segment(s) that this calculation will operate on.
	 * //TODO: this does not need to be an array
	 *
	 * @param segments variable number of Segment objects to analyze
	 */
	//TODO: this does not need to be an array
	public void setSegments(Segment... segments) {
		this.segments = segments;
	}

	/**
	 * Get the cached or computed calculation result.
	 * Computes the result if not already cached.
	 * //TODO, another way to recuri
	 *
	 * @return calculated value, cached if previously computed
	 */
	public double get() {
		if (solution != flag) return solution; //TODO, another way to recuri
		solution = calculate();
		return solution;
	}

	/**
	 * Returns the DataType for this calculation.
	 *
	 * @return DataType.SEGMENT_CALCULATION
	 */
	public DataType getType() {
		return DataType.SEGMENT_CALCULATION;
	}

	/**
	 * Indicates whether this is a statistic or a basic calculation.
	 *
	 * @return true if statistic, false if basic calculation
	 */
	public abstract boolean isStatistic();

	/**
	 * Perform the calculation on the segment.
	 * Must be implemented by subclasses.
	 *
	 * @return computed metric value
	 */
	public abstract double calculate();

}
