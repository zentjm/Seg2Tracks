package calculations;

import dataStructure.Segment;

public class LinkSetMean extends LinkSetStatistic {

	public LinkSetMean() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Mean";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate(String name) {

		double total = 0;
		for (Segment seg: linkSet) {
			total =+ seg.getCalculation(name);
		}
		return total/linkSet.size();
	}

}







