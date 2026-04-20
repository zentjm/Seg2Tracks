package analysisMethod;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import org.apache.poi.ss.usermodel.Workbook;

import calculations.Area;
import calculations.Perimeter;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import pairedDataStructure.PairedList;
import pairedDataStructure.PairedSegment;
import dataStructure.Segment;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import geometricTools.GeometricCalculations;
import pairedSegmentCalculations.*;

/**
 * Compares two different segmentation algorithms (or control vs experimental conditions) by
 * aligning cell tracks and computing overlap metrics (Jaccard, Dice, Overlap indices, Coverage).
 * Key analysis tool for benchmarking segmentation accuracy against manual control segmentations.
 * Outputs metrics including "coverage" (overlap area / control area) as per published methodology.
 */
public class SegmentationComparer extends CompareMethod {


	/**
	 * Problem: Pairing.
	 *
	**/



	boolean sortByLink;
	Color seg1Color = Color.RED;
	Color seg2Color = Color.GREEN;
	Color overlapColor = Color.YELLOW;


	/**
	 * Constructor initializing segmentation comparison settings.
	 * Aligns dataset components to perform side-by-side segmentation validation.
	 */
	//Aligns dataset components to perform vis-a-vis comparisons.
	public SegmentationComparer() {
		methodName = "Segmentation Comparer";
		description = "Compares two different types of segmentation";
		numberOfCalculations = 3;
		mergedCalculation = false;
		sortByLink = true;
	}

	/**
	 * Returns channel labels for the control and experimental segmentations.
	 *
	 * @return array with labels for Control and Experimental DataSets
	 */
	@Override
	public String[] getChannels() {
		return new String[] {
				"Control DataSet: ",
				"Experimental DataSet: "
		};
	}

	/**
	 * Convert input segmentation datasets to paired data structure for comparison.
	 * Aligns LinkSets from control vs experimental based on spatial overlap.
	 *
	 * @param inputSets array of two DataSets to pair
	 */
	void calculatePairedData(DataSet[] inputSets) {
		if (sortByLink) sortLinkSet (inputSets[0].getLinkSetList(), inputSets[1].getLinkSetList());
		if (!sortByLink); //TODO: method for sorting by segment
	}

	/**
	 * Align LinkSets from two segmentations by finding best spatial overlaps.
	 * Creates PairedLists pairing control LinkSets with experimental LinkSets.
	 * Assigns red color to control, green to experimental for visualization.
	 *
	 * @param list1 ArrayList of control LinkSets (primary)
	 * @param list2 ArrayList of experimental LinkSets
	 */
	//Create PairedLinks #1, align LinkSets
	void sortLinkSet(ArrayList<LinkSet> list1, ArrayList<LinkSet> list2) {

		//System.out.println("list1 length: " + list1.size());
		//System.out.println("list2 length: " + list2.size());

		int index = 0;
		double score;
		boolean[] list2Match = new boolean[list2.size()];


		/*
		for (int i = 0; i < list1.size(); i++) {

			int minIndex  = -1;
			double minScore = -1;
			for (int j = 0; j < list2.size(); j++) {

		}
		*/




		//link each list1 with the BEST corresponding list2 or multiple list 2 //LIST 1 is primary
		for (int i = 0; i < list1.size(); i++) {
			//System.out.println("Matching list1 #" + i);

			int minIndex  = -1;
			double minScore = -1;
			for (int j = 0; j < list2.size(); j++) {
				// Score for how the two linkSets overlap - based on intersection area
				score = overlapScore(list1.get(i), list2.get(j));
				if (score > minScore) {
					minScore = score;
					minIndex = j;
				}
			}
			if (minIndex == -1) {
				// Control segmentation with no corresponding experimental match
				pairedData.add(new PairedList(list1.get(i), null, index++));
				//System.out.println("Added listt1, NULL");
			}
			else {
				// Both control and experimental segmentations present
				pairedData.add(new PairedList(list1.get(i), list2.get(minIndex), index++));
				list2Match[minIndex] = true;
				//System.out.println("Added listt1, list2");
			}
		}


		//System.out.println("Marker 1");

		/*
		for (PairedList pList: pairedData) {
			//System.out.println("PairedList: " + pList.getName() + "   " + pList.get(0).getSeg1() + "   " + pList.get(0).getSeg2());
		}


		//TESTING
		for (PairedList pList: pairedData) {
			//System.out.println("PairedList: " + pList.getName() + "   " +
					pList.get(0).getSeg1() + "   " +
					pList.get(0).getSeg2().getInternalPerimeter().length
				);
		}
		*/


		//Add unlinked List2
		for (int i = 0; i < list2.size(); i++) {
			if (list2Match[i] == false) {
				pairedData.add(new PairedList(null, list2.get(i), index ++));
			}
		}


		//System.out.println("Marker 1.5");


		for (int i = 0; i < list2.size(); i++) {
			if (list2Match[i] == false) {
				int minIndex  = -1;
				double minScore = 0;
				for (int j = 0; j < list1.size(); j++) {
					score = overlapScore(list1.get(j), list2.get(i));
					if (score > minScore) {
						minScore = score;
						minIndex = j;
					}
				}
				if (minIndex == -1) pairedData.add(new PairedList(null, list2.get(i), index ++));
				else pairedData.add(new PairedList(list1.get(minIndex), list2.get(i), index ++));
			}
		}



		//System.out.println("Marker 2");

		/*
		//TESTING
		for (PairedList pList: pairedData) {
			//System.out.println("PairedList: " + pList.getName() + "   " +
					pList.get(0).getSeg1().getInternalPerimeter().length + "   " +
					pList.get(0).getSeg2().getInternalPerimeter().length
				);
		}
		*/

		//Sets segments with no area to null
		for (PairedList pairedList : pairedData)  {
			//XXX: Copies from overlay. Needs extension to more than frame zero (TODO).
			for (PairedSegment pairedSegment : pairedList.getList()) {

				if (pairedSegment == null) continue;

				if (pairedSegment.getSeg1() != null) {
					ShapeRoi seg1Internal = new ShapeRoi (getPolygonRoi(pairedSegment.getSeg1().getInternalPerimeter()).getPolygon());
					if (seg1Internal.getContainedPoints().length < 1) pairedSegment.setSeg1(null);
				}

				if (pairedSegment.getSeg2() != null) {
					ShapeRoi seg2Internal = new ShapeRoi (getPolygonRoi(pairedSegment.getSeg2().getInternalPerimeter()).getPolygon());
					if (seg2Internal.getContainedPoints().length < 1) pairedSegment.setSeg2(null);
				}
			}
		}

		//System.out.println("Marker 3");

		/*
		//TESTING
		for (PairedList pList: pairedData) {
			//System.out.println("PairedList: " + pList.getName() + "   " +
					pList.get(0).getSeg1().getInternalPerimeter().length + "   " +
					pList.get(0).getSeg2().getInternalPerimeter().length
				);
		}
		*/

		//Removes double-nulled segments and empty links //TODO: Possible redundency.


		for (int i = 0; i < pairedData.size(); i++) {
			PairedList pairedList = pairedData.get(i);
			boolean allNull = true;
			for (int j = 0; j < pairedList.getList().length; j++) {
				PairedSegment pairedSegment = pairedList.get(j);
				if (pairedSegment == null) continue;
				if (pairedSegment.getSeg1() != null || pairedSegment.getSeg2() != null) {
					allNull = false;
				}
			}
			if (allNull == true) pairedData.remove(i);
		}



		for (PairedList pList: pairedData) {
			int num = 1000;
			if (pList == null) System.out.println("Plist is null");
			if (pList.get(0) == null) {
				for (int i = 0; i < 20; i++) {
					if (pList.get(i) == null) {
						//System.out.println("PairedList: " + pList.getName() + "   plist.get" + i + " is null");
						continue;
					}
					if (i < num) num = i;
					//System.out.println("PairedList: " + pList.getName() + "   plist.get" + i + " is NOT null, num =" + num);
					break;
				}
			}
			if (pList.get(0) != null) num = 0;

			if (pList.get(num).getSeg1() == null) System.out.println("PairedList: " + pList.getName()
					+ "   " + "SEG1 is Null " + "   " + pList.get(num).getSeg2());
			if (pList.get(num).getSeg2() == null) System.out.println("PairedList: " + pList.getName()
			+ "   " + pList.get(num).getSeg1() + "   " + "SEG2 is Null");
			else System.out.println("PairedList: " + pList.getName() + "   " + pList.get(num).getSeg1() + "   " + pList.get(num).getSeg2());
		}



		//System.out.println("Marker 4");
	}


	/**
	 * Compute average centerpoint of a LinkSet across all frames.
	 * //TODO: need to allow starting at later frame
	 * Used for spatial matching when LinkSets cannot be aligned by track continuity.
	 *
	 * @param set the LinkSet to compute average center for
	 * @return Point representing average centroid position
	 */
	//Returns the average centerpoint over a linkSet XXX: May be DEPRECIATED
	Point averageLinkSetCenterpoint(LinkSet set) { //TODO: need to allow starting at later frame
		int averageX = set.get(0).getCenterPoint().x;
		int averageY = set.get(0).getCenterPoint().y;
		for (int i = 1; i < set.size(); i++) {
			averageX += set.get(i).getCenterPoint().x;
			averageY += set.get(i).getCenterPoint().y;
		}
		return new Point(averageX/set.size(), averageY/set.size());
	}


	/**
	 * Compute overlap score between two LinkSets.
	 * Score is the sum of intersection pixel areas across frames where both LinkSets have segments.
	 * Higher score indicates better spatial alignment.
	 *
	 * @param set1 first LinkSet for comparison
	 * @param set2 second LinkSet for comparison
	 * @return overlap score (total intersection pixel count across matching frames)
	 */
	//returns a score for overlap of different sets
	double overlapScore (LinkSet set1, LinkSet set2) {
		double score = 0;
		for (int i = 0; i < set1.size(); i ++) {
			for (int j = 0; j < set2.size(); j ++) {
				if (set1.get(i).getFrame() == set2.get(j).getFrame()) { //TODO: this loop can be significantly improved
					ShapeRoi seg1Internal = new ShapeRoi (getPolygonRoi(set1.get(i).getInternalPerimeter()).getPolygon());
					ShapeRoi seg2Internal = new ShapeRoi (getPolygonRoi(set2.get(j).getInternalPerimeter()).getPolygon());
					seg1Internal.and(seg2Internal);
					// sum of the amount of overlap TODO: Normalize to size
					score += (double) seg1Internal.getContainedPoints().length;
				}
			}
		}
		//System.out.println("Score: " + score);
		return score;
	}


	/**
	 * Extract overlay ROIs for a paired segment pair.
	 * Generates separate ROIs for control-only, experimental-only, and intersection regions.
	 * Colors distinguish the three regions: red (control), green (experimental), yellow (overlap).
	 *
	 * @param pair the PairedSegment to extract ROIs for
	 * @return array of Roi objects representing different overlap regions
	 */
	@Override
	Roi[] getOverlayParameter(PairedSegment pair) {


		//System.out.println("Getting overlay parameter");

		ArrayList<Roi> roiArray = new ArrayList<Roi>();

		if (pair.getSeg1() != null && pair.getSeg2() != null) {

			ShapeRoi seg1Internal = new ShapeRoi (getPolygonRoi(pair.getSeg1().getInternalPerimeter()).getPolygon());
			ShapeRoi seg2Internal = new ShapeRoi (getPolygonRoi(pair.getSeg2().getInternalPerimeter()).getPolygon());

			ShapeRoi seg1Only = (ShapeRoi) seg1Internal.clone();
			ShapeRoi seg2Only = (ShapeRoi) seg2Internal.clone();

			//seg1Only.not(seg2Internal); //TODO: Other operators linked to different options
			seg1Only.setStrokeColor(seg1Color);
			seg1Only.setFillColor(seg1Color);
			seg1Only.setName("Set1");
			if (seg1Only.getContainedPoints().length > 0) {
				roiArray.add(seg1Only);
			}

			//seg2Only.not(seg1Internal); //TODO: Other operators linked to different options
			seg2Only.setStrokeColor(seg2Color);
			seg2Only.setFillColor(seg2Color);
			seg2Only.setName("Set2");
			if (seg2Only.getContainedPoints().length > 0) {
				roiArray.add(seg2Only);
			}

			// Intersection region (overlap of both segmentations)
			Roi intersection = seg1Internal.and(seg2Internal); //TODO: Other operators linked to different options
			intersection.setStrokeColor(overlapColor);
			intersection.setFillColor(overlapColor);
			intersection.setName("Intersection");
			if (intersection.getContainedPoints().length > 0) {
				roiArray.add(intersection);
			}
		}

		if (pair.getSeg1() != null && pair.getSeg2() == null) {

			Roi seg1Only = new ShapeRoi (getPolygonRoi(pair.getSeg1().getInternalPerimeter()).getPolygon());
			seg1Only.setStrokeColor(seg1Color);
			seg1Only.setFillColor(seg1Color);
			seg1Only.setName("Set1");
			if (seg1Only.getContainedPoints().length > 0) {
				roiArray.add(seg1Only);
			}
		}

		if (pair.getSeg1() == null && pair.getSeg2() != null) {

			Roi seg2Only = new ShapeRoi (getPolygonRoi(pair.getSeg2().getInternalPerimeter()).getPolygon());
			seg2Only.setStrokeColor(seg2Color);
			seg2Only.setFillColor(seg2Color);
			seg2Only.setName("Set2");
			if (seg2Only.getContainedPoints().length > 0) {
				roiArray.add(seg2Only);
			}
		}

		Roi[] array = new Roi[roiArray.size()];
		for (int i = 0; i < roiArray.size(); i ++) {
			array[i] = roiArray.get(i);
		}

		return array;
	}

	/**
	 * Returns paired segment calculation objects for segmentation comparison.
	 * Computes Jaccard Index, Dice Index, Overlap Coefficient, Coverage, and Size Ratio.
	 *
	 * @return array of PairedSegmentCalculation objects
	 */
	@Override
	PairedSegmentCalculation[] pairedSegmentCalculations() {
		//System.out.println("Paired Segment Calculations");
		return new PairedSegmentCalculation[] {
				new JaccardIndex(),
				new DiceIndex(),
				new OverlapIndex(),
				new Coverage(),
				new SizeRatio()
		};
	}

	/**
	 * Returns paired list calculation objects for segmentation comparison.
	 * Aggregates paired segment metrics across LinkSet tracks.
	 *
	 * @return array of PairedListCalculation objects
	 */
	@Override
	PairedListCalculation[] pairedListCalculations() {
		//System.out.println("Paired List Calculations");
		return new PairedListCalculation[] {
				new ListMean()
		};
	}




}
