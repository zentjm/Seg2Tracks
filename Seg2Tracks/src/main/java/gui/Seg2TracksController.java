package gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JProgressBar;

import org.apache.poi.ss.usermodel.Workbook;

import dataStructure.DataSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.ImagePlus;
import ij.io.FileSaver;

/**
 * Top-level controller for the Seg2Tracks plugin. Manages the main application state, coordinates
 * multiple operation and analysis panels, handles view switching between segmentation and analysis modes,
 * and orchestrates data flow between components. Implements core plugin workflows and result export.
 */
public class Seg2TracksController {
	Seg2TracksModel model;
	
	ArrayList<OperationModel> operationModelList;
	ArrayList<OperationController> operationControllerList;
	
	ArrayList<AnalysisModel> analysisModelList;
	ArrayList<AnalysisController> analysisControllerList;
	
	Seg2TracksPanel view;
	HeadPanel headPanel;
	FloorPanel floorPanel;
	
	DataSet[] dataSets;
	
	ArrayList<ArrayList<ImagePlus>> imageOverlays;
	
	//ImagePlus[] imageOverlays;
	Workbook[] workbooks;
	
	Preferences preferences = Preferences.userRoot().node("/seg2tracks");
	String outputField;
	String outputFilePath;
	
	boolean analysisButtonEnabled;
	
	public Seg2TracksController(Seg2TracksModel model) {
		headPanel = new HeadPanel (this);
		outputField = preferences.get("OUTPUT_FILE_PATH", "Insert Output File Path");
		floorPanel = new FloorPanel (this);
		this.model = model;
		loadOperationControllers();
		loadAnalysisControllers();
		view = new Seg2TracksPanel(this, model, headPanel, floorPanel);
		view.createView();
		//System.out.println("Created view");
		analysisButtonEnabled = false;
	}
	
	//Load operation panels, restoring recursive parent-child links saved from the prior session
	public void loadOperationControllers() {
		operationControllerList = new ArrayList<OperationController>();
		operationModelList = model.getOperationModels();
		for (int i = 0; i < operationModelList.size(); i++) {
			OperationModel mod = operationModelList.get(i);
			int parentPos = model.panelParentPositions[i];
			if (model.panelIsRecursive[i] && parentPos >= 0 && parentPos < operationControllerList.size()) {
				OperationController parentCtrl = operationControllerList.get(parentPos);
				OperationController child = new OperationController(
						this, mod, mod.getPanelNumber(), true, parentCtrl);
				operationControllerList.add(child);
				// Mirror the state set by runSubsegment() so hasLinkedPanels() stays accurate
				parentCtrl.linkedPanelCount++;
			} else {
				operationControllerList.add(new OperationController(
						this, mod, mod.getPanelNumber(), true));
			}
		}
	}
	
	//Load analysis panels
	public void loadAnalysisControllers() {
		analysisControllerList = new ArrayList<AnalysisController>();
		analysisModelList = model.getAnalysisModels();
		for (int i = 0; i < analysisModelList.size(); i ++) {
			analysisControllerList.add(new AnalysisController(this, analysisModelList.get(i), analysisModelList.get(i).getPanelNumber()));
		}
	}
	
	//Retrieve operation panels
	public ArrayList<OperationController> getOperationList() {
		return operationControllerList;
	}
	
	//Retrieve analysis panels
	public ArrayList<AnalysisController> getAnalysisList() {
		return analysisControllerList;
	}
	
	//Open the user manual window
	public void openHelpMenu() {
		HelpMenuPanel panel = new HelpMenuPanel("Seg2Tracks — User Manual", "/UserManual.md");
	}

	//Open the change log window
	public void openChangeLog() {
		HelpMenuPanel panel = new HelpMenuPanel("Seg2Tracks — Change Log", "/ChangeLog.md");
	}
	
	//Retrieve all information from the operation panels and switch to analysis view
	public void switchToAnalysis() { //TODO: switch the code below to just be updateDataSets
		//Collect all dataSets from operations panels in an array
		dataSets = new DataSet[operationControllerList.size()]; //New array for holding dataSets
		for(int i = 0; i < operationControllerList.size(); i++) {
			operationControllerList.get(i).saveSettings(); //save the settings
			dataSets[i] = operationControllerList.get(i).getDataSet();
			if (dataSets[i] != null)
				dataSets[i].setDataSetName(operationControllerList.get(i).getDataSetName());
		}
		//Load dataSets into the analysis menu
		for(int i = 0; i < analysisControllerList.size(); i++) {
			analysisControllerList.get(i).setDataSet(dataSets);
		}
		
		//Load previous output field
		
		//Switch to the analysis panel.
		 view.switchToAnalysis();
		 updateAnalysisLoaded();
	}
	
	
	public void updateDataSets() {
		dataSets = new DataSet[operationControllerList.size()]; //New array for holding dataSets
		for(int i = 0; i < operationControllerList.size(); i++) {
			dataSets[i] = operationControllerList.get(i).getDataSet();
			if (dataSets[i] != null) { // panel may have no data loaded yet
				dataSets[i].setDataSetName(operationControllerList.get(i).getDataSetName());
			}
		}
	}
	
	public String getOutputField() {
		return outputField;
	}
	
	public void setOutputField(String outputField) {
		this.outputField = outputField;
	}
	
	public String getOutputFilePath() {
		return outputFilePath;
	}
	
	//Returns progress bar
	public JProgressBar getProgressBar() {
		return view.getProgressBar();
	}
	
	//pushes pre-checked input file to the model
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}
	
	//TODO: Switch back to the operation screen
	public void switchToOperation() {
		view.switchToOperation();
		allSegmentationLoaded();
	}
	
	public void addOperationPanel() {
		OperationModel tempMod = model.addOperationModel();
		OperationController tempContr = new OperationController(this, tempMod, tempMod.getPanelNumber(), false);
		operationModelList = model.getOperationModels();
		operationControllerList.add(tempContr);
		view.switchToOperation();
	}

	/**
	 * Creates a new subsegmentation panel already linked to {@code parentController} and
	 * inserts it immediately below the parent in the panel list so the display order
	 * reflects the parent–child relationship.
	 *
	 * @param parentController the controller of the panel whose Subsegment button was pressed
	 */
	public void addSubsegmentPanel(OperationController parentController) {
		RecursionOperationModel tempMod = model.addRecursionOperationModel();
		OperationController tempContr = new OperationController(
				this, tempMod, tempMod.getPanelNumber(), false, parentController);
		String autoName = parentController.getDataSetName() + " (Subsegmentation)";
		tempContr.initDataSetName(autoName);
		operationModelList = model.getOperationModels();
		int insertIndex = operationControllerList.indexOf(parentController) + 1;
		operationControllerList.add(insertIndex, tempContr);
		view.switchToOperation();
		// If the parent DataSet was already loaded (from file) and carries embedded child
		// DataSets, push them to this new panel immediately so it doesn't start empty.
		propagateChildDataSets(parentController);
	}
	
	public void removeOperationPanel() {
		OperationController removing = operationControllerList.get(operationControllerList.size() - 1);
		model.removeOperationModel();
		operationModelList = model.getOperationModels();
		operationControllerList.remove(operationControllerList.size() - 1);
		// If the removed panel was linked, notify its parent so the Subsegment button can re-enable
		if (removing.linkedSubsegment && removing.linkedParentController != null) {
			removing.linkedParentController.linkedPanelRemoved();
		}
		view.switchToOperation();
		allSegmentationLoaded();
	}
	
	public void exportResults() {
	
		imageOverlays = new ArrayList<ArrayList<ImagePlus>>(analysisControllerList.size());
		
		workbooks = new Workbook[analysisControllerList.size()];
		for(int i = 0; i < analysisControllerList.size(); i++) {
			analysisControllerList.get(i).saveSettings(); //save the settings
			imageOverlays.add(i, analysisControllerList.get(i).getOverlayedImages());
			workbooks[i] = analysisControllerList.get(i).getWorkbook();
		}
		
		//Save data
		for(int i = 0; i < analysisControllerList.size(); i++) {

			//Save ImagePlus with overlay
			for (int j = 0; j < imageOverlays.get(i).size(); j ++) {
				try {
					new FileSaver(imageOverlays.get(i).get(j)).saveAsTiff(outputFilePath + File.separator + "OverlayedImage_DataSet_" + i +
							"_Method_" + imageOverlays.get(i).get(j).getTitle() + ".tif");
				} 
				catch (Exception e) {
					//System.out.println("Failed to save image overlay file: " + i);
					e.printStackTrace();
				}
			}
				
			//Save Workbook with overlay //TODO: Ability to add to workbook
			try {
				String fileName = outputFilePath + File.separator + "ResultsOutput_DataSet_" + i + ".xlsx";
				try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
					workbooks[i].write(outputStream);
				}
				workbooks[i].close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			//Delete all data
			analysisControllerList.get(i).setOverlayImage(null, true);
			analysisControllerList.get(i).setWorkbook(null);
			
		}
	}
	
	//TODO: does this do anything?
	public DataSet[] getDataSets() {
		return dataSets;
	}
	
	/**
	 * Scans the OperationControllers linked to {@code parentController} and, when the
	 * freshly-loaded parent DataSet contains embedded child DataSets (from a prior
	 * recursive segmentation run), assembles and pushes the combined RecursiveDataSet
	 * to each linked child panel so its status label reflects the loaded data.
	 *
	 * @param parentController the controller whose DataSet was just loaded from file
	 */
	public void propagateChildDataSets(OperationController parentController) {
		DataSet parentDS = parentController.getDataSet();
		if (parentDS == null) return;
		// Quick check — skip the controller scan if no cell carries a child DataSet
		boolean hasChild = false;
		for (LinkSet ls : parentDS.getLinkSetList()) {
			if (ls.getChildDataSet() != null) { hasChild = true; break; }
		}
		if (!hasChild) return;
		for (OperationController child : operationControllerList) {
			if (child.linkedParentController == parentController) {
				child.receiveChildDataSet(parentDS);
			}
		}
	}

	/**
	 * Called after loading a parent DataSet.  If the DataSet contains embedded child
	 * DataSets but no linked subsegmentation panel is currently open, automatically
	 * creates and displays one — exactly as if the user had clicked the Subsegment
	 * button — and then populates it via the existing propagateChildDataSets path.
	 *
	 * No-op when a linked panel already exists (topology was restored on startup) or
	 * when the parent DataSet carries no embedded child data.
	 *
	 * @param parentController the controller whose DataSet was just loaded from file
	 */
	public void autoCreateSubsegmentPanel(OperationController parentController) {
		DataSet parentDS = parentController.getDataSet();
		if (parentDS == null) return;

		// Skip if a linked child panel already exists for this parent
		for (OperationController ctrl : operationControllerList) {
			if (ctrl.linkedParentController == parentController) return;
		}

		// Check whether the parent DataSet actually carries embedded child DataSets
		boolean hasChild = false;
		for (LinkSet ls : parentDS.getLinkSetList()) {
			if (ls.getChildDataSet() != null) { hasChild = true; break; }
		}
		if (!hasChild) return;

		// Mirror what runSubsegment() does, then addSubsegmentPanel handles the rest
		// (including calling propagateChildDataSets to populate the new panel).
		parentController.linkedPanelCount++;
		parentController.panel.buttonSubsegment.setEnabled(false);
		addSubsegmentPanel(parentController);
	}

	//TODO, Some issues with removing panels
	//Updates operation setting based on whether ALL possible segmentations have been run
	public void allSegmentationLoaded() {
		for (OperationController ctrl: operationControllerList) {
			if (!ctrl.isDataLoaded()) {
				view.enableAnalysis(false);
				return;
			}
		}
		view.enableAnalysis(true);
	}

	//Updates analysis setting based on whether analysis has been run
	public void updateAnalysisLoaded() {
		//System.out.println("Running Update Analysis Loaded");
		for (AnalysisController ctrl: analysisControllerList) {
			if (!ctrl.isDataLoaded()) {
				view.enableResults(false);
				//System.out.println("DISABLED result in Seg Controller");
				return;
			}
		}
		view.enableResults(true);
		//System.out.println("enabling result in Seg Controller");
	}
	
	//For making invisible when using manual segmentation
	public void setViewActive(boolean enabled) {
		view.setVisible(enabled);
	}
	
	//Save data when exiting program.
	public void exitProgram() {
		//System.out.println("Exited Seg2Tracks");

		//Save Seg2TracksModel information (#panels)
		model.saveSettings();

		// Save panel ordering for non-recursive panels only.
		// Recursion (subsegmentation) panels are intentionally excluded: they must
		// not reopen on startup without a parent DataSet present.  They are
		// recreated automatically by autoCreateSubsegmentPanel() whenever a DataSet
		// that contains embedded child data is loaded.
		int saveIdx = 0;
		for (OperationController ctrl : operationControllerList) {
			if (ctrl.linkedSubsegment) continue; // never persist recursion panels
			preferences.putInt("PANEL_ORDER_PANELNUM" + saveIdx, ctrl.panelNumber);
			preferences.putBoolean("PANEL_RECURSIVE" + saveIdx, false);
			preferences.putInt("PANEL_PARENT_POS" + saveIdx, -1);
			saveIdx++;
		}
		preferences.putInt("OPERATION_PANEL_NUMBER", saveIdx);

		//Save all OperationModel information
		for (OperationController o: operationControllerList) {
			o.saveSettings();
		}

		//Save all AnalysisModel information
		for (AnalysisController a: analysisControllerList) {
			a.saveSettings();
		}

		//Save Header (output) information
		if (outputFilePath != null) preferences.put("OUTPUT_FILE_PATH", outputFilePath);
	}
}
	

	

