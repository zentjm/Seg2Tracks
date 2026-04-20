package analysisMethod;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JProgressBar;

import calculations.Data;
import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.RecursiveLinkSetCalculation;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.RecursiveDataSet;
import dataStructure.ResultWorkbook;
import dataStructure.Segment;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

/**
 * Abstract base class for analysis of recursively-generated DataSets.
 *
 * Sibling to {@link OperationMethod}: where OperationMethod handles flat
 * DataSet analysis, RecursiveAnalysisMethod handles the parent–child hierarchy
 * produced by recursive segmentation (e.g. voids inside macrophages).
 *
 * <h3>Calculation tiers</h3>
 * <pre>
 *   CHILD VOID TIERS
 *     segmentCalculations()          — per void segment (area, circularity, …)
 *     childLinkSetCalculations()     — per void track aggregated across frames
 *     childLinkSetStatistics()       — statistics over void segments per track
 *     childFrameSetCalculations()    — child void population per timepoint
 *     childFrameSetStatistics()      — statistics over child void segments per frame
 *
 *   PARENT CELL TIERS
 *     parentSegmentCalculations()    — per parent-cell segment (for frame-level stats)
 *     parentLinkSetCalculations()    — aggregate per parent cell with child context
 *     parentFrameSetCalculations()   — parent cell population per timepoint
 *     parentFrameSetStatistics()     — statistics over parent-cell segments per frame
 * </pre>
 *
 * <h3>Output format</h3>
 * Wide format (one row per entity, one column per calculation/statistic),
 * optimised for Excel pivot chart integration.  Five sheets are produced:
 * <ol>
 *   <li>Void Segment Data      — one row per (child void × frame)
 *   <li>Void Track Data        — one row per child void track
 *   <li>Parent Cell Aggregation — one row per parent cell
 *   <li>Child Frame Data       — one row per timepoint (child void population)
 *   <li>Parent Frame Data      — one row per timepoint (parent cell population)
 * </ol>
 * Statistic columns use the pattern {@code StatName_CalcName} (e.g. Mean_Area).
 *
 * <h3>Color model</h3>
 * {@link #getParentColor(LinkSet)} is abstract; concrete subclasses choose parent
 * colors (fixed palette, stored on LinkSet, random, etc.).
 * {@link #getChildColor(LinkSet, LinkSet)} defaults to a hue-inherited lighter and
 * less-saturated variant of the parent color and may be overridden.
 *
 * <h3>Hierarchy extensibility</h3>
 * The {@link #parentDataSet} field is populated from
 * {@link RecursiveDataSet#getParentDataSet()}, exposing the ancestry chain for
 * deeper hierarchies.  Cross-parent (inter-cell) analysis requiring simultaneous
 * access to the full parent population is handled by the separate
 * {@link CrossRecursiveAnalysisMethod} stub.
 *
 * <h3>TODO</h3>
 * Recursive analysis panels should not be independently nameable in the UI.
 * Their output should be integrated into the parent panel's DataSet slot rather
 * than occupying an independent named slot.  Tracked for a future UI/model refactor.
 */
public abstract class RecursiveAnalysisMethod extends AnalysisMethod {

	// ── Data sources ──────────────────────────────────────────────────────────

	/** The recursively-generated child DataSet (voids inside parent cells). */
	RecursiveDataSet recursiveDataSet;

	/**
	 * The primary segmentation DataSet whose LinkSet interiors were segmented
	 * to produce {@link #recursiveDataSet}.  Populated from
	 * {@link RecursiveDataSet#getParentDataSet()} at initialize time.
	 * Stored as a field (not a local) to support deeper hierarchy traversal.
	 */
	DataSet parentDataSet;

	// ── Sheet indices ─────────────────────────────────────────────────────────

	int voidSegmentSheet;
	int childVoidTrackSheet;
	int parentCellSheet;
	int childFrameSheet;
	int parentFrameSheet;

	// ── Cached calculation name arrays ────────────────────────────────────────
	// Populated once in defineSheets() to build wide-format column headers.
	// Reused in retrieveCalculations() to identify which names to look up.

	String[] segmentCalcNames;
	String[] childLSCalcNames;
	String[] childLSStatNames;
	String[] parentSegCalcNames;
	String[] parentLSCalcNames;
	String[] childFSCalcNames;
	String[] childFSStatNames;
	String[] parentFSCalcNames;
	String[] parentFSStatNames;

	/**
	 * Configured {@link RecursiveLinkSetCalculation} instances keyed by parent
	 * LinkSet.  Populated in {@link #setCalculations()}, consumed in
	 * {@link #retrieveCalculations()}.  LinkedHashMap preserves parent cell
	 * insertion order for predictable output row ordering.
	 */
	Map<LinkSet, RecursiveLinkSetCalculation[]> parentCalcMap = new LinkedHashMap<>();

	// ═══════════════════════════════════════════════════════════════════════════
	// Initialization
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Initializes this analysis with a fresh workbook.
	 *
	 * @param target           the full-image ImagePlus (used for intensity calculations and overlay)
	 * @param recursiveDataSet the RecursiveDataSet containing all child void LinkSets
	 * @param progressBar      UI progress indicator
	 */
	public void initialize(ImagePlus target, RecursiveDataSet recursiveDataSet,
	                       JProgressBar progressBar) {
		this.target           = target;
		this.stack            = target.getImageStack();
		this.recursiveDataSet = recursiveDataSet;
		this.parentDataSet    = recursiveDataSet.getParentDataSet();
		this.dataSets         = new DataSet[] { recursiveDataSet }; // AnalysisMethod compat
		this.progressBar      = progressBar;
		workbook              = new ResultWorkbook();
	}

	/**
	 * Initializes this analysis appending results to an existing workbook.
	 *
	 * @param target           the full-image ImagePlus
	 * @param recursiveDataSet the RecursiveDataSet containing all child void LinkSets
	 * @param progressBar      UI progress indicator
	 * @param workbook         existing ResultWorkbook to append sheets to
	 */
	public void initialize(ImagePlus target, RecursiveDataSet recursiveDataSet,
	                       JProgressBar progressBar, ResultWorkbook workbook) {
		initialize(target, recursiveDataSet, progressBar);
		this.workbook = workbook;
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// AnalysisMethod overrides
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * No DataSet transformation is required for recursive analysis — the
	 * RecursiveDataSet is used directly as the authoritative source.
	 */
	@Override
	public final void analyze() {
		// intentional no-op: recursive data arrives pre-structured from the pipeline
	}

	/**
	 * Returns the channel label used by the analysis settings UI.
	 * Subclasses may override to return more specific labels.
	 */
	@Override
	public String[] getChannels() {
		return new String[] { "RecursiveDataSet:" };
	}

	/**
	 * Returns all {@link Data} objects across every calculation tier.
	 * Used by the analysis settings UI to enumerate available calculations.
	 */
	@Override
	public Data[] getCalculations() {
		List<Data> all = new ArrayList<>();
		collectAll(all, segmentCalculations());
		collectAll(all, childLinkSetCalculations());
		collectAll(all, childLinkSetStatistics());
		collectAll(all, parentSegmentCalculations());
		collectAll(all, parentLinkSetCalculations());
		collectAll(all, childFrameSetCalculations());
		collectAll(all, childFrameSetStatistics());
		collectAll(all, parentFrameSetCalculations());
		collectAll(all, parentFrameSetStatistics());
		return all.toArray(new Data[0]);
	}

	// ── Workbook pipeline ─────────────────────────────────────────────────────

	/**
	 * Creates five wide-format sheets in the workbook.
	 * Column headers are built dynamically from the calculation name arrays so
	 * adding a new calculation automatically extends the relevant sheet.
	 *
	 * Called first in the {@link AnalysisMethod#getWorkbook()} pipeline.
	 */
	@Override
	void defineSheets() {
		// Instantiate calculation arrays solely to harvest their names.
		// Fresh instances are created per data-object in setCalculations().
		segmentCalcNames   = names(segmentCalculations());
		childLSCalcNames   = names(childLinkSetCalculations());
		childLSStatNames   = names(childLinkSetStatistics());
		parentSegCalcNames = names(parentSegmentCalculations());
		parentLSCalcNames  = names(parentLinkSetCalculations());
		childFSCalcNames   = names(childFrameSetCalculations());
		childFSStatNames   = names(childFrameSetStatistics());
		parentFSCalcNames  = names(parentFrameSetCalculations());
		parentFSStatNames  = names(parentFrameSetStatistics());

		// 1. Void Segment Data — one row per (child void track × frame)
		voidSegmentSheet = workbook.addSheet("Void Segment Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Parent Cell", "Child Void", "Frame"},
				segmentCalcNames));

		// 2. Void Track Data — one row per child void track
		//    Stat columns: StatName_CalcName (e.g. Mean_Area)
		childVoidTrackSheet = workbook.addSheet("Void Track Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Parent Cell", "Child Void"},
				childLSCalcNames,
				statCalcHeaders(childLSStatNames, segmentCalcNames)));

		// 3. Parent Cell Aggregation — one row per parent cell
		parentCellSheet = workbook.addSheet("Parent Cell Aggregation",
			wideHeaders(
				new String[]{"DataSet", "Method", "Parent Cell"},
				parentLSCalcNames));

		// 4. Child Frame Data — one row per timepoint (child void population)
		childFrameSheet = workbook.addSheet("Child Frame Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Frame"},
				childFSCalcNames,
				statCalcHeaders(childFSStatNames, segmentCalcNames)));

		// 5. Parent Frame Data — one row per timepoint (parent cell population)
		parentFrameSheet = workbook.addSheet("Parent Frame Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Frame"},
				parentFSCalcNames,
				statCalcHeaders(parentFSStatNames, parentSegCalcNames)));
	}

	/**
	 * Configures all calculations on their respective data objects.
	 *
	 * Child void segment and LinkSet calculations are stored directly on the
	 * Segment / LinkSet objects (mirroring {@link OperationMethod}).
	 * Parent-level {@link RecursiveLinkSetCalculation} instances are stored in
	 * {@link #parentCalcMap} keyed by parent LinkSet because the parent
	 * LinkSets belong to the primary DataSet and have no built-in slot for
	 * this calculation type.
	 *
	 * Called second in the {@link AnalysisMethod#getWorkbook()} pipeline.
	 */
	@Override
	public void setCalculations() {

		// ── Child void segment and LinkSet tiers ──────────────────────────────
		progressBar.setString("Setting void calculations");
		progressBar.setMinimum(0);
		progressBar.setMaximum(recursiveDataSet.getLinkSetList().size());
		int progress = 0;

		for (LinkSet childLS : recursiveDataSet.getLinkSetList()) {

			// Segment-level calculations (fresh instances per segment)
			for (Segment seg : childLS) {
				SegmentCalculation[] calcs = segmentCalculations();
				if (calcs != null) {
					for (SegmentCalculation c : calcs) {
						c.setTargetStackSlice(stack, seg.getFrame() + 1);
						c.setSegments(seg);
						seg.setCalculation(c);
					}
				}
			}

			// Child LinkSet calculations
			LinkSetCalculation[] lsCalcs = childLinkSetCalculations();
			if (lsCalcs != null) {
				for (LinkSetCalculation c : lsCalcs) {
					childLS.setCalculation(c);
					c.setLinkSet(childLS);
				}
			}

			// Child LinkSet statistics (aggregate over segment calculations)
			LinkSetStatistic[] lsStats = childLinkSetStatistics();
			if (lsStats != null) {
				for (LinkSetStatistic s : lsStats) {
					childLS.setStatistic(s);
					s.setLinkSet(childLS);
				}
			}

			progressBar.setValue(++progress);
		}

		// ── Parent segment calculations (needed for parent frame-level stats) ─
		// Fresh instances per segment; stored on the parent cells' Segments so
		// parentFrameSetStatistics() can aggregate over them via the FrameSet.
		for (LinkSet parentLS : parentDataSet.getLinkSetList()) {
			for (Segment seg : parentLS) {
				SegmentCalculation[] calcs = parentSegmentCalculations();
				if (calcs != null) {
					for (SegmentCalculation c : calcs) {
						c.setTargetStackSlice(stack, seg.getFrame() + 1);
						c.setSegments(seg);
						seg.setCalculation(c);
					}
				}
			}
		}

		// ── Parent LinkSet aggregation (RecursiveLinkSetCalculation) ──────────
		// Fresh instances per parent cell; stored in parentCalcMap.
		parentCalcMap.clear();
		for (LinkSet parentLS : parentDataSet.getLinkSetList()) {
			List<LinkSet> children = recursiveDataSet.getChildLinkSets(parentLS);
			RecursiveLinkSetCalculation[] calcs = parentLinkSetCalculations();
			if (calcs != null) {
				for (RecursiveLinkSetCalculation c : calcs) {
					c.setContext(parentLS, children);
				}
				parentCalcMap.put(parentLS, calcs);
			}
		}

		// ── Child FrameSet calculations ───────────────────────────────────────
		for (int f = 0; f < recursiveDataSet.getFrameSetList().length; f++) {
			FrameSet fs = recursiveDataSet.getFrameSet(f);

			FrameSetCalculation[] fsCalcs = childFrameSetCalculations();
			if (fsCalcs != null) {
				for (FrameSetCalculation c : fsCalcs) {
					fs.setCalculation(c);
					c.setFrameSet(fs);
				}
			}

			FrameSetStatistic[] fsStats = childFrameSetStatistics();
			if (fsStats != null) {
				for (FrameSetStatistic s : fsStats) {
					fs.setStatistic(s);
					s.setFrameSet(fs);
				}
			}
		}

		// ── Parent FrameSet calculations ──────────────────────────────────────
		for (int f = 0; f < parentDataSet.getFrameSetList().length; f++) {
			FrameSet fs = parentDataSet.getFrameSet(f);

			FrameSetCalculation[] fsCalcs = parentFrameSetCalculations();
			if (fsCalcs != null) {
				for (FrameSetCalculation c : fsCalcs) {
					fs.setCalculation(c);
					c.setFrameSet(fs);
				}
			}

			FrameSetStatistic[] fsStats = parentFrameSetStatistics();
			if (fsStats != null) {
				for (FrameSetStatistic s : fsStats) {
					fs.setStatistic(s);
					s.setFrameSet(fs);
				}
			}
		}
	}

	/**
	 * Reads all configured calculations and writes wide-format rows to the workbook.
	 * Called third in the {@link AnalysisMethod#getWorkbook()} pipeline.
	 */
	@Override
	public void retrieveCalculations() {

		// ── Sheet 1: Void Segment Data ────────────────────────────────────────
		progressBar.setString("Retrieving void segment data");
		progressBar.setMinimum(0);
		progressBar.setMaximum(recursiveDataSet.getLinkSetList().size());
		int progress = 0;

		for (LinkSet childLS : recursiveDataSet.getLinkSetList()) {
			LinkSet parentLS  = recursiveDataSet.getParentLinkSet(childLS);
			String parentName = (parentLS != null) ? parentLS.getDisplayName() : "?";

			for (Segment seg : childLS) {
				List<Object> row = new ArrayList<>();
				row.add(recursiveDataSet.getName());
				row.add(methodName);
				row.add(parentName);
				row.add(childLS.getDisplayName());
				row.add(seg.getFrame() + 1);  // 1-based frame number for readability
				if (segmentCalcNames != null) {
					for (String name : segmentCalcNames) row.add(seg.getCalculation(name));
				}
				workbook.addLine(voidSegmentSheet, row.toArray());
			}
			progressBar.setValue(++progress);
		}

		// ── Sheet 2: Void Track Data ──────────────────────────────────────────
		progressBar.setString("Retrieving void track data");
		progressBar.setValue(0); progress = 0;

		for (LinkSet childLS : recursiveDataSet.getLinkSetList()) {
			LinkSet parentLS  = recursiveDataSet.getParentLinkSet(childLS);
			String parentName = (parentLS != null) ? parentLS.getDisplayName() : "?";

			List<Object> row = new ArrayList<>();
			row.add(recursiveDataSet.getName());
			row.add(methodName);
			row.add(parentName);
			row.add(childLS.getDisplayName());

			if (childLSCalcNames != null) {
				for (String name : childLSCalcNames) row.add(childLS.getCalculation(name));
			}
			// Stat columns: one per (stat × segment calculation) combination
			if (childLSStatNames != null && segmentCalcNames != null) {
				for (String stat : childLSStatNames) {
					for (String calc : segmentCalcNames) {
						row.add(childLS.getStatistic(stat, calc));
					}
				}
			}
			workbook.addLine(childVoidTrackSheet, row.toArray());
			progressBar.setValue(++progress);
		}

		// ── Sheet 3: Parent Cell Aggregation ──────────────────────────────────
		progressBar.setString("Retrieving parent cell aggregation data");
		progressBar.setMinimum(0);
		progressBar.setMaximum(parentDataSet.getLinkSetList().size());
		progressBar.setValue(0); progress = 0;

		for (LinkSet parentLS : parentDataSet.getLinkSetList()) {
			List<Object> row = new ArrayList<>();
			row.add(recursiveDataSet.getName());
			row.add(methodName);
			row.add(parentLS.getDisplayName());

			RecursiveLinkSetCalculation[] calcs = parentCalcMap.get(parentLS);
			if (calcs != null) {
				for (RecursiveLinkSetCalculation c : calcs) row.add(c.get());
			}
			workbook.addLine(parentCellSheet, row.toArray());
			progressBar.setValue(++progress);
		}

		// ── Sheet 4: Child Frame Data ─────────────────────────────────────────
		progressBar.setString("Retrieving child frame data");
		progressBar.setMinimum(0);
		progressBar.setMaximum(recursiveDataSet.getFrameSetList().length);
		progressBar.setValue(0); progress = 0;

		for (int f = 0; f < recursiveDataSet.getFrameSetList().length; f++) {
			FrameSet fs = recursiveDataSet.getFrameSet(f);

			List<Object> row = new ArrayList<>();
			row.add(recursiveDataSet.getName());
			row.add(methodName);
			row.add(f + 1); // 1-based

			if (childFSCalcNames != null) {
				for (String name : childFSCalcNames) row.add(fs.getCalculation(name));
			}
			if (childFSStatNames != null && segmentCalcNames != null) {
				for (String stat : childFSStatNames) {
					for (String calc : segmentCalcNames) {
						row.add(fs.getStatistic(stat, calc));
					}
				}
			}
			workbook.addLine(childFrameSheet, row.toArray());
			progressBar.setValue(++progress);
		}

		// ── Sheet 5: Parent Frame Data ────────────────────────────────────────
		progressBar.setString("Retrieving parent frame data");
		progressBar.setMinimum(0);
		progressBar.setMaximum(parentDataSet.getFrameSetList().length);
		progressBar.setValue(0); progress = 0;

		for (int f = 0; f < parentDataSet.getFrameSetList().length; f++) {
			FrameSet fs = parentDataSet.getFrameSet(f);

			List<Object> row = new ArrayList<>();
			row.add(recursiveDataSet.getName());
			row.add(methodName);
			row.add(f + 1);

			if (parentFSCalcNames != null) {
				for (String name : parentFSCalcNames) row.add(fs.getCalculation(name));
			}
			if (parentFSStatNames != null && parentSegCalcNames != null) {
				for (String stat : parentFSStatNames) {
					for (String calc : parentSegCalcNames) {
						row.add(fs.getStatistic(stat, calc));
					}
				}
			}
			workbook.addLine(parentFrameSheet, row.toArray());
			progressBar.setValue(++progress);
		}
	}

	// ── Overlay ───────────────────────────────────────────────────────────────

	/**
	 * Draws parent cell outlines then child void outlines on the full-image overlay.
	 * Parent cells are drawn first (behind) in {@link #getParentColor(LinkSet)}.
	 * Child voids are drawn on top in {@link #getChildColor(LinkSet, LinkSet)}.
	 */
	@Override
	void dataSetToOverlay(Overlay overlay, RoiManager manager) {
		// ── Parent cell outlines (drawn first, behind child voids) ────────────
		progressBar.setString("Generating parent cell overlay");
		for (LinkSet parentLS : parentDataSet.getLinkSetList()) {
			Color pc = getParentColor(parentLS);
			for (Segment seg : parentLS) {
				Roi roi = getParentOverlayRoi(seg);
				if (roi == null) continue;
				roi.setStrokeColor(pc);
				roi.setStrokeWidth(2);
				roi.setPosition(seg.getFrame() + 1);
				overlay.add(roi, "Parent:" + parentLS.getDisplayName());
				manager.add(target, roi, seg.getFrame() + 1);
			}
		}

		// ── Child void outlines (drawn on top of parent outlines) ─────────────
		progressBar.setString("Generating child void overlay");
		for (LinkSet childLS : recursiveDataSet.getLinkSetList()) {
			LinkSet parentLS = recursiveDataSet.getParentLinkSet(childLS);
			Color cc = getChildColor(childLS, parentLS);
			for (Segment seg : childLS) {
				Roi roi = getChildOverlayRoi(seg);
				if (roi == null) continue;
				roi.setStrokeColor(cc);
				roi.setStrokeWidth(1);
				roi.setPosition(seg.getFrame() + 1);
				String parentLabel = (parentLS != null) ? parentLS.getDisplayName() : "?";
				overlay.add(roi, "Void:" + childLS.getDisplayName() + " in " + parentLabel);
				manager.add(target, roi, seg.getFrame() + 1);
			}
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Color model
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Returns the stroke color for all overlay ROIs belonging to a parent cell.
	 * Concrete subclasses choose the color model: fixed palette, stored on
	 * the LinkSet, random assignment, etc.
	 *
	 * @param parent the parent cell LinkSet
	 * @return Color for all segments of this parent cell
	 */
	protected abstract Color getParentColor(LinkSet parent);

	/**
	 * Returns the stroke color for all overlay ROIs belonging to a child void.
	 *
	 * Default: hue-inherited from the parent color — same hue, reduced saturation,
	 * increased brightness — so parent and child are visually related but distinct.
	 *
	 * Override to use a different relationship model (complementary color,
	 * fixed offset, fully independent colors, etc.).
	 *
	 * @param child  the child void LinkSet
	 * @param parent the parent cell that contains this void (may be null)
	 * @return Color for all segments of this child void
	 */
	protected Color getChildColor(LinkSet child, LinkSet parent) {
		if (parent == null) return getColor(); // fallback: random color from AnalysisMethod
		Color pc = getParentColor(parent);
		float[] hsb = Color.RGBtoHSB(pc.getRed(), pc.getGreen(), pc.getBlue(), null);
		// Preserve hue; pull saturation down and brightness up for a lighter, related shade
		float sat = Math.max(0.15f, hsb[1] - 0.35f);
		float bri = Math.min(1.00f, hsb[2] + 0.30f);
		return Color.getHSBColor(hsb[0], sat, bri);
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Abstract calculation tiers — implemented by concrete subclasses
	// ═══════════════════════════════════════════════════════════════════════════

	// ── Child void tiers ──────────────────────────────────────────────────────

	/**
	 * Per-void-segment calculations (area, location, circularity, intensity, …).
	 * Also used as the basis for {@link #childLinkSetStatistics()} and
	 * {@link #childFrameSetStatistics()} column headers.
	 * Results appear as columns in the "Void Segment Data" sheet.
	 *
	 * @return array of SegmentCalculation objects, or null/empty if not needed
	 */
	protected abstract SegmentCalculation[] segmentCalculations();

	/**
	 * Per-void-track calculations aggregated across all frames of a track
	 * (e.g. track length in frames, first/last frame).
	 * Results appear as columns in the "Void Track Data" sheet.
	 *
	 * @return array of LinkSetCalculation objects, or null/empty if not needed
	 */
	protected abstract LinkSetCalculation[] childLinkSetCalculations();

	/**
	 * Statistics aggregating {@link #segmentCalculations()} values across all
	 * frames of each void track (e.g. mean area over track lifetime).
	 * Each statistic × each segment calculation = one column in "Void Track Data".
	 * Column header pattern: {@code StatName_CalcName} (e.g. Mean_Area).
	 *
	 * @return array of LinkSetStatistic objects, or null/empty if not needed
	 */
	protected abstract LinkSetStatistic[] childLinkSetStatistics();

	/**
	 * Frame-level calculations over the child void population at each timepoint.
	 * Results appear as columns in the "Child Frame Data" sheet.
	 *
	 * @return array of FrameSetCalculation objects, or null/empty if not needed
	 */
	protected abstract FrameSetCalculation[] childFrameSetCalculations();

	/**
	 * Statistics aggregating {@link #segmentCalculations()} across void segments
	 * within a single frame (e.g. mean void area per frame).
	 * Each statistic × each segment calculation = one column in "Child Frame Data".
	 *
	 * @return array of FrameSetStatistic objects, or null/empty if not needed
	 */
	protected abstract FrameSetStatistic[] childFrameSetStatistics();

	// ── Parent cell tiers ─────────────────────────────────────────────────────

	/**
	 * Per-parent-cell-segment calculations (e.g. parent cell area, circularity).
	 * These are stored on parent cell Segment objects and used as the basis for
	 * {@link #parentFrameSetStatistics()} column headers.  They are not output
	 * directly as a separate sheet.
	 *
	 * @return array of SegmentCalculation objects, or null/empty if not needed
	 */
	protected abstract SegmentCalculation[] parentSegmentCalculations();

	/**
	 * Aggregate calculations per parent cell using both the parent cell and its
	 * child void context (e.g. void count, total void area, void area fraction).
	 * These use {@link RecursiveLinkSetCalculation} rather than
	 * {@link LinkSetCalculation} because they require child context that a
	 * standard LinkSetCalculation cannot express.
	 * Results appear as columns in the "Parent Cell Aggregation" sheet.
	 *
	 * @return array of RecursiveLinkSetCalculation objects, or null/empty if not needed
	 */
	protected abstract RecursiveLinkSetCalculation[] parentLinkSetCalculations();

	/**
	 * Frame-level calculations over the parent cell population at each timepoint
	 * (e.g. total cell count per frame, population confluency).
	 * Results appear as columns in the "Parent Frame Data" sheet.
	 *
	 * @return array of FrameSetCalculation objects, or null/empty if not needed
	 */
	protected abstract FrameSetCalculation[] parentFrameSetCalculations();

	/**
	 * Statistics aggregating {@link #parentSegmentCalculations()} across parent-cell
	 * segments within a single frame (e.g. mean parent cell area per frame).
	 * Each statistic × each parent segment calculation = one column in "Parent Frame Data".
	 *
	 * @return array of FrameSetStatistic objects, or null/empty if not needed
	 */
	protected abstract FrameSetStatistic[] parentFrameSetStatistics();

	// ── Overlay ROI extraction ────────────────────────────────────────────────

	/**
	 * Extracts the overlay ROI for a single parent cell segment.
	 * Typically returns a PolygonRoi from the segment's external or internal perimeter.
	 *
	 * @param segment a segment from a parent cell LinkSet
	 * @return Roi for overlay display, or null to skip this segment
	 */
	protected abstract Roi getParentOverlayRoi(Segment segment);

	/**
	 * Extracts the overlay ROI for a single child void segment.
	 *
	 * @param segment a segment from a child void LinkSet
	 * @return Roi for overlay display, or null to skip this segment
	 */
	protected abstract Roi getChildOverlayRoi(Segment segment);

	// ═══════════════════════════════════════════════════════════════════════════
	// Private helpers
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Extracts {@link Data#getName()} from every element of a Data array.
	 * Returns an empty array (never null) if the input is null.
	 */
	private String[] names(Data[] calcs) {
		if (calcs == null) return new String[0];
		String[] out = new String[calcs.length];
		for (int i = 0; i < calcs.length; i++) out[i] = calcs[i].getName();
		return out;
	}

	/**
	 * Concatenates a fixed prefix array with one or more additional name arrays
	 * into a single flat String[] suitable for {@link ResultWorkbook#addSheet}.
	 * Null or empty arrays in {@code extra} are skipped.
	 */
	private String[] wideHeaders(String[] prefix, String[]... extra) {
		int total = prefix.length;
		for (String[] arr : extra) if (arr != null) total += arr.length;
		String[] out = new String[total];
		int pos = 0;
		System.arraycopy(prefix, 0, out, pos, prefix.length);
		pos += prefix.length;
		for (String[] arr : extra) {
			if (arr == null || arr.length == 0) continue;
			System.arraycopy(arr, 0, out, pos, arr.length);
			pos += arr.length;
		}
		return out;
	}

	/**
	 * Generates column header strings for every (statistic × calculation) pair.
	 * Pattern: {@code statName + "_" + calcName} (e.g. "Mean_Area").
	 * Returns an empty array if either input is null or empty.
	 */
	private String[] statCalcHeaders(String[] statNames, String[] calcNames) {
		if (statNames == null || statNames.length == 0
				|| calcNames == null || calcNames.length == 0) return new String[0];
		String[] out = new String[statNames.length * calcNames.length];
		int i = 0;
		for (String stat : statNames) {
			for (String calc : calcNames) out[i++] = stat + "_" + calc;
		}
		return out;
	}

	/** Null-safe bulk add from a Data[] into a List. */
	private void collectAll(List<Data> list, Data[] arr) {
		if (arr == null) return;
		for (Data d : arr) list.add(d);
	}
}
