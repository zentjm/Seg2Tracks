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

/**
 * The top header panel of the main Seg2Tracks window. Displays the help and change-log buttons, the last release,
 * and output file selection controls. Changes layout between segmentation mode (minimal) and
 * analysis mode (with output directory picker).
 */
public class HeadPanel extends JPanel implements ActionListener, Observer {

	GridBagConstraints constraints = new GridBagConstraints();
	Seg2TracksController controller;
	
	// Last release ("Last release: v0.5.4, 2026-10-06"), from version.properties; shown to the
	// right of the User Manual and Change Log buttons.
	JLabel releaseLabel = new JLabel(lastReleaseText());
	
	//Help Button
	JButton buttonHelp = new JButton ("User Manual");

	//Change Log Button
	JButton buttonChangeLog = new JButton ("Change Log");
	
	//Output Selections
	JButton buttonOutput = new JButton ("Output");
	JTextField textFieldOutput = new JTextField("Insert Output File", 25);
	JLabel outputMessage = new JLabel("  "); //TODO: italicize, create output
	
	Preferences preferences = Preferences.userRoot().node("/seg2tracks");
	
	
	//File Selection panel
	FileSelectionPanel outputSelection; // = new FileSelectionPanel("Output", "Input outputz", this);
	String outputFilePath;
	//File outputFile;
	
	/**
	 * "Last release: v0.5.4, 2026-10-06" from {@code version.properties}
	 * ({@code lastReleaseVersion}, {@code lastReleaseDate}), or "" if unavailable.
	 */
	private static String lastReleaseText() {
		try (java.io.InputStream is = HeadPanel.class.getResourceAsStream("/version.properties")) {
			if (is == null) return "";
			java.util.Properties props = new java.util.Properties();
			props.load(is);
			String v = props.getProperty("lastReleaseVersion", "").trim();
			String d = props.getProperty("lastReleaseDate", "").trim();
			if (v.isEmpty() && d.isEmpty()) return "";
			String text = "Last release: " + (v.isEmpty() ? "" : "v" + v) + (v.isEmpty() || d.isEmpty() ? "" : ", ") + d;
			return "<html><i>" + text + "</i></html>";
		} catch (java.io.IOException e) {
			return "";
		}
	}

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
		  
		//Help button
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonHelp, constraints);

        //Change Log button
        constraints.gridx = 1;
        add(buttonChangeLog, constraints);

        //Last release, to the right of the buttons
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(releaseLabel, constraints);

        //Add action listeners
        buttonHelp.addActionListener(this);
        buttonChangeLog.addActionListener(this);
        buttonOutput.addActionListener(this);

	}
	
	public void switchToOperation() {

		removeAll();
		//ROW 0
		constraints.gridy = 0;

		//Help button
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonHelp, constraints);

        //Change Log button
        constraints.gridx = 1;
        add(buttonChangeLog, constraints);

        //Last release, to the right of the buttons
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(releaseLabel, constraints);

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

        //Change Log button
        constraints.gridx = 3;
        add(buttonChangeLog, constraints);

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
		// A button press starts a new action: clear a finished result on the shared progress bar.
		// (Buttons only: some dropdowns also fire action events when refreshed programmatically.)
		if (e.getSource() instanceof javax.swing.AbstractButton) controller.clearProgressResult();
		if (e.getSource() == buttonHelp) {
			controller.openHelpMenu();
		}
		else if (e.getSource() == buttonChangeLog) {
			controller.openChangeLog();
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
