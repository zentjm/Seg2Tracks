package calculations;

import dataStructure.Segment;

public class Confluency extends FrameSetCalculation {

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public String getName() {
		return "Cell Confluency";
	}

	@Override
	public double calculate() {
		
		double total = 0;
		for (Segment seg : frameSet) {
			total += seg.getCalculation("Area"); //TODO: some sort of automatic finding of the "Area" name. 
		}
		
		return total;
	}
}
