package internalSegmentation;

import java.awt.Point;
import java.awt.Polygon;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import dataStructure.FrameSet;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.ImageProcessor;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedAutoThresholder.Method;

public class TriangleMethod extends InternalSegmentation{

	public TriangleMethod() {
		this.name = "Triangle Method";
		this.description = " "; //TODO
		this.externalDependence = false;
	}
	
	public boolean isExternallyDependent() {
		return false;
	}
	
	Method getMethod() {
		return Method.Triangle;
	}
	
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

}
