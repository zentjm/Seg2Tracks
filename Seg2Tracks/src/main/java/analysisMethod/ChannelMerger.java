package analysisMethod;

import java.awt.Color;
import java.util.ArrayList;

import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.gui.Roi;
import geometricTools.GeometricCalculations;

//Merges Multiple DataSets on a single Overlay image. 
public class ChannelMerger extends OperationMethod {

	public ChannelMerger() {
		methodName = "Overlay Channels";
		description = "Provides one overlay for two channels and but calculates objects separately";
		numberOfCalculations = 1;
	}
	
	@Override
	public String[] getChannels() {
		return new String[] {
				"DataSet 1: ",
				"DataSet 2: "
		};
	}


	@Override
	DataSet[] dataOperation(DataSet[] inputSet) {
		
		//Generate output dataSet
		DataSet mergedSet =  new DataSet(inputSet[0].getWidth(), inputSet[0].getHeight(), inputSet[0].getSize());
		
		//Modify DataSet colors
		Color colorOne = new Color (255, 0, 0, 127);
		Color colorTwo = new Color (0, 255, 0, 127);
		
		for (LinkSet linkSet: dataSets[0].getLinkSetList()) {
			linkSet.setColor(colorOne);
		}
		
		for (LinkSet linkSet: dataSets[1].getLinkSetList()) {
			linkSet.setColor(colorTwo);
		}
		
		//Merge DataSets
		ArrayList <LinkSet> mergedLinkSetList = new ArrayList<LinkSet>();
		mergedLinkSetList.addAll(dataSets[0].getLinkSetList());
		mergedLinkSetList.addAll(dataSets[1].getLinkSetList());
		
		FrameSet[] mergedFrameSetList = new FrameSet[dataSets[0].getFrameSetList().length];
		for (int i = 0; i < mergedFrameSetList.length; i ++) {
			mergedFrameSetList[i] = new FrameSet(i, mergedSet);
			mergedFrameSetList[i].addAll(dataSets[0].getFrameSet(i));
			mergedFrameSetList[i].addAll(dataSets[1].getFrameSet(i));
		}
		
		mergedSet.setLinkSetList(mergedLinkSetList);
		mergedSet.setFrameSetList(mergedFrameSetList);
	
		DataSet outputSets[] = {mergedSet};
		return outputSets;
	}
	
	//@Override
	Roi getOverlayParameter(Segment segment) {
		return getPolygonRoi(
				GeometricCalculations.straightPerimeter(
						segment.getInternalPerimeter())); //STRAIGHT LINE
	}


	@Override
	SegmentCalculation[] segmentCalculations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void defineSheets() {
		// TODO Auto-generated method stub
		
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
}
