package dataStructure;

import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.HashMap;

import calculations.SegmentCalculation;
import ij.gui.Roi;

public abstract class SegmentModel implements Serializable { 
	
	private static final long serialVersionUID = 1L;

	protected int frame;
	String name; 
	Color color = null;
	boolean isFirstFrame;
	boolean isLastFrame;
	LinkSet linkSet;
	boolean boundaryContact;
	
	//Holding Calculations
	transient HashMap <String, SegmentCalculation> calculationMap;
	
	//Getter methods
	public String getName() {
		return name;
	}

	public int getFrame() {
		return frame;
	}
	
	public LinkSet getLinkSet() {
		return linkSet;
	}
	
	public Color getColor() {
		return color;
	}
	
	//Setter methods
	public void setFrame(int frame) {
		this.frame = frame;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	
	//CURRENTLY UNUSED
	public boolean getIsFirstFrame() {
		return isFirstFrame;		
	}
	
	public boolean getIsLastFrame() {
		return isLastFrame;		
	}
	
	public void setIsFirstFrame(boolean isFirstFrame) {
		this.isFirstFrame = isFirstFrame;		
	}
	
	public void setIsLastFrame(boolean isLastFrame) {
		this.isLastFrame = isLastFrame;	
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
}
