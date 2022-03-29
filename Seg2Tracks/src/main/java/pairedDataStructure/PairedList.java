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

public class PairedList {

	DataSet dataSet1;
	DataSet dataSet2;
	
	LinkSet linkSet1;
	LinkSet linkSet2;
	
	Color seg1Color;
	Color seg2Color;
	
	enum Status {
		EMPTY,
		LINKED,
		UNLINKED
	}
	
	Status status;
	PairedSegment[] pairedList;
	int name;
	
	HashMap <String, PairedListCalculation> calculationMap; //For list-averaged segment calculations
	double calculation;										//For list calculation
	
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
		
		if (linkSet1 != null && linkSet2 != null) initializeByLinkSet();
		if (linkSet2 == null) {
			int frames = dataSet1.getSize();
			pairedList = new PairedSegment[frames];
			for (Segment seg : linkSet1) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
				pairedList[seg.getFrame()].setSeg1(seg);
				
			}
		}
		
		if (linkSet1 == null) {
			int frames = dataSet2.getSize();
			pairedList = new PairedSegment[frames];
			for (Segment seg : linkSet2) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
				pairedList[seg.getFrame()].setSeg2(seg);
			}
		}	
	}
	
	
	//aligns the linkSets. 
	public void initializeByLinkSet() {
		int frames = dataSet1.getSize();
		pairedList = new PairedSegment[frames]; //TODO: assume dataSet1 has the bigger size?

		//Initializes list with PairedSetgments.
		for (int i = 0; i < pairedList.length; i++) {
			//need to do anything?
		}
		
		//add from first linkset
		for (Segment seg : linkSet1) {
			if (pairedList[seg.getFrame()] == null) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
			}
			pairedList[seg.getFrame()].setSeg1(seg);	
		}
		
		//add from second linkset
		for (Segment seg : linkSet2) {
			if (pairedList[seg.getFrame()] == null) {
				pairedList[seg.getFrame()] = new PairedSegment(seg.getFrame(), this);
			}
			pairedList[seg.getFrame()].setSeg2(seg);
		}
	}
	
	public PairedSegment[] getList() {
		return pairedList;
	}
	
	public PairedSegment get(int i) {
		return pairedList[i];
	}
	
	public DataSet getDataSet1() {
		return dataSet1;
	}
	
	public DataSet getDataSet2() {
		return dataSet2;
	}
	
	public boolean hasSet1() {
		if (linkSet1 != null) return true;
		return false;
	}
	
	public boolean hasSet2() {
		if (linkSet2 != null) return true;
		return false;
	}
	
	public int getName() {
		return name;
	}
	
	public Color getSeg1Color() {
		return seg1Color;
	}
	
	public Color getSeg2Color() {
		return seg2Color;
	}
	
	public void setSeg1Color(Color seg1Color) {
		this.seg1Color = seg1Color;
		linkSet1.setColor(seg1Color);
		
	}
	
	public void setSeg2Color(Color seg2Color) {
		this.seg2Color = seg2Color;
		linkSet2.setColor(seg2Color);
	}
	
	//Sets the calculation methods
	public void setCalculation(PairedListCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, PairedListCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}
	
	//Gets the result of the calculation
	public double getCalculation(String name, PairedSegmentCalculation calc) {
		return calculationMap.get(name).get(calc);
	}
	
	
	
}
