package calculations;

import geometricTools.MatrixFunctions;

public class MajorAxisAngle extends SegmentCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {
		MatrixFunctions functions = new MatrixFunctions();
		functions.getMajorAxis(segments[0].getCenterPoint(), segments[0].getInternalPerimeter());
		return functions.getTheta();
	}

	@Override
	public String getName() {
		return "Major Axis Angle";
	}
}
