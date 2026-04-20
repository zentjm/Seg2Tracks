package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.apache.poi.ss.usermodel.Workbook;

import analysisMethod.AnalysisMethod;
import calculations.Data;
import dataStructure.DataSet;
import dataStructure.Segment;
import ij.ImagePlus;
import util.Seg2TracksClassLoader;

/**
 * Controller for the analysis panel in the Seg2Tracks plugin. Manages the application logic
 * for running analysis on segmented cell images to generate measurement data and results workbooks.
 * Follows MVC pattern by coordinating between AnalysisPanel (view) and AnalysisModel (model).
 */
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
	//ImagePlus overlayImage;
	ArrayList<ImagePlus> overlayImages;
	
	Workbook workbook;
	
	//Overlay Generated Booleans
	boolean overlayExists = false;
	boolean workbookExists = false;
	boolean loadedData = false;
	
	/**
	 * Constructor for AnalysisController. Initializes the analysis panel and loads plugins and settings.
	 *
	 * @param ctrl Reference to the top-level Seg2TracksController
	 * @param model The AnalysisModel holding analysis state for this panel
	 * @param panelNumber The panel index (0 or higher) used to differentiate settings for multiple analysis panels
	 */
	public AnalysisController(Seg2TracksController ctrl, AnalysisModel model, int panelNumber) {
		this.controller = ctrl;
		this.panelNumber = panelNumber;
		this.model = model;
		loadPlugins();
		loadSettings(); //TODO: make settings reset if plugin class is added or removed
		model.setController(this);
		panel = new AnalysisPanel(this, model, panelNumber);
		overlayImages = new ArrayList<ImagePlus>();
	}

	/**
	 * Dynamically loads analysis method plugins from the classpath using Seg2TracksClassLoader.
	 */
	public void loadPlugins() {
		Seg2TracksClassLoader classLoader = new Seg2TracksClassLoader();
		analysisMethods = classLoader.getAnalysisMethods();
	}

	/**
	 * Loads previously saved settings for this analysis panel from Java preferences.
	 * Also initializes the data list from the selected analysis method.
	 */
	public void loadSettings() {
		analysisMethodSelection = preferences.getInt("ANALYSIS_SELECTION" + panelNumber, 0);
		targetFilePath =  preferences.get("TEXT_FIELD_TARGET" + panelNumber,"Insert Target File Location");
		dataList = getAnalysisMethod().getCalculations();
	}

	/**
	 * Saves the current analysis settings to Java preferences for persistence across sessions.
	 */
	public void saveSettings() {
		preferences.putInt("ANALYSIS_SELECTION" + panelNumber, analysisMethodSelection);
		preferences.put("TEXT_FIELD_TARGET" + panelNumber, targetFieldText);
	}
	
	/** Sets the target file path text field value. */
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
	
	/* FOR SINGLE IMAGE
	public void setOverlayImage(ImagePlus overlayImage) {
		if (overlayImage != null) {
			this.overlayImage = overlayImage;
			overlayExists = true; 
			setAnalysisData();
		}
	}
	*/
	
	public void setOverlayImage(ImagePlus overlayImage, boolean override) {
		if (overlayImage != null) {
			if (override) { 
				overlayImages.clear();
				overlayImages.add(overlayImage);
			}
			else {
				overlayImages.add(overlayImage);
			}
			overlayExists = true; 	
		}
		else overlayExists = false; //for deleting data
		setAnalysisData();
	}
	

	public void setWorkbook(Workbook workbook) {
		if (workbook != null) {
			this.workbook = workbook;
			workbookExists = true;
		}
		else workbookExists = false; //for deleting data
		setAnalysisData();
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
	
	public ArrayList<ImagePlus> getOverlayedImages() {
		return overlayImages;
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
	
	public JProgressBar getProgressBar() {
		return controller.getProgressBar();
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
	/**
	 * Stores the DataSets from segmentation panels and updates the channel list displayed in the analysis panel.
	 * Allows Seg2Tracks controller to pass the generated dataSets to this analysis panel for analysis.
	 *
	 * @param dataSets Array of DataSet objects from completed segmentation operations
	 */
	public void setDataSet(DataSet[] dataSets) {
		this.dataSets = dataSets;
		channelList = new String[dataSets.length];
		for (int i = 0; i < dataSets.length; i ++) {
			channelList[i] = dataSets[i].getName();
		}
		panel.setChannelList(channelList);
	}

	/**
	 * Stores which DataSet should be used for a given channel index in the analysis.
	 * Allows the panel to specify which dataSets to pass to the analysis method.
	 *
	 * @param index The channel/index position
	 * @param selection The DataSet selection for that channel
	 */
	public void setChannelMethodSelection(int index, int selection) {
		dataSelections[index] = selection;
	}

	/** Returns the AnalysisPanel view component to the parent Seg2TracksPanel. */
	public AnalysisPanel getPanel() {
		return panel;
	}

	/**
	 * Determines and displays the current analysis data loading status. Updates the panel with flags indicating
	 * whether overlays and workbooks have been generated (using magic numbers that should be enums).
	 */
	public void setAnalysisData() {
		//System.out.println("Setting analysis Data");
		if (overlayExists && workbookExists) panel.updateAnalysisLoaded(0, 1); //TODO: Enums
		if (overlayExists && !workbookExists) panel.updateAnalysisLoaded(0, 2); //TODO: Enums
		if (!overlayExists && workbookExists) panel.updateAnalysisLoaded(0, 3); //TODO: Enums
		if (!overlayExists && !workbookExists) {
			panel.updateAnalysisLoaded(0, 4); //TODO: Enums
			loadedData = false;
		}
		if (overlayExists || workbookExists) loadedData = true;
		//System.out.println("Analysis is loaded");
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
			
			//XXX: Not thread
			//model.runIt();
				
			//Thread
			SwingWorker runAnalysis = runAnalysisThread();
			runAnalysis.execute();

		}
	}
	
	
	public SwingWorker runAnalysisThread() {
		return new SwingWorker<Void, Integer>() {
			@Override
			public Void doInBackground() {	
				model.runIt(); 
				return null;
			}
		};
	}
	
	
	
	
	
	
	/**
	 * Handles action events (though currently unimplemented).
	 *
	 * @param e The ActionEvent (currently unused)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}
	
	
	
	
	
	
	
	
}