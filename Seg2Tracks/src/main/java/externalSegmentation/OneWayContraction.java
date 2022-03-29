package externalSegmentation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import externalSegmentation.ExternalSegmentation.PointSet;
import geometricTools.GeometricCalculations;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ImageProcessor;

public class OneWayContraction extends ExternalSegmentation {

	public OneWayContraction() {
		this.name = "One-Way Contraction";
		this.description = " "; //TODO
	}

	@Override //Collects the internal points that should be used
	Point[] innerPoints(int currentSegment) {
		Point [] ptsList = new Point[1];
		ptsList[0] = segments.get(currentSegment).getCenterPoint();
		return ptsList;
	}
	
	@Override //Returns the centerpoint of the closest nearby segment
	Point[] outerPoints(Point [] centerPoint) {
		
		//Coordinates for the inner point
		int x1 = centerPoint[0].x;
		int y1 = centerPoint[0].y;
	
		//Finds the edge points to be included in this outline. 
		Point [] edgeList = {
				new Point(x1,0),
				new Point(x1,processor.getHeight() - 1),
				new Point(0,y1),
				new Point(processor.getWidth()-1,y1)
		};
		
		//Find the farthest edge neighbor of this point.
		double rad2 = Double.MIN_VALUE;
		Point edgePoint = null;
		for (int j = 0; j < edgeList.length; j++) {
			double r1 =  Math.pow(edgeList[j].x - x1, 2);
			double r2 =  Math.pow(edgeList[j].y - y1, 2);
			if ((r1 + r2)> rad2) {
				rad2 = r1 + r2;
				edgePoint = edgeList[j];
			}
		}	
			
				
		//Find the nearest segment neighbor of this point.
		int neighbor = -1;
		for (int j = 0; j < segments.size(); j++) {
			double r1 =  Math.pow(segments.get(j).getCenterPoint().x - x1, 2);
			double r2 =  Math.pow(segments.get(j).getCenterPoint().y - y1, 2);
			if ((r1 + r2) < rad2 && r1 + r2 != 0) {
				rad2 = r1 + r2;
				neighbor = j;
			}
		}
		
		Point [] ptList = new Point[1]; //Use single nearest neighbor for this method. 
		if (neighbor != -1) ptList[0] = segments.get(neighbor).getCenterPoint(); //If nearest point a segment
		else ptList[0] = edgePoint; //if nearest point an edge

		return ptList;
	}

	
	@Override //No boundary formed in this method, use centerpoint
	Point[] innerBounds(Point[] innerPts, Point[] outerPts) {
		return innerPts;
	}
	
	
	double radd;
	@Override //Calculates the pointlist for the boundary
	Point[] outerBounds(Point[] innerBounds, Point[] outerPts) {
		
		//Get Points 
		int innerX = innerBounds[0].x;
		int innerY = innerBounds[0].y;
		int outerX = outerPts[0].x;
		int outerY = outerPts[0].y;
		
		//Finds the distance to that nearest neighbor
		double r1 =  Math.pow(outerX - innerX, 2);
		double r2 =  Math.pow(outerY - innerY, 2);
		double rad = r1 + r2;
		
		//Parameters for generating circle
		int pivot = (int) Math.round(Math.pow(rad/2, 0.5));
		int radius = Math.round(Math.round(Math.pow(rad, 0.5)));
	
		
		//Holds the points
		LinkedList <Point> list = new LinkedList<Point>();
				
		//2c. loop around all possible points with four sectors
		for (int k = 0; k < 4; k++) {
			
			if (k == 0) { 
				for (int a= -pivot; a < pivot; a++){
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + a, innerY + b));
				}
			}
			
			if (k == 1) {
				for (int a = pivot; a > -pivot; a--)  {
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + b, innerY + a));
				}
			}
			
			if (k == 2) {
				for (int a= pivot; a > -pivot; a--)  {
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + a, innerY - b));
				}
			}
			
			if (k == 3) {
				for (int a= -pivot; a < pivot; a++)  {
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX - b, innerY + a));
				}
			}
		}
		
		Point[] ptList = new Point[list.size()];
		for (int i = 0; i < list.size(); i ++) {
			ptList[i] = list.get(i);
		}
		
		//ptList = GeometricCalculations.straightPerimeter(ptList);
		
		//TODO: Consider using Bresenham to fill in gaps?
		return ptList;
	}
	
	
	@Override //Link each point to the center
	PointSet[] boundaryMatch(Point[] innerBounds, Point[] outerBounds) {
		PointSet[] pointSetArray = new PointSet[outerBounds.length];
		for (int i = 0; i < outerBounds.length; i++) {
			pointSetArray[i] = new PointSet(innerBounds[0], outerBounds[i]);
		}
		return pointSetArray;
	}

	
	
	@Override //gets the thresholded point of the line
	Point getThreasholdPoint(Point[] pts) {
		
		int lowestIntensity = Integer.MAX_VALUE;
		Point darkestPt = pts[0];
		
	
		
		
		//finds lowest intensity pixel farthest from the centerpoint
		for (int i = pts.length-1; i > -1; i --) {
			if (processor.getPixel(pts[i].x, pts[i].y) < lowestIntensity) {
				lowestIntensity = processor.getPixel(pts[i].x, pts[i].y);
				darkestPt = pts[i];
			}
		}
		
		
		
		
		
		//OTHER OPTION: finds lowest intensity pixel closest to the centerpoint
		/*
		for (int i = 0; i < pts.length; i ++) {
			if (processor.getPixel(pts[i].x, pts[i].y) < lowestIntensity) {
				lowestIntensity = processor.getPixel(pts[i].x, pts[i].y);
				darkestPt = pts[i];
				System.out.println("Point count is: " + i + "/" +( pts.length -1));
			}
		}
		*/
		
	
		//OTHER OPTION: Generates darkest point using a threasholding method. 
		/*
		//generates histogram for image
		int [] histogram = new int[256]; 
		for (int i = pts.length-1; i > -1; i --) {
			histogram[processor.getPixel(pts[i].x, pts[i].y)]++;
		}
		
		//finds lowest using threashold
		AutoThresholder thresh = new AutoThresholder();
		int trs = thresh.getThreshold(Method.Triangle, histogram); 
		
		for (int i = 0; i < pts.length; i ++) {
			if (processor.getPixel(pts[i].x, pts[i].y) < trs) {
				darkestPt = pts[i];
				break;
			}
		}
		*/
		
		return darkestPt;
	}
}
