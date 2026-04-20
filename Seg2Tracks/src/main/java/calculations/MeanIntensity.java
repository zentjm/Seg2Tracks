package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;

/**
 * Computes mean pixel intensity within a segmented cell.
 * Normalized intensity metric calculated as Integrated Intensity / Area.
 * Independent of cell size variations for intensity comparisons.
 */
public class MeanIntensity extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Mean Intensity is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate mean intensity by dividing total cell intensity by area.
	 * Depends on Area and Integrated Intensity calculations being available.
	 *
	 * @return mean pixel intensity within the cell
	 */
	@Override
	public double calculate() {
		double area = segments[0].getCalculation("Area");
		double integratedIntensity = segments[0].getCalculation("Integrated Intensity");
		return integratedIntensity/area;
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
