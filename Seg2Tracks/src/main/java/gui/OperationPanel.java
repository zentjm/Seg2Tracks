package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import util.FileSelectionPanel;
import util.FileType;

/**
 * View component for a single segmentation operation panel. Displays UI controls for selecting
 * input files, segmentation algorithms (external, internal, linkage), configuring parameters,
 * running operations, and managing DataSet I/O (load/save). Supports subsegmentation mode for
 * recursive workflows. Updates status labels to reflect operation progress and completion.
 */
public class OperationPanel extends JPanel implements ActionListener, Observer {
	
	GridBagConstraints constraints = new GridBagConstraints();
	
	//User Input Variables
	File input;
	File loadFile;
	boolean externalDependence = false;
	int internalSegment = 0;
	int externalSegment = 0;
	int differenceSegment = 0;
	int linkageMechanism = 0;
	
	//Condition Flags
	boolean run = false;
	boolean typing = false;
	boolean internalButtonRunReady =  true;
	boolean externalButtonRunReady =  true;
	
	//IO Data Buttons
	JButton buttonLoad = new JButton ("Load");
	JButton buttonSave = new JButton ("Save");
	
	//Internal Segmentation Buttons
	JButton buttonSettingsInternal = new JButton ("Settings");
	JButton buttonRunInternal = new JButton ("Run");
	JButton buttonModifyInternal = new JButton ("Preview");
	
	//External Segmentation Buttons
	JButton buttonSettingsExternal = new JButton ("Settings");
	JButton buttonRunExternal = new JButton ("Run");
	JButton buttonModifyExternal = new JButton ("Manually Edit");
	
	//Linkage Buttons
	JButton buttonSettingsLinkage = new JButton ("Settings");
	JButton buttonCalibrateChannel = new JButton ("Object ID Settings");
	
	//Other Buttons
	JButton buttonInput = new JButton("Input");

	//Automation Labels
	JLabel internalSegmentationLabel = new JLabel("Segmentation:");
	JLabel externalSegmentationLabel = new JLabel("SARN Preprocessing:");
	JLabel linkageMechanismLabel = new JLabel("Linkage Mechanism:");
	
	//Loading Status Labels
	JLabel labelStatus1 = new JLabel("Status:");
	JLabel labelInfo1 = new JLabel(" ");
	JLabel labelStatus2 = new JLabel("Status:");
	JLabel labelInfo2 = new JLabel(" ");
	
	//Subsegmentation options
	boolean subsegmentOption = false;
	boolean subsegmentationLocked = false; // true while a completed run is committed
	
	//status messages
	JLabel inputMessage = new JLabel(" "); //TODO: italicize, create output

	//Create ComboBox Components
	JComboBox <String[]> comboBoxInternalSegment;
	JComboBox <String[]> comboBoxExternalSegment;
	JComboBox <String[]> comboBoxLinkage;
	JComboBox<String> comboBoxSubsegmentation;
	
	//Create CheckBox Components
	JCheckBox checkBoxExternalDependence = new JCheckBox("External Dependence"); //XXX: Only currently necessary for manual external analysis. 
	JCheckBox checkBoxSubsegmentation;  //XXX: Determines if this anaylsis is a recursive model
	
	//Create Text Field Components
	JTextField textFieldInput = new JTextField("", 25);
	JTextField dataSetName = new JTextField("", 10);
	
	//Alerts Label
	JLabel alert = new JLabel();

	//panel number
	int panelNumber;
	
	//MVC components
	OperationController controller;
	OperationModel model;

	//Input selection components
	FileSelectionPanel inputSelection;
	
	public OperationPanel(OperationController controller, OperationModel model, int panelNumber)  {
		this.panelNumber = panelNumber;
		this.controller = controller;
		this.model = model;
		//if (panelNumber > 0) subsegmentOption = true;
		subsegmentOption = controller.getSubsegmentation();
		initialize();
		setLayout(new GridBagLayout());	
		createPanel();
	}
	
	public void initialize() {
       
		//Load linkage and segmentation methods:
        comboBoxLinkage = new JComboBox(controller.getLinkageMethodNames());
    	comboBoxInternalSegment = new JComboBox(controller.getInternalSegmentationMethodNames());
    	comboBoxExternalSegment = new JComboBox(controller.getExternalSegmentationMethodNames());
    	
    	//Load previous selections
    	comboBoxLinkage.setSelectedIndex(controller.getLinkageSelection());
    	comboBoxInternalSegment.setSelectedIndex(controller.getInternalSegmentationSelection());
    	comboBoxExternalSegment.setSelectedIndex(controller.getExternalSegmentationSelection());
    	
    	//load previous dataSet names
    	dataSetName.setText(controller.getDataSetName());
    	
    	//Load Input file selector object
    	inputSelection = new FileSelectionPanel("Input", controller.getInputField(), this);
    	buttonInput = inputSelection.getButton();
    	textFieldInput = inputSelection.getField();
    	inputSelection.addObserver(this);
    	inputSelection.forceTimerUpdate(); //Ensures initial input text attempts to load a file
    	
    	// Subsegmentation option — always present on non-first panels.
    	// Greyed out until at least one dataset exists; combobox only enabled when checkbox is ticked.
    	if (panelNumber > 0) {
    		String[] dataSetNames = controller.getDataSetNames();
    		boolean hasDatasets = (dataSetNames != null && dataSetNames.length > 0);
    		checkBoxSubsegmentation = new JCheckBox("Subsegmentation", false);
    		checkBoxSubsegmentation.setEnabled(hasDatasets);
    		comboBoxSubsegmentation = new JComboBox<String>(); // always create; populated below if data exists
    		if (hasDatasets) {
    			for (String name : dataSetNames) comboBoxSubsegmentation.addItem(name);
    		}
    		comboBoxSubsegmentation.setEnabled(false); // only enabled when checkbox is ticked
    	}
    	
    	//Load file-loaded settings
    	updateSegmentationLoaded(0, 0);
    	updateSegmentationLoaded(1, 0);
	}

	//Arranged from top-to-bottom, right-to-left
	public void createPanel() {
		
		//Constraint constants
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    
	    //Adjust fonts
	    inputMessage.setFont(new Font(inputMessage.getFont().getName(), Font.ITALIC + Font.BOLD, inputMessage.getFont().getSize()));
	    labelInfo1.setFont(new Font(inputMessage.getFont().getName(), Font.ITALIC + Font.BOLD, inputMessage.getFont().getSize()));
	    labelInfo2.setFont(new Font(inputMessage.getFont().getName(), Font.ITALIC + Font.BOLD, inputMessage.getFont().getSize()));
	    
		//ROW 0
		constraints.gridy = 0;
		  
		//Panel title
        constraints.gridx = 0;
        add(new JLabel("DataSet Name: "), constraints);
        
        //Panel Name
        constraints.gridx = 1;
        add(dataSetName, constraints);
                 
  	  //Subsegmentation options — always present on non-first panels (greyed out until datasets exist)
        if (panelNumber > 0) {
        	constraints.gridx = 2;
        	add(checkBoxSubsegmentation, constraints);
        	constraints.gridx = 3;
        	add(comboBoxSubsegmentation, constraints);
        }
        
    	//ROW 1
        constraints.gridy = 1;
		
        //Input button
     	constraints.gridx = 0;
     	add(buttonInput, constraints);
		  
     	//Input Text Field
		constraints.gridx = 1;
		constraints.gridwidth = 8;
		add(textFieldInput, constraints);
		constraints.gridwidth = 1;
		
		//Input message box
		constraints.gridx = 9;
		constraints.gridwidth = 5;
		add(inputMessage, constraints); //TODO: italicize, create output
		constraints.gridwidth = 1;
		
		//ROW 2
        constraints.gridy = 2;
		
        //Linkage Label
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        add(new JLabel("Linkage Mechanism:"), constraints);
        constraints.gridwidth = 1;
        
		//Method selection
		constraints.gridx = 2;
		constraints.gridwidth = 3;
		add(comboBoxLinkage, constraints);
		constraints.gridwidth = 1;
        
		//Linkage Method Settings button
		constraints.gridx = 5;
		add(buttonSettingsLinkage, constraints);
		
		//Channel Calibration Button
		constraints.gridx = 6;
		add(buttonCalibrateChannel, constraints);
		
		//Load DataSet Button
		constraints.gridx = 7;
		add(buttonLoad, constraints);
		
		//Save DataSet Button
		constraints.gridx = 8;
		add(buttonSave, constraints);
     
        //ROW 3
		constraints.gridy = 3;
		
		//Internal Segmentation Label
		constraints.gridx = 0;
	    constraints.gridwidth = 2;
	    add(new JLabel("Segmentation:"), constraints);
	    constraints.gridwidth = 1;
		
		//Internal Segmentation ComboBox
		constraints.gridx = 2;
		constraints.gridwidth = 3;
		add(comboBoxInternalSegment, constraints);
		constraints.gridwidth = 1;
		
		//Internal Segmentation Settings Button
		constraints.gridx = 5;
		add(buttonSettingsInternal, constraints);
		
		//Internal Segmentation Run Button
		constraints.gridx = 6;
		add(buttonRunInternal, constraints);
		buttonRunInternal.setEnabled(!controller.isExternallyDependent());
		
		//Internal Segmentation Manual Modify Button
		constraints.gridx = 7;
		add(buttonModifyInternal, constraints);
		buttonModifyInternal.setEnabled(false);
		
		//Internal Segmentation Status 
		constraints.gridx = 8;
		add(labelInfo1, constraints);
		
		//ROW 4
		constraints.gridy = 4;
	
		//External Segmentation Label
		constraints.gridx = 0;
	    constraints.gridwidth = 2;
	    add(new JLabel("SARN Preprocessing:"), constraints);
	    constraints.gridwidth = 1;
	    
	  	//External Segmentation ComboBox
		constraints.gridx = 2;
		constraints.gridwidth = 3;
		add(comboBoxExternalSegment, constraints);
		constraints.gridwidth = 1;
		
		//External Segmentation Settings Button
		constraints.gridx = 5;
		add(buttonSettingsExternal, constraints);
		
		//External Segmentation Run Button
		constraints.gridx = 6;
		add(buttonRunExternal, constraints);
		
		//External Segmentation Manual Modify Button
		constraints.gridx = 7;
		add(buttonModifyExternal, constraints);
		
		//Segmentation Status Status
		constraints.gridx = 8;
		add(labelInfo2, constraints);
				
		//OBSERVERS:
		
	 	//Adds Button ActionListeners
		buttonSettingsInternal.addActionListener(this);
		buttonSettingsExternal.addActionListener(this);
		buttonSettingsLinkage.addActionListener(this);
		buttonRunInternal.addActionListener(this);
		buttonRunExternal.addActionListener(this);
		buttonModifyInternal.addActionListener(this);
		buttonModifyExternal.addActionListener(this);
		buttonLoad.addActionListener(this);
		buttonSave.addActionListener(this);
		buttonCalibrateChannel.addActionListener(this);
    	
		//Add ComboBox Action Listeners
    	comboBoxInternalSegment.addActionListener(this);
    	comboBoxExternalSegment.addActionListener(this);
		comboBoxLinkage.addActionListener(this);

		//Subsegmentation listeners (only if widgets exist)
		if (checkBoxSubsegmentation != null) {
			checkBoxSubsegmentation.addActionListener(this);
			comboBoxSubsegmentation.addActionListener(this);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == buttonCalibrateChannel) {
			controller.calibrateChannel();
		}
		
		if (e.getSource() == buttonSettingsLinkage) {
			controller.linkageSettings();
		}
		
		if (e.getSource() == buttonSettingsExternal) {
			controller.externalSegmentationSettings();
		}
		
		if (e.getSource() == buttonSettingsInternal) {
			controller.internalSegmentationSettings();
		}
		
		if (e.getSource() == buttonRunInternal) {
			controller.runInternalSegmentation();	
		}
		
		if (e.getSource() == buttonRunExternal) {
			controller.runExternalSegmentation();
		}
		
		if (e.getSource() == buttonModifyInternal) {
			controller.runModifyInternal();
		}
		
		if (e.getSource() == buttonModifyExternal) {
			controller.runModifyExternal();
		}
		
		if (e.getSource() == buttonLoad) {
			controller.loadDataSet();
		}
	
		if (e.getSource() == buttonSave) {
			controller.saveExternalSegmentation();
		}
		
		//ComboBoxes
		if (e.getSource() == comboBoxLinkage) {
			controller.setComboBoxLinkage(comboBoxLinkage.getSelectedIndex());
		}
		
		if (e.getSource() == comboBoxInternalSegment) {
			controller.setComboBoxInternalSegmentation(comboBoxInternalSegment.getSelectedIndex());
			
			if (controller.isExternallyDependent()) {
				buttonRunInternal.setEnabled(controller.externalSegmentationExists());
			}
			else {
				buttonRunInternal.setEnabled(true);
			}
		}
		
		if (e.getSource() == comboBoxExternalSegment) {
			controller.setComboBoxExternalSegmentation(comboBoxExternalSegment.getSelectedIndex());
		}

		// Subsegmentation checkbox: enable/disable the dataset selection combobox
		if (checkBoxSubsegmentation != null && e.getSource() == checkBoxSubsegmentation) {
			boolean checked = checkBoxSubsegmentation.isSelected();
			comboBoxSubsegmentation.setEnabled(checked);
			controller.setSubsegmentationEnabled(checked);
		}

		// Subsegmentation combobox: notify controller which parent dataset was selected
		if (comboBoxSubsegmentation != null && e.getSource() == comboBoxSubsegmentation) {
			controller.setSubsegmentationSelection(comboBoxSubsegmentation.getSelectedIndex());
		}
	}

	public boolean dialogAlert(String alert) {
		 int choice =  JOptionPane.showConfirmDialog((Component) null, alert, "alert", JOptionPane.OK_CANCEL_OPTION);
		 if (choice == JOptionPane.OK_OPTION) return true;
		 return false;
	}
	
	@Override
	public void update(Observable obs, Object arg) {
		if (obs instanceof FileSelectionPanel) {
			updateInputFile();
		}
	}
	
	//TODO: Split into two methods
	//TODO: terms are confusing in this whole thing. Needs some work. 
	//TODO: lock analysis until something is loaded
	//TODO: Enums for labels (0: externalSegmentation, 1: internalSegmentation)
	//TODO: Enums for commands (0: no file loaded, 1: run file loaded, 2: save file loaded)
	public void updateSegmentationLoaded(int label, int setting) {
		
		//Internal segmentation
		if (label == 1) {
			if (setting == 0) {
				buttonSave.setEnabled(false); //TODO: should be able to save if only one of the segmentations is cleared
				labelInfo1.setText(" ");
				buttonRunInternal.setText("Run");
				buttonModifyInternal.setEnabled(false);
				internalButtonRunReady =  true;
			}
			if (setting == 1) {
				buttonSave.setEnabled(true);
				labelInfo1.setText("Run Data Loaded");
				buttonRunInternal.setText("Clear");
				buttonModifyInternal.setEnabled(true);
				internalButtonRunReady =  false;
			}
			if (setting == 2) {
				buttonSave.setEnabled(true);
				labelInfo1.setText("Save Data Loaded");
				buttonRunInternal.setText("Clear");
				buttonModifyInternal.setEnabled(true);
				internalButtonRunReady =  false;
			}
			if (setting == 3) {
				buttonSave.setEnabled(true);
				labelInfo1.setText("Modified File Loaded");
				buttonRunInternal.setText("Clear");
				buttonModifyInternal.setEnabled(true);
				internalButtonRunReady =  false;
			}

			//TODO: make a cancel button work
			if (setting == 4) {
				buttonRunInternal.setText("Cancel");
				buttonModifyInternal.setEnabled(false);
			}

		}
		
		
		//External segmentation
		if (label == 0) {
			if (setting == 0) {
				buttonSave.setEnabled(false); //TODO: should be able to save if only one of the segmentations is cleared
				labelInfo2.setText(" ");
				buttonRunExternal.setText("Run");
				externalButtonRunReady =  true;
			}
			if (setting == 1) {
				buttonSave.setEnabled(true);
				labelInfo2.setText("Run Data Loaded");
				buttonRunExternal.setText("Clear");
				externalButtonRunReady =  false;
			}
			if (setting == 2) {
				buttonSave.setEnabled(true);
				labelInfo2.setText("Save Data Loaded");
				buttonRunExternal.setText("Clear");
				externalButtonRunReady =  false;
			}
			if (setting == 3) {
				buttonSave.setEnabled(true);
				labelInfo2.setText("Modified File Loaded");
				buttonRunExternal.setText("Clear");
				externalButtonRunReady =  false;
			}
			
			//TODO: make a cance button work
			if (setting == 4) {
				buttonRunExternal.setText("Cancel");
			}
			
		}
		
		if (controller.isExternallyDependent()) {
			if (!internalButtonRunReady) buttonRunInternal.setEnabled(true);
			else buttonRunInternal.setEnabled(controller.externalSegmentationExists());
		}
	}
	
	
	public String getDataSetName() {
		return dataSetName.getText();
	}

	/**
	 * Locks or unlocks the subsegmentation checkbox and combobox.
	 * Called after a successful run (locked=true) or after all data is cleared (locked=false).
	 * When unlocking, the checkbox is only re-enabled if datasets are available.
	 */
	public void setSubsegmentationLocked(boolean locked) {
		if (panelNumber == 0 || checkBoxSubsegmentation == null) return;
		subsegmentationLocked = locked;
		if (locked) {
			checkBoxSubsegmentation.setEnabled(false);
			comboBoxSubsegmentation.setEnabled(false);
		} else {
			String[] names = controller.getDataSetNames();
			boolean hasDatasets = (names != null && names.length > 0);
			checkBoxSubsegmentation.setEnabled(hasDatasets);
			comboBoxSubsegmentation.setEnabled(hasDatasets && checkBoxSubsegmentation.isSelected());
		}
	}

	/**
	 * Refreshes the subsegmentation checkbox and combobox when the pool of available
	 * datasets changes (e.g. another panel completes a run or clears its data).
	 * No-op on the first panel and while a run result is locked in.
	 */
	public void refreshSubsegmentation(String[] dataSetNames, boolean anyHasInternalSeg) {
		if (panelNumber == 0 || checkBoxSubsegmentation == null || subsegmentationLocked) return;
		comboBoxSubsegmentation.removeAllItems();
		if (dataSetNames != null) {
			for (String name : dataSetNames) comboBoxSubsegmentation.addItem(name);
		}
		// Only enable if at least one parent dataset has completed internal segmentation
		checkBoxSubsegmentation.setEnabled(anyHasInternalSeg);
		if (!anyHasInternalSeg) {
			checkBoxSubsegmentation.setSelected(false);
			comboBoxSubsegmentation.setEnabled(false);
		} else {
			comboBoxSubsegmentation.setEnabled(checkBoxSubsegmentation.isSelected());
		}
	}
	
	public void updateInputFile() {
		
		if (inputSelection.getFileType() == FileType.NOT_DIRECTORY) {
			inputMessage.setText("Not a valid directory");
			inputMessage.setForeground(Color.RED);
		}
		
		else if (inputSelection.getFileType() == FileType.NO_FILES) {
			inputMessage.setText("Directory has no files");
			inputMessage.setForeground(Color.RED);
		}
		
		else if (inputSelection.getFileType() == FileType.MULTIPLE_FILES) {
			inputMessage.setText("Directory has more than one file");
			inputMessage.setForeground(Color.RED);
		}
			
		else if (inputSelection.getFileType() == FileType.SINGLE_FILE) {
			inputMessage.setText("Input File Selected");
			inputMessage.setForeground(Color.BLACK);
			controller.setInputFilePath(inputSelection.getFile().getAbsolutePath());
		}	
		
		controller.setInputField(textFieldInput.getText());
		repaint();
		revalidate();
	}


			
			
			


}
