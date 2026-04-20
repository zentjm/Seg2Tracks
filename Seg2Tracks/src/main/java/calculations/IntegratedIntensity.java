package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;
import ij.process.ImageProcessor;

/**
 * Computes total (integrated) intensity within a segmented cell.
 * Sums all pixel intensities inside the cell's internal perimeter.
 * Useful for measuring total fluorescence, protein content, or marker expression.
 */
public class IntegratedIntensity extends SegmentCalculation {

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Integrated Intensity is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate total intensity by summing all pixel values within the internal perimeter.
	 *
	 * @return sum of all pixel intensities within the cell boundary
	 */
	@Override
	public double calculate() {
		Point[] area = GeometricCalculations.getAreaByRoi
				(segments[0].getInternalPerimeter());

		int intensity = 0;
		ImageProcessor processor = stack.getProcessor(slice);
		for (Point pt: area) {
			//intensity += (int) stack.getVoxel(pt.x, pt.y, slice);
			intensity += (int) processor.get(pt.x, pt.y);
		}
		return intensity;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Integrated Intensity"
	 */
	@Override
	public String getName() {
		return "Integrated Intensity";
	}

}
