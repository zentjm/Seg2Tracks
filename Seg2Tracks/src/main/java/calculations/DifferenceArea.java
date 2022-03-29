package calculations;

import geometricTools.GeometricCalculations;

public class DifferenceArea extends SegmentCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {
		return (double) GeometricCalculations.getAreaByRoi(segments[0].getExternalPerimeter()).length -
				GeometricCalculations.getAreaByRoi(segments[0].getInternalPerimeter()).length;	
	}

	@Override
	public String getName() {
		return "Area";
	}

}
