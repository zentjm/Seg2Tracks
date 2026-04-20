package pairedDataStructure;

import java.util.ArrayList;
import java.util.HashMap;

import pairedSegmentCalculations.PairedDataCalculation;
import pairedSegmentCalculations.PairedListCalculation;
import pairedSegmentCalculations.PairedSegmentCalculation;

/**
 * PairedDataSet holds all paired comparisons between automatic and manual segmentations across a dataset.
 * Extends ArrayList<PairedList> to manage multiple cell-level pairings.
 * Used for validation and quality metrics.
 */
public class PairedDataSet extends ArrayList<PairedList> {

	HashMap <String, PairedDataCalculation> calculationMap; // Cached dataset-level calculations


	/**
	 * Default constructor.
	 */
	public PairedDataSet() {

	}

	/**
	 * Registers a dataset-level calculation.
	 * @param dataCalculation PairedDataCalculation to cache
	 */
	public void setCalculation(PairedDataCalculation dataCalculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, PairedDataCalculation>();
		calculationMap.put(dataCalculation.getName(), dataCalculation);
	}


	/**
	 * Retrieves a dataset-level calculation result.
	 * @param name calculation name
	 * @param segCalc segment-level calculation
	 * @param listCalc list-level (cell-level) calculation
	 * @return calculated value
	 */
	public double getCalculation(String name, PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
		return calculationMap.get(name).get(segCalc, listCalc);
	}



	/*
	// DEAD CODE: Alternative structure design with separate segment and list calculation maps


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
