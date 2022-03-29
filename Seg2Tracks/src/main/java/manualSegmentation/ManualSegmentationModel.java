package manualSegmentation;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.process.FloatPolygon;

public class ManualSegmentationModel {

	/*STATES
	 * 
	 * States:
	 * 1. First Frame
	 * 2. Next Frame
	 * 3. Last Frame
	 */
	
	/*PANELS
	 * 
	 * Main Panel: Goes to other panels
	 * - [Add New Object]
	 * - [Modify Objects]
	 * - [End Segmentation]
	 * - [Save Progress]
	 * - [Show Object Contraction]
	 * 
	 * Add New Object
	 * - Next Frame
	 * - P
	 * - End Frame
	 * 
	 * 
	 * Modify Objects
	 * - Delete Object
	 * - Merge Objects --> Merge Menu (Draw line between merging objects)
	 * - Split Object  --> Split Menu (Draw line across object to split)
	 * - Break Linkage --> Break Menu (
	 * - Merge Linkage --> Merge Menu
	 * 
	 * End Segmentation
	 * - Are you sure?
	 * 
	 * Save Progress
	 */
	
	/*METHODS
	 * 
	 * Basic Methods:
	 * - Start object
	 * - Move to next frame
	 * - Undo/go to previous frame
	 * - End object
	 * 
	 * Modification Methods
	 * - Restore selection
	 * 
	 * IO methods:
	 * - Load (part of initialization)
	 * - Save
	 */
	
	DataSet dataSet;
	LinkSet linkSet;
	int runType;
	ManualSegmentationController controller;
	
	//TODO: Maybe move whole method to controller. 
	public ManualSegmentationModel(ManualSegmentationController controller, DataSet dataSet, int runType) {
		this.controller = controller;
		this.dataSet = dataSet;
		this.runType = runType;
	}
		

	//Resets the linkset for adding new
	public void newLinkSet() {
		linkSet = new LinkSet(dataSet);
		linkSet.setName(linkSet.getDataSet().getLinkSetList().size() - 1);
		linkSet.setColor(new Color(255,0,0));
	}
	
	
	public void addSegment(FloatPolygon floatPolygon, int frame) {
		
		//Convert floats to int points
		Point[] perimeter = new Point[floatPolygon.npoints];	
		for (int i = 0; i < floatPolygon.npoints; i ++) {
			perimeter[i] = new Point((int) floatPolygon.xpoints[i], (int) floatPolygon.ypoints[i]);
		}
	
		//TODO: Guassian-blur method for identifying the centerpoint
		
		//Construct geometric centerpoint
		Rectangle rect = floatPolygon.getBounds();
		Point centerpoint = new Point (rect.x + (rect.width / 2), rect.y + (rect.height / 2));
		System.out.println("CenterPoint is... x:" + centerpoint.x + "   y:"  + centerpoint.y);
		
		//Construct the SegmentModel
		Segment segment = new Segment(frame, centerpoint);
		
		
		//TODO: MAJOR ISSUE Unclear how one object can hold two segmentations. 
		if (runType == 0) segment.setExternalPerimeter(perimeter);
		if (runType == 1) segment.setInternalPerimeter(perimeter); //DEPRECIATED, cannot manually select internal perimeter
		
		//Add SegmentModel to LinkSet and FrameSet
		linkSet.addSegment(segment);
		segment.setLinkSet(linkSet);
		dataSet.getFrameSet(frame).add(segment);
	}
	
	
	public void newObject(Segment segment) {

	}
	

	
	
	
	//TODO wraps up object and adds frameSet references 
	public void finishObject() {
		//TODO: Add centerpoints
		//TODO: Add to frameSet
	}
	
	//TODO: Removes last segment and iterates frame back
	public void undo() {
		
	}
	
	//TODO: Returns last selection for restoration of selection
	public void getLastSelection() {
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	

}
