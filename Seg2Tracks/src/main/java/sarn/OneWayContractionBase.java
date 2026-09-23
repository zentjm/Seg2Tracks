package sarn;

import java.awt.Point;
import java.util.LinkedList;

/**
 * Shared base for the One-Way Contraction (OWC) family of SARN methods.
 * <p>
 * OWC methods treat the cell marker as the inner contour and a circle centred on that marker
 * (radius = distance to the nearest neighbouring cell, or the farthest image edge when the cell
 * has no neighbours) as the outer constraint, then contract inward along radial lines to the
 * dark-to-bright intensity transition.
 * <p>
 * This class hosts the pieces that are identical across {@link OneWayContraction},
 * {@link OneWayContraction_GradientDecent}, {@link OneWayContraction_Exclusion}, and
 * {@link MultiWayReductionContraction}: the marker inner point, the nearest-neighbour /
 * farthest-edge outer constraint, the pass-through inner boundary, the circle-point generation,
 * the centre-to-outer boundary matching, and the darkest-pixel threshold scan. Subclasses that
 * need a variation (e.g. a {@code straightPerimeter} pass or a stored radial-distance field in
 * {@code outerBounds}) override just that method and reuse {@link #generateCirclePoints} /
 * {@link #computePivot}. {@code MinimalBoundary} is not OWC-based and extends {@link Sarn} directly.
 */
public abstract class OneWayContractionBase extends Sarn {

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
			if (segments.get(j).getCenterPoint() == null) continue;
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

	/**
	 * Generates a circle of points centered on the inner marker with radius from the outer constraint.
	 * The circle is computed by iterating four quadrants and using the Pythagorean theorem. This is the
	 * default {@code outerBounds} behaviour; subclasses needing an additional side effect (a stored
	 * radial distance) or a {@code straightPerimeter} pass override {@code outerBounds} and reuse
	 * {@link #generateCirclePoints}.
	 *
	 * @param innerBounds the inner reference points
	 * @param outerPts the outer constraint point(s)
	 * @return array of points forming a circle between inner and outer
	 */
	@Override
	Point[] outerBounds(Point[] innerBounds, Point[] outerPts) {
		return generateCirclePoints(innerBounds[0], outerPts[0]);
	}

	/**
	 * Computes the quadrant pivot for the circle generation: {@code round(sqrt(rad/2))} where
	 * {@code rad} is the squared inner-to-outer distance. Extracted so subclasses that store the
	 * pivot as their radial-distance field can reuse the exact arithmetic.
	 *
	 * @param innerPt the circle center (marker)
	 * @param outerPt the outer constraint point
	 * @return the quadrant pivot value
	 */
	protected final int computePivot(Point innerPt, Point outerPt) {
		double r1 = Math.pow(outerPt.x - innerPt.x, 2);
		double r2 = Math.pow(outerPt.y - innerPt.y, 2);
		double rad = r1 + r2;
		return (int) Math.round(Math.pow(rad / 2, 0.5));
	}

	/**
	 * Generates the raw circle points centered on {@code innerPt} with radius equal to the
	 * inner-to-outer distance, walking four quadrants via the Pythagorean theorem. This is the
	 * shared core of every OWC variant's {@code outerBounds}.
	 *
	 * @param innerPt the circle center (marker)
	 * @param outerPt the outer constraint point (defines the radius)
	 * @return array of points forming the circle
	 */
	protected final Point[] generateCirclePoints(Point innerPt, Point outerPt) {

		// Extract coordinates
		int innerX = innerPt.x;
		int innerY = innerPt.y;
		int outerX = outerPt.x;
		int outerY = outerPt.y;

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
