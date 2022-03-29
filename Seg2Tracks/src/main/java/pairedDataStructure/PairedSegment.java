package pairedDataStructure;

import java.awt.Color;
import java.util.HashMap;

import calculations.SegmentCalculation;
import dataStructure.Segment;
import dataStructure.SegmentModel;
import pairedSegmentCalculations.PairedSegmentCalculation;

public class PairedSegment extends SegmentModel {
	
	private static final long serialVersionUID = 1L;
	Segment seg1;
	Segment seg2;
	PairedList list;
	HashMap <String, PairedSegmentCalculation> calculationMap;

	
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

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/* OLDER VERSION
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
