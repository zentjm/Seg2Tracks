package calculations;

import geometricTools.GeometricCalculations;

public class ExternalArea extends SegmentCalculation {

	@Override
	public String getName() {
		return "External Area";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}
	
	@Override //TODO: Move calculations here
	public double calculate() {
		return (double) GeometricCalculations.getAreaByRoi
				(segments[0].getExternalPerimeter()).length;	
	}
}
