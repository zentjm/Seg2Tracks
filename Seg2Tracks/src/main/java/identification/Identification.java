package identification;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.SwingWorker;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import geometricTools.PolarPoint;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;

public class Identification {

	//Overwrite your a name and description of your program here. Name appears for selection, description appears in help menu. 
	String name = "Method Name";
	String description = "How this Method Works";
		
	//Input parameters (no return objects, just add to segments)
	ImageStack inputStack;
	DataSet dataSet;
	GaussianBlur blurrer;
	double blurSigma;
	MaximumFinder finder;
	double percentThreashold;
	int kernelSize;

	public void initialize(ImageStack imageStack, DataSet dataSet) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
		kernelSize = 3; 
	}
	
	public void setBlur(GaussianBlur blurrer, double blurSigma) {
		this.blurrer = blurrer;
		this.blurSigma = blurSigma;
	}
	
	public void setFinder(MaximumFinder finder, double finderTolerance) {
		this.finder = finder;
		this.percentThreashold = finderTolerance;
	}
	
	//DEPRECIATED
	public void run() {
		ImageProcessor tempProcessor;
		Polygon poly;
		FrameSet frameSet;
		for (int i = 0; i < inputStack.size(); i ++) {
			frameSet = new FrameSet(i, dataSet);
			tempProcessor = inputStack.getProcessor(i+1);
			blurrer.blurGaussian(tempProcessor, blurSigma);
			poly = finder.getMaxima(tempProcessor, 0, true);
	

			//TODO: filter the points;
			System.out.println("Initial Maxima Points: " + poly.npoints);
			poly = filterLowPoints(poly, tempProcessor);
			System.out.println("Filtered Maxima Point: " + poly.npoints);
			
			
			System.out.println("found maxpoints of " + i +", poly number of points: " + poly.npoints);
			for (int j = 0; j < poly.npoints; j ++) {
				frameSet.add(new Segment(i, new Point(poly.xpoints[j], poly.ypoints[j])));
			}
			dataSet.addFrameSet(frameSet, i);
		}

	}
	

	public void runManualAdjustment() {
		ImageProcessor tempProcessor;
		Polygon poly;
		for (FrameSet frameSet : dataSet.getFrameSetList()) {
			tempProcessor = inputStack.getProcessor(frameSet.getFrame()+1);
			blurrer.blurGaussian(tempProcessor, blurSigma);
			poly = finder.getMaxima(tempProcessor, 0, true);
			System.out.println("found maxpoints of " + frameSet.getFrame() +", poly number of points: " + poly.npoints);
			adjustCenterPoint(frameSet, poly, tempProcessor);
		}
	}
	
	
	//DEPRECIATED
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
					
					
					System.out.println("found maxpoints of " + i +", poly number of points: " + poly.npoints);
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

	
	
	/**
	 * REMOVING LOW POINTS:
	 * 1. Collect all points
	 * 2. Calculate a value for all points with an image kernel
	 * 3. Filter with thresholding.
	 */
	public Polygon filterLowPoints(Polygon poly, ImageProcessor tempProcessor) {
		
		if (percentThreashold < 0) percentThreashold = 0;
		if (percentThreashold > 1) percentThreashold = 1;
		
		int[] intensities = new int[poly.npoints];
		
		//Collect intensities
		for (int i = 0; i < poly.npoints; i ++) {
		
			int intensity = 0;
			int count = 0;
			boolean edge = false;
			
			for (int j = -kernelSize ; j <= kernelSize; j ++) {
				if (poly.xpoints[i] + j > tempProcessor.getWidth() - 1 || poly.xpoints[i] + j < 0) {
					System.out.println("X overreach");
					edge = true;
					continue;
				}
				for (int k = -kernelSize ; k <= kernelSize; k ++) {
					if (poly.ypoints[i] + k > tempProcessor.getHeight() - 1 || poly.ypoints[i] + k < 0) {
						System.out.println("Y overreach");
						edge = true;
						continue;
					}
					intensity += tempProcessor.get(poly.xpoints[i] + j, poly.ypoints[i] + k);
					count++;
				}
			}
			if (edge == true) intensity = -1;
			intensities[i] = intensity/count;
		}
		
		//TODO: more sophisticated threasholding of points. 
		
		//convert percent threashold to value
		int temp;
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < tempProcessor.getWidth(); i ++) {
			for (int j = 0; j < tempProcessor.getHeight(); j ++) {
				temp = tempProcessor.get(i, j);
				if (temp < min) min = temp;  //TODO: could have some "average" min or "average" max
				if (temp > max) max = temp;
			}
		}
		
		int threashold = (int) (min + ((max - min) * percentThreashold));
	
		//System.out.println("Min: " + min + "   Max:" + max);
		//System.out.println("Threashold:" + threashold);
		
		//Filter points into new polygon
		Polygon newPoly = new Polygon();
		for (int i = 0; i < poly.npoints; i ++) {
			//System.out.println("Poly " + i + "/" + poly.npoints +
			//		"   Intensity:" + intensities[i] + "   Theashold: " + threashold);
			
			if (intensities[i] > threashold)
				newPoly.addPoint(poly.xpoints[i], poly.ypoints[i]);
		}
		return newPoly;	
	}
	
	
	
	//
	public void adjustCenterPoint(FrameSet frameSet, Polygon pts, ImageProcessor tempProcessor) {
		
		for (Segment segment : frameSet) {
			if (segment.isManuallyEdited()) continue;
			ArrayList<Point> matches = new ArrayList<Point>();
			Roi poly = GeometricCalculations.getPolygonRoi(segment.getExternalPerimeter());
			for (int i = 0; i < pts.npoints; i ++) {
				if (poly.contains(pts.xpoints[i], pts.ypoints[i])) {
					 matches.add(new Point(pts.xpoints[i], pts.ypoints[i]));
				}
			}
			
			Point centerPoint = null;
			
			//Find the best match
			if (matches.size() == 0) {
				System.out.println("No identification match");
				
				int maxIntensity = 0;
				Point areaPointList[] = GeometricCalculations.getAreaByRoi(segment.getExternalPerimeter());
				int[] intensities = new int[areaPointList.length];
				for (int i = 0; i < areaPointList.length; i++) {
					int tempIntensity = kernelIntensity(areaPointList[i], tempProcessor);
					if (tempIntensity > maxIntensity) {
						maxIntensity = tempIntensity;
						centerPoint = areaPointList[i];
					}
				}
			}
			
			//Use the brightest match
			else if (matches.size() > 1) {
				System.out.println("More than one identification match");
				int intensity = 0;
				for (Point pt: matches) {
					int tempIntensity = kernelIntensity(pt, tempProcessor);
					if (tempIntensity > intensity) {
						intensity = tempIntensity;
						centerPoint = pt;
					}
				}	
			}
			//if one match for outline
			else {
				System.out.println("One-to-one identification match");
				centerPoint = matches.get(0);
			}
			System.out.println("Old centerpoint  x:" + segment.getCenterPoint().x + "    y:" + segment.getCenterPoint().y);
			segment.setCenterPoint(centerPoint);
			segment.setManuallyEdited(true);
			System.out.println("New centerpoint  x:" + segment.getCenterPoint().x + "    y:" + segment.getCenterPoint().y);
		}	
	}
	
	
	//Kernel intensity
	int kernelIntensity (Point pt, ImageProcessor proc) {
		int intensity = 0;
		int count = 0;
		for (int j = kernelSize ; j <= kernelSize; j ++) {
			if (pt.x + j > proc.getWidth() - 1 || pt.x + j < 0) return -1;
			for (int k = -kernelSize  ; k <= kernelSize; k ++) {
				if (pt.y + k > proc.getHeight() - 1 || pt.y + k < 0) return -1;
				intensity += proc.get(pt.x + j, pt.y + k);
				count++;
			}
		}
		return intensity/count;
	}

}
