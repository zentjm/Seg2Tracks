package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CalibrationPanel extends JFrame implements ActionListener{
	OperationController controller;
	OperationModel model;
	
	JTextField gaussianBlurSigma = new JTextField(" ", 10);
	JTextField maximumFinderTolerance = new JTextField(" ", 10);
	
	JButton guidedCalibrationButton = new JButton("Guided Calibration");
	JButton buttonApply = new JButton("Apply");
	
	JLabel calibrationMessage = new JLabel(" "); //TODO: italicize, create output
	
	boolean loadedData;
	
	//Create Panel
	GridBagConstraints constraints;
	JPanel panel = new JPanel(new GridBagLayout());
	
	public CalibrationPanel(OperationController controller, boolean loadedData)  {
		super("Object Identification Settings");
		this.controller = controller;
		this.loadedData = loadedData;
		initialize();
		createView();
	}
	
	//load settings
	public void initialize() {
		gaussianBlurSigma.setText("" + controller.getGaussianBlurSigma());
		maximumFinderTolerance.setText("" + controller.getMaximumFinderTolerance());
	}
	
	//Create View
	public void createView() {
		
		//Sets JFrame Size
		setMinimumSize(new Dimension(200,200));
				
		//Create a new panel with GridBagLayout manager
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
	      
        //setCalibration
        calibrationMessage.setFont(new Font(calibrationMessage.getFont().getName(), Font.ITALIC + Font.BOLD, calibrationMessage.getFont().getSize()));
        
        //ROW 0
      	constraints.gridy = 0;
        
      	//Sigma Label
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.gridx = 0;
        panel.add(new JLabel("Sigma:"), constraints);
        
    	//Noise Label
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.gridx = 1;
        panel.add(gaussianBlurSigma, constraints);
        
        //ROW 1
      	constraints.gridy = 1;
        
      	//Sigma Text
        constraints.gridx = 0;
        panel.add(new JLabel("Threshold:"), constraints);
        
    	//Noise Text
        constraints.gridx = 1;
        panel.add(maximumFinderTolerance, constraints);
        
        //ROW 3
    	constraints.gridy = 2;
        
      	//Guided Calibration button
    	constraints.gridx = 0;
    	panel.add(guidedCalibrationButton, constraints);
    	guidedCalibrationButton.setEnabled(false); //TODO: make this work
    	
    	//Apply button
    	constraints.gridx = 1;
    	panel.add(buttonApply, constraints);
    
    	//ROW 4
    	constraints.gridy = 3;
    	
    	//Status message
    	constraints.gridx = 0;
    	constraints.gridwidth = 3;
    	panel.add(calibrationMessage,constraints);
    	constraints.gridwidth = 1;
    	
    	
    	//OBSERVERS
    	
    	//Add action listeners
    	guidedCalibrationButton.addActionListener(this);
    	buttonApply.addActionListener(this);
    	
    	//Finalize JFrame/JPanel
		add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
	}
	
	//Pushes the calibration settings back to the controller
	public void setCalibration () {
		if (loadedData) {
			//Checks user preference
			int choice = 1;
			Object[] options = {"Ok", "Cancel"};
			choice = JOptionPane.showOptionDialog(null, "Resetting calibration will remove currently loaded segmentations", "Warning",
			JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
			null, options, options[1]);
			
			if (choice == 1) return;
			controller.clearData();
			
			
			
		}
	

		try {
			controller.setGaussianBlurSigma(Double.parseDouble(gaussianBlurSigma.getText()));
			controller.setMaximumFinderTolerance(Double.parseDouble(maximumFinderTolerance.getText()));
			calibrationMessage.setForeground(Color.BLACK);
			calibrationMessage.setText("New values applied ");
		}
		catch (NumberFormatException exception) {
			calibrationMessage.setForeground(Color.RED);
			calibrationMessage.setText("Both values must be integers");
		}
	}
	
	
	
	public void runGuidedCalibration () {
		GuidedCalibration calibrate  = new GuidedCalibration(controller);
		calibrate.run();
		
		
		
		
	}
	
	
	
	
	//Action listener
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonApply) setCalibration();
		
		if (e.getSource() == guidedCalibrationButton) runGuidedCalibration();
		
		
	}
}
