package dataStructure;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.SegmentCalculation;
import pairedSegmentCalculations.PairedListCalculation;
import pairedSegmentCalculations.PairedSegmentCalculation;

public class LinkSet extends LinkSetModel<Segment> {

	private static final long serialVersionUID = 1L;
	int name;
	int length;
	int startFrame;
	int endFrame;

	boolean orphan;

	HashMap<String, LinkSetCalculation> calculationMap;
	HashMap<String, LinkSetStatistic> statisticMap;
	
	//Default for backwards compatibility as of September 24th
	Color color = null;
	
	Segment end;
	Segment start;
	
	DataSet dataSet;
	
	LinkSet parent;
	ArrayList<LinkSet> children;
	
	public LinkSet(DataSet dataSet) {
		this.dataSet = dataSet;
		this.dataSet.addLinkSet(this); //added Jul 6th
		children = new ArrayList<LinkSet>();
		orphan = false;
	}
	
	public LinkSet(Segment segment, DataSet dataSet) {
		this (dataSet);
		add(segment);
	}
	
	//Setters
	public void addSegment(Segment segment) {
		add(segment);
	}
	
	public void setName (int name) {
		this.name = name;
	}
	
	public void setEnd (Segment end) {
		this.end = end;
	}
	
	public void setStart (Segment start) {
		this.start = start;
	}
	
	public void setColor (Color color) {
		this.color = color;
	}
	
	public void addChild (LinkSet child) {
		children.add(child);
	}
	
	public void setOrphan (boolean orphan) {
		this.orphan = orphan;
	}
	
	//Getters
	public DataSet getDataSet() {
		return dataSet;
	}
	
	public int getName() {
		return name;
	}
	
	public Color getColor () {
		return color;
	}
	
	public boolean isOrphan() {
		return orphan;
	}
	
	public boolean getExternalBoundaryContact() {
		for (Segment segment : this) {
			System.out.println("Segment " + segment.getExternalBoundaryContact());
			if (segment.getExternalBoundaryContact()) return true;
		}
		return false;
	}
	
	public boolean getInternalBoundaryContact() {
		for (Segment segment : this) {
			if (segment.getInternalBoundaryContact()) return true;
		}
		return false;
	}
	
	
	//Sets calculations
	public void setCalculation(LinkSetCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, LinkSetCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}
	
	//Gets calculation
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}
	
	//Sets statistics
	public void setStatistic(LinkSetStatistic statistic) {
		if (statisticMap == null) statisticMap = new HashMap<String, LinkSetStatistic>();
		statisticMap.put(statistic.getName(), statistic);
	}
	
	//Gets statics
	public double getStatistic(String statistic, String calculation) {
		return statisticMap.get(statistic).get(calculation);
	}
	
	
	
	

}
