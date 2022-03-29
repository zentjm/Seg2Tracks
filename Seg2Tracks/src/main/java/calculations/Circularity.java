package calculations;

public class Circularity extends SegmentCalculation {
	//Also known as "Form Factor"

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate() {
		double area = segments[0].getCalculation("Area");
		double perimeter = segments[0].getCalculation("Perimeter");
		return ((4 * Math.PI) * area) / Math.pow(perimeter, 2);
	}

	@Override
	public String getName() {
		return "Circularity";
	}

}
