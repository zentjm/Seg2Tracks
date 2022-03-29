package pairedSegmentCalculations;

import geometricTools.GeometricCalculations;
import ij.gui.ShapeRoi;

public class Coverage extends PairedSegmentCalculation{

	@Override
	public String getName() {
		return "Coverage";
	}

	@Override
	public void calculate() {
		//If neither exists there is no overlap
		if (pairs[0].getSeg1() == null || pairs[0].getSeg2() == null) {
			solution = -1;
			return;
		}
		
		//TODO: this should be its own calculation. 	
		ShapeRoi seg1Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi
				(pairs[0].getSeg1().getInternalPerimeter()).getPolygon());
		ShapeRoi seg2Internal = new ShapeRoi (GeometricCalculations.getPolygonRoi
				(pairs[0].getSeg2().getInternalPerimeter()).getPolygon());
	
		ShapeRoi intersection = (ShapeRoi) seg1Internal.clone();
		intersection.and(seg2Internal); 
		
		double areaOverlap = (double) intersection.getContainedPoints().length;
		double seg1Area = seg1Internal.getContainedPoints().length;
		
		//Gives the area of overlap over the dataSet1
		solution = areaOverlap/seg1Area;
	}
}
