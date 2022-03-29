package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import util.FileSelectionPanel;
import util.FileType;

public class AnalysisPanel extends JPanel implements ActionListener, Observer {
	
	GridBagConstraints constraints = new GridBagConstraints();

	AnalysisController controller;
	AnalysisModel model;
	int panelNumber;
	
	//Buttons
	JButton buttonTarget; // = new JButton ("Target");
	JButton buttonRun = new JButton ("Run");
	JButton buttonSettings = new JButton ("Settings");

	//Automation Labels
	JLabel internalSegmentationLabel = new JLabel("Internal Segmentation:");

	//Holds name of channels for a loaded AnalysisMethod;
	String[] channelLabels;
	
	//Create ComboBox Components
	JComboBox <String[]> comboBoxAnalysisMethod;
	ArrayList <JComboBox> comboBoxChannelSelections = new ArrayList<JComboBox>();
	
	//Holds names of dataSets
	String[] channelList = {"empty"};
	String[] channelNames = {"empty"};
	
	//Create components for holding channels
	int channelNumber; //TODO: register total number of channels that were loaded
	JPanel channelPanel = new JPanel(new GridBagLayout());
	
	//Create Text Field Components
	JTextField textFieldTarget; // = new JTextField("", 25);
	JLabel targetMessage = new JLabel(" "); //TODO: italicize, create output

	//Target selection components
	FileSelectionPanel targetSelection;

	//Loading Status Labels
	JLabel labelInfoRun = new JLabel(" ");
	JLabel labelInfoGenerate = new JLabel(" ");
	
	
	public AnalysisPanel(AnalysisController controller, AnalysisModel model, int panelNumber) {
		channelNumber = 1; //TODO modify for expansion and control by dynamic class
		this.panelNumber = panelNumber;
		this.controller = controller;
		this.model = model;
		initialize();
		model.addObserver(this);
		setLayout(new GridBagLayout());	
		createPanel();
	}

	public void initialize() {
		//Load analysis methods:
		comboBoxAnalysisMethod = new JComboBox(controller.getAnalysisMethodNames());
		
		//Load previous selection:
		comboBoxAnalysisMethod.setSelectedIndex(controller.getAnalysisMethodSelection());
		
		//Load Target file selector object
		targetSelection = new FileSelectionPanel("Target", controller.getTargetFilePath(), this);
		buttonTarget = targetSelection.getButton();
		textFieldTarget = targetSelection.getField();
		targetSelection.addObserver(this);
		channelNames = controller.getChannels();
		updateChannelPanel(); 
		targetSelection.forceTimerUpdate(); //Ensures initial input text attempts to load a file
		
		//Load file-loaded settings
    	updateAnalysisLoaded(0, 0);
    	updateAnalysisLoaded(1, 0);
	}
	
	public void createPanel() {
		
		//Constraint constants
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    constraints.weightx = 1;
	    constraints.weighty = 1;
	    
	    //Adjust fonts for the error message
	    targetMessage.setFont(new Font(targetMessage.getFont().getName(), Font.ITALIC + Font.BOLD, targetMessage.getFont().getSize()));
	    labelInfoRun.setFont(new Font(labelInfoRun.getFont().getName(), Font.ITALIC + Font.BOLD, labelInfoRun.getFont().getSize()));
	    labelInfoGenerate.setFont(new Font(labelInfoGenerate.getFont().getName(), Font.ITALIC + Font.BOLD, labelInfoGenerate.getFont().getSize()));
	    
		//ROW 0
		constraints.gridy = 0;
		  
		//Panel title
        constraints.gridx = 0;
        add(new JLabel("Analysis " + panelNumber), constraints);
        
    	//ROW 1
        constraints.gridy = 1;
     	
        //Add target button
        constraints.gridx = 0;
     	add(buttonTarget, constraints);
     	
     	//Add target textField
     	constraints.gridx = 1;
     	constraints.gridwidth = 1;
     	add(textFieldTarget, constraints);
     	constraints.gridwidth = 1;
     	
     	//Add target response message
     	constraints.gridx = 2;
     	constraints.gridwidth = 5;
     	add(targetMessage, constraints);
     	constraints.gridwidth = 1;
    	
    	//ROW 2
        constraints.gridy = 2;
      
        //add operation label
        constraints.gridx = 0;
        add(new JLabel("Operation:"), constraints);
		
        //Add operation comboBox
    	constraints.gridx = 1;
		constraints.gridwidth = 1;
		add(comboBoxAnalysisMethod, constraints);
		constraints.gridwidth = 1;
        
		//Add overlay checkbox
		constraints.gridx = 3;
		add(buttonSettings, constraints);
		
		//ROW 3
        constraints.gridy = 3;
		
		//Add channel-selection panel
		constraints.gridx = 1;
		add(channelPanel, constraints);
		
		//Internal Segmentation Status 
		constraints.gridx = 3;
		add(buttonRun, constraints);
		
		//ROW 4
        constraints.gridy = 4;
		
		//ROW 5
		constraints.gridy = 5;
		
		//Internal Segmentation Status 
		constraints.gridx = 2;
		constraints.gridwidth = 5;
		add(labelInfoRun, constraints);
		constraints.gridwidth = 1;
		
		//OBSERVERS
		comboBoxAnalysisMethod.addActionListener(this);
		buttonSettings.addActionListener(this);
		buttonRun.addActionListener(this);
	}
	
	public void updateChannelPanel() {
		
		channelNames = controller.getChannels();
		
		channelPanel.removeAll();
		comboBoxChannelSelections.clear();
		
		System.out.println("Channel count is: " + channelNames.length);
		
		GridBagConstraints constraints = new GridBagConstraints();
		int row = 0;
		
		for (int i = 0; i < channelNames.length; i++) {
			comboBoxChannelSelections.add(new JComboBox(channelList));
		}	
		
		for (int i = 0; i < channelNames.length; i++) {
			
			//ROW #
			constraints.gridy = row + i;
			
			//Add the label
			constraints.gridx = 0;
			channelPanel.add(new JLabel(channelNames[i]), constraints); //TODO: Make label specific for the method, ex: "recursive channel", etc
			
			//Add the selection
			constraints.gridx = 1;
			channelPanel.add(comboBoxChannelSelections.get(i), constraints); //TODO: Modify for extensible channels
			constraints.gridwidth = 1;
			
			//Adds an actionListener
			comboBoxChannelSelections.get(i).addActionListener(this);
		}
		channelPanel.repaint();
		channelPanel.revalidate();
	}
		
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == comboBoxAnalysisMethod) {
			controller.setAnalysisMethodSelection(comboBoxAnalysisMethod.getSelectedIndex());
			updateChannelPanel();
			System.out.println("Method selected is " + comboBoxAnalysisMethod.getSelectedIndex());
		}
		
		//for (JComboBox comboBox: comboBoxChannelSelections) {
		for (int i = 0; i < comboBoxChannelSelections.size(); i ++) {
			if (e.getSource() == comboBoxChannelSelections.get(i)) {
				controller.setChannelMethodSelection(i,
						comboBoxChannelSelections.get(i).getSelectedIndex());
			}
		}
		
		if (e.getSource() == buttonSettings) {
			//TODO: get selectedAnalysis DataSets
			// {Name, link, 
			
			
			controller.openAnalysisSettings();
		}
		
		
		if (e.getSource() == buttonRun) {
			controller.runAnalysis();
		}
	}
	
	@Override
	public void update(Observable obs, Object arg) {
		if (obs instanceof FileSelectionPanel) {
			updateTargetFile();
		}	
	}

	public void updateTargetFile() {
		if (targetSelection.getFileType() == FileType.NOT_DIRECTORY) {
			targetMessage.setText("Not a valid directory");
			targetMessage.setForeground(Color.RED);
		}
		
		else if (targetSelection.getFileType() == FileType.NO_FILES) {
			targetMessage.setText("Directory has no files");
			targetMessage.setForeground(Color.RED);
		}
		
		else if (targetSelection.getFileType() == FileType.MULTIPLE_FILES) {
			targetMessage.setText("Directory has more than one file");
			targetMessage.setForeground(Color.RED);
		}
			
		else if (targetSelection.getFileType() == FileType.SINGLE_FILE) {
			targetMessage.setText("Target File Selected");
			targetMessage.setForeground(Color.BLACK);
			controller.setTargetFilePath(targetSelection.getFile().getAbsolutePath());
		}	
		
		controller.setTargetField(textFieldTarget.getText());
		repaint();
		revalidate();
	}
	
	//Setting 0: Unloaded run set.
	//Setting 1: loaded run set. 
	public void updateAnalysisLoaded(int label, int setting) { 
		//Choose external or internal seg label.
		JLabel labelInfo = null;
		if (label == 0) labelInfo = labelInfoRun;
		if (label == 1) labelInfo = labelInfoGenerate;
		
		if (setting == 0) { 
			//TODO: Controller determines generate results. 
			labelInfo.setText(" ");

		}
		//TODO: else disable generate
		
		if (setting == 1) labelInfo.setText("Overlay constructed and data analyzed");
		if (setting == 2) labelInfo.setText("Overlay constructed");
		if (setting == 3) labelInfo.setText("Data Analyzed");
		if (setting == 4) labelInfo.setText("No data generated");
	}
	
	
	
	
	/*
	public String getTargetPath() {
	 return textFieldTarget.getText();
	}
	*/
	
	public void setChannelList(String[] channelList) {
		this.channelList = channelList;
		updateChannelPanel();
	}

	
	



	
}


