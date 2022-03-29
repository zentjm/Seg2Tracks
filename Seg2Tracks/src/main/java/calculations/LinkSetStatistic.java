package calculations;

import java.util.HashMap;

import dataStructure.LinkSet;


public abstract class LinkSetStatistic extends Data {

	String name;
	boolean statistic;
	LinkSet linkSet;
	String [] calculationNames;
	
	//for STATISTICS data
	HashMap <String, Double> solutionMap;
	
	public LinkSetStatistic() {
		name = getName();
		statistic = isStatistic();
		solutionMap = new HashMap <String, Double>(); //STATISTIC	
	}

	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
	}
	
	public void setSegmentCalculations(String[] calculationNames) {
		this.calculationNames = calculationNames;
	}
	
	public double get(String calcName) {
		if (solutionMap.get(calcName) == null) solutionMap.put(calcName, calculate(calcName));
		return solutionMap.get(calcName);
	}
	
	public DataType getType() {
		return DataType.LINKSET_STATISTIC;
	}
	
	public abstract String getName();
	public abstract boolean isStatistic();
	public abstract double calculate(String calcName);

}
