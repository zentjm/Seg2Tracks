package gui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import geometricTools.PolarPoint;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.Histogram;
import ij.process.ImageProcessor;
import ij.util.Tools;

public class GuidedCalibration {

	OperationController controller;
	ImageStack inputStack;
	DataSet dataSet;
	int sigma;
	
	
	//TODO: relationship between the Guassian and the required blur amount?
	
	
	//return a sigma and noise based on user outline
	public GuidedCalibration(OperationController controller) {
		this.controller = controller; 
		this.inputStack = IJ.openVirtual(controller.getInputFilePath()).getImageStack();
		this.dataSet = controller.getDataSet();
	}
	
	//Run the program
	public void run() {
		
		/*
		//TEST HISTOGRAM
		
		int[] histogram = new int[100];
		for (int i = 0; i < histogram.length; i++) {
			histogram[i] = i+10;
		}
		
		displayHistogram(histogram);
		//END TEST
		*/
		
		
		
		int sigma = 0; 
		for (FrameSet frameSet : dataSet.getFrameSetList()) {
			sigma += calculateSigma (frameSet, inputStack.getProcessor(frameSet.getFrame()+1));
		}
		this.sigma = sigma;
		
	}
	
	
	
	
	public double calculateSigma(FrameSet frameSet, ImageProcessor tempProcessor) {
		
		//variables
		int samplingNumber = 45; //number of samples per image taken.
		double sigma = 0;
		
		
		//1. Get guassian curve around every object
		for (Segment segment : frameSet) {
			//Find farthest radius
			double radius = Double.MIN_VALUE;
			for (Point pt: segment.getExternalPerimeter()) {
				double distance = Math.sqrt(
					Math.pow(pt.x - segment.getCenterPoint().x, 2) + 
					Math.pow(pt.y - segment.getCenterPoint().y, 2)
				);
				if (distance > radius) radius = distance;
			}
			
			//Get points for gaussian function
			
			// int[line][intensityAlongLine]
			int[][] boundary = new int[samplingNumber][];
			
			//calculate angle step
			double step = Math.toRadians(180 / samplingNumber);
			double theta = 0;
			
			//Get all guassian curves for object
			for (int i = 0; i < boundary.length ; i ++) {
				PolarPoint pA = new PolarPoint (radius,  theta);
				PolarPoint pB = new PolarPoint (-radius, theta);
				theta += step;
				
				//System.out.println (" Theta:" + theta + "     Step:" + step);
				
				Point cA = pA.getCartesian(segment.getCenterPoint());
				Point cB = pB.getCartesian(segment.getCenterPoint());
			
				
				//Adjust for edges //TODO: simplify.
				if (cA.x > tempProcessor.getWidth() -1) cA.x = tempProcessor.getWidth() - 1;
				if (cA.x < 0) cA.x = 0;
	
				if (cA.y > tempProcessor.getHeight() -1) cA.y = tempProcessor.getHeight() - 1;
				if (cA.y < 0) cA.y = 0;
				
				if (cB.x > tempProcessor.getWidth() -1) cB.x = tempProcessor.getWidth() - 1;
				if (cB.x < 0) cB.x = 0;
	
				if (cB.y > tempProcessor.getHeight() -1) cB.y = tempProcessor.getHeight() - 1;
				if (cB.y < 0) cB.y = 0;
				
				
				
				//get list  
				ArrayList<Point> line =  GeometricCalculations.bresenham(cA.x, cA.y, cB.x, cB.y);
				
				/*
				//FOR TESTING
				for (Point pt: line) {
					tempProcessor.set(pt.x, pt.y, 255);
				}
				
				if (i == boundary.length -1) {
					System.out.println("cAx: " + cA.x + "cAy: " + cA.y + "cBx: " + cA.x + "cBy: " + cA.y );
					Point[] tempPt = new Point[line.size()];
					for (int j = 0; j < tempPt.length; j ++) {
						tempPt[j] = line.get(j);
					}
					drawLine(tempPt, tempProcessor);
					break;
				}
				//END TESTING
				*/
				
				//copy intensities to boundary 
				int[] intensities = new int[line.size()];
				for (int j = 0 ; j < line.size(); j ++ ) {
					Point pt = line.get(j);
					intensities[j] = tempProcessor.get(pt.x, pt.y);
				}
				
				//add to master array
				boundary[i] = intensities;
			}
			
			//Trims each intensity spread to is its minimum
			int [][] boundary2 = trim(boundary);
			
			//Averages point-spreads from the same segment into a single point-spread
			double [] averageIntensities = averageGuassian(boundary2);
				
			//calculates length to real length ratio
			double diameter = (2 * radius) - 1;
			double distance = averageIntensities.length;
			double adjustment = diameter/distance;
			System.out.println("Diameter: " + diameter + "   Distance: " + distance + "   Adjustment:   " + adjustment);
			
			
			//TEST single object
			//if (segment.getLinkSet().getName() == 0) {
				for (int k = 0; k < averageIntensities.length; k ++) {
					//System.out.println("Intensity: " + averageIntensities[k]);
				}
				displayHistogram(averageIntensities);
			//}
			
			
			//Calculate sigma
			System.out.println("Sigma for segment " + segment.getLinkSet().getName() + " is: " + calculateSigma(averageIntensities, adjustment));
			sigma += calculateSigma(averageIntensities, adjustment);
			

			
		}
		System.out.println("Sigma over frameSet Size:  " + sigma/(double)frameSet.size());
		return (sigma/(double)frameSet.size());
	}
	
	
	
	
	

	//Get average of circular evaluation (@param int[line][intensityAlongLine])
	double[] averageGuassian (int[][] guassians) {
		//TODO: weights for the averaging
		double [] averageIntensities = new double [guassians[0].length];
		//System.out.println("Average Intensities Length: " + averageIntensities.length);
		for (int j = 0; j < guassians[0].length; j++) {
			//System.out.println("j: " + j + "   Guassian length: " + guassians[0][j].length);
			double sum = 0;
			for (int i = 0; i < guassians.length; i++) {
				//System.out.println("Sum: " + sum);
				sum += (double) guassians[i][j];
			}
			averageIntensities[j] = sum / (double) guassians[0].length;
			//System.out.println("Average intensities at j: " + averageIntensities[j]);
		}
		return averageIntensities;
	}
	

	//Calculate sigma by partial fitting of guassian function (Ikki 2019 implementation)
	double calculateSigma(double intensity[], double adjustment) {
		double area1= 0; //area under curve because delta-x = 1; 
		double max = -1;
		for (int i = 0 ; i < intensity.length; i++) {
			if (intensity[i] > max) max = intensity[i];
			area1 += intensity[i];
		}
		area1 = area1 / adjustment;
		
		double area2 = Math.sqrt(Math.PI * 2) * max;
		return area1/area2;
	}
	
	
	//Display histogram
	void displayHistogram (double[] histogram) {
		
		//Testing: histogram of sigmas
		Plot plot = new Plot("Intensity Distribution", "Pixel", "Intensity");
		PlotWindow plowWindow;
		double[] xValues = new double[histogram.length];
		double[] yValues = new double[histogram.length];
		
		//Translates to doubles
		for (int i = 0; i < histogram.length; i++) {
			xValues[i] = (double) i;
			yValues[i] = (double) histogram[i];
		}
	
		plot.add("dot", xValues, yValues);
		ImageProcessor plotProc = plot.getProcessor();
		ImagePlus image = new ImagePlus("Plot", plotProc);
		image.show();
	}
	
	
	//Trims guassian functions to the minimal length
	int [][] trim(int array[][]) {
		
		//find shortest guassian
		int minLength = Integer.MAX_VALUE;
		for (int i = 0; i < array.length; i++) {
			if (array[i].length < minLength) minLength = array[i].length;
		}
		
		//trim others to shortest guassian
		for (int i = 0; i < array.length; i++) {
			int length = array[i].length;
			if (length > minLength) {
				//trim tail before head
				int trim = (length - minLength) / 2; //rounds down if odd
				boolean even = (length - minLength) % 2 == 0;
				if (even) array[i] = Arrays.copyOfRange(array[i], trim, length - trim);
				if (!even) array[i] = Arrays.copyOfRange(array[i], trim, length - trim + 1);		
			}
		}
		return array;
	}
	
	
	
	
	
	//Draws the line
	void drawLine(Point[] line, ImageProcessor tempProcessor) {
		
		//for (Point pt: line) {
		//	tempProcessor.set(pt.x, pt.y, 255);
		//}
		
		ImagePlus image = new ImagePlus("Line" ,tempProcessor);
		image.show();
	}

	
	
	
	
	
}
	
	
