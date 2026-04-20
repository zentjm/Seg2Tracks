package segmentation;

import dataStructure.FrameSet;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * SARN Binary Thresholding (SBT) using Li's Minimum Cross-Entropy algorithm.
 * Applies Li thresholding locally to each cell's external SARN region rather
 * than globally to the entire image. This combines the cross-entropy optimality
 * of Li's method with the local histogram adaptability of SARN, making it well
 * suited to images where cell intensity varies substantially across the field of view.
 *
 * Reference: Li C.H. and Lee C.K. (1993) "Minimum Cross Entropy Thresholding",
 * Pattern Recognition 26(4): 617-625.
 */
public class RestrictedLiMethod extends Segmentation {

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to true (SARN-restricted method).
	 */
	public RestrictedLiMethod() {
		this.name = "Restricted Li Method";
		this.description = "SARN-restricted thresholding via Li's Minimum Cross-Entropy algorithm.";
		this.externalDependence = true;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return true, as threshold is computed within each cell's external region
	 */
	@Override
	public boolean isExternallyDependent() {
		return true;
	}

	/**
	 * Returns the thresholding method enum for ModifiedAutoThresholder.
	 * @return Method.Li
	 */
	@Override
	Method getMethod() {
		return Method.Li;
	}

	/**
	 * Validates whether a threshold is acceptable. RestrictedLiMethod accepts all thresholds.
	 * A Kittler-Illingworth style rejection filter could be added here in the future
	 * if empirical testing reveals edge cases where Li produces degenerate thresholds
	 * on very small SARN regions.
	 * @param threshold Intensity threshold value
	 * @param histogram Regional intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

	/**
	 * Segments cells using restricted Li thresholding within SARN regions. Delegates
	 * to the shared pipeline in Segmentation.restrictedSegmentation(), which uses getMethod()
	 * to select the Li algorithm polymorphically.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	@Override
	void segmentation(FrameSet segments) {
		restrictedSegmentation(segments);
	}
}
