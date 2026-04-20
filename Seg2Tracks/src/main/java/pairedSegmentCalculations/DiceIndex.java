package pairedSegmentCalculations;

import ij.gui.ShapeRoi;
import geometricTools.GeometricCalculations;

/**
 * DiceIndex computes Sørensen–Dice similarity coefficient between two segmentations.
 * Formula: (2 * overlap) / (area1 + area2)
 * Value range: 0 (no similarity) to 1 (perfect match)
 * Symmetric (treats both segmentations equally).
 */
public class DiceIndex extends PairedSegmentCalculation {

	/**
	 * Default constructor.
	 */
	public DiceIndex() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Dice's coefficient";
	}

	/**
	 * Calculates Dice coefficient for a paired segment.
	 * Returns 0 if either segmentation is missing.
	 */
	@Override
	public void calculate() {

		// If neither exists there is no overlap
		if (pairs[0].getSeg1() == null || pairs[0].getSeg2() == null) {
			solution = 0;
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

		// Dice Coefficient = 2 * overlap / (area1 + area2)
		solution = (2 * areaOverlap)/(seg1Area + seg2Area);

	}

}
