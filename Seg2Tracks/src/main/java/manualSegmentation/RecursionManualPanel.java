package manualSegmentation;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ij.ImagePlus;
import ij.Prefs;

/**
 * View window for manual recursive (subsegmentation) controls.
 * Mirrors ManualSegmentationPanel but adds a "Next Segment" button so the user can
 * advance to the next parent segment when finished segmenting subsegments in the current one.
 *
 * Panel layout:
 *   Main Menu  : Start Segmentation | End Session
 *   Segment    : Start Object | End Object | Next Frame | Previous Frame |
 *                Restore Selection | Next Segment | Main Menu
 *
 * The window title is updated each time a new parent segment is shown via
 * setCellName(String), displaying the segment's display name in the title bar.
 */
public class RecursionManualPanel extends JFrame implements ActionListener {

	private static final long serialVersionUID = 7712834501029384756L;

	RecursionManualController controller;

	/** When false, segmentation and modification buttons are hidden — view-only mode. */
	boolean canEdit;

	JPanel panel;

	// ── Main menu ─────────────────────────────────────────────────────────────
	JButton buttonNewSegmentation  = new JButton("Segmentation");
	JButton buttonEndSession       = new JButton("End Session");

	// ── Segment panel ─────────────────────────────────────────────────────────
	JButton buttonStartObject      = new JButton("Start Object");
	JButton buttonNextFrame        = new JButton("Next Frame");
	JButton buttonPreviousFrame    = new JButton("Previous Frame");
	JButton buttonRestoreSelection = new JButton("Restore Selection");
	JButton buttonEndObject        = new JButton("End Object");

	/** Advances to the next parent segment without ending the session. */
	JButton buttonNextCell         = new JButton("Next Segment");

	// ── Modification panel ────────────────────────────────────────────────
	JButton buttonModify           = new JButton("Modify");
	JButton buttonDeleteObject     = new JButton("Delete Object");
	JButton buttonMergeObjects     = new JButton("Merge Objects");
	JButton buttonRedrawSegment    = new JButton("Redraw Segment");

	// ── Redraw-segment sub-panel ─────────────────────────────────────────
	JButton buttonApplyRedraw      = new JButton("Apply Redraw");
	JButton buttonCancelRedraw     = new JButton("Cancel Redraw");

	// ── Shared ────────────────────────────────────────────────────────────────
	JButton buttonMainMenu         = new JButton("Main Menu");

	// ── Settings sub-panel ────────────────────────────────────────────────────
	JButton buttonSettings         = new JButton("Settings");
	JButton buttonRoiColor         = new JButton("ROI Color...");
	JButton buttonDrawTool         = new JButton("Tool: Polygon");
	JButton buttonSettingsBack     = new JButton("Back");

	/** Base title prefix — cell name is appended dynamically. */
	private static final String TITLE_PREFIX = "Seg2Tracks — Recursive Segmentation";

	public RecursionManualPanel(RecursionManualController controller, boolean canEdit) {
		super(TITLE_PREFIX);
		this.controller = controller;
		this.canEdit    = canEdit;
		setAlwaysOnTop(true);
	}

	// ── Build window ──────────────────────────────────────────────────────────
	public void createView() {
		setMinimumSize(new Dimension(450, 100));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		panel = new JPanel(new GridLayout(2, 1));

		// Wire all buttons
		buttonNewSegmentation .addActionListener(this);
		buttonEndSession      .addActionListener(this);
		buttonStartObject     .addActionListener(this);
		buttonNextFrame       .addActionListener(this);
		buttonPreviousFrame   .addActionListener(this);
		buttonRestoreSelection.addActionListener(this);
		buttonEndObject       .addActionListener(this);
		buttonNextCell        .addActionListener(this);
		buttonModify          .addActionListener(this);
		buttonDeleteObject    .addActionListener(this);
		buttonMergeObjects    .addActionListener(this);
		buttonRedrawSegment   .addActionListener(this);
		buttonApplyRedraw     .addActionListener(this);
		buttonCancelRedraw    .addActionListener(this);
		buttonMainMenu        .addActionListener(this);
		buttonSettings        .addActionListener(this);
		buttonRoiColor        .addActionListener(this);
		buttonDrawTool        .addActionListener(this);
		buttonSettingsBack    .addActionListener(this);

		add(panel);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	// ── Panel states ──────────────────────────────────────────────────────────

	/**
	 * Shows the top-level menu.
	 * Edit mode   : Segmentation | Modify | End Session
	 * Preview mode: Next Segment | End Session   (sidebar handles all segment switching)
	 */
	public void setMainPanel() {
		int rows = canEdit ? 4 : 2;
		panel.setLayout(new GridLayout(rows, 1));
		panel.removeAll();
		if (canEdit) {
			panel.add(buttonNewSegmentation);
			panel.add(buttonModify);
			panel.add(buttonSettings);
		} else {
			panel.add(buttonNextCell);
		}
		panel.add(buttonEndSession);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	/**
	 * Shows the per-segment segmentation controls, including the "Next Segment" button.
	 * Call stateObject() immediately after to set initial button enable states.
	 */
	public void setSegmentPanel() {
		panel.setLayout(new GridLayout(8, 1));
		panel.removeAll();
		panel.add(buttonStartObject);
		panel.add(buttonEndObject);
		panel.add(buttonNextFrame);
		panel.add(buttonPreviousFrame);
		panel.add(buttonRestoreSelection);
		panel.add(buttonNextCell);
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

	/** Shows the modification controls: delete, merge, redraw, or return to main menu. */
	public void setModificationPanel() {
		panel.setLayout(new GridLayout(5, 1));
		panel.removeAll();
		panel.add(buttonDeleteObject);
		panel.add(buttonMergeObjects);
		panel.add(buttonRedrawSegment);
		panel.add(buttonNextCell);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
		pack();
	}

	/**
	 * Switches the panel to redraw-segment mode: hides all other buttons and shows
	 * only "Apply Redraw" and "Cancel Redraw". Called after the user clicks Redraw
	 * Segment with exactly one track selected, prompting them to draw a replacement
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

	/**
	 * Updates which segment-panel buttons are enabled based on workflow state.
	 * Mirrors ManualSegmentationPanel.stateObject() exactly.
	 *
	 * @param startObject      true when no object is being drawn (ready to start one)
	 * @param frameIteration   true while actively drawing an object frame by frame
	 * @param firstFrame       true when on the first frame of the current object
	 * @param lastFrame        true when on the last frame of the image stack
	 * @param selectingObject  true when it is safe to return to the main menu
	 */
	public void stateObject(boolean startObject, boolean frameIteration,
	                        boolean firstFrame,  boolean lastFrame,
	                        boolean selectingObject) {
		buttonStartObject     .setEnabled(startObject);
		buttonEndObject       .setEnabled(frameIteration);
		buttonNextFrame       .setEnabled(frameIteration && !lastFrame);
		buttonPreviousFrame   .setEnabled(frameIteration && !firstFrame);
		buttonRestoreSelection.setEnabled(frameIteration && !firstFrame);
		buttonMainMenu        .setEnabled(selectingObject);
		// "Next Segment" is available whenever the user is not mid-object
		buttonNextCell        .setEnabled(selectingObject);
	}

	// ── Cell identity ─────────────────────────────────────────────────────────

	/**
	 * Updates the window title to reflect which parent segment is currently being
	 * segmented.  Called by the controller each time a new segment is loaded.
	 *
	 * @param cellName the display name of the parent LinkSet (e.g. "3", "10.1")
	 */
	public void setCellName(String cellName) {
		setTitle(TITLE_PREFIX + ":  Segment " + cellName);
	}

	// ── Utilities ─────────────────────────────────────────────────────────────

	public void close() {
		// Directly hide and release resources — DO_NOTHING_ON_CLOSE means
		// dispatching WINDOW_CLOSING would silently no-op, leaving the panel open.
		setVisible(false);
		dispose();
	}

	public void dialogAlert(String message) {
		JOptionPane.showMessageDialog((Component) null, message,
		                              "Alert", JOptionPane.WARNING_MESSAGE);
	}

	// ── ActionListener ────────────────────────────────────────────────────────

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == buttonNewSegmentation)  controller.newSegmentation();
		if (e.getSource() == buttonEndSession)        controller.exit();

		if (e.getSource() == buttonStartObject)       controller.startObject();
		if (e.getSource() == buttonNextFrame)         controller.nextFrame();
		if (e.getSource() == buttonPreviousFrame)     controller.previousFrame();
		if (e.getSource() == buttonRestoreSelection)  controller.restoreSelection();
		if (e.getSource() == buttonEndObject)         controller.endObject();

		if (e.getSource() == buttonNextCell)          controller.nextCell();

		if (e.getSource() == buttonModify)            controller.modifyMenu();
		if (e.getSource() == buttonDeleteObject)      controller.deleteSelectedObject();
		if (e.getSource() == buttonMergeObjects)      controller.mergeSelectedObjects();
		if (e.getSource() == buttonRedrawSegment)     controller.startRedrawSegment();
		if (e.getSource() == buttonApplyRedraw)       controller.applyRedrawSegment();
		if (e.getSource() == buttonCancelRedraw)      controller.cancelRedrawSegment();
		if (e.getSource() == buttonSettings)          controller.openSettings();
		if (e.getSource() == buttonRoiColor)          controller.changeRoiColor();
		if (e.getSource() == buttonDrawTool)          controller.toggleDrawTool();
		if (e.getSource() == buttonSettingsBack)      controller.closeSettings();

		if (e.getSource() == buttonMainMenu)          controller.mainMenu();
	}
}
