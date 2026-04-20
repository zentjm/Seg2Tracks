package segmentation;

import dataStructure.FrameSet;
import geometricTools.ModifiedAutoThresholder;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * SARN Binary Thresholding (SBT) using Otsu's method. Applies Otsu thresholding locally
 * to each cell's external SARN region rather than globally. Includes Kittler-Illingworth
 * threshold validation to reject statistically invalid thresholds for extreme
 * background-to-foreground ratios common in fluorescent microscopy.
 */
public class RestrictedOtsuMethod extends Segmentation {

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to true (SARN-restricted method).
	 */
	public RestrictedOtsuMethod() {
		this.name = "Restricted Otsu's Method";
		this.description = " "; //TODO
		this.externalDependence = true;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return true, as threshold would be computed within each cell's external region
	 */
	public boolean isExternallyDependent() {
		return true;
	}

	/**
	 * Returns the thresholding method enum for ModifiedAutoThresholder.
	 * @return Method.Otsu
	 */
	Method getMethod() {
		return Method.Otsu;
	}

	/**
	 * Validates whether a threshold computed via Otsu's method is statistically acceptable.
	 * Rejects thresholds that place the threshold bin at the peak of the histogram, based on
	 * criteria from Kittler & Illingworth (1985) "On Threshold Selection Using Clustering Criteria".
	 * This prevents Otsu from selecting thresholds when background-to-object ratios are extreme.
	 *
	 * Algorithm:
	 * 1. Compute mean intensity below and above threshold
	 * 2. Reject if threshold is at the histogram peak AND above both means
	 * @param threshold Intensity threshold value to validate
	 * @param histogram Regional intensity histogram
	 * @return false if threshold violates criteria, true otherwise
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		int mean1 = 0;
		int mean2 = 0;
		int countMean1 = 0;
		int countMean2 = 0;

		// Compute weighted mean for pixels below and above threshold
		for (int i = 0; i < histogram.length; i++) {
			if (i < threshold) {
				mean1 += i * histogram[i]; // Accumulate weighted sum below threshold
				countMean1 += histogram[i];
			}
			if (i > threshold) {
				mean2 += i * histogram[i]; // Accumulate weighted sum above threshold
				countMean2 += histogram[i];
			}
		}

		// Check for empty partitions (no pixels below or above threshold)
		if (countMean1 == 0 || countMean2 == 0) return false;
		mean1 = mean1 / countMean1; // Average intensity below threshold
		mean2 = mean2 / countMean2; // Average intensity above threshold

		// Reject if threshold histogram bin is at peak AND exceeds both partition means
		if (histogram[threshold] > mean1 && histogram[threshold] > mean2) {
			return false;
		}
		return true;
	}

	/**
	 * Segments cells using restricted Otsu thresholding within SARN regions. Delegates
	 * to the shared pipeline in Segmentation.restrictedSegmentation(), which uses getMethod()
	 * to select Otsu polymorphically. The Kittler-Illingworth threshold validation in
	 * acceptableThreshold() is automatically invoked by the parent pipeline before each
	 * wand trace, so Otsu-specific rejection logic is preserved without any duplication.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	@Override
	void segmentation(FrameSet segments) {
		restrictedSegmentation(segments);
	}
}
