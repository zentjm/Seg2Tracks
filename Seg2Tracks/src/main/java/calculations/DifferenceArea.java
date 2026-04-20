package calculations;

import geometricTools.GeometricCalculations;

/**
 * Computes the exterior (pericellular) area difference between external and internal perimeters.
 * Represents the annular region surrounding a cell, useful for studying pericellular phenomena.
 * Area = ExternalPerimeter region area - InternalPerimeter region area.
 */
public class DifferenceArea extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Difference Area is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate exterior area as the difference between external and internal segmentation areas.
	 *
	 * @return exterior area (external area - internal area)
	 */
	@Override
	public double calculate() {
		return (double) GeometricCalculations.getAreaByRoi(segments[0].getExternalPerimeter()).length -
				GeometricCalculations.getAreaByRoi(segments[0].getInternalPerimeter()).length;
	}

	/**
	 * Returns the name of this calculation.
	 * Note: Returns "Area" (may conflict with Area.java; should consider renaming to "Exterior Area" or "Pericellular Area").
	 *
	 * @return "Area"
	 */
	@Override
	public String getName() {
		return "Area";
	}

}
