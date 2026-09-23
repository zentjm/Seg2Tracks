package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;

/**
 * Computes mean pixel intensity within a segmented cell.
 * Calculated as Integrated Intensity / Area — normalised by cell size so results
 * are comparable across cells of different areas.
 *
 * <h3>Dependency injection</h3>
 * {@code MeanIntensity} depends on {@link Area} and {@link IntegratedIntensity}.
 * The caller must pass the same instances that appear in {@code segmentCalculations()}
 * so that {@link #setSegment} is applied consistently to all three.
 */
public class MeanIntensity extends SegmentCalculation {

	private final Area area;
	private final IntegratedIntensity integratedIntensity;

	/**
	 * @param area                the {@link Area} instance shared with the analysis method
	 * @param integratedIntensity the {@link IntegratedIntensity} instance shared with the analysis method
	 */
	public MeanIntensity(Area area, IntegratedIntensity integratedIntensity) {
		this.area                = area;
		this.integratedIntensity = integratedIntensity;
	}

	/**
	 * Calculate mean intensity as Integrated Intensity / Area.
	 * Returns {@code Double.NaN} if area is zero (degenerate segment).
	 *
	 * @return mean pixel intensity within the cell boundary
	 */
	@Override
	public double calculate() {
		double a = area.get();
		if (a == 0) return Double.NaN;
		return integratedIntensity.get() / a;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Mean Intensity"
	 */
	@Override
	public String getName() {
		return "Mean Intensity";
	}
}
