package analysisMethod;

/**
 * Abstract stub for cross-parent (inter-cell) recursive analysis.
 *
 * Extends {@link RecursiveAnalysisMethod} to add a future tier of calculations
 * that require simultaneous access to the entire parent cell population and
 * their children — e.g. stratifying void circularity as a function of relative
 * parent cell size among peers, or ranking cells by void burden.
 *
 * <h3>Why a separate class</h3>
 * The cross-parent tier is structurally distinct from the per-parent-cell
 * aggregation in {@link RecursiveAnalysisMethod#parentLinkSetCalculations()}
 * because it cannot be computed independently for each parent cell: it requires
 * the full parent population to be visible at calculation time (e.g. for
 * ranking, percentile normalisation, or between-cell ratio metrics).
 *
 * <h3>Implementation roadmap</h3>
 * When the first concrete cross-parent analysis is needed, extend this class
 * and add:
 * <ol>
 *   <li>A new interface (e.g. {@code CrossParentCalculation}) with a
 *       {@code setContext(RecursiveDataSet)} method giving access to the full
 *       hierarchy.
 *   <li>A {@code crossParentCalculations()} abstract method returning that
 *       interface.
 *   <li>An additional workbook sheet ("Cross-Parent Data") with one row per
 *       parent cell and one column per cross-parent metric.
 *   <li>Override {@link #setCalculations()} and {@link #retrieveCalculations()}
 *       calling {@code super} first, then running the cross-parent tier.
 * </ol>
 *
 * Until then this class adds no behavior — it exists solely as a named
 * anchor in the class hierarchy.
 */
public abstract class CrossRecursiveAnalysisMethod extends RecursiveAnalysisMethod {

	// TODO: add cross-parent calculation tier (see class Javadoc).

}
