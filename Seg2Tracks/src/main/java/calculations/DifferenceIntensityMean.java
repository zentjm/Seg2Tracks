package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

/**
 * Computes mean intensity in the pericellular region surrounding a cell.
 * Calculated as Difference Intensity / Area — the pericellular total intensity
 * normalised by the cell's internal area so results are comparable across
 * cells of different sizes.
 *
 * <h3>Dependency injection</h3>
 * {@code DifferenceIntensityMean} depends on {@link Area} and
 * {@link DifferenceIntensity}.  The caller must pass the same instances that
 * appear in {@code segmentCalculations()} so that {@link #setSegment} is
 * applied consistently to all three.
 */
public class DifferenceIntensityMean extends SegmentCalculation {

	private final Area area;
	private final DifferenceIntensity differenceIntensity;

	/**
	 * @param area                the {@link Area} instance shared with the analysis method
	 * @param differenceIntensity the {@link DifferenceIntensity} instance shared with the analysis method
	 */
	public DifferenceIntensityMean(Area area, DifferenceIntensity differenceIntensity) {
		this.area                = area;
		this.differenceIntensity = differenceIntensity;
	}

	/**
	 * Calculate mean pericellular intensity as Difference Intensity / Area.
	 * Returns {@code Double.NaN} if area is zero (degenerate segment).
	 *
	 * @return mean pixel intensity in the pericellular annulus
	 */
	@Override
	public double calculate() {
		double a = area.get();
		if (a == 0) return Double.NaN;
		return differenceIntensity.get() / a;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Difference Intensity Mean"
	 */
	@Override
	public String getName() {
		return "Difference Intensity Mean";
	}
}
