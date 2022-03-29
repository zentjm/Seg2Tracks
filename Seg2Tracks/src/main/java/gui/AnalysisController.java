package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.apache.poi.ss.usermodel.Workbook;

import analysisMethod.AnalysisMethod;
import calculations.Data;
import dataStructure.DataSet;
import dataStructure.Segment;
import ij.ImagePlus;
import util.Seg2TracksClassLoader;

public class AnalysisController implements ActionListener {
	Seg2TracksController controller;
	AnalysisPanel panel;
	AnalysisModel model;
	int panelNumber;
	
	//java preferences loading directory
	Preferences preferences = Preferences.userRoot().node("/seg2tracks");
	
	//Plugin Analysis Methods
	AnalysisMethod [] analysisMethods;
	
	//Loaded and Saved inputs
	int channelCount;
	int analysisMethodSelection;
	String targetFieldText;
	boolean generateOverlay;
	boolean generateExcelData;
	int[] dataSelections;
	DataSet[] dataSets;
	
	//Others
	String targetFilePath;
	int channelSelection;
	String [] channelList;
	Data[] dataList;
	
	//Outputs
	ImagePlus overlayImage;
	Workbook workbook;
	
	//Overlay Generated Booleans
	boolean overlayExists = false;
	boolean workbookExists = false;
	boolean loadedData = false;
	
	
	//Constructor
	public AnalysisController(Seg2TracksController ctrl, AnalysisModel model, int panelNumber) {
		this.controller = ctrl;
		this.panelNumber = panelNumber;
		this.model = model;
		loadPlugins();
		loadSettings(); //TODO: make settings reset if plugin class is added or removed
		model.setController(this);
		panel = new AnalysisPanel(this, model, panelNumber);
	}
	
	public void loadPlugins() {
		Seg2TracksClassLoader classLoader = new Seg2TracksClassLoader();
		analysisMethods = classLoader.getAnalysisMethods();	
	}
	
	public void loadSettings() {
		analysisMethodSelection = preferences.getInt("ANALYSIS_SELECTION" + panelNumber, 0);
		targetFilePath =  preferences.get("TEXT_FIELD_TARGET" + panelNumber,"Insert Target File Location");
		dataList = getAnalysisMethod().getCalculations();
	}
	
	public void saveSettings() {
		preferences.putInt("ANALYSIS_SELECTION" + panelNumber, analysisMethodSelection);
		preferences.put("TEXT_FIELD_TARGET" + panelNumber, targetFieldText);
	}
	
	//setter methods
	public void setTargetField(String targetFieldText) {
		this.targetFieldText = targetFieldText;
	}
	
	
	public void setAnalysisMethodSelection(int analysisMethodSelection) {
		this.analysisMethodSelection = analysisMethodSelection;
		dataList = getAnalysisMethod().getCalculations();
	}
	
	/*//USED>?
	public void setChannelSelections(int analysisMethodSelection) {
		this.analysisMethodSelection = analysisMethodSelection;
	}
	*/
	
	public void generateOverlay (boolean generateOverlay) {
		this.generateOverlay = generateOverlay;
	}
	
	public void generateExcelData (boolean generateExcelData) {
		this.generateExcelData = generateExcelData;
	}
	
	public void setOverlayImage(ImagePlus overlayImage) {
		if (overlayImage != null) {
			this.overlayImage = overlayImage;
			overlayExists = true; 
			setAnalysisData();
		}
	}
	
	public void setWorkbook(Workbook workbook) {
		if (workbook != null) {
			this.workbook = workbook;
			workbookExists = true;
			setAnalysisData();
		}	
	}
	
	//getter methods
	public String getTargetField() {
		return targetFieldText;
	}
	
	public int getAnalysisMethodSelection() {
		return analysisMethodSelection;
	}
	
	public boolean getOverlay() {
		return generateOverlay;
	}

	public boolean getExcelData() {
		return generateExcelData;
	}
	
	public String[] getChannels() {
		dataSelections = new int[analysisMethods[analysisMethodSelection].getChannels().length];
		return analysisMethods[analysisMethodSelection].getChannels();
	}
	
	public ImagePlus getOverlayedImage() {
		return overlayImage;
	}
	
	public Workbook getWorkbook() {
		return workbook;
	}
	
	//Gets Analysis names for panel display
	public String[] getAnalysisMethodNames() {
		String [] names = new String [analysisMethods.length];
		for (int i = 0; i < names.length; i ++) {
			names[i] = analysisMethods[i].toString();
		}
		return names;
	}
	
	//Return selected Analysis methods to the AnalysisModel
	public AnalysisMethod getAnalysisMethod() {
		return analysisMethods[analysisMethodSelection];
	}

	public String getTargetFilePath() {
		return targetFilePath;
	}
	
	public void setTargetFilePath(String targetFilePath) {
		this.targetFilePath = targetFilePath;
	}
	
	//TODO: This seems like extremely sloppy naming - probably need to allow user to define name
	//Allows Seg2Tracks controller to port the generated dataSets
	public void setDataSet(DataSet[] dataSets) {
		this.dataSets = dataSets;
		channelList = new String[dataSets.length];
		for (int i = 0; i < dataSets.length; i ++) {
			channelList[i] = dataSets[i].getName();
		}
		panel.setChannelList(channelList);
	}
	
	//Allows panel to set which dataSets to pass
	public void setChannelMethodSelection(int index, int selection) {
		dataSelections[index] = selection;
	}
	
	//retrieves the selected dataSet for the model.
	public AnalysisPanel getPanel() {
		return panel;
	}
	
	//Determines if the analysis is loaded. 
	public void setAnalysisData() {
		if (overlayExists && workbookExists) panel.updateAnalysisLoaded(0, 1); //TODO: Enums
		if (overlayExists && !workbookExists) panel.updateAnalysisLoaded(0, 2); //TODO: Enums
		if (!overlayExists && workbookExists) panel.updateAnalysisLoaded(0, 3); //TODO: Enums
		if (!overlayExists && !workbookExists) {
			panel.updateAnalysisLoaded(0, 4); //TODO: Enums
			loadedData = false;
		}
		if (overlayExists || workbookExists) loadedData = true;
		controller.updateAnalysisLoaded();
	}
	
	//indicates if data has been loaded
	public boolean isDataLoaded () {
		return loadedData;
	}
	
	public void openAnalysisSettings() {
		AnalysisSettings settings = new AnalysisSettings(dataList);
	}
	
	public void setAnalysisSettings() {
		//TODO: this allows the analysisSettings menu to set the settings. 
	}
	
	
	//TODO: disable button if non-valid targetFilePath. 
	public void runAnalysis() {
		
		//determines what to do with repeat analysis run
		if(loadedData) {
			//Checks user preference
			int choice = 2;
			Object[] options = {"Override", "Add", "Cancel"};
			choice = JOptionPane.showOptionDialog(null, "Override or add new analysis to current results table?", "Rerun Analysis",
			JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
			null, options, options[2]);
			
			if (choice == 0) model.setOverride(true);
			if (choice == 1) model.setOverride(false);
			if (choice == 2) return;
		}
		
	
		if (targetFilePath != null) {
			DataSet[] passedDataSets = new DataSet[dataSelections.length];
			for (int i = 0; i < passedDataSets.length; i ++) {
				passedDataSets[i] = dataSets[dataSelections[i]];
			}
			model.setDataSet(passedDataSets);
			model.runIt();
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	
	
	
	
}