package analysisMethod;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import calculations.Area;
import calculations.AreaDistribution;
import calculations.Confluency;
import calculations.FrameSetCalculation;
import calculations.FrameSetMean;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetMean;
import calculations.LinkSetStatistic;
import calculations.Perimeter;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.gui.Roi;
import geometricTools.GeometricCalculations;


//Pulls out components of a channel from its interactions with another channel
public class Interactions extends OperationMethod {

	int threashold;
	
	public Interactions() {
		methodName = "Interactions";
		description = "Extracts Interacting Channels";
		numberOfCalculations = 1;
		threashold = 0;
	}

	@Override
	public String[] getChannels() {
		return new String[] {
				"Filter: ",
				"Analyzed Data: "
		};
	}

	
	@Override
	DataSet[] dataOperation(DataSet[] inputSet) {
		
		///Generate new output DataSets
		DataSet newSet1 =  new DataSet(inputSet[0].getWidth(), inputSet[0].getHeight(), inputSet[0].getSize());
		DataSet newSet2 =  new DataSet(inputSet[0].getWidth(), inputSet[0].getHeight(), inputSet[0].getSize());
		
		//Name dataSets
		newSet1.setDataSetName("Interaction");
		newSet2.setDataSetName("No Interaction");
		
		//Define overlays colors for each DataSet
		Color colorOne = new Color (0, 255, 0, 127);
		Color colorTwo = new Color (255, 0, 0, 127);
		
		//Set colors for DataSets
		//for (LinkSet linkSet: dataSets[0].getLinkSetList()) linkSet.setColor(colorOne);
		//for (LinkSet linkSet: dataSets[1].getLinkSetList()) linkSet.setColor(colorTwo);
		
		//Set up new LinkedList/FrameSet data holders
		ArrayList<LinkSet> newLinkSetList1 = new ArrayList<LinkSet>();
		ArrayList<LinkSet> newLinkSetList2 = new ArrayList<LinkSet>();
		FrameSet[] newFrameSetList1 = new FrameSet[dataSets[0].getFrameSetList().length];
		FrameSet[] newFrameSetList2 = new FrameSet[dataSets[1].getFrameSetList().length];
	
		ArrayList<LinkSet> overlapLinkSets = new ArrayList<LinkSet>();
		
		//Identify frames (segB) from Channel 2 that interact with Channel 1 (segA)
		for (int i = 0; i < newFrameSetList1.length; i ++) {
			//newFrameSetList1[i] = new FrameSet(i, newSet1);
			//newFrameSetList2[i] = new FrameSet(i, newSet2);
			for (int j = 0; j < dataSets[0].getFrameSet(i).size(); j++) {
				Segment segA = dataSets[0].getFrameSet(i).get(j);
				for (int k = 0; k < dataSets[1].getFrameSet(i).size(); k++) {
					Segment segB = dataSets[1].getFrameSet(i).get(k);
					
					//Test if interaction occurs
					boolean overlap = boundryOverlap(segA, segB, threashold);
					
					if (overlap && !overlapLinkSets.contains(segB.getLinkSet())) {
						overlapLinkSets.add(segB.getLinkSet());
					}				
				}
			}
		}
		
		LinkSet temp;
		int index1 = 0;
		int index2 = 0;
		
		//Redistribute LinkSet components
		for (LinkSet set: dataSets[1].getLinkSetList()) {
			if (overlapLinkSets.contains(set)) {
				temp = generateLinkSet(set, newSet1);
				temp.setColor(colorOne);
				temp.setName(index1++);
				newLinkSetList1.add(temp);
			}
			else {
				temp = generateLinkSet(set, newSet2);
				temp.setColor(colorTwo);
				temp.setName(index2++);
				newLinkSetList2.add(temp);
			}
		}
		
		
		//For dataSet 1, Generate appropriate FrameSet list to match LinkSet list
		for (int i = 0; i < newFrameSetList1.length; i ++) {
			newFrameSetList1[i] = new FrameSet(i, newSet1);
			for (LinkSet set: newLinkSetList1) {
				for (Segment segment: set) {
					if (segment.getFrame() == i) {
						newFrameSetList1[i].add(segment);
					}
				}
			}
		}
		
		//For dataSet 2, Generate appropriate FrameSet list to match LinkSet list
		for (int i = 0; i < newFrameSetList2.length; i ++) {
			newFrameSetList2[i] = new FrameSet(i, newSet2);
			for (LinkSet set: newLinkSetList2) {
				for (Segment segment: set) {
					if (segment.getFrame() == i) {
						newFrameSetList2[i].add(segment);
					}
				}
			}
		}
		
		
		//Add to DataSet1
		newSet1.setFrameSetList(newFrameSetList1);
		newSet1.setLinkSetList(newLinkSetList1);
		
		//Add to DataSet2
		newSet2.setFrameSetList(newFrameSetList2);
		newSet2.setLinkSetList(newLinkSetList2);
		
		//Add specific color to different dataSets?
		newSet1.setColor(colorOne);
		newSet2.setColor(colorTwo);
		
		//Return DataSet to analysis
		DataSet outputSets[] = {newSet1, newSet2};
		return outputSets;
	}
	

	//Determines if two point lists share a common point //TODO: threashold
	boolean boundryOverlap(Segment segA, Segment segB, int threashold) {
		
		Point[] A = GeometricCalculations.straightPerimeter(segA.getInternalPerimeter());
		Point[] B = GeometricCalculations.straightPerimeter(segB.getInternalPerimeter());
		
		for (Point pt1: A) {
			for (Point pt2: B) {
				if (pt1.x == pt2.x && pt1.y == pt2.y) return true;
			}
		}
		return false;
	}
		
	//TODO: Determines if a point list overlaps a certain pointList to some degree: 25% endocytosed, 50%, etc.
	
	
	
	//Determines
	LinkSet generateLinkSet(LinkSet linkSet, DataSet dataSet) {
		LinkSet newLinkSet = new LinkSet(dataSet);
		for (Segment seg: linkSet) {
			seg.setLinkSet(newLinkSet);	
			newLinkSet.add(seg);
		}
		return newLinkSet;	
	}
	
	
	
	//TODO: Determines if point list A lies entirely within pointListB
	boolean inside(Segment segA, Segment segB) {
		
		Point[] A = GeometricCalculations.straightPerimeter(segA.getInternalPerimeter());
		Point[] B = GeometricCalculations.straightPerimeter(segB.getInternalPerimeter());
		
		for (Point pt1: A) {
			for (Point pt2: B) {
				if (pt1.x == pt2.x && pt1.y == pt2.y) return true;
			}
		}
		return false;
	}
	
	
	@Override
	//TODO - different color for different overlays?
	Roi getOverlayParameter(Segment segment) {
		return getPolygonRoi(
				GeometricCalculations.straightPerimeter(
						segment.getInternalPerimeter())); //STRAIGHT LINE
	}
	
	
	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
			new Perimeter(),
			new Area()
		};
	}

	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
			new AreaDistribution()
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
			new Confluency()
		};
	}

	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
			new FrameSetMean()
		};
	}



}
