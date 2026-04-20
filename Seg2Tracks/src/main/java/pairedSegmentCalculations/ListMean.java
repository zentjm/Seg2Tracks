package pairedSegmentCalculations;

import pairedDataStructure.PairedSegment;

/**
 * ListMean computes the average of a metric across all frames for one tracked cell.
 * Aggregates frame-level (segment-level) statistics to cell level.
 */
public class ListMean extends PairedListCalculation {


	@Override
	public String getName() {
		return "Mean";
	}

	/**
	 * Calculates cell-level mean by averaging segment-level calculations across frames.
	 * @param calc segment-level calculation to aggregate
	 * @return average across all frames for this cell
	 */
	@Override
	public double calculate(PairedSegmentCalculation calc) {
		double sum = 0;
		for (int i = 0; i < pairedList.getList().length; i++) {
			sum += pairedList.get(i).getCalculation(calc.getName());
			//System.out.println("Sum @ " + i + " is: " + sum);
		}

		double average =  sum/pairedList.getList().length;
		//System.out.println("Average: " + average);
		return average;
	}

}
