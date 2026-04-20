package pairedSegmentCalculations;

import calculations.DataType;

/**
 * DataMean computes the average of a metric across all paired cells in a dataset.
 * Aggregates cell-level statistics to dataset level (mean of means).
 */
public class DataMean extends PairedDataCalculation {

	@Override
	public String getName() {
		return "Mean";
	}

	/**
	 * Calculates dataset-level mean by averaging cell-level calculations.
	 * @param segCalc segment-level calculation
	 * @param listCalc cell-level (list-level) aggregation
	 * @return average across all paired cells
	 */
	@Override
	public double calculate(PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
		double sum = 0;
		for (int i = 0; i < pairedDataSet.size(); i++) {
			sum += pairedDataSet.get(i).getCalculation(listCalc.getName(), segCalc);
			//System.out.println("Sum @ " + i + " is: " + sum); // DEBUG output
		}

		double average =  sum/pairedDataSet.size();
		//System.out.println("Average: " + average); // DEBUG output
		return average;
	}

}
