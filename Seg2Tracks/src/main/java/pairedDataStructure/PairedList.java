package pairedDataStructure;

import java.util.ArrayList;
import java.util.HashMap;

import calculations.SegmentCalculation;

import java.awt.Color;

import dataStructure.DataSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import pairedSegmentCalculations.PairedListCalculation;
import pairedSegmentCalculations.PairedSegmentCalculation;

/**
 * PairedList aligns two LinkSets (automatic and manual segmentation of one cell) frame-by-frame.
 * Creates PairedSegments for each frame, enabling direct comparison of automatic vs. manual segmentation.
 */
public class PairedList {

	DataSet dataSet1; // First dataset (typically automatic)
	DataSet dataSet2; // Second dataset (typically manual/control)

	LinkSet linkSet1; // Tracked cell from first dataset
	LinkSet linkSet2; // Tracked cell from second dataset

	Color seg1Color; // Display color for first segmentation
	Color seg2Color; // Display color for second segmentation

	enum Status {
		EMPTY, // No pairings exist
		LINKED, // Both linksets paired
		UNLINKED // Only one linkset exists
	}

	Status status; // Current pairing status
	PairedSegment[] pairedList; // Array of frame-by-frame pairings
	int name; // Identifier for this paired cell

	HashMap <String, PairedListCalculation> calculationMap; // For list-averaged segment calculations
	double calculation; // For list calculation

	/**
	 * Constructs a PairedList from two LinkSets representing the same cell.
	 * @param linkSet1 tracked cell from automatic segmentation (can be null)
	 * @param linkSet2 tracked cell from manual segmentation (can be null)
	 * @param name identifier for this paired cell
	 */
	public PairedList(LinkSet linkSet1, LinkSet linkSet2, int name) {

		this.name = name;

		if (linkSet1 != null) {
			this.linkSet1 = linkSet1;
			dataSet1 = linkSet1.getDataSet();
		}

		if (linkSet2 != null) {
			this.linkSet2 = linkSet2;
			dataSet2 = linkSet2.getDataSet();
		}

		// If both linksets exist, initialize frame-by-frame alignment
		if (linkSet1 != null && linkSet2 != null) initializeByLinkSet();


		// If only second linkset exists, create unlinked pairings
		if (linkSet2 == null) {
			int frames = dataSet1.getSize();
			pairedList = new PairedSegment[frames];
			for (Segment seg : linkSet1) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
				pairedList[seg.getFrame()].setSeg1(seg);

			}
		}

		// If only first linkset exists, create unlinked pairings
		if (linkSet1 == null) {
			int frames = dataSet2.getSize();
			pairedList = new PairedSegment[frames];
			for (Segment seg : linkSet2) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
				pairedList[seg.getFrame()].setSeg2(seg);
			}
		}
	}



	// Unused/deprecated: alternative initialization method

	/**
	 * Alternative initialization (currently unused).
	 */
	public void initializeByLinkSet2() {
		int frames = dataSet1.getSize();
		pairedList = new PairedSegment[frames]; // TODO: This assumes the number of frames in each dataset is equivalent

		// Adds segment to pairedList



		for (Segment seg : linkSet1) {
			if (pairedList[seg.getFrame()] == null) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
			}
			pairedList[seg.getFrame()].setSeg1(seg);
		}

		// add from second linkset
		for (Segment seg : linkSet2) {
			if (pairedList[seg.getFrame()] == null) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
			}
			pairedList[seg.getFrame()].setSeg2(seg);
		}




	}




	// Main initialization: aligns the linkSets frame-by-frame

	/**
	 * Aligns two LinkSets by creating PairedSegments for each frame.
	 * Matches segments in the same frame from both linksets.
	 */
	public void initializeByLinkSet() {
		int frames = dataSet1.getSize();
		pairedList = new PairedSegment[frames]; // TODO: This assumes the number of frames in each dataset is equivalent

		// Initialize list with PairedSegments (if needed).
		for (int i = 0; i < pairedList.length; i++) {
			// need to do anything?
		}

		// Add segments from first linkset
		for (Segment seg : linkSet1) {
			if (pairedList[seg.getFrame()] == null) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
			}
			pairedList[seg.getFrame()].setSeg1(seg);
		}

		// Add segments from second linkset
		for (Segment seg : linkSet2) {
			if (pairedList[seg.getFrame()] == null) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
			}
			pairedList[seg.getFrame()].setSeg2(seg);
		}
	}

	/**
	 * Gets all paired segments as an array.
	 * @return array of PairedSegments indexed by frame
	 */
	public PairedSegment[] getList() {
		return pairedList;
	}

	/**
	 * Gets a paired segment at a specific frame.
	 * @param i frame index
	 * @return PairedSegment at that frame
	 */
	public PairedSegment get(int i) {
		return pairedList[i];
	}

	/**
	 * Gets the first dataset.
	 * @return dataSet1
	 */
	public DataSet getDataSet1() {
		return dataSet1;
	}

	/**
	 * Gets the second dataset.
	 * @return dataSet2
	 */
	public DataSet getDataSet2() {
		return dataSet2;
	}

	/**
	 * Checks if first linkset exists.
	 * @return true if linkSet1 is not null
	 */
	public boolean hasSet1() {
		if (linkSet1 != null) return true;
		return false;
	}

	/**
	 * Checks if second linkset exists.
	 * @return true if linkSet2 is not null
	 */
	public boolean hasSet2() {
		if (linkSet2 != null) return true;
		return false;
	}

	/**
	 * Gets the identifier for this paired cell.
	 * @return name
	 */
	public int getName() {
		return name;
	}

	/**
	 * Gets the display color for first segmentation.
	 * @return seg1Color
	 */
	public Color getSeg1Color() {
		return seg1Color;
	}

	/**
	 * Gets the display color for second segmentation.
	 * @return seg2Color
	 */
	public Color getSeg2Color() {
		return seg2Color;
	}

	/**
	 * Sets the display color for first segmentation.
	 * Also updates the color in linkSet1.
	 * @param seg1Color color for first segmentation
	 */
	public void setSeg1Color(Color seg1Color) {
		this.seg1Color = seg1Color;
		linkSet1.setColor(seg1Color);

	}

	/**
	 * Sets the display color for second segmentation.
	 * Also updates the color in linkSet2.
	 * @param seg2Color color for second segmentation
	 */
	public void setSeg2Color(Color seg2Color) {
		this.seg2Color = seg2Color;
		linkSet2.setColor(seg2Color);
	}

	// Calculation caching methods

	/**
	 * Registers a list-level calculation.
	 * @param calculation PairedListCalculation to cache
	 */
	public void setCalculation(PairedListCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, PairedListCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	/**
	 * Retrieves a list-level calculation result.
	 * @param name calculation name
	 * @param calc segment-level calculation used in aggregation
	 * @return calculated value
	 */
	public double getCalculation(String name, PairedSegmentCalculation calc) {
		return calculationMap.get(name).get(calc);
	}


}
