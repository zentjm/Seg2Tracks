package calculations;

import geometricTools.MatrixFunctions;

public class MajorAxisLength extends SegmentCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {
		MatrixFunctions functions = new MatrixFunctions();
		return functions.getMajorAxis(segments[0].getCenterPoint(), segments[0].getInternalPerimeter()).length;
	}

	@Override
	public String getName() {
		return "Major Axis Length";
	}
}
