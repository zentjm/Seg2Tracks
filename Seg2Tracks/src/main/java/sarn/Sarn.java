package sarn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.JProgressBar;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 * Abstract base class implementing Segmentation Area Restriction by Neighborhood (SARN).
 * SARN defines local regions around fluorescent cells to compute local (per-cell) intensity
 * thresholds rather than a global threshold. Subclasses implement different boundary definition
 * and restriction-point-finding strategies. The framework processes image stacks frame-by-frame,
 * extracting perimeter points for each segmented cell.
 */
public abstract class Sarn {

	// Subclasses should override these to appear in the GUI method selector and help menu
	String name = "Method Name";
	String description = "How this Method Works";
	
	//Input parameters (no return objects, just add to segments)
	ImageStack inputStack;
	DataSet dataSet;
	GaussianBlur blurrer;
	double blurSigma;
	
	//Settings for import
	JProgressBar progressBar;

	// Recursive mode: frame→perimeter map defining the mask boundary (null in non-recursive runs)
	protected HashMap<Integer, Point[]> parentPerimeterMap;
	// Current frame index, set by run() before each outer-loop iteration
	protected int currentFrame;

	//Multiuse parameters
	ImageProcessor processor;
	FrameSet segments;
	
	/**
	 * Default constructor for SARN subclasses.
	 */
	public Sarn() {
	}

	/**
	 * Nested class pairing an inner contour point with an outer contour point.
	 * Used for drawing Bresenham paths along which intensity is sampled.
	 */
	class PointSet {
		Point innerPoint;
		Point outerPoint;

		/**
		 * Constructs a matched pair of inner and outer boundary points.
		 *
		 * @param innerPoint the cell interior point (typically the marker/center)
		 * @param outerPoint the corresponding outer boundary point
		 */
		PointSet (Point innerPoint, Point outerPoint) {
			this.innerPoint = innerPoint;
			this.outerPoint = outerPoint;
		}
	}

	/**
	 * Initializes the SARN processor with image data and UI components.
	 *
	 * @param imageStack the multi-frame image data to segment
	 * @param dataSet the hierarchical data structure (DataSet > LinkSet > FrameSet > Segment)
	 * @param progressBar the progress indicator for UI feedback
	 */
	public void initialize(ImageStack imageStack, DataSet dataSet, JProgressBar progressBar) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
		this.progressBar = progressBar;
	}

	/**
	 * Configures Gaussian blur parameters applied to the image during preprocessing.
	 *
	 * @param blurrer the GaussianBlur filter instance
	 * @param blurSigma the standard deviation (sigma) for the Gaussian kernel
	 */
	public void setBlur(GaussianBlur blurrer, double blurSigma) {
		this.blurrer = blurrer;
		this.blurSigma = blurSigma;
	}

	/**
	 * Sets the parent cell perimeter map for recursive segmentation mode.
	 * When set, SARN uses the farthest parent boundary point as the outer constraint
	 * and clips/stitches the resulting perimeter to the parent boundary.
	 *
	 * @param parentPerimeterMap map from 0-based frame index to the parent cell's perimeter
	 *                           (the same perimeter used to build the masked stack)
	 */
	public void setParentPerimeterMap(HashMap<Integer, Point[]> parentPerimeterMap) {
		this.parentPerimeterMap = parentPerimeterMap;
	}

	/**
	 * Returns the method name for GUI menu display.
	 *
	 * @return the descriptive name of this SARN method
	 */
	public String toString() {
		return name;
	}

	/**
	 * Returns the help-menu description of this SARN method.
	 *
	 * @return a description of how this method works
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Finds the initial inner boundary points for a given segment.
	 * In OWC, this returns the cell marker; other methods may return multiple hull points.
	 *
	 * @param currentSegment the index of the segment in the current frame
	 * @return array of points defining the inner contour
	 */
	abstract Point[] innerPoints (int currentSegment);

	/**
	 * Finds the outer boundary constraint for a given inner point.
	 * In OWC, this returns a single point at the edge or nearest neighbor; other methods may return a hull.
	 *
	 * @param innerPoints the inner contour point(s)
	 * @return array of point(s) defining the outer constraint
	 */
	abstract Point[] outerPoints (Point [] innerPoints);

	/**
	 * Generates refined inner boundary points.
	 * In OWC, this is a pass-through (returns innerPts); other methods may compute a boundary polygon.
	 *
	 * @param innerPts the initial inner points
	 * @param outerPts the outer constraint points
	 * @return the final inner boundary points
	 */
	abstract Point[] innerBounds (Point [] innerPts, Point [] outerPts);

	/**
	 * Generates the outer boundary points.
	 * In OWC, this generates a circle; other methods may compute a convex hull or other shape.
	 *
	 * @param innerBounds the final inner boundary points
	 * @param outerPts the outer constraint points
	 * @return array of points forming the outer boundary
	 */
	abstract Point[] outerBounds (Point[] innerBounds, Point[] outerPts);

	/**
	 * Matches each inner boundary point to a corresponding outer boundary point.
	 * Establishes the pairings used by the Bresenham line-drawing algorithm.
	 *
	 * @param innerBounds the final inner boundary points
	 * @param outerBounds the final outer boundary points
	 * @return array of PointSet pairs for intensity sampling
	 */
	abstract PointSet[] boundaryMatch (Point [] innerBounds, Point[] outerBounds);

	/**
	 * Determines the restriction point (threshold) along a Bresenham line.
	 * Typically returns the darkest (lowest intensity) pixel, but can implement more sophisticated thresholding.
	 *
	 * @param pts array of points along the Bresenham line from inner to outer
	 * @return the point where foreground transitions to background
	 */
	abstract Point getThreasholdPoint (Point[] pts);

	/**
	 * Main segmentation loop: iterates through all frames and cells, computing the refined cell SARN area.
	 * For each cell: (1) defines inner and outer contours, (2) matches them into Bresenham paths,
	 * (3) samples intensity along each path to find the restriction point, (4) applies geometric refinements,
	 * (5) stores the final SARN area in the segment.
	 */
	public void run() {
		progressBar.setString("Segmentation");
		// Iterate over all frames in the image stack
		for (int i = 0; i < inputStack.size(); i ++) {
			currentFrame = i;
			// Duplicate so blur does not mutate the stored in-memory stack
			processor = inputStack.getProcessor(i+1).duplicate();
			// Apply Gaussian blur to improve threshold stability
			blurrer.blurGaussian(processor, blurSigma);
			// Get all segments (cells) in the current frame
			segments = dataSet.getFrameSet(i);
			// Process each segment in the frame
			for (int n = 0; n < segments.size(); n++) {
				// Step 1: Establish initial inner and outer reference points.
				// In recursive mode: use nearest other void center as the outer constraint,
				// analogous to how non-recursive SARN uses the nearest neighboring cell center.
				// If this is the only void in the parent (no sibling voids), fall back to the
				// farthest parent perimeter point, analogous to the image-edge fallback in
				// non-recursive mode. clip+stitch (step 5) contains the final perimeter.
				Point [] innerPoints = innerPoints(n);
				Point [] outerPoints;
				if (parentPerimeterMap != null) {
					Point nearestVoid = getNearestVoidCenter(innerPoints[0], n);
					if (nearestVoid != null) {
						outerPoints = new Point[]{ nearestVoid };
					} else {
						Point parentPt = getParentOuterPoint(innerPoints[0].x, innerPoints[0].y);
						outerPoints = (parentPt != null) ? new Point[]{ parentPt } : outerPoints(innerPoints);
					}
				} else {
					outerPoints = outerPoints(innerPoints);
				}
				// Step 2: Generate and clean boundary contours
				Point [] innerBoundary = clean(innerBounds(innerPoints, outerPoints), segments.get(n));
				Point [] outerBoundary = clean(outerBounds(innerBoundary, outerPoints), segments.get(n));
				// Step 3: Match pairs and sample intensity along Bresenham lines
				PointSet [] matchedPoints = boundaryMatch(innerBoundary, outerBoundary);
				// Step 3.5 (recursive mode only): clip each matched outer point to 1 pixel
				// inside the parent perimeter. This ensures contractor() operates within
				// the masked region and the 1-pixel inset prevents getThreasholdPoint() from
				// finding boundary-adjacent pixels (darkened by Gaussian blur zero-bleed)
				// as the threshold point, which would place the SARN boundary at the parent
				// perimeter instead of the void wall.
				if (parentPerimeterMap != null) {
					Point[] parentPerim3 = parentPerimeterMap.get(currentFrame);
					if (parentPerim3 != null && parentPerim3.length > 0) {
						int[] ppx3 = new int[parentPerim3.length], ppy3 = new int[parentPerim3.length];
						for (int j = 0; j < parentPerim3.length; j++) { ppx3[j] = parentPerim3[j].x; ppy3[j] = parentPerim3[j].y; }
						PolygonRoi parentRoi3 = new PolygonRoi(ppx3, ppy3, parentPerim3.length, Roi.POLYGON);
						for (int pi = 0; pi < matchedPoints.length; pi++) {
							if (!parentRoi3.contains(matchedPoints[pi].outerPoint.x, matchedPoints[pi].outerPoint.y)) {
								Point[] line3 = bresenham(matchedPoints[pi].innerPoint, matchedPoints[pi].outerPoint);
								for (int li = line3.length - 1; li >= 0; li--) {
									if (parentRoi3.contains(line3[li].x, line3[li].y)) {
										// Inset 1 pixel from boundary so contractor finds threshold
										// points inside the polygon, not on the edge where contains()
										// is unreliable after geometric refinements.
										int insetIdx = Math.max(0, li - 1);
										matchedPoints[pi] = new PointSet(matchedPoints[pi].innerPoint, line3[insetIdx]);
										break;
									}
								}
							}
						}
					}
				}
				// Step 4: Find restriction points
				Point[] contractorResult = contractor(matchedPoints);
				Point[] perimeter =
						GeometricCalculations.straightPerimeter(
						GeometricCalculations.shortcutPerimeter(
						GeometricCalculations.straightPerimeter(
						contractorResult)));
				// Step 5 (recursive mode only): clip perimeter to parent boundary and stitch
				// gaps. shortcutPerimeter() can create chords that exit the parent perimeter
				// for non-convex cells (e.g. compressed toward a nearest neighbour).
				if (parentPerimeterMap != null) {
					Point[] parentPerim = parentPerimeterMap.get(currentFrame);
					if (parentPerim != null && parentPerim.length > 0) {
						perimeter = clipAndStitch(perimeter, parentPerim, innerBoundary[0]);
					}
				}
				segments.get(n).setExternalPerimeter(perimeter);	
			}
			// Update progress bar with current frame index
			progressBar.setValue(i);
		}
	}

	/**
	 * Cleans the boundary point list by clamping out-of-bounds points and removing duplicates.
	 * Also detects and records whether the boundary touches the image edge.
	 *
	 * @param ptsList the boundary points to clean
	 * @param segment the segment object to store boundary-contact status
	 * @return the cleaned point array (or original if deduplication fails)
	 */
	public Point[] clean (Point[] ptsList, Segment segment) {

		ArrayList<Point> ptsArrayList = new ArrayList<Point>();

		boolean contact = false;

		// Clamp all points to valid image coordinates, detect boundary contact, and populate list
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x < 0) ptsList[i].x = 0;
			if (ptsList[i].x > processor.getWidth() - 1) ptsList[i].x = processor.getWidth() - 1;
			if (ptsList[i].y < 0) ptsList[i].y = 0;
			if (ptsList[i].y > processor.getHeight() - 1) ptsList[i].y = processor.getHeight() - 1;

			// Detect boundary contact after clamping: a clamped point sitting exactly on an edge means contact
			if (ptsList[i].x == 0 || ptsList[i].x == processor.getWidth() - 1) contact = true;
			if (ptsList[i].y == 0 || ptsList[i].y == processor.getHeight() - 1) contact = true;

			ptsArrayList.add(ptsList[i]);
		}

		//System.out.println("Segment contact: " + contact);
		segment.setExternalBoundaryContact(contact);

		// Remove duplicate points using a LinkedHashSet to preserve insertion order
		Set<Point> set  = new LinkedHashSet<Point>(ptsArrayList);
		ptsArrayList.clear();
		ptsArrayList.addAll(set);

		// Reconstruct points list as array
		Point[] ptsList2 = new Point[ptsArrayList.size()];
		for (int i = 0; i < ptsArrayList.size(); i++) {
			ptsList2[i] = ptsArrayList.get(i);
		}
		return ptsList2;

	

	

	}
	


	/**
	 * Returns the farthest point on the parent mask boundary for the current frame.
	 * Using the farthest point ensures the SARN circle radius is large enough to enclose the
	 * full void interior even when the void center is close to one side of the parent boundary.
	 * The oversized perimeter is trimmed afterward by clipAndStitch().
	 *
	 * @param x x-coordinate of the void center
	 * @param y y-coordinate of the void center
	 * @return the farthest parent perimeter point, or null if no parent map / no entry this frame
	 */
	protected Point getParentOuterPoint(int x, int y) {
		if (parentPerimeterMap == null) return null;
		Point[] perimeter = parentPerimeterMap.get(currentFrame);
		if (perimeter == null || perimeter.length == 0) return null;
		Point farthest = perimeter[0];
		double maxDist = -1;
		for (Point p : perimeter) {
			double d = Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2);
			if (d > maxDist) { maxDist = d; farthest = p; }
		}
		return farthest;
	}

	/**
	 * Returns the center point of the nearest other void (segment) in the current frame,
	 * excluding the segment at currentSegmentIndex. Used in recursive mode to bound the
	 * SARN outer constraint to the nearest sibling void, analogous to how non-recursive
	 * SARN uses the nearest neighboring cell center.
	 * Returns null if this is the only void in the frame (no sibling voids exist).
	 *
	 * @param voidCenter          center of the current void segment
	 * @param currentSegmentIndex index of the current segment (to exclude self)
	 * @return center of the nearest sibling void, or null if none exists
	 */
	protected Point getNearestVoidCenter(Point voidCenter, int currentSegmentIndex) {
		if (segments == null || segments.size() <= 1) return null;
		Point nearest = null;
		double minDist = Double.MAX_VALUE;
		for (int i = 0; i < segments.size(); i++) {
			if (i == currentSegmentIndex) continue;
			Point other = segments.get(i).getCenterPoint();
			double d = Math.pow(other.x - voidCenter.x, 2) + Math.pow(other.y - voidCenter.y, 2);
			if (d < minDist) { minDist = d; nearest = other; }
		}
		return nearest;
	}

	/**
	 * Clips a SARN-generated perimeter to the parent cell boundary and stitches gaps.
	 * Points outside the parent boundary are replaced by the parent perimeter arc between
	 * the nearest parent vertices at the exit and entry crossing points.
	 *
	 * @param sarnPerim   perimeter produced by SARN (may extend outside the parent boundary)
	 * @param parentPerim parent cell boundary (the same perimeter used to build the masked stack)
	 * @return a closed perimeter clipped to the parent boundary with gaps stitched
	 */
	protected Point[] clipAndStitch(Point[] sarnPerim, Point[] parentPerim, Point voidCenter) {
		if (sarnPerim == null || sarnPerim.length == 0) return sarnPerim;

		// Build parent ROI for point-in-polygon test
		int[] px = new int[parentPerim.length], py = new int[parentPerim.length];
		for (int i = 0; i < parentPerim.length; i++) { px[i] = parentPerim[i].x; py[i] = parentPerim[i].y; }
		PolygonRoi parentRoi = new PolygonRoi(px, py, parentPerim.length, Roi.POLYGON);

		ArrayList<Point> result = new ArrayList<>();
		boolean prevInside = parentRoi.contains(sarnPerim[sarnPerim.length - 1].x,
		                                         sarnPerim[sarnPerim.length - 1].y);
		int exitIdx = -1; // parent perimeter index nearest to the last exit point

		for (int i = 0; i < sarnPerim.length; i++) {
			boolean inside = parentRoi.contains(sarnPerim[i].x, sarnPerim[i].y);

			if (inside && prevInside) {
				result.add(sarnPerim[i]);

			} else if (!inside && prevInside) {
				// Exit: snap to nearest parent vertex at the last inside point
				int prev = (i - 1 + sarnPerim.length) % sarnPerim.length;
				exitIdx = nearestParentVertex(sarnPerim[prev], parentPerim);

			} else if (inside && !prevInside) {
				// Entry: stitch parent arc from exitIdx to nearest parent vertex of this point
				if (exitIdx >= 0) {
					int entryIdx = nearestParentVertex(sarnPerim[i], parentPerim);
					stitchArc(parentPerim, exitIdx, entryIdx, result, voidCenter);
				}
				result.add(sarnPerim[i]);
			}
			// outside && !prevInside: still outside — skip

			prevInside = inside;
		}

		// If all points were inside, result is already complete.
		// If the perimeter started and ended outside, result may be empty — fall back to parent.
		if (result.isEmpty()) {
			ArrayList<Point> fallback = new ArrayList<>();
			for (Point p : parentPerim) fallback.add(p);
			return fallback.toArray(new Point[0]);
		}

		return result.toArray(new Point[0]);
	}

	private int nearestParentVertex(Point p, Point[] parentPerim) {
		int nearest = 0;
		double minDist = Double.MAX_VALUE;
		for (int i = 0; i < parentPerim.length; i++) {
			double d = Math.pow(parentPerim[i].x - p.x, 2) + Math.pow(parentPerim[i].y - p.y, 2);
			if (d < minDist) { minDist = d; nearest = i; }
		}
		return nearest;
	}

	private void stitchArc(Point[] parentPerim, int from, int to, ArrayList<Point> result, Point voidCenter) {
		int n = parentPerim.length;
		int fwd = (to - from + n) % n;
		int bwd = (from - to + n) % n;

		// Choose the arc whose midpoint is farthest from the void center.
		// For non-convex parent perimeters (e.g. compressed toward the nearest neighbour cell),
		// the shorter-vertex-count arc may cut through a concavity rather than follow the outer
		// boundary. The arc whose midpoint is farther from the void center is the one going
		// around the outside of the gap.
		int fwdMidIdx = ((from + fwd / 2) % n + n) % n;
		int bwdMidIdx = ((from - bwd / 2 + n) % n + n) % n;
		double fwdDist = Math.pow(parentPerim[fwdMidIdx].x - voidCenter.x, 2)
		               + Math.pow(parentPerim[fwdMidIdx].y - voidCenter.y, 2);
		double bwdDist = Math.pow(parentPerim[bwdMidIdx].x - voidCenter.x, 2)
		               + Math.pow(parentPerim[bwdMidIdx].y - voidCenter.y, 2);

		int step  = (fwdDist >= bwdDist) ? 1 : -1;
		int count = (fwdDist >= bwdDist) ? fwd : bwd;
		for (int k = 0; k <= count; k++) {
			int idx = ((from + k * step) % n + n) % n;
			result.add(parentPerim[idx]);
		}
	}

	/**
	 * Applies the core SARN restriction point algorithm: for each matched inner-outer pair,
	 * draws a Bresenham line and samples intensity to find the transition from foreground to background.
	 *
	 * @param matchedPoints array of inner-outer point pairs to process
	 * @return array of restriction points (one per matched pair)
	 */
	protected Point[] contractor (PointSet[] matchedPoints) {

		// Build list of restriction points by sampling along each Bresenham line
		LinkedList<Point> ptsLinkedList = new LinkedList<>();
		Point[] line;
		for (int n = 0; n < matchedPoints.length; n ++) {
			// Draw line from inner to outer point using Bresenham's algorithm
			line = bresenham (matchedPoints[n].innerPoint, matchedPoints[n].outerPoint);
			// Find the threshold point (transition from dark/foreground to bright/background)
			ptsLinkedList.add(getThreasholdPoint(line));
		}

		// Convert LinkedList to Point array for return
		Point[] ptsList = new Point[ptsLinkedList.size()];
		for (int i = 0; i < ptsLinkedList.size(); i++) {
			ptsList[i] = ptsLinkedList.get(i);
		}
		return ptsList;
	}
	
	
	//private static int[][] bresenham(int x,int y,int x2, int y2, int radius) {
	/**
	 * Bresenham line-drawing algorithm: generates all points along a line from innerPt to outerPt.
	 * Used for sampling pixel intensities between the inner and outer contours.
	 *
	 * @param innerPt the starting point (cell interior/marker)
	 * @param outerPt the ending point (cell exterior/boundary)
	 * @return array of all integer coordinates along the line
	 */
	protected static Point[] bresenham(Point innerPt, Point outerPt) {
	
		LinkedList<Point> line = new LinkedList<>();
		
		//x = centerpoint.x
		//x2 = centerpoint outer
		int x = innerPt.x;
		int y = innerPt.y;
		int w = outerPt.x - x;		  // width (delta-x)
	    int h = outerPt.y - y;		  // height (delta-y)
	    int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
	    if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
	    if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
	    if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
	    int longest = Math.abs(w) ;
	    int shortest = Math.abs(h) ;
	    if (!(longest>shortest)) {
	        longest = Math.abs(h) ;
	        shortest = Math.abs(w) ;
	        if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
	        dx2 = 0 ;            
	    }
	    int numerator = longest >> 1 ;
	    for (int i=0;i<=longest;i++) {
	        //adds to array
	    	line.add(new Point(x, y));
	        numerator += shortest ;
	        if (!(numerator<longest)) {
	            numerator -= longest ;
	            x += dx1 ;
	            y += dy1 ;
	        } else {
	            x += dx2 ;
	            y += dy2 ;
	        }
	    }
	    Point [] linePts = new Point[line.size()];
	    for (int i = 0; i < line.size(); i ++) {
	    	linePts[i] = line.get(i);
	    }
	    return linePts;
	}
	
	
	//TODO Gives info to the progress bar. 
	/**
	 * Updates the progress bar with current segmentation status.
	 * Currently not implemented (stub method).
	 */
	public void updateStatus() {
		
	}
	
	
	/**
	 * NEIGHBOR CONSOLIDATION
	 * (consider where this should be placed)
	 * 
	 * Also figure out how to determine if segments should be merged, or readjusted. 
	 * 
	 * If above trigger --> Assess overlap area
	 * 		--> if a dip found --> reassess borders
	 * 		--> if no dip found --> automatically merge
	 * 
	 * 
	 * 
	 */

	//Inputs: blured processor, cell1, cell2
	public FrameSet consolidate(ImageProcessor processor, FrameSet segments) {
		
		//TODO: make adjustable
		double overlapTrigger = 0.2;
		
		
		//initialize
		Segment segA;
		Segment segB;
		ArrayList<Point> overlap = new ArrayList<Point>();
	
		ShapeRoi segAshape;
		ShapeRoi segBshape; 
		//iterate all segments
		for (int i = 0; i < segments.size(); i++) {
			for (int j = i; j < segments.size(); j++) { //TODO: could probably just use closest
				segA = segments.get(i);
				segB = segments.get(j);
				
				//create overlap segment. 
				segAshape = new ShapeRoi(getPolygonRoi(segA.getExternalPerimeter()).getPolygon());
				segBshape = new ShapeRoi(getPolygonRoi(segB.getExternalPerimeter()).getPolygon());
				ShapeRoi segAOnly = (ShapeRoi) segAshape.clone();
				ShapeRoi segBOnly = (ShapeRoi) segBshape.clone();
				Roi intersection = segAshape.and(segBshape); 
				
				//compare intersection area to the area of each shape. 
				double overlapPix = intersection.size() / (segAOnly.size() + segBOnly.size() - intersection.size());
				if (overlapPix < overlapTrigger) continue;
					
				//generate curvature operations. 
				
				
	
			}	
		}
		return null;
	}
	
	
	//TODO: duplicate from class AnalysisMethod
	public final PolygonRoi getPolygonRoi(Point[] pointList) {
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
	}
		

	/**
	 * READJUST THIS
	 * (consider where this should be placed)
	 * 
	 * Also figure out how to determine if segments should be merged, or readjusted. 
	 * 
	 * If above trigger --> Assess overlap area
	 * 		--> if a dip found --> reassess borders
	 * 		--> if no dip found --> automatically merge
	 * 
	 * 
	 * 
	 */
	
	
	
	
	
	

	/**
	 * Methods required: 
	 * 1. getBlur() - Blurring parameters used for the guassian blur of the image. Default are those used for segmentation. If modifying, 
	 * 			calculate relative to the selections used for the ID - do not hard code.  
	 * 2. getPerimeter() - gives an ordered list of the object perimeter.  
	 * 3. 
	 */
	
	
	
	

	
	
	
	
}
