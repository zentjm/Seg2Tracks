package sarn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import sarn.Sarn.PointSet;
import ij.process.ImageProcessor;

/**
 * One-Way Contraction with Exclusion SARN method implementation.
 * <p>
 * Shares the marker/outer-constraint/circle/threshold mechanics of {@link OneWayContractionBase};
 * differs by bridging gaps between widely spaced threshold points with a minimal-intensity path
 * ({@link #minimalPath}) so the contracted boundary stays continuous even where straight radial
 * sampling would jump across a neighbouring cell.
 */
public class OneWayContraction_Exclusion extends OneWayContractionBase {

	/**
	 * Initializes the One-Way Contraction with Exclusion method with default parameters.
	 */
	public OneWayContraction_Exclusion() {
		this.name = "One-Way Contraction_Exclusion";
		this.description = "Contracts a circular boundary like One-Way Contraction, bridging gaps "
				+ "between widely spaced threshold points with a minimal-intensity path to keep the "
				+ "boundary continuous in dense fields where envelopes would otherwise overlap.";
	}

	double radd;
	//Calculates the pointlist for the boundary
	/**
	 * Generates the boundary circle via the shared generator, recording the quadrant pivot in
	 * {@link #radd}.
	 *
	 * @param innerBounds the inner reference points
	 * @param outerPts the outer constraint points
	 * @return array of points forming the outer boundary
	 */
	@Override
	Point[] outerBounds(Point[] innerBounds, Point[] outerPts) {
		radd = computePivot(innerBounds[0], outerPts[0]);
		//TODO: Consider using Bresenham to fill in gaps?
		return generateCirclePoints(innerBounds[0], outerPts[0]);
	}

	/**
	 * Finds the threshold point by returning the darkest pixel closest to the center (scans forward).
	 * Alternative to the inherited getThreasholdPoint which scans backward from the outer edge.
	 *
	 * @param pts array of points along the Bresenham line
	 * @return the darkest point closest to the line's starting point (center)
	 */
	Point getNearestThreasholdPoint(Point[] pts) {
		int lowestIntensity = Integer.MAX_VALUE;
		Point darkestPt = pts[0];
		// Finds lowest intensity pixel nearest to the centerpoint (forward scan).
		// get() skips bounds checking — Bresenham points are always within bounds.
		for (int i = 0; i < pts.length; i ++) {
			int px = processor.get(pts[i].x, pts[i].y);
			if (px < lowestIntensity) {
				lowestIntensity = px;
				darkestPt = pts[i];
			}
		}
		return darkestPt;
	}

	Point startPoint;
	// TODO: skipAllowance should be proportional to the Gaussian blur sigma for adaptive thresholding
	int skipAllowance = 20;

	// NOTE: Implementation method #4 ("Cold") appears to work well enough for current use cases.

	/**
	 * Applies restriction point detection with minimal path interpolation when gaps occur between sampled points.
	 * If the distance between consecutive threshold points exceeds skipAllowance, computes a minimal
	 * path through the gap to ensure boundary continuity.
	 *
	 * @param matchedPoints array of inner-outer point pairs to process
	 * @return array of restriction points with interpolated path coverage
	 */
	@Override
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

	/**
	 * Computes a minimal intensity path bridging a gap in the perimeter between two threshold points.
	 * When consecutive sampled points are too far apart (skipAllowance exceeded), this method iterates
	 * through adjacent Bresenham lines, finding the darkest pixels and combining them into a continuous path.
	 *
	 * @param matchedPoints array of all matched point pairs (context for neighbors)
	 * @param index current index in the matched points array
	 * @param startPoint the starting threshold point (from previous Bresenham line)
	 * @param endPoint the target threshold point (from current Bresenham line)
	 * @return array of points forming the minimal intensity path, or empty array if path is worse than direct line
	 */
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
				int newSmallestIndex = smallestIndex + stepDiff;
				smallestIndex = newSmallestIndex;
				if (smallestIndex < 1) smallestIndex = 1;
			}

			int maxInt = -1;
			int maxIndex = -1;

			//from the smallest index, iterate outward, finding brightest point in that range
			for (int i = smallestIndex; i < line.length; i ++) {
				int px = processor.get(line[i].x, line[i].y);
				if (px > maxInt) {
					maxInt = px;
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

			prevLine = line;
			//test if close
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

			//FOR CHECKING IF LOOP IS BAD IS BAD -- method 1
			Point[] brokenLine = bresenham (startPoint, endPoint);
			double avgIntBroken = 0;
			for (int i = 0; i < brokenLine.length; i ++) {
				avgIntBroken += processor.get(brokenLine[i].x, brokenLine[i].y);
			}
			avgIntBroken = avgIntBroken / brokenLine.length;

			double avgIntPtsList = 0;
			for (int i = 0; i < ptsList.length; i ++) {
				avgIntPtsList += processor.get(ptsList[i].x, ptsList[i].y);
			}
			avgIntPtsList = avgIntPtsList/ ptsList.length;

			if (avgIntPtsList < avgIntBroken) {
				return ptsList;
			}
		}

		return new Point[] {};
	}

	/**
	 * Finds the index position of a specific point within a line array.
	 * Used to determine where along a Bresenham path a particular threshold point was found.
	 *
	 * @param line array of points on the Bresenham line
	 * @param pt the point to locate
	 * @return the index of the point in the array, or 0 if not found
	 */
	protected int thresholdIndex(Point[] line, Point pt) {
		for (int i = 0; i < line.length; i ++) {
			if (pt.x == line[i].x && pt.y == line[i].y) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Calculates the Euclidean distance between two points.
	 *
	 * @param pt1 the first point
	 * @param pt2 the second point
	 * @return the straight-line distance between the two points
	 */
	private static double pyth(Point pt1, Point pt2) {
		return Math.sqrt(
				Math.pow(pt1.x - pt2.x, 2) +
				Math.pow(pt1.y - pt2.y, 2)
			);
	}
}
