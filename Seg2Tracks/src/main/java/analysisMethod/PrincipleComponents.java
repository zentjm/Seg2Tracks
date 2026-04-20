package analysisMethod;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import org.apache.poi.ss.usermodel.Workbook;

import calculations.*;
import dataStructure.DataSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.frame.RoiManager;
import geometricTools.GeometricCalculations;
import geometricTools.MatrixFunctions;
import geometricTools.PolarPoint;

/**
 * Analyzes principal component axes of segmented cells using eigenvalue decomposition.
 * Computes major axis length and angle to characterize cell morphology and orientation.
 * Colors cells by major axis angle for visualization of anisotropy patterns.
 */
public class PrincipleComponents extends OperationMethod {

	/**
	 * Constructor initializing principal component analysis settings.
	 */
	public PrincipleComponents() {
		methodName = "Principle Component";
		description = "General and abstracted morphological-intensity data of the internal segmentation";
	}

	/**
	 * Returns channel names for the input dataset.
	 *
	 * @return array with single element "DataSet:"
	 */
	@Override
	public String[] getChannels() {
		return new String[] { "DataSet:"};
	}

	/**
	 * Extract major axis principal component vector as an overlay ROI.
	 * Computes cell orientation via eigenvector decomposition and colors segment by angle.
	 * //TODO: how to determine the scale
	 *
	 * @param segment the Segment to extract principal component ROI for
	 * @return Roi representing the major axis as a straight line
	 */
	@Override
	Roi getOverlayParameter(Segment segment) {


		int scale = 100;
		//TODO: how to determine the scale


		//System.out.println("Beginning matrix exploration");

		//double[] vector1 = MatrixFunctions.getMajorAxis(segment.getInternalPerimeter());
		//Point[] pc1 = getVector(vector1, scale, segment);
		MatrixFunctions functions = new MatrixFunctions();

		//System.out.println("Created matrix function");

		// Get major axis endpoints from center point through the cell
		Point[] pc1 =  functions.getMajorAxis(segment.getCenterPoint(),segment.getInternalPerimeter());
		// Get the angle (theta) of the major axis
		double theta = functions.getTheta();
		if (theta < 0) theta += (2* Math.PI);
		// Map angle to RGB color hue for visualization
		int col = (int)(255 * (theta/(2 * Math.PI)));
		Color color = new Color(col,255 - col, 128);
		segment.setColor(color);



		/*
		Make color related to vector angle //TODO make static method
		double angle = Math.atan2(vector[0], vector[1]);
		if (angle < 0) angle += (2* Math.PI);
		int col = (int)(255 * (angle/(2 * Math.PI)));
		//System.out.println("Color is: " + col);
		//System.out.println("Angle is: " + angle);
		Color color = new Color(255-col,col,128); //TODO: Round smooth color scheme
		*/


		return getPolygonRoi(pc1); //is a straight line
	}


	/* depreciated
	Point[]	getVector(double[] vector, int scale, Segment segment) {
		double xLow = (-scale * vector[0]) + segment.getCenterPoint().x;
		double yLow = (-scale * vector[1]) + segment.getCenterPoint().y;
		double xHigh = (scale * vector[0]) + segment.getCenterPoint().x;
		double yHigh = (scale * vector[1]) + segment.getCenterPoint().y;
		Point lowPt = new Point((int)xLow, (int) yLow);
		Point highPt = new Point((int)xHigh, (int) yHigh);
		ArrayList<Point> pcList = GeometricCalculations.bresenham((int)xLow, (int)yLow, (int)xHigh, (int)yHigh);
		Point[] pc = new Point[pcList.size()];
		for (int i = 0; i < pcList.size(); i++) {
			pc[i] = pcList.get(i);
		}
		return pc;
	}
	*/



	/**
	 * Perform any necessary transformations on input datasets.
	 * Currently returns input unchanged.
	 *
	 * @param inputSets array of input DataSets
	 * @return same input DataSet array
	 */
	@Override
	DataSet[] dataOperation(DataSet[] inputSets) {
		return inputSets;
	}

	/**
	 * Returns segment-level calculations for principal component analysis.
	 * Computes major axis length and orientation angle.
	 *
	 * @return array of major axis calculations
	 */
	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
				new MajorAxisLength(),
				new MajorAxisAngle()
		};
	}

	/**
	 * Returns LinkSet-level calculations for principal component analysis.
	 * Currently empty.
	 *
	 * @return empty array
	 */
	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
		};
	}

	/**
	 * Returns LinkSet-level statistics for principal component analysis.
	 * Currently empty.
	 *
	 * @return empty array
	 */
	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return new LinkSetStatistic[] {
		};
	}

	/**
	 * Returns FrameSet-level calculations for principal component analysis.
	 * Currently empty.
	 *
	 * @return empty array
	 */
	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return new FrameSetCalculation[] {
		};
	}

	/**
	 * Returns FrameSet-level statistics for principal component analysis.
	 * Currently empty.
	 *
	 * @return empty array
	 */
	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
		};
	}
}
