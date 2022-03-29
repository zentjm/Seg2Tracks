package gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Polygon;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.prefs.Preferences;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import externalSegmentation.ExternalSegmentation;
import identification.Identification;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;
import internalSegmentation.InternalSegmentation;
import linkage.Linkage;
import manualSegmentation.ManualSegmentationController;
import manualSegmentation.ModifiedStackWindow;
import util.Seg2TracksClassLoader;

public class OperationModel extends Observable{
	int panelNumber;
	OperationController controller;
	
	//Methods for linkage
	Linkage linkageMethod;
	InternalSegmentation internalSegmentationMethod;
	ExternalSegmentation externalSegmentationMethod;
	
	//Chosen linkage method
	int linkageSelection;
	int internalSegmentationSelection;
	int externalSegmentationSelection;
	
	//InputFile
	ImageStack inputStack;
	
	//Main Dataset Object
	DataSet dataSet;
	
	//Component loading
	int progress = -1;
	
	//Constructs OperationModel
	public OperationModel(int panelNumber) {
		this.panelNumber = panelNumber;
		initialize();
		//build data structure	
	}
	
	//TODO:Any inital stuff
	public void initialize() {

	}
	
	//Sets the Controller
	public void setController(OperationController controller) {
		this.controller = controller;
	}
		
	//TODO: Enums for run types
	//0 is external segmentation, 1 is internal segmentation
	public void runIt(int runType) {
		
		System.out.println("Opening... " + controller.getInputFilePath());
		
		//open the file
		inputStack = IJ.openVirtual(controller.getInputFilePath()).getImageStack();
	
		
		//loads a new dataSet if none exists
		if (controller.getDataSet() == null) dataSet = new DataSet(inputStack.getWidth(), inputStack.getHeight(), inputStack.getSize());
		else dataSet = controller.getDataSet();
			

		//XXX: testing
		if (dataSet == null) System.out.println("DataSet is null");
		System.out.println("Check 1");
		if (dataSet.getIdentificationExists() == false) System.out.println("it is null");
		System.out.println("Identification Exists: " + dataSet.getIdentificationExists());
		System.out.println("Check 2");
		
		//Runs necessary functions
		if (!dataSet.getIdentificationExists()) runIdentification3(); //TODO: which to use?
		if (runType == 0) runExternalSegmentation();	//TODO: control
		if (runType == 1) runInternalSegmentation();	//TODO: implement, control
		if (!dataSet.getLinkageExists()) runLinkage();
		
		//Runs segmentation filters
		SegmentationFilters filter = new SegmentationFilters(controller, inputStack);
		filter.excludeEdges(dataSet, controller.getExcludeInternalEdges(), controller.getExcludeExternalEdges());
		
		//return data to controller
		controller.setRunData(runType, dataSet);	
	}

	//Blurs the image to identify the cells
	private void runIdentification() {
		ImageProcessor tempProcessor;
		Polygon poly;
		FrameSet frameSet;
		GaussianBlur blurrer = new GaussianBlur();
		MaximumFinder maxFinder = new MaximumFinder();
		
		for (int i = 0; i < inputStack.size(); i ++) {
			frameSet = new FrameSet(i, dataSet);
			tempProcessor = inputStack.getProcessor(i+1);
			blurrer.blurGaussian(tempProcessor, controller.getGaussianBlurSigma());
			poly = maxFinder.getMaxima(tempProcessor, controller.getMaximumFinderTolerance(), true);
	
			System.out.println("found maxpoints of " + i +", poly number of points: " + poly.npoints);
			for (int j = 0; j < poly.npoints; j ++) {
				frameSet.add(new Segment(i, new Point(poly.xpoints[j], poly.ypoints[j])));
			}
			dataSet.addFrameSet(frameSet, i);
		}

		dataSet.setIdentificationExists(true);
	}
	
	//Identifies image
	private void runIdentification3() {
		
		//Configure progressBar 
		JProgressBar progressBar = controller.getProgressBar();
		//progressBar.setString("Segmentation: ");
		progressBar.setMinimum(0);
		progressBar.setMaximum(inputStack.getSize());
		progressBar.setValue(0);
		
		Identification id = new Identification();
		id.initialize(inputStack, dataSet);
		id.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		id.setFinder(new MaximumFinder(), controller.getMaximumFinderTolerance());
		
		
		//Non-threaded
		id.run();
		dataSet.setIdentificationExists(true);
		
		/* FOR RUNNIN AS THREAD
		SwingWorker idSegWorker = id.runThread();
		 
		idSegWorker.execute();	
		//Track thread progress
		while (!idSegWorker.isDone()) {
			try { 
				int progress = idSegWorker.getProgress();
				System.out.println("Worker progress: " + progress);
				progressBar.setValue(progress / inputStack.size());
				progressBar.setString ("Identification: " + progress + "/" + inputStack.size());
				Thread.sleep(250);
			}
			catch (Exception ex) {
				System.err.println(ex);
			}
		 }
		 
		 if (idSegWorker.isDone()) {
			 dataSet.setIdentificationExists(true); //TODO: thread done statement should make this true. 
		 }
		 */
	}
	
	
	private void runIdentification2() {
		
		//Configure progressBar 
		JProgressBar progressBar = controller.getProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(inputStack.getSize());
		progressBar.setValue(0);
		
		//Run ID
		SwingWorker worker = runIdentificationThread();
		worker.execute();
			
		//Track thread progress
		while (!worker.isDone()) {
			try { 
				System.out.println("ID Worker progress: " + worker.getProgress());
				progressBar.setValue(worker.getProgress());
				Thread.sleep(250);
			}
			catch (Exception ex) {
				System.err.println(ex);
			}
		 }
		 
		 if (worker.isDone()) {
			 dataSet.setIdentificationExists(true); //TODO: thread done statement should make this true. 
		 }
	}
	
	
	private SwingWorker runIdentificationThread() {
		return new SwingWorker<Void, Integer>() {
			@Override
			public Void doInBackground() {		
				ImageProcessor tempProcessor;
				Polygon poly;
				FrameSet frameSet;
				GaussianBlur blurrer = new GaussianBlur();
				MaximumFinder maxFinder = new MaximumFinder();
				for (int i = 0; i < inputStack.size(); i ++) {
					frameSet = new FrameSet(i, dataSet);
					tempProcessor = inputStack.getProcessor(i+1);
					blurrer.blurGaussian(tempProcessor, controller.getGaussianBlurSigma());
					poly = maxFinder.getMaxima(tempProcessor, controller.getMaximumFinderTolerance(), true);
					System.out.println("found maxpoints of " + i +", poly number of points: " + poly.npoints);
					for (int j = 0; j < poly.npoints; j ++) {
						frameSet.add(new Segment(i, new Point(poly.xpoints[j], poly.ypoints[j])));
					}
					dataSet.addFrameSet(frameSet, i);
					setProgress(i);
				}
				return null;
			}
		};
	}
				
				
				
				
	//returns for progress bar
	public int operationProgress () {
		return progress;
	}
	
	
	//Runs an automatic External Segmentation Operation
	private void runExternalSegmentation() {	
		
		//Configure progressBar 
		JProgressBar progressBar = controller.getProgressBar();
		//progressBar.setString("Segmentation: ");
		progressBar.setMinimum(0);
		progressBar.setMaximum(inputStack.getSize());
		progressBar.setValue(0);
		progressBar.setString ("Segmentation: ");
		
		//Execute Run
		ExternalSegmentation exSeg = controller.getExternalSegmentationMethod();
		exSeg.initialize(inputStack, dataSet);
		exSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma()); //XXX: Is this the best implemented?
		
		SwingWorker exSegWorker = exSeg.runThread();
		exSegWorker.execute();	
		
		//Track thread progress
		while (!exSegWorker.isDone()) {
			try { 
				int progress = exSegWorker.getProgress();
				System.out.println("Worker progress: " + progress);
				progressBar.setValue(progress / inputStack.size());
				progressBar.setString ("Segmentation: " + progress + "/" + inputStack.size());
				Thread.sleep(250);
			}
			catch (Exception ex) {
				System.err.println(ex);
			}
		 }
		 
		 if (exSegWorker.isDone()) {
			 dataSet.setExternalSegmentationExists(true); //TODO: thread done statement should make this true. 
		 }
		 
		 
		 //Run DataSet through exclusion filters
		
	
		//Return dataSet status
		//dataSet.setExternalSegmentationExists(true); //TODO: thread done statement should make this true. 
		
		
		//ORIGINAL
		/*
		ExternalSegmentation exSeg = controller.getExternalSegmentationMethod();
		exSeg.initialize(inputStack, dataSet);
		exSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma()); //XXX: Is this the best implemented?
		exSeg.run();
		dataSet.setExternalSegmentationExists(true);
		*/
	}

	//Runs the Internal Segmentation Operation
	private void runInternalSegmentation() {
		
		//TODO: Its not necessarily clear that
		InternalSegmentation inSeg = controller.getInternalSegmentationMethod();
		inSeg.initialize(inputStack, dataSet);
		inSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma()); //XXX: Is this the best implemented?
		inSeg.run();
		dataSet.setInternalSegmentationExists(true);
	}
	
	//Runs the Linkage Operation
	private void runLinkage() {
		Linkage link = controller.getLinkageMethod();
		link.initialize(dataSet);
		link.run();
		dataSet.setLinkageExists(true);
	}
	
	//returns panelList to allow Seg2TracksController to construct OperationControllers 
	public int getPanelNumber() {
		return panelNumber;
	}
	
	//TODO: link operations to Progress Bar
	public void updateProgressBar() {
		
		
		//1. TODO: Progress bar #1: Linkage
		//2. TODO: Progress bar #2: Segmentation

		setChanged();
		notifyObservers();
	}
	
	
	
	
	

}
