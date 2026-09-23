package sarn;

/**
 * One-Way Contraction: simplest SARN implementation using a cell marker as the inner contour
 * and a circle centered on that marker (with radius = distance to nearest neighbor) as the outer constraint.
 * Searches inward along Bresenham radii to find the transition from dark to bright pixels,
 * yielding the cell perimeter.
 * <p>
 * All of the mechanics (marker inner point, nearest-neighbour outer constraint, circle generation,
 * boundary matching, darkest-pixel threshold scan) live in {@link OneWayContractionBase}; this class
 * uses them unchanged.
 */
public class OneWayContraction extends OneWayContractionBase {

	/**
	 * Initializes the One-Way Contraction method with default parameters.
	 */
	public OneWayContraction() {
		this.name = "One-Way Contraction";
		this.description = "Contracts a circular boundary centered on the cell marker, using the "
				+ "nearest neighbouring cell (or the image edge when the cell is isolated) as the "
				+ "outer radius, and finds the intensity transition along straight radial lines.";
	}
}
