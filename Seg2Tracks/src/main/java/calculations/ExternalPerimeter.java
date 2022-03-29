package calculations;

public class ExternalPerimeter extends SegmentCalculation {

	@Override
	public String getName() {
		return "External Perimeter";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}
	
	@Override //TODO: Move calculations here
	public double calculate() {
		return (double) segments[0].getExternalPerimeter().length;
	}
}
