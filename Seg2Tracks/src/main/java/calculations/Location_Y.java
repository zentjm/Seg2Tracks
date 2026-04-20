package calculations;

/**
 * Computes the Y-coordinate (vertical position) of a cell's centroid.
 * Returns the Y-component of the center point of the segmented cell.
 */
public class Location_Y extends SegmentCalculation {

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Location_Y"
	 */
	@Override
	public String getName() {
		return "Location_Y";
	}

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Location_Y is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the Y-coordinate of the cell centroid.
	 * //TODO: move calculations here
	 *
	 * @return Y-coordinate of center point
	 */
	@Override //TODO: move calculations here
	public double calculate() {
		return (double) segments[0].getCenterPoint().y;
	}
}
