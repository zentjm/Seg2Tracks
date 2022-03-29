package pairedSegmentCalculations;

import ij.gui.ShapeRoi;
import geometricTools.GeometricCalculations;

public class OverlapIndex extends PairedSegmentCalculation {

	@Override
	public String getName() {
		return "Overlap coefficient";
	}

	@Override
	public void calculate() {
		
		
		//If neither exists there is no overlap
		if (pairs[0].getSeg1() == null || pairs[0].getSeg2() == null) {
			solution = -1;
			return;
		}
		
		//TODO: this should be its own calculation. 	
		ShapeRoi seg1Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi(pairs[0].getSeg1().getInternalPerimeter()).getPolygon());
		ShapeRoi seg2Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi(pairs[0].getSeg2().getInternalPerimeter()).getPolygon());
		
		double seg1Area = seg1Internal.getContainedPoints().length;
		double seg2Area = seg2Internal.getContainedPoints().length;
		
		ShapeRoi intersection = seg1Internal.and(seg2Internal); //TODO: Other operators linked to different options
		
		double areaOverlap = (double) intersection.getContainedPoints().length;

		//Overlap Coefficient
		solution = areaOverlap/(Math.min(seg1Area, seg2Area));
		
	}
}
