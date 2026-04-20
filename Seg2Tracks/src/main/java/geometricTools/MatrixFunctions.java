package geometricTools;

import java.awt.Point;
import java.util.ArrayList;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixFormat;
import org.apache.commons.math3.stat.correlation.Covariance;

import dataStructure.Segment;
import ij.process.ImageProcessor;

/**
 * MatrixFunctions provides eigenvalue/eigenvector analysis for morphological measurements.
 * Computes principal axes of cell shapes using PCA (Principal Component Analysis).
 * Used to determine cell orientation and aspect ratio.
 */
public class MatrixFunctions {


	/**
	 * Eigenvalue decomposition provides:
	 * - Directional vectors (principal axes)
	 * - Covariance and variance matrix
	 * - Rotation angles for shape alignment
	 */

	double covariance; // UNUSED: covariance value
	double[] majorVector; // Primary principal component direction
	double[] minorVector; // Secondary principal component direction
	double theta; // Rotation angle (radians)



	/**
	 * Computes the major (longest) axis of a point cloud.
	 * @param centerPoint center of the shape
	 * @param pointList boundary or interior points
	 * @return array of points representing the major axis line
	 */
	public Point[] getMajorAxis (Point centerPoint, Point[] pointList) {
		//System.out.println("Running ... getMajorAxis");
		return getScaleByMorphology(centerPoint,pointList, getEigenVectors (pointList, 0));
	}

	/**
	 * Computes the minor (shortest) axis of a point cloud.
	 * @param centerPoint center of the shape
	 * @param pointList boundary or interior points
	 * @return array of points representing the minor axis line
	 */
	public Point[] getMinorAxis (Point centerPoint, Point[] pointList) {
		//System.out.println("Running ... getMajorAxis");
		return getScaleByMorphology(centerPoint, pointList, getEigenVectors (pointList, 1));
	}


	/**
	 * Computes eigenvectors of the covariance matrix for a point cloud.
	 * Performs PCA to find principal directions of variation.
	 * @param pointList input points
	 * @param eigenvector which eigenvector to return (0=primary, 1=secondary)
	 * @return eigenvector as [x, y] direction components
	 */
	public double[] getEigenVectors (Point[] pointList, int eigenvector) {

		// Convert points to 2D coordinate matrix
		double[][] coords = new double[pointList.length][2];
		for (int i = 0; i < pointList.length ; i++) {
			coords[i][0] = (double) pointList[i].x;
			coords[i][1] = (double) pointList[i].y;
		}

		RealMatrixFormat matrixFormat = new RealMatrixFormat("", "", "", "\n", "", ", ");

		// Matrix operations
		RealMatrix matrix = MatrixUtils.createRealMatrix(coords);
		//System.out.println("Matrix\n" + matrixFormat.format(matrix));

		// Covariance matrix // TODO: how degrees of freedom are calculated?
		RealMatrix covar = (new Covariance(matrix)).getCovarianceMatrix();
		//System.out.println("Covariance Matrix\n" + matrixFormat.format(covar)); // TODO:

		// Eigenvalues & vectors
		EigenDecomposition decomp = new EigenDecomposition(covar);
		double value = decomp.getRealEigenvalue(0);
		double[] vector = decomp.getEigenvector(eigenvector).toArray();

		// Convert back to points
		//System.out.println("Vector:");
		for (int i = 0; i < vector.length; i ++) {
			//System.out.println(vector[i]);
		}

		return vector;
	}


	/**
	 * Generates axis line using eigenvector direction and morphological extent.
	 * Rotates and scales based on principal component analysis.
	 * @param centerPoint shape centroid
	 * @param pointList shape boundary points
	 * @param eigenVector direction vector from PCA
	 * @return axis as Bresenham-interpolated point array
	 */
	public Point[] getScaleByMorphology (Point centerPoint, Point[] pointList, double[] eigenVector) {
	//System.out.println("Running ... getMajorAxis");

		// Transform centerPoint into matrix
		RealMatrix centerPointMatrix = MatrixUtils.createRealMatrix(new double [][] {
						{(double) centerPoint.x, (double) centerPoint.y }
					});

		// Transform pointList into matrix
		double[][] coords = new double[pointList.length][2];
		for (int i = 0; i < pointList.length ; i++) {
			coords[i][0] = (double) pointList[i].x;
			coords[i][1] = (double) pointList[i].y;
		}
		RealMatrix matrix = MatrixUtils.createRealMatrix(coords);

		// Normalize eigenvector to unit length
		// XXX: needed?
		double magnitude = Math.sqrt(Math.pow(eigenVector[0], 2) + Math.pow(eigenVector[1], 2));
		double unitX = eigenVector[0] / magnitude;
		double unitY = eigenVector[0] / magnitude;
		double[] xT = new double[pointList.length];
		double[] yT = new double[pointList.length];

		// Find theta (rotation angle) of the eigenvector
		double theta = Math.atan2(eigenVector[1],eigenVector[0]);
		this.theta = theta;

		// Create rotation transformation matrix
		double[][] transform = new double[][] {
			{Math.cos(theta), -Math.sin(theta)},
			{Math.sin(theta),  Math.cos(theta)}
		};
		RealMatrix transformMatrix = MatrixUtils.createRealMatrix(transform);

		// Apply transformation to centerPoint
		RealMatrix adjustedCenterPointMatrix = centerPointMatrix.multiply(transformMatrix);
		double[][] adjustedCenterPoint = adjustedCenterPointMatrix.getData();

		// Apply transformation to pointList
		RealMatrix productMatrix = matrix.multiply(transformMatrix);
		double[][] product = productMatrix.getData();

		// Convert adjusted center to point
		Point shiftedCenterPoint = new Point (
				Math.round(Math.round(adjustedCenterPoint[0][0])),
				Math.round(Math.round(adjustedCenterPoint[0][1]))
			);

		// Translate back with respect to centerpoint
		int deltaX = shiftedCenterPoint.x - centerPoint.x;
		int deltaY = shiftedCenterPoint.y - centerPoint.y;

		// Convert pointList Matrix back to points
		Point[] shiftedPointList = new Point[pointList.length];
		for (int i = 0; i < pointList.length; i ++) {
			shiftedPointList[i] = new Point(
				Math.round(Math.round(product[i][0])) - deltaX,
				Math.round(Math.round(product[i][1])) - deltaY
			);
		}


		// Get index of most distant point (should have shifted along X-axis)
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		int maxIndex = -1;
		int minIndex = -1;
		for (int i = 0; i < shiftedPointList.length; i++) {
			Point pt = shiftedPointList[i];
			if (pt.x > max) {
				max = pt.x;
				maxIndex = i;
			}
			if (pt.x < min) {
				min = pt.x;
				minIndex = i;
			}
			// System.out.println("Pt at x is: " + pt.x + "   Max is: " +  max + "    Min is: " + min);
			// System.out.println("Max Index is: " + maxIndex + "   Min Index is:" + minIndex);
		}

		// Calculate scale size for the vector (distance between extremes)
		int scale = Math.round(Math.round(Math.sqrt(
				Math.pow(pointList[maxIndex].x - pointList[minIndex].x, 2) +
				Math.pow(pointList[maxIndex].y - pointList[minIndex].y, 2)
			)));

		// Generate axis using scale size through the centerpoint
		double xLow = (-scale/2 * eigenVector[0]) + centerPoint.x;
		double yLow = (-scale/2 * eigenVector[1]) + centerPoint.y;
		double xHigh = (scale/2 * eigenVector[0]) + centerPoint.x;
		double yHigh = (scale/2 * eigenVector[1]) + centerPoint.y;
		Point lowPt = new Point((int)xLow, (int) yLow);
		Point highPt = new Point((int)xHigh, (int) yHigh);
		ArrayList<Point> pcList = GeometricCalculations.bresenham((int)xLow, (int)yLow, (int)xHigh, (int)yHigh);
		Point[] pc = new Point[pcList.size()];
		for (int i = 0; i < pcList.size(); i++) {
			pc[i] = pcList.get(i);
		}

		// Return axis line
		return pc;


		// return shiftedPointList; // XXX: for testing the rotation

	}

	/**
	 * Gets the computed rotation angle.
	 * @return theta in radians
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * Computes intensity-based morphological scale.
	 * TODO: Not yet implemented
	 * @param pointList shape boundary or interior
	 * @param segment segment to analyze
	 * @return intensity scale (currently returns null)
	 */
	// STUB: method not yet implemented — returns null
	public static double[] getScaleByIntensity (Point[] pointList, Segment segment) {

		double[][] coords = new double[pointList.length][2];
		for (int i = 0; i < pointList.length ; i++) {
			coords[i][0] = (double) pointList[i].x;
			coords[i][1] = (double) pointList[i].y;
		}

		return null;

	}




}
