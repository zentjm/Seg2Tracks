package calculations;

import dataStructure.Segment;
import dataStructure.SegmentModel;

public class FrameSetMean extends FrameSetStatistic{

	@Override
	public String getName() {
		return "Mean";
	}

	@Override
	public boolean isStatistic() {
		return true;
	}

	@Override
	public double calculate(String calcName) {
		double total = 0;
		for (SegmentModel seg: frameSet) {
			total =+ seg.getCalculation(calcName);
		}
		return total/frameSet.size();
	}
}
