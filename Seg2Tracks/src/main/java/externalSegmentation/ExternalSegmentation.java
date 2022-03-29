package externalSegmentation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.SwingWorker;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

public abstract class ExternalSegmentation {
	
	//Overwrite your a name and description of your program here. Name appears for selection, description appears in help menu. 
	String name = "Method Name";
	String description = "How this Method Works";
	
	//Input parameters (no return objects, just add to segments)
	ImageStack inputStack;
	DataSet dataSet;
	GaussianBlur blurrer;
	double blurSigma;
	
	//Settings for import

	
	//Multiuse parameters
	ImageProcessor processor;
	FrameSet segments;
	
	public ExternalSegmentation() {
	}


	
	//Nested class for use comparing two points //TODO: this type of construct used by other classes - combine as geometric tool?
	class PointSet {
		Point innerPoint;
		Point outerPoint;
		PointSet (Point innerPoint, Point outerPoint) {
			this.innerPoint = innerPoint;
			this.outerPoint = outerPoint;
		}
	}
	
	public void initialize(ImageStack imageStack, DataSet dataSet) {
		this.inputStack = imageStack;
		this.dataSet = dataSet;
	}
	
	
	public void setBlur(GaussianBlur blurrer, double blurSigma) {
		this.blurrer = blurrer;
		this.blurSigma = blurSigma;
	}
	
	//For name to put in menu
	public String toString() {
		return name;
	}
	
	//for description to put in help menu
	public String getDescription() {
		return description;
	}
	
	//Finds pointlist for establishing an inner boundary
	abstract Point[] innerPoints (int currentSegment);
	
	//Finds pointlist for establishing an outer boundary
	abstract Point[] outerPoints (Point [] innerPoints);
	
	//Generates point list for inner boundary
	abstract Point[] innerBounds (Point [] innerPts, Point [] outerPts);
	
	//Generates point list for outer boundary
	abstract Point[] outerBounds (Point[] innerBounds, Point[] outerPts);
	
	//Determines pair matching between inner and outer bounds
	abstract PointSet[] boundaryMatch (Point [] innerBounds, Point[] outerBounds);
	
	//Determines the threshold intensity for a given Bresenham line
	abstract Point getThreasholdPoint (Point[] pts);
	
	//thread which runs the external segmentation
	public SwingWorker runThread() {
		return new SwingWorker<Void, Integer>() {
			@Override
			public Void doInBackground() {				
				for (int i = 0; i < inputStack.size(); i ++) {
					processor = inputStack.getProcessor(i+1);
					blurrer.blurGaussian(processor, blurSigma); //TODO, separate control over blurring for the contraction.
					segments = dataSet.getFrameSet(i);
					for (int n = 0; n < segments.size(); n++) {
						Point [] innerPoints = innerPoints(n); 		
						Point [] outerPoints = outerPoints(innerPoints); 										
						Point [] innerBoundary = clean(innerBounds(innerPoints, outerPoints), segments.get(n)); 	
						Point [] outerBoundary = clean(outerBounds(innerBoundary, outerPoints), segments.get(n));							
						PointSet [] matchedPoints = boundaryMatch(innerBoundary, outerBoundary);
						//segments.get(n).setExternalPerimeter(outerBoundary); //XXX: to test outer bounds

						segments.get(n).setExternalPerimeter( //TODO: Adjust/fix
							GeometricCalculations.straightPerimeter(
							GeometricCalculations.shortcutPerimeter(
							GeometricCalculations.straightPerimeter(
							contractor(matchedPoints))))); //Proper event
					}
					setProgress(i);
				}
				return null;
			}
		};
	}
	
	//returns the perimeter of this cell
	public void run() {
		for (int i = 0; i < inputStack.size(); i ++) {
			processor = inputStack.getProcessor(i+1);
			blurrer.blurGaussian(processor, blurSigma); //TODO, separate control over blurring for the contraction.
			segments = dataSet.getFrameSet(i);
			for (int n = 0; n < segments.size(); n++) {
				Point [] innerPoints = innerPoints(n); 		
				Point [] outerPoints = outerPoints(innerPoints); 										
				Point [] innerBoundary = clean(innerBounds(innerPoints, outerPoints), segments.get(n)); 	
				Point [] outerBoundary = clean(outerBounds(innerBoundary, outerPoints), segments.get(n));							
				PointSet [] matchedPoints = boundaryMatch(innerBoundary, outerBoundary);
				//segments.get(n).setExternalPerimeter(outerBoundary); //XXX: to test outer bounds
				segments.get(n).setExternalPerimeter(contractor(matchedPoints)); //Proper event
			}
		}
	}


	//TODO: Method for removing duplicate values and ensuring pointList is inbounds
	public Point[] clean (Point[] ptsList, Segment segment) {
		
		ArrayList<Point> ptsArrayList = new ArrayList<Point>();
		
		boolean contact = false;
		
		/*
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x == 0)contact = true;
			if (ptsList[i].x == processor.getWidth() - 1) contact = true;
			if (ptsList[i].y == 0)contact = true;
			if (ptsList[i].y == processor.getHeight() - 1) contact = true;
			ptsArrayList.add(ptsList[i]);
		}
		*/

	
		/*
		//Bring out of bounds points within bounds
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x <= 0) {
				ptsList[i].x = 0; 
				contact = true;
			}
			if (ptsList[i].x >= processor.getWidth() - 1) {
				ptsList[i].x = processor.getWidth() - 1;
				contact = true;
			}
			if (ptsList[i].y <= 0) {
				ptsList[i].y = 0;
				contact = true;
			}
			if (ptsList[i].y >= processor.getHeight() - 1) {
				ptsList[i].y = processor.getHeight() - 1;
				contact = true;
			}
			ptsArrayList.add(ptsList[i]);
		}
		*/
		
		//brings out of bounds points into bounds
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x < 0) ptsList[i].x = 0;
			if (ptsList[i].x > processor.getWidth() - 1) ptsList[i].x = processor.getWidth() - 1;
			if (ptsList[i].y < 0) ptsList[i].y = 0;
			if (ptsList[i].y > processor.getHeight() - 1) ptsList[i].y = processor.getHeight() - 1;
		}
			
		
		System.out.println("Segment contact: " + contact);
		segment.setExternalBoundaryContact(contact);
	
		
		//Remove duplicate points
		Set<Point> set  = new LinkedHashSet<Point>(ptsArrayList);
		ptsArrayList.clear();
		ptsArrayList.addAll(set);
		
		//Reconstruct points list as array
		Point[] ptsList2 = new Point[ptsArrayList.size()];
		for (int i = 0; i < ptsArrayList.size(); i++) {
			ptsList2[i] = ptsArrayList.get(i);
		}	
		return ptsList;
	}
	
	
	//Accessory Method: finds closest, lowest intensity point along line
	protected Point[] contractor (PointSet[] matchedPoints) {
		
		//convert to pointList
		LinkedList<Point> ptsLinkedList = new LinkedList<>();
		Point[] line;
		for (int n = 0; n < matchedPoints.length; n ++) {
			line = bresenham (matchedPoints[n].innerPoint, matchedPoints[n].outerPoint);
			ptsLinkedList.add(getThreasholdPoint(line));	
		}
		
		//convert to pointList
		Point[] ptsList = new Point[ptsLinkedList.size()];
		for (int i = 0; i < ptsLinkedList.size(); i++) {
			ptsList[i] = ptsLinkedList.get(i);
		}	
		return ptsList;
	}
	
	
	//private static int[][] bresenham(int x,int y,int x2, int y2, int radius) {
	protected static Point[] bresenham(Point innerPt, Point outerPt) {
	
		LinkedList<Point> line = new LinkedList<>();
		
		//x = centerpoint.x
		//x2 = centerpoint outer
		int x = innerPt.x;
		int y = innerPt.y;
		int w = outerPt.x - x;
	    int h = outerPt.y - y;
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
	
	
	//Gives info to the progress bar. 
	public void updateStatus() {
		
	}
	
	
	

	/**
	 * Methods required: 
	 * 1. getBlur() - Blurring parameters used for the guassian blur of the image. Default are those used for segmentation. If modifying, 
	 * 			calculate relative to the selections used for the ID - do not hard code.  
	 * 2. getPerimeter() - gives an ordered list of the object perimeter.  
	 * 3. 
	 */
	
	
	
	

	
	
	
	
}
