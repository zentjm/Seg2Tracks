package dataStructure;

import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.HashMap;

import calculations.SegmentCalculation;
import ij.gui.Roi;

/**
 * SegmentModel is the abstract base class for segment-like objects in the data hierarchy.
 * Provides common interface for Segment and PairedSegment, with properties for frame,
 * name, color, tracking linkage, and calculation results.
 */
public abstract class SegmentModel implements Serializable {

	private static final long serialVersionUID = 1L;

	protected int frame; // Frame index (0-based)
	String name; // Human-readable identifier
	Color color = null; // Display color (null for default)
	boolean isFirstFrame; // Flag: is this the first frame of the cell?
	boolean isLastFrame; // Flag: is this the last frame of the cell?
	LinkSet linkSet; // Reference to parent LinkSet (for tracking)
	boolean boundaryContact; // UNUSED: whether this segment touches a boundary

	// Holding Calculations: cached results indexed by name
	transient HashMap <String, SegmentCalculation> calculationMap;

	// Getter methods

	/**
	 * Gets the name/identifier of this segment.
	 * @return name string
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the frame index.
	 * @return frame number (0-based)
	 */
	public int getFrame() {
		return frame;
	}

	/**
	 * Gets the parent LinkSet (tracking information).
	 * @return LinkSet if this segment is tracked, null otherwise
	 */
	public LinkSet getLinkSet() {
		return linkSet;
	}

	/**
	 * Gets the display color.
	 * @return Color or null for default
	 */
	public Color getColor() {
		return color;
	}

	// Setter methods

	/**
	 * Sets the frame index.
	 * @param frame frame number (0-based)
	 */
	public void setFrame(int frame) {
		this.frame = frame;
	}

	/**
	 * Sets the name/identifier.
	 * @param name identifier string
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the parent LinkSet for tracking.
	 * @param linkSet LinkSet to associate with
	 */
	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
	}

	/**
	 * Sets the display color.
	 * @param color Color for visualization
	 */
	public void setColor(Color color) {
		this.color = color;
	}


	// CURRENTLY UNUSED: first/last frame flags (may be deprecated)

	/**
	 * Checks if this is the first frame of the cell.
	 * @return true if first frame
	 */
	public boolean getIsFirstFrame() {
		return isFirstFrame;
	}

	/**
	 * Checks if this is the last frame of the cell.
	 * @return true if last frame
	 */
	public boolean getIsLastFrame() {
		return isLastFrame;
	}

	/**
	 * Sets first frame flag.
	 * @param isFirstFrame true if first frame
	 */
	public void setIsFirstFrame(boolean isFirstFrame) {
		this.isFirstFrame = isFirstFrame;
	}

	/**
	 * Sets last frame flag.
	 * @param isLastFrame true if last frame
	 */
	public void setIsLastFrame(boolean isLastFrame) {
		this.isLastFrame = isLastFrame;
	}

	// Checker methods

	/**
	 * Checks if this segment has been assigned to a LinkSet (tracked).
	 * @return true if linkSet is not null
	 */
	public boolean hasLinkSet() {
		if (linkSet != null) return true;
		return false;
	}

	// Calculation caching methods

	/**
	 * Registers a calculation to be cached for this segment.
	 * @param calculation SegmentCalculation to cache
	 */
	public void setCalculation(SegmentCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, SegmentCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	/**
	 * Retrieves a cached calculation result.
	 * @param name calculation name
	 * @return calculated value
	 */
	public double getCalculation(String name) {
		// System.out.println("Getting calculation map: " + name + "  " + calculationMap.get(name).get());
		return calculationMap.get(name).get();
	}

	/**
	 * Gets the entire calculation map for this segment.
	 * @return HashMap of all cached calculations
	 */
	public HashMap <String, SegmentCalculation> getInternalCalculationMap() {
		return calculationMap;
	}
}
