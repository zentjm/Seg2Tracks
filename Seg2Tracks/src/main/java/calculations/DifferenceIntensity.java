package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;

/**
 * Computes total intensity in the pericellular (exterior) region surrounding a cell.
 * Sums pixel intensities in the annular region between external and internal perimeters.
 * Useful for measuring extracellular activity (e.g., matrix metalloproteinase degradation).
 */
public class DifferenceIntensity extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Difference Intensity is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate total intensity in the exterior (pericellular) region.
	 * Defines exterior region as the ROI difference between external and internal perimeters.
	 *
	 * @return sum of all pixel intensities in the pericellular annulus
	 */
	@Override
	public double calculate() {
		ShapeRoi external = new ShapeRoi(GeometricCalculations.getPolygonRoi(segment.getExternalPerimeter()).getPolygon());
		ShapeRoi internal = new ShapeRoi(GeometricCalculations.getPolygonRoi(segment.getInternalPerimeter()).getPolygon());
		Roi roi = external.not(internal);

		Point[] points = roi.getContainedPoints();

		int intensity = 0;
		ImageProcessor processor = stack.getProcessor(slice);
		for (Point pt : points)	 {
			intensity += (int) processor.get(pt.x, pt.y);
		}
		return intensity;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Difference Intensity"
	 */
	@Override
	public String getName() {
		return "Difference Intensity";
	}
}
