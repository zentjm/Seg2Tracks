package geometricTools;

import java.awt.Point;
import java.io.Serializable;

/**
 * PolarPoint represents a 2D point in polar coordinates (radius, angle).
 * Used for circular/radial contour analysis and shape decomposition.
 */
public class PolarPoint implements Serializable {


	private static final long serialVersionUID = 3298889743435684196L;

	public double r; // Radial distance
	public double theta; // Angle (radians, 0-2π)
	public Point point; // Reference to original Cartesian point (optional)

	/**
	 * Default constructor.
	 */
	public PolarPoint() {
	}

	/**
	 * Constructs a polar point from radius and angle.
	 * @param r radial distance from origin
	 * @param theta angle in radians (0-2π)
	 */
	public PolarPoint (double r, double theta) {
		this.r = r;
		this.theta = theta;
	}

	/**
	 * Converts a Cartesian point to polar with origin at (0,0).
	 * @param point Cartesian coordinates
	 */
	public PolarPoint (Point point) {
		this.r = Math.sqrt((point.x * point.x) + (point.y * point.y));
		this.theta = Math.atan2(point.y, point.x); // XXX: Need conversion for signs?
		// Normalize theta to 0-2π range
		if (theta < 0) this.theta = (2* Math.PI) + theta;
		this.point = point;
	}

	/**
	 * Converts a Cartesian point to polar with custom origin.
	 * @param point Cartesian coordinates
	 * @param origin custom origin for polar coordinates
	 */
	public PolarPoint (Point point, Point origin) {
		double x = point.getX() - origin.getX(); // Translate to origin
		double y = point.getY() - origin.getY();
		this.r = Math.sqrt((x*x) + (y*y));
		this.theta = Math.atan2(y, x);
		// Normalize theta to 0-2π range
		if (theta < 0) this.theta = (2* Math.PI) + theta;
		this.point = point;
	}

	/**
	 * Gets the radial distance.
	 * @return r value
	 */
	public double getR() {
		return r;
	}

	/**
	 * Gets the angle.
	 * @return theta in radians
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * Converts back to Cartesian coordinates with origin at (0,0).
	 * @return Cartesian point
	 */
	public Point getCartesian() {
		if (point != null) return point;
		return (new Point ((int) (r * Math.cos(theta)), (int)(r * Math.sin(theta))));
	}

	/**
	 * Converts back to Cartesian with respect to a custom center point.
	 * TODO: better explanation needed
	 * @param pt center point for coordinate system
	 * @return Cartesian point relative to center
	 */
	public Point getCartesian(Point pt) {
		if (point != null) return point;
		return (new Point ((int) (r * Math.cos(theta)) + pt.x, (int)(r * Math.sin(theta)) + pt.y));
	}


	/**
	 * Tests equality with another PolarPoint.
	 * @param obj object to compare
	 * @return true if r and theta are equal
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof PolarPoint)) return false;
		PolarPoint p = (PolarPoint) obj;
		return (r == p.getR() && theta == p.getTheta());
	}

}
