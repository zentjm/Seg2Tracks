package manualSegmentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

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
import ij.Prefs;
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
	
	static final String PREF_ROI_COLOR = "seg2tracks.roi.color";
	static final String PREF_TOOL      = "seg2tracks.tool";

	//Roi roi;
	Color color;
	Color altColor;
	
	//Holding current segments
	Segment segment;
	Segment previousSegment;
	
	//Determines whether preview or editable
	boolean canEdit;

	// ── Split state ───────────────────────────────────────────────────────────
	/** LinkSet the user has selected for splitting; null when no split is in progress. */
	private LinkSet  splitTargetLinkSet = null;
	/** 0-based frame index at which the split is being performed. */
	private int      splitFrame         = -1;
	/** True while the controller is waiting for the user to draw a bisecting line. */
	private boolean  awaitingSplitLine  = false;

	public ManualSegmentationController(int runType, OperationController controller, boolean canEdit) {
		this.controller = controller;
		this.runType = runType;
		this.canEdit = canEdit;
		imagePlus = new ImagePlus("ManualSegmentation", IJ.openVirtual(controller.getInputFilePath()).getImageStack());
		color    = decodeColor(Prefs.get(PREF_ROI_COLOR, "#00ff00"));
		altColor = new Color(255, 0, 0);
		ij.gui.Roi.setColor(color);
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
	
	/** True while the segmentation sub-panel is showing; used by openSettings() to provide context. */
	private boolean inSegmentMode = false;

	//returns to main menu
	public void mainMenu() {
		inSegmentMode = false;
		if (mouseListenerActive != null && mouseListenerActive == true) {
			mouseListenerActive = false;
		}
		overlay.selectable(false);
		panel.setMainPanel(); //TODO: Set booleans on LOADED Files
		IJ.setTool("hand");
	}

	public void openSettings() {
		panel.setSettingsPanel(inSegmentMode);
	}

	public void closeSettings() {
		if (inSegmentMode) {
			panel.setSegmentPanel();
			// Intentionally omit stateObject() — preserve the current button-enable
			// state so that returning from Settings mid-draw keeps Next/End active.
		} else {
			mainMenu();
		}
	}

	public void newSegmentation() {
		inSegmentMode = true;
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
		IJ.setTool(Prefs.get(PREF_TOOL, "polygon"));
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
		//prevents selection out of frame — must check BEFORE saving userRoi so an
		//out-of-bounds draw cannot be offered back via Restore Selection
		if (!withinBounds(imagePlus.getRoi())) {
			panel.dialogAlert("Overlay must be within image bounds");
			return;
		}

		if (segment != null) { //all except for start
			segment.setUserRoi((Roi)imagePlus.getRoi().clone()); //holds for restore
			previousSegment = segment;
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
		} else {
			segment = null;
		}

		//2. iterate back
		frame--;
		imagePlus.setSlice(frame);
		if (segment != null) imagePlus.setRoi(segment.getUserRoi());
		
		
		
		//3 mod buttons
		panel.stateObject(false, true, frame == startFrame, 
				imagePlus.getCurrentSlice() == imagePlus.getImageStackSize(), false);
		
		
		//4 Report
		//System.out.println("PREV FRAME. LinkSet is:" + linkSet.size());
		//System.out.println("current: " + imagePlus.getCurrentSlice() + "   sliceStace: " + imagePlus.getImageStackSize());
		
		
		//TODO: reset the restoreSelection
	}
	
	
	
	public void restoreSelection() {
		if (previousSegment == null || previousSegment.getUserRoi() == null) return;
		imagePlus.setRoi((Roi) previousSegment.getUserRoi().clone());
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
		inSegmentMode = false;
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
						// While a Redraw Segment edit is armed, clicks are placing polygon/freehand
						// vertices for the new outline — must not also toggle selection underneath.
						if (mouseListenerActive && !redrawInProgress) {
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
		imagePlus.updateAndDraw();
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
		controller.setModifyData(runType, dataSet); //commit the merge on the same channel that was edited
		imagePlus.getCanvas().repaintOverlay();
		
		//System.out.println("newLink size is: " + newLink.size());
		//System.out.println("current number of linksets is: " + newLink.getDataSet().getLinkSetList().size());
		
		for (int i = 0; i < newLink.size(); i++) {
			if (newLink.get(i) == null) {
				//System.out.println("newLink @ " + i + " is null");
			}
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Per-segment SARN area editing ("Redraw Segment")
	//
	// Corrects a single frame's outline (externalPerimeter) within an
	// already-committed track, without deleting and re-drawing the whole track.
	// Reuses the existing click-to-select mechanism (selectObject() /
	// getRoiSelected()) that Delete and Merge already use — this action just
	// requires exactly one object selected, then acts on whichever frame is
	// currently displayed within it. The edit replaces the Segment object at
	// that frame — same LinkSet, same position, so the track stays linked
	// ahead and behind exactly as before — using the same getSegment() path
	// every other manual draw in this controller already uses.
	//
	// TODO: if this dataset already has downstream analysis results computed
	// against the old boundary, redrawing a segment could invalidate them. It
	// would be reasonable to warn the user before allowing the redraw in that
	// case. Not implemented.
	// ═══════════════════════════════════════════════════════════════════════════

	/** True while a Redraw Segment edit is armed (drawing in progress, awaiting Apply/Cancel). */
	private boolean redrawInProgress = false;

	/** The LinkSet whose current-frame segment is being redrawn. */
	private LinkSet redrawTargetLinkSet;

	/**
	 * Returns the single LinkSet with any selected (red-highlighted) segment in
	 * dataSet, or null if zero or more than one distinct LinkSet is selected.
	 */
	private LinkSet getSingleSelectedLinkSet() {
		LinkSet found = null;
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			for (Segment seg : dataSet.getFrameSet(i)) {
				if (seg.getRoiSelected()) {
					LinkSet ls = seg.getLinkSet();
					if (ls == null) continue;
					if (found == null) found = ls;
					else if (found != ls) return null; // more than one distinct object selected
				}
			}
		}
		return found;
	}

	/** Finds the segment belonging to {@code ls} on frame {@code frame} in dataSet, or null. */
	private Segment getSegmentOnFrame(LinkSet ls, int frame) {
		if (frame < 0 || frame >= dataSet.getFrameSetList().length) return null;
		for (Segment s : dataSet.getFrameSet(frame)) {
			if (s.getLinkSet() == ls) return s;
		}
		return null;
	}

	/**
	 * Begins a Redraw Segment edit: validates exactly one object is selected and that
	 * it has a segment on the currently displayed frame, then arms drawing mode.
	 */
	public void startRedrawSegment() {
		LinkSet target = getSingleSelectedLinkSet();
		if (target == null) {
			panel.dialogAlert("Select exactly one object (click to select) before redrawing a segment.");
			return;
		}

		int frame = imagePlus.getCurrentSlice() - 1;
		Segment seg = getSegmentOnFrame(target, frame);
		if (seg == null) {
			panel.dialogAlert("The selected object has no segment on the current frame. "
					+ "Navigate to a frame within its track, then try again.");
			return;
		}

		redrawTargetLinkSet = target;
		redrawInProgress = true;
		IJ.setTool(Prefs.get(PREF_TOOL, "polygon"));
		imagePlus.setRoi(seg.getRoi()); // preload the current outline as a starting point
		panel.setRedrawSegmentPanel();
	}

	/**
	 * Commits the drawn outline as the replacement for the selected object's segment
	 * on the current frame.
	 */
	public void applyRedrawSegment() {
		if (imagePlus.getRoi() == null) {
			panel.dialogAlert("Must draw a new outline before applying.");
			return;
		}
		if (!withinBounds(imagePlus.getRoi())) {
			panel.dialogAlert("Outline must be within image bounds");
			return;
		}

		int frame = imagePlus.getCurrentSlice() - 1;

		Segment oldSeg = getSegmentOnFrame(redrawTargetLinkSet, frame);
		if (oldSeg == null) {
			panel.dialogAlert("Could not locate the segment to replace. Redraw cancelled.");
			cancelRedrawSegment();
			return;
		}

		Segment newSeg = getSegment(frame, imagePlus.getRoi());
		newSeg.setLinkSet(redrawTargetLinkSet);

		int trackIdx = redrawTargetLinkSet.indexOf(oldSeg);
		if (trackIdx >= 0) redrawTargetLinkSet.set(trackIdx, newSeg);

		FrameSet frameSet = dataSet.getFrameSet(frame);
		int frameIdx = frameSet.indexOf(oldSeg);
		if (frameIdx >= 0) frameSet.set(frameIdx, newSeg);

		if (oldSeg.getRoi() != null) overlay.remove(oldSeg.getRoi());
		Roi newRoi = newSeg.getRoi();
		if (newRoi != null) {
			newRoi.setStrokeColor(color);
			newRoi.setStrokeWidth(2);
			newRoi.setPosition(frame + 1);
			overlay.add(newRoi);
		}

		// Clear selection across the whole track — it was selected (red) to enter this
		// mode; leave everything deselected afterward, matching mergeObject()'s convention.
		for (Segment s : redrawTargetLinkSet) {
			s.setRoiSelected(false);
			if (s.getRoi() != null) s.getRoi().setStrokeColor(color);
		}

		imagePlus.setOverlay(overlay);
		controller.setModifyData(runType, dataSet);
		imagePlus.getCanvas().repaintOverlay();
		controller.autosave();

		finishRedrawSegment();
	}

	/** Discards the drawn outline and returns to the modification panel without changing any data. */
	public void cancelRedrawSegment() {
		imagePlus.killRoi();
		finishRedrawSegment();
	}

	/** Restores normal navigation and returns to the modification panel after Apply or Cancel. */
	private void finishRedrawSegment() {
		redrawInProgress = false;
		redrawTargetLinkSet = null;
		IJ.setTool("hand");
		panel.setModificationPanel();
	}

	//Merges based on line intersections.
	private Point[] mergeSegments(Segment segA, Segment segB) {
		
		/**
		 * Code Snippet Purpose: gets the external points of the segments to be merged
		 *
		 * The use of removeLoops is absolutely necessary, as otherwise a set of perimeter points
		 * (1,2,3) that have the locations (A, B, A) will cause an error. This might be corrected in the
		 * future (TODO) but removeLoops will trim those points down to just (A) to avoid the error
		 * from happening. removeLoops and the subsequent intersection-based code requires that
		 * straightPerimeter has at some point been enacted but if modifying an automatic it should already
		 * have been run. However, I would need to confirm (TODO) that happens with all previous manual modification
		 * changes as well
		 */

		Point[] A = GeometricCalculations.removeLoops(GeometricCalculations.straightPerimeter(segA.getExternalPerimeter()));
		Point[] B = GeometricCalculations.removeLoops(GeometricCalculations.straightPerimeter(segB.getExternalPerimeter()));
		
		
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
		
		// Need at least two distinct contact points to form a valid merged perimeter.
		// With fewer contacts the while-loop boundary conditions degenerate (start == end),
		// producing a half-perimeter. Alert the user and abort rather than silently corrupt.
		if (contacts.size() < 2) {
			panel.dialogAlert("Could not merge: insufficient boundary overlap between selected objects.");
			return null;
		}

		int maxLength = 0;
		Point startIntersectB = null;
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
		 * @possiblebugs: Maybe need to use the straightPerimeter or removeLoops
		 * here for some applications? no idea..
		 */
		
		return C;
	}

	// ── Split command ─────────────────────────────────────────────────────────

	/**
	 * Phase 1 of the split workflow. Validates that exactly one object is selected
	 * in the current frame, then transitions to line-drawing mode: activates
	 * ImageJ's Line tool and switches the panel to the split-line sub-panel.
	 */
	public void splitObject() {
		int currentFrameIdx = imagePlus.getCurrentSlice() - 1;

		// Identify the one selected LinkSet in this frame
		LinkSet selectedLS = null;
		FrameSet fs = dataSet.getFrameSet(currentFrameIdx);
		if (fs == null) { panel.dialogAlert("No segments in this frame."); return; }
		for (Segment seg : fs) {
			if (seg.getRoiSelected()) {
				if (selectedLS == null) {
					selectedLS = seg.getLinkSet();
				} else if (selectedLS != seg.getLinkSet()) {
					panel.dialogAlert("Select exactly one object to split.");
					return;
				}
			}
		}
		if (selectedLS == null) {
			panel.dialogAlert("No object selected. Click an object first, then press Split.");
			return;
		}

		// Confirm the segment has a SARN perimeter
		Segment segInFrame = null;
		for (Segment seg : selectedLS) {
			if (seg.getFrame() == currentFrameIdx) { segInFrame = seg; break; }
		}
		if (segInFrame == null || segInFrame.getExternalPerimeter() == null) {
			panel.dialogAlert("Selected object has no SARN perimeter in this frame.\n"
					+ "Run external segmentation before using Split.");
			return;
		}

		// Store state and enter line-drawing mode
		splitTargetLinkSet  = selectedLS;
		splitFrame          = currentFrameIdx;
		awaitingSplitLine   = true;
		mouseListenerActive = false;   // suspend click-selection while drawing line
		IJ.setTool("line");
		panel.setSplitLinePanel();
	}

	/**
	 * Cancels an in-progress split and returns to the modification panel.
	 */
	public void cancelSplit() {
		splitTargetLinkSet  = null;
		splitFrame          = -1;
		awaitingSplitLine   = false;
		mouseListenerActive = true;
		IJ.setTool("hand");
		panel.setModificationPanel();
	}

	/**
	 * Phase 2 of the split workflow. Called when the user clicks "Apply Split"
	 * after drawing a bisecting line ROI. Validates the line, finds maxima on
	 * each side, re-runs SARN from each seed, propagates both branches forward
	 * and backward through the stack, and replaces the original LinkSet with two.
	 */
	public void confirmSplitLine() {
		if (!awaitingSplitLine || splitTargetLinkSet == null) return;

		// ── 1. Validate the line ROI ──────────────────────────────────────────
		Roi lineRoi = imagePlus.getRoi();
		if (lineRoi == null || lineRoi.getType() != Roi.LINE) {
			panel.dialogAlert("Please draw a straight line across the object first\n"
					+ "(activate the Line tool, drag across the object, then click Apply Split).");
			return;
		}

		// Extract line endpoints from the float polygon (first and last point)
		ij.process.FloatPolygon fp = lineRoi.getFloatPolygon();
		double lx1 = fp.xpoints[0], ly1 = fp.ypoints[0];
		double lx2 = fp.xpoints[fp.npoints - 1], ly2 = fp.ypoints[fp.npoints - 1];

		// ── 2. Find the segment to split ──────────────────────────────────────
		Segment segToSplit = null;
		for (Segment seg : splitTargetLinkSet) {
			if (seg.getFrame() == splitFrame) { segToSplit = seg; break; }
		}
		if (segToSplit == null || segToSplit.getExternalPerimeter() == null) {
			panel.dialogAlert("Cannot locate the segment perimeter. Has segmentation been cleared?");
			cancelSplit();
			return;
		}
		Point[] perim = segToSplit.getExternalPerimeter();

		// ── 3. Validate the line bisects the perimeter (≥ 2 crossings) ───────
		int crossings = 0;
		for (int i = 0; i < perim.length; i++) {
			Point p1 = perim[i];
			Point p2 = perim[(i + 1) % perim.length];
			if (Line2D.linesIntersect(lx1, ly1, lx2, ly2, p1.x, p1.y, p2.x, p2.y)) crossings++;
		}
		if (crossings < 2) {
			panel.dialogAlert("The line does not cross the object boundary at two points.\n"
					+ "Draw the line so it passes all the way through the object.");
			return;
		}

		// ── 4. Blurred frame and tolerance for this frame ─────────────────────
		ImageStack stack = imagePlus.getImageStack();
		ImageProcessor blurredFrame = stack.getProcessor(splitFrame + 1).duplicate();
		if (controller.getInvertIntensity()) blurredFrame.invert();
		new GaussianBlur().blurGaussian(blurredFrame, controller.getGaussianBlurSigma());
		double tolerance = computeTolerance(blurredFrame);

		// ── 5. Find the best maxima on each side of the line ──────────────────
		Polygon allMaxima = new ModifiedMaximumFinder().getMaxima(blurredFrame, tolerance, true);
		Point bestSideA = null, bestSideB = null;
		int   bestIntA  = Integer.MIN_VALUE, bestIntB = Integer.MIN_VALUE;

		for (int mi = 0; mi < allMaxima.npoints; mi++) {
			int mx = allMaxima.xpoints[mi];
			int my = allMaxima.ypoints[mi];
			// Keep only maxima inside the segment perimeter
			if (!GeometricCalculations.pointInsideShape(new Point(mx, my), perim, imagePlus.getWidth()))
				continue;
			// Classify by side of the line using the cross product sign
			double cross = (lx2 - lx1) * (my - ly1) - (ly2 - ly1) * (mx - lx1);
			int intensity = blurredFrame.get(mx, my);
			if (cross >= 0) {
				if (intensity > bestIntA) { bestIntA = intensity; bestSideA = new Point(mx, my); }
			} else {
				if (intensity > bestIntB) { bestIntB = intensity; bestSideB = new Point(mx, my); }
			}
		}

		if (bestSideA == null || bestSideB == null) {
			panel.dialogAlert("Could not find detectable intensity peaks on both sides of the line.\n"
					+ "Try adjusting the line position, or check that Object ID parameters are set correctly.");
			return;   // don't cancel — let the user try a different line
		}

		// ── 6. Valley check — warn if no clear intensity valley exists ─────────
		int numSamples = (int) java.lang.Math.max(
				java.lang.Math.abs(bestSideB.x - bestSideA.x),
				java.lang.Math.abs(bestSideB.y - bestSideA.y)) + 1;
		int minValley = java.lang.Math.min(bestIntA, bestIntB);
		for (int si = 0; si <= numSamples; si++) {
			double t  = (numSamples == 0) ? 0.0 : (double) si / numSamples;
			int    sx = (int) java.lang.Math.round(bestSideA.x + t * (bestSideB.x - bestSideA.x));
			int    sy = (int) java.lang.Math.round(bestSideA.y + t * (bestSideB.y - bestSideA.y));
			if (sx >= 0 && sx < blurredFrame.getWidth() && sy >= 0 && sy < blurredFrame.getHeight())
				minValley = java.lang.Math.min(minValley, blurredFrame.get(sx, sy));
		}
		boolean hasValley = (java.lang.Math.min(bestIntA, bestIntB) - minValley) > tolerance * 0.5;
		if (!hasValley) {
			int choice = JOptionPane.showConfirmDialog(null,
					"No clear intensity valley detected between the two peaks.\n"
					+ "The split may not represent two genuinely separate objects.\n"
					+ "Continue anyway?",
					"Split Warning", JOptionPane.YES_NO_OPTION);
			if (choice != JOptionPane.YES_OPTION) return;
		}

		// ── 7. Run SARN from each seed at the split frame (neighbor-aware) ────
		Sarn sarn = controller.getExternalSegmentationMethod();
		Segment newSegA = runSarnFromSeed(stack, bestSideA, splitFrame, sarn, bestSideB);
		Segment newSegB = runSarnFromSeed(stack, bestSideB, splitFrame, sarn, bestSideA);

		if (newSegA.getExternalPerimeter() == null || newSegB.getExternalPerimeter() == null) {
			panel.dialogAlert("SARN could not produce a valid boundary for one or both split halves.\n"
					+ "Try repositioning the line or adjusting segmentation parameters.");
			cancelSplit();
			return;
		}

		// ── 8. Create two new LinkSets ───────────────────────────────────────────
		LinkSet linkSetA = new LinkSet(dataSet);
		linkSetA.setName(dataSet.getLinkSetNameIterator());
		LinkSet linkSetB = new LinkSet(dataSet);
		linkSetB.setName(dataSet.getLinkSetNameIterator());

		// ── 9. Delete original LinkSet BEFORE adding new segments ─────────────
		deleteObject(splitTargetLinkSet);

		// ── 10. Register the split-frame segments ─────────────────────────────
		newSegA.setLinkSet(linkSetA); linkSetA.add(newSegA);
		dataSet.getFrameSet(splitFrame).add(newSegA);
		addSegmentToOverlay(newSegA, color);

		newSegB.setLinkSet(linkSetB); linkSetB.add(newSegB);
		dataSet.getFrameSet(splitFrame).add(newSegB);
		addSegmentToOverlay(newSegB, color);

		// ── 11. Propagate forward ─────────────────────────────────────────────
		Point  prevCenterA = bestSideA, prevCenterB = bestSideB;
		double radA = getEnvelopeRadius(newSegA), radB = getEnvelopeRadius(newSegB);
		boolean aAlive = true, bAlive = true;

		for (int f = splitFrame + 1; f < stack.getSize() && (aAlive || bAlive); f++) {
			ImageProcessor frameProc = stack.getProcessor(f + 1).duplicate();
			if (controller.getInvertIntensity()) frameProc.invert();
			new GaussianBlur().blurGaussian(frameProc, controller.getGaussianBlurSigma());
			double tol = computeTolerance(frameProc);

			if (aAlive) {
				Point maxA = findMaximaNearPoint(frameProc, prevCenterA, radA, tol);
				if (maxA != null) {
					Point neighborHint = bAlive ? prevCenterB : null;
					Segment segA = neighborHint != null
							? runSarnFromSeed(stack, maxA, f, sarn, neighborHint)
							: runSarnFromSeed(stack, maxA, f, sarn);
					segA.setLinkSet(linkSetA); linkSetA.add(segA);
					dataSet.getFrameSet(f).add(segA);
					addSegmentToOverlay(segA, color);
					prevCenterA = (segA.getCenterPoint() != null) ? segA.getCenterPoint() : maxA;
					radA = getEnvelopeRadius(segA);
				} else { aAlive = false; }
			}

			if (bAlive) {
				Point maxB = findMaximaNearPoint(frameProc, prevCenterB, radB, tol);
				if (maxB != null) {
					Point neighborHint = aAlive ? prevCenterA : null;
					Segment segB = neighborHint != null
							? runSarnFromSeed(stack, maxB, f, sarn, neighborHint)
							: runSarnFromSeed(stack, maxB, f, sarn);
					segB.setLinkSet(linkSetB); linkSetB.add(segB);
					dataSet.getFrameSet(f).add(segB);
					addSegmentToOverlay(segB, color);
					prevCenterB = (segB.getCenterPoint() != null) ? segB.getCenterPoint() : maxB;
					radB = getEnvelopeRadius(segB);
				} else { bAlive = false; }
			}
		}

		// ── 12. Propagate backward ────────────────────────────────────────────
		prevCenterA = bestSideA; prevCenterB = bestSideB;
		radA = getEnvelopeRadius(newSegA); radB = getEnvelopeRadius(newSegB);
		aAlive = true; bAlive = true;

		for (int f = splitFrame - 1; f >= 0 && (aAlive || bAlive); f--) {
			ImageProcessor frameProc = stack.getProcessor(f + 1).duplicate();
			if (controller.getInvertIntensity()) frameProc.invert();
			new GaussianBlur().blurGaussian(frameProc, controller.getGaussianBlurSigma());
			double tol = computeTolerance(frameProc);

			if (aAlive) {
				Point maxA = findMaximaNearPoint(frameProc, prevCenterA, radA, tol);
				if (maxA != null) {
					Point neighborHint = bAlive ? prevCenterB : null;
					Segment segA = neighborHint != null
							? runSarnFromSeed(stack, maxA, f, sarn, neighborHint)
							: runSarnFromSeed(stack, maxA, f, sarn);
					segA.setLinkSet(linkSetA); linkSetA.add(segA);
					dataSet.getFrameSet(f).add(segA);
					addSegmentToOverlay(segA, color);
					prevCenterA = (segA.getCenterPoint() != null) ? segA.getCenterPoint() : maxA;
					radA = getEnvelopeRadius(segA);
				} else { aAlive = false; }
			}

			if (bAlive) {
				Point maxB = findMaximaNearPoint(frameProc, prevCenterB, radB, tol);
				if (maxB != null) {
					Point neighborHint = aAlive ? prevCenterA : null;
					Segment segB = neighborHint != null
							? runSarnFromSeed(stack, maxB, f, sarn, neighborHint)
							: runSarnFromSeed(stack, maxB, f, sarn);
					segB.setLinkSet(linkSetB); linkSetB.add(segB);
					dataSet.getFrameSet(f).add(segB);
					addSegmentToOverlay(segB, color);
					prevCenterB = (segB.getCenterPoint() != null) ? segB.getCenterPoint() : maxB;
					radB = getEnvelopeRadius(segB);
				} else { bAlive = false; }
			}
		}

		// ── 13. Finish ────────────────────────────────────────────────────────
		splitTargetLinkSet  = null;
		splitFrame          = -1;
		awaitingSplitLine   = false;
		mouseListenerActive = true;

		imagePlus.getCanvas().repaintOverlay();
		panel.setModificationPanel();
		controller.setModifyData(runType, dataSet);
		controller.autosave();
	}

	// ── Split helpers ─────────────────────────────────────────────────────────

	/**
	 * Runs the given SARN method from a single seed point in a single frame,
	 * optionally with neighbor seed points to make the SARN neighbor-aware
	 * (preventing the envelope from expanding towards the neighbour).
	 * Creates a minimal 1-frame DataSet + ImageStack for efficiency.
	 *
	 * @param fullStack   the full image stack (not mutated)
	 * @param seed        the seed point (center of the new object)
	 * @param targetFrame 0-based frame index
	 * @param sarn        the SARN method to use (re-initialized internally)
	 * @param neighbors   optional additional seed points added as dummy neighbors
	 * @return a new Segment with the SARN external perimeter set, frame = targetFrame
	 */
	private Segment runSarnFromSeed(ImageStack fullStack, Point seed,
			int targetFrame, Sarn sarn, Point... neighbors) {
		// Build a 1-frame stack (avoids iterating all frames in sarn.run())
		ImageStack singleSlice = new ImageStack(fullStack.getWidth(), fullStack.getHeight());
		singleSlice.addSlice(fullStack.getProcessor(targetFrame + 1).duplicate());

		DataSet tempDS = new DataSet(fullStack.getWidth(), fullStack.getHeight(), 1);
		FrameSet fs    = new FrameSet(0, tempDS);
		tempDS.addFrameSet(fs, 0);

		// Primary seed
		Segment seedSeg = new Segment(0, new Point(seed.x, seed.y));
		fs.add(seedSeg);
		LinkSet tempLS = new LinkSet(tempDS);
		seedSeg.setLinkSet(tempLS);
		tempLS.add(seedSeg);

		// Neighbor seeds make SARN neighbor-aware (constrains the envelope)
		for (Point nb : neighbors) {
			if (nb == null) continue;
			Segment nbSeg = new Segment(0, new Point(nb.x, nb.y));
			fs.add(nbSeg);
			LinkSet nbLS = new LinkSet(tempDS);
			nbSeg.setLinkSet(nbLS);
			nbLS.add(nbSeg);
		}

		// Run SARN with a dummy (invisible) progress bar
		sarn.initialize(singleSlice, tempDS, new JProgressBar());
		sarn.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		sarn.setCleanupParams(controller.getSearchFraction(), controller.getSearchCeiling(), controller.getSimplificationEpsilon());
		sarn.run();

		// Build result Segment with the correct target frame
		Segment result = new Segment(targetFrame, new Point(seed.x, seed.y));
		result.setExternalPerimeter(seedSeg.getExternalPerimeter());
		return result;
	}

	/**
	 * Returns the nearest local intensity maxima to {@code center} within
	 * {@code searchRadius} pixels (Euclidean), above {@code tolerance}.
	 * Returns null if none is found.
	 */
	private Point findMaximaNearPoint(ImageProcessor blurredFrame, Point center,
			double searchRadius, double tolerance) {
		Polygon allMaxima = new ModifiedMaximumFinder().getMaxima(blurredFrame, tolerance, true);
		Point   nearest   = null;
		double  minDist   = Double.MAX_VALUE;
		for (int i = 0; i < allMaxima.npoints; i++) {
			double dx   = allMaxima.xpoints[i] - center.x;
			double dy   = allMaxima.ypoints[i] - center.y;
			double dist = java.lang.Math.sqrt(dx * dx + dy * dy);
			if (dist <= searchRadius && dist < minDist) {
				minDist = dist;
				nearest = new Point(allMaxima.xpoints[i], allMaxima.ypoints[i]);
			}
		}
		return nearest;
	}

	/**
	 * Estimates the SARN envelope radius of a segment from the bounding box
	 * of its external perimeter. Falls back to 5 × Gaussian sigma if no
	 * perimeter is available.
	 */
	private double getEnvelopeRadius(Segment seg) {
		Point[] perim = seg.getExternalPerimeter();
		if (perim == null || perim.length == 0)
			return controller.getGaussianBlurSigma() * 5.0;
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (Point p : perim) {
			if (p.x < minX) minX = p.x;  if (p.x > maxX) maxX = p.x;
			if (p.y < minY) minY = p.y;  if (p.y > maxY) maxY = p.y;
		}
		return java.lang.Math.max(maxX - minX, maxY - minY) / 2.0;
	}

	/**
	 * Computes the absolute MaximumFinder tolerance from a blurred frame by
	 * applying the controller's percentage tolerance to the frame's intensity range.
	 */
	private double computeTolerance(ImageProcessor blurredFrame) {
		int fMin = Integer.MAX_VALUE, fMax = Integer.MIN_VALUE;
		for (int x = 0; x < blurredFrame.getWidth(); x++) {
			for (int y = 0; y < blurredFrame.getHeight(); y++) {
				int v = blurredFrame.get(x, y);
				if (v < fMin) fMin = v;
				if (v > fMax) fMax = v;
			}
		}
		return controller.getMaximumFinderTolerance() * (fMax - fMin);
	}

	/**
	 * Creates a PolygonRoi from the segment's external perimeter, assigns it
	 * to the segment, and adds it to the overlay at the correct frame position.
	 */
	private void addSegmentToOverlay(Segment seg, Color c) {
		if (seg.getExternalPerimeter() == null) return;
		PolygonRoi roi = getPolygonRoi(seg.getExternalPerimeter());
		roi.setStrokeColor(c);
		roi.setStrokeWidth(2);
		roi.setPosition(seg.getFrame() + 1);
		seg.setRoi(roi);
		overlay.add(roi);
	}

	// ── Preferences ──────────────────────────────────────────────────────────

	/** Opens a colour chooser; applies the chosen colour to the draw tool outline and all non-selected overlay ROIs, and persists via Prefs. */
	public void changeRoiColor() {
		Color chosen = JColorChooser.showDialog(panel, "Choose ROI Outline Color", color);
		if (chosen == null) return;
		color = chosen;
		Prefs.set(PREF_ROI_COLOR, String.format("#%06x", chosen.getRGB() & 0xFFFFFF));
		ij.gui.Roi.setColor(color);
		for (LinkSet ls : dataSet.getLinkSetList()) {
			for (Segment s : ls) {
				if (s.getRoi() != null && !s.getRoiSelected())
					s.getRoi().setStrokeColor(color);
			}
		}
		imagePlus.updateAndDraw();
	}

	/** Toggles the drawing tool between polygon and freehand, persisting the choice via Prefs. */
	public void toggleDrawTool() {
		String next = "polygon".equals(Prefs.get(PREF_TOOL, "polygon")) ? "freehand" : "polygon";
		Prefs.set(PREF_TOOL, next);
		panel.updateDrawToolButton(next);
	}

	private static Color decodeColor(String hex) {
		try { return Color.decode(hex); }
		catch (NumberFormatException e) { return new Color(0, 255, 0); }
	}

	// ── Link command ─────────────────────────────────────────────────────────

	/**
	 * Merges exactly two selected LinkSets into one, ordered by frame number.
	 * Blocked if the two tracks have any overlapping frames.
	 *
	 * Selection: click one segment from each track (existing selectObject()
	 * highlights the whole LinkSet), then press Link.
	 */
	public void linkObject() {

		// ── 1. Collect the two selected LinkSets ──────────────────────────────
		LinkSet lsA = null, lsB = null;
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			if (dataSet.getFrameSet(i) == null) continue;
			for (Segment seg : dataSet.getFrameSet(i)) {
				if (!seg.getRoiSelected()) continue;
				LinkSet ls = seg.getLinkSet();
				if      (lsA == null)  { lsA = ls; }
				else if (ls == lsA)    { /* same LS, skip */ }
				else if (lsB == null)  { lsB = ls; }
				else if (ls == lsB)    { /* same LS, skip */ }
				else {
					panel.dialogAlert("More than two objects selected.\n"
							+ "Click exactly one segment from each of the two tracks to link.");
					return;
				}
			}
		}

		if (lsA == null || lsB == null) {
			panel.dialogAlert("Two tracks must be selected to link.\n"
					+ "Click one segment from each track, then press Link.");
			return;
		}

		// ── 2. Block overlapping frame ranges ─────────────────────────────────
		for (Segment sA : lsA) {
			for (Segment sB : lsB) {
				if (sA.getFrame() == sB.getFrame()) {
					panel.dialogAlert("The two selected tracks both have a segment in frame "
							+ (sA.getFrame() + 1) + ".\n"
							+ "Linking tracks with overlapping frames is not supported.");
					return;
				}
			}
		}

		// ── 3. Create merged LinkSet, sorted by frame ─────────────────────────
		LinkSet merged = new LinkSet(dataSet);
		merged.setName(dataSet.getLinkSetNameIterator());

		ArrayList<Segment> allSegs = new ArrayList<Segment>();
		for (Segment s : lsA) allSegs.add(s);
		for (Segment s : lsB) allSegs.add(s);
		allSegs.sort((a, b) -> Integer.compare(a.getFrame(), b.getFrame()));

		for (Segment seg : allSegs) {
			seg.setLinkSet(merged);
			merged.add(seg);
			seg.setRoiSelected(false);
			if (seg.getRoi() != null) seg.getRoi().setStrokeColor(color);
		}

		// ── 4. Remove the two original LinkSets ───────────────────────────────
		// Note: do NOT call deleteObject() — that removes segments from frameSets
		// and overlay. We only want to update the linkSetList.
		dataSet.getLinkSetList().remove(lsA);
		dataSet.getLinkSetList().remove(lsB);

		imagePlus.getCanvas().repaintOverlay();
		controller.setModifyData(runType, dataSet);
		controller.autosave();
	}

	// ── Unlink command ────────────────────────────────────────────────────────

	/**
	 * Splits the selected LinkSet at the current frame boundary:
	 * segments in frames ≤ current frame → LinkSet A,
	 * segments in frames >  current frame → LinkSet B.
	 *
	 * The selected LinkSet must have a segment in the current frame,
	 * and the current frame must not be the last frame of the track.
	 */
	public void unLinkObject() {

		int currentFrameIdx = imagePlus.getCurrentSlice() - 1;

		// ── 1. Identify the one selected LinkSet ──────────────────────────────
		LinkSet selectedLS = null;
		FrameSet fs = dataSet.getFrameSet(currentFrameIdx);
		if (fs == null) { panel.dialogAlert("No segments in this frame."); return; }

		for (Segment seg : fs) {
			if (seg.getRoiSelected()) {
				if (selectedLS == null) {
					selectedLS = seg.getLinkSet();
				} else if (selectedLS != seg.getLinkSet()) {
					panel.dialogAlert("More than one object selected in this frame.\n"
							+ "Click a single track segment, then press Unlink.");
					return;
				}
			}
		}
		if (selectedLS == null) {
			panel.dialogAlert("No object selected.\n"
					+ "Click a segment in the frame where you want to split the track, then press Unlink.");
			return;
		}

		// ── 2. Validate split position ────────────────────────────────────────
		boolean hasSegInFrame = false;
		int maxFrame = Integer.MIN_VALUE;
		for (Segment seg : selectedLS) {
			if (seg.getFrame() == currentFrameIdx) hasSegInFrame = true;
			if (seg.getFrame() > maxFrame) maxFrame = seg.getFrame();
		}
		if (!hasSegInFrame) {
			panel.dialogAlert("The selected track has no segment in the current frame.\n"
					+ "Navigate to a frame where the track exists, then press Unlink.");
			return;
		}
		if (currentFrameIdx >= maxFrame) {
			panel.dialogAlert("The current frame is the last frame of this track — nothing to split off.\n"
					+ "Select an earlier frame to unlink from.");
			return;
		}

		// ── 3. Split into two new LinkSets ────────────────────────────────────
		LinkSet lsA = new LinkSet(dataSet);  // frames ≤ currentFrameIdx
		lsA.setName(dataSet.getLinkSetNameIterator());
		LinkSet lsB = new LinkSet(dataSet);  // frames >  currentFrameIdx
		lsB.setName(dataSet.getLinkSetNameIterator());

		for (Segment seg : selectedLS) {
			if (seg.getFrame() <= currentFrameIdx) {
				seg.setLinkSet(lsA);
				lsA.add(seg);
			} else {
				seg.setLinkSet(lsB);
				lsB.add(seg);
			}
			seg.setRoiSelected(false);
			if (seg.getRoi() != null) seg.getRoi().setStrokeColor(color);
		}

		// ── 4. Remove original LinkSet (segments stay in frameSets + overlay) ─
		dataSet.getLinkSetList().remove(selectedLS);

		imagePlus.getCanvas().repaintOverlay();
		controller.setModifyData(runType, dataSet);
		controller.autosave();
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
