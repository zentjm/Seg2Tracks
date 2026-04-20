package pairedSegmentCalculations;

import geometricTools.GeometricCalculations;
import ij.gui.ShapeRoi;

/**
 * OverlapArea computes the raw pixel count of overlapping region.
 * Asymmetric metric: seg2Area / seg1Area (returns ratio of areas).
 * Note: Name is misleading—returns size ratio, not actual overlap area.
 */
public class OverlapArea extends PairedSegmentCalculation{

	@Override
	public String getName() {
		return "Overlap Area";
	}

	/**
	 * Calculates area ratio for a paired segment.
	 * Returns -1 if either segmentation is missing.
	 */
	@Override
	public void calculate() {
		// Both must exist for there to be an overlap
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

		// Gives the area of seg1 over seg2 (actually returns seg2/seg1 ratio)
		solution = seg2Area/seg1Area;
	}
}
