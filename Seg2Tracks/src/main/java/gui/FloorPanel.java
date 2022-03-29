package gui;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class FloorPanel extends JPanel implements ActionListener{

	GridBagConstraints constraints = new GridBagConstraints();
	
	Seg2TracksController controller;
	
	//Label
	JLabel progressLabel = new JLabel("Operation Progress:");
	
	JProgressBar progressBar = new JProgressBar();
	
	//Help Button
	JButton buttonAddPanel = new JButton("Add Panel");
	JButton buttonRemovePanel = new JButton("Remove Panel");
	JButton buttonAnalyzeMenu = new JButton ("DATA ANALYSIS >>");
	JButton buttonSegmentationMenu = new JButton ("<< SEGMENTATION");
	JButton buttonGenerateResults = new JButton ("GENERATE RESULTS");

	
	public FloorPanel(Seg2TracksController controller) {
		this.controller = controller;
		initialize();
		createView();
	}

	//initializes necessary functions
	public void initialize() {
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
	}
	
	
	public JProgressBar getProgressBar() {
		return progressBar;
	}
	
	
	public void setProgressTask(String task, int min, int max) {
		
		//basic properties
		progressBar.setString(task);
		progressBar.setMinimum(min);
		progressBar.setMaximum(max);
		
		//Initiate thread
	
	}
	
	private SwingWorker setProgressThead() {
		return new SwingWorker <Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				try {
					 //TODO: status update
					Thread.sleep(500);
					}
				catch (Exception ex) {
					System.err.println(ex);
					}
				return null;
			}
		};
			
	}
	
	

	public void createView() {
		
		//Constraint constants
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    
        //Add action listeners
        buttonAnalyzeMenu.addActionListener(this);	
        buttonSegmentationMenu.addActionListener(this);	
        buttonGenerateResults.addActionListener(this);	
        buttonAddPanel.addActionListener(this);
        buttonRemovePanel.addActionListener(this);
        
        switchToOperation();
	}
	
	public void allowAnalysisButton(boolean allow) {
		buttonAnalyzeMenu.setEnabled(allow);
	}
	
	public void allowResultsButton(boolean allow) {
		buttonGenerateResults.setEnabled(allow);
	}
	
	
	
	public void switchToOperation() {
		removeAll();
		
		//ROW 0
		constraints.gridy = 0;
		
		//Add operation panel
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(buttonAddPanel, constraints);
		
		//Remove operation panel
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(buttonRemovePanel, constraints);
		
		//Progress bar
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(progressLabel, constraints);
		
		//Panel title
        constraints.gridx = 3;
        constraints.anchor = GridBagConstraints.CENTER;
        add(progressBar, constraints);
        
        //Help button
        constraints.gridx = 4;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonAnalyzeMenu, constraints);
        
        //Reset analysis menu button
        buttonAnalyzeMenu.setEnabled(false);
        
	}

	public void switchToAnalysis() {
		
		removeAll();
		
		//ROW 0
		constraints.gridy = 0;
		 
		//Progress bar
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(progressLabel, constraints);
		
		//Panel title
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        add(progressBar, constraints);
        
        //Help button
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonSegmentationMenu, constraints);
       
        //Help button
        constraints.gridx = 3;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonGenerateResults, constraints);
        
        //Reset generate results menu button
        buttonGenerateResults.setEnabled(false);
        
        repaint();
        revalidate();
	}
	
	
	//Allows the generate-results only if acceptable conditions.
	
	public void allowPanelRemoval(boolean allow) {
		buttonRemovePanel.setEnabled(allow);
	}
	
	@Override
	public void actionPerformed(ActionEvent e ) {
		if (e.getSource() == buttonAnalyzeMenu) controller.switchToAnalysis();	
		if (e.getSource() == buttonSegmentationMenu) controller.switchToOperation();
		if (e.getSource() == buttonGenerateResults) {
			controller.exportResults();
			buttonGenerateResults.setEnabled(false);
			//TODO: Message saying results have been generated. 
		}
		if (e.getSource() == buttonAddPanel) controller.addOperationPanel();
		if (e.getSource() == buttonRemovePanel) controller.removeOperationPanel();
	}
		
	
	
		
		
		
		
		// TODO Auto-generated method stub
		
	
}
