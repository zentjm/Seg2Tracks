package pairedSegmentCalculations;

import ij.gui.ShapeRoi;
import geometricTools.GeometricCalculations;

/**
 * OverlapIndex computes normalized overlap (overlap coefficient).
 * Formula: overlap / min(area1, area2)
 * Value range: 0 (no overlap) to 1 (one fully contained in other)
 * Symmetric (treats both segmentations equally).
 */
public class OverlapIndex extends PairedSegmentCalculation {

	@Override
	public String getName() {
		return "Overlap coefficient";
	}

	/**
	 * Calculates overlap coefficient for a paired segment.
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
		ShapeRoi seg1Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi(pairs[0].getSeg1().getInternalPerimeter()).getPolygon());
		ShapeRoi seg2Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi(pairs[0].getSeg2().getInternalPerimeter()).getPolygon());

		double seg1Area = seg1Internal.getContainedPoints().length;
		double seg2Area = seg2Internal.getContainedPoints().length;

		// Calculate intersection
		ShapeRoi intersection = seg1Internal.and(seg2Internal); // TODO: Other operators linked to different options

		double areaOverlap = (double) intersection.getContainedPoints().length;

		// Overlap Coefficient = overlap / min(area1, area2)
		solution = areaOverlap/(Math.min(seg1Area, seg2Area));

	}
}
