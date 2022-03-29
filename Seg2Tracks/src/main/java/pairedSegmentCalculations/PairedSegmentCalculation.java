package pairedSegmentCalculations;

import calculations.Data;
import calculations.DataType;
import pairedDataStructure.PairedSegment;

public abstract class PairedSegmentCalculation extends Data {
	
	String name;
	double solution;
	final double flag = Double.MIN_VALUE;
	PairedSegment[] pairs;
	
	public PairedSegmentCalculation() {
		solution = flag;
		name = getName();
	}
	
	public void setSegments(PairedSegment...pairs) {
		this.pairs = pairs;
	}
	
	public double get() {
		if (solution != flag) return solution; 
		calculate();
		return solution;
	}
	
	public DataType getType() {
		return DataType.SEGMENT_CALCULATION;
	}
	
	public abstract void calculate();
	
}