package dataStructure;

import java.util.ArrayList;
import java.util.HashMap;

import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;

public class FrameSet extends ArrayList<Segment> {
	
	private static final long serialVersionUID = 1L;
	
	int frame;
	DataSet dataSet;
	
	HashMap<String, FrameSetCalculation> calculationMap;
	HashMap<String, FrameSetStatistic> statisticMap;
	
	public FrameSet(int frame, DataSet dataSet) {
		this.frame = frame;
		this.dataSet = dataSet;
	}
	
	public int getFrame() {
		return frame;
	}
	
	//Sets calculations
	public void setCalculation(FrameSetCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, FrameSetCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}
	
	//Gets calculation
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}
	
	//Sets statistics
	public void setStatistic(FrameSetStatistic statistic) {
		if (statisticMap == null) statisticMap = new HashMap<String, FrameSetStatistic>();
		statisticMap.put(statistic.getName(), statistic);
	}
	
	//Gets statics
	public double getStatistic(String statistic, String calculation) {
		return statisticMap.get(statistic).get(calculation);
	}
	
	//Remove segment
	public void removeSegment (SegmentModel segment) {
		//TODO: remove the segment
	}
	
	
	
	
	
	
	
	
	
	
	
	
}
