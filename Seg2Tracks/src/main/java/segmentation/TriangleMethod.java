package segmentation;

import dataStructure.FrameSet;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * Global Binary Thresholding (GBT) using the Triangle algorithm. Applies a single global threshold
 * computed via the Triangle method to the entire image, then extracts cell boundaries via contour tracing.
 * This is the primary algorithm validated in the published Seg2Tracks paper.
 */
public class TriangleMethod extends Segmentation{

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to false (global method).
	 */
	public TriangleMethod() {
		this.name = "Triangle Method";
		this.description = " "; //TODO
		this.externalDependence = false;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return false, as Triangle Method is globally applied
	 */
	public boolean isExternallyDependent() {
		return false;
	}

	/**
	 * Returns the thresholding method enum for ModifiedAutoThresholder.
	 * @return Method.Triangle
	 */
	Method getMethod() {
		return Method.Triangle;
	}

	/**
	 * Validates whether a threshold is acceptable. Triangle Method accepts all thresholds.
	 * @param threshold Intensity threshold value
	 * @param histogram Image intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}


	/**
	 * Segments cells using global Triangle thresholding. Delegates to the shared pipeline
	 * in Segmentation.globalSegmentation(), which uses getMethod() to select the Triangle
	 * algorithm polymorphically.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	@Override
	public void segmentation(FrameSet segments) {
		globalSegmentation(segments);
	}



}
