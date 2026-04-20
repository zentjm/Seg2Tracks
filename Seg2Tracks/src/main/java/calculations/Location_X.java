package calculations;

/**
 * Computes the X-coordinate (horizontal position) of a cell's centroid.
 * Returns the X-component of the center point of the segmented cell.
 */
public class Location_X extends SegmentCalculation {

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Location_X"
	 */
	@Override
	public String getName() {
		return "Location_X";
	}

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Location_X is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate the X-coordinate of the cell centroid.
	 * //TODO: move calculations here
	 *
	 * @return X-coordinate of center point
	 */
	@Override //TODO: move calculations here
	public double calculate() {
		return (double) segments[0].getCenterPoint().x;
	}
}
