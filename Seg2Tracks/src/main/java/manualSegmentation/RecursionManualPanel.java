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

/**
 * View window for manual recursive (void/subsegmentation) controls.
 * Mirrors ManualSegmentationPanel but adds a "Next Cell" button so the user can
 * advance to the next parent cell when finished segmenting voids in the current one.
 *
 * Panel layout:
 *   Main Menu  : Start Segmentation | End Session
 *   Segment    : Start Object | End Object | Next Frame | Previous Frame |
 *                Restore Selection | Next Cell | Main Menu
 *
 * The window title is updated each time a new parent cell is shown via
 * setCellName(String), displaying the cell's display name in the title bar.
 */
public class RecursionManualPanel extends JFrame implements ActionListener {

	private static final long serialVersionUID = 7712834501029384756L;

	RecursionManualController controller;

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

	/** Advances to the next parent cell without ending the session. */
	JButton buttonNextCell         = new JButton("Next Cell");

	// ── Modification panel ────────────────────────────────────────────────
	JButton buttonModify           = new JButton("Modify");
	JButton buttonDeleteObject     = new JButton("Delete Object");
	JButton buttonMergeObjects     = new JButton("Merge Objects");

	// ── Shared ────────────────────────────────────────────────────────────────
	JButton buttonMainMenu         = new JButton("Main Menu");

	/** Base title prefix — cell name is appended dynamically. */
	private static final String TITLE_PREFIX = "Seg2Tracks — Recursive Segmentation";

	public RecursionManualPanel(RecursionManualController controller) {
		super(TITLE_PREFIX);
		this.controller = controller;
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
		buttonMainMenu        .addActionListener(this);

		add(panel);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	// ── Panel states ──────────────────────────────────────────────────────────

	/** Shows the top-level menu: start segmenting, modify, or end session. */
	public void setMainPanel() {
		panel.removeAll();
		panel.add(buttonNewSegmentation);
		panel.add(buttonModify);
		panel.add(buttonEndSession);
		panel.repaint();
		panel.revalidate();
	}

	/**
	 * Shows the per-cell segmentation controls, including the "Next Cell" button.
	 * Call stateObject() immediately after to set initial button enable states.
	 */
	public void setSegmentPanel() {
		panel.removeAll();
		panel.add(buttonStartObject);
		panel.add(buttonEndObject);
		panel.add(buttonNextFrame);
		panel.add(buttonPreviousFrame);
		panel.add(buttonRestoreSelection);
		panel.add(buttonNextCell);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
	}

	/** Shows the modification controls: delete, merge, or return to main menu. */
	public void setModificationPanel() {
		panel.removeAll();
		panel.add(buttonDeleteObject);
		panel.add(buttonMergeObjects);
		panel.add(buttonNextCell);
		panel.add(buttonMainMenu);
		panel.repaint();
		panel.revalidate();
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
		// "Next Cell" is available whenever the user is not mid-object
		buttonNextCell        .setEnabled(selectingObject);
	}

	// ── Cell identity ─────────────────────────────────────────────────────────

	/**
	 * Updates the window title to reflect which parent cell is currently being
	 * segmented.  Called by the controller each time a new cell is loaded.
	 *
	 * @param cellName the display name of the parent LinkSet (e.g. "3", "10.1")
	 */
	public void setCellName(String cellName) {
		setTitle(TITLE_PREFIX + ":  Cell " + cellName);
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

		if (e.getSource() == buttonMainMenu)          controller.mainMenu();
	}
}
