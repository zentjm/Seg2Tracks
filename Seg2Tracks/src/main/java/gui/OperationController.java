package gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import dataStructure.DataSet;
import externalSegmentation.ExternalSegmentation;
import internalSegmentation.InternalSegmentation;
import linkage.Linkage;
import manualSegmentation.ManualSegmentationController;
import util.FileResourcesUtil;
import util.FileSelectionPanel;
import util.Seg2TracksClassLoader;

public class OperationController {
	
	Seg2TracksController controller;
	OperationPanel panel;
	OperationModel model;
	int panelNumber;
	
	//java preferences loading directory
	Preferences preferences = Preferences.userRoot().node("/seg2tracks");
	
	//Plugin Segmentation/Linkage
	Linkage [] linkageMethods;
	InternalSegmentation [] internalSegmentationMethods;
	ExternalSegmentation [] externalSegmentationMethods;
	
	//Loaded and Saved main inputs
	String inputField;
	String inputFilePath;
	String dataSetName;
	int linkageSelection;
	int internalSegmentationSelection;
	int externalSegmentationSelection;
	double gaussianBlurSigma;
	double maximumFinderTolerance;
	
	//DataSet acquisition and loading
	DataSet dataSet;
	boolean loadedInternalDataSet = false; //TODO: used?
	boolean loadedData = false;
	String [] autoSaves;
	
	//General external segmentation settings
	boolean excludeExternalEdges;
	boolean excludeInternalEdges;
	
	
	//General internal segmentation settings
	boolean internalEdgeExclusion;
	
	
	//Hold whether current External Method is interactive
	boolean automatic; 

	public OperationController(Seg2TracksController controller, OperationModel model, int panelNumber) {
		this.controller = controller;
		this.panelNumber = panelNumber;
		this.model = model;
		loadPlugins();
		loadSettings(); //TODO: make settings reset if plugin class is added or removed
		model.setController(this);
		panel = new OperationPanel(this, model, panelNumber);
	}
	
	//Dynamically load the segmentation and linkage classes
	public void loadPlugins() {
		Seg2TracksClassLoader classLoader = new Seg2TracksClassLoader(); //TODO: push this up to the Seg2TracksController
		linkageMethods = classLoader.getLinkageMethods();
		internalSegmentationMethods = classLoader.getInternalSegmentationMethods();
		externalSegmentationMethods = classLoader.getExternalSegmentationMethods();
	}
	
	//load from preferences
	public void loadSettings() {
		inputField = preferences.get("INPUT_FILE_PATH" + panelNumber, "Insert Input File Path");
		linkageSelection = preferences.getInt("LINKAGE_SELECTION" + panelNumber, 0);
		internalSegmentationSelection = preferences.getInt("INTERNAL_SEGMENTATION_SELECTION" + panelNumber, 0);
		externalSegmentationSelection = preferences.getInt("EXTERNAL_SEGMENTATION_SELECTION" + panelNumber, 0);
		gaussianBlurSigma = preferences.getDouble("GAUSSIAN_BLUR_SIGMA" + panelNumber, 20);
		maximumFinderTolerance = preferences.getDouble("MAXIMUM_FINDER_TOLERANCE" + panelNumber, 15);
		dataSetName = preferences.get("DATASET_NAME" + panelNumber, "DataSet" + panelNumber);
		excludeInternalEdges =  preferences.getBoolean("EXCLUDE_INTERNAL_EDGES" + panelNumber, true);
		excludeExternalEdges =  preferences.getBoolean("EXCLUDE_EXTERNAL_EDGES" + panelNumber, true);
	}
	
	//save to preferences
	public void saveSettings() {
		preferences.put("INPUT_FILE_PATH" + panelNumber, inputField);
		preferences.putInt("LINKAGE_SELECTION" + panelNumber, linkageSelection);
		preferences.putInt("INTERNAL_SEGMENTATION_SELECTION" + panelNumber, internalSegmentationSelection);
		preferences.putInt("EXTERNAL_SEGMENTATION_SELECTION" + panelNumber, externalSegmentationSelection);	
		preferences.putDouble("GAUSSIAN_BLUR_SIGMA" + panelNumber, gaussianBlurSigma);
		preferences.putDouble("MAXIMUM_FINDER_TOLERANCE" + panelNumber, maximumFinderTolerance);
		preferences.put("DATASET_NAME" + panelNumber, dataSetName);
		preferences.putBoolean("EXCLUDE_INTERNAL_EDGES" + panelNumber, excludeInternalEdges);
		preferences.putBoolean("EXCLUDE_EXTERNAL_EDGES" + panelNumber, excludeExternalEdges);
	}

	//setters for settings
	public void setInputField(String inputFieldText) {
		this.inputField = inputFieldText;
	}
	
	public void setComboBoxLinkage(int linkageSelection) {
		this.linkageSelection = linkageSelection;
	}
	
	public void setComboBoxInternalSegmentation(int internalSegmentationSelection) {
		this.internalSegmentationSelection = internalSegmentationSelection;
	}
	
	public void setComboBoxExternalSegmentation(int externalSegmentationSelection) {
		this.externalSegmentationSelection = externalSegmentationSelection;
	}
	
	public void setGaussianBlurSigma(double gaussianBlurSigma) {
		this.gaussianBlurSigma = gaussianBlurSigma;
	}
	
	public void setMaximumFinderTolerance(double maximumFinderTolerance) {
		this.maximumFinderTolerance = maximumFinderTolerance;
	}

	
	
	
	//getters for settings
	public String getInputField() {
		return inputField;
	}
	
	public int getLinkageSelection() {
		return linkageSelection;
	}
	
	public int getInternalSegmentationSelection() {
		return internalSegmentationSelection;
	}
	
	public int getExternalSegmentationSelection() {
		return externalSegmentationSelection;
	}
	
	public double getGaussianBlurSigma() {
		return gaussianBlurSigma;
	}
	
	public double getMaximumFinderTolerance() {
		return maximumFinderTolerance;
	}
	
	public String getDataSetName() {
		return dataSetName;
	}
	
	public JProgressBar getProgressBar() {
		return controller.getProgressBar();
	}
	
	
	//Gets Linkage/Segmentation names for panel display
	public String[] getLinkageMethodNames() {
		String [] names = new String [linkageMethods.length];
		for (int i = 0; i < names.length; i ++) {
			names[i] = linkageMethods[i].toString();
		}
		return names;
	}
	
	public String[] getInternalSegmentationMethodNames() {
		String [] names = new String [internalSegmentationMethods.length];
		for (int i = 0; i < names.length; i ++) {
			names[i] = internalSegmentationMethods[i].toString();
		}
		return names;
	}
	
	public String[] getExternalSegmentationMethodNames() {
		String [] names = new String [externalSegmentationMethods.length];
		for (int i = 0; i < names.length; i ++) {
			names[i] = externalSegmentationMethods[i].toString();
		}
		return names;
	}
	
	
	//TODO: Gets Linkage/Segmentation names for Help button display
	

	//Clears dataSet
	public void clearData() {
		dataSet = null;
		loadedData = false;
		panel.updateSegmentationLoaded(0,0);
		panel.updateSegmentationLoaded(1,0);
		controller.allSegmentationLoaded();
	}
	

	//Return selected Linkage/Segmentation methods to the OperatorModel
	public Linkage getLinkageMethod() {
		return linkageMethods[linkageSelection];
	}
	
	public InternalSegmentation getInternalSegmentationMethod() {
		return internalSegmentationMethods[internalSegmentationSelection];
	}
	
	public ExternalSegmentation getExternalSegmentationMethod() {
		return externalSegmentationMethods[externalSegmentationSelection];
	}
	
	//Returns whether internal segmentation is externally dependent
	public boolean isExternallyDependent() {
		return internalSegmentationMethods[internalSegmentationSelection].isExternallyDependent();
	}
	
	//pushes pre-checked input file to the model
	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}
	
	public String getInputFilePath() {
		return inputFilePath;
	}
	
	//to return the operationPanel to the Seg2TracksPanel
	public OperationPanel getPanel() {
		return panel;
	}
	
	//TODO: Enums. 0: externalSeg, 1: internalSeg
	public void setRunData(int type, DataSet dataSet) {
		this.dataSet = dataSet;
		loadedData = true;
		panel.updateSegmentationLoaded(type, 1);
		controller.allSegmentationLoaded();	
	}
	
	//for loading a dataSet from Operation Model modify
	//TODO: Enums. 0: externalSeg, 1: internalSeg
	public void setModifyData(int type, DataSet dataSet) {
		this.dataSet = dataSet;
		loadedData = true;
		panel.updateSegmentationLoaded(type, 3);
		controller.allSegmentationLoaded();	
	}
	
	public void setOverlayData(DataSet dataSet) {
		this.dataSet = dataSet;
		loadedData = true;
		panel.updateSegmentationLoaded(0, 2);
		controller.allSegmentationLoaded();	
	}
	
	//Returns dataSet to Seg2Tracks Controller and operation model
	public DataSet getDataSet() {
		dataSetName = panel.getDataSetName();
		return dataSet;
	}
	
	//To create the calibration menu
	public void calibrateChannel() {
		CalibrationPanel calibrationView = new CalibrationPanel(this, loadedData);
	}
	
	
	public void linkageSettings() {
		//construct a VC linkage window
		//link data to the model
	}
	
	public void internalSegmentationSettings() {
		InternalSegmentationSettings settings = new InternalSegmentationSettings(this);
	}
	
	public void externalSegmentationSettings() {
		ExternalSegmentationSettings settings = new ExternalSegmentationSettings(this);
	}
	
	//TODO: make external and internal runs inherited factory methods to get rid of runType parameter
	public void runExternalSegmentation () {
		if (dataSet != null) {
			if (dataSet.getExternalSegmentationExists() && !panel.dialogAlert("Overwrite current external segmentation data?")) return;
		}
		if (inputFilePath != null) { //TODO: block running if bad input path
			System.out.println("Input File Path: " + inputFilePath);
			model.runIt(0); //TODO: update use of run buttons as necessary
			controller.allSegmentationLoaded();
		}
		
		else System.out.println("Input Path null");
		//generate external segmentation factory and run
		//link model progress bar to view	
	}
	
	
	///TODO:
	public void runInternalSegmentation () {
		if (dataSet != null) {
			if (dataSet.getInternalSegmentationExists() && !panel.dialogAlert("Overwrite current internal segmentation data?")) return; //TODO: load data needs to be specific
		}
		if (inputFilePath != null) { //TODO: block running if bad input path
			System.out.println("Input File Path: " + inputFilePath);
			model.runIt(1); //TODO: update use of run buttons as necessary
			controller.allSegmentationLoaded();
		}
		
		else System.out.println("Input Path null");
		
		
		
		//generate internal segmentation factory and run
		//link model progress bar to view
		//link error reporting to view
		//update status on view
	}
	
	
	//Runs the manual editing over external segmentation //TODO: merge method with internal segmentation?
	public void runModifyExternal() {
		ManualSegmentationController mSeg = new ManualSegmentationController(0, this, true); //TODO: enum
		if (inputFilePath == null) return; //TODO: block running if bad input path by deactivating button
		if (dataSet == null || !dataSet.getExternalSegmentationExists()) {  
			mSeg.runDataSet();
			return;
		}	
		if (dataSet.getExternalSegmentationExists()) mSeg.runDataSet(dataSet); //TODO: Check Loaded External DataSet applies to any loaded data
		if (mSeg.isDataLoaded()) loadedData = true;
	}
	
	
	//Runs manual editing over internal segmentation //TODO: Change this into a dependency checkbox. 
	public void runModifyInternal() {
		ManualSegmentationController mSeg = new ManualSegmentationController(1, this, false); //TODO: enum for runType TODO: no runtype, only outer bounds
		if (inputFilePath == null) return; //TODO: block running if bad input path by deactivating button
		if (dataSet == null || !dataSet.getInternalSegmentationExists()) {  
			mSeg.runDataSet();
			return;
		}	
		if (dataSet.getInternalSegmentationExists()) mSeg.runDataSet(dataSet); //TODO: Check Loaded External DataSet applies to any loaded data
	}
	
	public void setViewActive(boolean enabled) {
		controller.setViewActive(enabled);
	}
	
	//For loaded internal segmentation
	public boolean externalSegmentationExists() {
		if (dataSet != null) return dataSet.getExternalSegmentationExists();
		return false;
	}
	
	
	
	//Loads DataSet
	//TODO: eunps for runType, loading
	public void loadDataSet () {		
		if (dataSet != null) {
			if ((dataSet.getExternalSegmentationExists()) && !panel.dialogAlert("Overwrite external segmentation data?")) return;
			if ((dataSet.getInternalSegmentationExists()) && !panel.dialogAlert("Overwrite internal segmentation data?")) return;
		}
		FileResourcesUtil util = new FileResourcesUtil();
		dataSet = util.loadData(panel);
		if (dataSet != null) {
			if (dataSet.getExternalSegmentationExists()) {
				panel.updateSegmentationLoaded(0, 2);
				loadedData = true;
			}
			if (dataSet.getInternalSegmentationExists()) {
				panel.updateSegmentationLoaded(1, 2);
				loadedData = true;
			}
		}
		if (dataSet == null) {
			panel.updateSegmentationLoaded(0, 0);
			panel.updateSegmentationLoaded(1, 0);
			loadedData = false;
		}
		controller.allSegmentationLoaded();		
	}

	//indicates if data has been loaded
	public boolean isDataLoaded () {
		return loadedData;
	}
	
	//Autosave DataSet
	public void autosave() {
		FileResourcesUtil util = new FileResourcesUtil();
		if (dataSet != null) util.autosaveDataSet(dataSet);
	}
	
	//Saves DataSet
	public void saveExternalSegmentation () {
		FileResourcesUtil util = new FileResourcesUtil();
		if (dataSet != null) util.saveDataSet(panel, dataSet);
		else System.out.println("dataSet is null");
	}
	
	public void errorMessage (String error) {
		JOptionPane.showMessageDialog(panel,error, "Error", JOptionPane.ERROR_MESSAGE);
	}

	public boolean confirmSelection(String alert) {
		int choice = JOptionPane.showConfirmDialog(panel, alert, "alert", JOptionPane.OK_CANCEL_OPTION);
		if (choice == JOptionPane.OK_OPTION) return true;
		else return false;
	}

	
	//Edge Exclusion Settings
	public void setExcludeExternalEdges(boolean excludeExternalEdges) {
		this.excludeExternalEdges = excludeExternalEdges;
	}
	
	public void setExcludeInternalEdges(boolean excludeInternalEdges) {
		this.excludeInternalEdges = excludeInternalEdges;
	}

	public boolean getExcludeExternalEdges() {
		return excludeExternalEdges;
	}
	
	public boolean getExcludeInternalEdges() {
		return excludeInternalEdges;
	}
	
	
	
	
	//Internal Segmentation Settings

	
	
	
}
