package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import calculations.Data;
import calculations.DataType;
import dataStructure.DataSet;
import util.FileLoadFromOverlay;

public class InternalSegmentationSettings extends JFrame implements ActionListener {

	JTextPane buttonGeneralInfo = new JTextPane();
	
	GridBagConstraints constraints;
	JPanel panel = new JPanel(new GridBagLayout());
	OperationController controller;

	JCheckBox checkBoxEdgeExclusion;
	
	public InternalSegmentationSettings(OperationController controller) {
		this.controller = controller;
		checkBoxEdgeExclusion = new JCheckBox("Edge Exclusion", controller.getExcludeInternalEdges());
		createView();
	}
	
	public void createView() {
		
		//Sets JFrame Size
		setMinimumSize(new Dimension(300,300));
				
		//Create a new panel with GridBagLayout manager
	    constraints = new GridBagConstraints();
	    constraints.anchor = GridBagConstraints.WEST;
	    constraints.anchor = GridBagConstraints.BASELINE_LEADING;
	      
	    //ROW 0
	    constraints.gridy = 0;
	   
	    //Measurements Label
	    constraints.gridx = 0;
	    panel.add(new JLabel("Internal Settings"), constraints);
	    
	    //ROW 1
	    constraints.gridy = 1;
	 
	    //Edge Exclusion Selection
	    constraints.gridx = 0;
	    panel.add(checkBoxEdgeExclusion, constraints);
	    
	    //Observers
	    checkBoxEdgeExclusion.addActionListener(this);
	    
		//Finalize JFrame/JPanel
		add(panel);
	    pack();
	    setLocationRelativeTo(null);
	    setVisible(true);
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == checkBoxEdgeExclusion) {
			controller.setExcludeInternalEdges(checkBoxEdgeExclusion.isSelected());
		}
			
	}
}
