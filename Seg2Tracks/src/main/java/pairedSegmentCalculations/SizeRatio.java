package pairedSegmentCalculations;

import geometricTools.GeometricCalculations;
import ij.gui.ShapeRoi;

/**
 * SizeRatio computes the area ratio between two segmentations.
 * Formula: seg2Area / seg1Area
 * Asymmetric metric (order matters).
 * Note: Despite the name, this is a ratio of areas, not a measure of overlap.
 */
public class SizeRatio extends PairedSegmentCalculation{

	@Override
	public String getName() {
		return "Size Ratio";
	}

	/**
	 * Calculates area ratio for a paired segment.
	 * Returns -1 if either segmentation is missing.
	 */
	@Override
	public void calculate() {
		// If neither exists there is no overlap
		if (pairs[0].getSeg1() == null || pairs[0].getSeg2() == null) {
			solution = -1;
			return;
		}

		// TODO: this should be its own calculation.
		ShapeRoi seg1Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi
				(pairs[0].getSeg1().getInternalPerimeter()).getPolygon());
		ShapeRoi seg2Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi
				(pairs[0].getSeg2().getInternalPerimeter()).getPolygon());

		double seg1Area = seg1Internal.getContainedPoints().length;
		double seg2Area = seg2Internal.getContainedPoints().length;

		// Gives the area of seg1 over seg2 (returns seg2/seg1 ratio)
		solution = seg2Area/seg1Area;
	}
}
