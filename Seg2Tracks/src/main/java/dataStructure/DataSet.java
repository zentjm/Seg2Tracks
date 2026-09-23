package dataStructure;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * DataSet is the top-level container for all segmentation and tracking data.
 * Holds all frames of a time-lapse/volumetric image and organizes cells tracked across them.
 * Tracks which processing steps (identification, linkage, segmentation) have been performed.
 */
public class DataSet implements Serializable {
	private static final long serialVersionUID = 1L;

	// Metadata: tracks which processing steps have been completed
	String name;
	boolean identificationExists;
	boolean linkageExists;
	boolean internalSegmentationExists;
	boolean externalSegmentationExists;
	boolean loadedFile;
	boolean manuallyEdited;

	// Data Collections: all frames and all tracked cells
	FrameSet [] frameSetList;
	ArrayList <LinkSet> linkSetList;

	// Name: human-readable identifier for this dataset
	String dataSetName = "noname";

	// Color: display color for visualization
	Color color = Color.WHITE;

	// Dimensions of Associated Input Image
	int width;
	int height;
	int depth;

	// Linkset name iterator: counter for assigning unique IDs to new LinkSets
	int linkSetNameIterator = 0;

	/**
	 * Constructs an empty DataSet with specified dimensions.
	 * @param width image width in pixels
	 * @param height image height in pixels
	 * @param depth number of frames (z-stack depth or time frames)
	 */
	public DataSet(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		frameSetList = new FrameSet[depth]; // Array of FrameSets, one per frame/slice
		linkSetList = new ArrayList <LinkSet>();

		// Initialize all processing flags to false
		identificationExists = false;
		linkageExists = false;
		internalSegmentationExists = false;
		externalSegmentationExists = false;
		loadedFile = false;
		manuallyEdited = false;
	}

	/**
	 * Retrieves the FrameSet for a specific frame index.
	 * @param frame frame index (0 to depth-1)
	 * @return FrameSet containing all Segments in this frame
	 */
	public FrameSet getFrameSet(int frame) {
		return frameSetList[frame];
	}

	/**
	 * Gets all FrameSets (one per frame/slice).
	 * @return array of FrameSets
	 */
	public FrameSet[] getFrameSetList() {
		return frameSetList;
	}

	/**
	 * Retrieves a tracked cell by index.
	 * @param number LinkSet index
	 * @return LinkSet representing one cell across all frames
	 */
	public LinkSet getLinkSet(int number) {
		return linkSetList.get(number);
	}

	/**
	 * Gets all tracked cells.
	 * @return ArrayList of all LinkSets in this dataset
	 */
	public ArrayList<LinkSet> getLinkSetList() {
		return linkSetList;
	}

	/**
	 * Gets the dataset name.
	 * @return human-readable name
	 */
	public String getName() {
		return dataSetName;
	}

	/**
	 * Gets the display color.
	 * @return Color for visualization
	 */
	public Color getColor() {
		return color;
	}

	// Checks which processing steps have been completed
	/**
	 * Checks if cell identification has been performed.
	 * @return true if Segments have been identified
	 */
	public boolean getIdentificationExists() {
		return identificationExists;
	}

	/**
	 * Checks if cell tracking/linkage has been performed.
	 * @return true if cells have been linked across frames
	 */
	public boolean getLinkageExists() {
		return linkageExists;
	}

	/**
	 * Checks if internal boundary segmentation exists.
	 * @return true if internal contours have been traced
	 */
	public boolean getInternalSegmentationExists() {
		return internalSegmentationExists;
	}

	/**
	 * Checks if external boundary segmentation exists.
	 * @return true if external contours have been traced
	 */
	public boolean getExternalSegmentationExists() {
		return externalSegmentationExists;
	}

	// Image dimension accessors
	/**
	 * Gets image width.
	 * @return width in pixels
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Gets image height.
	 * @return height in pixels
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Gets image depth (number of frames).
	 * @return depth (number of z-slices or time frames)
	 */
	public int getSize() {
		return depth;
	}

	/**
	 * Checks if dataset has been manually edited.
	 * @return true if user has modified data
	 */
	public boolean getManuallyEdited() {
		return manuallyEdited;
	}

	/**
	 * Gets the current counter for LinkSet naming.
	 * @return next ID to assign
	 */
	public int getLinkSetNameIterator() {
		return linkSetNameIterator;
	}

	// Setter methods
	/**
	 * Adds a new tracked cell to the dataset.
	 * @param linkSet new LinkSet representing a cell
	 */
	public void addLinkSet(LinkSet linkSet) {
		linkSetList.add(linkSet);
		linkSetNameIterator ++; // Increment counter for next unique ID
	}

	/**
	 * Stores a FrameSet at a specific frame index.
	 * @param frameSet collection of all Segments in frame
	 * @param frame frame index
	 */
	public void addFrameSet(FrameSet frameSet, int frame) {
		frameSetList[frame] = frameSet;
	}

	/**
	 * Replaces the entire FrameSet array.
	 * @param frameSetList new FrameSet array
	 */
	public void setFrameSetList(FrameSet[] frameSetList) {
		this.frameSetList = frameSetList;
	}

	/**
	 * Replaces the entire LinkSet list.
	 * @param linkSetList new ArrayList of LinkSets
	 */
	public void setLinkSetList(ArrayList<LinkSet> linkSetList) {
		this.linkSetList = linkSetList;
	}

	/**
	 * Sets identification processing flag.
	 * @param identificationExists true if cell identification has been performed
	 */
	public void setIdentificationExists(boolean identificationExists) {
		this.identificationExists = identificationExists;
	}

	/**
	 * Sets linkage processing flag.
	 * @param linkageExists true if cells have been tracked across frames
	 */
	public void setLinkageExists(boolean linkageExists) {
		this.linkageExists = linkageExists;
	}

	/**
	 * Sets internal segmentation flag.
	 * @param internalSegmentationExists true if internal boundaries exist
	 */
	public void setInternalSegmentationExists(boolean internalSegmentationExists) {
		this.internalSegmentationExists = internalSegmentationExists;
	}

	/**
	 * Sets external segmentation flag.
	 * @param externalSegmentationExists true if external boundaries exist
	 */
	public void setExternalSegmentationExists(boolean externalSegmentationExists) {
		this.externalSegmentationExists = externalSegmentationExists;
	}

	/**
	 * Sets the dataset name.
	 * @param dataSetName human-readable identifier
	 */
	public void setDataSetName(String dataSetName) {
		this.dataSetName = dataSetName;
	}

	/**
	 * Sets the display color.
	 * @param color Color for visualization
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * Sets the manually edited flag.
	 * @param manuallyEdited true if user has modified this dataset
	 */
	public void setManuallyEdited(boolean manuallyEdited) {
		this.manuallyEdited = manuallyEdited;
	}

	/**
	 * Merges another dataset into this one.
	 * Adds all LinkSets from the other dataset to this dataset.
	 * @param dataSet source dataset to merge from
	 */
	public void addDataSet(DataSet dataSet) {
		for (LinkSet set: dataSet.getLinkSetList()) {
			addLinkSet(set);
		}
	}

	// Operation methods for modifying dataset contents

	/**
	 * Removes a Segment and all its copies across frames (via its LinkSet).
	 * @param deleteSegment SegmentModel to delete
	 */
	public void removeSegment(SegmentModel deleteSegment) {
		LinkSet linkSet = deleteSegment.getLinkSet();
		// Remove all segments in this LinkSet from their respective FrameSets
		for (Segment segment : linkSet) {
			FrameSet fs = this.getFrameSet(segment.getFrame());
			if (fs != null) fs.remove(segment);
		}
		// Remove the LinkSet itself
		linkSetList.remove(linkSet);
	}
	
	
	/**
	 * Removes all segmentation boundaries of a specific type from the dataset.
	 * TODO: Consider replacing magic numbers with enum.
	 * @param type 0 = delete internal segmentations, 1 = delete external segmentations
	 */
	public void removeSegmentationType (int type) {
		// TODO: Other components need to be integrated into this?
		// UNCLEAR: Should this also update related calculations or visualizations?
		for (int i = 0; i < this.getFrameSetList().length; i++) {
			if (this.getFrameSet(i) == null) continue;
			for (Segment segment: this.getFrameSet(i)) {
				if (type == 0) segment.setInternalPerimeter(null); // Clear internal boundaries
				if (type == 1) segment.setExternalPerimeter(null); // Clear external boundaries
			}
		}
		// Update the flag to reflect removal
		if (type == 0) this.setInternalSegmentationExists(false);
		if (type == 1) this.setExternalSegmentationExists(false);
		// TODO: if type exists...
	}

}
