package dataStructure;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.io.Serializable;
import java.util.HashMap;

import calculations.SegmentCalculation;
import ij.gui.Roi;


/**
 * Segment represents a single segmented cell in a single frame.
 * Core data object in Seg2Tracks, storing morphological information including boundaries,
 * marker location, intensity data, and tracking information. Each Segment is associated
 * with a LinkSet (tracking across frames) and a FrameSet (all cells in one frame).
 */
public class Segment extends SegmentModel {

	private static final long serialVersionUID = 1L;

	Point centerPoint; // Central point marker for this cell
	Point [] externalPerimeter; // External boundary points (cell outline)
	Point [] internalPerimeter; // Internal boundary points (nucleus or internal structure)
	transient Roi roi; // ImageJ Region of Interest—not persisted
	transient Roi userRoi; // User-edited Region of Interest—not persisted
	boolean roiSelected; // Whether an ROI has been selected for this Segment
	boolean manuallyEdited; // Whether user has manually modified this Segment
	boolean externalBoundaryContact; // Whether cell touches image edge (external)
	boolean internalBoundaryContact; // Whether cell touches internal structure edge

	// Constructor

	/**
	 * Constructs a Segment for a specific frame with a center point marker.
	 * @param frame frame index (0-based)
	 * @param centerPoint center coordinates
	 */
	public Segment (int frame, Point centerPoint) {
		this.frame = frame;
		this.centerPoint = centerPoint;
		name = " ";
		isFirstFrame = true;
		isLastFrame = true;
		roiSelected = false;
		manuallyEdited = false;
		externalBoundaryContact = false;
		internalBoundaryContact = false;
	}

	// Constructor for deep copy of Segment

	/**
	 * Copy constructor—creates a new Segment with same frame and properties.
	 * Does NOT copy perimeter data.
	 * @param seg source Segment to copy
	 */
	public Segment (Segment seg) {
		this.frame = seg.frame;
		this.centerPoint = seg.centerPoint;
		name = " ";
		isFirstFrame = seg.isFirstFrame;
		isLastFrame = seg.isLastFrame;
		roiSelected = seg.roiSelected;
		manuallyEdited = seg.manuallyEdited;
		externalBoundaryContact = seg.externalBoundaryContact;
		internalBoundaryContact = seg.internalBoundaryContact;
	}

	// Constructor for deep copy with external boundary

	/**
	 * Copy constructor with option to copy external perimeter.
	 * @param seg source Segment
	 * @param fact boolean flag (when true, copies external perimeter)
	 */
	public Segment (Segment seg, boolean fact) {
		this.frame = seg.frame;
		this.centerPoint = seg.centerPoint;
		name = " ";
		isFirstFrame = seg.isFirstFrame;
		isLastFrame = seg.isLastFrame;
		roiSelected = seg.roiSelected;
		manuallyEdited = seg.manuallyEdited;
		externalBoundaryContact = seg.externalBoundaryContact;
		internalBoundaryContact = seg.internalBoundaryContact;
		externalPerimeter = seg.getExternalPerimeter(); // UNCLEAR: why only external and not internal?
		//internalPerimeter = seg.getInternalPerimeter();
	}

	// Getters

	/**
	 * Gets the center point marker.
	 * @return center coordinates
	 */
	public Point getCenterPoint() {
		return centerPoint;
	}

	/**
	 * Gets the external boundary points.
	 * @return array of external perimeter Points
	 */
	public Point[] getExternalPerimeter() {
		return externalPerimeter;
	}

	/**
	 * Gets the internal boundary points.
	 * @return array of internal perimeter Points
	 */
	public Point[] getInternalPerimeter() {
		return internalPerimeter;
	}

	/**
	 * Gets the ImageJ Region of Interest.
	 * @return transient ROI (not serialized)
	 */
	public Roi getRoi() {
		return roi;
	}

	/**
	 * Gets the user-edited Region of Interest.
	 * @return user ROI (not serialized)
	 */
	public Roi getUserRoi() {
		return userRoi;
	}

	/**
	 * Checks if an ROI has been selected for this Segment.
	 * @return true if ROI is selected
	 */
	public boolean getRoiSelected() {
		return roiSelected;
	}

	/**
	 * Checks if this Segment has been manually edited.
	 * @return true if user modified it
	 */
	public boolean isManuallyEdited() {
		return manuallyEdited;
	}

	/**
	 * Checks if this cell touches the external image boundary.
	 * @return true if external boundary contact
	 */
	public boolean getExternalBoundaryContact() {
		return  externalBoundaryContact;
	}

	/**
	 * Checks if this cell touches an internal boundary (nucleus).
	 * @return true if internal boundary contact
	 */
	public boolean getInternalBoundaryContact() {
		return internalBoundaryContact;
	}

	// Setters

	/**
	 * Sets the center point marker.
	 * @param centerPoint center coordinates
	 */
	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}

	/**
	 * Sets the external boundary points.
	 * @param externalPerimeter array of external perimeter Points
	 */
	public void setExternalPerimeter(Point[] externalPerimeter) {
		this.externalPerimeter = externalPerimeter;
	}

	/**
	 * Sets the internal boundary points.
	 * @param internalPerimeter array of internal perimeter Points
	 */
	public void setInternalPerimeter(Point[] internalPerimeter) {
		this.internalPerimeter = internalPerimeter;
	}

	/**
	 * Sets the ImageJ Region of Interest.
	 * @param roi transient ROI
	 */
	public void setRoi(Roi roi) {
		this.roi = roi;
	}

	/**
	 * Sets the user-edited Region of Interest.
	 * @param userRoi user ROI
	 */
	public void setUserRoi(Roi userRoi) {
		this.userRoi = userRoi;
	}

	/**
	 * Sets whether an ROI has been selected.
	 * @param roiSelected true if selected
	 */
	public void setRoiSelected (boolean roiSelected) {
		this.roiSelected = roiSelected;
	}

	/**
	 * Sets the manually edited flag.
	 * @param manuallyEdited true if user modified
	 */
	public void setManuallyEdited(boolean manuallyEdited) {
		this.manuallyEdited = manuallyEdited;
	}

	/**
	 * Sets external boundary contact flag.
	 * @param externalBoundaryContact true if touching image edge
	 */
	public void setExternalBoundaryContact(boolean externalBoundaryContact) {
		this.externalBoundaryContact = externalBoundaryContact;
	}

	/**
	 * Sets internal boundary contact flag.
	 * @param internalBoundaryContact true if touching internal boundary
	 */
	public void setInternalBoundaryContact(boolean internalBoundaryContact) {
		this.internalBoundaryContact = internalBoundaryContact;
	}


	/* OLDER VERSION (archived for reference)
	//for saving data
	private static final long serialVersionUID = 1L;

	//params
	int frame;
	String name;
	Point centerPoint;
	Point adjCenterPoint;
	Point [] externalPerimeter;
	Point [] internalPerimeter;
	RangeSetArea externalArea;
	RangeSetArea internalArea;
	boolean manuallyModified;

	//color
	Color color = null;

	//For marking if it is the first or last object
	boolean isFirstFrame;
	boolean isLastFrame;

	//Possible organization: TODO
	LinkSet linkSet;

	//Holding variables for manual selection/modification.
	transient Roi roi;
	boolean roiSelected;

	//Holding Calculations
	transient HashMap <String, SegmentCalculation> calculationMap;

	//Constructor
	public Segment (int frame, Point centerPoint) {
		this.frame = frame;
		this.centerPoint = centerPoint;
		name = " ";
		isFirstFrame = true;
		isLastFrame = true;
		roiSelected = false;
	}

	//Getter methods
	public int getFrame() {
		return frame;
	}

	public String getName() {
		//TODO: throw exception if null
		return name;
	}

	public Point getCenterPoint() {
		return centerPoint;
	}

	public Point getAdjCenterPoint() {
		//TODO: throw exception if null
		return adjCenterPoint;
	}

	public Point[] getExternalPerimeter() {
		//TODO: throw exception if null
		return externalPerimeter;
	}

	public Point[] getInternalPerimeter() {
		//TODO: throw exception if null
		return internalPerimeter;
	}

	public boolean getIsFirstFrame() {
		return isFirstFrame;
	}

	public boolean getIsLastFrame() {
		return isLastFrame;
	}

	public LinkSet getLinkSet() {
		return linkSet;
	}

	public Color getColor() {
		return color;
	}

	public Roi getRoi() {
		return roi;
	}

	public boolean getRoiSelected() {
		return roiSelected;
	}

	//Setter methods
	public void setFrame(int frame) {
		this.frame = frame;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}

	public void setAdjCenterPoint(Point adjCenterPoint) {
		this.adjCenterPoint = adjCenterPoint;
	}

	public void setExternalPerimeter(Point[] externalPerimeter) {
		this.externalPerimeter = externalPerimeter;
	}

	public void setInternalPerimeter(Point[] internalPerimeter) {
		this.internalPerimeter = internalPerimeter;
	}

	public void setIsFirstFrame(boolean isFirstFrame) {
		this.isFirstFrame = isFirstFrame;
	}

	public void setIsLastFrame(boolean isLastFrame) {
		this.isLastFrame = isLastFrame;
	}

	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
		linkSet.add(this);
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setRoi(Roi roi) {
		this.roi = roi;
	}

	public void setRoiSelected (boolean roiSelected) {
		this.roiSelected = roiSelected;
	}

	//Checker methods
	public boolean hasLinkSet() {
		if (linkSet != null) return true;
		return false;
	}

	//Sets the calculation methods
	public void setCalculation(SegmentCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, SegmentCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	//Gets the result of the calculation
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}

	//Gets the result map for the internal calculations
	public HashMap <String, SegmentCalculation> getInternalCalculationMap() {
		return calculationMap;
	}
	*/




}
