package segmentation;

import java.awt.Point;
import java.awt.Polygon;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import dataStructure.FrameSet;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.ImageProcessor;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedAutoThresholder;
import geometricTools.ModifiedWand;
import geometricTools.ModifiedAutoThresholder.Method;

/**
 * Converts SARN (Segmentation And Regional Neighborhoods) region coordinates into a format
 * usable by the segmentation pipeline. This is a trivial passthrough method that simply copies
 * the external (SARN) region boundary to the internal perimeter without modification.
 * Used primarily as a placeholder or for direct SARN-to-segmentation conversion pipelines.
 */
public class SARNtoSegmentationConversion extends Segmentation{

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to true (depends on SARN regions).
	 */
	public SARNtoSegmentationConversion() {
		this.name = "Convert SARN to Segmentation";
		this.description = " "; //TODO
		this.externalDependence = true;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return true, as the external boundary is used directly
	 */
	public boolean isExternallyDependent() {
		return true;
	}

	/**
	 * Returns the thresholding method enum.
	 * @return null, as this method does not perform thresholding
	 */
	Method getMethod() {
		return null;
	}

	/**
	 * Validates whether a threshold is acceptable. SARNtoSegmentationConversion accepts all thresholds.
	 * @param threshold Intensity threshold value
	 * @param histogram Image intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

	/**
	 * Converts SARN regions to segmentation boundaries. For each cell, directly copies the
	 * external perimeter (SARN region) to the internal perimeter without modification.
	 * This is a passthrough method useful as a baseline or for SARN-to-segmentation pipelines.
	 * //TODO: Implement actual conversion logic if more sophisticated transformation is needed
	 * @param segments FrameSet containing all cells in the current frame
	 */
	@Override
	public void segmentation(FrameSet segments) {
		for (int n = 0; n < segments.size(); n++) {
			// Copy external boundary directly to internal perimeter (passthrough)
			segments.get(n).setInternalPerimeter(segments.get(n).getExternalPerimeter());

		}
	}




}
