package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Random;
import java.util.prefs.Preferences;

import org.apache.poi.ss.usermodel.Workbook;

import analysisMethod.AnalysisMethod;
import dataStructure.DataSet;
import dataStructure.Segment;
import externalSegmentation.ExternalSegmentation;
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
import util.Seg2TracksClassLoader;

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
	
	//dataRerun
	boolean override;
	
	public AnalysisModel(int panelNumber) {
		this.panelNumber = panelNumber;
		override = false;
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
	
	public void runIt() {
		
		//Opens the targetImage file
		targetStack = IJ.openVirtual(controller.getTargetFilePath()).getImageStack();
		targetPlus = new ImagePlus("Testing", targetStack);
		
		//Initiates analysis method
		AnalysisMethod analysis = controller.getAnalysisMethod();
		
		//Initializes analysis with target image and segmented dataSets
		analysis.initialize(targetPlus, dataSets, override);
		
		//Runs analysis
		analysis.analyze();
		
		//Generates overlay //TODO: Allow multiple overlays
		ImagePlus overlayPlus = analysis.getOverlay();
		
		//Generates workbook //TODO: Multiple Workbook Tabs
		Workbook workbook = analysis.getWorkbook();
	
		//Returns data to controller
		controller.setOverlayImage(overlayPlus);
		controller.setWorkbook(workbook);
	}
		
	
	public void setOverride(boolean override) {
		this.override = override;
	}
	
	public void updateModel() {
		setChanged();
		notifyObservers();
		System.out.println("Analysis Model Updated");
	}
	

	
	

}
