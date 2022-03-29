package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;

public class IntegratedIntensity extends SegmentCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {
		Point[] area = GeometricCalculations.getAreaByRoi
				(segments[0].getInternalPerimeter());
		
		int intensity = 0;
		for (Point pt: area) {
			intensity += processor.get(pt.x, pt.y);
		}
		
		return intensity;
	}

	@Override
	public String getName() {
		return "Integrated Intensity";
	}

}



