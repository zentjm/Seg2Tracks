package sarn;

import java.awt.Point;
import java.util.LinkedList;

import geometricTools.GeometricCalculations;
import sarn.Sarn.PointSet;

/**
 * One-Way Contraction with Gradient Descent SARN method implementation.
 * <p>
 * Shares the marker/outer-constraint/circle/threshold mechanics of {@link OneWayContractionBase};
 * differs by tracing each radial path via greedy gradient ascent ({@link #surfaceTrace}) toward
 * high-intensity pixels rather than following a straight Bresenham line.
 */
public class OneWayContraction_GradientDecent extends OneWayContractionBase {

	/**
	 * Initializes the One-Way Contraction with Gradient Descent method with default parameters.
	 */
	public OneWayContraction_GradientDecent() {
		this.name = "One-Way Contraction_Gradient Descent";
		this.description = "Contracts a circular boundary like One-Way Contraction, but traces each "
				+ "radial path via greedy gradient ascent toward high-intensity pixels instead of a "
				+ "straight line, refining boundaries on cells with soft edges.";
	}

	double radd;
	//Calculates the pointlist for the boundary
	/**
	 * Generates the boundary circle (via the shared generator) and straightens it, while recording
	 * the true inner-to-outer radial distance in {@link #radd} for {@link #surfaceTrace} to use as a
	 * stopping cutoff.
	 *
	 * @param innerBounds the inner reference points
	 * @param outerPts the outer constraint points
	 * @return array of points forming the outer boundary
	 */
	@Override
	Point[] outerBounds(Point[] innerBounds, Point[] outerPts) {

		//TEMP: for surfaceTrace
		radd = pyth(innerBounds[0], outerPts[0]);

		Point[] ptList = generateCirclePoints(innerBounds[0], outerPts[0]);
		ptList = GeometricCalculations.straightPerimeter(ptList);

		//TODO: Consider using Bresenham to fill in gaps?
		return ptList;
	}

	/**
	 * Applies restriction point detection using gradient descent surface tracing.
	 * For each matched pair, traces along the surface toward the outer point,
	 * selecting pixels with high intensity that move toward the goal while not exceeding radial distance.
	 *
	 * @param matchedPoints array of inner-outer point pairs to process
	 * @return array of restriction points (one per matched pair)
	 */
	@Override
	protected Point[] contractor (PointSet[] matchedPoints) {

		LinkedList<Point> ptsLinkedList = new LinkedList<>();
		for (int n = 0; n < matchedPoints.length; n++) {
			Point[] line = surfaceTrace(matchedPoints[n].innerPoint, matchedPoints[n].outerPoint);
			ptsLinkedList.add(getThreasholdPoint(line));
		}

		Point[] ptsList = new Point[ptsLinkedList.size()];
		for (int i = 0; i < ptsLinkedList.size(); i++) {
			ptsList[i] = ptsLinkedList.get(i);
		}

		return ptsList;
	}

	Point startPoint;

	/**
	 * Traces a path from start to end point by iteratively moving to the neighboring pixel
	 * that has the highest intensity while moving closer to the goal. Implements a greedy
	 * gradient ascent algorithm for finding high-intensity pixels along the cell boundary.
	 *
	 * @param startPoint the beginning point (cell center)
	 * @param endPoint the goal point (outer constraint)
	 * @return array of points forming the traced path
	 */
	protected Point[] surfaceTrace(Point startPoint, Point endPoint) {

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
	    }
	    return linePts;
	}

	boolean prevDir = false;
	// UNCLEAR: prevDir is declared but never used in this method - may be dead code

	/**
	 * Finds the next neighboring pixel that maximizes intensity while moving closer to the goal.
	 * Considers 8-connected neighbors and selects based on highest intensity, with tie-breaking
	 * by preferring pixels farther from the start point (to avoid backtracking).
	 *
	 * @param currentPoint the current position on the surface
	 * @param endPoint the target/goal point to move toward
	 * @return the next pixel to move to
	 */
	private Point search(Point currentPoint, Point endPoint) {

		double maxIntensity = -1;
		Point nextPoint = new Point();
		double distanceStartCurr = pyth(currentPoint, startPoint);

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
				int intensity = processor.get(pt.x, pt.y);
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

	/**
	 * Checks if a point is within the valid image bounds.
	 *
	 * @param pt the point to check
	 * @return true if the point is within image dimensions, false otherwise
	 */
	private boolean inBorder (Point pt) {
		if (pt.x > processor.getWidth() - 1) return false;
		if (pt.x < 0) return false;
		if (pt.y > processor.getHeight() - 1) return false;
		if (pt.y < 0) return false;
		return true;
	}

	/**
	 * Returns a neighboring pixel in one of eight directions (8-connected neighborhood).
	 * Direction wraps around after 8 to support circular iteration.
	 *
	 * @param pt the center point
	 * @param direction 0-7 specifying the compass direction (0=down, 1=up, 2=down-right, etc.)
	 * @return the neighbor point in the specified direction
	 */
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
