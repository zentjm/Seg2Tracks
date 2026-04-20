package pairedSegmentCalculations;

import ij.gui.ShapeRoi;
import geometricTools.GeometricCalculations;

/**
 * JaccardIndex computes Jaccard similarity index (Intersection over Union).
 * Formula: overlap / union
 * Value range: 0 (no similarity) to 1 (perfect match)
 * Symmetric (treats both segmentations equally).
 */
public class JaccardIndex extends PairedSegmentCalculation {

	@Override
	public String getName() {
		return "Jaccard Index";
	}

	/**
	 * Calculates Jaccard index for a paired segment.
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

		// Calculate intersection and union
		ShapeRoi intersection = (ShapeRoi) seg1Internal.clone();
		ShapeRoi union = (ShapeRoi)	seg1Internal.clone();

		intersection.and(seg2Internal); // TODO: Other operators linked to different options
		union.or(seg2Internal);

		double areaOverlap = (double) intersection.getContainedPoints().length;
		double areaUnion = (double) union.getContainedPoints().length;

		// Jaccard Index = overlap / union
		solution = areaOverlap/areaUnion;

	}

}
