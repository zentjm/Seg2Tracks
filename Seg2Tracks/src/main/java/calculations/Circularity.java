package calculations;

/**
 * Computes circularity (form factor) of a segmented cell.
 * Formula: 4π × area / perimeter² (ranges from 0 for lines to 1 for perfect circles).
 * Also known as "Form Factor" or "Shape Factor".
 *
 * <h3>Dependency injection</h3>
 * {@code Circularity} depends on {@link Area} and {@link Perimeter} — it does not
 * re-fetch them by string key.  The caller (typically an {@link analysisMethod.OperationMethod}
 * subclass) must pass the same {@link Area} and {@link Perimeter} instances that are
 * also returned from {@code segmentCalculations()}, so that {@link #setSegment} reaches
 * all three objects consistently via the normal calculation loop.
 */
public class Circularity extends SegmentCalculation {

	private final Area area;
	private final Perimeter perimeter;

	/**
	 * @param area      the {@link Area} instance shared with the analysis method
	 * @param perimeter the {@link Perimeter} instance shared with the analysis method
	 */
	public Circularity(Area area, Perimeter perimeter) {
		this.area      = area;
		this.perimeter = perimeter;
	}

	/**
	 * Calculate circularity as 4π × area / perimeter².
	 * Returns {@code Double.NaN} if perimeter is zero (degenerate segment).
	 *
	 * @return circularity index (0–1 scale, or NaN for a zero-perimeter segment)
	 */
	@Override
	public double calculate() {
		double p = perimeter.get();
		if (p == 0) return Double.NaN;
		return (4 * Math.PI * area.get()) / (p * p);
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Circularity"
	 */
	@Override
	public String getName() {
		return "Circularity";
	}
}
