package pairedSegmentCalculations;

import pairedDataStructure.PairedSegment;

public class ListMean extends PairedListCalculation {

	
	@Override
	public String getName() {
		return "Mean";
	}

	@Override
	public double calculate(PairedSegmentCalculation calc) {
		double sum = 0;
		for (int i = 0; i < pairedList.getList().length; i++) {
			sum += pairedList.get(i).getCalculation(calc.getName());
			System.out.println("Sum @ " + i + " is: " + sum);
		}
		
		double average =  sum/pairedList.getList().length;
		System.out.println("Average: " + average);
		return average;
	}

}
