package calculations;

import java.util.List;

import dataStructure.LinkSet;

/**
 * Abstract base class for calculations that aggregate child void LinkSets
 * in the context of their parent cell LinkSet.
 *
 * Unlike {@link LinkSetCalculation}, which operates on a single LinkSet,
 * this class receives a (parent, children) pair so implementations can
 * compute metrics requiring both the parent cell and its void tracks —
 * e.g. void count, total void area, void area fraction of parent cell area.
 *
 * <h3>Usage pattern</h3>
 * <pre>
 *   RecursiveLinkSetCalculation calc = new VoidCount();
 *   calc.setContext(parentLS, childLSList);
 *   double result = calc.get();
 * </pre>
 *
 * Results are cached: once {@link #get()} is called the answer is stored
 * and returned immediately on subsequent calls.  Calling {@link #setContext}
 * again resets the cache so the calculation is recomputed for the new context.
 *
 * <h3>Extension</h3>
 * Subclasses may also store and access the {@link ij.ImageStack} if intensity
 * data is required.  No built-in stack field is provided here because most
 * parent-level aggregations are purely geometric (operating on perimeter and
 * segment data already stored on the LinkSets), but subclasses are free to
 * declare additional fields.
 */
public abstract class RecursiveLinkSetCalculation extends Data {

	/** The parent cell LinkSet from the primary segmentation. */
	protected LinkSet parentLinkSet;

	/** All child void LinkSets found inside the parent cell. */
	protected List<LinkSet> childLinkSets;

	private static final double FLAG = Double.MIN_VALUE;
	private double solution = FLAG;

	public RecursiveLinkSetCalculation() {}

	/**
	 * Supplies the parent–children context for this calculation and resets
	 * the result cache so the next {@link #get()} call triggers a fresh
	 * {@link #calculate()}.
	 *
	 * Must be called once before {@link #get()}.  May be called again with a
	 * new context (e.g. when the same instance is reused across parent cells —
	 * though the recommended pattern is to create fresh instances per cell).
	 *
	 * @param parent   the parent cell LinkSet from the primary segmentation
	 * @param children all child void LinkSets found inside that parent (may be empty)
	 */
	public void setContext(LinkSet parent, List<LinkSet> children) {
		this.parentLinkSet = parent;
		this.childLinkSets = children;
		this.solution      = FLAG; // reset cache so calculate() is re-triggered
	}

	/**
	 * Returns the cached or freshly computed result.
	 * {@link #setContext} must be called before the first invocation.
	 *
	 * @return computed metric value
	 */
	public double get() {
		if (solution != FLAG) return solution;
		solution = calculate();
		return solution;
	}

	/**
	 * {@inheritDoc}
	 * @return {@link DataType#RECURSIVE_LINKSET_CALCULATION}
	 */
	@Override
	public DataType getType() {
		return DataType.RECURSIVE_LINKSET_CALCULATION;
	}

	/**
	 * Computes the metric from {@link #parentLinkSet} and {@link #childLinkSets}.
	 * Called lazily by {@link #get()} and the result is cached until
	 * {@link #setContext} is called again.
	 *
	 * @return computed value
	 */
	public abstract double calculate();

	/** {@inheritDoc} */
	@Override
	public abstract String getName();
}
