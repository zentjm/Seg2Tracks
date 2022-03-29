package pairedSegmentCalculations;

import calculations.DataType;

public class DataMean extends PairedDataCalculation {

	@Override
	public String getName() {
		return "Mean";
	}

	@Override
	public double calculate(PairedSegmentCalculation segCalc, PairedListCalculation listCalc) {
		double sum = 0;
		for (int i = 0; i < pairedDataSet.size(); i++) {
			sum += pairedDataSet.get(i).getCalculation(listCalc.getName(), segCalc);
			System.out.println("Sum @ " + i + " is: " + sum);
		}
		
		double average =  sum/pairedDataSet.size();
		System.out.println("Average: " + average);
		return average;
	}
	
	

}
