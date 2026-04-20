package segmentation;

import dataStructure.FrameSet;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * SARN Binary Thresholding (SBT) using the Triangle algorithm. Applies Triangle thresholding locally
 * to each cell's SARN (Segmentation And Regional Neighborhoods) external region rather than globally
 * to the entire image. This is the key method validated and published in the Seg2Tracks paper, providing
 * cell-specific threshold computation that adapts to local intensity distributions.
 */
public class RestrictedTriangleMethod extends Segmentation {

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to true (SARN-restricted method).
	 */
	public RestrictedTriangleMethod() {
		this.name = "Restricted Triangle Method";
		this.description = " "; //TODO
		this.externalDependence = true;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return true, as threshold is computed within each cell's external region
	 */
	public boolean isExternallyDependent() {
		return true;
	}

	/**
	 * Returns the thresholding method enum for ModifiedAutoThresholder.
	 * @return Method.Triangle
	 */
	Method getMethod() {
		return Method.Triangle;
	}

	/**
	 * Segments cells using restricted Triangle thresholding within SARN regions. Delegates
	 * to the shared pipeline in Segmentation.restrictedSegmentation(), which uses getMethod()
	 * to select the Triangle algorithm polymorphically and acceptableThreshold() to validate
	 * each computed threshold. This is the key SBT method validated in the Seg2Tracks paper.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	@Override
	protected void segmentation(FrameSet segments) {
		restrictedSegmentation(segments);
	}



	/**
	 * Validates whether a threshold is acceptable. RestrictedTriangleMethod accepts all thresholds.
	 * @param threshold Intensity threshold value
	 * @param histogram Regional intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

}
