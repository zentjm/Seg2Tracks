package manualSegmentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.joml.Math;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
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
import geometricTools.ModifiedMaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import sarn.Sarn;

/**
 * Controller for manual segmentation/editing interface. Allows users to manually draw segmentation contours,
 * merge and delete segmented regions, and refine automatically generated segmentations. Manages the ImagePlus window,
 * overlay rendering, ROI manager integration, and DataSet modifications during manual editing workflows.
 * Supports both creating new segmentations from scratch and modifying existing ones.
 */
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
		
		// Iterate via linkSetList rather than frameSetList: linkSetList is always
		// populated after linkage, and also covers RecursiveDataSet (combinedDataSet)
		// whose frameSetList is never populated — iterating frameSetList directly
		// would NPE on its null entries.
		System.out.println("[Seg2Tracks] runDataSet: " + dataSet.getLinkSetList().size() + " LinkSet(s) found");
		int renderedSegments = 0;
		int skippedSegments = 0;
		for (LinkSet ls : dataSet.getLinkSetList()) {
			for (Segment segment : ls) {
				Point[] perimeter = (runType == 0) ? segment.getExternalPerimeter() : segment.getInternalPerimeter();
				if (perimeter == null || perimeter.length == 0) {
					System.out.println("[Seg2Tracks]   Skipping segment in frame " + segment.getFrame()
							+ " (LinkSet " + ls.getName() + "): null or empty perimeter");
					skippedSegments++;
					continue;
				}
				segment.setRoi(getPolygonRoi(perimeter));
				segment.getRoi().setStrokeColor(color);
				segment.getRoi().setPosition(segment.getFrame() + 1);
				segment.getRoi().setStrokeWidth(2);
				overlay.add(segment.getRoi());
				renderedSegments++;
			}
		}
		System.out.println("[Seg2Tracks] runDataSet: rendered=" + renderedSegments + ", skipped=" + skippedSegments);
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
		dataSet.setManuallyEdited(true);
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

		//System.out.println("Start Frame: " + startFrame + "   Frame: " + frame);
		//System.out.println("CurrentSlice: " + frame + "   StackSize: " + imagePlus.getImageStackSize());
	}
	
	public void nextFrame() {
		if (imagePlus.getRoi() == null) {
			panel.dialogAlert("Must select overlay for this frame");
			return;
		}
		if (segment != null) { //all except for start
			segment.setUserRoi((Roi)imagePlus.getRoi().clone()); //holds for restore
			previousSegment = segment;
		}
		
		//prevents selection out of frame
		if (!withinBounds(imagePlus.getRoi())) {
			panel.dialogAlert("Overlay must be within image bounds");
			return;
		}
		
		
		segment = getSegment(frame - 1, imagePlus.getRoi());
		//segment.setRoi(imagePlus.getRoi());
		segment.getRoi().setPosition(frame);
		//manager.add(imagePlus, segment.getRoi(), frame - 1);
		//linkSet.addSegment(segment);
		segment.setLinkSet(linkSet);
		linkSet.add(segment);
		//System.out.println("NEXT FRAME. LinkSet is:" + linkSet.size());
		//System.out.println("current: " + imagePlus.getCurrentSlice() + "   sliceStace: " + imagePlus.getImageStackSize());
		if (imagePlus.getCurrentSlice() != imagePlus.getImageStackSize()) {
			frame++;
			imagePlus.setSlice(frame);
		}
		panel.stateObject(false, true, frame == startFrame, 
				imagePlus.getCurrentSlice() == imagePlus.getImageStackSize(), false);
	}
	
	public void previousFrame() {
		
		
		linkSet.removeLastSegment();
		
		
		//1. Reconfigure segments
		if (linkSet.size() != 0) {
			segment = linkSet.get(linkSet.size() - 1);
		}
		
		//previousSegment = linkSet.get(linkSet.size() - 2);
		
		
		
		//2. iterate back
		frame--;
		imagePlus.setSlice(frame);
		imagePlus.setRoi(segment.getUserRoi());
		
		
		
		//3 mod buttons
		panel.stateObject(false, true, frame == startFrame, 
				imagePlus.getCurrentSlice() == imagePlus.getImageStackSize(), false);
		
		
		//4 Report
		//System.out.println("PREV FRAME. LinkSet is:" + linkSet.size());
		//System.out.println("current: " + imagePlus.getCurrentSlice() + "   sliceStace: " + imagePlus.getImageStackSize());
		
		
		//TODO: reset the restoreSelection
	}
	
	
	
	public void restoreSelection() {
		if (previousSegment.getUserRoi() == null) {
			//System.out.println("Previous segment Roi is null");
		}
		imagePlus.setRoi((Roi)previousSegment.getUserRoi().clone());
	}

	public void endObject() {
		if (imagePlus.getRoi() == null) {
			panel.dialogAlert("Must select overlay for this frame"); //TODO allow quit or return
			return;
		}
		
		//prevents selection out of frame
		if (!withinBounds(imagePlus.getRoi())) {
			panel.dialogAlert("Overlay must be within image bounds");
			return;
		}
		
		segment = getSegment(frame - 1, imagePlus.getRoi());
		segment.getRoi().setPosition(frame);
		segment.setLinkSet(linkSet);
		linkSet.add(segment);
		//linkSet.setName(linkSet.getDataSet().getLinkSetList().size() - 1);
		linkSet.setName(linkSet.getDataSet().getLinkSetNameIterator());
		panel.stateObject(true, false, false, false, true);
		//System.out.println("LinkSet length is: " + linkSet.size());
		
		int g = 0;
		for (Segment seg: linkSet) {
			g++;
			dataSet.getFrameSet(seg.getFrame()).add(seg);
			//System.out.println("Manual Segment " + g);
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
		dataSet.setManuallyEdited(true);
		
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
		//System.out.println("Number of linkSets: " + dataSet.getLinkSetList().size());
	}
	
	
	 //for (Map.Entry<String,String> entry : gfg.entrySet())
	
	
	//Selects and roi
	//First click selects and highlights, second click deselects. 
	public void selectObject(int x, int y) {
		//System.out.println("Clicked:" + x + " " + y);
		for (Segment seg: dataSet.getFrameSet(imagePlus.getCurrentSlice() - 1)) {
			if (seg.getRoi().contains(x, y)) {
				//collect entire segmentation linklist;
				LinkSet linkSet = seg.getLinkSet();
				for (Segment s: linkSet) {
					s.setRoiSelected(!s.getRoiSelected());
					if (s.getRoiSelected()) s.getRoi().setStrokeColor(altColor);
					if (!s.getRoiSelected()) s.getRoi().setStrokeColor(color);
				}
				//System.out.println("Selection/deselection complete");
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
	
	//TODO: does this work? is this used?
	public void deleteObject(Segment segment) {
		LinkSet linkSet = segment.getLinkSet();
		deleteObject(linkSet);
	}
	
	//TODO: is this even used?
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
					//System.out.println("Removed linkSet: "+ linkSet.getName());
					j--;
				}
			}	
		}
		//System.out.println("Number of Linksets: " + dataSet.getLinkSetList().size());
		imagePlus.getCanvas().repaintOverlay();
	}
	
	
	
	public void mergeObject() {
	
		int startSet1 = Integer.MAX_VALUE;
		int startSet2 = Integer.MAX_VALUE;
		int endSet1 = 0;
		int endSet2 = 0;
		
		LinkSet[] mergeSet = new LinkSet[2];
		
		//Sets start and end of new merged set
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			Segment segment;
			for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
				segment = dataSet.getFrameSet(i).get(j); //TODO: why is variable not method specific?
				if (segment.getRoiSelected()) {
					LinkSet linkSet = segment.getLinkSet();
					
					//initializes first selection
					if (mergeSet[0] == null) {
						mergeSet[0] = linkSet;
						startSet1 = segment.getFrame();
					}
					
					//initializes second selection
					if (mergeSet[0] != null &&  mergeSet[1] == null  && mergeSet[0].getName() != linkSet.getName()) {
						mergeSet[1] = linkSet;
						startSet2 = segment.getFrame();
					}
					
					//determines first frame of first selection
					if (mergeSet[0] != null && mergeSet[0].getName() == linkSet.getName()) {
						//if (segment.getFrame() < startSet1) startSet1 = segment.getFrame();
						if (segment.getFrame() >= endSet1) endSet1 = segment.getFrame();
						//continue;
					}
					
					//determines last frame  of second selection
					if (mergeSet[1] != null && mergeSet[1].getName() == linkSet.getName()) {
						//if (segment.getFrame() < startSet1) startSet1 = segment.getFrame();
						if (segment.getFrame() >= endSet2) endSet2 = segment.getFrame();
						//continue;	
					}
					
					//prevents attempted merging of more than two objects at once
					if (mergeSet[0] != null && mergeSet[1] != null  
							&& mergeSet[0].getName() != linkSet.getName()
							&& mergeSet[1].getName() != linkSet.getName()
					) {
						//System.out.println("Only two objects can be merged at a time");
						return;
					}
				}
			}
		}
		
		if (mergeSet[0] != null) System.out.println("mergedLinkSet[0] = " + mergeSet[0].getName());
		else { System.out.println("mergedLinkSet[0] is null");}
		if (mergeSet[1] != null) System.out.println("mergedLinkSet[1] = " + mergeSet[1].getName());
		else { System.out.println("mergedLinkSet[1] is null");}
		
		
		//System.out.println("startSet1: " + startSet1 + "     endSet1: " + endSet1);
		//System.out.println("startSet2: " + startSet2 + "     endSet2: " + endSet2);

		if (startSet1 > endSet2 || startSet2 > endSet1) {
			//System.out.println("No frame overlap between selections");
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
			if (seg1 == null && seg2 == null) {
				//System.out.println("Both Segments are null");
				continue; //TODO: shouldn't happen here, make an error code
			}
			if (seg1 != null && seg2 == null) {
				newSeg = new Segment(seg1, true);
				newLink.add(newSeg);
				continue;
			}
			if (seg1 == null && seg2 != null) {
				newSeg = new Segment(seg2, true);
				newLink.add(newSeg);
				continue;
			}
			
			/**
			 * @purpose: Makes sure that over the start-end length which defines the number of frames where
			 * both objects exist, that there is some sort of overlap between the objects in every frame. If not, 
			 * the segments will not be merged
			 * 
			 * This is a screening operation before running the merge operation, with this and the merge operation 
			 * running similar searches for intersections. These operations could be combined (TODO) to speed up 
			 * the merge process. Additionally, is there any reason for or logical approach to combining two segments 
			 * that have a lapse of overlap in some or all of their shared frames (TODO)?
			 * 
			 * @possiblebugs: none currently notable.
			 * 
			 */
			 
			//This should be used to screen earlier...doesnt fit within this loop properly
			Point[] A = GeometricCalculations.straightPerimeter(seg1.getExternalPerimeter());
			Point[] B = GeometricCalculations.straightPerimeter(seg2.getExternalPerimeter());
			overlap = GeometricCalculations.boundryOverlap(A, B, 0); //Threshold set for any overlap at all TODO: overlap?
			if (!overlap) {
				//System.out.println("NOT overlapping in frame " + i);
				//System.out.println("Selected objects are not overlapping");
				return;
			}
			
			/**
			 * Code Snippet Purpose: Merges the points to return a pointArray - see mergeSegments
			 * 
			 * @possiblebugs: See other method.
			 */
			
			Point[] newSegPoints = mergeSegments(seg1, seg2);
			
			/**
			 * Code Snippet Purpose: Determines new centerpoint for the merged object using the geometric
			 * average of the two centerpoints. If this point is outside the new merged object then
			 * uses the centerpoint of whichever of the merged objects was originally bigger. Have not tested 
			 * this approach against other approaches. If going to recalculate should probably just stick with
			 * the averaged centerpoint (TODO) regardless of whether or not it is in the merged segment.
			 * 
			 * @possiblebugs: This method wont necessarily work for all situations,
			 * especially if the external segmentation is to be recalculated.
			 */
			
			Point centerPoint = new Point (
					((seg1.getCenterPoint().x + seg2.getCenterPoint().x)/2),
					((seg1.getCenterPoint().y + seg2.getCenterPoint().y)/2)
					);
			
			if (!GeometricCalculations.pointInsideShape(centerPoint, newSegPoints, imagePlus.getWidth())) {
				int sizeA = GeometricCalculations.getAreaByRoi(A).length;
				int sizeB = GeometricCalculations.getAreaByRoi(B).length;
				centerPoint = sizeA > sizeB ? seg1.getCenterPoint(): seg2.getCenterPoint();	
			}
			
			
			/**
			 * Code Snippet Purpose: Creates the new segment and adds it.
			 * 
			 * @possiblebugs: See other method.
			 */
			
			newSeg = new Segment(i, centerPoint);
			newSeg.setExternalPerimeter(newSegPoints);
			newLink.add(newSeg);
		}
		
		/**
		 * @purpose: to be reviewed
		 * 
		 * @possiblebugs: to be reviewed
		 */
				
		//remove old objects
		//newLink.setName(dataSet.getLinkSetList().size());
		deleteObject(mergeSet[0]);
		deleteObject(mergeSet[1]);
		//newLink.setName(newLink.getDataSet().getLinkSetList().size() - 1);
		newLink.setName(newLink.getDataSet().getLinkSetNameIterator());
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
		
		//System.out.println("newLink size is: " + newLink.size());
		//System.out.println("current number of linksets is: " + newLink.getDataSet().getLinkSetList().size());
		
		for (int i = 0; i < newLink.size(); i++) {
			if (newLink.get(i) == null) {
				//System.out.println("newLink @ " + i + " is null");
			}
		}
		
	}
	
	
	
	//Merges based on line intersections.
	private Point[] mergeSegments(Segment segA, Segment segB) {
		
		/**
		 * Code Snippet Purpose: gets the external points of the segments to be merged
		 * 
		 * The use of shortcutPerimeter is absolutely necessary, as otherwise a set of perimeter points
		 * (1,2,3) that have the locations (A, B, A) will cause an error. This might be corrected in the
		 * future (TODO) but shorcutPerimeter will trim those points down to just (A) to avoid the error
		 * from happening. ShortcutPerimeter and the subsequent intersection-based code requires that 
		 * straightPerimeter has at some point been enacted but if modifying an automatic it should already 
		 * have been run. However, I would need to confirm (TODO) that happens with all previous manual modification 
		 * changes as well 
		 */
		
		Point[] A = GeometricCalculations.shortcutPerimeter(GeometricCalculations.straightPerimeter(segA.getExternalPerimeter()));
		Point[] B = GeometricCalculations.shortcutPerimeter(GeometricCalculations.straightPerimeter(segB.getExternalPerimeter()));
		
		
		/**
		 * Code Snippet Purpose: For holding as a package the points of segA (A[]) and segB (B[]) found
		 * at intersections together with the index of the intersection as it sits along A[].
		 * 
		 * The points A and B may be the same, but Point B may also be adjacent (8-way) to Point A. It is 
		 * unclear whether Point A is actually important to save as it currently is unused. Possibly it might
		 * be used (TODO) if the choice of which Point A route needs to assess internal vs. external point location
		 * with respect to being within segB, but so far bugs requiring that change have not appeared
		 */
		
		class PointHolder {
			Point A;
			Point B;
			int index;
			public PointHolder(Point A, Point B, int index) {
				this.A = A;
				this.B = B;
				this.index = index;
			}
		}
		
		
		/**
		 * Code Snippet Purpose: finds intersections and classifies them as having a shared point (if statement 1),
		 * having a subsequent or adjacent shared point (if statement 2), or having no shared points at all (if 
		 * statement 3). These classifications are used to assign "intersected" point pairs with associated indices 
		 * along SegA (A[]) to be used for merging.
		 * 
		 * @ int a1 = (a < A.length - 1) ? a + 1 : 0;
		 * This is code used to allow circulating back from A.length to zero.
		 * 
		 * @ Line2D
		 * This is used to create lines between each adjacent point for A[] and B[] which then can be used to assess
		 * for intersection. I could possibly write my own intersection code without the need to pull in other object
		 * classes (TODO) but this might not even be faster and would obviously be more error prone. 
		 * 
		 * For the if statements, the first basically just says that if the first points of the intersection share the 
		 * same first point (first-first match), that point defines the intersect. The second queries if the intersecting 
		 * lines they share adjacent points (fist-last, last-first, last-last) and punts all those types of matches
		 * to the next loop iteration where they might be identified as first-first or otherwise trip the third if which
		 * deals with non-point-matched intersections. It is unclear whether all these match permutations should be handled
		 * this way or what effect changing the criteria would have (TODO) - possible source of bugs. Since a non-matched 
		 * intersection ultimately results in 4 different points around the intersection, the nested functions within the 
		 * third if statement use the pointInsideShape function to choose the A and B points that are outside the other shape,
		 * thus are before the intersection (from the point of view that intersecting means they now share points). This code
		 * appears to work and all lines are reach in currently tested code but has not been thoroughly tested to prove it 
		 * actually works as claimed (TODO). 
		 * 
		 * @possiblebugs: evaluate the utility of the conditions for the second if statement
		 */
		
		ArrayList<PointHolder> contacts = new ArrayList<PointHolder>();
		for (int a = 0; a < A.length; a++) {
			int a1 = (a < A.length - 1) ? a + 1 : 0; //facilitates circularity
			Line2D.Double lineA = new Line2D.Double(A[a], A[a1]);
			for (int b = 0; b < B.length; b++) {
				int b1 = (b < B.length - 1) ? b + 1 : 0; 	//facilitates circularity
				Line2D.Double lineB = new Line2D.Double(B[b], B[b1]);
				if (lineA.intersectsLine(lineB)) {
					if (A[a].x == B[b].x && A[a].y == B[b].y) {  //IF statement 1
						contacts.add(new PointHolder(A[a],B[b],a));
						//System.out.println("At a#" + a + " b#" + b + " PointA[" +A[a].x +"][" + A[a].y + 
						//		"]   PointB[" + B[b].x + "][" + B[b].y +"]       found a match");
						continue;
					}
					else if ((A[a].x == B[b1].x && A[a].y == B[b1].y) || //IF statement 2
						(A[a1].x == B[b].x && A[a1].y == B[b].y) ||
						(A[a1].x == B[b1].x && A[a1].y == B[b1].y)
						) {
						//System.out.println("At a#" + a + " b#" + b + " PointA[" +A[a].x +"][" + A[a].y + 
						//		"]   PointB[" + B[b].x + "][" + B[b].y +"]       found a LATER match");
						continue;
					}
					else { //IF statement 2
						if (!GeometricCalculations.pointInsideShape(A[a], B, imagePlus.getWidth())) {
							if (!GeometricCalculations.pointInsideShape(B[b], A, imagePlus.getWidth())) {
								contacts.add(new PointHolder(A[a],B[b],a));
								//System.out.println("At #" + a + " PointA[" +A[a].x +"][" + A[a].y + 
								//		"]   PointB[" + B[b].x + "][" + B[b].y +"]       found an a-b intersect");
							}
							else {
								contacts.add(new PointHolder(A[a],B[b1],a));
								//System.out.println("At #" + a + " PointA[" +A[a].x +"][" + A[a].y + 
								//		"]   PointB[" + B[b1].x + "][" + B[b1].y +"]       found an a-b1 intersect");
							}
						}
						else {
							if (!GeometricCalculations.pointInsideShape(B[b], A, imagePlus.getWidth())) {
								contacts.add(new PointHolder(A[a1],B[b],a1));
								//System.out.println("At #" + a + " PointA[" +A[a1].x +"][" + A[a1].y + 
								//		"]   PointB[" + B[b].x + "][" + B[b].y +"]       found an a1-b intersect");
							}
							else {
								contacts.add(new PointHolder(A[a1],B[b1],a1));
								//System.out.println("At #" + a + " PointA[" +A[a1].x +"][" + A[a1].y + 
								//		"]   PointB[" + B[b1].x + "][" + B[b1].y +"]       found an a1-b1 intersect");
							}
						}
					}
				}	
			}
		}
		
		/**
		 * Code Snippet Purpose: Delineates subarray of segA (A[]) for merging in final new segment based 
		 * on which of the intersections are farthest apart. Also records SegB intersect start and end points
		 * in the process.
		 * 
		 * There may be a role for using the criteria of which intersections are farthest part EXCLUDING
		 * in the distance calculation points within A that fall within SegB (TODO). However, up until now
		 * it seems to work fine. 
		 * 
		 * @possiblebugs: off-by-one bugs stemming from circulizing the array search loop, particularly the
		 * while loop when creating the subarray from the identified start and end points
		 */
		
		int maxLength = 0;;
		Point startIntersectB= null;
		Point endIntersectB = null;
		int start = 0;
		int end = 0;
		for (int i = 0; i < contacts.size(); i++) {
			int i1 = (i < contacts.size() - 1) ? i + 1 : 0; //facilitates circularity
			int length = -1;
			if (contacts.get(i1).index > contacts.get(i).index) {
				length = contacts.get(i1).index - contacts.get(i).index;
			}
			else length = (A.length + 1 -contacts.get(i).index) + contacts.get(i1).index;
			if (length > maxLength) {
				maxLength = length;
				start = contacts.get(i).index;
				end = contacts.get(i1).index;
				startIntersectB = contacts.get(i).B;
				endIntersectB = contacts.get(i1).B;
			}
		}
		ArrayList<Point> listA1 = new ArrayList<Point>();
		int n = start;		
		while (n != end) { //adds start and end
			listA1.add(A[n]);
			n++;
			if (n > A.length -1) n = n - A.length;
		}
		
		/**
		 * Code Snippet Purpose: Sets end of subarrayA as start of subarrayB and start of subarrayA as
		 * the end of subarrayB for the purpose of creating a correct circular path for the final merged 
		 * product.
		 * 
		 * @possiblebugs: none?
		 */
		
		//Aligns start and end B's with start and end A's
		int startIndexB = -1;
		int endIndexB = -1;
		for (int i = 0; i < B.length; i++) {
			if (B[i].x == startIntersectB.x && B[i].y == startIntersectB.y) endIndexB = i;
			if (B[i].x == endIntersectB.x && B[i].y == endIntersectB.y) startIndexB = i;
		}	
		
		/**
		 * Code Snippet Purpose: adds subarrayB start-to-finish to the subarray of A to create merged
		 * objects. Creates two possible options (option 1 and option 2) with each subarray following one 
		 * of the two paths from start to end along the B path (since it is circular). These get compared 
		 * to choose the correct one later.
		 * 
		 * @possiblebugs: the while loops are prone to off-by-one issues that may be symptomatic
		 * under the appropriate rare circumstances
		 */
	

		ArrayList<Point> listB1 = (ArrayList<Point>) listA1.clone(); //Option1
		n = startIndexB + 1;
		while (n != endIndexB) { 
			if (n > B.length -1) n = n - B.length;
			listB1.add(B[n]);
			n++;
		}
		ArrayList<Point> listB2 = (ArrayList<Point>) listA1.clone(); //Option2
		n = startIndexB - 1;
		while (n != endIndexB) {
			if (n < 0) n = n + B.length;
			listB2.add(B[n]);
			n--;
		}
		
		
		/**
		 * Code Snippet Purpose: Transfers merge options from ArrayLists to array for further use.
		 * 
		 * Code for tranferring listA is commented out but is needed for testing/display.
		 * 
		 * @possiblebugs: none?
		 */
		
		Point[] B1 = new Point[listB1.size()];
		for (int i = 0; i < B1.length; i++) {
			B1[i] = listB1.get(i);
		}
		
		Point[] B2 = new Point[listB2.size()];
		for (int i = 0; i < B2.length; i++) {
			B2[i] = listB2.get(i);
		}
		
		/*
		//TESTING for just the first half
		Point[] A1 = new Point[listA1.size()];
		for (int i = 0; i < A1.length; i++) {
			A1[i] = listA1.get(i);
		}
		Point[] C = A1;
		*/
		
		
		/**
		 * Code Snippet Purpose: Counts the number of array points of the two merge options 
		 * listB1 and listB2 that are not located within the SegA (A[]) boundaries. Chooses
		 * which one is the best based on 
		 * 
		 * B1 and B2 also contain the listA points and previously the comparison did 
		 * not include those points. It shouldn't make a difference but it will increase the 
		 * program time due to unnecessarily assessing those points. Possibly, code should be
		 * modified (TODO) to exclude those points from the loop. Another quesion is whether this
		 * is the best metric to use. Commented out is another option using the fraction of points
		 * outside rather than total. Count is probably better though because longer loop segments get
		 * more sway.
		 * 
		 * @possiblebugs: The metric could be inappropriate for all conditions. 
		 */
		
		double countB1 = 0;
		double countB2 = 0;
		for (Point pt : B1) {
			if (!GeometricCalculations.pointInsideShape(pt, A, imagePlus.getWidth())) countB1++;
		}
		for (Point pt : B2) {
			if (!GeometricCalculations.pointInsideShape(pt, A, imagePlus.getWidth())) countB2++;
		}
		Point[]C = countB1 > countB2 ? B1 : B2;
		
		//Could be used for determining C[] instead:
		//Double fractionB1 = countB1 / listB1.size();
		//Double fractionB2 = countB2 / listB2.size();
	
		/**
		 * Code Snippet Purpose: converts C[] to new segment and returns.
		 *
		 * @possiblebugs: Maybe need to use the straightPerimeter or shortcutPerimeter 
		 * here for some applications? no idea..
		 */
		
		return C;
	}

	//TODO
	public void splitObject() {
		
	}
	
	//TODO
	public void linkObject() {
		
	}
	
	//TODO
	public void unLinkObject() {
		
	}
	
	//TODO
	private Segment getSegment (int frame, Roi roi) {
		
		//get a float from this
		FloatPolygon floatPolygon = roi.getFloatPolygon("close");
		
		//Get geometric centerpoint
		Rectangle rect = floatPolygon.getBounds();
		Point centerpoint = new Point (rect.x + (rect.width / 2), rect.y + (rect.height / 2));
		
		//TODO: fix centerpoint. 
		//System.out.println("CenterPoint is... x:" + centerpoint.x + "   y:"  + centerpoint.y);
		
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
	
	//
	public void exit() {
		if (runType == 0) {
			adjustCenterPoints();
		}
		dataSet.setIdentificationExists(true); //TODO: Should be here?
		dataSet.setLinkageExists(true); //TODO: Should be here?
		dataSet.setExternalSegmentationExists(true); //TODO: Should be here?
		dataSet.setManuallyEdited(false);
		controller.setViewActive(true);
		imagePlus.close();
		panel.close();
	}
	
	//Fix centerpoints //TODO: speed this up by only caalculating for modified objects
	public void adjustCenterPoints() {
		// Build the working stack. If invert intensity is enabled, produce an inverted
		// copy so identification sees the same polarity as the automated pipeline.
		// The displayed imagePlus is never modified.
		ImageStack stack = imagePlus.getImageStack();
		if (controller.getInvertIntensity()) {
			ImageStack invertedStack = new ImageStack(stack.getWidth(), stack.getHeight());
			for (int i = 1; i <= stack.getSize(); i++) {
				ImageProcessor ip = stack.getProcessor(i).duplicate();
				ip.invert();
				invertedStack.addSlice(ip);
			}
			stack = invertedStack;
		}
		Identification id = new Identification();
		id.initialize(stack, dataSet);
		id.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		id.setFinder(new ModifiedMaximumFinder(), controller.getMaximumFinderTolerance());
		id.runManualAdjustment();
	}
	
	//makes sure an Roi is within the bounds of an image
	public boolean withinBounds(Roi roi) {
		if (roi.getBounds().x + roi.getBounds().width > imagePlus.getWidth() -1) return false;
		if (roi.getBounds().x < 0) return false;
		if (roi.getBounds().y + roi.getBounds().height > imagePlus.getHeight() -1) return false;
		if (roi.getBounds().y < 0) return false;
		return true;
	}
	
	
	
	
}
