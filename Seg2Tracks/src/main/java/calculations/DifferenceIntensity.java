package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

public class DifferenceIntensity extends SegmentCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {	
		ShapeRoi external = new ShapeRoi (GeometricCalculations.getPolygonRoi(segments[0].getExternalPerimeter()).getPolygon());
		ShapeRoi internal = new ShapeRoi (GeometricCalculations.getPolygonRoi(segments[0].getInternalPerimeter()).getPolygon());
		Roi roi = external.not(internal);
		
		Point[] points = roi.getContainedPoints();
	
		int intensity = 0;
		for (Point pt : points)	 {
			intensity += processor.get(pt.x, pt.y);
		}
		return intensity;
	}

	@Override
	public String getName() {
		return "Integrated Intensity";
	}
}
