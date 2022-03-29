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

public class SegmentationComparer extends CompareMethod {

	
	boolean sortByLink;
	Color seg1Color = Color.RED;
	Color seg2Color = Color.GREEN;
	Color overlapColor = Color.YELLOW;
	
	
	//Aligns dataset components to perform vis-a-vis comparisons.  
	public SegmentationComparer() {
		methodName = "Segmentation Comparer";
		description = "Compares two different types of segmentation";
		numberOfCalculations = 3;
		mergedCalculation = false;
		sortByLink = true;	
	}
	
	@Override
	public String[] getChannels() {
		return new String[] {
				"Matching DataSet: ",
				"Control DataSet: "
		};
	}

	void calculatePairedData(DataSet[] inputSets) {
		
		if (sortByLink) sortLinkSet (inputSets[0].getLinkSetList(), inputSets[1].getLinkSetList());
		if (!sortByLink); //TODO: method for sorting by segment
	}
	
	//Create PairedLinks #1, align LinkSets
	void sortLinkSet(ArrayList<LinkSet> list1, ArrayList<LinkSet> list2) {
	
		System.out.println("list1 length: " + list1.size());
		System.out.println("list2 length: " + list2.size());
		
		int index = 0;
		double score;
		boolean[] list2Match = new boolean[list2.size()];
		
		//link each list1 with a corresponding list2
		for (int i = 0; i < list1.size(); i++) {
			int minIndex  = -1;
			double minScore = 0;
			for (int j = 0; j < list2.size(); j++) {
				score = overlapScore(list1.get(i), list2.get(j));
				if (score > minScore) {
					minScore = score;
					minIndex = j;
				}
			}
			if (minIndex == -1) pairedData.add(new PairedList(list1.get(i), null, index++)); 
			else {
				pairedData.add(new PairedList(list1.get(i), list2.get(minIndex), index++));
				list2Match[minIndex] = true;
			}
		}
		
		//attempt to link each unlinked list2 with a corresponding list1
		
		for (int i = 0; i < list2.size(); i++) {
			if (list2Match[i] == false) {
				pairedData.add(new PairedList(null, list2.get(i), index ++));
			}
		}
	
		/*
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
		*/
	
		
		
		
		//Sets segments with no area to null
		for (PairedList pairedList : pairedData)  { 	
			//XXX: Copies from overlay. Needs extension to more than frame zero (TODO). 
			for (PairedSegment pairedSegment : pairedList.getList()) { 
		
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
		
		//Removes double-nulled segments and empty links //TODO: Possible redundency. 
		for (int i = 0; i < pairedData.size(); i++) {
			PairedList pairedList = pairedData.get(i);
			boolean allNull = true;
			for (int j = 0; j < pairedList.getList().length; j++) {
				PairedSegment pairedSegment = pairedList.get(j);
				if (pairedSegment.getSeg1() != null || pairedSegment.getSeg2() != null) {
					allNull = false;
				}
			}
			if (allNull == true) pairedData.remove(i);
		}
		
		for (PairedList pList: pairedData) {
			System.out.println("PairedList: " + pList.getName() + "   " + pList.get(0).getSeg1() + "   " + pList.get(0).getSeg2());
		}
	}
	
	
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
	
	
	//returns a score for overlap of different sets
	double overlapScore (LinkSet set1, LinkSet set2) {
		double score = 0;
		for (int i = 0; i < set1.size(); i ++) {
			for (int j = 0; j < set2.size(); j ++) {
				if (set1.get(i).getFrame() == set2.get(j).getFrame()) { //TODO: this loop can be significantly improved
					ShapeRoi seg1Internal = new ShapeRoi (getPolygonRoi(set1.get(i).getInternalPerimeter()).getPolygon());
					ShapeRoi seg2Internal = new ShapeRoi (getPolygonRoi(set2.get(j).getInternalPerimeter()).getPolygon());
					seg1Internal.and(seg2Internal);
					score += (double) seg1Internal.getContainedPoints().length;
				}
			}
		}	
		return score;
	}
	
	
	@Override
	Roi[] getOverlayParameter(PairedSegment pair) {
		
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

	@Override
	PairedSegmentCalculation[] pairedSegmentCalculations() {
		return new PairedSegmentCalculation[] {
				new JaccardIndex(),
				new DiceIndex(),
				new OverlapIndex(),
				new Coverage(),
				new SizeRatio()
		};
	}

	@Override
	PairedListCalculation[] pairedListCalculations() {
		return new PairedListCalculation[] {
				new ListMean()
		};
	}
	
	
	
	


	

	

	
	
	
	
	
	
	
	//EXTRA CODE
	/*
	 
	 
	 @Override //Aligns 
	DataSet[] calculateDataSet(DataSet[] inputSets) {
	
		//TESTING ---------------------------------------------------------------------
		System.out.println("inputSets[0] size: " +  inputSets[0].getSize());
		System.out.println("inputSets[1] size: " +  inputSets[1].getSize());
		System.out.println("inputSets[0] linkSetList size: " +  inputSets[0].getLinkSetList().size());
		System.out.println("inputSets[1] linkSetList size: " +  inputSets[1].getLinkSetList().size());
		for (int i = 0; i < inputSets[0].getLinkSetList().size(); i ++) {	
			System.out.println("InputSets[0], LinkSet" + i + ": " + 
					inputSets[0].getLinkSetList().get(i).getName() + "  seg frame:" + 
					inputSets[0].getLinkSetList().get(i).get(0).getInternalPerimeter());
		}
		for (int i = 0; i < inputSets[0].getFrameSetList().length; i ++) {
			System.out.println("InputSets[0], FrameSet" + i + ": " + 
					inputSets[0].getFrameSet(i).size());
		}
		//END TESTING ------------------------------------------------------------------
		
		
		//TODO: Currently makes assumption that Seg2Tracks will detect equal or greater # of objects. 
		

		
		/*Old sort
		ArrayList<LinkSet> sortedList = sortLinkSet(inputSets[0].getLinkSetList(), inputSets[1].getLinkSetList());
		inputSets[1].setLinkSetList(sortedList);
		return inputSets;
		
		
		
		
		
	
		return inputSets;
	}
	
	 
	 
	 
	 
	 
	 
	 
	 
	 
	//Segment calculation
	void calculationHook() {
		int newObjects = 0;
		String [] calculationNames = {"Jaccard Index", "Dice Coefficient", "Overlap Coefficient"};
		
		
		for (int i = 0; i < outputDataSets[0].getFrameSetList().length; i++) {  //FrameSets in dataSet
			for (int j = 0; j < outputDataSets[0].getFrameSet(i).size(); j++) { //Segments in FrameSet
				SegmentModel segmentOne = outputDataSets[0].getFrameSet(i).get(j);
				SegmentModel segmentTwo = outputDataSets[1].getLinkSet(segmentOne.getLinkSet().getName()).get(i); //matching segment
				
				double [] calculations = multiCalculations(segmentOne, segmentTwo);
				
				System.out.println("SegmentOne LinkSet: " + segmentOne.getLinkSet().getDataSet().getName());
				System.out.println("SegmentTwo LinkSet: " + segmentTwo.getLinkSet().getDataSet().getName());
				
				String dataSetNames = segmentOne.getLinkSet().getDataSet().getName() + " & " + 
						segmentTwo.getLinkSet().getDataSet().getName();
					
				for (int k = 0; k < calculations.length; k ++) {
					Object[] segmentCalculations = new Object[] {
						outputDataSets[i].getName(),
						methodName,
						segmentOne.getLinkSet().getName(),
						segmentOne.getFrame() + 1,
						calculationNames[k],
						calculations[k],
					};
					workbook.addLine(segmentModelSheet, segmentCalculations); //TODO: dataSet name
				}
			}		
		}
	}
	*/
	
	
	
	
	
}
