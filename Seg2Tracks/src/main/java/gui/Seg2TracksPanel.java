package gui;

	
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ij.process.AutoThresholder.Method;

public class Seg2TracksPanel extends JFrame implements ItemListener, Observer {
	
	//version number
	static String version = "v0.4.1";
	
	//MVC connections
	Seg2TracksController controller;
	Seg2TracksModel model;

	//File Locations
	static File fileLocation = new File(System.getProperty("user.dir") + "/Saves/");
	String directory = System.getProperty("user.dir");
	String startupDir = directory + "/" + "STARTUP.txt";
	String saveDir = directory + "/" + "SAVES.txt";
	
	//User Input Variables
	File input;
	File loadFile;
	boolean load = false;
	boolean manual = false;
	boolean automatic = false;
	boolean externalDependence = false;
	int internalSegment = 0;
	int externalSegment = 0;
	int differenceSegment = 0;
	int linkageMechanism = 0;
	
	//Split Directories
	File [] inputs;
	
	//Condition Flags
	boolean run = false;
	
	//JProgressBar
	JProgressBar progressbar;
	
	//Create CheckBox Components
	JCheckBox checkBoxExternalDependence = new JCheckBox("External Dependence"); //XXX: Only currently necessary for manual external analysis. 
	
	//Create Text Field Components
	final JTextField textFieldInput = new JTextField("Insert Input File Location", 25);
	
	//Alerts Label
	JLabel alert = new JLabel();
	
	//Create Master Panel
	JPanel masterPanel = new JPanel();
	BoxLayout boxLayout = new BoxLayout(masterPanel, BoxLayout.Y_AXIS);
	
	//Create Panel
	GridBagConstraints constraints;
	HeadPanel headPanel;
	FloorPanel floorPanel;
	
	//Operation
	ArrayList<OperationController> operationControllerList;
	
	//Analysis
	ArrayList<AnalysisController> analysisControllerList;

	public Seg2TracksPanel(Seg2TracksController controller, Seg2TracksModel model, HeadPanel headPanel, FloorPanel floorPanel) {
       
		super("Seg2Tracks " + version);
		this.controller = controller;
		this.model = model;
		this.headPanel = headPanel;
		this.floorPanel = floorPanel;
	}
	
	public void createView () {

		operationControllerList = controller.getOperationList();
		analysisControllerList = controller.getAnalysisList();
		masterPanel.setLayout(boxLayout);
		
		//Sets JFrame Properties //TODO: Work out ideal frame size
		setMinimumSize(new Dimension(850,250));
		//setPreferredSize(new Dimension(850,250));
		
		//Handles frame closing
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				JFrame frame = (JFrame)e.getSource();
				
				int result = JOptionPane.showConfirmDialog(
					frame,
					"Are you sure you want to exit Seg2Tracks?",
					"Exit Seg2Tracks",
					JOptionPane.YES_NO_OPTION);
				
				if (result == JOptionPane.YES_OPTION) {
					controller.exitProgram();
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				}
				
				if (result == JOptionPane.NO_OPTION) {
					frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
			}
		});
		
	
		//Adds panels;
		masterPanel.add(headPanel);
		
		for (int i=0; i < operationControllerList.size(); i ++) {
			masterPanel.add(operationControllerList.get(i).getPanel(), i + 1);
		}
		
		if (operationControllerList.size() < 2) floorPanel.allowPanelRemoval(false);
		masterPanel.add(floorPanel);
		add(masterPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

	public void switchToAnalysis() {
		masterPanel.removeAll();
		headPanel.switchToAnalysis();
		masterPanel.add(headPanel);
		
		//Add analysis panels
		for (int i=0; i < analysisControllerList.size(); i ++) {
			masterPanel.add(analysisControllerList.get(i).getPanel());
		}
		
		floorPanel.switchToAnalysis();
		masterPanel.add(floorPanel);
	
		pack();
		repaint();
		revalidate();
	}
	
	public void switchToOperation() {
		masterPanel.removeAll();
		operationControllerList = controller.getOperationList();
		headPanel.switchToOperation();
		masterPanel.add(headPanel);
		
		//Add operation panels
		for (int i=0; i < operationControllerList.size(); i ++) {
			masterPanel.add(operationControllerList.get(i).getPanel(), i + 1);
		}
		
		if (operationControllerList.size() < 2) floorPanel.allowPanelRemoval(false);
		else floorPanel.allowPanelRemoval(true);
		floorPanel.switchToOperation();
		masterPanel.add(floorPanel);
	
		pack();
		repaint();
		revalidate();
	}
	
	public void enableAnalysis(boolean enable) {
		floorPanel.allowAnalysisButton(enable);
	}
	
	public void enableResults(boolean enable) {
		floorPanel.allowResultsButton(enable);
	}
	
	public void loadHeaderPanel() {
		//help menu
	}
	
	public File[] getInputs () {
		return inputs;	
	}
	
	public boolean getLoad() {
		return load;
	}
	
	public File getLoadFile() {
		return loadFile;
	}
	
	public boolean getExternalDependence() {
		return externalDependence;
	}
	
	public boolean getRunManual () {
		return manual;
	}
	
	public int getInternalSegment () {
		return internalSegment;
	}
	
	public int getExternalSegment () {
		return externalSegment;
	}
	
	public int getDifferenceSegment () {
		return differenceSegment;
	}

	public JProgressBar getProgressBar() {
		return floorPanel.getProgressBar();
	}
	
	public int linkageMechanism () {
		return linkageMechanism;
	}
	
	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		
	}

	

}


