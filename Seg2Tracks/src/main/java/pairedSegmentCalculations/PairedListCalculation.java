package pairedSegmentCalculations;

import java.util.HashMap;

import calculations.Data;
import calculations.DataType;
import pairedDataStructure.PairedList;
import pairedDataStructure.PairedSegment;

public abstract class PairedListCalculation extends Data {
	
	String name;
	PairedList pairedList;
	PairedSegmentCalculation[] segmentCalcs;
	
	//for STATISTICS data
	HashMap <PairedSegmentCalculation, Double> solutionMap;
	
	public PairedListCalculation() {
		name = getName();
		solutionMap = new HashMap <PairedSegmentCalculation, Double>(); //STATISTIC
	}
	
	public void setList(PairedList pairedList) {
		this.pairedList = pairedList;
	}
	
	public void setSegmentCalculations(PairedSegmentCalculation[] segmentCalcs) {
		this.segmentCalcs = segmentCalcs;
	}
	
	public double get(PairedSegmentCalculation calc) {
		if (solutionMap.get(calc) == null) solutionMap.put(calc,calculate(calc));
		return solutionMap.get(calc);
	}
	
	public DataType getType() {
		return DataType.LINKSET_CALCULATION;
	}
	
	//Return the calculation on the paired segment
	public abstract double calculate(PairedSegmentCalculation calc);
	
}

