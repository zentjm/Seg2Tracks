package calculations;

import java.util.HashMap;

import dataStructure.FrameSet;

public abstract class FrameSetStatistic extends Data {

	boolean statistic;
	String name;
	FrameSet frameSet;
	String [] calculationNames;
	
	HashMap <String, Double> solutionMap;

	public FrameSetStatistic() {
		name = getName();
		statistic = isStatistic();
		solutionMap = new HashMap <String, Double>(); //STATISTIC	
	}
	
	public void setFrameSet(FrameSet frameSet) {
		this.frameSet = frameSet;
	}

	public void setFrameSetCalculations(String[] calculationNames) {
		this.calculationNames = calculationNames;
	}
	
	public double get(String calcName) {
		if (solutionMap.get(calcName) == null) solutionMap.put(calcName, calculate(calcName));
		return solutionMap.get(calcName);
	}
	
	public DataType getType() {
		return DataType.FRAMESET_STATISTIC;
	}
	
	public abstract String getName();
	public abstract boolean isStatistic();
	public abstract double calculate(String calcName);

}
