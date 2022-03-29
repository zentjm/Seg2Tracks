package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.FileSelectionPanel;
import util.FileType;

public class HeadPanel extends JPanel implements ActionListener, Observer {

	GridBagConstraints constraints = new GridBagConstraints();
	Seg2TracksController controller;
	
	//Label
	JLabel citationReference = new JLabel("Please cite: xyz");
	
	//Help Button
	JButton buttonHelp = new JButton ("Help");
	
	//Output Selections
	JButton buttonOutput = new JButton ("Output");
	JTextField textFieldOutput = new JTextField("Insert Output File", 25);
	JLabel outputMessage = new JLabel("  "); //TODO: italicize, create output
	
	Preferences preferences = Preferences.userRoot().node("/seg2tracks");
	
	
	//File Selection panel
	FileSelectionPanel outputSelection; // = new FileSelectionPanel("Output", "Input outputz", this);
	String outputFilePath;
	//File outputFile;
	
	public HeadPanel(Seg2TracksController controller) {
		this.controller = controller;
		initialize();
		createPanel();
	}

	public void initialize() {
		//inputSelection = new FileSelectionPanel("Input", controller.getInputField(), this);
		outputSelection = new FileSelectionPanel("Output", controller.getOutputField(), this); //outputFilePath
		buttonOutput = outputSelection.getButton();
		textFieldOutput = outputSelection.getField();
		outputSelection.addObserver(this);
		outputSelection.forceTimerUpdate();
	}
	
	public void saveSettings() {
		preferences.put("OUTPUT_FILE_PATH", outputFilePath);
	}
	
	
	public void createPanel() {
	
		outputMessage.setFont(new Font(outputMessage.getFont().getName(), Font.ITALIC + Font.BOLD, outputMessage.getFont().getSize()));
		
		//Constraint constants
		//setPreferredSize(new Dimension(850,50));
		setMinimumSize(new Dimension(850,75));
		//constraints.anchor = GridBagConstraints.BASELINE_LEADING;
	    //constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.weighty = 1;
	   
		
		//ROW 0
		constraints.gridy = 0;
		  
		//Panel title
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(citationReference, constraints);
        
        //Help button
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonHelp, constraints);
 
        //Add action listeners
        buttonHelp.addActionListener(this);
        buttonOutput.addActionListener(this);
             
	}
	
	public void switchToOperation() {

		removeAll();
		//ROW 0
		constraints.gridy = 0;
		  
		//Panel title
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(citationReference, constraints);
        

        
        
        //Help button
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonHelp, constraints);
 
        //Add action listeners
        buttonHelp.addActionListener(this);
        buttonOutput.addActionListener(this);
		
        repaint();
		revalidate();
	}

	
	

	public void switchToAnalysis() {

		removeAll();
		//ROW 0
		constraints.gridy = 0;
		  
		//Output Button
        constraints.gridx = 0;
        add(buttonOutput, constraints);
        
        //Output Text Field
        constraints.gridx = 1;
        textFieldOutput.setText(controller.getOutputField());
        add(textFieldOutput, constraints);  
        
        //Help button
        constraints.gridx = 2;
        add(buttonHelp, constraints);

        //ROW 1
        constraints.gridy = 1;
     
        //add feedback for output file
        constraints.gridx = 0;
		constraints.gridwidth = 5;
    	add(outputMessage, constraints);
    	constraints.gridwidth = 1;
    	
		repaint();
		revalidate();
	}
	
	
	
	
	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonHelp) {
			controller.openHelpMenu();
		}	
	}

	public String getOutputPath () {
		return outputFilePath;
	}
	
	
	//updates the message and output file
	public void updateOutputFile() {
		
		if (outputSelection.getFileType() == FileType.NOT_DIRECTORY) {
			outputMessage.setText("Not a valid directory");
			outputMessage.setForeground(Color.RED);
		}
		
		else if (outputSelection.getFileType() != FileType.NO_FILES) {
			outputMessage.setText("Warning: Output directory is not empty");
			outputMessage.setForeground(Color.ORANGE);
			controller.setOutputFilePath(outputSelection.getDirectory().getAbsolutePath());
			//TODO: Check if file will be overwritten
		}
		
		else if (outputSelection.getFileType() == FileType.NO_FILES) {
			//outputFile = outputSelection.getDirectory();
			outputMessage.setText("Output directory selected");
			outputMessage.setForeground(Color.BLACK);
			//outputFilePath = outputTextField.getText();
			controller.setOutputFilePath(outputSelection.getDirectory().getAbsolutePath());
		}
		
		controller.setOutputField(textFieldOutput.getText());
			
		repaint();
		revalidate();
	}
	
	
	
	public void update(Observable obs, Object arg) {
		if (obs instanceof FileSelectionPanel) {
			updateOutputFile();
		}
		
		//If the output is updated, update the seg2Tracks output
		
	}
	
}
