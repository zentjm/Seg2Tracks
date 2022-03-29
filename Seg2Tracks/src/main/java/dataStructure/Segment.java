package dataStructure;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.io.Serializable;
import java.util.HashMap;

import calculations.SegmentCalculation;
import ij.gui.Roi;


/**
 * @author jmz0000
 * The SegmentModel class is the basic data unit for Seg2Tracks, holding morphological information
 * about a segmented area. In timelapse or 3D analysis, these objects are joined along frames in a FrameSet and 
 * matched along the 3rd dimension in a LinkSet
 */

public class Segment extends SegmentModel {
	
	private static final long serialVersionUID = 1L;
	Point centerPoint;
	Point [] externalPerimeter;
	Point [] internalPerimeter;
	transient Roi roi;
	boolean roiSelected;
	boolean manuallyEdited;
	boolean externalBoundaryContact;
	boolean internalBoundaryContact;
	
	//Constructor
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
	
	//Constructor for deepCopy Segment
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
	
	
	
	//Getters
	public Point getCenterPoint() {
		return centerPoint;
	}
	
	public Point[] getExternalPerimeter() {
		return externalPerimeter;
	}
	
	public Point[] getInternalPerimeter() {
		return internalPerimeter;
	}
	
	public Roi getRoi() {
		return roi;
	}
	
	public boolean getRoiSelected() {
		return roiSelected;
	}
	
	public boolean isManuallyEdited() {
		return manuallyEdited;
	}
	
	public boolean getExternalBoundaryContact() {
		return  externalBoundaryContact;
	}
	
	public boolean getInternalBoundaryContact() {
		return internalBoundaryContact;
	}

	//Setters
	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}
	
	public void setExternalPerimeter(Point[] externalPerimeter) {
		this.externalPerimeter = externalPerimeter;
	}
	
	public void setInternalPerimeter(Point[] internalPerimeter) {
		this.internalPerimeter = internalPerimeter;
	}
	
	public void setRoi(Roi roi) {
		this.roi = roi;
	}
	
	public void setRoiSelected (boolean roiSelected) {
		this.roiSelected = roiSelected;
	}
	
	public void setManuallyEdited(boolean manuallyEdited) {
		this.manuallyEdited = manuallyEdited;
	}
	
	public void setExternalBoundaryContact(boolean externalBoundaryContact) {
		this.externalBoundaryContact = externalBoundaryContact;
	}
	
	public void setInternalBoundaryContact(boolean internalBoundaryContact) {
		this.internalBoundaryContact = internalBoundaryContact;
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/* OLDER VERSION
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
