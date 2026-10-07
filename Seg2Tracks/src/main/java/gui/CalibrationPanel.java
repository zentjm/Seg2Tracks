package gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.Tooltips;

/**
 * Dialog window for configuring object identification (detection) parameters.
 *
 * <p>Two modes are supported, controlled by {@code isRecursive}:
 * <ul>
 *   <li><b>Primary</b> ({@code isRecursive = false}): Sigma and Threshold only.
 *       The recursive tolerance has no effect in primary identification and is
 *       therefore not shown.
 *   <li><b>Recursive</b> ({@code isRecursive = true}): Sigma, Threshold, and
 *       Recursive Tolerance.  All three parameters are used when detecting voids
 *       inside masked parent-cell crops.
 * </ul>
 *
 * <p>For automatic parameter estimation, open <b>Guided Calibration</b>.  That panel
 * provides both LoG scale-space sigma estimation and DataSet-supervised sigma + threshold
 * estimation, with immediate live-preview feedback.
 */
public class CalibrationPanel extends JFrame implements ActionListener {

	OperationController controller;

	/** True when this dialog is opened from a recursive (subsegmentation) panel. */
	boolean isRecursive;

	/** True when the panel already has loaded segmentation data.
	 *  Changing parameters while data is loaded prompts the user to clear it. */
	boolean loadedData;

	// ── Labels ────────────────────────────────────────────────────────────────
	JLabel labelSigma              = new JLabel("Sigma:");
	JLabel labelThreshold          = new JLabel("Threshold (%):");
	JLabel labelRecursiveTolerance = new JLabel("Recursive Tolerance (%):");

	// ── Input fields ──────────────────────────────────────────────────────────
	JTextField gaussianBlurSigma      = new JTextField(" ", 10);
	JTextField maximumFinderTolerance = new JTextField(" ", 10);  // displayed as %
	JTextField recursiveTolerancePct  = new JTextField(" ", 10);  // displayed as %

	// ── Buttons / controls ────────────────────────────────────────────────────
	JCheckBox checkBoxInvertIntensity = new JCheckBox("Invert Intensity");
	JButton   guidedCalibrationButton = new JButton("Guided Calibration");
	JButton   buttonApply             = new JButton("Apply");

	// ── Status ────────────────────────────────────────────────────────────────
	JLabel calibrationMessage = new JLabel(" ");

	// ── Layout ────────────────────────────────────────────────────────────────
	GridBagConstraints constraints;
	JPanel panel = new JPanel(new GridBagLayout());

	// ── Constructor ───────────────────────────────────────────────────────────

	/**
	 * @param controller  the OperationController whose settings this dialog edits
	 * @param loadedData  true if segmentation data is already loaded (warns on change)
	 * @param isRecursive true when opened from a recursive subsegmentation panel;
	 *                    shows the Recursive Tolerance field
	 */
	public CalibrationPanel(OperationController controller, boolean loadedData, boolean isRecursive) {
		super(isRecursive ? "Subsegment Identification Settings" : "Object Identification Settings");
		this.controller  = controller;
		this.loadedData  = loadedData;
		this.isRecursive = isRecursive;
		initialize();
		createView();
	}

	// ── Initialisation ────────────────────────────────────────────────────────

	/** Populates fields from the controller's current stored values. */
	public void initialize() {
		gaussianBlurSigma.setText(SettingsFields.format(controller.getGaussianBlurSigma()));
		// Threshold stored as fraction [0–1]; display as percent.
		maximumFinderTolerance.setText(SettingsFields.format(controller.getMaximumFinderTolerance() * 100));
		if (isRecursive) {
			recursiveTolerancePct.setText(SettingsFields.format(controller.getRecursiveTolerancePct()));
		}
		checkBoxInvertIntensity.setSelected(controller.getInvertIntensity());
	}

	/**
	 * Receives values from Guided Calibration's "Send to Settings": fills the boxes only.
	 * Nothing is stored until the user presses Apply here, so these boxes stay the single
	 * path into the settings (and the loaded-data warning in {@link #setCalibration} applies).
	 *
	 * @param sigma          Gaussian blur sigma
	 * @param thresholdPct   threshold, in percent
	 * @param recTolPct      recursive tolerance in percent (ignored unless recursive)
	 */
	public void receiveCalibration(double sigma, double thresholdPct, double recTolPct) {
		gaussianBlurSigma.setText(SettingsFields.format(sigma));
		maximumFinderTolerance.setText(SettingsFields.format(thresholdPct));
		if (isRecursive) recursiveTolerancePct.setText(SettingsFields.format(recTolPct));
		updateApplyState();
		toFront();
	}

	/**
	 * Enables Apply only while the boxes hold valid numbers that differ from the stored
	 * settings, and shows whether there are unapplied changes.
	 */
	void updateApplyState() {
		boolean changed;
		try {
			double sigma  = Double.parseDouble(gaussianBlurSigma.getText().trim());
			double thresh = Double.parseDouble(maximumFinderTolerance.getText().trim()) / 100.0;
			changed = !SettingsFields.same(sigma, controller.getGaussianBlurSigma())
					|| !SettingsFields.same(thresh, controller.getMaximumFinderTolerance())
					|| checkBoxInvertIntensity.isSelected() != controller.getInvertIntensity();
			if (isRecursive) {
				double recTol = Double.parseDouble(recursiveTolerancePct.getText().trim());
				changed |= !SettingsFields.same(recTol, controller.getRecursiveTolerancePct());
			}
		} catch (NumberFormatException e) {
			buttonApply.setEnabled(false);
			SettingsFields.showError(calibrationMessage, "All values must be numeric");
			return;
		}
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
		labelSigma            .setToolTipText(Tooltips.ObjectID.SIGMA);
		gaussianBlurSigma     .setToolTipText(Tooltips.ObjectID.SIGMA);
		labelThreshold        .setToolTipText(Tooltips.ObjectID.THRESHOLD);
		maximumFinderTolerance.setToolTipText(Tooltips.ObjectID.THRESHOLD);
		labelRecursiveTolerance.setToolTipText(Tooltips.ObjectID.RECURSIVE_TOLERANCE);
		recursiveTolerancePct .setToolTipText(Tooltips.ObjectID.RECURSIVE_TOLERANCE);

		// ── Rows ──────────────────────────────────────────────────────────────
		// Use a running row counter so adding/removing optional rows never
		// requires renumbering the rows that follow.
		int row = 0;

		// ROW — Sigma
		constraints.gridy = row++;
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.gridx = 0;  panel.add(labelSigma,        constraints);
		constraints.gridx = 1;  panel.add(gaussianBlurSigma, constraints);

		// ROW — Threshold
		constraints.gridy = row++;
		constraints.gridx = 0;  panel.add(labelThreshold,          constraints);
		constraints.gridx = 1;  panel.add(maximumFinderTolerance,  constraints);

		// ROW — Recursive Tolerance (recursive panels only)
		if (isRecursive) {
			constraints.gridy = row++;
			constraints.gridx = 0;  panel.add(labelRecursiveTolerance, constraints);
			constraints.gridx = 1;  panel.add(recursiveTolerancePct,   constraints);
		}

		// ROW — Invert Intensity
		constraints.gridy = row++;
		constraints.gridx = 0;
		constraints.gridwidth = 2;
		panel.add(checkBoxInvertIntensity, constraints);
		constraints.gridwidth = 1;

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
		checkBoxInvertIntensity.addActionListener(this);
		guidedCalibrationButton.addActionListener(this);
		buttonApply            .addActionListener(this);
		SettingsFields.onEdit(this::updateApplyState,
				gaussianBlurSigma, maximumFinderTolerance, recursiveTolerancePct);
		updateApplyState();

		add(panel);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	// ── Apply ─────────────────────────────────────────────────────────────────

	/** Validates and pushes all field values back to the controller. */
	public void setCalibration() {
		if (loadedData) {
			Object[] options = {"Ok", "Cancel"};
			int choice = JOptionPane.showOptionDialog(null,
				"Resetting calibration will remove currently loaded segmentations", "Warning",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
				null, options, options[1]);
			if (choice == 1) return;
			controller.clearData(2); //TODO: enum
			loadedData = false; // cleared; don't warn again on the next Apply
		}

		try {
			controller.setGaussianBlurSigma(Double.parseDouble(gaussianBlurSigma.getText().trim()));
			// Threshold entered as percent; store as fraction [0–1].
			controller.setMaximumFinderTolerance(
				Double.parseDouble(maximumFinderTolerance.getText().trim()) / 100.0);
			if (isRecursive) {
				controller.setRecursiveTolerancePct(
					Double.parseDouble(recursiveTolerancePct.getText().trim()));
			}
			controller.setInvertIntensity(checkBoxInvertIntensity.isSelected());
			initialize(); // show the stored values, normalised
		} catch (NumberFormatException e) {
			// Apply is disabled while a value is invalid; kept as a safeguard.
		}
		updateApplyState();
	}

	// ── Guided calibration ────────────────────────────────────────────────────

	public void runGuidedCalibration() {
		GuidedCalibration calibrate = new GuidedCalibration(controller, isRecursive, this);
		calibrate.run();
	}

	// ── ActionListener ────────────────────────────────────────────────────────

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonApply)             setCalibration();
		if (e.getSource() == guidedCalibrationButton) runGuidedCalibration();
		// Invert Intensity is applied with the other values (Apply), not immediately.
		if (e.getSource() == checkBoxInvertIntensity) updateApplyState();
	}
}
