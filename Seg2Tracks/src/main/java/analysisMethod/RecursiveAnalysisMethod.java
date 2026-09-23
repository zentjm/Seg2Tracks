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
 * produced by recursive segmentation (subsegments inside parent segments).
 *
 * <h3>Calculation tiers</h3>
 * <pre>
 *   CHILD SUBSEGMENT TIERS
 *     segmentCalculations()          — per subsegment (area, circularity, …)
 *     childLinkSetCalculations()     — per subsegment track aggregated across frames
 *     childLinkSetStatistics()       — statistics over subsegments per track
 *     childFrameSetCalculations()    — subsegment population per timepoint
 *     childFrameSetStatistics()      — statistics over subsegments per frame
 *
 *   PARENT SEGMENT TIERS
 *     parentSegmentCalculations()    — per parent segment (for frame-level stats)
 *     parentLinkSetCalculations()    — aggregate per parent segment with child context
 *     parentFrameSetCalculations()   — parent segment population per timepoint
 *     parentFrameSetStatistics()     — statistics over parent segments per frame
 * </pre>
 *
 * <h3>Output format</h3>
 * Wide format (one row per entity, one column per calculation/statistic),
 * optimised for Excel pivot chart integration.  Five sheets are produced:
 * <ol>
 *   <li>Subsegment Data         — one row per (subsegment × frame)
 *   <li>Subsegment Track Data   — one row per subsegment track
 *   <li>Segment Aggregation     — one row per parent segment
 *   <li>Subsegment Frame Data   — one row per timepoint (subsegment population)
 *   <li>Segment Frame Data      — one row per timepoint (parent segment population)
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
 * deeper hierarchies.  Cross-parent analysis requiring simultaneous access to the
 * full parent population is handled by the separate
 * {@link CrossRecursiveAnalysisMethod} stub.
 *
 * <h3>TODO</h3>
 * Recursive analysis panels should not be independently nameable in the UI.
 * Their output should be integrated into the parent panel's DataSet slot rather
 * than occupying an independent named slot.  Tracked for a future UI/model refactor.
 */
public abstract class RecursiveAnalysisMethod extends AnalysisMethod {

	// ── Data sources ──────────────────────────────────────────────────────────

	/** The recursively-generated child DataSet (subsegments inside parent segments). */
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
	 * {@link #retrieveCalculations()}.  LinkedHashMap preserves parent segment
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
	 * @param recursiveDataSet the RecursiveDataSet containing all child subsegment LinkSets
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
	 * @param recursiveDataSet the RecursiveDataSet containing all child subsegment LinkSets
	 * @param progressBar      UI progress indicator
	 * @param workbook         existing ResultWorkbook to append sheets to
	 */
	public void initialize(ImagePlus target, RecursiveDataSet recursiveDataSet,
	                       JProgressBar progressBar, ResultWorkbook workbook) {
		initialize(target, recursiveDataSet, progressBar);
		this.workbook = workbook;
	}

	// ── Bridge overrides for the standard AnalysisMethod interface ────────────
	// AnalysisModel.runIt() calls initialize(ImagePlus, DataSet[], JProgressBar).
	// These overrides extract dataSets[0] as a RecursiveDataSet and delegate.

	/**
	 * Bridge override: called by {@link gui.AnalysisModel} when a recursive
	 * analysis method is loaded via {@code seg2tracks.config}.
	 * Extracts the first element of {@code dataSets} as a {@link RecursiveDataSet}
	 * and delegates to the typed {@link #initialize(ImagePlus, RecursiveDataSet, JProgressBar)}.
	 *
	 * @throws IllegalArgumentException if {@code dataSets} is null/empty or if
	 *                                  {@code dataSets[0]} is not a RecursiveDataSet
	 */
	@Override
	public void initialize(ImagePlus target, DataSet[] dataSets, JProgressBar progressBar) {
		if (dataSets == null || dataSets.length == 0 || !(dataSets[0] instanceof RecursiveDataSet)) {
			throw new IllegalArgumentException(
				getClass().getSimpleName() + " requires a RecursiveDataSet as its first input channel.");
		}
		initialize(target, (RecursiveDataSet) dataSets[0], progressBar);
	}

	/**
	 * Bridge override: workbook-append variant.
	 * Extracts {@code dataSets[0]} as a {@link RecursiveDataSet} and delegates to
	 * {@link #initialize(ImagePlus, RecursiveDataSet, JProgressBar, ResultWorkbook)}.
	 *
	 * @throws IllegalArgumentException if {@code dataSets[0]} is not a RecursiveDataSet
	 */
	@Override
	public void initialize(ImagePlus target, DataSet[] dataSets, JProgressBar progressBar,
	                       ResultWorkbook workbook) {
		if (dataSets == null || dataSets.length == 0 || !(dataSets[0] instanceof RecursiveDataSet)) {
			throw new IllegalArgumentException(
				getClass().getSimpleName() + " requires a RecursiveDataSet as its first input channel.");
		}
		initialize(target, (RecursiveDataSet) dataSets[0], progressBar, workbook);
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
	 * Creates five wide-format sheets in the workbook:
	 * Subsegment Data, Subsegment Track Data, Segment Aggregation,
	 * Subsegment Frame Data, and Segment Frame Data.
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

		// 1. Subsegment Data — one row per (subsegment track × frame)
		voidSegmentSheet = workbook.addSheet("Subsegment Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Segment", "Subsegment", "Frame"},
				segmentCalcNames));

		// 2. Subsegment Track Data — one row per subsegment track
		//    Stat columns: StatName_CalcName (e.g. Mean_Area)
		childVoidTrackSheet = workbook.addSheet("Subsegment Track Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Segment", "Subsegment"},
				childLSCalcNames,
				statCalcHeaders(childLSStatNames, segmentCalcNames)));

		// 3. Segment Aggregation — one row per parent segment
		parentCellSheet = workbook.addSheet("Segment Aggregation",
			wideHeaders(
				new String[]{"DataSet", "Method", "Segment"},
				parentLSCalcNames));

		// 4. Subsegment Frame Data — one row per timepoint (subsegment population)
		childFrameSheet = workbook.addSheet("Subsegment Frame Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Frame"},
				childFSCalcNames,
				statCalcHeaders(childFSStatNames, segmentCalcNames)));

		// 5. Segment Frame Data — one row per timepoint (parent segment population)
		parentFrameSheet = workbook.addSheet("Segment Frame Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Frame"},
				parentFSCalcNames,
				statCalcHeaders(parentFSStatNames, parentSegCalcNames)));
	}

	/**
	 * Configures all calculations on their respective data objects.
	 *
	 * Child subsegment and LinkSet calculations are stored directly on the
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

		// ── Child subsegment and LinkSet tiers ───────────────────────────────
		progressBar.setString("Setting subsegment calculations");
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
						c.setSegment(seg);
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
		// Fresh instances per segment; stored on the parent segments' Segments so
		// parentFrameSetStatistics() can aggregate over them via the FrameSet.
		for (LinkSet parentLS : parentDataSet.getLinkSetList()) {
			for (Segment seg : parentLS) {
				SegmentCalculation[] calcs = parentSegmentCalculations();
				if (calcs != null) {
					for (SegmentCalculation c : calcs) {
						c.setTargetStackSlice(stack, seg.getFrame() + 1);
						c.setSegment(seg);
						seg.setCalculation(c);
					}
				}
			}
		}

		// ── Parent LinkSet aggregation (RecursiveLinkSetCalculation) ──────────
		// Fresh instances per parent segment; stored in parentCalcMap.
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

		// ── Sheet 1: Subsegment Data ──────────────────────────────────────────
		progressBar.setString("Retrieving subsegment data");
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

		// ── Sheet 2: Subsegment Track Data ───────────────────────────────────
		progressBar.setString("Retrieving subsegment track data");
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

		// ── Sheet 3: Segment Aggregation ─────────────────────────────────────
		progressBar.setString("Retrieving segment aggregation data");
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

		// ── Sheet 4: Subsegment Frame Data ───────────────────────────────────
		progressBar.setString("Retrieving subsegment frame data");
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

		// ── Sheet 5: Segment Frame Data ───────────────────────────────────────
		progressBar.setString("Retrieving segment frame data");
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
	 * Draws parent segment outlines then subsegment outlines on the full-image overlay.
	 * Parent segments are drawn first (behind) in {@link #getParentColor(LinkSet)}.
	 * Subsegments are drawn on top in {@link #getChildColor(LinkSet, LinkSet)}.
	 */
	@Override
	void dataSetToOverlay(Overlay overlay, RoiManager manager) {
		// ── Parent segment outlines (drawn first, behind subsegments) ────────
		progressBar.setString("Generating segment overlay");
		for (LinkSet parentLS : parentDataSet.getLinkSetList()) {
			Color pc = getParentColor(parentLS);
			for (Segment seg : parentLS) {
				Roi roi = getParentOverlayRoi(seg);
				if (roi == null) continue;
				roi.setStrokeColor(pc);
				roi.setStrokeWidth(2);
				roi.setPosition(seg.getFrame() + 1);
				overlay.add(roi, "Segment:" + parentLS.getDisplayName());
				manager.add(target, roi, seg.getFrame() + 1);
			}
		}

		// ── Subsegment outlines (drawn on top of parent outlines) ─────────────
		progressBar.setString("Generating subsegment overlay");
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
				overlay.add(roi, "Subsegment:" + childLS.getDisplayName() + " in " + parentLabel);
				manager.add(target, roi, seg.getFrame() + 1);
			}
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// Color model
	// ═══════════════════════════════════════════════════════════════════════════

	/**
	 * Returns the stroke color for all overlay ROIs belonging to a parent segment.
	 * Concrete subclasses choose the color model: fixed palette, stored on
	 * the LinkSet, random assignment, etc.
	 *
	 * @param parent the parent segment LinkSet
	 * @return Color for all segments of this parent segment
	 */
	protected abstract Color getParentColor(LinkSet parent);

	/**
	 * Returns the stroke color for all overlay ROIs belonging to a child subsegment.
	 *
	 * Default: hue-inherited from the parent color — same hue, reduced saturation,
	 * increased brightness — so parent and child are visually related but distinct.
	 *
	 * Override to use a different relationship model (complementary color,
	 * fixed offset, fully independent colors, etc.).
	 *
	 * @param child  the child subsegment LinkSet
	 * @param parent the parent segment that contains this subsegment (may be null)
	 * @return Color for all segments of this child subsegment
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

	// ── Child subsegment tiers ────────────────────────────────────────────────

	/**
	 * Per-subsegment calculations (area, location, circularity, intensity, …).
	 * Also used as the basis for {@link #childLinkSetStatistics()} and
	 * {@link #childFrameSetStatistics()} column headers.
	 * Results appear as columns in the "Subsegment Data" sheet.
	 *
	 * @return array of SegmentCalculation objects, or null/empty if not needed
	 */
	protected abstract SegmentCalculation[] segmentCalculations();

	/**
	 * Per-subsegment-track calculations aggregated across all frames of a track
	 * (e.g. track length in frames, first/last frame).
	 * Results appear as columns in the "Subsegment Track Data" sheet.
	 *
	 * @return array of LinkSetCalculation objects, or null/empty if not needed
	 */
	protected abstract LinkSetCalculation[] childLinkSetCalculations();

	/**
	 * Statistics aggregating {@link #segmentCalculations()} values across all
	 * frames of each subsegment track (e.g. mean area over track lifetime).
	 * Each statistic × each segment calculation = one column in "Subsegment Track Data".
	 * Column header pattern: {@code StatName_CalcName} (e.g. Mean_Area).
	 *
	 * @return array of LinkSetStatistic objects, or null/empty if not needed
	 */
	protected abstract LinkSetStatistic[] childLinkSetStatistics();

	/**
	 * Frame-level calculations over the subsegment population at each timepoint.
	 * Results appear as columns in the "Subsegment Frame Data" sheet.
	 *
	 * @return array of FrameSetCalculation objects, or null/empty if not needed
	 */
	protected abstract FrameSetCalculation[] childFrameSetCalculations();

	/**
	 * Statistics aggregating {@link #segmentCalculations()} across subsegments
	 * within a single frame (e.g. mean subsegment area per frame).
	 * Each statistic × each segment calculation = one column in "Subsegment Frame Data".
	 *
	 * @return array of FrameSetStatistic objects, or null/empty if not needed
	 */
	protected abstract FrameSetStatistic[] childFrameSetStatistics();

	// ── Parent segment tiers ──────────────────────────────────────────────────

	/**
	 * Per-parent-segment calculations (e.g. parent segment area, circularity).
	 * These are stored on parent segment Segment objects and used as the basis for
	 * {@link #parentFrameSetStatistics()} column headers.  They are not output
	 * directly as a separate sheet.
	 *
	 * @return array of SegmentCalculation objects, or null/empty if not needed
	 */
	protected abstract SegmentCalculation[] parentSegmentCalculations();

	/**
	 * Aggregate calculations per parent segment using both the parent segment and its
	 * child subsegment context (e.g. subsegment count, total subsegment area, area fraction).
	 * These use {@link RecursiveLinkSetCalculation} rather than
	 * {@link LinkSetCalculation} because they require child context that a
	 * standard LinkSetCalculation cannot express.
	 * Results appear as columns in the "Segment Aggregation" sheet.
	 *
	 * @return array of RecursiveLinkSetCalculation objects, or null/empty if not needed
	 */
	protected abstract RecursiveLinkSetCalculation[] parentLinkSetCalculations();

	/**
	 * Frame-level calculations over the parent segment population at each timepoint
	 * (e.g. total segment count per frame, population confluency).
	 * Results appear as columns in the "Segment Frame Data" sheet.
	 *
	 * @return array of FrameSetCalculation objects, or null/empty if not needed
	 */
	protected abstract FrameSetCalculation[] parentFrameSetCalculations();

	/**
	 * Statistics aggregating {@link #parentSegmentCalculations()} across parent
	 * segments within a single frame (e.g. mean parent segment area per frame).
	 * Each statistic × each parent segment calculation = one column in "Segment Frame Data".
	 *
	 * @return array of FrameSetStatistic objects, or null/empty if not needed
	 */
	protected abstract FrameSetStatistic[] parentFrameSetStatistics();

	// ── Overlay ROI extraction ────────────────────────────────────────────────

	/**
	 * Extracts the overlay ROI for a single parent segment.
	 * Typically returns a PolygonRoi from the segment's external or internal perimeter.
	 *
	 * @param segment a segment from a parent segment LinkSet
	 * @return Roi for overlay display, or null to skip this segment
	 */
	protected abstract Roi getParentOverlayRoi(Segment segment);

	/**
	 * Extracts the overlay ROI for a single child subsegment.
	 *
	 * @param segment a segment from a child subsegment LinkSet
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
