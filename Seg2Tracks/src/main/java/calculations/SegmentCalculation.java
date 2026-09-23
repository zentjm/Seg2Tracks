package calculations;

import dataStructure.Segment;
import ij.ImageStack;

/**
 * Abstract base class for calculations performed on individual segments (cells).
 * Computes morphological metrics (area, perimeter, shape) and intensity properties.
 * Caches results to avoid redundant computation across multiple statistic aggregations.
 *
 * <h3>Caching</h3>
 * Results are cached via a {@code boolean computed} flag rather than a sentinel value,
 * so {@code Double.NaN} results (e.g. from a missing perimeter) are cached correctly
 * and never cause spurious recomputation.
 *
 * <h3>Statistics eligibility</h3>
 * {@link #isStatistic()} controls whether this calculation's per-segment values are
 * aggregated into {@link calculations.LinkSetStatistic} and
 * {@link calculations.FrameSetStatistic} columns in the output workbook.
 * The default is {@code true}; override to return {@code false} to exclude a
 * calculation from those aggregate columns.
 */
public abstract class SegmentCalculation extends Data {

	/** Whether this calculation's values are included in LinkSet/FrameSet statistics. */
	boolean statistic;
	String name;
	private double solution;
	private boolean computed = false;

	/** The single segment this calculation operates on. Set via {@link #setSegment}. */
	protected Segment segment;
	protected ImageStack stack;
	protected int slice;

	/**
	 * Constructor — caches name and statistic flag for later use.
	 */
	public SegmentCalculation() {
		name = getName();
		statistic = isStatistic();
	}

	/**
	 * Set the target image stack and slice (frame number) for intensity calculations.
	 *
	 * @param stack the ImageStack containing pixel intensity data
	 * @param slice the frame/slice number (1-based) to extract intensity from
	 */
	public void setTargetStackSlice(ImageStack stack, int slice) {
		this.stack = stack;
		this.slice = slice;
	}

	/**
	 * Set the segment this calculation will operate on.
	 *
	 * @param segment the Segment to analyze
	 */
	public void setSegment(Segment segment) {
		this.segment = segment;
	}

	/**
	 * Returns the cached or freshly computed result.
	 * {@code Double.NaN} results are cached normally — a NaN answer will not
	 * trigger recomputation on subsequent calls.
	 *
	 * @return computed (or cached) metric value
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
	 * @return {@link DataType#SEGMENT_CALCULATION}
	 */
	public DataType getType() {
		return DataType.SEGMENT_CALCULATION;
	}

	/**
	 * Whether this calculation's per-segment values should be included in
	 * LinkSet-level and FrameSet-level aggregate statistics (mean, variance, etc.).
	 * Returns {@code true} by default; override to return {@code false} to exclude.
	 *
	 * @return true if this calculation participates in aggregate statistics
	 */
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Perform the calculation on {@link #segment}.
	 * Called lazily by {@link #get()} and the result is cached until a new
	 * segment is set.
	 *
	 * @return computed metric value
	 */
	public abstract double calculate();

}
