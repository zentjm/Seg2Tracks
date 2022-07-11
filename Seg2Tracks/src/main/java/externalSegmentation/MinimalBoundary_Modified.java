package externalSegmentation;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;



import externalSegmentation.ExternalSegmentation.PointSet;
import geometricTools.PolarPoint;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import geometricTools.GeometricCalculations;

public class MinimalBoundary_Modified extends ExternalSegmentation {

	public MinimalBoundary_Modified() {
		this.name = "Minimal Boundry Modified";
		this.description = " "; //TODO
	}

	@Override //Just the center point
	Point[] innerPoints(int currentSegment) {
		Point [] ptsList = new Point[1];
		ptsList[0] = segments.get(currentSegment).getCenterPoint();
		return ptsList;
	}

	@Override // Points defined by minimal convex boundary 
	Point[] outerPoints(Point[] innerPoints) {
		
		Point centerPoint = innerPoints[0];
		
		//Holds polar point array
		ArrayList<PolarPoint> pts = new ArrayList<PolarPoint>();
		
		/**
		 * 1. Create point list using all other segments and include edge points
		 */
		
		//Adds edge points
		pts.add(new PolarPoint((new Point(centerPoint.x, 0)), centerPoint));
		pts.add(new PolarPoint((new Point(centerPoint.x, processor.getHeight() - 1)), centerPoint));
		pts.add(new PolarPoint((new Point(0, centerPoint.y)), centerPoint));
		pts.add(new PolarPoint((new Point(processor.getWidth() - 1, centerPoint.y)), centerPoint));
		
		//Adds remaining points
		for (int i = 0; i < segments.size(); i++) {
			 if (segments.get(i).getCenterPoint().equals(centerPoint)) {
				 continue;
			 }
			 pts.add(new PolarPoint(segments.get(i).getCenterPoint(), centerPoint));
		}
		
		/**
		 * 2. Order point list by polar coordinate spiral
		 */
		
		//Convert to array for sorting
		PolarPoint[] ptArray = new PolarPoint[pts.size()];
		for (int i = 0; i < pts.size(); i++) {
			ptArray[i] = pts.get(i);
		}
		
		//Sort by R
		Arrays.sort(ptArray, new Comparator<PolarPoint>() {
			public int compare(PolarPoint a, PolarPoint b) {
				return (a.getR() < b.getR() ? -1 : a.getR() > b.getR() ? 1 : 0);
			}
		});
		
		//TESTING
		/*
		System.out.println("Centerpoint: x = " + centerPoint.getX() + "   x = " + centerPoint.getY());
		for (int i = 0; i < ptArray.length; i ++) {
			System.out.println("Organized ptArray at " + i + ", r: " + ptArray[i].getR() + "   theta: " + ptArray[i].getTheta());
		}
		*/
	
		/**
		 * 3. Find first triangle that overlays the cell //TODO: fewer than 3 points, square problem, **make WHILE loop until closed hull
		 */
		
		//Ordered ArrayList
		ArrayList<PolarPoint> outerPoints = new ArrayList<PolarPoint>(); //holds triangle
		
		//Add first and second closest points (closest)
		outerPoints.add(ptArray[0]);
		PolarPoint point2 = ptArray[1];
		outerPoints.add(point2);
		
		//Order points by increasing theta
		if (outerPoints.get(0).getTheta() > outerPoints.get(1).getTheta()) {
			outerPoints.add(outerPoints.remove(0));

		}
		
		//revert order if crosses the 0 marker //TODO: may be a more general problem. 
		if ((Math.abs(outerPoints.get(1).getTheta() - outerPoints.get(0).getTheta()) > Math.PI) && 
				(Math.abs(outerPoints.get(0).getTheta() - outerPoints.get(1).getTheta()) > Math.PI)) {
			outerPoints.add(outerPoints.remove(0));
			
		}
		
		//System.out.println("First Point theta: " + outerPoints.get(0).getTheta());
		//System.out.println("Second Point theta: " + outerPoints.get(1).getTheta());
		
		//Search range for third point
		double thetaLow = normalizeRads(outerPoints.get(0).getTheta() - Math.PI);
		double thetaHigh = normalizeRads(outerPoints.get(1).getTheta() - Math.PI);
		
		
		//System.out.println("Theta Low is: " + thetaLow);
		//System.out.println("Theta High is: " + thetaHigh);
		
		
		//Finds third point
		double tempR = Double.MAX_VALUE;
		PolarPoint point3 = null;
		for (int k = 0; k < ptArray.length; k++) {
			//if (thetaLow < ptArray[k].getTheta() && ptArray[k].getTheta() < thetaHigh) {
			if (searchAngle(ptArray[k].getTheta(), thetaLow, thetaHigh)) {
				if (ptArray[k].getR() < tempR) {
					tempR = ptArray[k].getR();
					point3 = ptArray[k];
				}
			}
		}
	
		//private boolean searchAngle (double theta, double start, double end) {
		
		//TESTING
		//if (point3 != null)	System.out.println("Third point found");
		//if (point3 == null)	System.out.println("Third point NOT found");
		
		//If third point not found find edge point //XXX: because R iterates +1 but is non-integer double, not likely finding exact edge.
		if (point3 == null) {
			//double thetaAvg = normalizeRads(thetaHigh - thetaLow);
			double thetaAvg = averageAngle(thetaLow, thetaHigh);
			int tempR2 = 0; //TODO: we know the minimum is the closest distance of the centerpoint to the wall -1
			
			//System.out.println("Average angle is: " + thetaAvg);

			//Extend r until it hits boundary of image
			while (true) { //TODO: Sloppy code
				int nextX = (int) Math.round((((tempR2 + 1) * Math.cos(thetaAvg)))) + centerPoint.x; //Accuracy
				int nextY = (int) Math.round((((tempR2 + 1) * Math.sin(thetaAvg)))) + centerPoint.y;
				if (nextX < 0 || nextX > processor.getWidth() - 1) break;
				if (nextY < 0 || nextY > processor.getHeight() - 1) break;
				tempR2 ++;
			}
			
			//Necessary adjustment to properly orient angle with respect to centerPoint
			int x3 = (int) Math.round((((tempR2 + 1) * Math.cos(thetaAvg)))) + centerPoint.x;
			int y3 = (int) Math.round((((tempR2 + 1) * Math.sin(thetaAvg)))) + centerPoint.y;
			point3 = new PolarPoint (new Point(x3, y3), centerPoint);
		}
		
		//Add third point
		outerPoints.add(point3);
		//System.out.println("Third point is: r = " + point3.getR() + "   theta = " + point3.getTheta());
		//System.out.println("Third point is: x = " + point3.getCartesian().getX() + "   y = " + point3.getCartesian().getY());
		
		
		
		//GOOD UP UNTIL HERE
		
		
		/**
		 * 4. Run broken angles to get final picture
		 */
		
		
		boolean thirdRemoved = false;
		boolean secondRemoved = false;
		
	
		ArrayList<PolarPoint> jList;
		
		for (int i = 0; i < outerPoints.size(); i++) {
				
			//System.out.println("outerPoints.size = " + outerPoints.size());
			
			int n = i + 1;
			if (i >= outerPoints.size() - 1) n = 0; //circular condition
			
			//System.out.println("i = " + i + ", n = "+ n);
			
			//get search angles
			double theta1 = outerPoints.get(i).getTheta();
			double theta3 = outerPoints.get(n).getTheta();
			
			//Search list
			jList = new ArrayList<PolarPoint>();
			
			//find the smallest point within the angle
			//tempR = Double.MAX_VALUE;
			//int minJ = -1;
			for (int j = 0; j < ptArray.length; j++) {
				if (outerPoints.contains(ptArray[j])) continue; //skip contained points
				if (searchAngle(ptArray[j].getTheta(), theta1, theta3)) {
					
					//for multi-test
					jList.add(ptArray[j]);
					
					/*for single test
					if (ptArray[j].getR() < tempR) {
						tempR = ptArray[j].getR();
						minJ = j;
					}
					*/
					
				}
			}
			
			if (jList.isEmpty()) continue;
			
			//Convert to array for sorting
			PolarPoint[] jArray = new PolarPoint[jList.size()];
			for (int z = 0; z < jList.size(); z++) {
				jArray[z] = jList.get(z);
			}
			
			//Sort for minimum R
			Arrays.sort(jArray, new Comparator<PolarPoint>() {
				public int compare(PolarPoint a, PolarPoint b) {
					return (a.getR() < b.getR() ? -1 : a.getR() > b.getR() ? 1 : 0);
				}
			});
			
			boolean completed = false;
			int g = 0;
			while (!completed) {
				
				PolarPoint nextPoint = jArray[g];
				
				//Get dimensions of point
				double r2 = nextPoint.getR();
				double theta2 = nextPoint.getTheta();
				
				//Get angles relative to point
				//double lowerAngle = Math.abs(theta2 - theta1); //TODO: Correct for special cases
				//double higherAngle = Math.abs(theta3 - theta2); //TODO: Correct for special cases
				
				double lowerAngle = subtractAngles(theta1, theta2); //TODO: Correct for special cases
				double higherAngle = subtractAngles(theta2, theta3);
						
				//Get angles of bounding point
				double r1 = outerPoints.get(i).getR();
				double r3 = outerPoints.get(n).getR();
				
				//Determine lengths of opposing vectors
				double lowerR = Math.sqrt(Math.pow(r1, 2) + Math.pow(r2,2) - (2 * r1 * r2 * Math.cos(lowerAngle)));
				double higherR = Math.sqrt(Math.pow(r3, 2) + Math.pow(r2,2) - (2 * r3 * r2 * Math.cos(higherAngle)));
				
				//Determine the bend angles
				double lowerBend = Math.acos((Math.pow(r1, 2) + Math.pow(lowerR, 2) - Math.pow(r2, 2))/(2 * r1 * lowerR));
				double higherBend = Math.acos((Math.pow(r3, 2) + Math.pow(higherR, 2) - Math.pow(r2, 2))/(2 * r3 * higherR));
				
				//System.out.println("lowerBend = " + lowerBend);
				//System.out.println("higherBend = " + higherBend);
				
				//double lowerBend = Math.asin(r2 * Math.sin(lowerAngle)/lowerR);
				//double higherBend = Math.asin((r2 * Math.sin(higherAngle) / higherR));
				
				//Determine if bend angles are both less than pi/2
				if (lowerBend <= (Math.PI/2) && higherBend <= (Math.PI/2)) {
					if (g > 0) {
						//TODO: Check if resulting contains another point?
						//By definition, would the smaller point have not been selected if that was the case?
					}
					
					outerPoints.add(n, nextPoint); //
					completed = true;
					i --;
					//i = 0;
					//System.out.println("Found match, outerPoints size: " + outerPoints.size());
					
					
					
					//private boolean polarContains (PolarPoint removedPoint, PolarPoint polarCenterPoint, ArrayList<PolarPoint> list) {
					if (thirdRemoved) {
						if (polarContains(point3, centerPoint, outerPoints)) {
							outerPoints.remove(point3);
							thirdRemoved = true;
							i = 0;
						}
					}
					
					if (secondRemoved) {
						if (polarContains(point2, centerPoint, outerPoints)) {
							outerPoints.remove(point2);
							secondRemoved = true;
							i = 0;
						}
					}
					
					
				}
				else {
					if (g == jArray.length - 1) completed = true;
					else g++;
				}
				
				
				
				//TODO: check vaildity of second and third points. 
			}		
		}
	
				
				
				
				
				/*test removal of third point
				if (thirdRemoved) {
				
					int x[] = new int[outerPoints.size()];
					int y[] = new int[outerPoints.size()];
					
					int thirdPoint = -1;
					for (int k = 0; k < outerPoints.size(); k++) {
						if (!outerPoints.get(k).equals(point3)) {
							x[k] = outerPoints.get(k).getCartesian().x;
							y[k] = outerPoints.get(k).getCartesian().y;
							System.out.println("Found third point");
						}
						else {
							thirdPoint = k;
						}
					}
					
					PolygonRoi testPoly = new PolygonRoi(x,y,x.length, Roi.POLYGON);
					if (testPoly.contains(centerPoint.x, centerPoint.y)) {
						outerPoints.remove(thirdPoint);
						if (i > 0) i--;
						thirdRemoved = true;
						System.out.println("Third point deleted");
					}		
				}
				
				
				
				//test removal of second point
				if (secondRemoved) {
					
		
					int x[] = new int[outerPoints.size()];
					int y[] = new int[outerPoints.size()];
					
					int secondPoint = -1;
					for (int k = 0; k < outerPoints.size(); k++) {
						if (!outerPoints.get(k).equals(point2)) {
							x[k] = outerPoints.get(k).getCartesian().x;
							y[k] = outerPoints.get(k).getCartesian().y;
						}
						else {
							secondPoint = k;
						}
					}
					
					PolygonRoi testPoly = new PolygonRoi(x,y,x.length, Roi.POLYGON);
					if (testPoly.contains(centerPoint.x, centerPoint.y)) {
						outerPoints.remove(secondPoint);
						if (i > 0) i--;
						secondRemoved = true;
						System.out.println("Second point deleted");
					}		
				}
				*/
				
		
		
		
		//Convert to Point array
		Point[] outerPointsArray = new Point[outerPoints.size()];
		for (int i = 0; i < outerPoints.size(); i++) {
			outerPointsArray[i] = outerPoints.get(i).getCartesian();
		}
		return outerPointsArray;
	}

	private double normalizeRads(double theta) {
		if (theta > (2 * Math.PI)) return (theta % (2*Math.PI)); 
		if (theta < (-2 * Math.PI)) theta = theta % (2*Math.PI); 
		if (theta < 0) return (2 * Math.PI) + theta;
		else return theta;
	}

	private double subtractAngles(double theta1, double theta2) {
		if (theta2 - theta1 >= 0) return theta2 - theta1;
		else return theta2 - theta1 + 2*Math.PI;
	}
	
	private boolean searchAngle (double theta, double start, double end) {
		if (end - start >= 0) return (start < theta && theta < end);
		if (start < theta && theta < (2* Math.PI)) return true;  //if theta is between the first and 2pi, must be true
		if (theta < end) return true; //if theta is lower than end, must be true. 
		return false;
	}
	
	private double averageAngle (double start, double end) {
		return Math.atan2((0.5 * (Math.sin(end) + Math.sin(start))), (0.5 * (Math.cos(end) + Math.cos(start))));
	}

	private boolean polarContains (PolarPoint removedPoint, Point centerPoint, ArrayList<PolarPoint> list) {
		int x[] = new int[list.size()];
		int y[] = new int[list.size()];

		for (int i = 0; i < list.size(); i++) {
			if (!list.get(i).equals(removedPoint)) {
				x[i] = list.get(i).getCartesian().x;
				y[i] = list.get(i).getCartesian().y;
			}
		}
		
		PolygonRoi poly = new PolygonRoi(x,y,x.length, Roi.POLYGON);
		if (poly.contains(centerPoint.x, centerPoint.y)) return true;
		return false;
	}
		
	
	@Override //No boundary formed in this method, use centerpoint
	Point[] innerBounds(Point[] innerPts, Point[] outerPts) {
		return innerPts;
	}

	@Override //connect points
	Point[] outerBounds(Point[] innerBounds, Point[] outerPts) {
		return GeometricCalculations.straightPerimeter(outerPts);
	}

	@Override //Match each point to center
	PointSet[] boundaryMatch(Point[] innerBounds, Point[] outerBounds) {
		PointSet[] pointSetArray = new PointSet[outerBounds.length];
		for (int i = 0; i < outerBounds.length; i++) {
			pointSetArray[i] = new PointSet(innerBounds[0], outerBounds[i]);
		}
		return pointSetArray;
	}

	@Override //
	Point getThreasholdPoint(Point[] pts) {

		//return pts[pts.length - 1]; //Testing

		
		// TODO: implement this for contractor
		
		
		int lowestIntensity = Integer.MAX_VALUE;
		Point darkestPt = pts[0];
		
		//finds lowest intensity pixel
		//for (int i = 0; i < pts.length; i ++) {
		for (int i = pts.length - 1; i > -1; i --) {
			if (processor.getPixel(pts[i].x, pts[i].y) < lowestIntensity) {
				lowestIntensity = processor.getPixel(pts[i].x, pts[i].y);
				darkestPt = pts[i];
			}
		}
		return darkestPt;
		//
		
	}

}
