package internalSegmentation;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

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

public abstract class InternalSegmentation {
	
	//Overwrite your a name and description of your program here. Name appears for selection, description appears in help menu. 
	String name = "Method Name";
	String description = "How this Method Works";
	ImageProcessor processor;

	//Selection of different aspects
	boolean externalDependence; //TODO: Needs to be a setting
	
	//Input parameters (no return objects, just add to segments)
	ImageStack inputStack;
	DataSet dataSet;
	GaussianBlur blurrer;
	double blurSigma;
		
	public InternalSegmentation() {
		name = "Method Name";
		description = "How this Method Works";
	}
	
	public void initialize(ImageStack imageStack, DataSet dataSet) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
	}
	
	public void setBlur(GaussianBlur blurrer, double blurSigma) {
		this.blurrer = blurrer;
		this.blurSigma = blurSigma;
	}
	
	public String toString() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	//returns the perimeter of this cell
	public void run() {
		for (int i = 0; i < inputStack.size(); i ++) {
			processor = inputStack.getProcessor(i+1);
			processor = processor.convertToByte(true);
			//blurrer.blurGaussian(processor, blurSigma/2);
			if (!externalDependence) binarySegmentation(dataSet.getFrameSet(i));
			if (externalDependence) dependentBinarySegmentation(dataSet.getFrameSet(i));
		}	
	}
	
	//abstract Method getMethod();
	abstract Method getMethod();
	abstract boolean acceptableThreshold(int threshold, int[] histogram);
	public abstract boolean isExternallyDependent();
	

	//Binary segmentation independent of external factors. 
	protected void binarySegmentation(FrameSet segments) {
	
		ModifiedAutoThresholder thresh = new ModifiedAutoThresholder();
		ImageProcessor blurredPro = processor.duplicate();
		blurrer.blurGaussian(blurredPro, 2);
		int[] histogram = blurredPro.getHistogram(256); //TODO: allow 8-bit, 16-bit
		int trs = thresh.getThreshold(getMethod(), histogram); 
		processor.threshold(trs);
		int targetColor = 255;
		
		for (int n = 0; n < segments.size(); n++) {
		
			System.out.println("Segmenting segment: " + n + " threshold is: " + trs); 
			
			//Holds centerpoint;
			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;
	
			//checks if centerpoint is at least selected
		
			System.out.println("Segment " + n + " centerpoint value is: " + processor.get(x, y));
			
			//Wand for outlining
			ModifiedWand wand = new ModifiedWand(processor);
			wand.setAllPoints(false);
			wand.autoOutline(x, y, trs, 255, ModifiedWand.EIGHT_CONNECTED);
		
			//Convert wand int arrays to Point arrays
			Point [] points = new Point[wand.npoints];
			for (int i = 0; i < wand.npoints; i++)	points[i] = new Point(wand.xpoints[i], wand.ypoints[i]);
			
			
			//Sets SegmentModel internal perimeter //TODO: some external control on geometric operations	
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(
				GeometricCalculations.shortcutPerimeter(
				GeometricCalculations.straightPerimeter(
								points))));
			clean(segments.get(n));

		}
	}
	
	
	//Dependent binary Segmentation
	protected void dependentBinarySegmentation(FrameSet segments) {
		
		ModifiedAutoThresholder thresh = new ModifiedAutoThresholder();
		int targetColor = 255;
		
		//Associate each polygon with its extraction
		for (int n = 0; n < segments.size(); n++) {
			
			//Gets centerpoints
			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;
			
			//Gets externalSegmentation points
			Point[] exPts = GeometricCalculations.getAreaByRoi(segments.get(n).getExternalPerimeter());
			
			//TODO: Need to use more neighbor tracing 
			 
		
			//Initiate histogram
			int [] histogram = new int [256];
			int totalPoints = processor.getHeight() * processor.getWidth();

		
			//Edited from Old InternalSegmentationBoundry
			Polygon tempPoly = getPolygon(exPts);
			System.out.println("ExPts = " + exPts.length + " out of total points " + totalPoints + "... " + (double)exPts.length/totalPoints);
			int count = 0;
			
			
			ImageProcessor blurredPro = processor.duplicate();
			blurrer.blurGaussian(blurredPro, 2);
			
			for (int j = 0; j < tempPoly.npoints; j ++)  {
				count ++;
				int intensity = blurredPro.get(tempPoly.xpoints[j], tempPoly.ypoints[j]);
				histogram[intensity] = histogram[intensity] + 1;
			}
			
			//histogram[0] = 0;
			//histogram[255] = 0;
			
			//Threshold histogram 
			int trs = thresh.getThreshold (getMethod(), histogram); //TODO allow transitional stuff.
			Roi roi = getPolygonRoi(segments.get(n).getExternalPerimeter());
			
			if (!acceptableThreshold(trs, histogram)) trs = 255;
			
			//Modified wand
			ModifiedWand wand = new ModifiedWand(processor, roi);
			//wand.setAllPoints(false);
			wand.autoOutline(x, y, trs, 255, ModifiedWand.EIGHT_CONNECTED);
			
			//Roi roi = getPolygonRoi(segments.get(n).getExternalPerimeter());
			
			//Transfer wand points to LinkedList
			LinkedList<Point> wandPts = new LinkedList<Point>();
			for (int i = 0; i < wand.npoints; i ++) {
				wandPts.add(new Point(wand.xpoints[i], wand.ypoints[i]));
			}
			
			//Convert wandPts LinkedList to Point arrays
			Point [] points = new Point[wandPts.size()];
			for (int i = 0; i < wandPts.size(); i++) {
				points[i] = wandPts.get(i);
			}
			
			//Sets SegmentModel internal perimeter //TODO: some external control on geometric operations	
		
			System.out.println("Segment frame count: " + segments.size());
			System.out.println("Current segment: " + n);
			

			
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(
				GeometricCalculations.shortcutPerimeter(
				GeometricCalculations.straightPerimeter(
								points))));
			clean(segments.get(n));
		
		}
	}
	
	public void clean (Segment segment) {
		
		Point[] ptsList = segment.getInternalPerimeter();
		boolean contact = false;
	
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x == 0)contact = true;
			if (ptsList[i].x == processor.getWidth() - 1) contact = true;
			if (ptsList[i].y == 0)contact = true;
			if (ptsList[i].y == processor.getHeight() - 1) contact = true;
		}
		
		//System.out.println("Segment contact: " + contact);
		segment.setInternalBoundaryContact(contact);
	}
		
	//Converts a pointList to a Polygon
	public final Polygon getPolygon(Point[] pointList) {
		int[] xPoints = new int[pointList.length];
		int[] yPoints = new int[pointList.length];
		for (int i = 0; i < pointList.length; i ++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new Polygon(xPoints, yPoints, xPoints.length);
	}
	
	//Converts a pointList to a PolygonRoi
	public final PolygonRoi getPolygonRoi(Point[] pointList) {
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i ++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
	}
	
	
	//Display histogram
	void displayHistogram (int[] histogram) {
		
		//Testing: histogram of sigmas
		Plot plot = new Plot("Intensity Distribution", "Pixel", "Intensity");
		PlotWindow plowWindow;
		double[] xValues = new double[histogram.length];
		double[] yValues = new double[histogram.length];
		
		//Translates to doubles
		for (int i = 0; i < histogram.length; i++) {
			xValues[i] = (double) i;
			yValues[i] = (double) histogram[i];
		}
	
		plot.add("dot", xValues, yValues);
		ImageProcessor plotProc = plot.getProcessor();
		ImagePlus image = new ImagePlus("Plot", plotProc);
		image.show();
	}
	//Allows access to whether or not method is externally dependent
	
	
	//Processes connectivity score
	public void connectivityScore(ImageProcessor proc, Point[] pts) {
		
		//1. Score all points. 
		int sR = 2; //search radius
		int scoreMax = Integer.MIN_VALUE;
		int scoreMin = Integer.MAX_VALUE;
		int[] scores = new int[pts.length];
		for (int i = 0; i < pts.length; i ++) {
			int score = 0;
			for (int j = pts[i].x - sR; j <= pts[i].x + sR; j++) {
				for (int k = pts[i].y - sR; k <= pts[i].y + sR; k++) {
					if (j > proc.getWidth() -1 || j < 0) continue;
					if (k > proc.getHeight() -1 || k < 0) continue;
					if (proc.get(j,k) == 255) score ++;
				}
			}
			if (score < scoreMin) scoreMin = score;
			if (score > scoreMax) scoreMax = score;
			scores[i] = score;
		}
		
		//2. Generate a histogram of scores
		int[] histogram = new int[scoreMax + 1];
		for (int i = 0; i < scores.length; i ++) {
			//System.out.println("SCORE: " + scores[i]);
			//System.out.println("Score Max: " + scoreMax);
			histogram[scores[i]]++;
		}
		
		//3. Threshold scores:
		//ModifiedAutoThresholder thresh = new ModifiedAutoThresholder();
		//int trs = thresh.getThreshold(Method.Default, histogram); 
		int trs = 0;
		for (int i = 0; i < histogram.length; i++) {
			System.out.println("Histogram @ " + i + " is: " + histogram[i]);
			if (histogram[i] > histogram[trs]) trs = i;
		}
		System.out.println("Threshold: " + trs);
		
		
		//4. Modify processor based on threshold scores
		for (int i = 0; i < pts.length; i ++) {
			if (scores[i] > trs) {
				proc.set(pts[i].x, pts[i].y, 255);	
			}
		}
	}
	
}

