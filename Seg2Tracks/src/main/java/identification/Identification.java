package identification;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import java.util.HashMap;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedMaximumFinder;
import geometricTools.PolarPoint;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 * Detects cell point markers in fluorescent microscopy images by applying Gaussian blur
 * and finding local intensity maxima above a user-defined threshold. Includes optional
 * filtering to remove low-intensity detections based on kernel intensity analysis.
 */
public class Identification {

	// Name and description displayed in the user interface
	String name = "Method Name";
	String description = "How this Method Works";
		
	// Input parameters (no return objects, just add to segments)
	ImageStack inputStack; // The image sequence to analyze
	DataSet dataSet; // Container for all detection results and metadata
	GaussianBlur blurrer; // Pre-processing filter to reduce noise
	double blurSigma; // Standard deviation of Gaussian blur kernel
	ModifiedMaximumFinder finder; // ImageJ tool to locate local intensity peaks
	double percentThreashold; // Threshold for filtering maxima (0.0 to 1.0, fraction of intensity range)
	int kernelSize; // Radius of square neighborhood for intensity averaging
	// In recursive mode: frame→perimeter map for bounding the maxima scan to each parent cell
	HashMap<Integer, Point[]> recursionPerimeterMap = null;

	// Status tracking UI
	JProgressBar progressBar; // Visual feedback for processing progress

	/**
	 * Initialize detector without progress bar. Sets kernel size to 3 pixels.
	 * @param imageStack Time-lapse image sequence to analyze
	 * @param dataSet Container to receive detected segments
	 */
	public void initialize(ImageStack imageStack, DataSet dataSet) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
		kernelSize = 3;
	}
	
	/**
	 * Initialize detector with progress bar. Sets kernel size to 3 pixels.
	 * @param imageStack Time-lapse image sequence to analyze
	 * @param dataSet Container to receive detected segments
	 * @param progressBar UI progress bar for user feedback
	 */
	public void initialize(ImageStack imageStack, DataSet dataSet, JProgressBar progressBar) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
		this.progressBar = progressBar;
		kernelSize = 3;
	}
	
	/**
	 * Configure Gaussian blur for noise reduction.
	 * @param blurrer ImageJ GaussianBlur filter instance
	 * @param blurSigma Standard deviation in pixels; higher = more blur
	 */
	public void setBlur(GaussianBlur blurrer, double blurSigma) {
		this.blurrer = blurrer;
		this.blurSigma = blurSigma;
	}
	
	/**
	 * Configure peak detection threshold.
	 * @param finder ImageJ MaximumFinder instance for locating intensity maxima
	 * @param finderTolerance Fraction of image intensity range (0.0–1.0) for filtering; higher = fewer detections
	 */
	public void setFinder(ModifiedMaximumFinder finder, double finderTolerance) {
		this.finder = finder;
		this.percentThreashold = finderTolerance;
	}

	/**
	 * Configure recursive mode: restrict the maxima scan to each parent cell's perimeter
	 * and exclude candidate maxima at the masked boundary.
	 * @param map frame (0-based) → parent cell internal perimeter points; pass null to clear
	 */
	public void setRecursionPerimeterMap(HashMap<Integer, Point[]> map) {
		this.recursionPerimeterMap = map;
	}
	
	/**
	 * Process entire image stack to detect cell markers. For each frame:
	 * applies Gaussian blur, finds local intensity maxima, filters by threshold,
	 * and creates Segment objects at detected positions.
	 */
	public void run() {
		progressBar.setString("Identification");
		ImageProcessor tempProcessor;
		
		Polygon poly; // Array of (x, y) coordinates for detected maxima
		FrameSet frameSet; // Container for all detections in one time frame
		
		
		for (int i = 0; i < inputStack.size(); i++) {
			frameSet = new FrameSet(i, dataSet);
			// Duplicate so blur does not mutate the stored in-memory stack
			tempProcessor = inputStack.getProcessor(i+1).duplicate();
			
			
			
			
			
			// Apply Gaussian blur to reduce noise before peak detection
			blurrer.blurGaussian(tempProcessor, blurSigma);
			// In recursive mode: set the parent cell ROI so the scan is bounded to the
			// parent perimeter and boundary pixels are treated as edge pixels.
			if (recursionPerimeterMap != null) {
				Point[] perim = recursionPerimeterMap.get(i);
				if (perim != null && perim.length > 0) {
					int[] rx = new int[perim.length], ry = new int[perim.length];
					for (int p = 0; p < perim.length; p++) { rx[p] = perim[p].x; ry[p] = perim[p].y; }
					finder.setRecursionParentRoi(new PolygonRoi(rx, ry, perim.length, Roi.POLYGON));
				} else {
					finder.setRecursionParentRoi(null); // frame has no parent segment; scan will find nothing useful
				}
			}
			
			
			
			// In recursive mode, compute tolerance as 10% of the image intensity range so
			// analyzeAndMarkMaxima can distinguish separate void peaks even when the blurred
			// image contains a connected plateau of equal-valued pixels.
			// TODO: expose this as a user-overridable parameter in Object ID Settings (pre-v1.0).
			// For non-recursive mode, keep tolerance=0 (existing behaviour for primary segmentation).
			double tolerance = 0;
			if (recursionPerimeterMap != null) {
				float gMin = Float.MAX_VALUE, gMax = -Float.MAX_VALUE;
				for (int px = 0; px < tempProcessor.getWidth(); px++) {
					for (int py = 0; py < tempProcessor.getHeight(); py++) {
						float v = tempProcessor.getPixelValue(px, py);
						if (v < gMin) gMin = v;
						if (v > gMax) gMax = v;
					}
				}
				tolerance = (gMax - gMin) * 0.1;
			}

			// Find all local maxima. Tolerance is 0 in primary mode; image-range-derived in
			// recursive mode. percentThreashold feeds filterLowPoints only (separate concern).
			poly = finder.getMaxima(tempProcessor, tolerance, true);

			// Filter detections by kernel intensity
			poly = filterLowPoints(poly, tempProcessor);

			//System.out.println("found maxpoints of " + i +", poly number of points: " + poly.npoints);
			// Create a Segment (marker) for each detected point
			for (int j = 0; j < poly.npoints; j++) {
				frameSet.add(new Segment(i, new Point(poly.xpoints[j], poly.ypoints[j])));
			}
			dataSet.addFrameSet(frameSet, i);
			progressBar.setValue(i);
		}
	}
	

	/**
	 * Refine manually edited segments by re-detecting peaks and adjusting
	 * center points to best match the manually drawn outlines. Only runs if user
	 * has edited the segmentation manually.
	 */
	public void runManualAdjustment() {
		if (dataSet.getManuallyEdited() == false) return;
		//System.out.println("Running manual adjustment");
		ImageProcessor tempProcessor;
		Polygon poly;
		for (FrameSet frameSet : dataSet.getFrameSetList()) {
			// Duplicate so blur does not mutate the stored in-memory stack
			tempProcessor = inputStack.getProcessor(frameSet.getFrame()+1).duplicate();
			// Re-detect peaks on the same frame
			blurrer.blurGaussian(tempProcessor, blurSigma);
			poly = finder.getMaxima(tempProcessor, 0, true);
			//System.out.println("found maxpoints of " + frameSet.getFrame() +", poly number of points: " + poly.npoints);
			// Adjust each manually-edited segment's center point to best detected peak
			adjustCenterPoint(frameSet, poly, tempProcessor);
		}
	}
	
	
	// DEAD CODE: Old SwingWorker-based threaded implementation. Replaced by standard run() method.
	// Kept for historical reference but not used; consider removing if method signature no longer needed.
	/*
	public SwingWorker runThread() {
		return new SwingWorker<Void, Integer>() {
			@Override
			public Void doInBackground() {
				ImageProcessor tempProcessor;
				Polygon poly;
				FrameSet frameSet;
				for (int i = 0; i < inputStack.size(); i ++) {
					frameSet = new FrameSet(i, dataSet);
					tempProcessor = inputStack.getProcessor(i+1);
					blurrer.blurGaussian(tempProcessor, blurSigma);
					poly = finder.getMaxima(tempProcessor, percentThreashold, true);


					//System.out.println("found maxpoints of " + i +", poly number of points: " + poly.npoints);
					for (int j = 0; j < poly.npoints; j ++) {
						frameSet.add(new Segment(i, new Point(poly.xpoints[j], poly.ypoints[j])));
					}
					dataSet.addFrameSet(frameSet, i);
					setProgress(i);
				}
				return null;
			}
		};
	}
	*/

	
	
	/**
	 * Filter detections by local intensity. Three steps:
	 * (1) Compute average intensity around each detection using square kernel
	 * (2) Determine threshold as percentThreashold of image min-max range
	 * (3) Keep only detections above threshold.
	 *
	 * @param poly Polygon of detected maxima points
	 * @param tempProcessor Image data (after blur) to measure intensities
	 * @return New Polygon containing only points with sufficient kernel intensity
	 */
	public Polygon filterLowPoints(Polygon poly, ImageProcessor tempProcessor) {
		
		// Clamp threshold to valid range [0, 1]
		if (percentThreashold < 0) percentThreashold = 0;
		if (percentThreashold > 1) percentThreashold = 1;

		int[] intensities = new int[poly.npoints]; // Average intensity around each point

		// Step 1: Collect kernel-averaged intensities for all points
		for (int i = 0; i < poly.npoints; i++) {

			int intensity = 0;
			int count = 0;
			boolean edge = false;

			// Sum pixel values in square neighborhood
			for (int j = -kernelSize; j <= kernelSize; j++) {
				if (poly.xpoints[i] + j > tempProcessor.getWidth() - 1 || poly.xpoints[i] + j < 0) {
					//System.out.println("X overreach");
					edge = true;
					continue;
				}
				for (int k = -kernelSize; k <= kernelSize; k++) {
					if (poly.ypoints[i] + k > tempProcessor.getHeight() - 1 || poly.ypoints[i] + k < 0) {
						//System.out.println("Y overreach");
						edge = true;
						continue;
					}
					intensity += tempProcessor.get(poly.xpoints[i] + j, poly.ypoints[i] + k);
					count++;
				}
			}
			// Mark edge detections as invalid
			if (edge == true) intensity = -1;
			intensities[i] = intensity/count;
		}
		
		// Step 2: Find global min/max intensity in image to define threshold
		int temp;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		// Scan entire image for min/max
		for (int i = 0; i < tempProcessor.getWidth(); i++) {
			for (int j = 0; j < tempProcessor.getHeight(); j++) {
				temp = tempProcessor.get(i, j);
				if (temp < min) min = temp;
				if (temp > max) max = temp;
			}
		}

		// Convert percentile threshold to absolute intensity value
		int threashold = (int) (min + ((max - min) * percentThreashold));

		// Step 3: Filter points into new polygon
		Polygon newPoly = new Polygon();
		for (int i = 0; i < poly.npoints; i++) {
			// Keep point if kernel intensity exceeds threshold
			if (intensities[i] > threashold)
				newPoly.addPoint(poly.xpoints[i], poly.ypoints[i]);
		}
		return newPoly;	
	}
	
	
	
	/**
	 * Refine center points of manually edited segments by finding the best-matching
	 * detected peak within each segment's perimeter, or picking the brightest peak
	 * within the segment area if no detection matches the perimeter exactly.
	 *
	 * @param frameSet Container of segments for this frame
	 * @param pts Polygon of all re-detected maxima in this frame
	 * @param tempProcessor Image data for measuring intensity
	 */
	public void adjustCenterPoint(FrameSet frameSet, Polygon pts, ImageProcessor tempProcessor) {
		
		for (Segment segment : frameSet) {
			// Skip segments not manually edited by user
			if (!segment.isManuallyEdited()) {
				continue;
			}
			// Collect all re-detected peaks that fall within this segment's outline
			ArrayList<Point> matches = new ArrayList<Point>();
			Roi poly = GeometricCalculations.getPolygonRoi(segment.getExternalPerimeter());
			for (int i = 0; i < pts.npoints; i++) {
				if (poly.contains(pts.xpoints[i], pts.ypoints[i])) {
					matches.add(new Point(pts.xpoints[i], pts.ypoints[i]));
				}
			}

			Point centerPoint = null;

			// Case 1: No detected peak falls on the perimeter — find brightest point in entire segment area
			if (matches.size() == 0) {
				//System.out.println("No identification match");

				int maxIntensity = 0;
				Point areaPointList[] = GeometricCalculations.getAreaByRoi(segment.getExternalPerimeter());
				for (int i = 0; i < areaPointList.length; i++) {
					int tempIntensity = kernelIntensity(areaPointList[i], tempProcessor);
					if (tempIntensity > maxIntensity) {
						maxIntensity = tempIntensity;
						centerPoint = areaPointList[i];
					}
				}
			}

			// Case 2: Multiple detected peaks match — pick the brightest one
			else if (matches.size() > 1) {
				//System.out.println("More than one identification match");
				int intensity = 0;
				for (Point pt: matches) {
					int tempIntensity = kernelIntensity(pt, tempProcessor);
					if (tempIntensity > intensity) {
						intensity = tempIntensity;
						centerPoint = pt;
					}
				}
			}
			// Case 3: Exactly one detected peak matches the perimeter
			else {
				//System.out.println("One-to-one identification match");
				centerPoint = matches.get(0);
			}
			//System.out.println("Old centerpoint  x:" + segment.getCenterPoint().x + "    y:" + segment.getCenterPoint().y);
			segment.setCenterPoint(centerPoint);
			segment.setManuallyEdited(true);
			//System.out.println("New centerpoint  x:" + segment.getCenterPoint().x + "    y:" + segment.getCenterPoint().y);
		}	
	}
	
	
	/**
	 * Compute average intensity of pixels in a square kernel around a point.
	 * Returns -1 if the kernel extends outside image bounds.
	 *
	 * @param pt Center point of the kernel
	 * @param proc Image data to sample from
	 * @return Average kernel intensity, or -1 if kernel extends beyond image boundary
	 *
	 * Averages pixel intensity over a (2*kernelSize+1) x (2*kernelSize+1) square
	 * neighborhood centered on pt. Returns -1 if any part of the kernel falls outside
	 * the image bounds.
	 */
	int kernelIntensity (Point pt, ImageProcessor proc) {
		int intensity = 0;
		int count = 0;
		for (int j = -kernelSize; j <= kernelSize; j++) {
			if (pt.x + j > proc.getWidth() - 1 || pt.x + j < 0) return -1;
			for (int k = -kernelSize; k <= kernelSize; k++) {
				if (pt.y + k > proc.getHeight() - 1 || pt.y + k < 0) return -1;
				intensity += proc.get(pt.x + j, pt.y + k);
				count++;
			}
		}
		return intensity/count;
	}

}
