package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.Tooltips;

/**
 * Dialog window for configuring boundary-cleanup parameters shared by every SARN and
 * internal-segmentation boundary-generation pipeline: how far {@code removeLoops} searches for
 * loop/revisit artifacts (scaled by a boundary's own point count, capped by an absolute
 * ceiling), and the {@code douglasPeucker} shape-fidelity tolerance.
 *
 * <p>For automatic parameter estimation, open <b>Guided Calibration</b>. That panel estimates a
 * starting point from the currently loaded dataset's own boundary point-count distribution, with
 * immediate live-preview feedback (a before/after boundary overlay).
 */
public class BoundaryCleanupPanel extends JFrame implements ActionListener {

	OperationController controller;

	// ── Labels ────────────────────────────────────────────────────────────────
	JLabel labelSearchFraction = new JLabel("Search Distance (%):");
	JLabel labelSearchCeiling  = new JLabel("Search Distance Ceiling (px):");
	JLabel labelEpsilon        = new JLabel("Simplification Epsilon (px):");

	// ── Input fields ──────────────────────────────────────────────────────────
	JTextField searchFraction = new JTextField(" ", 10); // displayed as %
	JTextField searchCeiling  = new JTextField(" ", 10);
	JTextField epsilon        = new JTextField(" ", 10);

	// ── Buttons / controls ────────────────────────────────────────────────────
	JButton guidedCalibrationButton = new JButton("Guided Calibration");
	JButton buttonApply             = new JButton("Apply");

	// ── Status ────────────────────────────────────────────────────────────────
	JLabel calibrationMessage = new JLabel(" ");

	// ── Layout ────────────────────────────────────────────────────────────────
	GridBagConstraints constraints;
	JPanel panel = new JPanel(new GridBagLayout());

	// ── Constructor ───────────────────────────────────────────────────────────

	/**
	 * @param controller the OperationController whose settings this dialog edits
	 */
	public BoundaryCleanupPanel(OperationController controller) {
		super("Boundary Cleanup Settings");
		this.controller = controller;
		initialize();
		createView();
	}

	// ── Initialisation ────────────────────────────────────────────────────────

	/** Populates fields from the controller's current stored values. */
	public void initialize() {
		// Stored as fraction [0-1]; display as percent.
		searchFraction.setText("" + (controller.getSearchFraction() * 100));
		searchCeiling.setText("" + controller.getSearchCeiling());
		epsilon.setText("" + controller.getSimplificationEpsilon());
	}

	// ── View construction ─────────────────────────────────────────────────────

	public void createView() {

		setMinimumSize(new Dimension(200, 200));

		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;

		calibrationMessage.setFont(new Font(
			calibrationMessage.getFont().getName(),
			Font.ITALIC + Font.BOLD,
			calibrationMessage.getFont().getSize()));

		// ── Tooltips ──────────────────────────────────────────────────────────
		labelSearchFraction.setToolTipText(Tooltips.BoundaryCleanup.SEARCH_FRACTION);
		searchFraction     .setToolTipText(Tooltips.BoundaryCleanup.SEARCH_FRACTION);
		labelSearchCeiling .setToolTipText(Tooltips.BoundaryCleanup.SEARCH_CEILING);
		searchCeiling      .setToolTipText(Tooltips.BoundaryCleanup.SEARCH_CEILING);
		labelEpsilon       .setToolTipText(Tooltips.BoundaryCleanup.EPSILON);
		epsilon            .setToolTipText(Tooltips.BoundaryCleanup.EPSILON);

		// ── Rows ──────────────────────────────────────────────────────────────
		int row = 0;

		// ROW — Search Distance %
		constraints.gridy = row++;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.gridx = 0;  panel.add(labelSearchFraction, constraints);
		constraints.gridx = 1;  panel.add(searchFraction,      constraints);

		// ROW — Search Distance Ceiling
		constraints.gridy = row++;
		constraints.gridx = 0;  panel.add(labelSearchCeiling, constraints);
		constraints.gridx = 1;  panel.add(searchCeiling,      constraints);

		// ROW — Simplification Epsilon
		constraints.gridy = row++;
		constraints.gridx = 0;  panel.add(labelEpsilon, constraints);
		constraints.gridx = 1;  panel.add(epsilon,      constraints);

		// ROW — Guided Calibration | Apply
		constraints.gridy = row++;
		constraints.gridx = 0;
		panel.add(guidedCalibrationButton, constraints);

		constraints.gridx = 1;
		panel.add(buttonApply, constraints);

		// ROW — Status message
		constraints.gridy = row;
		constraints.gridx = 0;
		constraints.gridwidth = 3;
		panel.add(calibrationMessage, constraints);
		constraints.gridwidth = 1;

		// ── Wire listeners ────────────────────────────────────────────────────
		guidedCalibrationButton.addActionListener(this);
		buttonApply            .addActionListener(this);

		add(panel);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	// ── Apply ─────────────────────────────────────────────────────────────────

	/** Validates and pushes all field values back to the controller. */
	public void setCalibration() {
		try {
			// Percent entered; store as fraction [0-1].
			double fraction = Double.parseDouble(searchFraction.getText()) / 100.0;
			int ceiling = Integer.parseInt(searchCeiling.getText());
			double eps = Double.parseDouble(epsilon.getText());

			if (fraction <= 0 || ceiling <= 0 || eps <= 0) {
				calibrationMessage.setForeground(Color.RED);
				calibrationMessage.setText("All values must be positive");
				return;
			}

			controller.setSearchFraction(fraction);
			controller.setSearchCeiling(ceiling);
			controller.setSimplificationEpsilon(eps);
			calibrationMessage.setForeground(Color.BLACK);
			calibrationMessage.setText("New values applied");
		} catch (NumberFormatException e) {
			calibrationMessage.setForeground(Color.RED);
			calibrationMessage.setText("All values must be numeric");
		}
	}

	// ── Guided calibration ────────────────────────────────────────────────────

	public void runGuidedCalibration() {
		BoundaryCleanupCalibration calibrate = new BoundaryCleanupCalibration(controller);
		calibrate.run();
	}

	// ── ActionListener ────────────────────────────────────────────────────────

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonApply)             setCalibration();
		if (e.getSource() == guidedCalibrationButton) runGuidedCalibration();
	}
}
