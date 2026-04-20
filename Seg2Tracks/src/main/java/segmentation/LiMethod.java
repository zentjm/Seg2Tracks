package segmentation;

import dataStructure.FrameSet;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * Global Binary Thresholding (GBT) using Li's Minimum Cross-Entropy algorithm.
 * Computes a single global threshold by minimizing the cross-entropy (information
 * distance) between the thresholded binary image and the original grayscale image.
 * This iterative method performs well on sparse fluorescent images where foreground
 * cells occupy a small fraction of the image, making it a strong alternative to
 * TriangleMethod. Unlike Otsu, Li does not assume equal foreground/background pixel
 * counts, and unlike Triangle, it is invariant to histogram shape asymmetry.
 *
 * Reference: Li C.H. and Lee C.K. (1993) "Minimum Cross Entropy Thresholding",
 * Pattern Recognition 26(4): 617-625.
 */
public class LiMethod extends Segmentation {

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to false (global method).
	 */
	public LiMethod() {
		this.name = "Li Method";
		this.description = "Global thresholding via Li's Minimum Cross-Entropy algorithm.";
		this.externalDependence = false;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return false, as Li Method is globally applied
	 */
	@Override
	public boolean isExternallyDependent() {
		return false;
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
	 * Validates whether a threshold is acceptable. LiMethod accepts all thresholds.
	 * @param threshold Intensity threshold value
	 * @param histogram Image intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

	/**
	 * Segments cells using global Li thresholding. Delegates to the shared pipeline
	 * in Segmentation.globalSegmentation(), which uses getMethod() to select the Li
	 * algorithm polymorphically.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	@Override
	void segmentation(FrameSet segments) {
		globalSegmentation(segments);
	}
}
