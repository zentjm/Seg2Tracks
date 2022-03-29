package pairedDataStructure;

import java.util.ArrayList;
import java.util.HashMap;

import pairedSegmentCalculations.PairedDataCalculation;
import pairedSegmentCalculations.PairedListCalculation;
import pairedSegmentCalculations.PairedSegmentCalculation;

public class PairedDataSet extends ArrayList<PairedList> {

	HashMap <String, PairedDataCalculation> calculationMap;

	
	public PairedDataSet() {
	
	}
	
	public void setCalculation(PairedDataCalculation dataCalculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, PairedDataCalculation>();
		calculationMap.put(dataCalculation.getName(), dataCalculation);
	}
	

	public double getCalculation(String name, PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
		return calculationMap.get(name).get(segCalc, listCalc);
	}
	
	
	
	/*
	
	
	//Sets the result of measurement derived from a segment-calculated measurement //TODO calculations should have common abstract class
	public void setCalculation(PairedSegmentCalculation segmentCalculation, PairedListCalculation listCalculation) {
		if (segmentCalculation != null) {
			if (segmentCalculationMap == null) segmentCalculationMap = new HashMap<String, PairedSegmentCalculation>();
			segmentCalculationMap.put(segmentCalculation.getName(), segmentCalculation);
		}
		
		if (listCalculation != null) {
			if (listCalculationMap == null) listCalculationMap = new HashMap<String, PairedListCalculation>();
			listCalculationMap.put(listCalculation.getName(), listCalculation);
		}
	}
	
	//Sets the result of measurement derived from a list-calculated measurement 
	
	
	/*Gets the result of a list-averaged segment measurement 
	public double getCalculation(String name, PairedSegmentCalculation calc) {
		
		if (calc == null) return getCalculation(name);
		
		return calculationMap.get(name).get(calc);
	}
	
	
	public double getCalculation(String name) {
		return 0;
	}
	*/
	
	
	

		
}
