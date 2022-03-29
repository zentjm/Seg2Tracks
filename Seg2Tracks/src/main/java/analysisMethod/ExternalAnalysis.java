package analysisMethod;

import java.awt.Color;

import calculations.ExternalArea;
import calculations.ExternalPerimeter;
import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import ij.gui.Roi;

public class ExternalAnalysis extends OperationMethod {

	public ExternalAnalysis() {
		methodName = "External Segmentation Data";
		description = "General and abstracted morphological-intensity data of the external segmentation";
	}

	@Override
	Roi getOverlayParameter(Segment segment) {
		segment.setColor(Color.YELLOW);
		return getPolygonRoi(segment.getExternalPerimeter()); //STRAIGHT LINE
	}

	@Override
	DataSet[] dataOperation(DataSet[] inputSets) {
		return inputSets;
	}
	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
			new ExternalPerimeter(),
			new ExternalArea()
		};
	}

	@Override
	LinkSetCalculation[] linkSetCalculations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	LinkSetStatistic[] linkSetStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	FrameSetCalculation[] frameSetCalculations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	FrameSetStatistic[] frameSetStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getChannels() {
		return new String[] { "DataSet:"};
	}

}
