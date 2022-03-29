package calculations;

public class Location_X extends SegmentCalculation {

	@Override
	public String getName() {
		return "Location_X";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}
	
	@Override //TODO: move calculations here
	public double calculate() {
		return (double) segments[0].getCenterPoint().x;
	}
}



