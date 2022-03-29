package externalSegmentation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Stack;

import externalSegmentation.ExternalSegmentation.PointSet;
import geometricTools.GeometricCalculations;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import ij.process.ImageProcessor;

public class OneWayContraction_Exclusion extends ExternalSegmentation {

	public OneWayContraction_Exclusion() {
		this.name = "One-Way Contraction_Exclusion";
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
		
		//finds the farthest away edge point - for the sake of single point segmentations. 
		double rad2 = Double.MIN_VALUE;
		Point edgePoint = null;
		for (int j = 0; j < edgeList.length; j++) {
			double r1 =  Math.pow(edgeList[j].x - x1, 2);
			double r2 =  Math.pow(edgeList[j].y - y1, 2);
			if ((r1 + r2) > rad2) {
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
	
		radd = pivot;
		
		//Holds the points
		LinkedList <Point> list = new LinkedList<Point>();
		
		//2c. loop around all possible points with four sectors
		for (int k = 0; k < 4; k++) {
			
			if (k == 0) { 
				for (int a = -pivot; a < pivot; a++){
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
				for (int a = pivot; a > -pivot; a--)  {
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + a, innerY - b));
				}
			}
			
			if (k == 3) {
				for (int a = -pivot; a < pivot; a++)  {
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
		Point darkestPt = pts[pts.length-1];
		//finds lowest intensity pixel FARTHEST from the centerpoint
		for (int i = pts.length-1; i > -1; i --) {
			if (processor.getPixel(pts[i].x, pts[i].y) < lowestIntensity) {
				lowestIntensity = processor.getPixel(pts[i].x, pts[i].y);
				darkestPt = pts[i];
			}
		}
		return darkestPt;
	}
	
	
	Point getNearestThreasholdPoint(Point[] pts) {
		int lowestIntensity = Integer.MAX_VALUE;
		Point darkestPt = pts[0];
		//finds lowest intensity pixel NEAREST to the centerpoint
		for (int i = 0; i < pts.length; i ++) {
			if (processor.getPixel(pts[i].x, pts[i].y) < lowestIntensity) {
				lowestIntensity = processor.getPixel(pts[i].x, pts[i].y);
				darkestPt = pts[i];
				//System.out.println("Point count is: " + i + "/" +( pts.length -1));
			}
		}
		return darkestPt;
	}
	
	
	Point startPoint;
	int skipAllowance = 20; //TODO: it makes sense for this to be proportional to sigma in some way
	

	//////////////////////////////////////Following Cold (method#4) works well enough//////////
	

	@Override //Accessory Method: finds closest, lowest intensity point along line
	protected Point[] contractor (PointSet[] matchedPoints) {
		
		ImageProcessor copyProcessor = processor.duplicate();
		
		LinkedList<Point> ptsLinkedList = new LinkedList<>();
		Point[] line;
		
		//first point
		line = bresenham (matchedPoints[0].innerPoint, matchedPoints[0].outerPoint);
		Point prev = getThreasholdPoint(line);
		Point startMark = prev;
		Point curr;
		ptsLinkedList.add(prev);	
		
		for (int n = 1; n < matchedPoints.length; n ++) {
		
			startPoint = matchedPoints[n].innerPoint;
			line = bresenham (matchedPoints[n].innerPoint, matchedPoints[n].outerPoint);
			curr = getThreasholdPoint(line);
			
			//checks if you are a significant distance from previous point
			if (pyth(prev, curr) > skipAllowance) {
				Point[] path = minimalPath(matchedPoints, n, prev, curr); //adds path
				System.out.println("Finished Path");
				System.out.println("Path needed. Length: " + path.length);
				for (int i = 0; i < path.length; i ++) {
					ptsLinkedList.add(path[i]);
				}
			}
			ptsLinkedList.add(curr);
			prev = curr;
		}
		
		//handles end points
		if (pyth(prev, startMark) > skipAllowance) {
			Point[] path = minimalPath(matchedPoints, 0, prev, startMark); //adds path
			System.out.println("Finished Path");
			System.out.println("Path needed. Length: " + path.length);
			for (int i = 0; i < path.length; i ++) {
				ptsLinkedList.add(path[i]);
			}
		}
		
		//convert to pointList
		Point[] ptsList = new Point[ptsLinkedList.size()];
		for (int i = 0; i < ptsLinkedList.size(); i++) {
			ptsList[i] = ptsLinkedList.get(i);
		}
		
		////TESTING
		for (Point pt : ptsList) copyProcessor.set(pt.x, pt.y, 255);
		
		//ImagePlus image = new ImagePlus("Test", copyProcessor);
		//image.show();
		
		return ptsList;	
	}
	
	
	
	
	protected Point[] minimalPath(PointSet[] matchedPoints, int index, Point startPoint, Point endPoint) {
		
		boolean solved = false;
		ArrayList<Point> nearList = new ArrayList<Point>();
		ArrayList<Point> farList = new ArrayList<Point>();
		boolean stepOut;
		
		//Aquires lines for points before and after the jump gap
		Point[] prevLine = bresenham (matchedPoints[index-1].innerPoint, matchedPoints[index-1].outerPoint);
		Point[] currLine = bresenham (matchedPoints[index].innerPoint, matchedPoints[index].outerPoint);
		
		//finds the location of the respective minimums farthest from center
		int prevIndex = thresholdIndex(prevLine, startPoint);
		int currIndex = thresholdIndex(currLine, endPoint);
		
		//if the previous is closer than the current, it is smallest and the point radius is stepping out
		int smallestIndex; 
		if (prevIndex < currIndex) {
			smallestIndex = prevIndex;
			stepOut = true;
		}
		//Otherwise, the current is the smallest and the point radius is stepping in. 
		else {
			smallestIndex = currIndex;
			stepOut = false;
			prevLine = currLine;
		}
		
	
		System.out.println("Smallest index: " + smallestIndex + "   currIndex: " + currIndex + "   prevIndex: " + prevIndex);
		
		
		for (int j = 0; j < matchedPoints.length; j++) {
			int n;
			//iterate forward on the line if stepping out.
			if (stepOut) {
				n = index + j;
				if (n > matchedPoints.length - 1) n = (n - matchedPoints.length + 1);
			}
			//Otherwise iterate backwards
			else {
				n = index - j;
				if (n < 0) n = (n + matchedPoints.length - 1);
			}
			
			//for each iteration, get the line from the centerpoint
			Point[] line = bresenham (matchedPoints[n].innerPoint, matchedPoints[n].outerPoint); 
			
			//XXX:adjust smallest index based on line length
			if (line.length != prevLine.length) {
				int stepDiff = line.length - prevLine.length;
				System.out.println("Line: " + line.length + ", prevLine: " + prevLine.length);
				System.out.println("Line lengths are different by: " + stepDiff + " at j = " + j);
				int newSmallestIndex = smallestIndex + stepDiff;
				System.out.println("smallIndex: " + smallestIndex + ", new smallest index: " + newSmallestIndex);
				smallestIndex = newSmallestIndex;
				if (smallestIndex < 1) smallestIndex = 1;
			}
			
			
			
			int maxInt = -1;
			int maxIndex = -1;
		
			//from the smallest index, iterate outward, finding brightest point in that range
			for (int i = smallestIndex; i < line.length; i ++) {
				if (processor.getPixel(line[i].x, line[i].y) > maxInt) {
					maxInt = processor.getPixel(line[i].x, line[i].y);
					maxIndex = i;
				}
			}

			//Find the most distal minimum between 0 and this highest intensity point
			Point[] line1 = Arrays.copyOfRange(line, 0, maxIndex); //inclusive of 0, exclusive of maxIndex
			Point pt1 = getThreasholdPoint(line1);
			nearList.add(pt1);
			
			//Find the most proximal minimum between the highest intensity point and the end of the line
			Point[] line2 = Arrays.copyOfRange(line, maxIndex, line.length);
			Point pt2 = getNearestThreasholdPoint(line2);
			farList.add(pt2);
		
			//Get smallest index location on line
			smallestIndex = thresholdIndex(line, pt1) + 1 ; //XXX: still not sure why +1 is neccessary. 
			int smallestIndex2 = thresholdIndex(line, pt1);
			System.out.println("Smallest index: " + smallestIndex + ", smallestIndex2: " + smallestIndex2);

			prevLine = line;
			//test if close
			//System.out.println("Distance between points: " + pyth(pt1, pt2));
			if (pyth(pt1, pt2) <= skipAllowance) {
				solved = true;
				break;
			}
			
		}
		
		
		
		
		if (solved) {
			//Combine and reorganize	
			Point[] ptsList = new Point[nearList.size() + farList.size() -1];
			if (stepOut) {
				for (int i = 0; i < nearList.size(); i ++) {
					ptsList[i] = nearList.get(i);
				}
				for (int i = 0; i < farList.size(); i ++) {
					ptsList[nearList.size() -1 + i] = farList.get(farList.size() -1 - i);
				}
			}
			else {
				
				for (int i = 0; i < farList.size(); i ++) {
					ptsList[i] = farList.get(i);
				}
				
				for (int i = 0; i < nearList.size(); i ++) {
					ptsList[farList.size() -1 + i] = nearList.get(nearList.size() -1 - i);
				}
			}
			System.out.println("ptsList Length: " + ptsList.length);
			
			
	
			
			//FOR CHECKING IF LOOP IS BAD IS BAD -- method 1
			Point[] brokenLine = bresenham (startPoint, endPoint);
			System.out.println("brokenline length: " + brokenLine.length);
			double avgIntBroken = 0;
			for (int i = 0; i < brokenLine.length; i ++) {
				avgIntBroken += processor.getPixel(brokenLine[i].x, brokenLine[i].y);
			}
			avgIntBroken = avgIntBroken / brokenLine.length;
			System.out.println("Broken average: " + avgIntBroken);
			
			double avgIntPtsList = 0;
			for (int i = 0; i < ptsList.length; i ++) {
				avgIntPtsList += processor.getPixel(ptsList[i].x, ptsList[i].y);
			}
			avgIntPtsList = avgIntPtsList/ ptsList.length;
			System.out.println("PtsList average: " + avgIntPtsList);
			
		
			if (avgIntPtsList < avgIntBroken) {
				return ptsList;	
			}
		}
		
		return new Point[] {};
	}
	
	
	protected int thresholdIndex(Point[] line, Point pt) {
		for (int i = 0; i < line.length; i ++) {
			if (line[i] == null) {
				System.out.println("Point on line @ " + i + " is: " + line[i].x + "," + line[i].y);
			}
			if (pt.x == line[i].x && pt.y == line[i].y) {
				return i;
			}
		}
		return 0;
	}
		
	//Euclidian distance between two points
	private static double pyth(Point pt1, Point pt2) {
		return Math.sqrt(
				Math.pow(pt1.x - pt2.x, 2) +
				Math.pow(pt1.y - pt2.y, 2)
			);
	}
	
	private boolean inBorder (Point pt) {
		if (pt.x > processor.getWidth() - 1) return false;
		if (pt.x < 0) return false;
		if (pt.y > processor.getHeight() - 1) return false;
		if (pt.y < 0) return false;
		return true;
	}
}
	
	
	
	
	