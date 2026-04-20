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


/**
 * Analyzes cell-cell interactions between two channels.
 * Separates segments into interacting and non-interacting populations based on boundary overlap.
 * Generates two output datasets: one with interacting segments, one with non-interacting segments.
 */
public class Interactions extends OperationMethod {

	int threashold;

	/**
	 * Constructor initializing interaction analysis settings.
	 */
	public Interactions() {
		methodName = "Interactions";
		description = "Extracts Interacting Channels";
		numberOfCalculations = 1;
		threashold = 0;
	}

	/**
	 * Returns channel labels for the two input datasets being compared.
	 *
	 * @return array with labels for filter and analyzed data channels
	 */
	@Override
	public String[] getChannels() {
		return new String[] {
				"Filter: ",
				"Analyzed Data: "
		};
	}


	/**
	 * Separate input segments into two output DataSets based on interaction with first channel.
	 * Generates "Interaction" dataset for segments with boundary overlap and "No Interaction" dataset for others.
	 * Assigns semi-transparent green and red colors respectively.
	 *
	 * @param inputSet array of two input DataSets to compare for interactions
	 * @return array of two output DataSets: [Interaction, No Interaction]
	 */
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


	/**
	 * Determines if two segments share a common boundary point (interact).
	 * //TODO: threashold for partial overlap detection
	 *
	 * @param segA first Segment
	 * @param segB second Segment
	 * @param threashold distance threshold for interaction (currently unused)
	 * @return true if segments share any boundary point, false otherwise
	 */
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



	/**
	 * Create a new LinkSet with segments reassigned to a new DataSet.
	 *
	 * @param linkSet the source LinkSet to copy
	 * @param dataSet the target DataSet to assign to
	 * @return new LinkSet with segments reassigned
	 */
	//Determines
	LinkSet generateLinkSet(LinkSet linkSet, DataSet dataSet) {
		LinkSet newLinkSet = new LinkSet(dataSet);
		for (Segment seg: linkSet) {
			seg.setLinkSet(newLinkSet);
			newLinkSet.add(seg);
		}
		return newLinkSet;
	}



	/**
	 * //TODO: Determines if point list A lies entirely within pointListB
	 *
	 * @param segA first Segment
	 * @param segB second Segment
	 * @return true if segA is contained within segB (not yet implemented)
	 */
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


	/**
	 * Extract overlay ROI from segment using straight line perimeter.
	 * //TODO - different color for different overlays?
	 *
	 * @param segment the Segment to extract ROI for
	 * @return Roi using straightened perimeter
	 */
	@Override
	//TODO - different color for different overlays?
	Roi getOverlayParameter(Segment segment) {
		return getPolygonRoi(
				GeometricCalculations.straightPerimeter(
						segment.getInternalPerimeter())); //STRAIGHT LINE
	}


	/**
	 * Returns segment-level calculations for interaction analysis.
	 * Computes perimeter and area metrics.
	 *
	 * @return array of SegmentCalculation objects
	 */
	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
			new Perimeter(),
			new Area()
		};
	}

	/**
	 * Returns LinkSet-level calculations for interaction analysis.
	 *
	 * @return array containing AreaDistribution (Area Standard Deviation) calculation
	 */
	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
			new AreaDistribution()
		};
	}

	/**
	 * Returns LinkSet-level statistics for interaction analysis.
	 *
	 * @return array containing LinkSetMean statistic
	 */
	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return new LinkSetStatistic[] {
			new LinkSetMean()
		};
	}

	/**
	 * Returns FrameSet-level calculations for interaction analysis.
	 *
	 * @return array containing Confluency calculation
	 */
	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return new FrameSetCalculation[] {
			new Confluency()
		};
	}

	/**
	 * Returns FrameSet-level statistics for interaction analysis.
	 *
	 * @return array containing FrameSetMean statistic
	 */
	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
			new FrameSetMean()
		};
	}



}
