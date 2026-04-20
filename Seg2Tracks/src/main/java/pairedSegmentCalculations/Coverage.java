package pairedSegmentCalculations;

import geometricTools.GeometricCalculations;
import ij.gui.ShapeRoi;

/**
 * Coverage is the primary quality metric reported in the Seg2Tracks paper.
 * Measures the fraction of automatic segmentation that overlaps with manual control.
 * Formula: (overlap area) / (automatic segmentation area)
 * Value range: 0 (no overlap) to 1 (perfect match)
 */
public class Coverage extends PairedSegmentCalculation{

	@Override
	public String getName() {
		return "Coverage";
	}

	/**
	 * Calculates coverage metric for a paired segment.
	 * If either segmentation is missing, returns -1.
	 */
	@Override
	public void calculate() {
		// If neither exists there is no overlap
		if (pairs[0].getSeg1() == null || pairs[0].getSeg2() == null) {
			solution = -1;
			return;
		}

		// TODO: this should be its own calculation.
		// Get internal boundaries and convert to polygons
		ShapeRoi seg1Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi
				(pairs[0].getSeg1().getInternalPerimeter()).getPolygon());
		ShapeRoi seg2Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi
				(pairs[0].getSeg2().getInternalPerimeter()).getPolygon());

		// Compute intersection (overlap)
		ShapeRoi intersection = (ShapeRoi) seg1Internal.clone();
		intersection.and(seg2Internal);

		// Calculate areas (pixel counts)
		double areaOverlap = (double) intersection.getContainedPoints().length;
		double seg1Area = seg1Internal.getContainedPoints().length;

		// Coverage = overlap / automatic segmentation area
		solution = areaOverlap/seg1Area;
	}
}

