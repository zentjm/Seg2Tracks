package geometricTools;

import java.awt.Point;
import java.io.Serializable;

public class PolarPoint implements Serializable {


	private static final long serialVersionUID = 3298889743435684196L;

	public double r;
	public double theta;
	public Point point;
	
	public PolarPoint() {
	}
	
	public PolarPoint (double r, double theta) {
		this.r = r;
		this.theta = theta;
	}
	
	//convert Cartesian to polar with respect to origin at zero-zero
	public PolarPoint (Point point) {
		this.r = Math.sqrt((point.x * point.x) + (point.y * point.y));
		this.theta = Math.atan2(point.y, point.x); //XXX: Need conversion for signs?
		if (theta < 0) this.theta = (2* Math.PI) + theta;
		this.point = point;
	}
			  
	//Convert Cartesian to polar with respect to a new origin point
	public PolarPoint (Point point, Point origin) {
		double x = point.getX() - origin.getX();
		double y = point.getY() - origin.getY();
		this.r = Math.sqrt((x*x) + (y*y));
		this.theta = Math.atan2(y, x);
		if (theta < 0) this.theta = (2* Math.PI) + theta;
		this.point = point;
	}
	
	public double getR() {
		return r;
	}
	
	public double getTheta() {
		return theta;
	}
	
	public Point getCartesian() {
		if (point != null) return point;
		return (new Point ((int) (r * Math.cos(theta)), (int)(r * Math.sin(theta))));
	}
	
	//get cartesian with respect to new centerpoint //TODO: better explanation needed
	public Point getCartesian(Point pt) {
		if (point != null) return point;
		return (new Point ((int) (r * Math.cos(theta)) + pt.x, (int)(r * Math.sin(theta)) + pt.y));
	}
	
	
	public boolean equals(Object obj) {
		if (!(obj instanceof PolarPoint)) return false;
		PolarPoint p = (PolarPoint) obj;
		return (r == p.getR() && theta == p.getTheta());
	}
	

		  
	
	
	
	
	
}
