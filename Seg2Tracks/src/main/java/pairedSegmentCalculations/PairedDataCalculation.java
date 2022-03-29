package pairedSegmentCalculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import calculations.Data;
import calculations.DataType;
import pairedDataStructure.PairedDataSet;
import pairedDataStructure.PairedList;
import pairedDataStructure.PairedSegment;

public abstract class PairedDataCalculation extends Data {

	String name;
	PairedDataSet pairedDataSet;
	PairedSegmentCalculation[] segmentCalcs;
	PairedListCalculation listCalc;
	
	class Key {
		
		PairedListCalculation listCalc;
		PairedSegmentCalculation segCalc;
		
		public Key(PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
			this.listCalc = listCalc;
			this.segCalc = segCalc;
		}
		
		 @Override
	    public boolean equals(Object o) {
			if (this == o) return true;
	        if (!(o instanceof Key)) return false;
	        Key key = (Key) o;
	        return listCalc == key.listCalc && segCalc == key.segCalc;
	    }

		 //x = listCalc, y = segCalc
	    @Override
	    public int hashCode() {
	       int result = listCalc.hashCode();
	        result = 31 * result + segCalc.hashCode(); //TODO: stronger hashCode?
	        return result;  
	    }
	}
	
	//for STATISTICS data
	HashMap <Key, Double> solutionMap;
	
	public PairedDataCalculation() {
		name = getName();
		solutionMap = new HashMap <Key, Double>();
	}
	
	public void setList(PairedDataSet pairedDataSet) {
		this.pairedDataSet = pairedDataSet;
	}
	
	public void setCalculations(PairedSegmentCalculation[] segmentCalcs, PairedListCalculation listCalc) {
		this.listCalc = listCalc;
		this.segmentCalcs = segmentCalcs;
	}
	
	public double get(PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
		if (solutionMap.get(new Key(segCalc,listCalc)) == null) {
			solutionMap.put(new Key(segCalc,listCalc), calculate(segCalc, listCalc));
		}
		return solutionMap.get(new Key(segCalc,listCalc));
	}
	
	public DataType getType() {
		return DataType.DATASET_CALCULATON;
	}

	//return the calculation on the paired segment
	public abstract double calculate(PairedSegmentCalculation calc, PairedListCalculation listCalc);
	//As in the mean of means
		
	
}
