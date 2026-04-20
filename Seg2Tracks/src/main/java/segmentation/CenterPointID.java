package segmentation;

import java.awt.*;

import dataStructure.FrameSet;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedAutoThresholder.Method;
import ij.gui.Roi;
import ij.gui.Wand;


/**
 * Utility class for identifying and visualizing cell center points. Rather than performing actual
 * segmentation, this method simply creates a small square region around each cell's pre-computed
 * center point. Useful for visualization and debugging to verify that cell centers are correctly identified.
 */
public class CenterPointID extends Segmentation {

	/**
	 * Constructor. Initializes name, description, and sets externalDependence to false (no SARN dependency).
	 */
	public CenterPointID() {
		this.name = "CenterPointID";
		this.description = " "; //TODO
		this.externalDependence = false;
	}

	/**
	 * Returns whether this method depends on external SARN regions.
	 * @return false, as this is purely a visualization utility
	 */
	public boolean isExternallyDependent() {
		return false;
	}

	/**
	 * Returns the thresholding method enum.
	 * @return null, as this method does not perform thresholding
	 */
	Method getMethod() {
		return null;
	}

	/**
	 * Segments cells by marking their center points. For each cell, creates a small square
	 * (5-pixel radius) around the pre-computed center point and sets it as the internal perimeter.
	 * This is primarily a visualization tool for debugging center point detection.
	 * @param segments FrameSet containing all cells in the current frame
	 */
	@Override
	protected void segmentation(FrameSet segments) {

		for (int n = 0; n < segments.size(); n++) {

			// Fetch pre-computed center point
			//Rectangle(int x, int y, int width, int height)
			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;

			int size = 5; // Half-width of square marking the center point

			// Create 4 corner points of a square around the center
			Point[] rect = new Point[4];
			rect[0] = new Point(x - size, y - size); // Top-left
			rect[2] = new Point(x - size, y + size); // Bottom-left
			rect[1] = new Point(x + size, y - size); // Top-right
			rect[3] = new Point(x + size, y + size); // Bottom-right


			// Set the interpolated square perimeter as the cell's internal boundary
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(rect));

		}
	}

	/**
	 * Validates whether a threshold is acceptable. CenterPointID accepts all thresholds.
	 * @param threshold Intensity threshold value
	 * @param histogram Image intensity histogram
	 * @return true (all thresholds accepted)
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}



}
