package calculations;

import geometricTools.GeometricCalculations;

public class Area extends SegmentCalculation {

	@Override
	public String getName() {
		return "Area";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}
	
	@Override //TODO: move calculations here
	public double calculate() {
		return (double) GeometricCalculations.getAreaByRoi
				(segments[0].getInternalPerimeter()).length;	
	}
}


