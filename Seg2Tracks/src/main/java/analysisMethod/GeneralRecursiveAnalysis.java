package analysisMethod;

import java.awt.Color;
import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JProgressBar;

import calculations.Area;
import calculations.LinkSetMean;
import calculations.LinkSetStatistic;
import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.RecursiveLinkSetCalculation;
import calculations.SegmentCalculation;
import calculations.TotalVoidArea;
import calculations.VoidCount;
import dataStructure.LinkSet;
import dataStructure.RecursiveDataSet;
import dataStructure.Segment;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * General analysis of recursively-segmented subsegment structures inside parent segments.
 *
 * <h3>Output sheets</h3>
 * <ol>
 *   <li><b>Subsegment Data</b> — area of each subsegment per frame</li>
 *   <li><b>Subsegment Track Data</b> — mean area over each subsegment's lifetime</li>
 *   <li><b>Segment Aggregation</b> — subsegment count and total area per parent segment</li>
 *   <li><b>Subsegment Frame Data</b> — (reserved for future per-frame subsegment calculations)</li>
 *   <li><b>Segment Frame Data</b> — (reserved for future per-frame parent calculations)</li>
 * </ol>
 *
 * <h3>Color model</h3>
 * Parent segments are drawn in colors from a fixed eight-color palette, cycling if
 * there are more segments than palette entries.  Subsegments inherit their parent's
 * hue at reduced saturation and increased brightness (the default behavior defined
 * in {@link RecursiveAnalysisMethod#getChildColor}).
 *
 * <h3>Overlay ROIs</h3>
 * Parent segments are drawn using {@code internalPerimeter} (the final segment boundary).
 * Subsegments are drawn using {@code internalPerimeter} when available (after
 * restricted segmentation) and fall back to {@code externalPerimeter} (SARN
 * envelope) so the overlay is visible at any pipeline stage.
 */
public class GeneralRecursiveAnalysis extends RecursiveAnalysisMethod {

	// ── Color palette ─────────────────────────────────────────────────────────

	/**
	 * Default parent-cell palette — eight visually distinct colors.
	 * Additional parent cells cycle through this list.
	 */
	private static final Color[] PALETTE = {
		new Color( 70, 130, 180), // Steel Blue
		new Color(210, 105,  30), // Chocolate
		new Color( 60, 179, 113), // Medium Sea Green
		new Color(220,  20,  60), // Crimson
		new Color(138,  43, 226), // Blue Violet
		new Color( 32, 178, 170), // Light Sea Green
		new Color(218, 165,  32), // Goldenrod
		new Color(255,  20, 147), // Deep Pink
	};

	/**
	 * Cache of assigned parent segment colors, maintained in insertion order so
	 * palette assignment is deterministic across repeated {@link #getParentColor}
	 * calls for the same session.
	 */
	private final Map<LinkSet, Color> parentColorCache = new LinkedHashMap<>();

	// ── Constructor ───────────────────────────────────────────────────────────

	public GeneralRecursiveAnalysis() {
		methodName = "General Recursive Segmentation Data";
		description = "General and abstracted morphological-intensity data of the recursive segmentation";
	}

	// ── Initialization ────────────────────────────────────────────────────────

	/**
	 * Refuses to run when any subsegment lacks an internal perimeter, because
	 * {@link Area} measures the internal boundary and would otherwise fail with an
	 * NPE deep inside the calculation.
	 * <p>
	 * There is deliberately no fallback to the external (SARN) perimeter: doing so
	 * would silently hide that internal segmentation was skipped. If the SARN
	 * outline is the intended final boundary, the user must say so explicitly by
	 * running "Convert SARN to Segmentation".
	 *
	 * @throws IllegalStateException if any subsegment has no internal perimeter
	 */
	@Override
	public void initialize(ImagePlus target, RecursiveDataSet recursiveDataSet,
	                       JProgressBar progressBar) {
		int total = 0, missing = 0;
		for (LinkSet linkSet : recursiveDataSet.getLinkSetList()) {
			for (Segment s : linkSet) {
				total++;
				if (s.getInternalPerimeter() == null) missing++;
			}
		}
		if (missing > 0) {
			throw new IllegalStateException(missing + " of " + total
				+ " subsegments have no internal segmentation, so their area cannot be measured."
				+ "\n\nRun internal segmentation on the subsegmentation panel first. If the SARN"
				+ " outline is the intended final boundary, use \"Convert SARN to Segmentation\".");
		}
		super.initialize(target, recursiveDataSet, progressBar);
	}

	// ── Color model ───────────────────────────────────────────────────────────

	/**
	 * Returns a palette color for the given parent segment, assigning a new one on
	 * first encounter.  Colors cycle through {@link #PALETTE} in the order
	 * parent segments are first requested.
	 *
	 * @param parent the parent segment LinkSet
	 * @return the Color assigned to this parent segment
	 */
	@Override
	protected Color getParentColor(LinkSet parent) {
		return parentColorCache.computeIfAbsent(parent,
			p -> PALETTE[parentColorCache.size() % PALETTE.length]);
	}

	// ── Overlay ROI extraction ────────────────────────────────────────────────

	/**
	 * Returns the overlay ROI for a parent segment using its
	 * {@code internalPerimeter} (the final segment boundary).
	 * Returns {@code null} if no internal perimeter is available, causing
	 * {@link RecursiveAnalysisMethod#dataSetToOverlay} to skip this segment.
	 *
	 * @param segment a segment from a parent segment LinkSet
	 * @return PolygonRoi from the internal perimeter, or null
	 */
	@Override
	protected Roi getParentOverlayRoi(Segment segment) {
		Point[] perim = segment.getInternalPerimeter();
		if (perim == null || perim.length == 0) return null;
		return toPolygonRoi(perim);
	}

	/**
	 * Returns the overlay ROI for a child subsegment.
	 * Prefers {@code internalPerimeter} (final constricted boundary after restricted
	 * segmentation) when available, and falls back to {@code externalPerimeter}
	 * (SARN envelope) so the overlay is populated at any pipeline stage.
	 * Returns {@code null} if neither perimeter is available.
	 *
	 * @param segment a segment from a child subsegment LinkSet
	 * @return PolygonRoi from the best available perimeter, or null
	 */
	@Override
	protected Roi getChildOverlayRoi(Segment segment) {
		Point[] perim = segment.getInternalPerimeter();
		if (perim == null || perim.length == 0) perim = segment.getExternalPerimeter();
		if (perim == null || perim.length == 0) return null;
		return toPolygonRoi(perim);
	}

	// ── Calculation tiers ─────────────────────────────────────────────────────

	/**
	 * Subsegment calculations: pixel area of each subsegment's final boundary.
	 * Feeds the "Subsegment Data" sheet and provides values for
	 * {@link #childLinkSetStatistics()} (mean area per track).
	 */
	@Override
	protected SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
			new Area(),
		};
	}

	/** No per-track subsegment LinkSet calculations beyond statistics for now. */
	@Override
	protected LinkSetCalculation[] childLinkSetCalculations() {
		return null;
	}

	/**
	 * Mean subsegment area across all frames of each subsegment track.
	 * Produces a "Mean_Area" column in the "Subsegment Track Data" sheet.
	 */
	@Override
	protected LinkSetStatistic[] childLinkSetStatistics() {
		return new LinkSetStatistic[] {
			new LinkSetMean(),
		};
	}

	/** No subsegment frame-level calculations yet. */
	@Override
	protected FrameSetCalculation[] childFrameSetCalculations() {
		return null;
	}

	/** No subsegment frame-level statistics yet. */
	@Override
	protected FrameSetStatistic[] childFrameSetStatistics() {
		return null;
	}

	/** No per-segment parent segment calculations yet. */
	@Override
	protected SegmentCalculation[] parentSegmentCalculations() {
		return null;
	}

	/**
	 * Parent segment aggregation: subsegment count and total subsegment area per parent segment.
	 * Produces two columns in the "Segment Aggregation" sheet.
	 */
	@Override
	protected RecursiveLinkSetCalculation[] parentLinkSetCalculations() {
		return new RecursiveLinkSetCalculation[] {
			new VoidCount(),
			new TotalVoidArea(),
		};
	}

	/** No parent segment frame-level calculations yet. */
	@Override
	protected FrameSetCalculation[] parentFrameSetCalculations() {
		return null;
	}

	/** No parent segment frame-level statistics yet. */
	@Override
	protected FrameSetStatistic[] parentFrameSetStatistics() {
		return null;
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	/** Converts a Point array to a float-coordinate PolygonRoi. */
	private static PolygonRoi toPolygonRoi(Point[] perim) {
		float[] x = new float[perim.length];
		float[] y = new float[perim.length];
		for (int i = 0; i < perim.length; i++) {
			x[i] = perim[i].x;
			y[i] = perim[i].y;
		}
		return new PolygonRoi(x, y, Roi.POLYGON);
	}
}
