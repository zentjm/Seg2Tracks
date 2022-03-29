package geometricTools;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;



import dataStructure.Segment;
import geometricTools.ModifiedAutoThresholder.Method;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class GeometricCalculations {
	

	
	
	//Detects and removes static
	public static Point[] attenuateStatic(Point[] pts) {
		
		int[] noise = new int[pts.length];
		
		//do first one and last one
		
		int range = 1;
		//Evaluate static along line
		for (int i = range; i < pts.length - range; i ++) {
			int xSum = 0;
			int ySum = 0;
			for (int j = i - range; j < i + range - 1; j ++) {
				int x = pts[j+1].x - pts[j].x;
				int y = pts[j+1].y - pts[j].y;
				xSum += x;
				ySum += y;
			}
			
			double xDiff = xSum;
	        double yDiff = ySum;
	        double angle = Math.toDegrees(Math.atan2(yDiff, xDiff));
			
			
			int sum = (int)Math.round(Math.pow(Math.pow(xSum, 2) + Math.pow(ySum, 2), 0.5));
			
			//System.out.println("Point: " + i + "   Angle: " + angle);
			//System.out.println("Point: " + i + "   Sum: " + sum);
			//System.out.println("Point: " + i + "   ySum: " + ySum + "   xSum: " + xSum);
		}
		
		//arraylist and transfer
		ArrayList<Point> ptsArray = new ArrayList<Point>();
		for (Point pt: pts) ptsArray.add(pt);
		return null;
	}
	

	
	//Snips off excess curves
	public static Point[] snip (Point[] pts) {
		
		//arraylist and transfer
		ArrayList<Point> ptsArray = new ArrayList<Point>();
		for (Point pt: pts) ptsArray.add(pt);
		
		int searchRadius = 500;
		int gap = 10;
		int repeats = 1;
		
		for (int k = 0; k < repeats; k ++) {
			for (int i = 0; i < ptsArray.size(); i ++) {
				for (int j = i; j < Math.min(i + searchRadius, ptsArray.size()); j ++) {	
					if (Math.abs(i - j) > gap ) { //skip close points nearby
						if (Math.abs(ptsArray.get(i).x - ptsArray.get(j).x) < gap) {
							if (Math.abs(ptsArray.get(i).y - ptsArray.get(j).y) < gap) {
								ptsArray.subList(i + 1  , j -1).clear();
								break;
							}
						}
					}	
				}	
			}
		}
		
		Point[] pts2 = new Point[ptsArray.size()];
		for (int i = 0; i < pts2.length; i++) {
			pts2[i] = ptsArray.get(i);
		}
		
		return pts2 ;
	}
	
	
	
	
	
	
	//Calculates a smoothing spline
	public static Point[] smooth (Point[] pts) {
		
		//Stretch = place before and after with biggest effect
		int search = 5; //TODO: Should be adjustable
		double [][] vectorList = new double[pts.length][2];
		//System.out.println("Points length is: " + pts.length);
		for (int i = 0; i < pts.length; i ++) {
			int xSum = 0;
			int ySum = 0;
			int count = 0;
			//System.out.println("Variable XXX i is: " + i);
			for (int j = i - search; j < i + search - 1; j ++) { //scan whole search area
				int k = j;
				if (j < 0)  k = pts.length + j - 1;
				if (j > pts.length - 2) k = j - pts.length + 1;
				//System.out.println("Variable j is: " + j);
				//System.out.println("temp k is: " + k);
				xSum += pts[k + 1].x - pts[k].x;
		        ySum += pts[k + 1].y - pts[k].y;
		        count ++;
			}

			//adds to vector list
			vectorList[i][0] = xSum/count;
			vectorList[i][1] = ySum/count;
		}
	
		Point[] ptsList = new Point[pts.length]; //generate new points list
		ptsList[0] = pts[0]; //Set to same starting location
		for (int i = 1; i < ptsList.length; i ++) {
			int x = ptsList[i-1].x + (int) vectorList[i][0];
			int y = ptsList[i-1].y + (int) vectorList[i][1];
			Point pt = new Point(x,y);
			//System.out.println("X is: " + x + ",  Y is: " + y);
			ptsList[i] = pt;
			
		}
				
		return ptsList;	
	}
	

	public static boolean boundryOverlap(Point[] A, Point[] B, double threashold) {
	
		int count = 0;
		if (threashold < 0) threashold = 0;
		if (threashold > 1) threashold = 1;
		int returnCount = 0;
		if (threashold > 0) returnCount = (int) (threashold * getAreaByRoi(A).length);
			
		
		for (Point pt1: A) {
			for (Point pt2: B) {
				if (pt1.x == pt2.x && pt1.y == pt2.y) {
					count ++;
					if (count > returnCount) return true;
				}
			}
		}
		return false;
	}
	

	
	public static double[] getIntensity (Point[] pointList, ImagePlus target) {
		
		Point areaPointList[] = getAreaByRoi(pointList);
		int [] intensityList = new int[areaPointList.length];
		ImageProcessor pr = target.getProcessor();
	
		int intensity = 0;
		
		for (int i = 0; i < areaPointList.length; i++) {
			intensityList[i] = pr.get(areaPointList[i].x, areaPointList[i].y);
			intensity += pr.get(areaPointList[i].x, areaPointList[i].y);
		}
		
		//sort ascending
		Arrays.sort(intensityList);
		
		//mean
		double mean = intensity/intensityList.length;
		
		//median
		double median = -1;
		if (intensityList.length % 2 == 1) median = intensityList[(intensityList.length + 1)/2 - 1];
		else median = intensityList[((intensityList.length/2 - 1) + (intensityList.length/2))/2];
	
		//mode
		double mode = -1;
		int count = 0;
		int maxCount = 0;
		for (int i = 1; i < intensityList.length; i++) {
			if (intensityList[i] != intensityList[i-1]) {
				count = 0;
				continue;
			}
			count ++;
			if (count > maxCount) {
				maxCount = count;
				mode = intensityList[i];
			}
		}
		
		//results
		double[] results = {
			(double) intensity, 		//1. integrated intensity
			mean, 						//2. mean
			median, 					//3. median
			mode						//4. mode
		};
		return results;
	}
	
	
	
	
	
	
	
	
		
		
		
	//Gets the area of the roi by generating a new polygon roi
	public static Point[] getAreaByRoi (Point[] pointList) {
		
		//pointList = straightPerimeter(pointList);
		
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = -1;
		int maxY = -1;
		
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i ++) {
			
			if (pointList[i].x < minX) minX = pointList[i].x;
			if (pointList[i].y < minY) minY = pointList[i].y;
			if (pointList[i].x > maxX) maxX = pointList[i].x;
			if (pointList[i].y > maxY) maxY = pointList[i].y;
			
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		
		Roi poly = new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
		
		Point [] pointList2 = poly.getContainedPoints();
		
		return pointList2;
	}
	
		

	public static PolygonRoi getPolygonRoi(Point[] pointList) {
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
	}
	

	//Gets area from perimeter //TODO: think there are some bugs
	public static Point[] getArea (Point[] pts) {
		
		//TODO - 1. Arrange the points for easy identificaiton
		
		//collect and sort points
		pts = pts.clone(); //XXX
		pts = straightPerimeter(pts);
		
		
		//Sorts Y points
		Arrays.sort(pts, new Comparator<Point>() {
			@Override
			public int compare (Point a, Point b) {
				/*
				if (a.y < b.y) return -1;
				if (a.y > b.y) return 1;
				if (a.x < b.x) return -1;
				if (a.x > b.x) return 1;
				else return 0;
				*/
				
				return (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : (a.x < b.x) ? -1 : (a.x > b.x) ? 1 : 0;
			}
		});
		
		/*
		//Sorts X points
		Arrays.sort(pts, new Comparator<Point>() {
			@Override
			public int compare (Point a, Point b) {
				return (a.x < b.x) ? -1 : (a.x > b.x) ? 1 : 0; // (a.y < b.y) ? -1 : (a.y > b.y) ? 1 : 0;
			}
		});
		*/
		
		
		//TODO - 2. Restrict the search to the bounding rectangle (exponentially more important with bigger images)
		
		
		//TODO: - 3. Search the rectangle and record. 
		

		boolean collect = false;

	/*
		//Find the point borders?
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		for (int i = 0; i < pts.length; i++) {
			int x = pts[i].x;
			int y = pts[i].y;
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
		}
		*/
		
		/*
		 * grab = 0 --> no prev pixel - OK to Flip
		 * grab = 1 --> has prev pixel - NO Flip
		 * 
		 */
		
		int count = 0;
		int grab = 0;
		ArrayList<Point> newPts = new ArrayList<>();
		//Y-scan
		collect = false;
		for (int  i = 1; i < pts.length -1; i ++) {
			
			//all perimeter points are included - TODO: include perimeter?
			newPts.add(pts[i]); 
			
			//If the previous point is too the right dont change the collect
			if (pts[i].x == pts[i-1].x + 1) grab = 1;//previous point exists. 
			else grab = 0;
			
			//If the next point is empty change the collect
			if (pts[i].x != pts[i+1].x) collect = !collect;   ///TODO: INVERT the raster scan with each line, and make sure it is a polyline. 

			//If restarting line make collect false
			if (pts[i].y > pts[i-1].y) {
				grab = 0; //restart at new line
				collect = false;
			}
			
			//System.out.println("Grab is: " + grab + " collect is: " + collect);
			if (grab == 0 && collect) {
				//System.out.println("Initiated");
				for (int j = pts[i-1].x + 1; j < pts[i].x; j++) { //add internal points
					newPts.add(new Point(j, pts[i].y));
					count ++;
				}
			}
		}
		
		System.out.println("Points Length: " + pts.length);
		System.out.println("New Points Length: " +  newPts.size());
		System.out.println("Center Points Added: " + count);
		
		
		Point[] pts2 = new Point[newPts.size()];
		for (int j = 0; j < newPts.size(); j ++) {
			pts2[j] = newPts.get(j);
		}
		
		
		return pts2;
	}
	
	//Collect all the points across the entire line
	public Point[] collectLine (Point [] pts, int start, int end) {
		for (int i = 0; i < end; i ++) {			
		}
		return null;
	}
	
	//allows skipping for neighbors
	public static Point[] shortcutPerimeter (Point[] pts) {
		
		//Check for no points
		if (pts.length == 0) {
			//System.out.println("This pointlist has no points");
			return pts; //deals with null arrays 
		}
		
		//Returns if just a couple of points have been found //TODO: Really important
		if (pts.length < 200) {
			return pts;
		}
		
		//return pts;
		return  shortcutPerimeter(pts, 100, 2, 2, 4);
	}
	
	//Perimeter filter
	public static Point[] shortcutPerimeter (Point[] pts, int searchDistance, int range, int smoothing, int minimumSize) {
		System.out.println("Running Shortcut");
		
		
		
		//Check for no points
		if (pts.length == 0) {
			//System.out.println("This pointlist has no points");
			return pts; //deals with null arrays 
		}
		
		//Returns if just a couple of points have been found //TODO: expand
		if (pts.length < 50) {
			return pts;
		}
		
		
		//Convert to arrayList
		ArrayList<Point> ptsList = new ArrayList<Point>();
		for (int i = 0; i < pts.length; i ++) {
			ptsList.add(pts[i]);
		}
		
		//setup index
		int index1 = 0;
		int index2 = 0;
		
		//iterate ArrayList
		//for (int i = 0 ; i < ptsList.size() - searchDistance; i++) {
		for (int i = 0 ; i < ptsList.size() - 1; i++) {
			
			//iterate index1
			index1  = i;
			if (index1 > ptsList.size() - 1) {
				index1 = index1 - ptsList.size() + 1;
			}	
			
			Point pt1 = ptsList.get(index1);
			
			//Point pt1 = ptsList.get(index1 + i < ptsList.size() - 1 ? index1 + i : index1 + i - ptsList.size() + 1);
			
			//Point pt1 = ptsList.get(index1);
		
			//search ahead for shortcuts
			for (int j = range; j < searchDistance; j++) {
				index2 = (index1 + j < ptsList.size() -1) ? index1 + j : index1 + j - ptsList.size() + 1;
				Point pt2 = ptsList.get(index2);
				
				//System.out.println("Point 1 at i =" + i + "  index1: " + index1 + " xy: " + pt1.x + "," + pt1.y +
				//		"    Point 2 at j =" + j + "  index2: " + index2 + " xy: " + pt2.x + "," + pt2.y );
						
					
			
				//System.out.println("index1:" + index1 + "   index2:" + index2);
				
				//check if a shortcut is found
				//if (index2 - index1 > 2 ) { //list adjacency check
					if (Math.abs(pt1.x - pt2.x) + Math.abs(pt1.y - pt2.y) < smoothing) { //spatial check for left, right, up, or down
						
						//System.out.println("HIT!!!    Point 1 at i =" + i + "  index1: " + index1 + " xy: " + pt1.x + "," + pt1.y +
						//"    Point 2 at j =" + j + "  index2: " + index2 + " xy: " + pt2.x + "," + pt2.y );
						
						
						//System.out.println("Neighbor detected");
						//delete all values between i and j
						if (index1 < index2) {
							for (int k = index1 + 1; k < index2; k++) {
								//System.out.println("Removed point @ " + k + ": " + ptsList.get(k).x + "," + ptsList.get(k).y);
								ptsList.set(k, null);
							}
						}
						
						if (index1 > index2) {
							for (int k = index1 + 1; k < ptsList.size() - 1; k++) {
								//System.out.println("Removed point @ " + k + ": " + ptsList.get(k).x + "," + ptsList.get(k).y);
								ptsList.set(k, null);
							}
							
							for (int k = 0; k < index2; k++) {
								//System.out.println("Removed point @ " + k + ": " + ptsList.get(k).x + "," + ptsList.get(k).y);
								ptsList.set(k, null);
							}
						}
						
						while (ptsList.remove(null));
						
						//reset i
						//System.out.println("reset i");
						i = 0;
						j = range;
						
						//break loop
						break;
					}
				//}
					
					//System.out.println("ptsList length is: " + ptsList.size());
			}
			//System.out.println("Index:" + index1 + "    PtsList size is:" + ptsList.size());
			if (ptsList.size() < minimumSize) break;
		}
		
	
		//Strip out nulls
		/*
		ArrayList<Point> ptsList2 = new ArrayList<Point>();
		for (Point pt : ptsList) {
			if (pt != null) ptsList2.add(pt);
		}
		*/
		
		
		Point[] newPts = new Point[ptsList.size()];
		ptsList.toArray(newPts);
		
		//System.out.println("Initial ptsList:" + pts.length + "    New ptsList:" + newPts.length);
		
		return newPts;
	}
		//--------
		
		/*
		System.out.println("Starting SHORTCUT...");
		
	
		if (pts.length == 0) {
			System.out.println("This pointlist has no points");
			return pts; //deals with null arrays 
		}
		
		//empty Pts list: 
		//Point[] newPts = new Point[pts.length];
		
		//empty arrayList
		ArrayList<Point> newPtsList = new ArrayList<Point>();
	
		//int index = 0;
		int offset = 0;
		boolean detected = false;
		int index = 0;
		for (int i = 0 ; i < pts.length; i++) {
			offset = 0;
			Point pt1 = pts[i];
			if (i < pts.length - searchDistance) {
				for (int j = i; j < i + searchDistance; j++) {
					Point pt2 = j < pts.length - 1 ? pts[j] : pts [j - pts.length + 1];
					if (j > range + i) { //not sequential
						if (Math.abs(pt1.x - pt2.x) + Math.abs(pt1.y - pt2.y) < 2) { //physically next to each other
							offset = j - i;
							System.out.println("RANGE:" + range);
							System.out.println("Shorcut between Point at i:" + i + "  j:" + j);
							detected = true;
						}
					}
				}
			}
			
			index = index + offset < pts.length - 1 ? index + offset : index + offset - pts.length + 1;
			newPtsList.add(pts[index]);
			index ++;
			System.out.println("   Index: " + index + "  Offset:" + offset + "   pts.length:" + pts.length);
		}

		
		Point[] newPts = new Point[newPtsList.size()];
		
		newPtsList.toArray(newPts);
		
		//Foo[] array = new Foo[list.size()];
		//list.toArray(array); // fill the array
		
		
		/*
		//trim new array
		newPts = Arrays.copyOf(newPts, index);
		System.out.println("Old Array Length: " + pts.length);
		System.out.println("New Array Length: " + newPts.length);
		
		
		for (int i = 0; i < newPts.length; i++) {
			if (newPts[i] == null) {
				System.out.println("newPts null @ " + i);
			}
		}
		*/
		//if (detected) newPts = shortcutPerimeter (newPts, searchDistance, range + 1);
		
		//return newPts;
	
	
	
	
	
	
	
	
	
	
	
	//Handles self-intersection
	public static Point[] snipPerimeter (Point[] pts) {
		return snipPerimeter(pts, 300);
	}
	
	
	//Snips at self-intersection to eliminate cul-de-sac
	public static Point[] snipPerimeter (Point[] pts, int searchDistance) {
	
		System.out.println("Starting snipping...");
		
		if (pts.length == 0) {
			
			System.out.println("This pointlist has no points");
			return pts; //deals with null arrays 
		}
		
		//empty Pts list: 
		Point[] newPts = new Point[pts.length];
		newPts[0] = pts[0];
		
		int index = 1;
		for (int i = 1; i < pts.length; i++) {
			Point pt1 = pts[i];
			
			for (int j = i - 1; j > 1 && j > i - searchDistance; j--) {
				Point pt2 = pts[j];
				if (pt1.equals(pt2)) {
					int tempIndex = index;
					index = index - (i - j);
					System.out.println("Matched Point at i:" + i + "  j:" + j + "  pre-index:" + tempIndex + "   after-index:" + index);
				}
			}
			if (index < 1) index = 1;
			newPts[index] = pts[i];
			index ++;
					
		}
		
		//trim new array
		newPts = Arrays.copyOf(newPts, index);
		System.out.println("Old Array Length: " + pts.length);
		System.out.println("New Array Length: " + newPts.length);
		
		
		for (int i = 0; i < newPts.length; i++) {
			if (newPts[i] == null) {
				System.out.println("newPts null @ " + i);
			}
		}
		
		
		return newPts;
	}
	
	
	
	
	
	
	//Gets a point-to-point parimeter (no spline)
	public static Point[] straightPerimeter (Point[] pts) {
		
		if (pts.length == 0) return pts; //deals with points already right next to each other. 
		
		ArrayList<Point> newPts = new ArrayList<>();
		//newPts.add(pts[0]); //Adds first point
			
		
		ArrayList<Point> list;
		for (int  j = 1; j < pts.length; j ++) {
			/*
			 * Adds everything from this bresenham algorithm to the end of the list
			 * 
			 * By finding all the points between each point and the next, 
			 * 
			 */
			
			//newPts.addAll(bresenham(pts[j-1].x, pts[j-1].y, pts[j].x, pts[j].y));
			
			//testing
			list = bresenham(pts[j-1].x, pts[j-1].y, pts[j].x, pts[j].y);
			list.remove(0);
			newPts.addAll(list);
		}
		
		//Adds connects last point to first point
		list = bresenham(pts[pts.length-1].x, pts[pts.length-1].y, pts[0].x, pts[0].y);
		list.remove(0);
		newPts.addAll(list);
		
		//converts to Point[]
		Point[] pts2 = new Point[newPts.size()];
		for (int j = 0; j < newPts.size(); j ++) {
			pts2[j] = newPts.get(j);
		}
		
		return pts2;
	}
	
	//Provides pixels along a line
	public static ArrayList<Point> bresenham(int x1, int y1, int x2, int y2) {
		ArrayList<Point> line = new ArrayList<Point>();
		
		//int length = (int) Math.round((Math.sqrt((Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2))))); //attempt to find reasonable array length
		//int[][] line = new int[1000000][2];
		
		//for (int i = 0; i < line.length; i ++) {
		//	line[i][0] = -1;
		//}
		
		
		int w = x2 - x1 ;
	    int h = y2 - y1 ;
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
	    int numerator = longest >> 1 ; //>> divides by 2
	    for (int i=0;i<=longest;i++) {
	        //adds to array
	    	line.add(new Point(x1,y1));
	    	//line[i][0] = x1;
	    	//line[i][1] = y1;
	    
	        numerator += shortest ;
	        if (!(numerator<longest)) {
	            numerator -= longest ;
	            x1 += dx1 ;
	            y1 += dy1 ;
	        } else {
	            x1 += dx2 ;
	            y1 += dy2 ;
	        }
	       
	    }
	return line;
	}	
	
	
	
}
