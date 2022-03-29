package manualSegmentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import externalSegmentation.ExternalSegmentation;
import geometricTools.GeometricCalculations;
import gui.OperationController;
import gui.Seg2TracksController;
import gui.Seg2TracksModel;
import identification.Identification;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

public class ManualSegmentationController {


	//ImageJ ij = new ImageJ(); //XXX: For testing only
	
	OperationController controller;
	int runType;
	ImagePlus imagePlus;
	ImageProcessor processor;
	ManualSegmentationModel model;
	ManualSegmentationPanel panel;
	ManualSegmentationWindow window;
	Overlay overlay;
	RoiManager manager;
	DataSet dataSet;
	LinkSet linkSet;
	Boolean mouseListenerActive;
	
	int frame = 1;
	int startFrame = -1;

	Roi restoreRoi;
	
	//Roi roi;
	Color color;
	Color altColor;
	
	//Holding current segments
	Segment segment;
	Segment previousSegment;
	
	//Determines whether preview or editable
	boolean canEdit;

	public ManualSegmentationController(int runType, OperationController controller, boolean canEdit) {
		this.controller = controller;
		this.runType = runType;
		this.canEdit = canEdit;
		imagePlus = new ImagePlus("ManualSegmentation", IJ.openVirtual(controller.getInputFilePath()).getImageStack());
		color = new Color(0, 255, 0);
		altColor = new Color (255, 0, 0);
		initialize();
	}
		
	public void initialize() { 
		//Create overlay
		overlay = new Overlay();
		overlay.drawNames(true);
		overlay.drawLabels(true);
		overlay.setLabelColor(Color.BLACK);
		overlay.drawBackgrounds(true);
		overlay.setLabelFont(new Font ("TimesRoman", Font.BOLD, 15));
		imagePlus.setOverlay(overlay);
		
		//Open RoiManager
		manager = RoiManager.getInstance();
		
		//Imageprocessor
		processor = imagePlus.getProcessor();
		
		//Holding SelectedRoi
		//segmentRoi = new SegmentRoi();
		//segmentRoiArray = new ArrayList<SegmentRoi>();
	}
	
	
	//Is data
	public boolean isDataLoaded () {
		if (dataSet == null) return false;
		if (dataSet.getLinkSetList().size() > 0) return true;
		return false; 
	}
	
	
	//Initializes an empty dataSet
	public void runDataSet() {
		dataSet = new DataSet(imagePlus.getWidth(), imagePlus.getHeight(), imagePlus.getNSlices());
		for(int i = 0; i < dataSet.getFrameSetList().length; i ++) {
			dataSet.getFrameSetList()[i] = new FrameSet(i, dataSet);
		}
		run();
		dataSet.setExternalSegmentationExists(true);
	}
	
	//Loads a dataSet
	public void runDataSet(DataSet dataSet) {
		//Check for appropriate dimensions
		if (dataSet.getWidth() != imagePlus.getWidth()) {
			controller.errorMessage("Width of input image does not match loaded data file");
			return;
		}
		if (dataSet.getHeight() != imagePlus.getHeight()) {
			controller.errorMessage("Height of input image does not match loaded data file");
			return;
		}
		if (dataSet.getSize() != imagePlus.getNSlices()) {
			controller.errorMessage("Size of input image does not match loaded data file");
			return;
		}
		
		Segment segment;
		//SelectedRoi segmentRoi = new SelectedRoi();
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			System.out.println("Frame size is: " + dataSet.getFrameSet(i).size());
			for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
				//segmentRoi = new SegmentRoi();
				segment = dataSet.getFrameSet(i).get(j);
				if (runType == 0) segment.setRoi(getPolygonRoi(segment.getExternalPerimeter()));
				if (runType == 1) segment.setRoi(getPolygonRoi(segment.getInternalPerimeter()));
				
				segment.getRoi().setStrokeColor(color);
				segment.getRoi().setPosition(segment.getFrame() + 1);
				segment.getRoi().setStrokeWidth(2);
				overlay.add(segment.getRoi());	
			}
		}
		this.dataSet = dataSet;
		run();
	}
		
	
	public void run() {
		
	
		//Freeze Seg2Tracks Menu
		controller.setViewActive(false);
		
		//Load inputs
		//model = new ManualSegmentationModel(this, dataSet, runType); //TODO: modify to allow loading. 
		panel = new ManualSegmentationPanel(this, imagePlus, canEdit);
		
		//Create window panel 
		window = new ManualSegmentationWindow(imagePlus, this);
		imagePlus.setWindow(window);

		//Create views
		imagePlus.show();
		panel.createView();
		
		//Modify locations
		window.setLocation(window.getLocation().x, window.getLocation().y - (int) panel.getBounds().getHeight());
		panel.setLocation(window.getLocation().x + (int) (0.5 * window.getBounds().getWidth() - 0.5 * panel.getBounds().getWidth()),
				window.getLocation().y + (int) window.getBounds().getHeight() + (int) (0.5 * panel.getBounds().getHeight()));
		
		//Start with Main Menu
		mainMenu();
	}
	
	
	/*
	 * TODO: This is the same method as in the the Analysis Method "Generate X
	 * Parameter" so utilize this. 
	 */
	public PolygonRoi getPolygonRoi(Point[] pointList) {
		
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		
		for (int i = 0; i < pointList.length; i ++) {
			
			//System.out.println("Point x: " + pointList[i].x);
			//System.out.println("Point y: " + pointList[i].y);
			
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, Roi.POLYLINE);
	}
	
	//returns to main menu
	public void mainMenu() {
		if (mouseListenerActive != null && mouseListenerActive == true) {
			mouseListenerActive = false;
		}
		
		overlay.selectable(false);
		panel.setMainPanel(); //TODO: Set booleans on LOADED Files
		IJ.setTool("hand");
	}
	
	public void newSegmentation() {
		panel.setSegmentPanel();
		panel.stateObject(true, false, false, false, true);
	}
	
	//TODO: Convert button to a cancel button
	public void startObject() {
		segment = null;
		linkSet = new LinkSet(dataSet);
		//dataSet.addLinkSet(linkSet); //TODO: autoadding to dataSet maybe not such a good idea
		window.setUserInput(false);
		//if (manager == null) manager = new RoiManager(true);
		IJ.setTool("polygon");
		frame = imagePlus.getCurrentSlice();
		startFrame = frame;
		panel.stateObject(false, true, frame == startFrame, 
				frame == imagePlus.getImageStackSize(), false);

		System.out.println("Start Frame: " + startFrame + "   Frame: " + frame);
		System.out.println("CurrentSlice: " + frame + "   StackSize: " + imagePlus.getImageStackSize());
	}
	
	public void previousFrame() {
		//TODO: reset the restoreSelection
	}
	
	public void nextFrame() {
		if (imagePlus.getRoi() == null) {
			panel.dialogAlert("Must select overlay for this frame");
			return;
		}
		if (segment != null) { //all except for start
			previousSegment = segment;
		}
		segment = getSegment(frame - 1, imagePlus.getRoi());
		//segment.setRoi(imagePlus.getRoi());
		segment.getRoi().setPosition(frame);
		//manager.add(imagePlus, segment.getRoi(), frame - 1);
		//linkSet.addSegment(segment);
		segment.setLinkSet(linkSet);
		linkSet.add(segment);
		System.out.println("NEXT FRAME. LinkSet is:" + linkSet.size());
		System.out.println("current: " + imagePlus.getCurrentSlice() + "   sliceStace: " + imagePlus.getImageStackSize());
		if (imagePlus.getCurrentSlice() != imagePlus.getImageStackSize()) {
			frame++;
			imagePlus.setSlice(frame);
		}
		panel.stateObject(false, true, frame == startFrame, 
				imagePlus.getCurrentSlice() == imagePlus.getImageStackSize(), false);
	}
	
	public void restoreSelection() {
		imagePlus.setRoi(previousSegment.getRoi());
	}

	public void endObject() {
		if (imagePlus.getRoi() == null) {
			panel.dialogAlert("Must select overlay for this frame"); //TODO allow quit or return
			return;
		}
		segment = getSegment(frame - 1, imagePlus.getRoi());
		segment.getRoi().setPosition(frame);
		segment.setLinkSet(linkSet);
		linkSet.add(segment);
		linkSet.setName(linkSet.getDataSet().getLinkSetList().size() - 1);
		panel.stateObject(true, false, false, false, true);
		System.out.println("LinkSet length is: " + linkSet.size());
		
		int g = 0;
		for (Segment seg: linkSet) {
			g++;
			dataSet.getFrameSet(seg.getFrame()).add(seg);
			System.out.println("Manual Segment " + g);
			seg.getRoi().setStrokeColor(color);
			seg.getRoi().setStrokeWidth(2); //TODO: Settable stroke width
			overlay.add(seg.getRoi()); //TODO: Better naming scheme?
		}
		
		imagePlus.setOverlay(overlay);
		imagePlus.setSlice(startFrame);
		
		//Reset
		IJ.run(imagePlus, "Select None", "");
		IJ.setTool("hand");
		//manager.removeAll();
		window.setUserInput(true);
		controller.setModifyData(runType, dataSet);
		controller.autosave();
	}
	
	public void modifyMenu() {
		panel.setModificationPanel();
		IJ.setTool("hand");
		overlay.selectable(false);
		
		//TODO: ability to switch this off
		if (mouseListenerActive == null) {
			mouseListenerActive = true;
			imagePlus.getCanvas().addMouseListener(
				new MouseAdapter () {
					public void mousePressed(MouseEvent event) {
						if (mouseListenerActive) {
							selectObject(imagePlus.getCanvas().offScreenX(event.getX()),
									imagePlus.getCanvas().offScreenY(event.getY()));
						}
					}
				}
			);
		}
		else mouseListenerActive = true;
	}
	
	
	 //for (Map.Entry<String,String> entry : gfg.entrySet())
	
	
	//Selects and roi
	//First click selects and highlights, second click deselects. 
	public void selectObject(int x, int y) {
		System.out.println("Clicked:" + x + " " + y);
		for (Segment seg: dataSet.getFrameSet(imagePlus.getCurrentSlice() - 1)) {
			if (seg.getRoi().contains(x, y)) {
				//collect entire segmentation linklist;
				LinkSet linkSet = seg.getLinkSet();
				for (Segment s: linkSet) {
					s.setRoiSelected(!s.getRoiSelected());
					if (s.getRoiSelected()) s.getRoi().setStrokeColor(altColor);
					if (!s.getRoiSelected()) s.getRoi().setStrokeColor(color);
				}
				System.out.println("Selection/deselection complete");
			}
		}
		imagePlus.getCanvas().repaintOverlay();
	}
			
	
	public void deleteObject(LinkSet linkSet) {
		for (Segment seg: linkSet) {
			seg.getLinkSet().getDataSet().getFrameSet(seg.getFrame()).remove(seg);
			overlay.remove(seg.getRoi());
		}
		linkSet.getDataSet().getLinkSetList().remove(linkSet);
	}
	
	
	public void deleteObject(Segment segment) {
		LinkSet linkSet = segment.getLinkSet();
		deleteObject(linkSet);
	}
	

	public void deleteObject() {
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			Segment segment;
			for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
				//segmentRoi = new SegmentRoi();
				segment = dataSet.getFrameSet(i).get(j);
				if (segment.getRoiSelected()) {
					LinkSet linkSet = segment.getLinkSet();
					for (Segment seg: linkSet) {
						seg.getLinkSet().getDataSet().getFrameSet(seg.getFrame()).remove(seg);
						overlay.remove(seg.getRoi());
						//s.getRoi().setImage(null);
					}
					linkSet.getDataSet().getLinkSetList().remove(linkSet);
					j--;
				}
			}	
		}
		imagePlus.getCanvas().repaintOverlay();
	}
	
	
	
	
	
	
	public void mergeObject() {
	
		int startSet1 = Integer.MAX_VALUE;
		int startSet2 = Integer.MAX_VALUE;
		int endSet1 = 0;
		int endSet2 = 0;
		
		LinkSet[] mergeSet = new LinkSet[2];
		
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			Segment segment;
			for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
				segment = dataSet.getFrameSet(i).get(j); //TODO: why is variable not method specific?
				if (segment.getRoiSelected()) {
					LinkSet linkSet = segment.getLinkSet();
						
					if (mergeSet[0] != null && mergeSet[0].getName() == linkSet.getName()) {
						//if (segment.getFrame() < startSet1) startSet1 = segment.getFrame();
						if (segment.getFrame() > endSet1) endSet1 = segment.getFrame();
						continue;
					}
					if (mergeSet[1] != null && mergeSet[1].getName() == linkSet.getName()) {
						//if (segment.getFrame() < startSet1) startSet1 = segment.getFrame();
						if (segment.getFrame() > endSet1) endSet1 = segment.getFrame();
						continue;	
					}
			
					if (mergeSet[0] == null ) {
						mergeSet[0] = linkSet;
						startSet1 = segment.getFrame();
					}
					if (mergeSet[0] != null &&  mergeSet[1] == null  && mergeSet[0].getName() != linkSet.getName()) {
						mergeSet[1] = linkSet;
						startSet2 = segment.getFrame();
					}
					
					if (mergeSet[0] != null && mergeSet[1] != null  
							&& mergeSet[0].getName() != linkSet.getName()
							&& mergeSet[1].getName() != linkSet.getName()
							) {
						System.out.println("Only two objects can be merged at a time");
						return;
					}
				}
			}
		}
		
		if(mergeSet[0] != null) System.out.println("mergedLinkSet[0] = " + mergeSet[0].getName());
		else { System.out.println("mergedLinkSet[0] is null");}
		if(mergeSet[1] != null) System.out.println("mergedLinkSet[1] = " + mergeSet[1].getName());
		else { System.out.println("mergedLinkSet[1] is null");}
		
		
		System.out.println("startSet1: " + startSet1 + "     endSet1: " + endSet1);
		System.out.println("startSet2: " + startSet2 + "     endSet2: " + endSet2);

		if (startSet1 > endSet2 || startSet2 > endSet1) {
			System.out.println("No frame overlap between selections");
			return;
		}
		
		int start = startSet1 < startSet2 ? startSet1 : startSet2;
		int end = endSet1 > endSet2 ? endSet1 : endSet2;
	
		
		LinkSet newLink = new LinkSet(dataSet);
		//Checks that there is some overlap
		boolean overlap = false;
		for (int i = start; i < end + 1; i ++) {
			
			Segment seg1 = null;
			Segment seg2 = null;
			Segment newSeg = null;
			//Get segments
			for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
				Segment segment = dataSet.getFrameSet(i).get(j);
				if (segment.getLinkSet() == mergeSet[0]) seg1 = segment; //TODO: should be able to access a certain frame from a linkset
				if (segment.getLinkSet() == mergeSet[1]) seg2 = segment;
			}
			if (seg1 == null && seg2 == null) continue; //TODO: shouldnt happen here
			if (seg1 != null && seg2 == null) {
				newSeg = new Segment(seg1);
				newLink.add(newSeg);
				continue;
			}
			if (seg1 == null && seg2 != null) {
				newSeg = new Segment(seg2);
				newLink.add(newSeg);
				continue;
			}
			
			//TODO: check if overlapping. 
			Point[] A = GeometricCalculations.straightPerimeter(seg1.getExternalPerimeter());
			Point[] B = GeometricCalculations.straightPerimeter(seg2.getExternalPerimeter());
			overlap = GeometricCalculations.boundryOverlap(A, B, 0); //Threshold set for any overlap at all
			if (!overlap) {
				System.out.println("Selected objects are not overlapping");
				return;
			}
			
			newSeg = mergeSegments(seg1, seg2);
			newLink.add(newSeg);
		}	
		
		//remove old objects
		//newLink.setName(dataSet.getLinkSetList().size());
		deleteObject(mergeSet[0]);
		deleteObject(mergeSet[1]);
		dataSet.addLinkSet(newLink);
		for (Segment seg: newLink) {
			dataSet.getFrameSet(seg.getFrame()).add(seg);
			seg.setLinkSet(newLink);
			seg.setRoi(getPolygonRoi(seg.getExternalPerimeter()));
			seg.getRoi().setStrokeColor(color);
			seg.getRoi().setStrokeWidth(2); //TODO: Settable stroke width
			seg.setRoiSelected(false);
			seg.getRoi().setPosition(seg.getFrame() + 1);
			overlay.add(seg.getRoi()); //TODO: Better naming scheme?
		}
		imagePlus.setOverlay(overlay);
		controller.setModifyData(0, dataSet); //0 means external segmentation
		imagePlus.getCanvas().repaintOverlay();
		
		for (int i = 0; i < newLink.size(); i++) {
			if (newLink.get(i) == null) {
				System.out.println("newLink @ " + i + " is null");
			}
		}
		
	}
		
	
	
	private Segment mergeSegments(Segment segA, Segment segB) {
		
		Point[] A = GeometricCalculations.straightPerimeter(segA.getExternalPerimeter());
		Point[] B = GeometricCalculations.straightPerimeter(segB.getExternalPerimeter());
		
		//Find points of contact
		ArrayList<Point> contacts = new ArrayList<Point>();
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < B.length; j++) {
				if (A[i].x == B[j].x && A[i].y == B[j].y) {
					contacts.add(A[i]);
					indexes.add(i);
				}
			}
		}
		
		//find start and end points for intersections. 
		int maxLength = 0;
		Point startIntersect = null;
		Point endIntersect = null;
		int start = 0;
		int end = 0;
		for (int i = 1; i < indexes.size(); i++) {
			if (indexes.get(i) - indexes.get(i-1) > maxLength) {
				maxLength = indexes.get(i) - indexes.get(i-1);
				start = indexes.get(i -1);
				end = indexes.get(i);
				startIntersect = contacts.get(i-1);
				endIntersect = contacts.get(i);
			}
		}
	
		if (indexes.get(0) + (A.length + 1 - indexes.get(indexes.size()-1)) > maxLength) {
			maxLength = indexes.get(0) + (A.length + 1 - indexes.get(indexes.size()-1));
			start = indexes.get(indexes.size()-1);
			end = indexes.get(0);
			startIntersect = contacts.get(contacts.size()-1);
			endIntersect = contacts.get(0);
		}
		
		//Generate new segment for point A
		ArrayList<Point> listA1 = new ArrayList<Point>();
		int n = start;
		while (n + 1 != end) { //adds start and end
			if (n > A.length -1) n = n - A.length + 1;
			listA1.add(A[n]);
			n++;
		}
	
		//determine if start for B is before or after end
		int startIndexB = -1;
		int endIndexB = -1;
		for (int i = 0; i < B.length; i++) {
			if (B[i].x == startIntersect.x && B[i].y == startIntersect.y) endIndexB = i;
			if (B[i].x == endIntersect.x && B[i].y == endIntersect.y) startIndexB = i;
		}

	
		
		//Adds start to end
		ArrayList<Point> listB1 = (ArrayList<Point>) listA1.clone();
		n = startIndexB + 1;
		while (n != endIndexB) { 
			if (n > B.length -1) n = n - B.length;
			listB1.add(B[n]);
			n++;
		}
		
		//Adds end to start
		ArrayList<Point> listB2 = (ArrayList<Point>) listA1.clone();
		n = startIndexB - 1;
		while (n != endIndexB) {
			if (n < 0) n = n + B.length;
			listB2.add(B[n]);
			n--;
		}
		
		//Chooses new segment perimeter by which has largest area //TODO: not the cleanest. 
		Point[] B1 = new Point[listB1.size()];
		for (int i = 0; i < B1.length; i++) {
			B1[i] = listB1.get(i);
		}
		
		Point[] B2 = new Point[listB2.size()];
		for (int i = 0; i < B2.length; i++) {
			B2[i] = listB2.get(i);
		}
		
		int lengthB1 = GeometricCalculations.getAreaByRoi(B1).length;
		int lengthB2 = GeometricCalculations.getAreaByRoi(B2).length;
		Point[] C = lengthB1 > lengthB2 ? B1 : B2;
		
		//Determines new centerpoint by which of the previous was the biggest object //TODO: better method for selection
		int sizeA = GeometricCalculations.getAreaByRoi(A).length;
		int sizeB = GeometricCalculations.getAreaByRoi(B).length;
		Point centerPoint = sizeA > sizeB ? segA.getCenterPoint(): segB.getCenterPoint();	
		
		//Return new segment
		Segment segC = new Segment(segA);
		segC.setManuallyEdited(true);
		segC.setCenterPoint(centerPoint);
		segC.setExternalPerimeter(C);
		return segC;
	}
	
		
	
	public void splitObject() {
		
	}
	
	public void linkObject() {
		
	}
	
	public void unLinkObject() {
		
	}
	
	private Segment getSegment (int frame, Roi roi) {
		
		//get a float from this
		FloatPolygon floatPolygon = roi.getFloatPolygon("close");
		
		//Get geometric centerpoint
		Rectangle rect = floatPolygon.getBounds();
		Point centerpoint = new Point (rect.x + (rect.width / 2), rect.y + (rect.height / 2));
		
		//TODO: fix centerpoint. 
		System.out.println("CenterPoint is... x:" + centerpoint.x + "   y:"  + centerpoint.y);
		
		//Create new centerpoint
		Segment segment = new Segment(frame, centerpoint);
		Point[] perimeter = new Point[floatPolygon.npoints];	
		for (int i = 0; i < floatPolygon.npoints; i ++) {
			perimeter[i] = new Point((int) floatPolygon.xpoints[i], (int) floatPolygon.ypoints[i]);
		}
		
		//Fill points of perimeter
		Point[] adjustedPerimeter = GeometricCalculations.straightPerimeter(perimeter);
		
		segment.setExternalPerimeter(adjustedPerimeter);
		segment.setRoi(getPolygonRoi(segment.getExternalPerimeter()));
		return segment;
	}
	
	/*
	private Point[] getPerimeter(Roi roi) {
		FloatPolygon floatPolygon = roi.getFloatPolygon("close");
		Point[] perimeter = new Point[floatPolygon.npoints];	
		for (int i = 0; i < floatPolygon.npoints; i ++) {
			perimeter[i] = new Point((int) floatPolygon.xpoints[i], (int) floatPolygon.ypoints[i]);
		}
		return perimeter;
	}
	*/
	

	public void exit() {
		if (runType == 0) {
			adjustCenterPoints();
		}
		dataSet.setIdentificationExists(true); //TODO: Should be here?
		dataSet.setLinkageExists(true); //TODO: Should be here?
		dataSet.setExternalSegmentationExists(true); //TODO: Should be here?
		controller.setViewActive(true);
		imagePlus.close();
		panel.close();
	}
	
	
	//Fix centerpoints
	public void adjustCenterPoints() {
		Identification id = new Identification();
		id.initialize(imagePlus.getImageStack(), dataSet);
		id.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		id.setFinder(new MaximumFinder(), controller.getMaximumFinderTolerance());
		id.runManualAdjustment();
	}
	

	
	
	
	
}
