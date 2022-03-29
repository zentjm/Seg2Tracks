package calculations;

import dataStructure.Segment;
import ij.process.ImageProcessor;

public abstract class SegmentCalculation extends Data {
	
	boolean statistic; //TODO: determines if LinkSet values (mean, etc) will be performed on this one. 
	String name;
	double solution;
	double flag = Double.MIN_VALUE;
	Segment[] segments;
	ImageProcessor processor;
	
	public SegmentCalculation() {
		solution = flag;
		name = getName();
		statistic = isStatistic();
	}
	
	public void setTargetProcessor(ImageProcessor processor) {
		this.processor = processor;
	}
	
	public void setSegments(Segment... segments) { //TODO: this does not need to be an array
		this.segments = segments;
	}
	
	public double get() {
		if (solution != flag) return solution; 
		solution = calculate();
		return solution;
	}

	public DataType getType() {
		return DataType.SEGMENT_CALCULATION;
	}
	
	public abstract boolean isStatistic();
	
	public abstract double calculate();
	
}
