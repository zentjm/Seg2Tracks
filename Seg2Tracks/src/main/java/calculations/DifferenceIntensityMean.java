package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

/**
 * Computes mean intensity in the pericellular region surrounding a cell.
 * Calculated as: Difference Intensity / Exterior Area (normalized by region size).
 * Provides intensity measure independent of exterior region size variations.
 */
public class DifferenceIntensityMean extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Difference Intensity Mean is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate mean intensity by dividing total pericellular intensity by exterior area.
	 * Depends on "Area" and "Difference Intensity" calculations being available.
	 *
	 * @return mean pixel intensity in the pericellular region
	 */
	@Override
	public double calculate() {
		double area = segments[0].getCalculation("Area");
		double integratedIntensity = segments[0].getCalculation("Difference Intensity");
		return integratedIntensity/area;
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
