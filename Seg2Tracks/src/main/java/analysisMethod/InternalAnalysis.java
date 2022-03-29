package analysisMethod;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.Random;

import org.apache.poi.ss.usermodel.Workbook;

import calculations.*;
import dataStructure.DataSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import geometricTools.GeometricCalculations;

//Gives overlay and data on the internal perimeter of a single dataSet
public class InternalAnalysis extends OperationMethod {

	public InternalAnalysis() {
		methodName = "Internal Segmentation Data";
		description = "General and abstracted morphological-intensity data of the internal segmentation";
	}
	
	@Override
	public String[] getChannels() {
		return new String[] { "DataSet:"};
	}

	@Override
	Roi getOverlayParameter(Segment segment)  {
		segment.setColor(Color.YELLOW);
		return getPolygonRoi(segment.getInternalPerimeter()); //STRAIGHT LINE
	}

	@Override
	DataSet[] dataOperation(DataSet[] inputSets) {
		return inputSets;
	}
	
	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
			new Location_X(),
			new Location_Y(),
			new Perimeter(),
			new Area(),
			new IntegratedIntensity(),
			new MeanIntensity(),
			new Circularity(),
		};
	}

	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
			//new AreaDistribution() //TODO: finish
		};
	}

	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return new LinkSetStatistic[] {
			new LinkSetMean()
		};	
	}

	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return new FrameSetCalculation[] {
			//new Confluency() //TODO: finish
		};
	}

	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
			new FrameSetMean()
		};
	}
}
