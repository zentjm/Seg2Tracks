package manualSegmentation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.RecursiveDataSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import gui.OperationController;
import ij.IJ;
import ij.Prefs;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ScrollbarWithLabel;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import geometricTools.ModifiedMaximumFinder;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import identification.Identification;

/**
 * Controller for manual recursive (void/subsegmentation) segmentation.
 *
 * Mirrors ManualSegmentationController but operates cell-by-cell instead of on
 * the full image:
 *
 *  1. Iterates through each parent {@link LinkSet} in the prior panel's DataSet.
 *  2. For each parent cell, builds a fixed-size canvas whose dimensions are
 *     calculated once across ALL parent cells so the window never resizes.
 *     The canvas is large enough to contain any cell's expansion in any
 *     direction from the anchor (first-frame center) of each LinkSet.
 *  3. Each cell's first frame is centered in the canvas. Subsequent frames
 *     re-center only for symmetric translation (equal leading-edge growth
 *     and trailing-edge retraction), keeping the cell visually stable.
 *  4. A sidebar lists all parent cells as clickable buttons, with Previous/Next
 *     navigation at the bottom, docked to the left of the image display.
 *  5. The user draws voids with the standard Start Object → Next Frame →
 *     End Object workflow, then clicks a cell button or "Next" to advance.
 *  6. Drawn void segments are stored with coordinates translated back to the
 *     original (full-image) coordinate space and accumulated into a
 *     {@link RecursiveDataSet}.
 *
 * Data collection is partially stubbed — see TODO markers — and will be wired
 * to the recursive pipeline in a subsequent implementation pass.
 */
public class RecursionManualController {

	// ── MVC / context ─────────────────────────────────────────────────────────
	OperationController controller;
	RecursionManualPanel panel;

	/** When false the panel shows only navigation — no drawing or modification. */
	boolean canEdit;
	ModifiedStackWindow  window;
	Overlay              overlay;

	// ── Composite window (sidebar + image) ───────────────────────────────────
	RecursionCompositeWindow compositeWindow;
	RecursionSegmentListPanel sidebar;

	// ── Parent dataset ────────────────────────────────────────────────────────
	/** The completed outer segmentation whose cells we recurse into. */
	DataSet parentDataSet;

	/** Ordered list of parent cells to iterate through. */
	List<LinkSet> parentLinkSets;

	/** Index of the cell currently shown to the user. */
	int cellIndex = 0;

	// ── Offset matrix (single source of truth for coordinate conversion) ────────
	/**
	 * Per-cell, per-frame crop origins in full-image pixel space.
	 * {@code cellFrameOffsets[cellIdx][f][0]} = X origin of the canvas crop for
	 * cell {@code cellIdx} at full-stack frame {@code f}.
	 * {@code cellFrameOffsets[cellIdx][f][1]} = Y origin.
	 *
	 * Populated ONCE in {@link #buildOffsetMatrix()} at session start and never
	 * modified thereafter.  All coordinate conversions use
	 * {@link #convertToOffset} / {@link #convertFromOffset} which read from here,
	 * eliminating any drift from per-visit recalculation.
	 */
	int[][][] cellFrameOffsets;

	/**
	 * Per-cell canvas dimensions, parallel to {@link #cellFrameOffsets}.
	 * Populated by {@link #buildOffsetMatrix()} alongside the offset matrix.
	 *
	 * TODO: Canvas size is computed from the first segment's perimeter only and
	 * uses a fixed padding constant.  Cells with large variance in object size
	 * along the LinkSet (growing/shrinking cells) may be clipped in later frames
	 * or have excessive empty space in earlier frames.  A more robust approach
	 * would take the bounding box across ALL segments in the LinkSet, but this
	 * requires the composite window to support per-cell resizing, which is not
	 * yet implemented.
	 */
	int[] cellCanvasWidth;
	int[] cellCanvasHeight;

	/**
	 * Convenience copies of the current cell's canvas dimensions, set from the
	 * matrix at the start of each {@link #loadCell(int)} call.
	 * Read-only; do NOT assign these directly — use the matrix arrays instead.
	 */
	int canvasWidth;
	int canvasHeight;

	// ── Per-cell state ────────────────────────────────────────────────────────
	/** The ImagePlus displayed for the current cell (uniform canvas size). */
	ImagePlus currentImagePlus;

	/** DataSet scoped to the current cell's cropped image (canvas-space coords). */
	DataSet cellDataSet;

	/** The LinkSet currently being drawn by the user. */
	LinkSet linkSet;

	/** The most recently committed segment (used by restoreSelection). */
	Segment segment;
	Segment previousSegment;

	/**
	 * Full-stack frame indices (0-based) of the first and last segments in the
	 * current parent LinkSet. The displayed ImagePlus only covers this range.
	 */
	int cellFirstFrame = 0;
	int cellLastFrame  = 0;

	/** Current frame (1-based, matching ImagePlus slice numbering). */
	int frame      = 1;
	int startFrame = -1;

	/** Saved ROI for the Restore Selection action. */
	Roi restoreRoi;

	boolean mouseListenerActive = false;

	/**
	 * True while the user is in an active segmentation (the "Segmentation" button
	 * has been pressed but neither "End Object" nor "Main Menu" has been pressed yet).
	 * Cell-switching via the sidebar and Previous/Next is blocked in this state.
	 */
	boolean segmentationInProgress = false;

	/** The frame scrollbar (ScrollbarWithLabel) extracted from the hidden StackWindow. */
	ScrollbarWithLabel frameScrollbar;

	/** Non-interactive full-image preview at the bottom of the composite. */
	RecursionPreviewPanel previewPanel;

	// ── Accumulated output ────────────────────────────────────────────────────
	/** All child void segments from all cells, stored in full-image coordinates. */
	RecursiveDataSet accumulatedDataSet;

	/**
	 * The child DataSet for the currently loaded cell. This IS the source of
	 * truth — editing operations commit directly to it (no batch round-trip).
	 */
	RecursiveDataSet currentChildDS;

	/**
	 * Maps each canvas-space LinkSet (in cellDataSet) to its corresponding
	 * full-image LinkSet in currentChildDS, so deleteSelectedObject() and
	 * mergeSelectedObjects() can find and modify the child DS entry directly.
	 */
	Map<LinkSet, LinkSet> canvasToChildLS = new HashMap<>();

	// ── Colours ───────────────────────────────────────────────────────────────
	Color color    = decodeColor(Prefs.get(ManualSegmentationController.PREF_ROI_COLOR, "#00ff00"));
	Color altColor = new Color(255, 0, 0);
	{ ij.gui.Roi.setColor(color); }  // apply saved draw-tool outline color on construction

	/** Pixels of padding to add around the cell bounding box when cropping. */
	static final int CROP_PADDING = 20;

	/** Cached full-image stack (loaded once, reused for every cell). */
	ImageStack fullStack;

	// ── Constructor ───────────────────────────────────────────────────────────

	/**
	 * @param controller   the parent {@link OperationController} (provides image
	 *                     path, freezes the main UI, etc.)
	 * @param parentDataSet the completed external segmentation DataSet whose
	 *                     LinkSets represent the parent cells
	 */
	/**
	 * @param controller    the parent {@link OperationController}
	 * @param parentDataSet the completed outer-segmentation DataSet
	 * @param canEdit       true for the full edit workflow; false for view-only preview
	 */
	public RecursionManualController(OperationController controller, DataSet parentDataSet, boolean canEdit) {
		this.controller     = controller;
		this.parentDataSet  = parentDataSet;
		this.parentLinkSets = new ArrayList<>(parentDataSet.getLinkSetList());
		this.canEdit        = canEdit;
	}

	// ── Entry point ───────────────────────────────────────────────────────────

	/**
	 * Starts the recursive manual segmentation session.
	 * Freezes the main Seg2Tracks window, calculates the global canvas size,
	 * creates the composite window with sidebar, and loads the first cell.
	 */
	public void run() {
		if (parentLinkSets.isEmpty()) {
			IJ.error("No parent cells found in the selected dataset.");
			return;
		}

		// Load the full image stack once into memory
		ImagePlus virtualImp = IJ.openVirtual(controller.getInputFilePath());
		ImageStack virtualStack = virtualImp.getImageStack();
		fullStack = new ImageStack(virtualStack.getWidth(), virtualStack.getHeight());
		for (int i = 1; i <= virtualStack.getSize(); i++) {
			fullStack.addSlice(virtualStack.getProcessor(i).duplicate());
		}

		// Initialise the accumulated output dataset for all cells' results
		accumulatedDataSet = new RecursiveDataSet(
				fullStack.getWidth(), fullStack.getHeight(), fullStack.getSize(), parentDataSet);
		for (int i = 0; i < fullStack.getSize(); i++) {
			accumulatedDataSet.getFrameSetList()[i] = new FrameSet(i, accumulatedDataSet);
		}

		// Build the offset matrix ONCE for all cells.
		// All coordinate conversions during this session read from this matrix.
		buildOffsetMatrix();

		// Seed canvasWidth/canvasHeight from cell 0 so the composite window
		// can be sized before loadCell() is called.
		canvasWidth  = cellCanvasWidth[0];
		canvasHeight = cellCanvasHeight[0];

		// Freeze main UI
		controller.setViewActive(false);

		// Create the floating control panel
		panel = new RecursionManualPanel(this, canEdit);
		panel.createView();

		// Create the sidebar
		sidebar = new RecursionSegmentListPanel(this, parentLinkSets, canvasHeight);

		// Create the composite window (sidebar + image area)
		compositeWindow = new RecursionCompositeWindow(
				"Seg2Tracks — Recursive Segmentation",
				sidebar, canvasWidth, canvasHeight);
		compositeWindow.setVisible(true);

		// Load the first cell — packs and sizes the composite window
		loadCell(0);

		// Center the composite window on whichever screen contains the mouse pointer,
		// then place the control panel just above it (overriding the initial placement
		// done inside loadCell so the final position is relative to the centered window).
		Rectangle sb = getScreenForMouse().getDefaultConfiguration().getBounds();
		int cx = sb.x + (sb.width  - compositeWindow.getWidth())  / 2;
		int cy = sb.y + (sb.height - compositeWindow.getHeight()) / 2;
		// Clamp: never push any edge off-screen; if taller than the screen, pin to the top
		cx = Math.max(sb.x, Math.min(sb.x + sb.width  - compositeWindow.getWidth(),  cx));
		cy = Math.max(sb.y, Math.min(sb.y + sb.height - compositeWindow.getHeight(), cy));
		compositeWindow.setLocation(cx, cy);
		positionControlPanel();
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Offset matrix — computed ONCE per session, read-only thereafter
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Populates {@link #cellFrameOffsets}, {@link #cellCanvasWidth}, and
	 * {@link #cellCanvasHeight} for every parent cell in one pass.
	 * Called once from {@link #run()} after the full stack is loaded.
	 * No coordinate conversion anywhere else in this class recomputes these values.
	 */
	private void buildOffsetMatrix() {
		int numCells  = parentLinkSets.size();
		int numFrames = fullStack.getSize();
		cellFrameOffsets = new int[numCells][numFrames][2];
		cellCanvasWidth  = new int[numCells];
		cellCanvasHeight = new int[numCells];
		for (int ci = 0; ci < numCells; ci++) {
			computeCanvasSizeForCell(ci, parentLinkSets.get(ci));
			computeFrameOffsetsForCell(ci, parentLinkSets.get(ci));
		}
	}

	/**
	 * Sets {@code cellCanvasWidth[ci]} and {@code cellCanvasHeight[ci]} for the
	 * given cell.  Uses the bounding box of the first segment's external perimeter
	 * plus {@link #CROP_PADDING} on all sides, clamped to the source image size.
	 *
	 * TODO: Bounding box is taken from the first segment only.  Cells with large
	 *   variance in object size along the LinkSet (e.g. rapidly growing or
	 *   shrinking objects) may be clipped in later frames or waste space in earlier
	 *   frames.  A more robust approach would union bounding boxes across the whole
	 *   LinkSet, but this requires the composite window to support per-cell canvas
	 *   resizing, which is not yet implemented.
	 */
	private void computeCanvasSizeForCell(int ci, LinkSet ls) {
		if (ls == null || ls.size() == 0) {
			cellCanvasWidth[ci]  = 200;
			cellCanvasHeight[ci] = 200;
			return;
		}
		Segment firstSeg = ls.get(0);
		Point[] perim = (firstSeg != null) ? firstSeg.getExternalPerimeter() : null;
		if (perim == null || perim.length == 0) {
			cellCanvasWidth[ci]  = 200;
			cellCanvasHeight[ci] = 200;
			return;
		}
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (Point p : perim) {
			if (p.x < minX) minX = p.x;  if (p.x > maxX) maxX = p.x;
			if (p.y < minY) minY = p.y;  if (p.y > maxY) maxY = p.y;
		}
		cellCanvasWidth[ci]  = Math.min((maxX - minX) + CROP_PADDING * 2, fullStack.getWidth());
		cellCanvasHeight[ci] = Math.min((maxY - minY) + CROP_PADDING * 2, fullStack.getHeight());
	}

	/**
	 * Fills {@code cellFrameOffsets[ci][f][0..1]} for every frame, centering
	 * the crop on each frame's parent-cell centerPoint.
	 * Frames with no segment hold the previous frame's offset (forward-fill);
	 * frames before the first segment are backward-filled from the first valid frame.
	 */
	private void computeFrameOffsetsForCell(int ci, LinkSet ls) {
		int numFrames = fullStack.getSize();
		int cw = cellCanvasWidth[ci];
		int ch = cellCanvasHeight[ci];

		Segment[] segByFrame = new Segment[numFrames];
		for (Segment seg : ls) {
			int f = seg.getFrame();
			if (f >= 0 && f < numFrames) segByFrame[f] = seg;
		}

		int defaultOX = (fullStack.getWidth()  - cw) / 2;
		int defaultOY = (fullStack.getHeight() - ch) / 2;

		for (int f = 0; f < numFrames; f++) {
			Point cp = (segByFrame[f] != null) ? segByFrame[f].getCenterPoint() : null;
			if (cp != null) {
				cellFrameOffsets[ci][f][0] = cp.x - cw / 2;
				cellFrameOffsets[ci][f][1] = cp.y - ch / 2;
			} else if (f > 0) {
				cellFrameOffsets[ci][f][0] = cellFrameOffsets[ci][f - 1][0];
				cellFrameOffsets[ci][f][1] = cellFrameOffsets[ci][f - 1][1];
			} else {
				cellFrameOffsets[ci][f][0] = defaultOX;
				cellFrameOffsets[ci][f][1] = defaultOY;
			}
		}
		// Backward-fill frames before the first valid segment
		int firstValidF = -1;
		for (int f = 0; f < numFrames; f++) {
			if (segByFrame[f] != null && segByFrame[f].getCenterPoint() != null) {
				firstValidF = f; break;
			}
		}
		if (firstValidF > 0) {
			for (int f = 0; f < firstValidF; f++) {
				cellFrameOffsets[ci][f][0] = cellFrameOffsets[ci][firstValidF][0];
				cellFrameOffsets[ci][f][1] = cellFrameOffsets[ci][firstValidF][1];
			}
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Coordinate conversion helpers
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Converts a canvas-space point to full-image space for the given cell and frame.
	 * {@code fullImageX = canvasX + cellFrameOffsets[cellIdx][frame][0]}
	 */
	private Point convertToOffset(int cellIdx, int frame, int canvasX, int canvasY) {
		int ox = cellFrameOffsets[cellIdx][frame][0];
		int oy = cellFrameOffsets[cellIdx][frame][1];
		return new Point(canvasX + ox, canvasY + oy);
	}

	/**
	 * Converts a full-image-space point to canvas space for the given cell and frame.
	 * {@code canvasX = fullImageX − cellFrameOffsets[cellIdx][frame][0]}
	 * <p>
	 * For auto-SARN child DataSet segments whose coordinates are already in
	 * full-image space and whose stored offset fields are zero, this degenerates
	 * correctly to {@code canvasX = fullX − currentFrameOffset}.
	 */
	private Point convertFromOffset(int cellIdx, int frame, int fullX, int fullY) {
		int ox = cellFrameOffsets[cellIdx][frame][0];
		int oy = cellFrameOffsets[cellIdx][frame][1];
		return new Point(fullX - ox, fullY - oy);
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Cell navigation
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Public entry point for the sidebar and Previous/Next buttons to load a
	 * cell by index. Commits current cell data first.
	 */
	public void loadCellByIndex(int index) {
		if (segmentationInProgress) return; // sidebar is locked during active segmentation
		if (index < 0 || index >= parentLinkSets.size()) return;
		if (index == cellIndex && currentImagePlus != null) return; // already loaded
		loadCell(index);
	}

	/**
	 * Opens the image for the cell at {@code index} and updates the GUI.
	 * Closes any previously open cell image first.
	 */
	private void loadCell(int index) {
		// Detach the embedded canvas from the composite window before close() disposes
		// the StackWindow — leaving a disposed canvas in a live Swing panel corrupts
		// the AWT event queue.
		if (compositeWindow != null) {
			JPanel ip = compositeWindow.getImagePanel();
			ip.removeAll();
			ip.revalidate();
		}

		// Close the previous cell image if one is open.
		// ImagePlus.close() internally disposes its own StackWindow, so we must NOT
		// call window.dispose() separately — double-disposing the same AWT component
		// corrupts the event queue and causes the CPU hot-loop the user observes.
		if (currentImagePlus != null) {
			currentImagePlus.changes = false; // suppress "save?" dialog
			currentImagePlus.close();   // also disposes window internally
			currentImagePlus = null;
		}
		window = null; // already disposed by ImagePlus.close() above

		cellIndex = index;
		LinkSet parentCell = parentLinkSets.get(cellIndex);

		// Determine the frame range spanned by this cell's segments
		cellFirstFrame = Integer.MAX_VALUE;
		cellLastFrame  = Integer.MIN_VALUE;
		for (Segment seg : parentCell) {
			int f = seg.getFrame();
			if (f < cellFirstFrame) cellFirstFrame = f;
			if (f > cellLastFrame)  cellLastFrame  = f;
		}
		if (cellFirstFrame == Integer.MAX_VALUE) {
			cellFirstFrame = 0;
			cellLastFrame  = 0;
		}

		// Read canvas size from the pre-built matrix (no recalculation).
		canvasWidth  = cellCanvasWidth[cellIndex];
		canvasHeight = cellCanvasHeight[cellIndex];

		// Build the ImageStack covering only this cell's frame range,
		// using the matrix offsets as the single source of truth for crop origins.
		currentImagePlus = buildCenteredImagePlus(cellIndex, parentCell);

		// Initialise per-cell DataSet (dimensions of the canvas)
		cellDataSet = new DataSet(canvasWidth, canvasHeight, currentImagePlus.getNSlices());
		for (int i = 0; i < cellDataSet.getFrameSetList().length; i++) {
			cellDataSet.getFrameSetList()[i] = new FrameSet(i, cellDataSet);
		}

		// Initialise overlay
		overlay = new Overlay();
		overlay.drawNames(true);
		overlay.drawLabels(true);
		overlay.setLabelColor(Color.BLACK);
		overlay.drawBackgrounds(true);
		overlay.setLabelFont(new Font("TimesRoman", Font.BOLD, 14));
		currentImagePlus.setOverlay(overlay);

		// Reset canvas→childDS mapping for the new cell
		canvasToChildLS = new HashMap<>();

		// Resolve (or create) the child DataSet for this cell.
		// This is the single source of truth — all edits commit here directly.
		DataSet existingChild = parentCell.getChildDataSet();
		
		if (existingChild instanceof RecursiveDataSet) {
			currentChildDS = (RecursiveDataSet) existingChild;
		} else {
			currentChildDS = new RecursiveDataSet(
					fullStack.getWidth(), fullStack.getHeight(), fullStack.getSize(), parentDataSet);
			for (int i = 0; i < fullStack.getSize(); i++) {
				currentChildDS.getFrameSetList()[i] = new FrameSet(i, currentChildDS);
			}
			parentCell.setChildDataSet(currentChildDS);
			currentChildDS.setLinkageExists(true);
		}

		// Draw any existing results for this cell onto the overlay
		drawExistingResults(parentCell);

		// Parent outline is shown only in the context preview, not on the edit frame.

		// Create the ImageJ StackWindow for internal state. The constructor makes
		// the Frame visible, so hide it immediately — the canvas is re-parented
		// into the composite window instead.
		window = new ModifiedStackWindow(currentImagePlus);
		window.setVisible(false);
		currentImagePlus.setWindow(window);

		// ── Embed canvas in the composite's image panel ──────────────────────
		ImageCanvas ic = window.getCanvas();
		if (ic.getParent() != null) ic.getParent().remove(ic); // detach from hidden StackWindow
		JPanel imagePanel = compositeWindow.getImagePanel();
		imagePanel.removeAll();

		// Wrap the canvas with an "Edit Frame" label
		JPanel editPanel = new JPanel(new BorderLayout());
		JLabel editLabel = new JLabel("Edit Frame", JLabel.CENTER);
		editLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
		editPanel.add(editLabel, BorderLayout.NORTH);
		editPanel.add(ic, BorderLayout.CENTER);
		imagePanel.add(editPanel, BorderLayout.CENTER);

		// Mouse wheel scrolling on the canvas changes the displayed frame
		// and keeps the preview in sync.
		ic.addMouseWheelListener(e -> {
			if (currentImagePlus == null) return;
			int newSlice = currentImagePlus.getCurrentSlice() + e.getWheelRotation();
			if (newSlice >= 1 && newSlice <= currentImagePlus.getStackSize()) {
				currentImagePlus.setSlice(newSlice);
				syncPreview();
			}
		});

		// ── Extract frame scrollbar and full-image preview into bottom panel ─
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

		frameScrollbar = window.getZSelector();
		if (frameScrollbar != null) {
			if (frameScrollbar.getParent() != null) frameScrollbar.getParent().remove(frameScrollbar);
			frameScrollbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, frameScrollbar.getPreferredSize().height));
			bottomPanel.add(frameScrollbar);

			// Sync the full-image preview when the user drags the scrollbar
			frameScrollbar.addAdjustmentListener(e -> syncPreview());
		}

		// Full-image context preview with parent cell outline
		JLabel contextLabel = new JLabel("Context Frame", JLabel.CENTER);
		contextLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
		contextLabel.setAlignmentX(0.5f);
		bottomPanel.add(contextLabel);

		int previewWidth = Math.max(canvasWidth, 300);
		previewPanel = new RecursionPreviewPanel(fullStack, parentCell, cellFirstFrame, previewWidth);
		bottomPanel.add(previewPanel);

		imagePanel.add(bottomPanel, BorderLayout.SOUTH);
		imagePanel.revalidate();

		// Apply initial zoom so the canvas fills at least 25% of screen in both dimensions
		applyInitialZoom();

		// Update sidebar highlight
		if (sidebar != null) {
			sidebar.setActiveCell(cellIndex);
		}

		// Update the control panel title and reposition it above the composite window
		panel.setCellName(parentCell.getDisplayName());
		positionControlPanel();

		mainMenu();
	}

	/**
	 * Called when the user clicks "Next Cell".
	 * Finalises any in-progress object, collects this cell's data, and advances.
	 * If there are no more cells the session ends automatically.
	 */
	public void nextCell() {
		if (segmentationInProgress) return; // locked during active segmentation
		int next = cellIndex + 1;
		if (next < parentLinkSets.size()) {
			loadCell(next);
		} else {
			finishSession();
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Standard segmentation actions (mirrors ManualSegmentationController)
	// ═══════════════════════════════════════════════════════════════════════════

	/** True while the segmentation sub-panel is showing; used by openSettings() to provide context. */
	private boolean inSegmentMode = false;

	/** Returns to this cell's main menu, unlocking cell-switching in the sidebar. */
	public void mainMenu() {
		inSegmentMode = false;
		segmentationInProgress = false;
		if (sidebar != null) sidebar.setCellSwitchingEnabled(true);
		mouseListenerActive = false;
		overlay.selectable(false);
		panel.setMainPanel();
		IJ.setTool("hand");
	}

	public void openSettings() {
		panel.setSettingsPanel(inSegmentMode);
	}

	public void closeSettings() {
		if (inSegmentMode) {
			panel.setSegmentPanel();
			// Intentionally omit stateObject() — preserve current button-enable
			// state so that returning from Settings mid-draw keeps Next/End active.
		} else {
			mainMenu();
		}
	}

	/** Begins the segmentation sub-panel for drawing new void objects. */
	public void newSegmentation() {
		inSegmentMode = true;
		segmentationInProgress = true;
		if (sidebar != null) sidebar.setCellSwitchingEnabled(false);
		panel.setSegmentPanel();
		panel.stateObject(true, false, false, false, true);
		cellDataSet.setManuallyEdited(true);
	}

	/** Starts drawing a new void object. */
	public void startObject() {
		segment = null;
		linkSet = new LinkSet(cellDataSet);
		if (frameScrollbar != null) frameScrollbar.setEnabled(false);
		IJ.setTool(Prefs.get(ManualSegmentationController.PREF_TOOL, "polygon"));
		frame = currentImagePlus.getCurrentSlice();
		startFrame = frame;
		panel.stateObject(false, true,
		                  frame == startFrame,
		                  frame == currentImagePlus.getImageStackSize(),
		                  false);
	}

	/**
	 * Commits the ROI on the current frame, advances to the next frame.
	 * Mirrors ManualSegmentationController.nextFrame().
	 */
	public void nextFrame() {
		if (currentImagePlus.getRoi() == null) {
			panel.dialogAlert("Must select an outline for this frame");
			return;
		}
		if (!withinBounds(currentImagePlus.getRoi())) {
			panel.dialogAlert("Outline must be within image bounds");
			return;
		}

		if (segment != null) {
			segment.setUserRoi((Roi) currentImagePlus.getRoi().clone());
			previousSegment = segment;
		}

		segment = getSegment(frame - 1 + cellFirstFrame, currentImagePlus.getRoi()); // frame is 1-based slice; full-stack index = frame-1+cellFirstFrame
		segment.getRoi().setPosition(frame);
		segment.setLinkSet(linkSet);
		linkSet.add(segment);

		if (currentImagePlus.getCurrentSlice() != currentImagePlus.getImageStackSize()) {
			frame++;
			currentImagePlus.setSlice(frame);
		}
		syncPreview();
		panel.stateObject(false, true,
		                  frame == startFrame,
		                  currentImagePlus.getCurrentSlice() == currentImagePlus.getImageStackSize(),
		                  false);
	}

	/** Steps back one frame, removing the last committed segment. */
	public void previousFrame() {
		linkSet.removeLastSegment();

		if (linkSet.size() != 0) {
			segment = linkSet.get(linkSet.size() - 1);
		} else {
			segment = null; // track emptied — do not keep a reference to the removed segment
		}

		if (frame > startFrame) {
			frame--;
			currentImagePlus.setSlice(frame);
		}

		// Restore the saved ROI on the previous frame
		if (segment != null && segment.getUserRoi() != null) {
			currentImagePlus.setRoi(segment.getUserRoi());
			restoreRoi = segment.getUserRoi();
		}

		syncPreview();
		panel.stateObject(false, true,
		                  frame == startFrame,
		                  currentImagePlus.getCurrentSlice() == currentImagePlus.getImageStackSize(),
		                  false);
	}

	/** Restores the saved ROI from the previous frame onto the current canvas. */
	public void restoreSelection() {
		if (previousSegment != null) {
			restoreRoi = previousSegment.getUserRoi();
		}
		if (restoreRoi != null) {
			currentImagePlus.setRoi(restoreRoi);
		}
	}

	/**
	 * Finalises the current void object: commits the last frame's ROI,
	 * adds the LinkSet to the cell DataSet, and re-enables navigation.
	 */
	public void endObject() {
		if (currentImagePlus.getRoi() == null) {
			panel.dialogAlert("Must select an outline for the final frame");
			return;
		}
		if (!withinBounds(currentImagePlus.getRoi())) {
			panel.dialogAlert("Outline must be within image bounds");
			return;
		}

		// Commit final frame's segment
		segment = getSegment(frame - 1 + cellFirstFrame, currentImagePlus.getRoi()); // frame is 1-based slice; full-stack index = frame-1+cellFirstFrame
		segment.getRoi().setPosition(frame);
		segment.setLinkSet(linkSet);
		linkSet.add(segment);

		// Add overlay
		for (Segment s : linkSet) {
			Roi roi = s.getRoi();
			if (roi != null) {
				roi.setStrokeColor(color);
				roi.setStrokeWidth(2);
				overlay.add(roi);
			}
		}
		currentImagePlus.updateAndDraw();

		// linkSet was registered with cellDataSet by the LinkSet constructor in
		// startObject() — do NOT call cellDataSet.addLinkSet(linkSet) again here.
		for (Segment s : linkSet) {
			int fIdx = s.getFrame() - cellFirstFrame;
			if (fIdx >= 0 && fIdx < cellDataSet.getFrameSetList().length) {
				cellDataSet.getFrameSet(fIdx).add(s);
			}
		}

		// Commit to currentChildDS in full-image coords via convertToOffset.
		// new LinkSet(currentChildDS) auto-registers with currentChildDS — no
		// explicit addLinkSet() call needed or wanted.
		LinkSet parentCell = parentLinkSets.get(cellIndex);
		LinkSet fullImageLS = new LinkSet(currentChildDS);
		for (Segment s : linkSet) {
			int f = s.getFrame();
			Point cp       = s.getCenterPoint();
			Point fullCp   = (cp != null)
					? convertToOffset(cellIndex, f, cp.x, cp.y)
					: convertToOffset(cellIndex, f, 0, 0);
			Point[] canvasPerim = s.getExternalPerimeter();
			Point[] fullPerim   = null;
			if (canvasPerim != null) {
				fullPerim = new Point[canvasPerim.length];
				for (int i = 0; i < canvasPerim.length; i++) {
					Point fp = convertToOffset(cellIndex, f, canvasPerim[i].x, canvasPerim[i].y);
					fullPerim[i] = fp;
				}
			}
			Segment fullSeg = new Segment(f, fullCp);
			fullSeg.setExternalPerimeter(fullPerim);
			fullSeg.setLinkSet(fullImageLS);
			fullImageLS.add(fullSeg);
			if (f >= 0 && f < currentChildDS.getFrameSetList().length) {
				currentChildDS.getFrameSet(f).add(fullSeg);
			}
		}
		currentChildDS.setExternalSegmentationExists(true);
		currentChildDS.setIdentificationExists(true);
		canvasToChildLS.put(linkSet, fullImageLS);

		// Accumulate into session-wide dataset (explicit add needed — fullImageLS
		// was constructed with currentChildDS, not accumulatedDataSet).
		accumulatedDataSet.addLinkSet(fullImageLS);
		accumulatedDataSet.addChildParentMapping(fullImageLS, parentCell);
		for (Segment s : fullImageLS) {
			int f = s.getFrame();
			if (f >= 0 && f < accumulatedDataSet.getFrameSetList().length) {
				accumulatedDataSet.getFrameSet(f).add(s);
			}
		}

		// Void successfully committed — unlock cell-switching so the user may
		// navigate to another cell or remain here to draw another void.
		segmentationInProgress = false;
		if (sidebar != null) sidebar.setCellSwitchingEnabled(true);
		if (frameScrollbar != null) frameScrollbar.setEnabled(true);
		panel.stateObject(true, false, false, false, true);
	}

	/** Ends the entire recursive manual session. */
	public void exit() {
		finishSession();
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Modification actions (delete / merge)
	// ═══════════════════════════════════════════════════════════════════════════

	// ═══════════════════════════════════════════════════════════════════════════
	// Per-segment SARN area editing ("Redraw Segment")
	//
	// Corrects a single frame's SARN envelope (externalPerimeter) within an
	// already-committed void track, without deleting and re-drawing the whole
	// track. Reuses the existing click-to-select mechanism (selectObject() /
	// getRoiSelected()) that Delete and Merge already use — this action just
	// requires exactly one track selected, then acts on whichever frame is
	// currently displayed within it (navigate there first via the frame
	// scrollbar). The edit replaces the Segment object at that frame — same
	// LinkSet, same position, so the track stays linked ahead and behind
	// exactly as before — using the same getSegment()/convertToOffset() path
	// every other manual draw in this controller already uses.
	//
	// TODO: if this cell already has internal segmentation results, redrawing
	// a segment's external boundary can invalidate them (the internal
	// perimeter was computed against the old boundary). It would be
	// reasonable to warn the user before allowing the redraw in that case.
	// Not implemented — the user is currently responsible for re-running
	// (restricted) internal segmentation manually afterward if needed.
	// ═══════════════════════════════════════════════════════════════════════════

	/** True while a Redraw Segment edit is armed (drawing in progress, awaiting Apply/Cancel). */
	private boolean redrawInProgress = false;

	/** The canvas-space LinkSet whose current-frame segment is being redrawn. */
	private LinkSet redrawTargetLinkSet;

	/**
	 * Returns the single LinkSet with any selected (red-highlighted) segment in
	 * cellDataSet, or null if zero or more than one distinct LinkSet is selected.
	 */
	private LinkSet getSingleSelectedLinkSet() {
		LinkSet found = null;
		for (int i = 0; i < cellDataSet.getFrameSetList().length; i++) {
			for (Segment seg : cellDataSet.getFrameSet(i)) {
				if (seg.getRoiSelected()) {
					LinkSet ls = seg.getLinkSet();
					if (ls == null) continue;
					if (found == null) found = ls;
					else if (found != ls) return null; // more than one distinct track selected
				}
			}
		}
		return found;
	}

	/** Finds the segment belonging to {@code ls} on relative frame {@code relFrame} in cellDataSet, or null. */
	private Segment getCanvasSegment(LinkSet ls, int relFrame) {
		if (relFrame < 0 || relFrame >= cellDataSet.getFrameSetList().length) return null;
		for (Segment s : cellDataSet.getFrameSet(relFrame)) {
			if (s.getLinkSet() == ls) return s;
		}
		return null;
	}

	/**
	 * Begins a Redraw Segment edit: validates exactly one track is selected and that
	 * it has a segment on the currently displayed frame, then arms drawing mode.
	 */
	public void startRedrawSegment() {
		LinkSet target = getSingleSelectedLinkSet();
		if (target == null) {
			panel.dialogAlert("Select exactly one object (click to select) before redrawing a segment.");
			return;
		}

		int relFrame = currentImagePlus.getCurrentSlice() - 1;
		Segment canvasSeg = getCanvasSegment(target, relFrame);
		if (canvasSeg == null) {
			panel.dialogAlert("The selected object has no segment on the current frame. "
					+ "Navigate to a frame within its track, then try again.");
			return;
		}

		redrawTargetLinkSet = target;
		redrawInProgress = true;
		if (frameScrollbar != null) frameScrollbar.setEnabled(false);
		if (sidebar != null) sidebar.setCellSwitchingEnabled(false);
		IJ.setTool(Prefs.get(ManualSegmentationController.PREF_TOOL, "polygon"));
		currentImagePlus.setRoi(canvasSeg.getRoi()); // preload the current outline as a starting point
		panel.setRedrawSegmentPanel();
	}

	/**
	 * Commits the drawn outline as the replacement for the selected track's segment
	 * on the current frame — canvas, per-cell child dataset, and the session-wide
	 * accumulated dataset all get the same object swapped in at the same position.
	 */
	public void applyRedrawSegment() {
		if (currentImagePlus.getRoi() == null) {
			panel.dialogAlert("Must draw a new outline before applying.");
			return;
		}
		if (!withinBounds(currentImagePlus.getRoi())) {
			panel.dialogAlert("Outline must be within image bounds");
			return;
		}

		int relFrame = currentImagePlus.getCurrentSlice() - 1;
		int absFrame = relFrame + cellFirstFrame;

		Segment oldCanvasSeg = getCanvasSegment(redrawTargetLinkSet, relFrame);
		if (oldCanvasSeg == null) {
			panel.dialogAlert("Could not locate the segment to replace. Redraw cancelled.");
			cancelRedrawSegment();
			return;
		}

		// ── Canvas-space replace ──────────────────────────────────────────────
		Segment newCanvasSeg = getSegment(absFrame, currentImagePlus.getRoi());
		newCanvasSeg.setLinkSet(redrawTargetLinkSet);

		int trackIdx = redrawTargetLinkSet.indexOf(oldCanvasSeg);
		if (trackIdx >= 0) redrawTargetLinkSet.set(trackIdx, newCanvasSeg);

		FrameSet canvasFrameSet = cellDataSet.getFrameSet(relFrame);
		int canvasIdx = canvasFrameSet.indexOf(oldCanvasSeg);
		if (canvasIdx >= 0) canvasFrameSet.set(canvasIdx, newCanvasSeg);

		if (oldCanvasSeg.getRoi() != null) overlay.remove(oldCanvasSeg.getRoi());
		Roi newRoi = newCanvasSeg.getRoi();
		if (newRoi != null) {
			newRoi.setStrokeColor(color);
			newRoi.setStrokeWidth(2);
			overlay.add(newRoi);
		}

		// ── Full-image-space replace (currentChildDS + accumulatedDataSet) ───
		// fullImageLS and its Segments are shared by reference between currentChildDS
		// and accumulatedDataSet (see endObject()), but each DataSet's FrameSet is a
		// separate List holding that same reference — replacing the object identity
		// means updating all three lists (track, child FrameSet, accumulated FrameSet).
		LinkSet fullImageLS = canvasToChildLS.get(redrawTargetLinkSet);
		if (fullImageLS != null) {
			Segment oldFullSeg = null;
			for (Segment s : fullImageLS) {
				if (s.getFrame() == absFrame) { oldFullSeg = s; break; }
			}
			if (oldFullSeg != null) {
				Point[] canvasPerim = newCanvasSeg.getExternalPerimeter();
				Point centreCanvas  = newCanvasSeg.getCenterPoint();
				Point fullCp = convertToOffset(cellIndex, absFrame, centreCanvas.x, centreCanvas.y);
				Point[] fullPerim = null;
				if (canvasPerim != null) {
					fullPerim = new Point[canvasPerim.length];
					for (int i = 0; i < canvasPerim.length; i++) {
						fullPerim[i] = convertToOffset(cellIndex, absFrame, canvasPerim[i].x, canvasPerim[i].y);
					}
				}
				Segment newFullSeg = new Segment(absFrame, fullCp);
				newFullSeg.setExternalPerimeter(fullPerim);
				newFullSeg.setLinkSet(fullImageLS);

				int fullTrackIdx = fullImageLS.indexOf(oldFullSeg);
				if (fullTrackIdx >= 0) fullImageLS.set(fullTrackIdx, newFullSeg);

				if (absFrame >= 0 && absFrame < currentChildDS.getFrameSetList().length) {
					FrameSet childFrameSet = currentChildDS.getFrameSet(absFrame);
					int childIdx = childFrameSet.indexOf(oldFullSeg);
					if (childIdx >= 0) childFrameSet.set(childIdx, newFullSeg);
				}
				if (absFrame >= 0 && absFrame < accumulatedDataSet.getFrameSetList().length) {
					FrameSet accFrameSet = accumulatedDataSet.getFrameSet(absFrame);
					int accIdx = accFrameSet.indexOf(oldFullSeg);
					if (accIdx >= 0) accFrameSet.set(accIdx, newFullSeg);
				}
			}
		}

		// Clear selection across the whole track — it was selected (red) to enter this
		// mode; leave everything deselected afterward, matching mergeSelectedObjects().
		for (Segment s : redrawTargetLinkSet) {
			s.setRoiSelected(false);
			if (s.getRoi() != null) s.getRoi().setStrokeColor(color);
		}

		currentImagePlus.updateAndDraw();
		finishRedrawSegment();
	}

	/** Discards the drawn outline and returns to the modification panel without changing any data. */
	public void cancelRedrawSegment() {
		currentImagePlus.killRoi();
		finishRedrawSegment();
	}

	/** Restores normal navigation and returns to the modification panel after Apply or Cancel. */
	private void finishRedrawSegment() {
		redrawInProgress = false;
		redrawTargetLinkSet = null;
		if (frameScrollbar != null) frameScrollbar.setEnabled(true);
		if (sidebar != null) sidebar.setCellSwitchingEnabled(true);
		IJ.setTool("hand");
		panel.setModificationPanel();
	}

	/** Switches to the modification panel and enables click-to-select on the canvas. */
	public void modifyMenu() {
		inSegmentMode = false;
		segmentationInProgress = false;
		if (sidebar != null) sidebar.setCellSwitchingEnabled(true);
		panel.setModificationPanel();
		IJ.setTool("hand");
		overlay.selectable(false);

		if (!mouseListenerActive) {
			mouseListenerActive = true;
			ImageCanvas ic = currentImagePlus.getCanvas();
			if (ic != null) {
				ic.addMouseListener(new java.awt.event.MouseAdapter() {
					@Override
					public void mousePressed(java.awt.event.MouseEvent event) {
						// While a Redraw Segment edit is armed, clicks are placing polygon/freehand
						// vertices for the new outline — must not also toggle selection underneath.
						if (mouseListenerActive && !redrawInProgress) {
							selectObject(
								ic.offScreenX(event.getX()),
								ic.offScreenY(event.getY()));
						}
					}
				});
			}
		} else {
			mouseListenerActive = true;
		}
	}

	/**
	 * Toggles selection on the LinkSet whose segment contains (x, y) on the
	 * current frame. Selected segments are highlighted in red; deselected
	 * segments revert to green.
	 */
	private void selectObject(int x, int y) {
		int fIdx = currentImagePlus.getCurrentSlice() - 1;
		if (fIdx < 0 || fIdx >= cellDataSet.getFrameSetList().length) return;

		for (Segment seg : cellDataSet.getFrameSet(fIdx)) {
			if (seg.getRoi() != null && seg.getRoi().contains(x, y)) {
				LinkSet ls = seg.getLinkSet();
				if (ls == null) continue;
				for (Segment s : ls) {
					s.setRoiSelected(!s.getRoiSelected());
					if (s.getRoi() != null) {
						s.getRoi().setStrokeColor(s.getRoiSelected() ? altColor : color);
					}
				}
			}
		}
		currentImagePlus.getCanvas().repaintOverlay();
	}

	/** Deletes all currently selected (red-highlighted) LinkSets from the cell DataSet. */
	public void deleteSelectedObject() {
		for (int i = 0; i < cellDataSet.getFrameSetList().length; i++) {
			for (int j = 0; j < cellDataSet.getFrameSet(i).size(); j++) {
				Segment seg = cellDataSet.getFrameSet(i).get(j);
				if (seg.getRoiSelected()) {
					LinkSet ls = seg.getLinkSet();
					if (ls != null) {
						// Remove from canvas-space cellDataSet and overlay
						for (Segment s : ls) {
							int fIdx = s.getFrame() - cellFirstFrame;
							if (fIdx >= 0 && fIdx < cellDataSet.getFrameSetList().length) {
								cellDataSet.getFrameSet(fIdx).remove(s);
							}
							if (s.getRoi() != null) overlay.remove(s.getRoi());
						}
						cellDataSet.getLinkSetList().remove(ls);

						// Also remove from currentChildDS (full-image source of truth)
						LinkSet childLS = canvasToChildLS.remove(ls);
						if (childLS != null) {
							for (Segment s : childLS) {
								int f = s.getFrame();
								if (f >= 0 && f < currentChildDS.getFrameSetList().length) {
									currentChildDS.getFrameSet(f).remove(s);
								}
							}
							currentChildDS.getLinkSetList().remove(childLS);
							// Also remove from accumulatedDataSet
							for (Segment s : childLS) {
								int f = s.getFrame();
								if (f >= 0 && f < accumulatedDataSet.getFrameSetList().length) {
									accumulatedDataSet.getFrameSet(f).remove(s);
								}
							}
							accumulatedDataSet.getLinkSetList().remove(childLS);
						}
					}
					j--;
				}
			}
		}
		currentImagePlus.updateAndDraw();
	}

	/**
	 * Merges two selected LinkSets into one combined outline using the same
	 * algorithm as ManualSegmentationController.mergeObject().
	 */
	public void mergeSelectedObjects() {
		LinkSet[] mergeSet = new LinkSet[2];
		int startSet1 = Integer.MAX_VALUE, startSet2 = Integer.MAX_VALUE;
		int endSet1 = 0, endSet2 = 0;

		for (int i = 0; i < cellDataSet.getFrameSetList().length; i++) {
			for (int j = 0; j < cellDataSet.getFrameSet(i).size(); j++) {
				Segment seg = cellDataSet.getFrameSet(i).get(j);
				if (!seg.getRoiSelected()) continue;
				LinkSet ls = seg.getLinkSet();
				if (ls == null) continue;

				if (mergeSet[0] != null && mergeSet[0] == ls) {
					if (seg.getFrame() > endSet1) endSet1 = seg.getFrame();
					continue;
				}
				if (mergeSet[1] != null && mergeSet[1] == ls) {
					if (seg.getFrame() > endSet2) endSet2 = seg.getFrame();
					continue;
				}
				if (mergeSet[0] == null) {
					mergeSet[0] = ls;
					startSet1 = seg.getFrame();
					endSet1 = seg.getFrame();
				} else if (mergeSet[1] == null) {
					mergeSet[1] = ls;
					startSet2 = seg.getFrame();
					endSet2 = seg.getFrame();
				} else {
					panel.dialogAlert("Only two objects can be merged at a time");
					return;
				}
			}
		}

		if (mergeSet[0] == null || mergeSet[1] == null) {
			panel.dialogAlert("Select exactly two objects to merge");
			return;
		}

		if (startSet1 > endSet2 || startSet2 > endSet1) {
			panel.dialogAlert("No frame overlap between selections");
			return;
		}

		int start = Math.min(startSet1, startSet2);
		int end = Math.max(endSet1, endSet2);

		LinkSet newLink = new LinkSet(cellDataSet);
		for (int f = start; f <= end; f++) {
			int fIdx = f - cellFirstFrame;
			if (fIdx < 0 || fIdx >= cellDataSet.getFrameSetList().length) continue;

			Segment seg1 = null, seg2 = null;
			for (Segment seg : cellDataSet.getFrameSet(fIdx)) {
				if (seg.getLinkSet() == mergeSet[0]) seg1 = seg;
				if (seg.getLinkSet() == mergeSet[1]) seg2 = seg;
			}
			if (seg1 == null && seg2 == null) continue;
			if (seg1 != null && seg2 == null) { newLink.add(new Segment(seg1)); continue; }
			if (seg1 == null) { newLink.add(new Segment(seg2)); continue; }

			// Both present — merge their perimeters
			Point[] A = GeometricCalculations.straightPerimeter(seg1.getExternalPerimeter());
			Point[] B = GeometricCalculations.straightPerimeter(seg2.getExternalPerimeter());
			if (!GeometricCalculations.boundryOverlap(A, B, 0)) {
				panel.dialogAlert("Selected objects are not overlapping on frame " + f);
				return;
			}
			newLink.add(mergeSegments(seg1, seg2));
		}

		// Remove old, add merged
		deleteObjectInternal(mergeSet[0]);
		deleteObjectInternal(mergeSet[1]);
		cellDataSet.addLinkSet(newLink);
		for (Segment seg : newLink) {
			int fIdx = seg.getFrame() - cellFirstFrame;
			if (fIdx >= 0 && fIdx < cellDataSet.getFrameSetList().length) {
				cellDataSet.getFrameSet(fIdx).add(seg);
			}
			seg.setLinkSet(newLink);
			seg.setRoi(getPolygonRoi(seg.getExternalPerimeter()));
			seg.getRoi().setStrokeColor(color);
			seg.getRoi().setStrokeWidth(2);
			seg.setRoiSelected(false);
			seg.getRoi().setPosition(seg.getFrame() - cellFirstFrame + 1);
			overlay.add(seg.getRoi());
		}

		// ── Update canvasToChildLS, currentChildDS, and accumulatedDataSet ───
		// The two old canvas LinkSets had corresponding full-image entries that
		// must be removed, and a new merged full-image entry must be created from
		// the merged canvas segments — exactly mirroring the endObject() commit path.

		// 1. Remove old child DS entries for both merged canvas objects
		LinkSet childLS1 = canvasToChildLS.remove(mergeSet[0]);
		LinkSet childLS2 = canvasToChildLS.remove(mergeSet[1]);

		if (childLS1 != null) {
			for (Segment s : childLS1) {
				int f = s.getFrame();
				if (f >= 0 && f < currentChildDS.getFrameSetList().length)
					currentChildDS.getFrameSet(f).remove(s);
				if (f >= 0 && f < accumulatedDataSet.getFrameSetList().length)
					accumulatedDataSet.getFrameSet(f).remove(s);
			}
			currentChildDS.getLinkSetList().remove(childLS1);
			accumulatedDataSet.getLinkSetList().remove(childLS1);
		}
		if (childLS2 != null) {
			for (Segment s : childLS2) {
				int f = s.getFrame();
				if (f >= 0 && f < currentChildDS.getFrameSetList().length)
					currentChildDS.getFrameSet(f).remove(s);
				if (f >= 0 && f < accumulatedDataSet.getFrameSetList().length)
					accumulatedDataSet.getFrameSet(f).remove(s);
			}
			currentChildDS.getLinkSetList().remove(childLS2);
			accumulatedDataSet.getLinkSetList().remove(childLS2);
		}

		// 2. Build new full-image-space LinkSet from merged canvas segments
		LinkSet parentCell = parentLinkSets.get(cellIndex);
		LinkSet mergedChildLS = new LinkSet(currentChildDS); // auto-registers with currentChildDS
		for (Segment canvasSeg : newLink) {
			int f = canvasSeg.getFrame();
			Point cp     = canvasSeg.getCenterPoint();
			Point fullCp = (cp != null)
					? convertToOffset(cellIndex, f, cp.x, cp.y)
					: convertToOffset(cellIndex, f, 0, 0);
			Point[] canvasPerim = canvasSeg.getExternalPerimeter();
			Point[] fullPerim   = null;
			if (canvasPerim != null) {
				fullPerim = new Point[canvasPerim.length];
				for (int i = 0; i < canvasPerim.length; i++) {
					fullPerim[i] = convertToOffset(cellIndex, f, canvasPerim[i].x, canvasPerim[i].y);
				}
			}
			Segment fullSeg = new Segment(f, fullCp);
			fullSeg.setExternalPerimeter(fullPerim);
			fullSeg.setLinkSet(mergedChildLS);
			mergedChildLS.add(fullSeg);
			if (f >= 0 && f < currentChildDS.getFrameSetList().length) {
				currentChildDS.getFrameSet(f).add(fullSeg);
			}
		}

		// 3. Register merged entry in accumulatedDataSet
		accumulatedDataSet.addLinkSet(mergedChildLS);
		accumulatedDataSet.addChildParentMapping(mergedChildLS, parentCell);
		for (Segment s : mergedChildLS) {
			int f = s.getFrame();
			if (f >= 0 && f < accumulatedDataSet.getFrameSetList().length) {
				accumulatedDataSet.getFrameSet(f).add(s);
			}
		}

		// 4. Map canvas → child DS so future delete/merge can find this entry
		canvasToChildLS.put(newLink, mergedChildLS);

		currentImagePlus.updateAndDraw();
	}

	// ── Preferences ──────────────────────────────────────────────────────────

	/** Opens a colour chooser; applies the chosen colour to the draw tool outline and all non-selected overlay ROIs, and persists via Prefs. */
	public void changeRoiColor() {
		Color chosen = javax.swing.JColorChooser.showDialog(panel, "Choose ROI Outline Color", color);
		if (chosen == null) return;
		color = chosen;
		Prefs.set(ManualSegmentationController.PREF_ROI_COLOR,
		          String.format("#%06x", chosen.getRGB() & 0xFFFFFF));
		ij.gui.Roi.setColor(color);
		for (LinkSet ls : cellDataSet.getLinkSetList()) {
			for (Segment s : ls) {
				if (s.getRoi() != null && !s.getRoiSelected())
					s.getRoi().setStrokeColor(color);
			}
		}
		currentImagePlus.updateAndDraw();
	}

	/** Toggles the drawing tool between polygon and freehand, persisting the choice via Prefs. */
	public void toggleDrawTool() {
		String next = "polygon".equals(Prefs.get(ManualSegmentationController.PREF_TOOL, "polygon"))
		              ? "freehand" : "polygon";
		Prefs.set(ManualSegmentationController.PREF_TOOL, next);
		panel.updateDrawToolButton(next);
	}

	private static Color decodeColor(String hex) {
		try { return Color.decode(hex); }
		catch (NumberFormatException e) { return new Color(0, 255, 0); }
	}

	/** Internal helper: removes a LinkSet from cellDataSet and overlay without repaint. */
	private void deleteObjectInternal(LinkSet ls) {
		for (Segment s : ls) {
			int fIdx = s.getFrame() - cellFirstFrame;
			if (fIdx >= 0 && fIdx < cellDataSet.getFrameSetList().length) {
				cellDataSet.getFrameSet(fIdx).remove(s);
			}
			if (s.getRoi() != null) overlay.remove(s.getRoi());
		}
		cellDataSet.getLinkSetList().remove(ls);
	}

	/**
	 * Merges two overlapping segments into one by combining their perimeters.
	 * Same algorithm as ManualSegmentationController.mergeSegments().
	 */
	private Segment mergeSegments(Segment segA, Segment segB) {
		Point[] A = GeometricCalculations.straightPerimeter(segA.getExternalPerimeter());
		Point[] B = GeometricCalculations.straightPerimeter(segB.getExternalPerimeter());

		// Find points of contact
		ArrayList<Point> contacts = new ArrayList<>();
		ArrayList<Integer> indexes = new ArrayList<>();
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < B.length; j++) {
				if (A[i].x == B[j].x && A[i].y == B[j].y) {
					contacts.add(A[i]);
					indexes.add(i);
				}
			}
		}

		if (contacts.isEmpty()) {
			// Fallback: return the larger segment
			int sizeA = GeometricCalculations.getAreaByRoi(A).length;
			int sizeB = GeometricCalculations.getAreaByRoi(B).length;
			return sizeA > sizeB ? new Segment(segA) : new Segment(segB);
		}

		// Find start and end points for longest intersection gap
		int maxLength = 0;
		int start = 0, end = 0;
		Point startIntersect = contacts.get(0), endIntersect = contacts.get(0);
		for (int i = 1; i < indexes.size(); i++) {
			if (indexes.get(i) - indexes.get(i - 1) > maxLength) {
				maxLength = indexes.get(i) - indexes.get(i - 1);
				start = indexes.get(i - 1);
				end = indexes.get(i);
				startIntersect = contacts.get(i - 1);
				endIntersect = contacts.get(i);
			}
		}

		if (indexes.get(0) + (A.length + 1 - indexes.get(indexes.size() - 1)) > maxLength) {
			start = indexes.get(indexes.size() - 1);
			end = indexes.get(0);
			startIntersect = contacts.get(contacts.size() - 1);
			endIntersect = contacts.get(0);
		}

		// Build segment A portion
		ArrayList<Point> listA1 = new ArrayList<>();
		int n = start;
		while (n != end) {
			if (n > A.length - 1) n = n - A.length + 1;
			listA1.add(A[n]);
			n++;
		}

		// Find B indices for start/end intersections
		int startIndexB = -1, endIndexB = -1;
		for (int i = 0; i < B.length; i++) {
			if (B[i].x == startIntersect.x && B[i].y == startIntersect.y) endIndexB = i;
			if (B[i].x == endIntersect.x && B[i].y == endIntersect.y) startIndexB = i;
		}

		// Build two candidate merged perimeters (B traversed in each direction)
		ArrayList<Point> listB1 = new ArrayList<>(listA1);
		n = startIndexB + 1;
		while (n != endIndexB) {
			if (n > B.length - 1) n = n - B.length;
			listB1.add(B[n]);
			n++;
		}

		ArrayList<Point> listB2 = new ArrayList<>(listA1);
		n = startIndexB - 1;
		while (n != endIndexB) {
			if (n < 0) n = n + B.length;
			listB2.add(B[n]);
			n--;
		}

		// Pick the larger area candidate
		Point[] b1Arr = listB1.toArray(new Point[0]);
		Point[] b2Arr = listB2.toArray(new Point[0]);
		int lenB1 = GeometricCalculations.getAreaByRoi(b1Arr).length;
		int lenB2 = GeometricCalculations.getAreaByRoi(b2Arr).length;
		Point[] C = lenB1 > lenB2 ? b1Arr : b2Arr;

		// Use centerpoint from the larger original segment
		int sizeA = GeometricCalculations.getAreaByRoi(A).length;
		int sizeB = GeometricCalculations.getAreaByRoi(B).length;
		Point centerPoint = sizeA > sizeB ? segA.getCenterPoint() : segB.getCenterPoint();

		Segment segC = new Segment(segA);
		segC.setManuallyEdited(true);
		segC.setCenterPoint(centerPoint);
		segC.setExternalPerimeter(C);
		return segC;
	}

	/** Creates a PolygonRoi from a Point array. */
	private PolygonRoi getPolygonRoi(Point[] pointList) {
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, pointList.length, Roi.POLYGON);
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Internal helpers
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Builds the cropped {@link ImagePlus} for the given cell using crop origins
	 * read directly from {@link #cellFrameOffsets} — the matrix is the sole
	 * source of truth so the pixel layout always matches the overlay coordinates.
	 *
	 * @param cellIdx  index into the offset matrix for this cell
	 * @param parentCell  the parent LinkSet (used only for the title)
	 */
	private ImagePlus buildCenteredImagePlus(int cellIdx, LinkSet parentCell) {
		int cw = cellCanvasWidth[cellIdx];
		int ch = cellCanvasHeight[cellIdx];
		ImageStack canvas = new ImageStack(cw, ch);
		// Slice 1 in the resulting ImagePlus = full-stack frame cellFirstFrame.
		for (int f = cellFirstFrame; f <= cellLastFrame; f++) {
			ImageProcessor source = fullStack.getProcessor(f + 1); // 1-based
			ImageProcessor slice  = source.createProcessor(cw, ch);
			int ox = cellFrameOffsets[cellIdx][f][0];
			int oy = cellFrameOffsets[cellIdx][f][1];
			for (int y = 0; y < ch; y++) {
				int srcY = oy + y;
				if (srcY < 0 || srcY >= source.getHeight()) continue;
				for (int x = 0; x < cw; x++) {
					int srcX = ox + x;
					if (srcX < 0 || srcX >= source.getWidth()) continue;
					slice.set(x, y, source.get(srcX, srcY));
				}
			}
			canvas.addSlice(slice);
		}
		return new ImagePlus("Cell: " + parentCell.getDisplayName(), canvas);
	}

	/**
	 * Loads any existing automated segmentation results for this parent cell
	 * into {@code cellDataSet} so they are editable, and draws them on the
	 * overlay in green (same colour as manually drawn segments) so auto and
	 * manual results are visually interchangeable.
	 *
	 * Coordinates are translated from full-image space into canvas space
	 * using the per-frame offsets.
	 */
	/**
	 * Reads all existing void segments from {@link #currentChildDS} and draws
	 * them on the overlay as canvas-space ROIs, ready for display and selection.
	 *
	 * Coordinate conversion uses {@link #convertFromOffset} with the pre-built
	 * offset matrix, so the result is identical on every visit regardless of any
	 * canvas-size state at call time.
	 *
	 * The canvas-copy LinkSets created here are registered with {@link #cellDataSet}
	 * automatically by the {@link LinkSet} constructor.  Explicit
	 * {@code cellDataSet.addLinkSet()} calls are therefore intentionally absent.
	 *
	 * For auto-SARN child DataSet segments whose coordinates are already in
	 * full-image space (stored offset == 0,0), {@link #convertFromOffset} degenerates
	 * to {@code fullX − currentFrameOffset}, which is correct.
	 */
	private void drawExistingResults(LinkSet parentCell) {
		if (currentChildDS == null) return;

		for (LinkSet ls : currentChildDS.getLinkSetList()) {
			// new LinkSet(cellDataSet) auto-registers with cellDataSet —
			// do NOT call cellDataSet.addLinkSet(copy) after this line.
			LinkSet copy = new LinkSet(cellDataSet);

			for (Segment seg : ls) {
				// Edit mode   → SARN envelope (externalPerimeter): the editable boundary.
				// Preview mode → segmented result (internalPerimeter) only; if restricted
				//               segmentation has not yet been run, skip this segment entirely
				//               so no overlay is shown rather than showing the wrong boundary.
				Point[] fullPerim;
				if (canEdit) {
					fullPerim = seg.getExternalPerimeter();
				} else {
					fullPerim = seg.getInternalPerimeter();
				}
				if (fullPerim == null || fullPerim.length == 0) continue;

				int f = seg.getFrame();
				float[] xPoints    = new float[fullPerim.length];
				float[] yPoints    = new float[fullPerim.length];
				Point[] canvasPerim = new Point[fullPerim.length];
				for (int i = 0; i < fullPerim.length; i++) {
					Point cp = convertFromOffset(cellIndex, f, fullPerim[i].x, fullPerim[i].y);
					xPoints[i]    = cp.x;
					yPoints[i]    = cp.y;
					canvasPerim[i] = cp;
				}

				Point rawCp    = seg.getCenterPoint();
				Point canvasCp = (rawCp != null)
						? convertFromOffset(cellIndex, f, rawCp.x, rawCp.y)
						: new Point(0, 0);
				Segment canvasSeg = new Segment(f, canvasCp);
				canvasSeg.setExternalPerimeter(canvasPerim);

				PolygonRoi roi = new PolygonRoi(xPoints, yPoints, fullPerim.length, Roi.POLYGON);
				roi.setStrokeColor(color);
				roi.setStrokeWidth(2);
				roi.setPosition(f - cellFirstFrame + 1);
				canvasSeg.setRoi(roi);
				canvasSeg.setLinkSet(copy);
				copy.add(canvasSeg);
				overlay.add(roi);

				int fIdx = f - cellFirstFrame;
				if (fIdx >= 0 && fIdx < cellDataSet.getFrameSetList().length) {
					cellDataSet.getFrameSet(fIdx).add(canvasSeg);
				}
			}

			if (copy.size() > 0) {
				canvasToChildLS.put(copy, ls);
			} else {
				// No displayable segments — remove the empty shell the constructor registered.
				cellDataSet.getLinkSetList().remove(copy);
			}
		}
	}

	/**
	 * Draws the parent cell's outline on the overlay in a faint colour so the
	 * user can see the cell boundary while segmenting voids inside it.
	 */
	private void drawParentOutline(LinkSet parentCell) {
		Color parentColor = new Color(255, 0, 0, 200);
		for (Segment seg : parentCell) {
			Point[] perim = seg.getInternalPerimeter();
			if (perim == null || perim.length == 0) continue;
			int f = seg.getFrame();
			float[] xPoints = new float[perim.length];
			float[] yPoints = new float[perim.length];
			for (int i = 0; i < perim.length; i++) {
				Point cp = convertFromOffset(cellIndex, f, perim[i].x, perim[i].y);
				xPoints[i] = cp.x;
				yPoints[i] = cp.y;
			}
			PolygonRoi roi = new PolygonRoi(xPoints, yPoints, perim.length, Roi.POLYGON);
			roi.setStrokeColor(parentColor);
			roi.setStrokeWidth(1);
			roi.setPosition(f - cellFirstFrame + 1);
			overlay.add(roi);
		}
	}

	/**
	 * Builds a {@link Segment} from the polygon ROI drawn on the canvas image.
	 * Coordinates are kept in canvas space here; translation to full-image
	 * space happens in {@link #endObject()} via {@link #convertToOffset}.
	 */
	private Segment getSegment(int frame, Roi roi) {
		FloatPolygon fp    = roi.getFloatPolygon();
		Point[]      perim = new Point[fp.npoints];
		int sumX = 0, sumY = 0;
		for (int i = 0; i < fp.npoints; i++) {
			perim[i] = new Point((int) fp.xpoints[i], (int) fp.ypoints[i]);
			sumX += perim[i].x;
			sumY += perim[i].y;
		}
		Point centre = new Point(sumX / fp.npoints, sumY / fp.npoints);

		Segment seg = new Segment(frame, centre);
		seg.setExternalPerimeter(perim);
		seg.setRoi((Roi) roi.clone());
		seg.getRoi().setStrokeColor(color);
		seg.getRoi().setStrokeWidth(2);
		return seg;
	}

	/**
	 * Sets an initial zoom on the image canvas so the displayed image is at least
	 * 25% of the screen width AND 25% of the screen height (aspect ratio locked).
	 *
	 * The required magnification to satisfy each constraint is:
	 *   magW = (0.25 * screenWidth)  / canvasWidth
	 *   magH = (0.25 * screenHeight) / canvasHeight
	 * We take the larger of the two so BOTH thresholds are met simultaneously.
	 * ImageJ magnifications below 1.0 are clamped to 1.0 (never zoom out past
	 * actual size — if the canvas is already larger than 25% of screen, leave it).
	 */
	// ── Window placement helpers ─────────────────────────────────────────────

	/**
	 * Returns the screen (GraphicsDevice) containing the current mouse pointer.
	 * Used to determine which monitor to centre the composite window on when the
	 * session opens.  Falls back to the default screen if the pointer cannot be
	 * queried (e.g. a headless environment).
	 */
	private static GraphicsDevice getScreenForMouse() {
		try {
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			for (GraphicsDevice gd : GraphicsEnvironment
					.getLocalGraphicsEnvironment().getScreenDevices()) {
				if (gd.getDefaultConfiguration().getBounds().contains(mouse))
					return gd;
			}
		} catch (Exception ignored) {}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	}

	/**
	 * Returns the screen that contains the largest portion of the given component.
	 * Used when the composite window is already on a specific monitor and we need
	 * to keep the control panel on that same screen.
	 */
	private static GraphicsDevice getScreenFor(java.awt.Component comp) {
		try {
			Rectangle cb = comp.getBounds();
			cb.setLocation(comp.getLocationOnScreen());
			GraphicsDevice best = null;
			int bestArea = 0;
			for (GraphicsDevice gd : GraphicsEnvironment
					.getLocalGraphicsEnvironment().getScreenDevices()) {
				Rectangle overlap = gd.getDefaultConfiguration().getBounds().intersection(cb);
				int area = overlap.isEmpty() ? 0 : overlap.width * overlap.height;
				if (area > bestArea) { bestArea = area; best = gd; }
			}
			if (best != null) return best;
		} catch (Exception ignored) {}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	}

	/**
	 * Positions the floating control panel just above the composite window,
	 * horizontally centred on it.
	 *
	 * <p>If the composite window sits close to the top of its screen and there is
	 * not enough room above, the panel overlaps the top portion of the composite
	 * window (shifted 10 px down from the composite's top edge) rather than
	 * sliding off-screen.  Either way the panel is always fully within the
	 * horizontal bounds of the screen.
	 */
	private void positionControlPanel() {
		if (compositeWindow == null || panel == null) return;

		Rectangle cw = compositeWindow.getBounds();
		Rectangle sb = getScreenFor(compositeWindow).getDefaultConfiguration().getBounds();
		int pw = panel.getWidth();
		int ph = panel.getHeight();

		// Horizontally centred on the composite window, clamped to screen edges
		int px = cw.x + (cw.width - pw) / 2;
		px = Math.max(sb.x, Math.min(sb.x + sb.width - pw, px));

		// Prefer sitting just above the composite window
		int py = cw.y - ph - 4;
		// If that would go off the top of the screen, overlap the top of the composite window
		if (py < sb.y) py = cw.y + 10;

		panel.setLocation(px, py);
	}

	private void applyInitialZoom() {
		if (window == null || currentImagePlus == null) return;

		Rectangle screen = getScreenForMouse().getDefaultConfiguration().getBounds();
		double minScreenW = screen.width  * 0.25;
		double minScreenH = screen.height * 0.25;

		double magForWidth  = minScreenW / canvasWidth;
		double magForHeight = minScreenH / canvasHeight;

		// Take the larger so both dimensions meet the 25% minimum
		double mag = Math.max(magForWidth, magForHeight);

		// Never zoom below 1:1 — only zoom IN when the canvas is small
		if (mag < 1.0) mag = 1.0;

		// Apply to ImageJ canvas
		ImageCanvas ic = currentImagePlus.getCanvas();
		if (ic == null) return;
		ic.setMagnification(mag);
		ic.setSize((int) Math.ceil(canvasWidth  * mag),
		           (int) Math.ceil(canvasHeight * mag));
		// Repack the composite so its layout adapts to the zoomed canvas size.
		// Do NOT call window.repaint() — the StackWindow is hidden; repaint there is a no-op.
		// Do NOT call window.pack() — that triggers StackWindow.componentResized which resets
		// the magnification, causing a resize→repaint→resize oscillation that pegs the CPU.
		compositeWindow.getImagePanel().revalidate();
		compositeWindow.pack();

	}

	/**
	 * Synchronises the full-image preview panel to the current frame displayed
	 * in the cropped cell image.
	 */
	private void syncPreview() {
		if (previewPanel != null && currentImagePlus != null) {
			int slice = currentImagePlus.getCurrentSlice(); // 1-based
			previewPanel.setFrame(slice - 1 + cellFirstFrame);  // 0-based full-stack
		}
	}

	/**
	 * Checks that the given ROI does not extend outside the current image bounds.
	 */
	private boolean withinBounds(Roi roi) {
		Rectangle r = roi.getBounds();
		return r.x >= 0
		    && r.y >= 0
		    && r.x + r.width  <= currentImagePlus.getWidth()
		    && r.y + r.height <= currentImagePlus.getHeight();
	}

	/** Closes everything, adjusts center points, returns data, and re-activates the main UI. */
	private void finishSession() {
		// Adjust center points for all accumulated void segments
		if (accumulatedDataSet != null && !accumulatedDataSet.getLinkSetList().isEmpty()) {
			adjustCenterPoints();
			accumulatedDataSet.setIdentificationExists(true);
			accumulatedDataSet.setLinkageExists(true);
			accumulatedDataSet.setExternalSegmentationExists(true);
		}

		if (currentImagePlus != null) {
			currentImagePlus.changes = false;
			currentImagePlus.close();
			currentImagePlus = null;
		}
		window = null;
		if (compositeWindow != null) {
			compositeWindow.dispose();
			compositeWindow = null;
		}
		panel.close();

		// Register the manual void results as the recursive panel's external-segmentation output.
		// This is the same call RecursionOperationModel makes at the end of runIt(0), so the
		// panel state (loadedExternalData, button enables, subsegment lock) is consistent
		// regardless of whether voids came from auto-SARN or manual drawing.
		//
		// Safety: setRunData sets *this* controller's dataSet field.  getPriorDataSet() reads
		// from Seg2TracksController.getDataSets() using subsegmentSelection or panelNumber-1,
		// neither of which is this panel's own dataSet slot.  So a subsequent manual session
		// still receives the primary-segmentation DataSet as its parent — not accumulatedDataSet.
		// The original concern that this would corrupt the outer-cell DataSet was therefore
		// unfounded; calling setRunData here is both safe and necessary.
		if (accumulatedDataSet != null && !accumulatedDataSet.getLinkSetList().isEmpty()) {
			controller.setRunData(0, accumulatedDataSet);
		}
		// Autosave the parent panel so child DataSet results are persisted.
		// autosave() on a child controller delegates to the parent automatically.
		controller.autosave();
		controller.setViewActive(true);
	}

	/**
	 * Adjusts center points for all void segments using intensity maxima
	 * from the blurred full image, mirroring ManualSegmentationController.adjustCenterPoints().
	 */
	private void adjustCenterPoints() {
		Identification id = new Identification();
		id.initialize(fullStack, accumulatedDataSet);
		id.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		id.setFinder(new ModifiedMaximumFinder(), controller.getMaximumFinderTolerance());
		id.runManualAdjustment();
	}
}
