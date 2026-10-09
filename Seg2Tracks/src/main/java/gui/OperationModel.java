package gui;

import java.util.Observable;

import javax.swing.JProgressBar;

import dataStructure.DataSet;
import pipeline.PipelineSettings;
import pipeline.Seg2TracksPipeline;
import ij.ImageStack;
import linkage.Linkage;
import sarn.Sarn;
import segmentation.Segmentation;

/**
 * Model for the operation/segmentation panel. Orchestrates the execution of identification, external segmentation,
 * internal segmentation, linkage, and segmentation filtering operations on image stacks.
 * Manages the DataSet structure and coordinates with pluggable algorithm implementations.
 */
public class OperationModel extends Observable{
	int panelNumber;
	OperationController controller;
	
	//Methods for linkage
	Linkage linkageMethod;
	Segmentation internalSegmentationMethod;
	Sarn externalSegmentationMethod;
	
	//Chosen linkage method
	int linkageSelection;
	int internalSegmentationSelection;
	int externalSegmentationSelection;
	
	//InputFile
	ImageStack inputStack;
	
	//Main Dataset Object
	DataSet dataSet;
	
	//Component loading
	JProgressBar progressBar;
	int progress = -1;
	
	//Constructs OperationModel
	public OperationModel(int panelNumber) {
		this.panelNumber = panelNumber;
		initialize();
		//build data structure	
	}
	
	//TODO:Any initial stuff
	public void initialize() {

	}
	
	//Sets the Controller
	public void setController(OperationController controller) {
		this.controller = controller;
	}
		
	//TODO: Enums for run types
	//0 is external segmentation, 1 is internal segmentation
	public void runIt(int runType) throws Exception {
		
		progressBar = controller.getProgressBar();
		
		//System.out.println("Opening... " + controller.getInputFilePath());
		
		// Load the full stack into memory (inverted once if requested) before any stage runs.
		inputStack = Seg2TracksPipeline.loadStack(controller.getInputFilePath(), controller.getInvertIntensity());
	
		//loads a new dataSet if none exists
		if (controller.getDataSet() == null) dataSet = new DataSet(inputStack.getWidth(), inputStack.getHeight(), inputStack.getSize());
		else dataSet = controller.getDataSet();
			

		/* XXX: testing
		if (dataSet == null) System.out.println("DataSet is null");
		//System.out.println("Check 1");
		if (dataSet.getIdentificationExists() == false) System.out.println("it is null");
		//System.out.println("Identification Exists: " + dataSet.getIdentificationExists());
		//System.out.println("Check 2");
		*/
		
		if (!dataSet.getIdentificationExists()) runIdentification(); //TODO: which to use?
		if (runType == 0) runExternalSegmentation();	//TODO: control
		if (runType == 1) runInternalSegmentation();	//TODO: implement, control
		if (!dataSet.getLinkageExists()) runLinkage();
	
		
		//Runs Segmentation Filters
		if (dataSet.getInternalSegmentationExists()) runSegmentationFilters();
		
		//return data to controller
		controller.setRunData(runType, dataSet);	
		
		//update progress bar: "<panel>: Operation Complete", until the next action
		controller.operationComplete();
	}

	// ── Steps ────────────────────────────────────────────────────────────────
	// Each step runs through the GUI-independent pipeline core (pipeline.Seg2TracksPipeline),
	// with this panel's current settings and its own method instances. RecursionOperationModel
	// overrides runIdentification/runExternalSegmentation and reuses the other three.

	/** This panel's current settings (read fresh for each step, as before). */
	protected PipelineSettings settings() {
		return controller.toPipelineSettings();
	}

	//Identifies points
	protected void runIdentification() {
		Seg2TracksPipeline.identify(inputStack, dataSet, settings(), progressBar);
	}
	
	//Runs an automatic External Segmentation Operation
	protected void runExternalSegmentation() {	
		Seg2TracksPipeline.segmentExternal(inputStack, dataSet, settings(),
				controller.getExternalSegmentationMethod(), progressBar);
	}
	
	//Runs the Internal Segmentation Operation
	protected void runInternalSegmentation() {
		Seg2TracksPipeline.segmentInternal(inputStack, dataSet, settings(),
				controller.getInternalSegmentationMethod(), progressBar);
	}
	
	//Runs the Linkage Operation
	protected void runLinkage() {
		Seg2TracksPipeline.link(dataSet, controller.getLinkageMethod(), progressBar);
	}
	
	//Runs Segmentation Filters
	protected void runSegmentationFilters() {
		Seg2TracksPipeline.filter(dataSet, settings(), progressBar);
	}
	
	
	//returns panelList to allow Seg2TracksController to construct OperationControllers 
	public int getPanelNumber() {
		return panelNumber;
	}
	
}

