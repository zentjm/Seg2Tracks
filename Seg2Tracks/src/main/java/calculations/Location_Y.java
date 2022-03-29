package calculations;

public class Location_Y extends SegmentCalculation {

	@Override
	public String getName() {
		return "Location_Y";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}
	
	@Override //TODO: move calculations here
	public double calculate() {
		return (double) segments[0].getCenterPoint().y;
	}
}



