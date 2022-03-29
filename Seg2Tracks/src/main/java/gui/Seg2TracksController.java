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
import dataStructure.Segment;
import ij.ImagePlus;
import ij.io.FileSaver;

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
	
	ImagePlus[] imageOverlays;
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
		System.out.println("Created view");
		analysisButtonEnabled = false;
	}
	
	//Load operation panels
	public void loadOperationControllers() {
		operationControllerList = new ArrayList<OperationController>();
		operationModelList = model.getOperationModels();
		for (int i = 0; i < operationModelList.size(); i ++) {
			operationControllerList.add(new OperationController(this, operationModelList.get(i), operationModelList.get(i).getPanelNumber()));
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
	
	//Open the help menu //TODO: load the help menu with information
	public void openHelpMenu() {
		HelpMenuPanel panel = new HelpMenuPanel();	
	}
	
	//Retrieve all information from the operation panels and switch to analysis view
	public void switchToAnalysis() {
		//Collect all dataSets from operations panels in an array
		dataSets = new DataSet[operationControllerList.size()]; //New array for holding dataSets
		for(int i = 0; i < operationControllerList.size(); i++) {
			operationControllerList.get(i).saveSettings(); //save the settings
			dataSets[i] = operationControllerList.get(i).getDataSet();
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
		OperationController tempContr = new OperationController(this, tempMod, tempMod.getPanelNumber());
		operationModelList = model.getOperationModels();
		operationControllerList.add(tempContr);
		view.switchToOperation();
	}
	
	public void removeOperationPanel() {
		model.removeOperationModel();
		operationModelList = model.getOperationModels();
		operationControllerList.remove(operationControllerList.size()-1);
		view.switchToOperation();
		allSegmentationLoaded(); //Fixes panel removal bug. 
	}
	
	public void exportResults() {
		//Collect all analysis outputs from analysis panels
		imageOverlays = new ImagePlus[analysisControllerList.size()];
		workbooks = new Workbook[analysisControllerList.size()];
		for(int i = 0; i < analysisControllerList.size(); i++) {
			analysisControllerList.get(i).saveSettings(); //save the settings
			imageOverlays[i] = analysisControllerList.get(i).getOverlayedImage();
			workbooks[i] = analysisControllerList.get(i).getWorkbook();
		}
		
		//Get output directory
		//String outputDirectory = headPanel.getOutputPath();
		
		//Save data
		for(int i = 0; i < analysisControllerList.size(); i++) {

			//Save ImagePlus with overlay
			try {
				//new FileSaver(imageOverlays[i]).saveAsTiff(outputDirectory + File.separator + "OverlayedImage_" + i + ".tif");
				new FileSaver(imageOverlays[i]).saveAsTiff(outputFilePath + File.separator + "OverlayedImage_" + i + ".tif");
			} 
			catch (Exception e) {
				System.out.println("Failed to save image overlay file: " + i);
				e.printStackTrace();
			}
				
			//Save Workbook with overlay //TODO: Ability to add to workbook
			try {
				String fileName = outputFilePath + File.separator + "ResultsOutput_" + i + ".xlsx";
				File file = new File(fileName);
				FileOutputStream outputStream = new FileOutputStream(fileName);
				workbooks[i].write(outputStream);
				//workbooks[i].close();	
			}
			
			catch (Exception e) {
				System.out.println("Failed to save results file: " + i);
				e.printStackTrace();
			}		
		}	
	}
	
	//TODO: does this do anything?
	public DataSet[] getDataSets() {
		return dataSets;
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
		for (AnalysisController ctrl: analysisControllerList) {

			if (!ctrl.isDataLoaded()) {
				view.enableResults(false);
				return;
			}
		}
		view.enableResults(true);
	}
	
	
	
	
	
	//For making invisible when using manual segmentation
	public void setViewActive(boolean enabled) {
		view.setVisible(enabled);
	}
	
	//Save data when exiting program. 
	public void exitProgram() {
		System.out.println("Exited Seg2Tracks");
		
		//Save Seg2TracksModel information (#panels)
		model.saveSettings();
		
		//Save all OperationModel information
		for (OperationController o: operationControllerList) {
			o.saveSettings();
		}
		
		//Save all AnalysisModel information
		for (AnalysisController a: analysisControllerList) {
			a.saveSettings();
		}
		
		//Save Header (output) information
		if (outputFilePath != null)preferences.put("OUTPUT_FILE_PATH", outputFilePath);
	}
}
	

	

