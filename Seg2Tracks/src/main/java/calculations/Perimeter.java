package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

public class Perimeter extends SegmentCalculation {

	@Override
	public String getName() {
		return "Perimeter";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}
	
	@Override //TODO: Move calculations here
	public double calculate() {
		return (double) segments[0].getInternalPerimeter().length;
	}
}


