package segmentation;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.JProgressBar;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
//import ij.gui.Wand;
import ij.plugin.filter.GaussianBlur;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedAutoThresholder;
import geometricTools.ModifiedAutoThresholder.Method;
import geometricTools.ModifiedWand;

/**
 * Abstract base class for all segmentation methods. Segmentation creates a binary mask of cell regions
 * from a grayscale fluorescent image by applying intensity thresholding. Subclasses implement different
 * thresholding algorithms (Triangle, Otsu, Active Contour) and dependency models (global vs. region-restricted).
 */
public abstract class Segmentation {

	// Subclass name and description for UI selection and help menus
	String name = "Method Name";
	String description = "How this Method Works";
	// ImageProcessor operates on the current frame being segmented
	ImageProcessor processor;

	// Flag indicating whether thresholding is restricted to specific regions (true) or global (false)
	boolean externalDependence; //TODO: Needs to be a setting

	// Input parameters for the segmentation pipeline
	ImageStack inputStack; // Stack of all image frames to process
	DataSet dataSet; // Container for all segmentation results
	GaussianBlur blurrer; // Gaussian blur filter for pre-processing
	double blurSigma; // Blur kernel standard deviation parameter

	// Boundary-cleanup parameters (removeLoops search-distance scaling + douglasPeucker
	// tolerance), tunable per dataset via Boundary Cleanup Settings / Guided Calibration.
	// See sarn.Sarn's identical fields/setter for the full rationale.
	double searchFraction = 0.15;
	int searchCeiling = 400;
	double epsilon = GeometricCalculations.DEFAULT_SIMPLIFICATION_EPSILON;

	// Status tracking
	JProgressBar progress; // UI progress bar for user feedback

	// Recursive mode: when true, exclude bin 0 from histogram before threshold computation.
	// In recursive mode the masked exterior is always zero, biasing the histogram.
	boolean skipZeroBin = false;

	/**
	 * Constructor. Initializes name and description fields.
	 */
	public Segmentation() {
		name = "Method Name";
		description = "How this Method Works";
	}

	/**
	 * Initializes the segmentation engine with input image stack, dataset, and progress bar.
	 * @param imageStack Stack of frames to segment
	 * @param dataSet DataSet to populate with segmentation results
	 * @param progress JProgressBar for UI feedback
	 */
	public void initialize(ImageStack imageStack, DataSet dataSet, JProgressBar progress) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
		this.progress = progress;
	}

	/**
	 * Configures Gaussian blur parameters for pre-processing.
	 * @param blurrer GaussianBlur filter instance
	 * @param blurSigma Standard deviation of blur kernel
	 */
	public void setBlur(GaussianBlur blurrer, double blurSigma) {
		this.blurrer = blurrer;
		this.blurSigma = blurSigma;
	}

	/**
	 * Configures boundary-cleanup parameters used by {@code extractInternalPerimeter}. See
	 * {@link sarn.Sarn#setCleanupParams} for the full rationale — identical parameters, applied
	 * to the internal-perimeter pipeline instead of the external (SARN) one.
	 * @param searchFraction fraction of a contour's own point count used as removeLoops's
	 *                        search-ahead window
	 * @param searchCeiling  upper bound (in points) on the search window
	 * @param epsilon        perpendicular-distance tolerance (pixels) for douglasPeucker
	 */
	public void setCleanupParams(double searchFraction, int searchCeiling, double epsilon) {
		this.searchFraction = searchFraction;
		this.searchCeiling = searchCeiling;
		this.epsilon = epsilon;
	}

	/**
	 * When set to true, histogram bin 0 is excluded from threshold computation.
	 * Use in recursive mode where the masked exterior is always zero-valued.
	 * @param skip true to exclude zero bin
	 */
	public void setSkipZeroBin(boolean skip) {
		this.skipZeroBin = skip;
	}

	/**
	 * Returns the name of this segmentation method.
	 * @return name String identifying the algorithm
	 */
	public String toString() {
		return name;
	}

	/**
	 * Returns the description of how this segmentation method works.
	 * @return description User-facing help text
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Main execution loop. Processes each frame in the input stack sequentially by converting to byte
	 * intensity, then invoking the subclass-specific segmentation implementation.
	 */
	public void run() {
		progress.setString("Segmentation");
		for (int i = 0; i < inputStack.size(); i ++) {
			processor = inputStack.getProcessor(i+1); // Fetch processor for frame i (1-indexed)
			processor = processor.convertToByte(true); // Convert to 8-bit grayscale
			//blurrer.blurGaussian(processor, blurSigma/2);

			segmentation(dataSet.getFrameSet(i)); // Delegate to subclass implementation
			progress.setValue(i); // Update progress bar
		}
	}

	/**
	 * Abstract method implemented by subclasses to perform segmentation on a single frame.
	 * Subclasses must set the internal perimeter (binary mask) for all segments in the FrameSet.
	 * @param segments FrameSet containing all cells/regions to segment in this frame
	 */
	abstract void segmentation(FrameSet segments);

	/**
	 * Abstract method to validate whether a computed threshold is acceptable for the image.
	 * Used by some methods (e.g., RestrictedOtsuMethod) to reject thresholds that violate statistical criteria.
	 * @param threshold Intensity threshold value to validate
	 * @param histogram Intensity histogram of the image or region
	 * @return true if threshold is acceptable, false to reject and use fallback
	 */
	abstract boolean acceptableThreshold(int threshold, int[] histogram);

	/**
	 * Abstract method to query whether this segmentation method depends on external regions (SARN).
	 * @return true if method is SARN-dependent (restricted/local thresholding), false if global
	 */
	public abstract boolean isExternallyDependent();

	/**
	 * Abstract method to return the thresholding algorithm this subclass uses.
	 * Called polymorphically by globalSegmentation() and restrictedSegmentation() so that
	 * shared pipeline code in the parent class can apply the correct algorithm without knowing
	 * which subclass is running.
	 * @return ModifiedAutoThresholder.Method enum value identifying the algorithm
	 */
	abstract Method getMethod();

	/**
	 * Global thresholding pipeline shared by all non-restricted segmentation methods
	 * (e.g. TriangleMethod, OtsuMethod, LiMethod). Computes a single threshold from
	 * the full-frame blurred histogram via getMethod(), binarizes the image, then traces
	 * each cell boundary from its center using ModifiedWand. Subclasses select the algorithm
	 * by overriding getMethod(); no other override is required to add a new global method.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	protected void globalSegmentation(FrameSet segments) {
		ModifiedAutoThresholder thresh = new ModifiedAutoThresholder();
		ImageProcessor blurredPro = processor.duplicate();
		blurrer.blurGaussian(blurredPro, 2); // Pre-blur for smoother histogram (TODO: make configurable)
		int[] histogram = blurredPro.getHistogram(256); // 8-bit only (TODO: support 16-bit)
		if (skipZeroBin) histogram[0] = 0; // exclude masked exterior from threshold computation
		int trs = thresh.getThreshold(getMethod(), histogram); // Algorithm selected by subclass
		processor.threshold(trs); // Binarize the working processor

		for (int n = 0; n < segments.size(); n++) {
			//System.out.println("Segmenting segment: " + n + " threshold is: " + trs);

			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;

			// Verify center pixel is within foreground after thresholding
			//System.out.println("Segment " + n + " centerpoint value is: " + processor.get(x, y));

			// Flood-fill contour trace from cell center — no ROI constraint (global method)
			ModifiedWand wand = new ModifiedWand(processor);
			wand.setAllPoints(false);
			wand.autoOutline(x, y, trs, 255, ModifiedWand.EIGHT_CONNECTED);

			// Convert wand int arrays to Point array
			Point[] points = new Point[wand.npoints];
			for (int i = 0; i < wand.npoints; i++) points[i] = new Point(wand.xpoints[i], wand.ypoints[i]);

			// Smooth perimeter: straighten → remove loops → simplify (Douglas-Peucker) → straighten again
			Point[] densifiedInternal = GeometricCalculations.straightPerimeter(points);
			int internalSearchDistance = GeometricCalculations.scaledSearchDistance(
					densifiedInternal.length, searchFraction, searchCeiling);
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(
				GeometricCalculations.douglasPeucker(
				GeometricCalculations.removeLoops(
				densifiedInternal, internalSearchDistance,
				GeometricCalculations.LOOP_REMOVAL_RANGE,
				GeometricCalculations.LOOP_REMOVAL_SMOOTHING),
				epsilon)));
			clean(segments.get(n)); // Flag any boundary-contact segments
		}
	}

	/**
	 * Restricted (SARN) thresholding pipeline shared by all externally-dependent segmentation methods
	 * (e.g. RestrictedTriangleMethod, RestrictedOtsuMethod). For each cell, builds an intensity histogram
	 * from pixels within its SARN external region only, computes a local threshold via getMethod(), then
	 * traces the cell boundary constrained to that region. Subclasses select the algorithm by overriding
	 * getMethod(); threshold validation is delegated to acceptableThreshold() so each subclass can enforce
	 * statistical criteria (e.g. Kittler-Illingworth for Otsu) without modifying the pipeline.
	 * @param segments FrameSet containing all cells to segment in the current frame
	 */
	protected void restrictedSegmentation(FrameSet segments) {
		ModifiedAutoThresholder thresh = new ModifiedAutoThresholder();
		int totalPoints = processor.getHeight() * processor.getWidth();

		for (int n = 0; n < segments.size(); n++) {
			
			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;

			// Extract all pixel positions within this cell's SARN external region
			Point[] exPts = GeometricCalculations.getAreaByRoi(segments.get(n).getExternalPerimeter());

			// Build histogram from SARN-region pixels only (local threshold computation)
			int[] histogram = new int[256];
			Polygon tempPoly = getPolygon(exPts);
			//System.out.println("ExPts = " + exPts.length + " out of total points " + totalPoints
			//			+ "... " + (double) exPts.length / totalPoints);

			// Sample blurred intensities to reduce noise in the regional histogram, TODO: better understand what is going on here
			ImageProcessor blurredPro = processor.duplicate();
			blurrer.blurGaussian(blurredPro, 2);
			for (int j = 0; j < tempPoly.npoints; j++) {
				int intensity = blurredPro.get(tempPoly.xpoints[j], tempPoly.ypoints[j]);
				histogram[intensity]++;
			}

			// Compute local threshold; delegate algorithm choice to subclass via getMethod()
			int trs = thresh.getThreshold(getMethod(), histogram);
			Roi roi = getPolygonRoi(segments.get(n).getExternalPerimeter());

			// Allow subclass to reject threshold (e.g. Kittler-Illingworth validation in RestrictedOtsuMethod)
			if (!acceptableThreshold(trs, histogram)) trs = 255; // Fallback: accept entire region

			// Flood-fill contour trace constrained within the SARN ROI
			ModifiedWand wand = new ModifiedWand(processor, roi);
			wand.autoOutline(x, y, trs, 255, ModifiedWand.EIGHT_CONNECTED);

			// Collect wand output via LinkedList (size unknown before trace)
			LinkedList<Point> wandPts = new LinkedList<Point>();
			for (int i = 0; i < wand.npoints; i++) {
				wandPts.add(new Point(wand.xpoints[i], wand.ypoints[i]));
			}
			Point[] points = wandPts.toArray(new Point[0]); // LinkedList → fixed array

			//System.out.println("Segment frame count: " + segments.size() + "  Current segment: " + n);

			// Smooth perimeter: straighten → remove loops → simplify (Douglas-Peucker) → straighten again
			Point[] densifiedInternal = GeometricCalculations.straightPerimeter(points);
			int internalSearchDistance = GeometricCalculations.scaledSearchDistance(
					densifiedInternal.length, searchFraction, searchCeiling);
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(
				GeometricCalculations.douglasPeucker(
				GeometricCalculations.removeLoops(
				densifiedInternal, internalSearchDistance,
				GeometricCalculations.LOOP_REMOVAL_RANGE,
				GeometricCalculations.LOOP_REMOVAL_SMOOTHING),
				epsilon)));
			clean(segments.get(n)); // Flag any boundary-contact segments
		}
	}

	/**
	 * Marks whether a segment touches the image boundary. Sets internal boundary contact flag
	 * on the segment based on whether any point on its perimeter lies on the image border.
	 * @param segment Segment whose boundary contact status should be checked
	 */
	public void clean (Segment segment) {

		Point[] ptsList = segment.getInternalPerimeter();
		boolean contact = false;

		// Check all perimeter points against image boundaries
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x == 0) contact = true; // Left edge
			if (ptsList[i].x == processor.getWidth() - 1) contact = true; // Right edge
			if (ptsList[i].y == 0) contact = true; // Top edge
			if (ptsList[i].y == processor.getHeight() - 1) contact = true; // Bottom edge
		}

		//System.out.println("Segment contact: " + contact);
		segment.setInternalBoundaryContact(contact);
	}

	/**
	 * Converts a Point array to an AWT Polygon for geometric operations.
	 * @param pointList Array of Points defining polygon vertices
	 * @return Polygon with vertices in same order as pointList
	 */
	public final Polygon getPolygon(Point[] pointList) {
		int[] xPoints = new int[pointList.length];
		int[] yPoints = new int[pointList.length];
		for (int i = 0; i < pointList.length; i ++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new Polygon(xPoints, yPoints, xPoints.length);
	}

	/**
	 * Converts a Point array to an ImageJ PolygonRoi (Region of Interest) for display/selection.
	 * @param pointList Array of Points defining polygon vertices
	 * @return PolygonRoi representing the boundary
	 */
	public final PolygonRoi getPolygonRoi(Point[] pointList) {
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i ++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
	}


	/**
	 * Displays an intensity histogram as a plot window. Utility method for debugging and analysis.
	 * @param histogram Array where index is intensity value and element is pixel count
	 */
	void displayHistogram (int[] histogram) {

		// Create plot with intensity distribution data
		Plot plot = new Plot("Intensity Distribution", "Pixel", "Intensity");
		PlotWindow plowWindow;
		double[] xValues = new double[histogram.length];
		double[] yValues = new double[histogram.length];

		// Convert histogram integer counts to double for plotting
		for (int i = 0; i < histogram.length; i++) {
			xValues[i] = (double) i;
			yValues[i] = (double) histogram[i];
		}

		plot.add("dot", xValues, yValues);
		ImageProcessor plotProc = plot.getProcessor();
		ImagePlus image = new ImagePlus("Plot", plotProc);
		image.show();
	}


	/**
	 * Scores perimeter points based on local connectivity (density of nearby foreground pixels).
	 * Removes low-connectivity points from the segmentation boundary. Works by computing a local
	 * neighborhood score for each point, then thresholding to keep only well-connected points.
	 * @param proc Binary ImageProcessor (255=foreground, 0=background)
	 * @param pts Array of perimeter points to score and filter
	 */
	public void connectivityScore(ImageProcessor proc, Point[] pts) {

		// 1. Score all points based on nearby 255 values within search radius
		int sR = 2; // Search radius in pixels
		int scoreMax = Integer.MIN_VALUE;
		int scoreMin = Integer.MAX_VALUE;
		int[] scores = new int[pts.length];
		for (int i = 0; i < pts.length; i ++) {
			int score = 0; // Count of foreground pixels in neighborhood
			for (int j = pts[i].x - sR; j <= pts[i].x + sR; j++) {
				for (int k = pts[i].y - sR; k <= pts[i].y + sR; k++) {
					if (j > proc.getWidth() -1 || j < 0) continue; // Skip out-of-bounds
					if (k > proc.getHeight() -1 || k < 0) continue;
					if (proc.get(j,k) == 255) score ++; // Count foreground neighbors
				}
			}
			if (score < scoreMin) scoreMin = score;
			if (score > scoreMax) scoreMax = score;
			scores[i] = score;
		}

		// 2. Build histogram of connectivity scores
		int[] histogram = new int[scoreMax + 1];
		for (int i = 0; i < scores.length; i ++) {
			//System.out.println("SCORE: " + scores[i]);
			//System.out.println("Score Max: " + scoreMax);
			histogram[scores[i]]++;
		}

		// 3. Compute threshold: find score bin with maximum frequency
		//ModifiedAutoThresholder thresh = new ModifiedAutoThresholder();
		//int trs = thresh.getThreshold(Method.Default, histogram);
		int trs = 0;
		for (int i = 0; i < histogram.length; i++) {
			//System.out.println("Histogram @ " + i + " is: " + histogram[i]);
			if (histogram[i] > histogram[trs]) trs = i; // Peak of score distribution
		}
		//System.out.println("Threshold: " + trs);


		// 4. Keep only points with score exceeding threshold
		for (int i = 0; i < pts.length; i ++) {
			if (scores[i] > trs) {
				proc.set(pts[i].x, pts[i].y, 255); // Mark high-connectivity points
			}
		}
	}


	// Method for seeding a constrictive component (placeholder comment from original)
}
