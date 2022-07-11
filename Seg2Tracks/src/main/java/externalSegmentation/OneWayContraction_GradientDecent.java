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

public class OneWayContraction_GradientDecent extends ExternalSegmentation {

	public OneWayContraction_GradientDecent() {
		this.name = "One-Way Contraction_Gradient Descent";
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
		
		double rad2 = Double.MAX_VALUE;
		Point edgePoint = null;
		for (int j = 0; j < edgeList.length; j++) {
			double r1 =  Math.pow(edgeList[j].x - x1, 2);
			double r2 =  Math.pow(edgeList[j].y - y1, 2);
			if ((r1 + r2) < rad2) {
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
	
		
		//TEMP: for surfaceTrace
		radd = pyth(innerBounds[0], outerPts[0]);
		
		
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
		
		ptList = GeometricCalculations.straightPerimeter(ptList);
		
		
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
		
		
		//Find maxima seeds for external points. 
		
		
		
		
		
		
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
	
	//Accessory Method: finds closest, lowest intensity point along line
	
	
	
	@Override //Accessory Method: finds closest, lowest intensity point along line
	protected Point[] contractor (PointSet[] matchedPoints) {
		
		ImageProcessor copyProcessor = processor.duplicate();
		
		//convert to pointList
		LinkedList<Point> ptsLinkedList = new LinkedList<>();
		Point[] line;
		for (int n = 0; n < matchedPoints.length; n ++) {
			
			
			//TESTING
			if (n % 10 == 0) {
				line = surfaceTrace(matchedPoints[n].innerPoint, matchedPoints[n].outerPoint);
				for (Point pt : line) {
					copyProcessor.set(pt.x, pt.y, 255);
				}
			}
			
			
			else {
				line = surfaceTrace(matchedPoints[n].innerPoint, matchedPoints[n].outerPoint);
			}
			ptsLinkedList.add(getThreasholdPoint(line));	
		}
		
		//convert to pointList
		Point[] ptsList = new Point[ptsLinkedList.size()];
		for (int i = 0; i < ptsLinkedList.size(); i++) {
			ptsList[i] = ptsLinkedList.get(i);
		}
		
		//TESTING
		//ImagePlus image = new ImagePlus("Test", copyProcessor);
		//image.show();
		
		return ptsList;
	}
	
	
	
	
	
	

	
	Point startPoint;
	protected Point[] surfaceTrace(Point startPoint, Point endPoint) {
		
		
		//Start with start point
		
		


		LinkedList<Point> line = new LinkedList<>();
		this.startPoint = startPoint;
		Point currentPoint = startPoint;
		do {
			line.add(currentPoint);
			currentPoint = search(currentPoint, endPoint);
		
		
			
		} while (!currentPoint.equals(endPoint) && pyth(currentPoint, startPoint) < radd -1);
		
		
		
		//Convert to Point[]
	    Point [] linePts = new Point[line.size()];
	    for (int i = 0; i < line.size(); i ++) {
	    	linePts[i] = line.get(i);
	    	//System.out.println("Linepts:  " + linePts[i].x + "," + linePts[i].y);
	    }
	    //System.out.println("linePts size is: " + linePts.length);
	    return linePts;
	}

	
	boolean prevDir = false;
	//highest exterior
	private Point search(Point currentPoint, Point endPoint) {
		
		double maxIntensity = -1;
		Point nextPoint = new Point();
		double distanceStartCurr = pyth(currentPoint, startPoint);
		
		//if (!inBorder(currentPoint)) return getBorder(currentPoint);
		
		double currentDistance = pyth(currentPoint, endPoint);
		for (int i= 0; i < 10; i ++) {
			Point pt = getNeighbors(currentPoint, i);
			if (pt.equals(endPoint)) return endPoint;
			if (!inBorder(pt)) continue;
			double distance = pyth(pt, endPoint);
			if (distance < currentDistance) {
				int intensity = processor.get(pt.x, pt.y);
				if (intensity == maxIntensity) {
					double distanceStartPt = pyth(pt, startPoint);
					if (distanceStartPt > distanceStartCurr) {
						maxIntensity = intensity;
						nextPoint = pt;
					}
				}
				if (intensity > maxIntensity) {
					maxIntensity = intensity;
					nextPoint = pt;
				}
			}
			if (distance == currentDistance) {
				System.out.println("Same distance found: " + distance);
				int intensity = processor.get(pt.x, pt.y);
				int currIntensity = processor.get(currentPoint.x,currentPoint.y);
				double distanceStartPt = pyth(currentPoint, startPoint);
				if (intensity > maxIntensity && distanceStartPt > distanceStartCurr) {
					maxIntensity = intensity;
					nextPoint = pt;
				}
				if (intensity == maxIntensity) {
					if (distanceStartPt > distanceStartCurr) {
						maxIntensity = intensity;
						nextPoint = pt;
					}
				}
			}
			
		}
		return nextPoint;
	}
	
	
	private boolean inBorder (Point pt) {
		if (pt.x > processor.getWidth() - 1) return false;
		if (pt.x < 0) return false;
		if (pt.y > processor.getHeight() - 1) return false;
		if (pt.y < 0) return false;
		return true;
	}
	

	// check pixel in a given direction from vertex (x,y)
    private static Point getNeighbors(Point pt, int direction) {
    	direction = direction % 8;
    	int x = pt.x;
    	int y = pt.y;
    	switch(direction) {        
            case 0: return new Point(x, y+1);
            case 1: return new Point(x, y-1);
            case 2: return new Point(x+1, y+1);
            case 3: return new Point(x-1, y+1);
            case 4: return new Point(x+1, y-1);
            case 5: return new Point(x-1, y-1);
            case 6: return new Point(x+1, y);
            case 7: return new Point(x-1, y);
        }
        return null; //will never occur, needed for the compiler
    }
	
	

	//Euclidian distance between two points
	private static double pyth(Point pt1, Point pt2) {
		return Math.sqrt(
				Math.pow(pt1.x - pt2.x, 2) +
				Math.pow(pt1.y - pt2.y, 2)
			);
	}
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	