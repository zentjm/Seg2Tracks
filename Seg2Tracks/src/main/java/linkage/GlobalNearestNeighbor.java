package linkage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import dataStructure.LinkSet;
import dataStructure.Segment;

public class GlobalNearestNeighbor extends Linkage{

	int serialNumber;
	
	public GlobalNearestNeighbor() {
		name = "Global Nearest Neighbor";
		description = " "; //TODO: Fill out
	}
	
	//You have this: ArrayList<SegmentModel> [] DataSet
	public void run() {
		
		System.out.println("Running Global Nearest Neighbor");
		serialNumber = 0;
		
		//Generates a new LinkSet for all initial segmentations
		for (Segment segment: dataSet.getFrameSet(0)) { //TODO: Account for frames not started at the first frame
			
			//v2
			LinkSet linkSet = new LinkSet(segment, dataSet);
			segment.setLinkSet(linkSet);
			segment.getLinkSet().setStart(segment);
			segment.getLinkSet().setName(segment.getLinkSet().getDataSet().getLinkSetList().size() - 1);
			serialNumber ++;
			
			//TODO: this is v1
			//segment.setIsFirstFrame(true);
			//segment.setName(serialNumber);
			//serialNumber ++;
		}
		
		//runs through all segments:
		for (int i = 0; i < dataSet.getFrameSetList().length - 1; i++) {
			setLinkage(i);
		}
	}
	
	public void setLinkage(int step) {
		
		System.out.println("Set Linkage: " + step);
		
		//Generates the calculation matrix
		ArrayList<Segment> list1 = dataSet.getFrameSet(step);
		ArrayList<Segment> list2 = dataSet.getFrameSet(step + 1);
		
		//Matrix for holding entire linkset (value, i, j) and sort min to max 
		double[][] matrix = new double[list1.size() * list2.size()][3];
		
		System.out.println("List1 Size: " + list1.size());
		System.out.println("List2 Size: " + list2.size());
		System.out.println("Matrix Size: " + matrix.length);
		
		//Calculates a score between objects and inserts into a ranking matrix
		for (int i = 0; i < list1.size(); i ++) {
			for (int j = 0; j < list2.size(); j ++) {	
				matrix[(i *list2.size()) + j][0] = linkFormula(list1.get(i),list2.get(j)); //TODO: Matrix
				matrix[(i *list2.size()) + j][1] = i;
				matrix[(i *list2.size()) + j][2] = j;
			}
		}
	
		//Sorts the ranking matrix by lowest-to-highest score 
		Arrays.sort(matrix, Comparator.comparingDouble(x -> x[0]));
		
		Segment sm1;
		Segment sm2;
		
		//TODO: Cleanup method for orphans. 
		//Connects list1 and list2 segments
		for (int i = 0; i < matrix.length; i ++) {
			sm1 = list1.get((int) matrix[i][1]);
			sm2 = list2.get((int) matrix[i][2]);
			
			//XXX: For testing
			System.out.println("Running matrixFinder: " + i + ", Score:" + matrix[i][0]
					+ ", x =" + matrix[i][1] + ", y =" + matrix[i][2]);
			
			
			if (sm1.getIsLastFrame()) {	
				if (sm2.hasLinkSet()) {
					
					//Sm2 already matched - possible object loss
					//Add sm1 to orphan tails
					sm1.getLinkSet().setEnd(sm1); //Necessary? What use is this segmentModel pointer?
					sm1.getLinkSet().setOrphan(true);
					//TODO: Somehow add to an orphan loss set	
				}
				

				if (!sm2.hasLinkSet()) {
					//match them
					sm2.setLinkSet(sm1.getLinkSet());
					sm1.getLinkSet().add(sm2);
					sm1.setIsLastFrame(false);
				}
			}
			
			
			if (!sm1.getIsLastFrame()) {	
				if (sm2.hasLinkSet()) {
					//Both already matched - do nothing
				}
				
				if (!sm2.hasLinkSet()) {
					//Sm1 already matched - possible new object
					//Add Sm2 to orphan heads
					LinkSet linkSet = new LinkSet(sm2, dataSet);
					sm2.setLinkSet(linkSet);
					dataSet.addLinkSet(linkSet);
					sm2.getLinkSet().setStart(sm2);
					sm1.getLinkSet().addChild(sm2.getLinkSet()); //adds to the children set. //UTILIZE
					sm2.getLinkSet().setName(sm2.getLinkSet().getDataSet().getLinkSetList().size() - 1);
					serialNumber ++;
				}	
			}
		}
	}
	
	public double linkFormula(Segment one, Segment two) {
		
		//written out here for clarity
		int x1 = one.getCenterPoint().x;
		int y1 = one.getCenterPoint().y;
		int x2 = two.getCenterPoint().x;
		int y2 = two.getCenterPoint().y;
		
		//simple pythagorean distance (^2)
		return (Math.pow(y2-y1, 2) + Math.pow(x2-x1, 2));
	}
}
		

		
		

		 
		

