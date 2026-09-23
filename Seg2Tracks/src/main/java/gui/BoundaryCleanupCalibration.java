package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import identification.AutoCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.process.ImageProcessor;

/**
 * Live-preview calibration tool for Boundary Cleanup parameters ({@code removeLoops}
 * search-distance scaling and {@code douglasPeucker} shape-fidelity tolerance).
 *
 * <p>Opens a Fiji ImagePlus window showing a single frame with one representative object's
 * boundary overlaid twice: <b>red</b> is the object's currently stored boundary re-densified
 * back to full pixel resolution (undoing whatever simplification already produced it, so the
 * effect of the current slider values is visible), <b>green</b> is that same boundary re-run
 * through {@code removeLoops}/{@code douglasPeucker} at the current slider values. Two sliders —
 * Search Distance % and Simplification Epsilon — plus a Search Distance Ceiling slider let the
 * user tune interactively; cleanup is re-run each time a slider is <em>released</em>.
 *
 * <p><b>Honest limitation</b>: the true pre-cleanup boundary (the raw SARN/internal-segmentation
 * output before any {@code removeLoops} pass ever ran) is not persisted anywhere — only the
 * final, already-cleaned boundary is stored on a {@link Segment}. This preview therefore
 * demonstrates the effect of the current slider values on real dataset geometry (point-count
 * reduction, corner/shape preservation), which is the primary tuning signal Douglas-Peucker's
 * {@code epsilon} needs. It does <em>not</em> reproduce a genuine loop-artifact collapse, since a
 * boundary that was already cleaned typically has no remaining loop defects to remove — a search
 * fraction change showing little visible effect on the red/green overlay is expected in that case,
 * not a bug.
 *
 * <p>"Apply to Settings" pushes the current slider values back to the {@link OperationController},
 * exactly as clicking "Apply" in the parent {@link BoundaryCleanupPanel} would.
 */
public class BoundaryCleanupCalibration extends JFrame implements ChangeListener, ActionListener {

	/** Slider ticks per percentage point: {@code sliderValue / SEARCH_FRACTION_SCALE = percent}. */
	private static final int SEARCH_FRACTION_SCALE = 10; // 0.1% resolution per tick

	/** Slider ticks per pixel: {@code sliderValue / EPSILON_SCALE = epsilon (px)}. */
	private static final int EPSILON_SCALE = 10; // 0.1 px resolution per tick

	// ── State ─────────────────────────────────────────────────────────────────

	private final OperationController controller;
	private ImageStack virtualStack;
	private ImagePlus previewPlus;
	private Point[] previewBoundary; // the stored (already-cleaned) boundary being re-previewed
	private int previewFrame1Based;  // 1-based virtualStack frame the preview boundary came from

	/** Suppresses {@link #refreshPreview()} during a programmatic slider update. */
	private boolean suppressRefresh = false;

	// ── Sliders ───────────────────────────────────────────────────────────────

	private JSlider sliderSearchFraction;
	private JSlider sliderSearchCeiling;
	private JSlider sliderEpsilon;

	// ── Value labels ──────────────────────────────────────────────────────────

	private JLabel labelSearchFractionVal;
	private JLabel labelSearchCeilingVal;
	private JLabel labelEpsilonVal;

	// ── Auto-calibration ──────────────────────────────────────────────────────

	private JButton btnAutoCalibrate;

	// ── Status / buttons ──────────────────────────────────────────────────────

	private JLabel  statusLabel;
	private JButton buttonApply;
	private JButton buttonClose;

	// ── Constructor ───────────────────────────────────────────────────────────

	public BoundaryCleanupCalibration(OperationController controller) {
		super("Guided Calibration — Boundary Cleanup");
		this.controller = controller;
	}

	// ── Entry point ───────────────────────────────────────────────────────────

	/**
	 * Opens the guided calibration window and live preview. Requires a loaded input file and an
	 * existing external segmentation, since Boundary Cleanup previews against already-generated
	 * boundaries — there is nothing to preview if no boundary has been generated yet.
	 */
	public void run() {
		String path = controller.getInputFilePath();
		if (path == null || path.isEmpty()) {
			JOptionPane.showMessageDialog(null,
				"No input file selected.\nPlease select an input file before opening Guided Calibration.",
				"No Input File", JOptionPane.WARNING_MESSAGE);
			return;
		}
		DataSet ds = controller.getDataSet();
		if (ds == null || !ds.getExternalSegmentationExists()) {
			JOptionPane.showMessageDialog(null,
				"No external segmentation found.\nRun segmentation at least once before calibrating"
				+ " Boundary Cleanup — this tool previews the effect of parameters on already-generated boundaries.",
				"No Boundary Data", JOptionPane.WARNING_MESSAGE);
			return;
		}
		ImagePlus virt = IJ.openVirtual(path);
		if (virt == null) {
			JOptionPane.showMessageDialog(null,
				"Could not open input file:\n" + path,
				"File Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		virtualStack = virt.getImageStack();

		if (!findPreviewBoundary(ds)) {
			JOptionPane.showMessageDialog(null,
				"No usable boundary found in the loaded DataSet.",
				"No Boundary Data", JOptionPane.WARNING_MESSAGE);
			return;
		}

		createView();
		refreshPreview();
	}

	/**
	 * Finds one representative segment's external perimeter to preview, starting from the frame
	 * closest to whatever ImageJ window is currently active and searching outward until a frame
	 * with at least one usable boundary is found. Sets {@link #previewBoundary} and
	 * {@link #previewFrame1Based} on success.
	 * @return true if a usable boundary was found
	 */
	private boolean findPreviewBoundary(DataSet ds) {
		int startFrame0 = detectCurrentFrame() - 1; // convert to 0-based DataSet frame index
		FrameSet[] frameSets = ds.getFrameSetList();
		if (frameSets == null) return false;

		for (int offset = 0; offset < frameSets.length; offset++) {
			for (int sign = -1; sign <= 1; sign += 2) {
				if (offset == 0 && sign == 1) continue; // avoid checking frame 0 twice
				int fi = startFrame0 + sign * offset;
				if (fi < 0 || fi >= frameSets.length || frameSets[fi] == null) continue;
				for (Segment seg : frameSets[fi]) {
					Point[] perim = seg.getExternalPerimeter();
					if (perim != null && perim.length >= 3) {
						previewBoundary = perim;
						previewFrame1Based = fi + 1;
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Returns the 1-based frame to start searching from — mirrors GuidedCalibration's logic. */
	private int detectCurrentFrame() {
		ImagePlus active = WindowManager.getCurrentImage();
		if (active != null) {
			int n = Math.max(active.getNSlices(), active.getNFrames());
			if (n == virtualStack.getSize()) {
				int pos = (active.getNSlices() > 1) ? active.getCurrentSlice() : active.getT();
				if (pos >= 1 && pos <= virtualStack.getSize()) return pos;
			}
		}
		return Math.max(1, (virtualStack.getSize() + 1) / 2); // middle frame
	}

	// ── View construction ─────────────────────────────────────────────────────

	private void createView() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(true);

		int initFraction = clamp((int) Math.round(controller.getSearchFraction() * 100 * SEARCH_FRACTION_SCALE),
		                          1, 50 * SEARCH_FRACTION_SCALE);
		int initCeiling  = clamp(controller.getSearchCeiling(), 50, 2000);
		int initEpsilon  = clamp((int) Math.round(controller.getSimplificationEpsilon() * EPSILON_SCALE),
		                          1, 50);

		sliderSearchFraction = new JSlider(1, 50 * SEARCH_FRACTION_SCALE, initFraction);
		sliderSearchCeiling  = new JSlider(50, 2000, initCeiling);
		sliderEpsilon        = new JSlider(1, 50, initEpsilon);

		configTicks(sliderSearchFraction, 10 * SEARCH_FRACTION_SCALE, 5 * SEARCH_FRACTION_SCALE);
		configTicks(sliderSearchCeiling, 500, 100);
		configTicks(sliderEpsilon, 10, 5);

		labelSearchFractionVal = valueLabel(fractionText(initFraction));
		labelSearchCeilingVal  = valueLabel(String.valueOf(initCeiling));
		labelEpsilonVal        = valueLabel(epsilonText(initEpsilon));

		sliderSearchFraction.addChangeListener(this);
		sliderSearchCeiling .addChangeListener(this);
		sliderEpsilon        .addChangeListener(this);

		btnAutoCalibrate = new JButton("Auto-calibrate from DataSet");
		btnAutoCalibrate.setToolTipText(
				"<html>Estimate Search Distance % and Ceiling from the currently loaded DataSet's<br>"
				+ "own boundary point-count distribution. Updates those two sliders, then refreshes<br>"
				+ "the preview. Does not estimate Epsilon — tune that visually.</html>");
		btnAutoCalibrate.addActionListener(this);

		statusLabel = new JLabel(" ");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
		buttonApply = new JButton("Apply to Settings");
		buttonClose = new JButton("Close");
		buttonApply.addActionListener(this);
		buttonClose.addActionListener(this);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 6, 3, 6);
		c.anchor = GridBagConstraints.WEST;
		int row = 0;

		addSliderRow(panel, c, row++, "Search Distance (%):", sliderSearchFraction, labelSearchFractionVal);
		addSliderRow(panel, c, row++, "Search Distance Ceiling (px):", sliderSearchCeiling, labelSearchCeilingVal);
		addSliderRow(panel, c, row++, "Simplification Epsilon (px):", sliderEpsilon, labelEpsilonVal);

		c.gridy = row++;
		panel.add(statusLabel, c);

		JPanel autoCalRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
		autoCalRow.add(btnAutoCalibrate);
		c.gridy = row++;
		panel.add(autoCalRow, c);

		JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
		buttons.add(buttonApply);
		buttons.add(buttonClose);
		c.gridy = row;
		panel.add(buttons, c);

		setContentPane(panel);
		pack();
		setMinimumSize(new Dimension(500, getHeight()));
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private static void addSliderRow(JPanel panel, GridBagConstraints c, int row,
	                                  String labelText, JSlider slider, JLabel val) {
		c.gridy = row; c.gridwidth = 1;
		c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
		panel.add(new JLabel(labelText), c);
		c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(slider, c);
		c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
		panel.add(val, c);
	}

	private static void configTicks(JSlider s, int major, int minor) {
		s.setPaintTicks(true);
		s.setMajorTickSpacing(major);
		s.setMinorTickSpacing(minor);
	}

	private static JLabel valueLabel(String text) {
		JLabel lbl = new JLabel(text);
		lbl.setPreferredSize(new Dimension(50, lbl.getPreferredSize().height));
		lbl.setHorizontalAlignment(JLabel.RIGHT);
		return lbl;
	}

	// ── Preview ───────────────────────────────────────────────────────────────

	/**
	 * Re-densifies the stored preview boundary, re-runs {@code removeLoops}/{@code douglasPeucker}
	 * at the current slider values, and redraws both as overlays (red = before, green = after) on
	 * the preview ImagePlus.
	 */
	private void refreshPreview() {
		double fraction = sliderSearchFraction.getValue() / (double) (SEARCH_FRACTION_SCALE * 100);
		int    ceiling  = sliderSearchCeiling.getValue();
		double epsilon  = sliderEpsilon.getValue() / (double) EPSILON_SCALE;

		ImageProcessor display = virtualStack.getProcessor(previewFrame1Based).duplicate();

		String title = "Boundary Cleanup Preview — Frame " + previewFrame1Based;
		if (previewPlus == null || !previewPlus.isVisible()) {
			previewPlus = new ImagePlus(title, display.duplicate());
			previewPlus.show();
		} else {
			previewPlus.setTitle(title);
			previewPlus.setProcessor(display.duplicate());
		}

		Point[] before = GeometricCalculations.straightPerimeter(previewBoundary);
		int searchDistance = GeometricCalculations.scaledSearchDistance(before.length, fraction, ceiling);
		Point[] after = GeometricCalculations.straightPerimeter(
				GeometricCalculations.douglasPeucker(
				GeometricCalculations.removeLoops(before, searchDistance,
						GeometricCalculations.LOOP_REMOVAL_RANGE, GeometricCalculations.LOOP_REMOVAL_SMOOTHING),
				epsilon));

		Overlay overlay = new Overlay();
		PolygonRoi beforeRoi = GeometricCalculations.getPolygonRoi(before);
		beforeRoi.setStrokeColor(Color.RED);
		beforeRoi.setStrokeWidth(1.0);
		overlay.add(beforeRoi);

		PolygonRoi afterRoi = GeometricCalculations.getPolygonRoi(after);
		afterRoi.setStrokeColor(Color.GREEN);
		afterRoi.setStrokeWidth(2.0);
		overlay.add(afterRoi);

		previewPlus.setOverlay(overlay);
		previewPlus.updateAndRepaintWindow();

		statusLabel.setText("Before: " + before.length + " pts (red)   After: " + after.length
				+ " pts (green)   Search window: " + searchDistance + " pts");
	}

	// ── Listeners ─────────────────────────────────────────────────────────────

	@Override
	public void stateChanged(ChangeEvent e) {
		updateValueLabels();
		JSlider src = (JSlider) e.getSource();
		if (!suppressRefresh && !src.getValueIsAdjusting()) {
			refreshPreview();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonApply)       applyToSettings();
		if (e.getSource() == buttonClose)       close();
		if (e.getSource() == btnAutoCalibrate)  runAutoCalibrate();
	}

	private void updateValueLabels() {
		labelSearchFractionVal.setText(fractionText(sliderSearchFraction.getValue()));
		labelSearchCeilingVal .setText(String.valueOf(sliderSearchCeiling.getValue()));
		labelEpsilonVal        .setText(epsilonText(sliderEpsilon.getValue()));
	}

	private static String fractionText(int sliderVal) {
		return String.format("%.1f", sliderVal / (double) SEARCH_FRACTION_SCALE);
	}

	private static String epsilonText(int sliderVal) {
		return String.format("%.1f", sliderVal / (double) EPSILON_SCALE);
	}

	// ── Apply / close ─────────────────────────────────────────────────────────

	private void applyToSettings() {
		controller.setSearchFraction(sliderSearchFraction.getValue() / (double) (SEARCH_FRACTION_SCALE * 100));
		controller.setSearchCeiling(sliderSearchCeiling.getValue());
		controller.setSimplificationEpsilon(sliderEpsilon.getValue() / (double) EPSILON_SCALE);
		statusLabel.setText("Settings applied.");
	}

	// ── Auto-calibration ──────────────────────────────────────────────────────

	/**
	 * Estimates Search Distance %/Ceiling from the currently loaded DataSet's own boundary
	 * point-count distribution via {@link AutoCalibration#estimateSearchFraction}. Does not
	 * estimate Epsilon — there is no data-driven signal for shape-fidelity tolerance the way
	 * there is for search-window scaling; the user tunes that visually against the overlay.
	 */
	private void runAutoCalibrate() {
		DataSet ds = controller.getDataSet();
		if (ds == null || !ds.getExternalSegmentationExists()) {
			statusLabel.setText("No external segmentation DataSet loaded.");
			return;
		}

		setAllControlsEnabled(false);
		statusLabel.setText("Auto-calibrating from DataSet…");

		new SwingWorker<double[], Void>() {
			@Override
			protected double[] doInBackground() {
				return AutoCalibration.estimateSearchFraction(ds);
			}

			@Override
			protected void done() {
				try {
					double[] result = get();
					if (result == null) {
						statusLabel.setText("No usable boundary data found in DataSet.");
						setAllControlsEnabled(true);
						return;
					}
					double fraction = result[0];
					int    ceiling  = (int) Math.round(result[1]);

					int fractionVal = clamp((int) Math.round(fraction * 100 * SEARCH_FRACTION_SCALE),
					                         sliderSearchFraction.getMinimum(), sliderSearchFraction.getMaximum());
					int ceilingVal  = clamp(ceiling, sliderSearchCeiling.getMinimum(), sliderSearchCeiling.getMaximum());

					suppressRefresh = true;
					sliderSearchFraction.setValue(fractionVal);
					sliderSearchCeiling .setValue(ceilingVal);
					suppressRefresh = false;
					updateValueLabels();
					refreshPreview();
					statusLabel.setText(String.format(
							"Calibrated: search=%.1f%%, ceiling=%d px — click \"Apply to Settings\" to commit.",
							fraction * 100, ceiling));
				} catch (InterruptedException | ExecutionException ex) {
					Throwable cause = ex.getCause();
					statusLabel.setText("Auto-calibration failed: "
							+ (cause != null ? cause.getMessage() : ex.getMessage()));
				}
				setAllControlsEnabled(true);
			}
		}.execute();
	}

	private void setAllControlsEnabled(boolean enabled) {
		sliderSearchFraction.setEnabled(enabled);
		sliderSearchCeiling .setEnabled(enabled);
		sliderEpsilon        .setEnabled(enabled);
		buttonApply          .setEnabled(enabled);
		buttonClose          .setEnabled(enabled);
		btnAutoCalibrate     .setEnabled(enabled);
	}

	private void close() {
		if (previewPlus != null && previewPlus.isVisible()) previewPlus.close();
		dispose();
	}

	@Override
	public void dispose() {
		if (previewPlus != null && previewPlus.isVisible()) previewPlus.close();
		super.dispose();
	}

	// ── Utilities ─────────────────────────────────────────────────────────────

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
