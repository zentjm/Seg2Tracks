package dataStructure;

import java.util.ArrayList;
import java.util.HashMap;

import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;

/**
 * FrameSet represents all segmented cells detected in a single image frame.
 * Extends ArrayList<Segment> to hold all Segments in one frame, with calculations and statistics.
 */
public class FrameSet extends ArrayList<Segment> {

	private static final long serialVersionUID = 1L;

	int frame; // Frame index (0-based)
	DataSet dataSet; // Reference to parent dataset

	HashMap<String, FrameSetCalculation> calculationMap; // Cached frame-level calculations
	HashMap<String, FrameSetStatistic> statisticMap; // Statistical measures for this frame

	/**
	 * Constructs a FrameSet for a specific frame within a dataset.
	 * @param frame frame index (0-based, 0 to depth-1)
	 * @param dataSet parent DataSet
	 */
	public FrameSet(int frame, DataSet dataSet) {
		this.frame = frame;
		this.dataSet = dataSet;
	}

	/**
	 * Gets the frame index of this FrameSet.
	 * @return frame index
	 */
	public int getFrame() {
		return frame;
	}

	/**
	 * Stores a frame-level calculation result.
	 * @param calculation FrameSetCalculation to cache
	 */
	public void setCalculation(FrameSetCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, FrameSetCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	/**
	 * Retrieves a cached frame-level calculation.
	 * @param name name of calculation
	 * @return calculated value
	 */
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}

	/**
	 * Stores a frame-level statistic.
	 * @param statistic FrameSetStatistic to cache
	 */
	public void setStatistic(FrameSetStatistic statistic) {
		if (statisticMap == null) statisticMap = new HashMap<String, FrameSetStatistic>();
		statisticMap.put(statistic.getName(), statistic);
	}

	/**
	 * Retrieves a cached frame-level statistic.
	 * @param statistic name of statistic
	 * @param calculation name of underlying calculation
	 * @return statistical result
	 */
	public double getStatistic(String statistic, String calculation) {
		return statisticMap.get(statistic).get(calculation);
	}

	/**
	 * Removes a Segment from this frame.
	 * @param segment SegmentModel to remove
	 */
	// STUB: method not yet implemented — needs to be completed
	public void removeSegment (SegmentModel segment) {
		// TODO: remove the segment from this frame's ArrayList
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
