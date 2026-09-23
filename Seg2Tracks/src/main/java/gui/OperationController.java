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
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.RecursiveDataSet;
import dataStructure.Segment;
import linkage.Linkage;
import manualSegmentation.ManualSegmentationController;
import manualSegmentation.RecursionManualController;
import sarn.Sarn;
import segmentation.Segmentation;
import util.FileResourcesUtil;
import util.FileSelectionPanel;
import util.Seg2TracksClassLoader;

/**
 * Controller for the segmentation operation panel. Manages the entire segmentation workflow including
 * identification (object detection), external segmentation (SARN), internal segmentation, linkage,
 * and post-segmentation filtering. Coordinates with OperationPanel and OperationModel following MVC pattern.
 * Supports multiple operation panels for recursive/subsegmentation workflows.
 */
public class OperationController {
	
	Seg2TracksController controller;
	OperationPanel panel;
	OperationModel model;
	int panelNumber;
	
	//java preferences loading directory
	Preferences preferences = Preferences.userRoot().node("/seg2tracks");
	
	//Plugin Segmentation/Linkage
	Linkage [] linkageMethods;
	Segmentation [] internalSegmentationMethods;
	Sarn [] externalSegmentationMethods;
	
	//Loaded and Saved main inputs
	String inputField;
	String inputFilePath;
	String dataSetName;
	int linkageSelection;
	int internalSegmentationSelection;
	int externalSegmentationSelection;
	double gaussianBlurSigma;
	double maximumFinderTolerance;   // stored as fraction [0–1]; UI displays as percent
	double recursiveTolerancePct;    // stored as percent [0–100]; used as fraction in Identification
	boolean invertIntensity;

	// Boundary Cleanup settings (removeLoops search-distance scaling + douglasPeucker
	// tolerance) — see sarn.Sarn.setCleanupParams for the full rationale.
	double searchFraction;         // fraction of a contour's own point count
	int searchCeiling;             // upper bound (points) on the search window
	double simplificationEpsilon;  // douglasPeucker perpendicular-distance tolerance (pixels)
	
	//DataSet acquisition and loading
	DataSet dataSet;
	boolean loadedInternalData = false; //TODO: used?
	boolean loadedExternalData = false;
	//boolean loadedData = false;
	String [] autoSaves;
	
	//General external segmentation settings
	boolean excludeExternalEdges;
	boolean excludeInternalEdges;
	
	//General internal segmentation settings
	boolean internalEdgeExclusion;
	
	//Allow this controller to subsegment another
	boolean subsegmentOption = false;

	// Pre-linked subsegmentation (created via the Subsegment button)
	boolean linkedSubsegment = false;
	OperationController linkedParentController = null; // direct reference to the parent panel's controller

	// Number of linked subsegmentation panels currently open from this panel.
	// Subsegment button is disabled while this is > 0.
	int linkedPanelCount = 0;
	
	//Operation threads
	SwingWorker runExternalSegmentation;
	SwingWorker runInternalSegmentation;
	
	
	//Hold whether current External Method is interactive
	boolean automatic; 

	public OperationController(Seg2TracksController controller, OperationModel model, int panelNumber, boolean initialLoad) {
		this.controller = controller;
		this.panelNumber = panelNumber;
		this.model = model;
		loadPlugins();
		loadSettings(); //TODO: make settings reset if plugin class is added or removed
		model.setController(this);
		panel = new OperationPanel(this, model, panelNumber);
	}

	/**
	 * Constructor for panels created via the Subsegment button.
	 * The new panel is pre-linked directly to {@code parentController} and starts in
	 * recursive mode — no checkbox or combobox selection is required.
	 *
	 * @param parentController the controller of the parent panel that spawned this one
	 */
	public OperationController(Seg2TracksController controller, OperationModel model,
	                           int panelNumber, boolean initialLoad, OperationController parentController) {
		this.controller = controller;
		this.panelNumber = panelNumber;
		this.model = model;
		this.linkedSubsegment = true;
		this.linkedParentController = parentController;
		this.subsegmentOption = true;
		loadPlugins();
		loadSettings();
		model.setController(this);
		panel = new OperationPanel(this, model, panelNumber, true);
	}
	
	//Dynamically load the segmentation and linkage classes
	public void loadPlugins() {
		Seg2TracksClassLoader classLoader = new Seg2TracksClassLoader(); //TODO: push this up to the Seg2TracksController
		linkageMethods = classLoader.getLinkageMethods();
		internalSegmentationMethods = classLoader.getSegmentationMethods();
		externalSegmentationMethods = classLoader.getSarnMethods();
	}
	
	//load from preferences
	public void loadSettings() {
		inputField = preferences.get("INPUT_FILE_PATH" + panelNumber, "Insert Input File Path");
		linkageSelection = preferences.getInt("LINKAGE_SELECTION" + panelNumber, 1); // default: ModifiedHungarian (index 1)
		internalSegmentationSelection = preferences.getInt("INTERNAL_SEGMENTATION_SELECTION" + panelNumber, 0);
		externalSegmentationSelection = preferences.getInt("EXTERNAL_SEGMENTATION_SELECTION" + panelNumber, 0);
		gaussianBlurSigma = preferences.getDouble("GAUSSIAN_BLUR_SIGMA" + panelNumber, 20);
		// Stored as fraction [0–1]. Migrate any legacy value > 1 (was stored as a raw percent) automatically.
		double rawTolerance = preferences.getDouble("MAXIMUM_FINDER_TOLERANCE" + panelNumber, 0.15);
		maximumFinderTolerance = (rawTolerance > 1.0) ? rawTolerance / 100.0 : rawTolerance;
		recursiveTolerancePct = preferences.getDouble("RECURSIVE_TOLERANCE_PCT" + panelNumber, 10.0);
		invertIntensity = preferences.getBoolean("INVERT_INTENSITY" + panelNumber, false);
		dataSetName = preferences.get("DATASET_NAME" + panelNumber, "Set " + (panelNumber + 1));
		excludeInternalEdges =  preferences.getBoolean("EXCLUDE_INTERNAL_EDGES" + panelNumber, false);
		//excludeExternalEdges =  preferences.getBoolean("EXCLUDE_EXTERNAL_EDGES" + panelNumber, false);
		// Defaults are the same illustrative starting values documented in CLAUDE.md — the point
		// of Boundary Cleanup Settings is that the user tunes these per dataset, not that these
		// defaults are individually "correct."
		searchFraction = preferences.getDouble("SEARCH_FRACTION" + panelNumber, 0.15);
		searchCeiling = preferences.getInt("SEARCH_CEILING" + panelNumber, 400);
		simplificationEpsilon = preferences.getDouble("SIMPLIFICATION_EPSILON" + panelNumber, 1.5);
	}

	//save to preferences
	public void saveSettings() {
		preferences.put("INPUT_FILE_PATH" + panelNumber, inputField);
		preferences.putInt("LINKAGE_SELECTION" + panelNumber, linkageSelection);
		preferences.putInt("INTERNAL_SEGMENTATION_SELECTION" + panelNumber, internalSegmentationSelection);
		preferences.putInt("EXTERNAL_SEGMENTATION_SELECTION" + panelNumber, externalSegmentationSelection);
		preferences.putDouble("GAUSSIAN_BLUR_SIGMA" + panelNumber, gaussianBlurSigma);
		preferences.putDouble("MAXIMUM_FINDER_TOLERANCE" + panelNumber, maximumFinderTolerance);
		preferences.putDouble("RECURSIVE_TOLERANCE_PCT" + panelNumber, recursiveTolerancePct);
		preferences.putBoolean("INVERT_INTENSITY" + panelNumber, invertIntensity);
		preferences.put("DATASET_NAME" + panelNumber, dataSetName);
		preferences.putBoolean("EXCLUDE_INTERNAL_EDGES" + panelNumber, excludeInternalEdges);
		//preferences.putBoolean("EXCLUDE_EXTERNAL_EDGES" + panelNumber, excludeExternalEdges);
		preferences.putDouble("SEARCH_FRACTION" + panelNumber, searchFraction);
		preferences.putInt("SEARCH_CEILING" + panelNumber, searchCeiling);
		preferences.putDouble("SIMPLIFICATION_EPSILON" + panelNumber, simplificationEpsilon);
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
		saveSettings();
	}

	public void setMaximumFinderTolerance(double maximumFinderTolerance) {
		this.maximumFinderTolerance = maximumFinderTolerance;
		saveSettings();
	}

	public double getRecursiveTolerancePct() {
		return recursiveTolerancePct;
	}

	public void setRecursiveTolerancePct(double recursiveTolerancePct) {
		this.recursiveTolerancePct = recursiveTolerancePct;
		saveSettings();
	}

	public boolean getInvertIntensity() {
		return invertIntensity;
	}

	public void setInvertIntensity(boolean invertIntensity) {
		this.invertIntensity = invertIntensity;
		saveSettings();
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

	public double getSearchFraction() {
		return searchFraction;
	}

	public void setSearchFraction(double searchFraction) {
		this.searchFraction = searchFraction;
		saveSettings();
	}

	public int getSearchCeiling() {
		return searchCeiling;
	}

	public void setSearchCeiling(int searchCeiling) {
		this.searchCeiling = searchCeiling;
		saveSettings();
	}

	public double getSimplificationEpsilon() {
		return simplificationEpsilon;
	}

	public void setSimplificationEpsilon(double simplificationEpsilon) {
		this.simplificationEpsilon = simplificationEpsilon;
		saveSettings();
	}
	
	public String getDataSetName() {
		return dataSetName;
	}

	/**
	 * Sets the DataSet name programmatically, keeping the controller field, the panel
	 * text field, and the stored preference all in sync.  Used to give auto-generated
	 * names to linked subsegmentation panels immediately after construction.
	 */
	void initDataSetName(String name) {
		dataSetName = name;
		panel.dataSetName.setText(name);
		preferences.put("DATASET_NAME" + panelNumber, name);
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
	
	public void cancelExternalRun() {
		cancelExternalSegmentation();
	}

	/**
	 * Cancels a running internal segmentation worker and resets the panel to its
	 * pre-run state.  The background thread may continue briefly if the algorithm
	 * does not respond to the interrupt; the UI resets immediately regardless.
	 */
	public void cancelInternalSegmentation() {
		if (runInternalSegmentation != null) runInternalSegmentation.cancel(true);
		panel.updateSegmentationLoaded(1, 0); // reset button to "Run"
	}
	
	
	//TODO: Gets Linkage/Segmentation names for Help button display
	

	//Clears dataSet
	/*
	 * TODO:
	 * 0: Clear internal  -> only internal
	 * 1: Clear external  -> only external unless internally dependent. Clear internal if it is dependent (note with dialog)
	 * 2: Clear all       -> reset dataSet
	 * 	  If last cleared -> reset DataSet. 
	 * 
	 */
	public void clearData(int choice) {
		
		//clear internal segmentation
		if (choice == 0) {
			if (!dataSet.getExternalSegmentationExists()) choice = 2;
			else {
				dataSet.removeSegmentationType(0);
				loadedInternalData = false;
				panel.updateSegmentationLoaded(1,0);
			}
			
		}
		
		//clear external segmentation
		if (choice == 1) {
			if (!dataSet.getInternalSegmentationExists()) choice = 2;
			else {
				dataSet.removeSegmentationType(1);
				loadedExternalData = false;
				panel.updateSegmentationLoaded(0,0);
			}
		}
		
		//clear all segmentation
		if (choice == 2)	 {
			dataSet = null;
			//loadedData = false;
			loadedInternalData = false;
			loadedExternalData = false;
			panel.updateSegmentationLoaded(0,0);
			panel.updateSegmentationLoaded(1,0);
			controller.allSegmentationLoaded();
		}
	}
	

	//Return selected Linkage/Segmentation methods to the OperatorModel
	public Linkage getLinkageMethod() {
		return linkageMethods[linkageSelection];
	}
	
	public Segmentation getInternalSegmentationMethod() {
		return internalSegmentationMethods[internalSegmentationSelection];
	}
	
	public Sarn getExternalSegmentationMethod() {
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
	// Runs r on the EDT: immediately if already there, otherwise via invokeLater.
	// Background threads (SwingWorker.doInBackground) call the setData/overlay methods,
	// so every Swing mutation they trigger must be funneled through here.
	private static void runOnEdt(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) r.run();
		else SwingUtilities.invokeLater(r);
	}

	public void setRunData(int type, DataSet dataSet) {
		this.dataSet = dataSet;
		if (type == 1) loadedInternalData = true;
		if (type == 0) loadedExternalData = true;
		final int t = type;
		runOnEdt(() -> {
			panel.updateSegmentationLoaded(t, 1);
			controller.allSegmentationLoaded();
		});
	}

	//for loading a dataSet from Operation Model modify
	//TODO: Enums. 0: externalSeg, 1: internalSeg
	public void setModifyData(int type, DataSet dataSet) {
		this.dataSet = dataSet;
		if (type == 1) loadedInternalData = true;
		if (type == 0) loadedExternalData = true;
		final int t = type;
		runOnEdt(() -> {
			panel.updateSegmentationLoaded(t, 3);
			controller.allSegmentationLoaded();
		});
	}

	public void setOverlayData(DataSet dataSet) {
		this.dataSet = dataSet;
		loadedInternalData = true;
		loadedExternalData = true;
		runOnEdt(() -> {
			panel.updateSegmentationLoaded(0, 2);
			controller.allSegmentationLoaded();
		});
	}
	
	//Returns dataSet to Seg2Tracks Controller and operation model
	public DataSet getDataSet() {
		dataSetName = panel.getDataSetName();
		return dataSet;
	}
	
	//To create the calibration menu
	public void calibrateChannel() {
		new CalibrationPanel(this, loadedInternalData || loadedExternalData, linkedSubsegment);
	}

	//To create the boundary cleanup settings menu
	public void boundaryCleanupSettings() {
		new BoundaryCleanupPanel(this);
	}
	
	public void linkageSettings() {
		//construct a VC linkage window
		//link data to the model
	}
	
	public void internalSegmentationSettings() {
		InternalSegmentationSettings settingsIn = new InternalSegmentationSettings(this);
	}
	
	public void externalSegmentationSettings() {
		ExternalSegmentationSettings settingEx = new ExternalSegmentationSettings(this);
	}

	

	//TODO: make external and internal runs inherited factory methods to get rid of runType paramete
	public void runExternalSegmentation () {
		
		//NO THREAD
		//Clear previous run
		if (dataSet != null) { 
			if (dataSet.getExternalSegmentationExists()) {
				if (panel.dialogAlert("Clear current external segmentation data?")) {
					clearData(1);
				}
				return;
			}
		}
		
		//TODO: effective use of a cancel button
		
		if (inputFilePath != null) { //TODO: block running if bad input path
			//System.out.println("Input File Path: " + inputFilePath);
		
			//THREAD
			runExternalSegmentation = runExternalSegmentationThread();
			runExternalSegmentation.execute();
			panel.updateSegmentationLoaded(0, 4); // show Cancel button while running
		}
		else System.out.println("Input Path null");
		
	}
	
	
	public SwingWorker runExternalSegmentationThread() {
		return new SwingWorker<Void, Integer>() {
			@Override
			public Void doInBackground() throws Exception {	
				try {
					model.runIt(0); 
					return null;
				}
				catch (InterruptedException e) {
					System.out.println("INTERRUPTED");
					if (isCancelled()) {
						System.out.println("CANCELLED");
					};
					return null;
				}
				catch (Exception e) {
					System.err.println("[Seg2Tracks] External segmentation failed:");
					e.printStackTrace();
					runOnEdt(controller::allSegmentationLoaded);
					return null;
				}
				
			}
		};
	}
	
	
	
	
	public void cancelExternalSegmentation() {
		if (runExternalSegmentation != null) runExternalSegmentation.cancel(true);
		panel.updateSegmentationLoaded(0, 0); // reset button to "Run" — no data produced
	}
		

	
	
	 
	/// This is used
	public void runInternalSegmentation () {
		
		/*
		if (dataSet != null) {
			if (dataSet.getInternalSegmentationExists() && !panel.dialogAlert("Overwrite current internal segmentation data?")) return; //TODO: load data needs to be specific
		}
		*/
		
		if (dataSet != null) { 
			if (dataSet.getInternalSegmentationExists()) {
				if (panel.dialogAlert("Clear current internal segmentation data?")) {
					clearData(0);
				}
				return;
			}
		}
		
		if (inputFilePath != null) { //TODO: block running if bad input path
			//System.out.println("Input File Path: " + inputFilePath);
			
			//THREAD
			runInternalSegmentation = runInternalSegmentationThread();
			runInternalSegmentation.execute();
			panel.updateSegmentationLoaded(1, 4);
		}
		
		else System.out.println("Input Path null");
	}
	
	public SwingWorker runInternalSegmentationThread() {
		return new SwingWorker<Void, Integer>() {
			@Override
			public Void doInBackground() {	
				try {
					model.runIt(1);
					if (!isCancelled()) runOnEdt(controller::allSegmentationLoaded);
					return null;
				}
				catch (Exception e) {
					System.err.println("[Seg2Tracks] Internal segmentation failed:");
					e.printStackTrace();
					runOnEdt(controller::allSegmentationLoaded);
					return null;
				}
			
			}
		};
	}
	
	//Runs the manual editing over external segmentation //TODO: merge method with internal segmentation?
	//TODO: Rename "Manually Edit" button — discuss appropriate replacement label (e.g. "Edit Voids", "Draw Voids")
	public void runModifyExternal() {

		// When subsegmentation is active, always open RecursionManualController.
		// It shows existing automated results (from a prior SARN run) via
		// drawExistingResults(), which reads parentCell.getChildDataSet(), so the
		// user can inspect and correct per-cell even after running automatic SARN.
		if (subsegmentOption) {
			DataSet priorDataSet = getPriorDataSet();
			if (priorDataSet == null || priorDataSet.getLinkSetList().isEmpty()) {
				errorMessage("No parent segmentation found. "
						+ "Complete segmentation on the selected dataset before running "
						+ "recursive manual segmentation.");
				return;
			}
			RecursionManualController recSeg = new RecursionManualController(this, priorDataSet, true);
			recSeg.run();
			return;
		}

		// Standard (non-recursive) manual SARN editing
		ManualSegmentationController mSeg = new ManualSegmentationController(0, this, true); //TODO: enum
		if (inputFilePath == null) return; //TODO: block running if bad input path by deactivating button
		if (dataSet == null || !dataSet.getExternalSegmentationExists()) {
			mSeg.runDataSet();
			return;
		}
		if (dataSet.getExternalSegmentationExists()) mSeg.runDataSet(dataSet); //TODO: Check Loaded External DataSet applies to any loaded data
		//if (mSeg.isDataLoaded()) loadedData = true;
		if (mSeg.isDataLoaded()) loadedExternalData = true;
	}
	
	
	/**
	 * Opens a new subsegmentation panel pre-linked to this panel's DataSet,
	 * then disables the Subsegment button until that panel is removed.
	 */
	public void runSubsegment() {
		if (dataSet == null || !dataSet.getInternalSegmentationExists()) return;
		if (dataSet.getLinkSetList().isEmpty()) {
			errorMessage("No segmented objects found. Run internal segmentation before subsegmenting.");
			return;
		}
		linkedPanelCount++;
		panel.buttonSubsegment.setEnabled(false);
		controller.addSubsegmentPanel(this);
	}

	/** Returns true while at least one linked subsegmentation panel is open from this panel. */
	boolean hasLinkedPanels() {
		return linkedPanelCount > 0;
	}

	/**
	 * Called by {@link Seg2TracksController} when a linked panel spawned from this one
	 * is removed.  Re-enables the Subsegment button when the last linked panel is gone
	 * and internal segmentation is still loaded.
	 */
	void linkedPanelRemoved() {
		if (linkedPanelCount > 0) linkedPanelCount--;
		if (linkedPanelCount == 0 && dataSet != null && dataSet.getInternalSegmentationExists()) {
			panel.buttonSubsegment.setEnabled(true);
		}
	}

	//Runs manual preview over internal segmentation //TODO: Change this into a dependency checkbox.
	public void runModifyInternal() {
		// Recursive panels: show the same cell-by-cell view as "Manually Edit" but
		// read-only — same composite window, sidebar, and context frame; no drawing
		// or modification controls.
		if (subsegmentOption) {
			DataSet priorDataSet = getPriorDataSet();
			if (priorDataSet == null || priorDataSet.getLinkSetList().isEmpty()) {
				errorMessage("No parent segmentation found. "
						+ "Complete segmentation on the selected dataset before previewing.");
				return;
			}
			RecursionManualController recPrev = new RecursionManualController(this, priorDataSet, false);
			recPrev.run();
			return;
		}
		// Standard (non-recursive) preview
		ManualSegmentationController mSeg = new ManualSegmentationController(1, this, false); //TODO: enum for runType
		if (inputFilePath == null) return; //TODO: block running if bad input path by deactivating button
		if (dataSet == null || !dataSet.getInternalSegmentationExists()) {
			mSeg.runDataSet();
			return;
		}
		if (dataSet.getInternalSegmentationExists()) mSeg.runDataSet(dataSet);
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
		// Recursive panels carry no independent file — their data is embedded inside the
		// parent DataSet and propagated automatically when the parent panel loads.
		if (linkedSubsegment) return;
		if (dataSet != null) {
			if ((dataSet.getExternalSegmentationExists()) && !panel.dialogAlert("Overwrite external segmentation data?")) return;
			if ((dataSet.getInternalSegmentationExists()) && !panel.dialogAlert("Overwrite internal segmentation data?")) return;
		}
		FileResourcesUtil util = new FileResourcesUtil();
		dataSet = util.loadData(panel);
		if (dataSet != null) {
			if (dataSet.getExternalSegmentationExists()) {
				panel.updateSegmentationLoaded(0, 2);
				//loadedData = true;
				loadedExternalData = true;
			}
			if (dataSet.getInternalSegmentationExists()) {
				panel.updateSegmentationLoaded(1, 2);
				//loadedData = true;
				loadedInternalData = true;
			}
		}
		if (dataSet == null) {
			panel.updateSegmentationLoaded(0, 0);
			panel.updateSegmentationLoaded(1, 0);
			loadedInternalData = loadedExternalData = false;
		}
		controller.allSegmentationLoaded();
		// Push embedded child DataSets (if any) to linked recursive child panels.
		// If no linked panel exists yet, auto-create and display one.
		controller.propagateChildDataSets(this);
		controller.autoCreateSubsegmentPanel(this);
	}

	//indicates if data has been loaded
	public boolean isDataLoaded () {
		//return loadedData;
		return loadedInternalData || loadedExternalData;
	}
	
	//Autosave DataSet
	public void autosave() {
		// Recursive (child) panels have no independent save file.
		// Delegate to the parent panel, whose DataSet carries child results
		// embedded in each LinkSet.childDataSet field.
		if (linkedSubsegment) {
			if (linkedParentController != null) linkedParentController.autosave();
			return;
		}
		FileResourcesUtil util = new FileResourcesUtil();
		if (dataSet != null) util.autosaveDataSet(dataSet);
	}

	//Saves DataSet
	public void saveExternalSegmentation () {
		// Recursive panels carry no independent file — save via parent panel.
		if (linkedSubsegment) return;
		FileResourcesUtil util = new FileResourcesUtil();
		if (dataSet != null) util.saveDataSet(panel, dataSet);
		else System.out.println("dataSet is null");
	}

	/**
	 * Called by {@link Seg2TracksController#propagateChildDataSets} after the parent
	 * DataSet is loaded from file.  Assembles the per-cell embedded RecursiveDataSets
	 * into a single combined dataset and marks this panel as containing loaded data.
	 * No-op if this panel already holds data (avoids overwriting a freshly-run result).
	 *
	 * @param parentDS the parent DataSet that was just loaded
	 */
	void receiveChildDataSet(DataSet parentDS) {
		if (dataSet != null) return; // Already has live data — don't overwrite

		// Build a combined RecursiveDataSet from the per-cell embedded children
		RecursiveDataSet combined = new RecursiveDataSet(
				parentDS.getWidth(), parentDS.getHeight(), parentDS.getSize(), parentDS);
		for (int f = 0; f < parentDS.getSize(); f++) {
			combined.addFrameSet(new FrameSet(f, combined), f);
		}

		for (LinkSet parentLS : parentDS.getLinkSetList()) {
			DataSet cellChild = parentLS.getChildDataSet();
			if (cellChild == null) continue;
			for (LinkSet childLS : cellChild.getLinkSetList()) {
				// Add directly to list (avoids double-incrementing the name iterator)
				combined.getLinkSetList().add(childLS);
				combined.addChildParentMapping(childLS, parentLS);
				for (Segment s : childLS) {
					int f = s.getFrame();
					if (f >= 0 && f < combined.getFrameSetList().length
							&& combined.getFrameSet(f) != null) {
						combined.getFrameSet(f).add(s);
					}
				}
			}
		}

		if (!combined.getLinkSetList().isEmpty()) {
			combined.setExternalSegmentationExists(true);
			combined.setIdentificationExists(true);
			this.dataSet = combined;
			loadedExternalData = true;
			runOnEdt(() -> {
				panel.updateSegmentationLoaded(0, 2); // "Save Data Loaded"
				controller.allSegmentationLoaded();
			});
		}
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
	/*
	public void setExcludeExternalEdges(boolean excludeExternalEdges) {
		this.excludeExternalEdges = excludeExternalEdges;
	}
	*/
	
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

	
	
	//Subsegmentation Settings

	/**
	 * Returns the parent DataSet for a linked subsegmentation panel.
	 * For panels opened via the Subsegment button, delegates directly to the stored parent
	 * controller reference (safe against list reordering).  Falls back to the preceding
	 * panel's DataSet otherwise.
	 */
	public DataSet getPriorDataSet() {
		if (linkedSubsegment && linkedParentController != null) {
			return linkedParentController.getDataSet();
		}
		// Fallback: panel immediately before this one
		DataSet[] dataSets = controller.getDataSets();
		if (panelNumber > 0 && dataSets != null && panelNumber - 1 < dataSets.length) {
			return dataSets[panelNumber - 1];
		}
		return null;
	}
	
	
	
	
}
