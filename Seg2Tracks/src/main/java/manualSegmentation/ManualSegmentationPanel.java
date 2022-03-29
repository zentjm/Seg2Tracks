package manualSegmentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;



public class ManualSegmentationPanel extends JFrame implements ActionListener {

	private static final long serialVersionUID = 3344773321063920231L;

	//controller role fulfilled by ManualSegmentation Class;
	ManualSegmentationController controller;
	
	//Panel
	JPanel panel;
	
	//Main menu
	JButton buttonNewSegmentation = new JButton("Segmentation");
	JButton buttonModifyObjects = new JButton("Modify Objects");
	JButton buttonEndSession = new JButton("End Session");
	
	//Return to Main Menu
	JButton buttonMainMenu = new JButton("Main Menu");
	
	//Selection Menu
	JButton buttonStartObject = new JButton("Start Object");
	JButton buttonNextFrame = new JButton("Next Frame");
	JButton buttonPreviousFrame = new JButton("Previous Frame");
	JButton buttonRestoreSelection = new JButton("Restore Selection");
	JButton buttonEndObject = new JButton("End Object");
	
	//Modification Menu
	JButton buttonDeleteObject = new JButton("Delete");
	JButton buttonMergeObject = new JButton("Merge");
	JButton buttonSplitObject = new JButton("Split");
	JButton buttonLinkObjects = new JButton("Link");
	JButton buttonUnlinkObjects = new JButton("Unlink");
	
	//Editable
	boolean canEdit;

	//inputImage
	ImagePlus imagePlus;
	ModifiedStackWindow window;

	public ManualSegmentationPanel(ManualSegmentationController controller, ImagePlus imagePlus, boolean canEdit) {
		super("Seg2Tracks - Manual Segmentation");
		this.controller = controller;
		this.imagePlus = imagePlus;
		this.canEdit = canEdit;
		setAlwaysOnTop(true);
	}
	
	public void createView() {
		
		setMinimumSize(new Dimension(400, 100));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		//Set up Panel
		if (canEdit) {
			setSize(400, 100); //TODO: does nothing
			panel = new JPanel(new GridLayout(3,1));
		}
		if (!canEdit) {
			setSize(400, 25); //TODO does nothing
			panel = new JPanel(new GridLayout(1,1));
			
		}
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	
		//Main Panel
		buttonNewSegmentation.addActionListener(this);
		buttonModifyObjects.addActionListener(this);
		buttonEndSession.addActionListener(this);

		//Selection Panel
		buttonStartObject.addActionListener(this);
		buttonNextFrame.addActionListener(this);
		buttonPreviousFrame.addActionListener(this);
		buttonRestoreSelection.addActionListener(this);
		buttonEndObject.addActionListener(this);
		buttonMainMenu.addActionListener(this); //Multiple panels use
	
		//Modification Panel
		buttonDeleteObject.addActionListener(this);
		buttonMergeObject.addActionListener(this);
		buttonSplitObject.addActionListener(this);
		buttonLinkObjects.addActionListener(this);
		buttonUnlinkObjects.addActionListener(this);
		
		
		//XXX: Disabling unimplemented buttons
		buttonSplitObject.setEnabled(false);
		buttonLinkObjects.setEnabled(false);
		buttonUnlinkObjects.setEnabled(false);
		
		
				
		//Initiate Panel
		add(panel);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void setMainPanel() {
		
		panel.removeAll();
		if (canEdit) panel.add(buttonNewSegmentation);
		if (canEdit) panel.add(buttonModifyObjects);
		panel.add(buttonEndSession);
		panel.repaint();
		panel.revalidate();
	}
	
	public void setSegmentPanel() {
		panel.removeAll();
		panel.add(buttonStartObject);
		panel.add(buttonEndObject);
		panel.add(buttonNextFrame);
		panel.add(buttonPreviousFrame);
		panel.add(buttonRestoreSelection);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
	}
	
	public void setModificationPanel() {
		panel.removeAll();
		panel.add(buttonDeleteObject);
		panel.add(buttonMergeObject);
		panel.add(buttonSplitObject);
		panel.add(buttonLinkObjects);
		panel.add(buttonUnlinkObjects);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
	}
	
	public void stateDataLoaded(boolean loaded) {
		buttonModifyObjects.setEnabled(loaded);
	}
	
	//TODO: this is messy, can we clean it up?
	public void stateObject(boolean startObject, boolean frameIteration, boolean firstFrame, boolean lastFrame, boolean selectingObject) {
		buttonStartObject.setEnabled(startObject);
		buttonEndObject.setEnabled(frameIteration);
		buttonNextFrame.setEnabled(frameIteration && !lastFrame);
		buttonPreviousFrame.setEnabled(frameIteration && !firstFrame);
		buttonRestoreSelection.setEnabled(frameIteration && !firstFrame);
		buttonMainMenu.setEnabled(selectingObject);
	}
	
	
	public void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}
	
	public void dialogAlert(String alert) {
		JOptionPane.showMessageDialog((Component) null, alert, "alert", JOptionPane.WARNING_MESSAGE);
		 //int choice =  JOptionPane.showConfirmDialog((Component) null, alert, "alert", JOptionPane.OK_CANCEL_OPTION);
		 //if (choice == JOptionPane.OK_OPTION) return true;
		 //return false;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

		//Main menu buttons
		if (e.getSource() == buttonNewSegmentation) controller.newSegmentation();
		if (e.getSource() == buttonModifyObjects) controller.modifyMenu();	
		
		
		if (e.getSource() == buttonEndSession) controller.exit();
			//TODO: Exit with loading. 
		
		//Object Creation Buttons //TODO
		if (e.getSource() == buttonStartObject) controller.startObject();
		if (e.getSource() == buttonNextFrame) controller.nextFrame();
		if (e.getSource() == buttonPreviousFrame) {
			//TODO: Implement?
		}
		if (e.getSource() == buttonRestoreSelection) controller.restoreSelection();
		if (e.getSource() == buttonEndObject) controller.endObject();
		
		//Object modification buttons
		if (e.getSource() == buttonDeleteObject) controller.deleteObject();
		if (e.getSource() == buttonMergeObject) controller.mergeObject();
		if (e.getSource() == buttonSplitObject) controller.splitObject();
		if (e.getSource() == buttonLinkObjects) controller.linkObject();
		if (e.getSource() == buttonUnlinkObjects) controller.unLinkObject();
		
		//Main menu
		if (e.getSource() == buttonMainMenu) controller.mainMenu();	
	}	
}

