package calculations;

import dataStructure.FrameSet;

public abstract class FrameSetCalculation extends Data {

	String name;
	boolean statistic;
	double solution;
	final double flag = Double.MIN_VALUE;
	FrameSet frameSet;
	
	public FrameSetCalculation() {
		solution = flag;
		name = getName();
		statistic = isStatistic(); 
	}
		
	//access link/segments from here
	public void setFrameSet(FrameSet frameSet) {
		this.frameSet = frameSet;
	}
	
	public double get() {
		if (solution != flag) return solution; 
		solution = calculate();
		return solution; 
	}
	
	public DataType getType() {
		return DataType.FRAMESET_CALCULATION;
	}
	
	public abstract boolean isStatistic();
	
	public abstract String getName();
	
	public abstract double calculate();	
		
}
