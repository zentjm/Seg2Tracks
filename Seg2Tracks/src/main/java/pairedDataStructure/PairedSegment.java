package pairedDataStructure;

import java.awt.Color;
import java.util.HashMap;

import calculations.SegmentCalculation;
import dataStructure.Segment;
import dataStructure.SegmentModel;
import pairedSegmentCalculations.PairedSegmentCalculation;

/**
 * PairedSegment pairs an automatic and manual Segment from the same frame for direct comparison.
 * Enables calculation of overlap metrics (Coverage, Dice Index, Jaccard Index, etc.).
 */
public class PairedSegment extends SegmentModel {

	private static final long serialVersionUID = 1L;

	Segment seg1; // First segmentation (typically automatic)
	Segment seg2; // Second segmentation (typically manual control)
	PairedList list; // Parent PairedList
	HashMap <String, PairedSegmentCalculation> calculationMap; // Cached comparison metrics


	/**
	 * Constructs a PairedSegment for a specific frame.
	 * @param frame frame index (0-based)
	 * @param list parent PairedList
	 */
	public PairedSegment(int frame, PairedList list) {
		this.frame = frame;
		this.list = list;
	}

	/**
	 * Sets the first segment in this pair.
	 * @param seg1 first segment (typically automatic)
	 */
	public void setSeg1 (Segment seg1) {
		this.seg1 = seg1;
	}

	/**
	 * Sets the second segment in this pair.
	 * @param seg2 second segment (typically manual)
	 */
	public void setSeg2 (Segment seg2) {
		this.seg2 = seg2;
	}

	/**
	 * Gets the first segment.
	 * @return seg1
	 */
	public Segment getSeg1() {
		return seg1;
	}

	/**
	 * Gets the second segment.
	 * @return seg2
	 */
	public Segment getSeg2() {
		return seg2;
	}

	/**
	 * Registers a comparison metric.
	 * @param calculation PairedSegmentCalculation to cache
	 */
	public void setCalculation(PairedSegmentCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, PairedSegmentCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	/**
	 * Retrieves a cached comparison metric.
	 * @param name metric name
	 * @return calculated value
	 */
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}

	/**
	 * Gets the entire calculation map.
	 * @return HashMap of cached metrics
	 */
	public HashMap <String, PairedSegmentCalculation> getCalculationMap() {
		return calculationMap;
	}

	/**
	 * Gets the parent PairedList.
	 * @return parent list
	 */
	public PairedList getPairedList() {
		return list;
	}




	/* OLDER VERSION (archived for reference)
	//TODO: allow more than one-to-one comparisons.
	Segment seg1;
	Segment seg2;
	PairedList list;
	int frame;

	HashMap <String, PairedSegmentCalculation> calculationMap;

	//color
	Color color = null;

	public PairedSegment(int frame, PairedList list) {
		this.frame = frame;
		this.list = list;
	}

	public void setSeg1 (Segment seg1) {
		this.seg1 = seg1;
	}

	public void setSeg2 (Segment seg2) {
		this.seg2 = seg2;
	}

	public Segment getSeg1() {
		return seg1;
	}

	public Segment getSeg2() {
		return seg2;
	}

	public int getFrame() {
		return frame;
	}

	//Sets the calculation methods
	public void setCalculation(PairedSegmentCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, PairedSegmentCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	//Gets the result of the calculation
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}

	//Gets the result map for the internal calculations
	public HashMap <String, PairedSegmentCalculation> getCalculationMap() {
		return calculationMap;
	}

	public PairedList getPairedList() {
		return list;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void getOverlayParameter () {

	}
	*/
}
