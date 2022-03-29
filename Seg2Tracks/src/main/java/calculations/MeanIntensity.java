package calculations;

import java.awt.Point;

import geometricTools.GeometricCalculations;

public class MeanIntensity extends SegmentCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {
		double area = segments[0].getCalculation("Area");
		double integratedIntensity = segments[0].getCalculation("Integrated Intensity");
		return integratedIntensity/area;
	}

	@Override
	public String getName() {
		return "Mean Intensity";
	}

}
