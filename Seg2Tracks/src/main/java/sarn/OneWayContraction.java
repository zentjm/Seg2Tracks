package sarn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import geometricTools.GeometricCalculations;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.AutoThresholder.Method;
import sarn.Sarn.PointSet;
import ij.process.ImageProcessor;

/**
 * One-Way Contraction: simplest SARN implementation using a cell marker as the inner contour
 * and a circle centered on that marker (with radius = distance to nearest neighbor) as the outer constraint.
 * Searches inward along Bresenham radii to find the transition from dark to bright pixels,
 * yielding the cell perimeter.
 */
public class OneWayContraction extends Sarn {

	/**
	 * Initializes the One-Way Contraction method with default parameters.
	 */
	public OneWayContraction() {
		this.name = "One-Way Contraction";
		this.description = " "; //TODO
	}

	/**
	 * Returns the marker point for the current segment as the sole inner reference point.
	 *
	 * @param currentSegment index of the segment in the current frame
	 * @return array containing only the segment's center point (marker)
	 */
	@Override
	Point[] innerPoints(int currentSegment) {
		Point [] ptsList = new Point[1];
		ptsList[0] = segments.get(currentSegment).getCenterPoint();
		return ptsList;
	}

	/**
	 * Finds the outer boundary constraint for the marker: either the nearest neighboring cell center
	 * or, if no neighbor is closer, the farthest image edge in the dominant axis direction.
	 * This defines the radius of the circle centered on the marker.
	 *
	 * @param centerPoint the marker point (from innerPoints)
	 * @return array containing a single outer constraint point (another cell or image edge)
	 */
	@Override
	Point[] outerPoints(Point [] centerPoint) {

		// Extract marker coordinates
		int x1 = centerPoint[0].x;
		int y1 = centerPoint[0].y;

		// Pre-compute four cardinal edge points from the marker
		Point [] edgeList = {
				new Point(x1,0),
				new Point(x1,processor.getHeight() - 1),
				new Point(0,y1),
				new Point(processor.getWidth()-1,y1)
		};

		// Find the farthest edge point (maximum distance from marker)
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

		// Find the nearest neighboring cell center (closest to this marker)
		int neighbor = -1;
		for (int j = 0; j < segments.size(); j++) {
			double r1 =  Math.pow(segments.get(j).getCenterPoint().x - x1, 2);
			double r2 =  Math.pow(segments.get(j).getCenterPoint().y - y1, 2);
			// Update nearest neighbor if this cell is closer and not the same cell
			if ((r1 + r2) < rad2 && r1 + r2 != 0) {
				rad2 = r1 + r2;
				neighbor = j;
			}
		}

		// Return the nearest neighbor if found, otherwise return farthest edge
		Point [] ptList = new Point[1];
		if (neighbor != -1) ptList[0] = segments.get(neighbor).getCenterPoint();
		else ptList[0] = edgePoint;

		return ptList;
	}


	/**
	 * Returns the inner reference points unchanged (marker is used as-is).
	 *
	 * @param innerPts the initial inner points
	 * @param outerPts the outer constraint points
	 * @return the inner points unchanged
	 */
	@Override
	Point[] innerBounds(Point[] innerPts, Point[] outerPts) {
		return innerPts;
	}


	double radd;
	/**
	 * Generates a circle of points centered on the inner marker with radius from the nearest neighbor.
	 * The circle is computed by iterating four quadrants and using the Pythagorean theorem.
	 *
	 * @param innerBounds the inner reference points
	 * @param outerPts the outer constraint point(s)
	 * @return array of points forming a circle between inner and outer
	 */
	@Override
	Point[] outerBounds(Point[] innerBounds, Point[] outerPts) {

		// Extract coordinates
		int innerX = innerBounds[0].x;
		int innerY = innerBounds[0].y;
		int outerX = outerPts[0].x;
		int outerY = outerPts[0].y;

		// Calculate distance from inner to outer point
		double r1 =  Math.pow(outerX - innerX, 2);
		double r2 =  Math.pow(outerY - innerY, 2);
		double rad = r1 + r2;

		// Parameters for circle generation using Pythagorean formula
		int pivot = (int) Math.round(Math.pow(rad/2, 0.5));
		int radius = Math.round(Math.round(Math.pow(rad, 0.5)));


		// Holds the circle points
		LinkedList <Point> list = new LinkedList<Point>();

		// Iterate over four quadrants to generate circle
		for (int k = 0; k < 4; k++) {

			if (k == 0) {
				// First quadrant: +x, +y
				for (int a= -pivot; a < pivot; a++){
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + a, innerY + b));
				}
			}

			if (k == 1) {
				// Second quadrant: +x, -y
				for (int a = pivot; a > -pivot; a--)  {
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + b, innerY + a));
				}
			}

			if (k == 2) {
				// Third quadrant: -x, -y
				for (int a= pivot; a > -pivot; a--)  {
					int b = (int) Math.round(Math.pow(Math.pow(radius, 2) - Math.pow(a, 2), 0.5));
					list.add(new Point(innerX + a, innerY - b));
				}
			}

			if (k == 3) {
				// Fourth quadrant: -x, +y
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


	/**
	 * Matches each outer boundary point to the single inner marker.
	 * All Bresenham lines radiate from the center outward.
	 *
	 * @param innerBounds the inner reference points
	 * @param outerBounds the outer boundary points
	 * @return array of PointSet pairs, each pairing the center with an outer point
	 */
	@Override
	PointSet[] boundaryMatch(Point[] innerBounds, Point[] outerBounds) {
		PointSet[] pointSetArray = new PointSet[outerBounds.length];
		for (int i = 0; i < outerBounds.length; i++) {
			pointSetArray[i] = new PointSet(innerBounds[0], outerBounds[i]);
		}
		return pointSetArray;
	}



	/**
	 * Finds the threshold point along a Bresenham line by returning the darkest (lowest intensity) pixel.
	 * Scans backward (from outer to inner) to find the boundary transition point.
	 *
	 * @param pts array of points along the Bresenham line
	 * @return the point with the lowest pixel intensity (darkest point on the line)
	 */
	@Override
	Point getThreasholdPoint(Point[] pts) {

		int lowestIntensity = Integer.MAX_VALUE;
		Point darkestPt = pts[0];



		// Finds lowest intensity pixel farthest from the centerpoint (scan backward).
		// get() is used instead of getPixel() — Bresenham points are always within bounds.
		// Pixel value is cached to avoid reading the same pixel twice per iteration.
		for (int i = pts.length-1; i > -1; i --) {
			int px = processor.get(pts[i].x, pts[i].y);
			if (px < lowestIntensity) {
				lowestIntensity = px;
				darkestPt = pts[i];
			}
		}




		return darkestPt;
	}
}
