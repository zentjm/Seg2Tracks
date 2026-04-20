package calculations;

/**
 * Computes circularity (form factor) of a segmented cell.
 * Formula: 4π * area / perimeter² (ranges from 0 for lines to 1 for circles).
 * Also known as "Form Factor" or "Shape Factor".
 */
public class Circularity extends SegmentCalculation {
	//Also known as "Form Factor"

	/**
	 * Indicates whether this metric should be used in LinkSet and FrameSet statistics.
	 *
	 * @return true (Circularity is a statistic)
	 */
	@Override
	public boolean isStatistic() {
		return true;
	}

	/**
	 * Calculate circularity as 4π * area / perimeter².
	 * Depends on Area and Perimeter segment calculations being available.
	 *
	 * @return circularity index (0-1 scale)
	 */
	@Override
	public double calculate() {
		double area = segments[0].getCalculation("Area");
		double perimeter = segments[0].getCalculation("Perimeter");
		return ((4 * Math.PI) * area) / Math.pow(perimeter, 2);
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return "Circularity"
	 */
	@Override
	public String getName() {
		return "Circularity";
	}

}
