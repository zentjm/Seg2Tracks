package gui;

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
 * Dialog window for configuring the SARN (external) boundary-cleanup parameters (internal
 * segmentation keeps its traced outline uncleaned; see {@code Segmentation.tracedPerimeter}):
 * how far {@code removeLoops} searches for
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
		searchFraction.setText(SettingsFields.format(controller.getSearchFraction() * 100));
		searchCeiling.setText("" + controller.getSearchCeiling());
		epsilon.setText(SettingsFields.format(controller.getSimplificationEpsilon()));
	}

	/**
	 * Receives values from Guided Calibration's "Send to Settings": fills the boxes only.
	 * Nothing is stored until the user presses Apply here.
	 *
	 * @param fractionPct search distance, in percent
	 * @param ceiling     search distance ceiling (points)
	 * @param eps         simplification epsilon (px)
	 */
	public void receiveCalibration(double fractionPct, int ceiling, double eps) {
		searchFraction.setText(SettingsFields.format(fractionPct));
		searchCeiling.setText("" + ceiling);
		epsilon.setText(SettingsFields.format(eps));
		updateApplyState();
		toFront();
	}

	// ── Current box values (Guided Calibration starts from these) ─────────────
	// Each falls back to the stored setting if its box does not hold a valid number.

	double boxSearchFractionPct() {
		return SettingsFields.parseOr(searchFraction, controller.getSearchFraction() * 100);
	}

	int boxSearchCeiling() {
		return (int) Math.round(SettingsFields.parseOr(searchCeiling, controller.getSearchCeiling()));
	}

	double boxEpsilon() {
		return SettingsFields.parseOr(epsilon, controller.getSimplificationEpsilon());
	}

	/**
	 * Enables Apply only while the boxes hold valid positive numbers that differ from the
	 * stored settings, and shows whether there are unapplied changes.
	 */
	void updateApplyState() {
		double fraction, eps;
		int ceiling;
		try {
			fraction = Double.parseDouble(searchFraction.getText().trim()) / 100.0;
			ceiling  = Integer.parseInt(searchCeiling.getText().trim());
			eps      = Double.parseDouble(epsilon.getText().trim());
		} catch (NumberFormatException e) {
			buttonApply.setEnabled(false);
			SettingsFields.showError(calibrationMessage, "All values must be numeric (ceiling a whole number)");
			return;
		}
		if (fraction <= 0 || ceiling <= 0 || eps <= 0) {
			buttonApply.setEnabled(false);
			SettingsFields.showError(calibrationMessage, "All values must be positive");
			return;
		}
		boolean changed = !SettingsFields.same(fraction, controller.getSearchFraction())
				|| ceiling != controller.getSearchCeiling()
				|| !SettingsFields.same(eps, controller.getSimplificationEpsilon());
		buttonApply.setEnabled(changed);
		if (changed) SettingsFields.showPending(calibrationMessage);
		else SettingsFields.clear(calibrationMessage);
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
		SettingsFields.onEdit(this::updateApplyState, searchFraction, searchCeiling, epsilon);
		updateApplyState();

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
			double fraction = Double.parseDouble(searchFraction.getText().trim()) / 100.0;
			int ceiling = Integer.parseInt(searchCeiling.getText().trim());
			double eps = Double.parseDouble(epsilon.getText().trim());
			if (fraction > 0 && ceiling > 0 && eps > 0) {
				controller.setSearchFraction(fraction);
				controller.setSearchCeiling(ceiling);
				controller.setSimplificationEpsilon(eps);
				initialize(); // show the stored values, normalised
			}
		} catch (NumberFormatException e) {
			// Apply is disabled while a value is invalid; kept as a safeguard.
		}
		updateApplyState();
	}

	// ── Guided calibration ────────────────────────────────────────────────────

	public void runGuidedCalibration() {
		BoundaryCleanupCalibration calibrate = new BoundaryCleanupCalibration(controller, this);
		calibrate.run();
	}

	// ── ActionListener ────────────────────────────────────────────────────────

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonApply)             setCalibration();
		if (e.getSource() == guidedCalibrationButton) runGuidedCalibration();
	}
}
