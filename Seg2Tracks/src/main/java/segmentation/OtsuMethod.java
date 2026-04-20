package segmentation;

import dataStructure.FrameSet;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * Global Binary Thresholding (GBT) using Otsu's method. Applies a single global threshold
 * computed via Otsu's algorithm to the entire image. Note that Otsu assumes roughly equal
 * foreground/background pixel counts, which may not hold for sparse fluorescent images;
 * TriangleMethod is generally preferred for that use case.
 */
public class OtsuMethod extends Segmentation {


	/**
	 * Constructor. Initializes name, description, and sets externalDependence to false (global method).
	 */
	public OtsuMethod() {
		this.name = "Otsu's Method";
		this.description = " "; //TODO
		this.externalDependence = false;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return false, as Otsu Method is globally applied
	 */
	public boolean isExternallyDependent() {
		return false;
	}

	/**
	 * Returns the thresholding method enum for ModifiedAutoThresholder.
	 * @return Method.Otsu
	 */
	Method getMethod() {
		return Method.Otsu;
	}


	/**
	 * Validates whether a threshold is acceptable. OtsuMethod accepts all thresholds.
	 * @param threshold Intensity threshold value
	 * @param histogram Image intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

	/**
	 * Segments cells using global Otsu thresholding. Delegates to the shared pipeline
	 * in Segmentation.globalSegmentation(), which uses getMethod() to select Otsu
	 * polymorphically. For notes on Otsu's suitability vs Triangle for fluorescent
	 * microscopy, see ModifiedAutoThresholder.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	@Override
	void segmentation(FrameSet segments) {
		globalSegmentation(segments);
	}
}
