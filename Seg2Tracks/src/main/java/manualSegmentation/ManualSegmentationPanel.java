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
import ij.Prefs;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;



/**
 * View window for manual segmentation controls. Displays context-specific button panels for:
 * main menu (create/modify/end segmentation), drawing individual segments (frame progression),
 * and modifying existing objects (delete/merge/split/link operations). Updates button enabled states
 * based on workflow progress and data availability.
 */
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
	JButton buttonCancelObject = new JButton("Cancel Object");
	
	//Modification Menu
	JButton buttonDeleteObject = new JButton("Delete");
	JButton buttonMergeObject = new JButton("Merge");
	JButton buttonRedrawSegment = new JButton("Redraw Segment");
	JButton buttonSplitObject = new JButton("Split");
	JButton buttonLinkObjects = new JButton("Link");
	JButton buttonUnlinkObjects = new JButton("Unlink");

	// Split line confirmation sub-panel
	JButton buttonConfirmSplit = new JButton("Apply Split");
	JButton buttonCancelSplit  = new JButton("Cancel Split");

	// Redraw-segment confirmation sub-panel
	JButton buttonApplyRedraw  = new JButton("Apply Redraw");
	JButton buttonCancelRedraw = new JButton("Cancel Redraw");

	// Settings sub-panel
	JButton buttonSettings     = new JButton("Settings");
	JButton buttonRoiColor     = new JButton("ROI Color...");
	JButton buttonDrawTool     = new JButton("Tool: Polygon");
	JButton buttonSettingsBack = new JButton("Back");

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
		buttonCancelObject.addActionListener(this);
		buttonMainMenu.addActionListener(this); //Multiple panels use
	
		//Modification Panel
		buttonDeleteObject.addActionListener(this);
		buttonMergeObject.addActionListener(this);
		buttonRedrawSegment.addActionListener(this);
		buttonSplitObject.addActionListener(this);
		buttonLinkObjects.addActionListener(this);
		buttonUnlinkObjects.addActionListener(this);
		buttonConfirmSplit.addActionListener(this);
		buttonCancelSplit .addActionListener(this);
		buttonApplyRedraw .addActionListener(this);
		buttonCancelRedraw.addActionListener(this);
		buttonSettings    .addActionListener(this);
		buttonRoiColor    .addActionListener(this);
		buttonDrawTool    .addActionListener(this);
		buttonSettingsBack.addActionListener(this);
				
		//Initiate Panel
		add(panel);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void setMainPanel() {
		panel.setLayout(new GridLayout(canEdit ? 4 : 1, 1));
		panel.removeAll();
		if (canEdit) panel.add(buttonNewSegmentation);
		if (canEdit) panel.add(buttonModifyObjects);
		if (canEdit) panel.add(buttonSettings);
		panel.add(buttonEndSession);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	public void setSegmentPanel() {
		panel.setLayout(new GridLayout(8, 1));
		panel.removeAll();
		panel.add(buttonStartObject);
		panel.add(buttonEndObject);
		panel.add(buttonCancelObject);
		panel.add(buttonNextFrame);
		panel.add(buttonPreviousFrame);
		panel.add(buttonRestoreSelection);
		panel.add(buttonSettings);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	/**
	 * Shows the Settings sub-panel.
	 * @param fromSegment true when entered from the segmentation panel — adds the draw-tool toggle.
	 */
	public void setSettingsPanel(boolean fromSegment) {
		int rows = fromSegment ? 3 : 2;
		panel.setLayout(new GridLayout(rows, 1));
		panel.removeAll();
		panel.add(buttonRoiColor);
		if (fromSegment) {
			updateDrawToolButton(Prefs.get(ManualSegmentationController.PREF_TOOL, "polygon"));
			panel.add(buttonDrawTool);
		}
		panel.add(buttonSettingsBack);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	/** Updates the draw-tool toggle button label to reflect the currently stored preference. */
	public void updateDrawToolButton(String tool) {
		buttonDrawTool.setText("freehand".equals(tool) ? "Tool: Freehand" : "Tool: Polygon");
	}
	
	public void setModificationPanel() {
		panel.setLayout(new GridLayout(7, 1));
		panel.removeAll();
		panel.add(buttonDeleteObject);
		panel.add(buttonMergeObject);
		panel.add(buttonRedrawSegment);
		panel.add(buttonSplitObject);
		panel.add(buttonLinkObjects);
		panel.add(buttonUnlinkObjects);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	/**
	 * Switches the panel to split-line mode: hides all other buttons and shows
	 * only "Apply Split" and "Cancel Split". Called after the user clicks Split
	 * and selects a target object, prompting them to draw a bisecting line ROI.
	 */
	public void setSplitLinePanel() {
		panel.setLayout(new GridLayout(3, 1));
		panel.removeAll();
		panel.add(new javax.swing.JLabel("Draw a line across the object, then:"));
		panel.add(buttonConfirmSplit);
		panel.add(buttonCancelSplit);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	/**
	 * Switches the panel to redraw-segment mode: hides all other buttons and shows
	 * only "Apply Redraw" and "Cancel Redraw". Called after the user clicks Redraw
	 * Segment with exactly one object selected, prompting them to draw a replacement
	 * outline for the segment on the currently displayed frame.
	 */
	public void setRedrawSegmentPanel() {
		panel.setLayout(new GridLayout(3, 1));
		panel.removeAll();
		panel.add(new javax.swing.JLabel("Draw a new outline for this frame, then:"));
		panel.add(buttonApplyRedraw);
		panel.add(buttonCancelRedraw);
		panel.repaint();
		panel.revalidate();
		pack();
	}
	
	public void stateDataLoaded(boolean loaded) {
		buttonModifyObjects.setEnabled(loaded);
	}
	
	//TODO: this is messy, can we clean it up?
	public void stateObject(boolean startObject, boolean frameIteration, boolean firstFrame, boolean lastFrame, boolean selectingObject) {
		buttonStartObject.setEnabled(startObject);
		buttonEndObject.setEnabled(frameIteration);
		buttonCancelObject.setEnabled(frameIteration);
		buttonNextFrame.setEnabled(frameIteration && !lastFrame);
		buttonPreviousFrame.setEnabled(frameIteration && !firstFrame);
		buttonRestoreSelection.setEnabled(frameIteration && !firstFrame);
		buttonMainMenu.setEnabled(selectingObject);
	}
	
	
	public void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}
	
	/** Shows an OK/Cancel question; returns true if the user chose OK. */
	public boolean dialogConfirm(String question) {
		return JOptionPane.showConfirmDialog((Component) null, question, "Confirm",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
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
		if (e.getSource() == buttonPreviousFrame) controller.previousFrame();
		
		if (e.getSource() == buttonRestoreSelection) controller.restoreSelection();
		if (e.getSource() == buttonEndObject) controller.endObject();
		if (e.getSource() == buttonCancelObject) controller.cancelObject();
		
		//Object modification buttons
		if (e.getSource() == buttonDeleteObject) controller.deleteObject();
		if (e.getSource() == buttonMergeObject) controller.mergeObject();
		if (e.getSource() == buttonRedrawSegment) controller.startRedrawSegment();
		if (e.getSource() == buttonApplyRedraw)  controller.applyRedrawSegment();
		if (e.getSource() == buttonCancelRedraw) controller.cancelRedrawSegment();
		if (e.getSource() == buttonSplitObject)  controller.splitObject();
		if (e.getSource() == buttonConfirmSplit) controller.confirmSplitLine();
		if (e.getSource() == buttonCancelSplit)  controller.cancelSplit();
		if (e.getSource() == buttonLinkObjects)   controller.linkObject();
		if (e.getSource() == buttonUnlinkObjects) controller.unLinkObject();
		if (e.getSource() == buttonSettings)      controller.openSettings();
		if (e.getSource() == buttonRoiColor)      controller.changeRoiColor();
		if (e.getSource() == buttonDrawTool)      controller.toggleDrawTool();
		if (e.getSource() == buttonSettingsBack)  controller.closeSettings();

		//Main menu
		if (e.getSource() == buttonMainMenu) controller.mainMenu();
	}	
}

