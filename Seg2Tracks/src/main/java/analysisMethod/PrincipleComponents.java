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

//Gives overlay and data on the internal perimeter of a single dataSet
public class PrincipleComponents extends OperationMethod {

	public PrincipleComponents() {
		methodName = "Principle Component";
		description = "General and abstracted morphological-intensity data of the internal segmentation";
	}
	
	@Override
	public String[] getChannels() {
		return new String[] { "DataSet:"};
	}

	@Override
	Roi getOverlayParameter(Segment segment) {
		
		
		int scale = 100;
		//TODO: how to determine the scale
		
		//double[] vector1 = MatrixFunctions.getMajorAxis(segment.getInternalPerimeter()); 
		//Point[] pc1 = getVector(vector1, scale, segment);
		MatrixFunctions functions = new MatrixFunctions();
		Point[] pc1 =  functions.getMajorAxis(segment.getCenterPoint(),segment.getInternalPerimeter());
		double theta = functions.getTheta();
		if (theta < 0) theta += (2* Math.PI);
		int col = (int)(255 * (theta/(2 * Math.PI)));
		Color color = new Color(col,255 - col, 128);
		segment.setColor(color);
		
		
		
		/*
		Make color related to vector angle //TODO make static method
		double angle = Math.atan2(vector[0], vector[1]);
		if (angle < 0) angle += (2* Math.PI);
		int col = (int)(255 * (angle/(2 * Math.PI)));
		System.out.println("Color is: " + col);
		System.out.println("Angle is: " + angle);
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
	
	
	
	@Override
	DataSet[] dataOperation(DataSet[] inputSets) {
		return inputSets;
	}
	
	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
				new MajorAxisLength(),
				new MajorAxisAngle()
		};
	}

	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
		};
	}

	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return new LinkSetStatistic[] {
		};	
	}

	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return new FrameSetCalculation[] {
		};
	}

	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
		};
	}
}