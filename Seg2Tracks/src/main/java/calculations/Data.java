package calculations;

import dataStructure.Segment;

/**
 * Abstract base class for all data calculation types.
 * Represents a computation that can be performed on morphological or intensity data.
 * Subclasses include SegmentCalculation, LinkSetCalculation, FrameSetCalculation, etc.
 */
public abstract class Data {

	boolean active;

	/**
	 * Constructor initializing calculation as inactive.
	 */
	public Data() {
		active = false;
	}

	/**
	 * Returns the name of this calculation.
	 *
	 * @return name as String
	 */
	//Name of Calculation
	public abstract String getName();

	/**
	 * Returns the type of data structure this calculation applies to.
	 *
	 * @return DataType indicating segment/linkset/frameset/dataset level
	 */
	//Type of data calculation is applied to
	public abstract DataType getType();

	/**
	 * Sets whether this calculation will be executed.
	 *
	 * @param active true to enable calculation, false to skip
	 */
	//Sets whether calculation will be implemented
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Gets whether this calculation is active.
	 *
	 * @return true if calculation is enabled, false otherwise
	 */
	//Gets active state
	public boolean isActive() {
		return active;
	}

}
