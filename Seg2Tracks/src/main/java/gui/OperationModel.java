package gui;

import java.util.Observable;

import javax.swing.JProgressBar;

import dataStructure.DataSet;
import identification.Identification;
import ij.IJ;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import geometricTools.ModifiedMaximumFinder;
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
		
		// Load the full image stack into memory so each pipeline stage reads from RAM
		// rather than reloading from disk, and so a single inversion pass covers all stages.
		ImageStack virtualStack = IJ.openVirtual(controller.getInputFilePath()).getImageStack();
		inputStack = new ImageStack(virtualStack.getWidth(), virtualStack.getHeight());
		for (int i = 1; i <= virtualStack.getSize(); i++) {
			inputStack.addSlice(virtualStack.getProcessor(i).duplicate());
		}

		// Apply intensity inversion once here before any stage touches the stack.
		// Identification, Sarn, and Segmentation all work on per-frame duplicates,
		// so this in-memory copy stays clean across the full pipeline.
		if (controller.getInvertIntensity()) {
			for (int i = 1; i <= inputStack.getSize(); i++) {
				inputStack.getProcessor(i).invert();
			}
		}
	
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
		
		//update progress bar
		progressBar.setValue(progressBar.getMaximum());
		progressBar.setString("Operation Complete");
	}

	//Identifies points
	protected void runIdentification() {
		
		progressBar.setMinimum(0);
		progressBar.setMaximum(inputStack.getSize());
		progressBar.setValue(0);
	
		Identification id = new Identification();
		id.initialize(inputStack, dataSet, progressBar);
		id.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		id.setFinder(new ModifiedMaximumFinder(), controller.getMaximumFinderTolerance());
		
		id.run();
		dataSet.setIdentificationExists(true);
	}
	
	//Runs an automatic External Segmentation Operation
	protected void runExternalSegmentation() {	
		Sarn exSeg = controller.getExternalSegmentationMethod();
		exSeg.initialize(inputStack, dataSet, progressBar);
		exSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma()); //XXX: Is this the best implemented?
		exSeg.run();
		dataSet.setExternalSegmentationExists(true);
	}
	
	//Runs the Internal Segmentation Operation
	protected void runInternalSegmentation() {
		Segmentation inSeg = controller.getInternalSegmentationMethod();
		inSeg.initialize(inputStack, dataSet, progressBar);
		inSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma()); //XXX: Is this the best implemented?
		inSeg.run();
		dataSet.setInternalSegmentationExists(true);
	}
	
	//Runs the Linkage Operation
	protected void runLinkage() {
		Linkage link = controller.getLinkageMethod();
		link.initialize(dataSet, progressBar);
		link.run();
		dataSet.setLinkageExists(true);
	}
	
	//Runs Segmentation Filters
	protected void runSegmentationFilters() {
		SegmentationFilters filter = new SegmentationFilters(); //Todo select
		filter.initialize(dataSet, progressBar, controller.getExcludeInternalEdges());
		//System.out.println("Exclude edges is: " + controller.getExcludeInternalEdges());
		filter.run();
		
		//filter.excludeEdges(dataSet, controller.getExcludeInternalEdges());
	}
	
	
	//returns panelList to allow Seg2TracksController to construct OperationControllers 
	public int getPanelNumber() {
		return panelNumber;
	}
	
}

