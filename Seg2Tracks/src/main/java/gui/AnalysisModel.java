package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.JProgressBar;

import org.apache.poi.ss.usermodel.Workbook;

import analysisMethod.AnalysisMethod;
import dataStructure.DataSet;
import dataStructure.ResultWorkbook;
import dataStructure.Segment;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import linkage.Linkage;
import sarn.Sarn;
import util.Seg2TracksClassLoader;

/**
 * Model for the analysis panel. Manages the execution of analysis methods on segmented image data
 * and generation of measurement results workbooks and overlay images. Uses the Observable pattern
 * to notify listeners of state changes.
 */
public class AnalysisModel extends Observable {
	int panelNumber;
	
	boolean generateOverlay;
	boolean generateExcelData;
	
	//File target;
	String targetFilePath;
	int channelCount;
	AnalysisController controller;
	
	//Target file
	ImagePlus targetPlus;
	ImageStack targetStack;
	DataSet[] dataSets;
	
	//Component loading
	JProgressBar progressBar;
	int progress = -1;
	
	//dataRerun
	boolean override = true;
	boolean addResults;
	
	//holds result workbook
	ResultWorkbook workbook;
	
	public AnalysisModel(int panelNumber) {
		this.panelNumber = panelNumber;
		override = true;
		initialize();
	}

	public void initialize() {
	}
	
	public void setController(AnalysisController controller) {
		this.controller = controller;
	}
	
	public int getPanelNumber() {
		return panelNumber;
	}
	
	//Used by Seg2TracksController to set the data when switchToAnalysis is called
	public void setDataSet(DataSet[] dataSets) {
		this.dataSets = dataSets;
	}
	
	/**
	 * Executes the analysis workflow: loads the target image, initializes the analysis method,
	 * runs the analysis, generates results (workbook and overlay), and reports completion.
	 * Either overrides previous results or appends new results depending on the override flag.
	 */
	public void runIt() {

		// Access the progress bar from the main controller
		progressBar = controller.getProgressBar();

		// Opens the target image file (virtually to save memory)
		targetStack = IJ.openVirtual(controller.getTargetFilePath()).getImageStack();
		targetPlus = new ImagePlus("methodName", targetStack);

		// Initiates analysis method
		AnalysisMethod analysis = controller.getAnalysisMethod();

		// Initializes analysis with target image and segmented dataSets
		if (override) analysis.initialize(targetPlus, dataSets, progressBar);
		else analysis.initialize(targetPlus, dataSets, progressBar, workbook);

		// Runs analysis
		analysis.analyze();

		// Generates workbook
		workbook = analysis.getWorkbook();

		// Generates overlay image with analysis results
		ImagePlus overlayPlus = analysis.getOverlay();

		// Returns data to controller
		controller.setOverlayImage(overlayPlus, override);
		controller.setWorkbook(workbook.getWorkbook());

		// Update progress bar to completion
		progressBar.setValue(progressBar.getMaximum());
		progressBar.setString("Analysis Complete");
	}
		
	public void setOverride(boolean override) {
		this.override = override;
	}
	
	
	public void updateModel() {
		setChanged();
		notifyObservers();
		//System.out.println("Analysis Model Updated");
	}
	

	
	

}
