package calculations;

import java.util.HashMap;
import dataStructure.LinkSet;
import dataStructure.Segment;

public abstract class LinkSetCalculation extends Data {


	boolean statistic; ///TODO: if this can be abstracted for dataset statistics 
	
	String name;
	double solution;
	final double flag = Double.MIN_VALUE;
	LinkSet linkSet;
	
	//For Statistic
	public LinkSetCalculation() {
		solution = flag;
		name = getName();
		statistic = isStatistic(); 
	}
	
	//access link/segments from here
	public void setLinkSet(LinkSet linkSet) {
		this.linkSet = linkSet;
	}
	
	public double get() {
		if (solution != flag) return solution; 
		solution = calculate();
		return solution; 
	}
	
	public DataType getType() {
		return DataType.LINKSET_CALCULATION;
	}
	
	public abstract boolean isStatistic();
	
	public abstract String getName();
	
	public abstract double calculate();
		
}


	/*
 	 * FOR ACCESS TO Seg2tracks MEAN:
 	 * --> For each SegmentModel
 	 * 	--> go linkset, access segment model, access calculations 
 	 * 		--> for each calculation set to "Statistic"
 	 * 			--> get calculation. If no calculation, force calculation then return
 	 * 			--> add calculation results to hashmap of calculation
 	 * 
 	 * FOR SINGLE STAT generation for a LINKSET
 	 * --> For each segmentModel
 	 * --> Go linkset, access segment model OR segmentModel calculation
 	 * 		--> Do calculation.
 	 * 		--> Get data.
 	 * 		--> Store in solution.
 	 * 
 	 * get calculations of all segments 
 	 * 
 	 * 
 	 * 
 	 * 
 	 */		





/*
 * String name;
	double solution;
	double flag = Double.MIN_VALUE;
	SegmentModel[] segments;
	
	public SegmentCalculation() {
		solution = flag;
		name = getName();
	}
	
	public void setSegments(SegmentModel... segments) {
		this.segments = segments;
	}
	
	public double get() {
		if (solution != flag) return solution; 
		calculate();
		return solution;
	}
	
	
	public abstract String getName();
	
	public abstract void calculate();
 */



