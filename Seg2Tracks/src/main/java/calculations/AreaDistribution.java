package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

/**
 * Computes the standard deviation of cell area across a cell's tracked lifetime.
 * Measures how much a cell's size varies frame-to-frame, which can indicate morphological
 * activity such as spreading, contraction, or blebbing. A low value indicates stable
 * cell size; a high value indicates active morphological change.
 *
 * Area per frame is computed as the pixel count within the cell's internal perimeter
 * (consistent with the Area SegmentCalculation). Standard deviation is population-based
 * (divided by N) since the full observed track is available.
 */
public class AreaDistribution extends LinkSetCalculation {

	/**
	 * Indicates whether this is a statistic.
	 *
	 * @return false (this is a per-track calculation, not an aggregate statistic)
	 */
	@Override
	public boolean isStatistic() {
		return false;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Area Standard Deviation"
	 */
	@Override
	public String getName() {
		return "Area Standard Deviation";
	}

	/**
	 * Computes the population standard deviation of cell area across all frames in the track.
	 * For each frame, area is computed as the pixel count within the internal perimeter.
	 * Frames with a null internal perimeter (unsegmented) are treated as area = 0.
	 *
	 * Algorithm:
	 * 1. Collect area for each segment in the LinkSet
	 * 2. Compute mean area across all frames
	 * 3. Compute population standard deviation: sqrt( sum((area_i - mean)^2) / N )
	 *
	 * @return population standard deviation of area in pixels, across all tracked frames
	 */
	@Override
	public double calculate() {
		int n = linkSet.size();
		if (n == 0) return 0;

		// Pass 1: collect per-frame areas and compute mean
		double[] areas = new double[n];
		double mean = 0;
		for (int i = 0; i < n; i++) {
			Segment seg = linkSet.get(i);
			if (seg.getInternalPerimeter() != null) {
				areas[i] = GeometricCalculations.getAreaByRoi(seg.getInternalPerimeter()).length;
			} else {
				areas[i] = 0; // Unsegmented frame treated as zero area
			}
			mean += areas[i];
		}
		mean /= n;

		// Pass 2: compute population variance, then return std dev
		double variance = 0;
		for (int i = 0; i < n; i++) {
			double diff = areas[i] - mean;
			variance += diff * diff;
		}
		return Math.sqrt(variance / n);
	}
}
