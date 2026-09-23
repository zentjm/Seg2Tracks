package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dataStructure.DataSet;
import geometricTools.ModifiedMaximumFinder;
import identification.AutoCalibration;
import identification.Identification;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 * Live-preview calibration tool for Object Identification parameters.
 *
 * <p>Opens a Fiji ImagePlus window showing a single frame from the input stack with
 * detected objects highlighted as red circles.  Three sliders — Sigma, Threshold,
 * and (in recursive mode) Recursive Tolerance — let the user tune parameters
 * interactively.  Detection is re-run each time a slider is <em>released</em>;
 * value labels update continuously while dragging for immediate feedback.
 *
 * <p>A <b>Stepwise Adjustment</b> section provides "Increase" and "Decrease" buttons
 * for each tunable slider.  Each press steps the parameter until the number of
 * detected objects changes by at least one, using a background {@link SwingWorker}
 * so the UI remains responsive.
 *
 * <p>Only the currently selected frame is processed (not the full stack), keeping
 * response times fast.  The frame can be changed via its own slider.
 *
 * <p>"Apply to Settings" pushes the current slider values back to the
 * {@link OperationController}, exactly as clicking "Apply" in the parent
 * {@link CalibrationPanel} would.
 *
 * <h3>Detection pipeline (per refresh)</h3>
 * <ol>
 *   <li>Duplicate the selected frame; apply intensity inversion if enabled.
 *   <li>Gaussian blur the copy with {@code sigma}.
 *   <li>In recursive mode: compute noise tolerance as
 *       {@code (frameMax − frameMin) × recursiveTolerancePct / 100}.
 *       In primary mode: tolerance = 0 (all local maxima are candidates).
 *   <li>{@link ModifiedMaximumFinder#getMaxima(ImageProcessor, double, boolean)} →
 *       candidate Polygon.  If the raw candidate count exceeds {@link #MAX_DETECTIONS},
 *       the refresh is aborted and the status label explains why.
 *   <li>{@link Identification#filterLowPoints(Polygon, ImageProcessor)} with
 *       {@code threshold} → filtered Polygon.
 *   <li>Draw a red filled circle overlay at each surviving point.
 * </ol>
 */
public class GuidedCalibration extends JFrame implements ChangeListener, ActionListener {

	/** Slider ticks per sigma unit: {@code sliderValue / SIGMA_SCALE = sigma (px)}. */
	private static final int SIGMA_SCALE = 2; // 0.5 px resolution per tick

	/**
	 * Maximum raw-candidate count before a detection pass is aborted.
	 *
	 * <p>If {@link ModifiedMaximumFinder#getMaxima} returns more than this many
	 * candidates the refresh or stepwise step is aborted and the status label
	 * explains why.  This prevents the UI from becoming unresponsive or crashing
	 * when Sigma or Threshold is set to a value that generates thousands of false
	 * detections.
	 *
	 * <p>Set at 2× the maximum expected real object count (≈500) to accommodate
	 * high-density images without blocking legitimate detections.
	 */
	private static final int MAX_DETECTIONS = 1000;

	/**
	 * Maximum wall-clock time (ms) allowed for a single detection step during
	 * stepwise adjustment before the search is aborted.
	 *
	 * <p>This calibrates the guard to processing power rather than detection count
	 * alone: a slow machine may struggle with 500 candidates while a fast one can
	 * handle 1 000 without issue.  If any single {@link #quickCount} call exceeds
	 * this budget, the search stops and the status label reports how long it took.
	 */
	private static final long STEPWISE_TIMEOUT_MS = 5_000L;

	// ── State ─────────────────────────────────────────────────────────────────

	private final OperationController controller;
	private final boolean isRecursive;
	private ImageStack virtualStack;
	private ImagePlus previewPlus;

	/**
	 * When {@code true}, the {@link ChangeListener} on each slider suppresses the
	 * automatic {@link #refreshPreview()} call.  Set during stepwise adjustment so
	 * a single programmatic {@code setValue()} does not trigger an extra refresh.
	 */
	private boolean suppressRefresh = false;

	// ── Sliders ───────────────────────────────────────────────────────────────

	private JSlider sliderFrame;
	private JSlider sliderSigma;
	private JSlider sliderThreshold;
	private JSlider sliderRecTol;      // recursive mode only

	// ── Value labels (update live while dragging) ─────────────────────────────

	private JLabel labelFrameVal;
	private JLabel labelSigmaVal;
	private JLabel labelThresholdVal;
	private JLabel labelRecTolVal;     // recursive mode only

	// ── Stepwise adjustment buttons ───────────────────────────────────────────

	/**
	 * "Increase" / "Decrease" buttons for each tunable slider.
	 * "Increase" steps the slider in the direction that adds detected objects;
	 * "Decrease" steps it in the direction that removes detected objects.
	 * Both search one tick at a time until the filtered count changes by ≥ 1.
	 */
	private JButton btnSigmaInc,    btnSigmaDec;
	private JButton btnThreshInc,   btnThreshDec;
	private JButton btnRecTolInc,   btnRecTolDec;   // recursive mode only

	// ── Auto-calibration buttons ─────────────────────────────────────────────

	/** Estimates sigma via LoG scale-space; uses the already-open {@link #virtualStack}. */
	private JButton btnAutoCalibrate;

	/**
	 * Estimates sigma + threshold from the loaded ground-truth DataSet.
	 * Enabled only when a DataSet with external perimeter data is present.
	 */
	private JButton btnAutoCalibrateFromDataSet;

	// ── Status / buttons ──────────────────────────────────────────────────────

	private JLabel  statusLabel;
	private JButton buttonApply;
	private JButton buttonClose;

	// ── Constructor ───────────────────────────────────────────────────────────

	/**
	 * @param controller  the controller whose settings this dialog previews / updates
	 * @param isRecursive true when opened from a recursive (subsegmentation) panel;
	 *                    shows the Recursive Tolerance slider and uses it for detection
	 */
	public GuidedCalibration(OperationController controller, boolean isRecursive) {
		super(isRecursive ? "Guided Calibration — Subsegment Identification"
		                  : "Guided Calibration — Object Identification");
		this.controller  = controller;
		this.isRecursive = isRecursive;
	}

	// ── Entry point ───────────────────────────────────────────────────────────

	/**
	 * Opens the guided calibration window and live preview.
	 * Loads the input stack virtually (no full-stack memory load), detects the
	 * currently active frame in ImageJ, then builds the slider UI and runs an
	 * initial detection pass.
	 */
	public void run() {
		String path = controller.getInputFilePath();
		if (path == null || path.isEmpty()) {
			JOptionPane.showMessageDialog(null,
				"No input file selected.\nPlease select an input file before opening Guided Calibration.",
				"No Input File", JOptionPane.WARNING_MESSAGE);
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

		createView(detectCurrentFrame());
		refreshPreview();
	}

	// ── Frame detection ───────────────────────────────────────────────────────

	/**
	 * Returns the 1-based frame to use as the initial preview frame.
	 * Reads the current slice of the active ImageJ window if its depth matches the
	 * input stack; otherwise defaults to the middle frame.
	 */
	private int detectCurrentFrame() {
		ImagePlus active = WindowManager.getCurrentImage();
		if (active != null) {
			int n = Math.max(active.getNSlices(), active.getNFrames());
			if (n == virtualStack.getSize()) {
				int pos = (active.getNSlices() > 1)
				        ? active.getCurrentSlice()
				        : active.getT();
				if (pos >= 1 && pos <= virtualStack.getSize()) return pos;
			}
		}
		return Math.max(1, (virtualStack.getSize() + 1) / 2); // middle frame
	}

	// ── View construction ─────────────────────────────────────────────────────

	private void createView(int initialFrame) {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(true);

		// Seed sliders from current controller values
		int initSigma  = clamp((int) Math.round(controller.getGaussianBlurSigma() * SIGMA_SCALE),
		                       1, 100);
		int initThresh = clamp((int) Math.round(controller.getMaximumFinderTolerance() * 100),
		                       0, 100);
		int initRecTol = clamp((int) Math.round(controller.getRecursiveTolerancePct()),
		                       0, 50);

		// Sliders
		sliderFrame     = new JSlider(1, Math.max(1, virtualStack.getSize()), initialFrame);
		sliderSigma     = new JSlider(1, 100, initSigma);
		sliderThreshold = new JSlider(0, 100, initThresh);

		configTicks(sliderFrame,     Math.max(1, virtualStack.getSize() / 5), 1);
		configTicks(sliderSigma,     20, 2);   // major every 10.0 σ, minor every 1.0 σ
		configTicks(sliderThreshold, 20, 5);   // major every 20 %, minor every 5 %

		// Value labels
		labelFrameVal     = valueLabel(String.valueOf(initialFrame));
		labelSigmaVal     = valueLabel(sigmaText(initSigma));
		labelThresholdVal = valueLabel(initThresh + "%");

		// Wire slider listeners
		sliderFrame    .addChangeListener(this);
		sliderSigma    .addChangeListener(this);
		sliderThreshold.addChangeListener(this);

		// Stepwise adjustment buttons for Sigma and Threshold.
		// "Increase" steps the slider value up (+1); "Decrease" steps it down (-1).
		// For all tunable parameters (Sigma, Threshold, RecTol), a higher slider value
		// produces fewer detections and a lower value produces more detections.
		btnSigmaInc = stepButton("Increase");
		btnSigmaDec = stepButton("Decrease");
		btnSigmaInc.addActionListener(e -> runStepwise(sliderSigma, +1));
		btnSigmaDec.addActionListener(e -> runStepwise(sliderSigma, -1));

		btnThreshInc = stepButton("Increase");
		btnThreshDec = stepButton("Decrease");
		btnThreshInc.addActionListener(e -> runStepwise(sliderThreshold, +1));
		btnThreshDec.addActionListener(e -> runStepwise(sliderThreshold, -1));

		// Auto-calibration buttons
		btnAutoCalibrate = new JButton("Auto-calibrate");
		btnAutoCalibrate.setToolTipText(
				"<html>Estimate sigma automatically using Laplacian-of-Gaussian scale-space analysis.<br>"
				+ "Requires no segmentation data.  Updates the Sigma slider only, then refreshes the preview.</html>");

		btnAutoCalibrateFromDataSet = new JButton("Auto-calibrate from DataSet");
		btnAutoCalibrateFromDataSet.setToolTipText(
				"<html>Estimate sigma and threshold from the currently loaded ground-truth segmentation.<br>"
				+ "Requires a DataSet with external perimeter data.<br>"
				+ "Updates both Sigma and Threshold sliders, then refreshes the preview.</html>");
		btnAutoCalibrateFromDataSet.setEnabled(controller.externalSegmentationExists());

		btnAutoCalibrate           .addActionListener(this);
		btnAutoCalibrateFromDataSet.addActionListener(this);

		// Status & buttons
		statusLabel = new JLabel(" ");
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
		buttonApply = new JButton("Apply to Settings");
		buttonClose = new JButton("Close");
		buttonApply.addActionListener(this);
		buttonClose.addActionListener(this);

		// Layout
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 6, 3, 6);
		c.anchor = GridBagConstraints.WEST;
		int row = 0;

		// Frame row — only shown when the stack has multiple frames; no stepwise buttons
		if (virtualStack.getSize() > 1) {
			addSliderRow(panel, c, row++, "Frame:", sliderFrame, labelFrameVal, null);
		}

		// ── Separator + "Stepwise Adjustment" header (above the button column) ─
		c.gridy = row++; c.gridx = 0; c.gridwidth = 4;
		c.fill  = GridBagConstraints.HORIZONTAL;
		panel.add(new JSeparator(), c);
		c.gridwidth = 1; c.fill = GridBagConstraints.NONE;

		String stepTip = "<html>"
			+ "Steps the parameter one tick at a time until the number of detected<br>"
			+ "objects changes by at least one.<br>"
			+ "<br>"
			+ "<b>Decrease</b> — removes one detected object (tightens the parameter).<br>"
			+ "<b>Increase</b> — adds one more detected object (loosens the parameter).<br>"
			+ "<br>"
			+ "Runs in the background; all controls are disabled during the search."
			+ "</html>";
		c.gridy = row++;
		c.gridx = 3; // position directly above the button column
		JLabel stepLabel = new JLabel("Stepwise Adjustment");
		stepLabel.setFont(stepLabel.getFont().deriveFont(Font.BOLD));
		stepLabel.setToolTipText(stepTip);
		ToolTipManager.sharedInstance().setDismissDelay(30000);
		panel.add(stepLabel, c);

		// Sigma row (with stepwise buttons)
		addSliderRow(panel, c, row++, "Sigma:", sliderSigma, labelSigmaVal,
				makeStepPanel(btnSigmaDec, btnSigmaInc));

		// Threshold row (with stepwise buttons)
		addSliderRow(panel, c, row++, "Threshold (%):", sliderThreshold, labelThresholdVal,
				makeStepPanel(btnThreshDec, btnThreshInc));

		// Recursive Tolerance row — recursive panels only
		if (isRecursive) {
			sliderRecTol   = new JSlider(0, 50, initRecTol);
			labelRecTolVal = valueLabel(initRecTol + "%");
			configTicks(sliderRecTol, 10, 5); // major every 10 %, minor every 5 %
			sliderRecTol.addChangeListener(this);

			btnRecTolInc = stepButton("Increase");
			btnRecTolDec = stepButton("Decrease");
			btnRecTolInc.addActionListener(e -> runStepwise(sliderRecTol, +1));
			btnRecTolDec.addActionListener(e -> runStepwise(sliderRecTol, -1));

			addSliderRow(panel, c, row++, "Recursive Tolerance (%):", sliderRecTol, labelRecTolVal,
					makeStepPanel(btnRecTolDec, btnRecTolInc));
		}

		// Separator + status
		c.gridy = row++; c.gridx = 0; c.gridwidth = 4;
		c.fill  = GridBagConstraints.HORIZONTAL;
		panel.add(new JSeparator(), c);

		c.gridy = row++;
		panel.add(statusLabel, c);

		// Auto-calibrate button row (left-aligned, full width)
		JPanel autoCalRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		autoCalRow.add(btnAutoCalibrate);
		autoCalRow.add(btnAutoCalibrateFromDataSet);
		c.gridy = row++;
		panel.add(autoCalRow, c);

		// Apply / Close button row (right-aligned)
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
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

	/** Adds a label | slider | value-label | [extra] row using GridBagLayout. */
	private static void addSliderRow(JPanel panel, GridBagConstraints c,
	                                 int row, String labelText,
	                                 JSlider slider, JLabel val,
	                                 JPanel extra) {
		c.gridy = row; c.gridwidth = 1;
		c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
		panel.add(new JLabel(labelText), c);
		c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(slider, c);
		c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
		panel.add(val, c);
		if (extra != null) {
			c.gridx = 3;
			panel.add(extra, c);
		}
	}

	/** Configures tick spacing on a slider. */
	private static void configTicks(JSlider s, int major, int minor) {
		s.setPaintTicks(true);
		s.setMajorTickSpacing(major);
		s.setMinorTickSpacing(minor);
	}

	/** Creates a right-aligned value label with a fixed minimum width. */
	private static JLabel valueLabel(String text) {
		JLabel lbl = new JLabel(text);
		lbl.setPreferredSize(new Dimension(42, lbl.getPreferredSize().height));
		lbl.setHorizontalAlignment(JLabel.RIGHT);
		return lbl;
	}

	/** Creates a small stepwise-adjustment button. */
	private static JButton stepButton(String label) {
		JButton btn = new JButton(label);
		btn.setMargin(new Insets(1, 4, 1, 4));
		return btn;
	}

	/** Wraps a "Decrease" then "Increase" button pair into a small panel. */
	private static JPanel makeStepPanel(JButton dec, JButton inc) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		p.add(dec);
		p.add(inc);
		return p;
	}

	// ── Detection & overlay ───────────────────────────────────────────────────

	/**
	 * Runs the identification pipeline on the currently selected single frame and
	 * redraws the overlay on the preview ImagePlus.
	 *
	 * <p>The pipeline is intentionally inlined here (rather than delegating to
	 * {@link Identification#run()}) so that:
	 * <ul>
	 *   <li>No progress-bar or DataSet infrastructure is required for a preview.
	 *   <li>Recursive Tolerance is applied to the MaximumFinder noise tolerance even
	 *       without a {@code parentPerimeterMap} (which is only available during the
	 *       real recursive pipeline run, not at calibration time).
	 * </ul>
	 */
	private void refreshPreview() {
		int    frame  = sliderFrame.getValue();
		double sigma  = sliderSigma.getValue() / (double) SIGMA_SCALE;
		double thresh = sliderThreshold.getValue() / 100.0;

		// ── Display processor ─────────────────────────────────────────────────
		// Original pixel values for the ImagePlus window; possibly inverted.
		ImageProcessor display = virtualStack.getProcessor(frame).duplicate();
		if (controller.getInvertIntensity()) display.invert();

		// Open or update the preview ImagePlus
		String title = "Object ID Preview — Frame " + frame;
		if (previewPlus == null || !previewPlus.isVisible()) {
			previewPlus = new ImagePlus(title, display.duplicate());
			previewPlus.show();
		} else {
			previewPlus.setTitle(title);
			previewPlus.setProcessor(display.duplicate());
		}

		// ── Detection pipeline ────────────────────────────────────────────────
		// Blur a separate copy so the display processor is never mutated.
		ImageProcessor blurred = display.duplicate();
		new GaussianBlur().blurGaussian(blurred, sigma);

		double recTolPct = (isRecursive && sliderRecTol != null) ? sliderRecTol.getValue() : 0;

		// Step 1: find all local maxima
		Polygon poly = detectRawMaxima(blurred, recTolPct);

		// Guard: abort if the raw candidate count is implausibly large.
		// Continuing with thousands of candidates would stall filterLowPoints()
		// and the overlay repaint, potentially freezing or crashing the UI.
		if (poly.npoints > MAX_DETECTIONS) {
			statusLabel.setText(poly.npoints + " raw candidates — too many to preview."
					+ " Try increasing Sigma or Threshold.");
			previewPlus.setOverlay(new Overlay());
			previewPlus.updateAndRepaintWindow();
			return;
		}

		// Step 2: filter by kernel-averaged intensity threshold
		poly = filterMaxima(poly, blurred, thresh);

		// ── Overlay ───────────────────────────────────────────────────────────
		Overlay overlay = new Overlay();
		for (int i = 0; i < poly.npoints; i++) {
			OvalRoi circle = new OvalRoi(poly.xpoints[i] - 6, poly.ypoints[i] - 6, 12, 12);
			circle.setFillColor(Color.RED);
			circle.setStrokeColor(Color.RED);
			circle.setStrokeWidth(1.5);
			overlay.add(circle);
		}
		previewPlus.setOverlay(overlay);
		previewPlus.updateAndRepaintWindow();

		int n = poly.npoints;
		statusLabel.setText(n + " object" + (n == 1 ? "" : "s")
		        + " detected  (frame " + frame + ")");
	}

	// ── Stepwise adjustment ───────────────────────────────────────────────────

	/**
	 * Steps {@code slider} one tick at a time in the given direction until the
	 * number of detected objects changes by at least one, then updates the UI.
	 *
	 * <p>Runs on a background {@link SwingWorker}; all controls are disabled during
	 * the search and re-enabled on completion.
	 *
	 * <p>If a detection pass produces more than {@link #MAX_DETECTIONS} raw candidates
	 * the search is aborted: the slider is left at the last safe position and the
	 * status label reports the approximate count (rounded to the nearest 10).
	 *
	 * @param slider    the slider whose value to step
	 * @param direction {@code +1} to increase the slider value (fewer objects for all
	 *                  tunable parameters); {@code -1} to decrease it (more objects)
	 *
	 * <p>Note on direction polarity: for all three tunable parameters (Sigma, Threshold,
	 * Recursive Tolerance), <em>higher</em> slider values produce <em>fewer</em>
	 * detections.  The "Increase" button therefore uses {@code +1} and "Decrease"
	 * uses {@code -1}.
	 */
	private void runStepwise(JSlider slider, int direction) {
		setAllControlsEnabled(false);
		statusLabel.setText("Adjusting…");

		// Snapshot all current slider values before the background thread starts.
		final int   startVal    = slider.getValue();
		final double snapSigma  = sliderSigma.getValue()    / (double) SIGMA_SCALE;
		final double snapThresh = sliderThreshold.getValue() / 100.0;
		final double snapRecTol = (isRecursive && sliderRecTol != null) ? sliderRecTol.getValue() : 0;
		final int   frame       = sliderFrame.getValue();

		// Result array: int[5] = { safeVal, baseCount, altData, flag, guardVal }
		//   safeVal  — slider value to apply when the guard fires (last safe position)
		//   guardVal — slider value that triggered the guard (used if user overrides)
		// flag 0 = normal (count changed);  altData = new count,         guardVal = safeVal
		// flag 1 = at slider limit;         altData = baseCount,         guardVal = safeVal
		// flag 2 = count guard;             altData = raw candidate count (positive)
		// flag 3 = time guard;              altData = elapsed ms for the slow step
		// flag 4 = guard at baseline;       starting position already exceeds MAX_DETECTIONS
		new SwingWorker<int[], Void>() {
			@Override
			protected int[] doInBackground() {
				// Detect count at the CURRENT (unmodified) parameters
				int baseCount = quickCount(frame, snapSigma, snapThresh, snapRecTol);
				if (baseCount < 0) {
					// Starting position already exceeds guard — no useful step to take
					return new int[]{ startVal, 0, -baseCount, 4, startVal };
				}

				int val = startVal;
				while (true) {
					int nextVal = val + direction;
					if (nextVal < slider.getMinimum() || nextVal > slider.getMaximum()) {
						return new int[]{ val, baseCount, baseCount, 1, val }; // at limit
					}
					val = nextVal;

					// Compute detection parameters with only this slider varied
					double sigma  = snapSigma;
					double thresh = snapThresh;
					double recTol = snapRecTol;
					if      (slider == sliderSigma)     sigma  = val / (double) SIGMA_SCALE;
					else if (slider == sliderThreshold) thresh = val / 100.0;
					else                                recTol = val; // sliderRecTol

					long t0       = System.currentTimeMillis();
					int  newCount = quickCount(frame, sigma, thresh, recTol);
					long elapsed  = System.currentTimeMillis() - t0;

					if (newCount < 0) {
						// Count guard — revert to last safe position; guardVal lets user override
						return new int[]{ val - direction, baseCount, -newCount, 2, val };
					}
					if (elapsed > STEPWISE_TIMEOUT_MS) {
						// Time guard — revert to last safe position; guardVal lets user override
						return new int[]{ val - direction, baseCount, (int) Math.min(elapsed, Integer.MAX_VALUE), 3, val };
					}
					if (newCount != baseCount) {
						return new int[]{ val, baseCount, newCount, 0, val }; // normal
					}
				}
			}

			@Override
			protected void done() {
				try {
					int[] r        = get();
					int safeVal    = r[0];
					int baseCount  = r[1];
					int altData    = r[2];
					int flag       = r[3];
					int guardVal   = r[4];

					// Apply the safe slider position and refresh
					suppressRefresh = true;
					slider.setValue(safeVal);
					suppressRefresh = false;
					updateValueLabels();
					refreshPreview();

					switch (flag) {
						case 1: // slider limit — no override needed
							statusLabel.setText("Already at "
									+ (direction > 0 ? "maximum" : "minimum")
									+ " — no further change possible.");
							break;

						case 4: // baseline already over guard — no useful override
							int baseRounded = (int) Math.round(altData / 10.0) * 10;
							statusLabel.setText("Starting position already has ~" + baseRounded
									+ " raw candidates. Raise Sigma or Threshold first.");
							break;

						case 2: { // count guard — offer override
							int rounded = (int) Math.round(altData / 10.0) * 10;
							String msg = "<html>The next step would produce approximately <b>"
									+ rounded + "</b> raw detection candidates.<br>"
									+ "This may cause the software to become unresponsive or crash.<br><br>"
									+ "Continue anyway?</html>";
							int choice = JOptionPane.showConfirmDialog(
									GuidedCalibration.this, msg,
									"Guard Warning — High Candidate Count",
									JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (choice == JOptionPane.YES_OPTION) {
								suppressRefresh = true;
								slider.setValue(guardVal);
								suppressRefresh = false;
								updateValueLabels();
								refreshPreview();
								statusLabel.setText("Override applied — results may be unreliable.");
							} else {
								statusLabel.setText("Adjustment stopped at last safe value (~"
										+ rounded + " raw candidates if continued).");
							}
							break;
						}

						case 3: { // time guard — offer override
							double secs = altData / 1000.0;
							String msg = String.format(
									"<html>The detection step took <b>%.1f s</b>.<br>"
									+ "Continuing may cause further slowdowns or crashes.<br><br>"
									+ "Continue anyway?</html>", secs);
							int choice = JOptionPane.showConfirmDialog(
									GuidedCalibration.this, msg,
									"Guard Warning — Slow Detection",
									JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (choice == JOptionPane.YES_OPTION) {
								suppressRefresh = true;
								slider.setValue(guardVal);
								suppressRefresh = false;
								updateValueLabels();
								refreshPreview();
								statusLabel.setText("Override applied — proceed with caution.");
							} else {
								statusLabel.setText(String.format(
										"Adjustment stopped. Detection took %.1f s at this setting.", secs));
							}
							break;
						}
						// case 0: normal — statusLabel already set by refreshPreview()
					}

				} catch (InterruptedException | ExecutionException ex) {
					statusLabel.setText("Error during stepwise adjustment.");
				}
				setAllControlsEnabled(true);
			}
		}.execute();
	}

	/**
	 * Runs one detection pass and returns the <em>filtered</em> object count, or a
	 * <em>negative sentinel</em> {@code -rawCount} if the raw candidate count from
	 * {@link ModifiedMaximumFinder#getMaxima} exceeds {@link #MAX_DETECTIONS}.
	 *
	 * <p>Callers in {@link #runStepwise} also measure wall-clock time around this
	 * method to enforce {@link #STEPWISE_TIMEOUT_MS}; neither the count guard nor
	 * the time guard is enforced inside this method itself.
	 *
	 * <p>Safe to call from a background thread — no Swing state is touched.
	 *
	 * @param frameNo   1-based frame index into {@link #virtualStack}
	 * @param sigma     Gaussian blur sigma in pixels
	 * @param thresh    intensity threshold as a fraction [0–1]
	 * @param recTolPct recursive tolerance as a percentage [0–100]
	 * @return filtered object count (≥ 0), or {@code -rawCount} if the count guard fires
	 */
	private int quickCount(int frameNo, double sigma, double thresh, double recTolPct) {
		ImageProcessor proc = virtualStack.getProcessor(frameNo).duplicate();
		if (controller.getInvertIntensity()) proc.invert();

		ImageProcessor blurred = proc.duplicate();
		new GaussianBlur().blurGaussian(blurred, sigma);

		Polygon raw = detectRawMaxima(blurred, recTolPct);
		if (raw.npoints > MAX_DETECTIONS) return -raw.npoints; // guard sentinel

		return filterMaxima(raw, blurred, thresh).npoints;
	}

	/**
	 * Computes the noise tolerance for {@link ModifiedMaximumFinder}.
	 * Returns 0 in primary mode; returns {@code (max − min) × recTolPct / 100}
	 * in recursive mode.  Extracted to avoid duplicating the min/max scan.
	 */
	private double computeTolerance(ImageProcessor blurred, double recTolPct) {
		if (!isRecursive || recTolPct == 0) return 0;
		float gMin = Float.MAX_VALUE, gMax = -Float.MAX_VALUE;
		for (int px = 0; px < blurred.getWidth(); px++) {
			for (int py = 0; py < blurred.getHeight(); py++) {
				float v = blurred.getPixelValue(px, py);
				if (v < gMin) gMin = v;
				if (v > gMax) gMax = v;
			}
		}
		return (gMax - gMin) * (recTolPct / 100.0);
	}

	/**
	 * Finds the raw local maxima on an already-blurred processor, using the same
	 * tolerance rule as {@link Identification#run}. Shared by {@link #refreshPreview}
	 * and {@link #quickCount} so the detection recipe lives in exactly one place.
	 */
	private Polygon detectRawMaxima(ImageProcessor blurred, double recTolPct) {
		double tolerance = computeTolerance(blurred, recTolPct);
		return new ModifiedMaximumFinder().getMaxima(blurred, tolerance, true);
	}

	/**
	 * Filters raw maxima by kernel-averaged intensity threshold. The null stack/dataSet
	 * passed to {@code initialize} are never accessed by {@code filterLowPoints}; it only
	 * uses percentThreashold and kernelSize (=3).
	 */
	private Polygon filterMaxima(Polygon raw, ImageProcessor blurred, double thresh) {
		Identification tmpId = new Identification();
		tmpId.initialize(null, null); // sets kernelSize = 3
		tmpId.setFinder(new ModifiedMaximumFinder(), thresh);
		return tmpId.filterLowPoints(raw, blurred);
	}

	/**
	 * Enables or disables all interactive controls.
	 * Called before and after a background {@link SwingWorker} stepwise search.
	 */
	private void setAllControlsEnabled(boolean enabled) {
		sliderSigma    .setEnabled(enabled);
		sliderThreshold.setEnabled(enabled);
		if (sliderFrame != null) sliderFrame.setEnabled(enabled);
		buttonApply    .setEnabled(enabled);
		buttonClose    .setEnabled(enabled);
		btnSigmaInc    .setEnabled(enabled);
		btnSigmaDec    .setEnabled(enabled);
		btnThreshInc   .setEnabled(enabled);
		btnThreshDec   .setEnabled(enabled);
		if (isRecursive && sliderRecTol != null) {
			sliderRecTol  .setEnabled(enabled);
			btnRecTolInc  .setEnabled(enabled);
			btnRecTolDec  .setEnabled(enabled);
		}
		btnAutoCalibrate.setEnabled(enabled);
		// Re-enable the DataSet button only when a suitable DataSet is actually present
		btnAutoCalibrateFromDataSet.setEnabled(enabled && controller.externalSegmentationExists());
	}

	// ── Label updates ─────────────────────────────────────────────────────────

	/** Updates all value labels from the current slider positions. */
	private void updateValueLabels() {
		labelFrameVal    .setText(String.valueOf(sliderFrame.getValue()));
		labelSigmaVal    .setText(sigmaText(sliderSigma.getValue()));
		labelThresholdVal.setText(sliderThreshold.getValue() + "%");
		if (isRecursive && labelRecTolVal != null) {
			labelRecTolVal.setText(sliderRecTol.getValue() + "%");
		}
	}

	private static String sigmaText(int sliderVal) {
		return String.format("%.1f", sliderVal / (double) SIGMA_SCALE);
	}

	// ── Listeners ─────────────────────────────────────────────────────────────

	/**
	 * Updates value labels immediately on every slider tick (cheap, instant feedback).
	 * Re-runs detection only when the slider is released ({@code !getValueIsAdjusting()})
	 * and {@link #suppressRefresh} is not set (i.e. not a programmatic stepwise update).
	 */
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
		if (e.getSource() == buttonApply)                    applyToSettings();
		if (e.getSource() == buttonClose)                    close();
		if (e.getSource() == btnAutoCalibrate)               runAutoCalibrateLoG();
		if (e.getSource() == btnAutoCalibrateFromDataSet)    runAutoCalibrateFromDataSet();
	}

	// ── Apply / close ─────────────────────────────────────────────────────────

	/**
	 * Pushes the current slider values to the controller.
	 * Sigma is converted from slider units ({@code val / SIGMA_SCALE}).
	 * Threshold is converted from percent to fraction ({@code val / 100.0}).
	 * Recursive Tolerance is stored as percent directly.
	 */
	private void applyToSettings() {
		controller.setGaussianBlurSigma(sliderSigma.getValue() / (double) SIGMA_SCALE);
		controller.setMaximumFinderTolerance(sliderThreshold.getValue() / 100.0);
		if (isRecursive) {
			controller.setRecursiveTolerancePct(sliderRecTol.getValue());
		}
		statusLabel.setText("Settings applied.");
	}

	// ── Auto-calibration ─────────────────────────────────────────────────────

	/**
	 * Estimates sigma via LoG scale-space analysis on the already-open
	 * {@link #virtualStack}.  Runs in a background thread; on completion sets
	 * {@link #sliderSigma} to the nearest representable value and refreshes the preview.
	 */
	private void runAutoCalibrateLoG() {
		setAllControlsEnabled(false);
		statusLabel.setText("Auto-calibrating sigma (LoG)…");

		new SwingWorker<Double, Void>() {
			@Override
			protected Double doInBackground() throws Exception {
				return AutoCalibration.estimateSigmaLoG(virtualStack);
			}

			@Override
			protected void done() {
				try {
					double sigma    = get();
					int sliderVal   = clamp((int) Math.round(sigma * SIGMA_SCALE),
					                        sliderSigma.getMinimum(), sliderSigma.getMaximum());
					suppressRefresh = true;
					sliderSigma.setValue(sliderVal);
					suppressRefresh = false;
					updateValueLabels();
					refreshPreview();
					statusLabel.setText(String.format(
							"Sigma estimated: %.2f px  — click \"Apply to Settings\" to commit.", sigma));
				} catch (InterruptedException | ExecutionException ex) {
					Throwable cause = ex.getCause();
					statusLabel.setText("Auto-calibration failed: "
							+ (cause != null ? cause.getMessage() : ex.getMessage()));
				}
				setAllControlsEnabled(true);
			}
		}.execute();
	}

	/**
	 * Estimates sigma and threshold from the currently loaded ground-truth DataSet,
	 * using the already-open {@link #virtualStack}.  Runs in a background thread;
	 * on completion sets both {@link #sliderSigma} and {@link #sliderThreshold} and
	 * refreshes the preview.
	 */
	private void runAutoCalibrateFromDataSet() {
		DataSet ds = controller.getDataSet();
		if (ds == null || !ds.getExternalSegmentationExists()) {
			statusLabel.setText("No external segmentation DataSet loaded.");
			return;
		}
		boolean invert = controller.getInvertIntensity();

		setAllControlsEnabled(false);
		statusLabel.setText("Auto-calibrating from DataSet…");

		new SwingWorker<double[], Void>() {
			@Override
			protected double[] doInBackground() throws Exception {
				return AutoCalibration.calibrateFromDataSet(ds, virtualStack, invert);
			}

			@Override
			protected void done() {
				try {
					double[] result = get();
					if (result == null) {
						statusLabel.setText("No usable perimeter data found in DataSet.");
						setAllControlsEnabled(true);
						return;
					}
					double sigma     = result[0];
					double threshold = result[1]; // fraction [0–1]

					int sigmaVal  = clamp((int) Math.round(sigma * SIGMA_SCALE),
					                      sliderSigma.getMinimum(), sliderSigma.getMaximum());
					int threshVal = clamp((int) Math.round(threshold * 100),
					                      sliderThreshold.getMinimum(), sliderThreshold.getMaximum());

					suppressRefresh = true;
					sliderSigma    .setValue(sigmaVal);
					sliderThreshold.setValue(threshVal);
					suppressRefresh = false;
					updateValueLabels();
					refreshPreview();
					statusLabel.setText(String.format(
							"Calibrated: σ=%.2f px, threshold=%.1f%%  — click \"Apply to Settings\" to commit.",
							sigma, threshold * 100.0));
				} catch (InterruptedException | ExecutionException ex) {
					Throwable cause = ex.getCause();
					statusLabel.setText("Auto-calibration failed: "
							+ (cause != null ? cause.getMessage() : ex.getMessage()));
				}
				setAllControlsEnabled(true);
			}
		}.execute();
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
