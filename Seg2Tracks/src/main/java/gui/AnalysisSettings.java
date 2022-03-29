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

public class AnalysisSettings extends JFrame implements ActionListener {

	JTextPane buttonGeneralInfo = new JTextPane();

	GridBagConstraints constraints;
	JPanel panel = new JPanel(new GridBagLayout());

	Data[] dataList;
	
  	int columns;
	
	ArrayList <JCheckBox> segCalcList;
	ArrayList <JCheckBox> linkCalcList;
	ArrayList <JCheckBox> linkStatsList;
	ArrayList <JCheckBox> frameCalcList;
	ArrayList <JCheckBox> frameStatsList;
	
	public AnalysisSettings(Data[] dataList) {
		this.dataList = dataList;
		columns = 3;
		organizeMenu();
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
	    panel.add(new JLabel("CALCULATIONS"), constraints);
	    
	    //ROW 1
	    constraints.gridy = 1;
	 
	    //Segment Calculation Label
	    constraints.gridx = 0;
	    panel.add(new JLabel("Segments:"), constraints);
	    
	    //Next Rows
	    int row = 1;
	    //Balance lists across rows
	    for (int i = 0; i < segCalcList.size(); i ++) {
	    	if (i%columns == 0) row++;
	    	constraints.gridx = i%columns;
	    	constraints.gridy = row;
	    	panel.add(segCalcList.get(i), constraints);
	    }
	    
	    //LinkSet Calculation Label
	    row++;
	    constraints.gridy = row;
	    constraints.gridx = 0;
	    panel.add(new JLabel("LinkSets:"), constraints);
	    
	    //Next Rows
	    for (int i = 0; i < linkCalcList.size(); i ++) {
	    	if (i%columns == 0) row++;
	    	constraints.gridx = i%columns;
	    	constraints.gridy = row;
	    	panel.add(linkCalcList.get(i), constraints);
	    }
	    
	    //FrameSet Calculation Label
	    row++;
	    constraints.gridy = row;
	    constraints.gridx = 0;
	    panel.add(new JLabel("FrameSets:"), constraints);
	    
	    //Next Rows
	    for (int i = 0; i < frameCalcList.size(); i ++) {
	    	if (i%columns == 0) row++;
	    	constraints.gridx = i%columns;
	    	constraints.gridy = row;
	    	panel.add(frameCalcList.get(i), constraints);
	    }
	    
	    
	    //Add Separator,TODO: make colored
	    row++;
	    constraints.gridy = row;
	    constraints.gridx = 0;
	    JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
	    sep.setForeground(Color.BLACK);
	    sep.setBackground(Color.BLACK);
	    sep.setPreferredSize(new Dimension(this.getWidth(),5));
	    panel.add(sep, constraints);
	   
	    
	    //Statistics label
	    row++;
	    constraints.gridy = row;
	    constraints.gridx = 0;
	    panel.add(new JLabel("STATISTICS"), constraints);
	    
	    
	    //LinkSet Statistics Label
	    row++;
	    constraints.gridy = row;
	    constraints.gridx = 0;
	    panel.add(new JLabel("LinkSets:"), constraints);
	    
	    //Next Rows
	    for (int i = 0; i < linkStatsList.size(); i ++) {
	    	if (i%columns == 0) row++;
	    	constraints.gridx = i%columns;
	    	constraints.gridy = row;
	    	panel.add(linkStatsList.get(i), constraints);
	    }
	    
	    //LinkSet Statistics Label
	    row++;
	    constraints.gridy = row;
	    constraints.gridx = 0;
	    panel.add(new JLabel("FrameSets:"), constraints);
	    
	    //Next Rows
	    for (int i = 0; i < frameStatsList.size(); i ++) {
	    	if (i%columns == 0) row++;
	    	constraints.gridx = i%columns;
	    	constraints.gridy = row;
	    	panel.add(frameStatsList.get(i), constraints);
	    }
	    
	    
		//Finalize JFrame/JPanel
		add(panel);
	    pack();
	    setLocationRelativeTo(null);
	    setVisible(true);
	}

	public void organizeMenu() {
		
		segCalcList = new ArrayList<JCheckBox>();
		linkCalcList = new ArrayList<JCheckBox>();
		linkStatsList = new ArrayList<JCheckBox>();
		frameCalcList = new ArrayList<JCheckBox>();
		frameStatsList = new ArrayList<JCheckBox>();
		
		for (Data data: dataList) {
			if (data.getType() == DataType.SEGMENT_CALCULATION) {
				segCalcList.add(new JCheckBox(data.getName())); 
			}
			if (data.getType() == DataType.LINKSET_CALCULATION) {
				linkCalcList.add(new JCheckBox(data.getName()));
			}
			if (data.getType() == DataType.LINKSET_STATISTIC) {
				linkStatsList.add(new JCheckBox(data.getName()));
			}
			if (data.getType() == DataType.FRAMESET_CALCULATION) {
				frameCalcList.add(new JCheckBox(data.getName()));
			}
			if (data.getType() == DataType.FRAMESET_STATISTIC) {
				frameStatsList.add(new JCheckBox(data.getName()));
			}
		}	
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
}
