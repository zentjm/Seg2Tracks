package sarn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import geometricTools.GeometricCalculations;

/**
 * Multi-Way Reduction Contraction (MRC) — an OWC-based SARN method.
 *
 * <h2>Purpose</h2>
 * An OWC-based SARN method that decomposes the outer-constraint problem into
 * per-neighbour angular sectors rather than using a single global nearest-neighbour
 * radius for the entire cell boundary.  Each angular sector of cell X's envelope
 * is governed by the spatially most-relevant neighbour for that direction, producing
 * boundaries that are locally correct in every direction.
 *
 * <h2>Terminology</h2>
 * <ul>
 *   <li><b>Resolved sector</b> — an angular range [θ_start, θ_end] around cell X's
 *       centre for which the OWC contraction boundary has been finalised.  Once
 *       resolved, a sector is never re-evaluated.</li>
 *   <li><b>Uncommitted arc</b> — the remaining angular range not yet covered by any
 *       resolved sector.  Each recursion step claims a portion of the uncommitted arc.</li>
 * </ul>
 *
 * <h2>Algorithm (per frame)</h2>
 * <ol>
 *   <li><b>Pass 1 — identical to base OWC:</b> run the inherited OWC logic for every
 *       cell in the frame using only centre points and image edges.  This produces the
 *       starting-state SARN envelopes that all subsequent passes read from.</li>
 *
 *   <li><b>Angular pre-pass (per cell X):</b> for each neighbour Y (in ascending
 *       distance order), compute the angular range subtended by Y's SARN perimeter as
 *       seen from X's centre point by calculating atan2(p.y − X.y, p.x − X.x) for
 *       every point in Y's perimeter array and taking the circular min/max.  This is
 *       O(perimeter_length) and gives the exact angular extent with no sampling risk.</li>
 *
 *   <li><b>Bresenham sector scan:</b> using the angular range from the pre-pass as a
 *       guide, cast Bresenham rays from X outward with slight overlap and thickness
 *       (guaranteed to hit even a 1-pixel-wide SARN boundary) <em>within the
 *       uncommitted arc only</em>.  Run the OWC contraction scan for those rays using
 *       Y's distance as the outer constraint.  The resulting boundary points within
 *       Y's angular range become the resolved sector for Y.</li>
 *
 *   <li><b>Recursion:</b> advance to the next nearest neighbour Z and repeat.  If Z's
 *       SARN perimeter does not extend into the uncommitted arc, skip Z and advance.</li>
 *
 *   <li><b>Stopping condition:</b> stop when EITHER ≥90% of the 360° arc is resolved,
 *       OR 8 neighbours have been processed.  Image edges are treated as pseudo-neighbours
 *       and participate in the recursion order by distance, ensuring coverage in sparse
 *       regions.</li>
 *
 *   <li><b>Fallback:</b> any arc still uncommitted after all stopping conditions are
 *       met falls back to standard OWC behaviour (the cell's own pass-1 perimeter) for
 *       that sector.  The worst case degrades gracefully to standard OWC — never to an
 *       undefined boundary.</li>
 *
 *   <li><b>Batch update:</b> all new perimeters for all cells in the frame are
 *       accumulated into a staging structure <em>before</em> any cell's envelope is
 *       replaced.  This prevents mid-pass contamination (cell B's pass must not see
 *       cell A's updated envelope).  Staged envelopes are applied atomically after the
 *       full-frame sweep completes.</li>
 * </ol>
 *
 * <h2>Recursive (subsegmentation) mode</h2>
 * When {@code parentPerimeterMap != null}, pass 1 uses the inherited recursive-mode outer
 * constraint (nearest sibling void / farthest parent-boundary point) exactly as
 * {@link Sarn#run()} does, the cardinal image-edge pseudo-neighbours are skipped (they are
 * not meaningful inside a masked sub-image), and each staged perimeter is clipped/stitched to
 * the parent boundary via {@link Sarn#clipAndStitch} at apply time.
 *
 * <h2>Current state</h2>
 * Implemented: {@link #run()} performs the multi-pass sweep described above, reusing the
 * inherited OWC primitives for pass 1 and the sector helpers below for the refinement pass.
 */
public class MultiWayReductionContraction extends OneWayContractionBase {

	/** Maximum number of neighbours to process per cell before stopping. */
	private static final int MAX_NEIGHBOURS = 8;

	/** Minimum fraction of the 360° arc that must be resolved before stopping early. */
	private static final double RESOLVED_FRACTION_THRESHOLD = 0.90;

	/** Full circle in radians. */
	private static final double TWO_PI = 2 * Math.PI;

	/**
	 * Initialises the Multi-Way Reduction Contraction method.
	 */
	public MultiWayReductionContraction() {
		this.name        = "Multi-Way Reduction Contraction";
		this.description = "Sector-by-sector OWC: each angular region of the boundary "
				+ "is governed by its most spatially relevant neighbour, producing "
				+ "locally correct boundaries in every direction.";
	}

	/**
	 * Distance-ordered neighbour of a cell: a real segment (with its pass-1 perimeter) or a
	 * cardinal image-edge pseudo-neighbour (perimeter {@code null}, using a fixed angular extent).
	 */
	private static class Neighbour {
		final double dist;        // distance from the current cell centre (ordering + outer constraint)
		final Point[] perimeter;  // pass-1 perimeter (real neighbour); null for cardinal pseudo-neighbour
		final double theta;       // cardinal direction angle (used only when perimeter == null)
		Neighbour(double dist, Point[] perimeter, double theta) {
			this.dist = dist; this.perimeter = perimeter; this.theta = theta;
		}
	}

	/**
	 * Multi-pass MRC sweep. For each frame: (1) compute every cell's baseline OWC perimeter,
	 * (2) refine each cell sector-by-sector against its neighbours, (3) apply the staged
	 * perimeters atomically (clipping to the parent boundary in recursive mode).
	 */
	@Override
	public void run() {
		if (progressBar != null) progressBar.setString("Segmentation");

		for (int i = 0; i < inputStack.size(); i++) {
			currentFrame = i;
			// Duplicate so blur does not mutate the stored in-memory stack
			processor = inputStack.getProcessor(i + 1).duplicate();
			blurrer.blurGaussian(processor, blurSigma);
			segments = dataSet.getFrameSet(i);

			int cellCount = segments.size();

			// ── Pass 1: baseline OWC perimeter per cell (in-memory only, not yet applied) ──
			// computeSegmentPerimeter() is Sarn's shared per-segment body (steps 1-4); it does not
			// clip to the parent boundary or call setExternalPerimeter, both deferred to the apply phase.
			Point[][] pass1 = new Point[cellCount][];
			for (int n = 0; n < cellCount; n++) {
				pass1[n] = computeSegmentPerimeter(n);
			}

			// ── Pass 2: MRC sector refinement per cell, reading pass1 for neighbours ──
			Point[][] staged = new Point[cellCount][];
			for (int n = 0; n < cellCount; n++) {
				staged[n] = computeMrcPerimeter(n, pass1);
			}

			// ── Apply staged perimeters atomically for this frame ──
			for (int n = 0; n < cellCount; n++) {
				Point[] finalPerimeter = staged[n];
				// Recursive mode: clip/stitch each cell's perimeter to the parent boundary
				// (per-cell, matching Sarn.run() timing) after the full-frame sweep.
				if (parentPerimeterMap != null) {
					Point[] parentPerim = parentPerimeterMap.get(currentFrame);
					if (parentPerim != null && parentPerim.length > 0) {
						Point voidCenter = segments.get(n).getCenterPoint();
						finalPerimeter = clipAndStitch(finalPerimeter, parentPerim, voidCenter);
					}
				}
				segments.get(n).setExternalPerimeter(finalPerimeter);
			}

			if (progressBar != null) progressBar.setValue(i);
		}
	}

	/**
	 * Refines one cell's boundary sector-by-sector against its distance-ordered neighbours,
	 * filling any still-uncommitted arc from the cell's own pass-1 perimeter.
	 *
	 * @param n     segment index in the current frame
	 * @param pass1 baseline OWC perimeters for every segment in the frame
	 * @return the MRC-refined perimeter for segment {@code n}
	 */
	private Point[] computeMrcPerimeter(int n, Point[][] pass1) {
		Point cellCentre = segments.get(n).getCenterPoint();
		if (cellCentre == null) return pass1[n];

		List<Neighbour> neighbours = buildNeighbours(n, cellCentre, pass1);

		ArrayList<double[]> resolvedSectors = new ArrayList<double[]>(); // non-wrapping [start,end] pairs
		ArrayList<Point> sectorPts = new ArrayList<Point>();

		int processed = 0;
		for (Neighbour y : neighbours) {
			if (processed >= MAX_NEIGHBOURS) break;
			if (resolvedFraction(resolvedSectors) >= RESOLVED_FRACTION_THRESHOLD) break;

			double[] range;
			if (y.perimeter != null) {
				// Real neighbour: skip unless its perimeter actually reaches into the uncommitted arc.
				if (!perimeterIntersectsUncommittedArc(cellCentre, y.perimeter, resolvedSectors)) {
					processed++;
					continue;
				}
				range = angularPrePass(cellCentre, y.perimeter);
				if (range == null) { processed++; continue; }
			} else {
				// Cardinal pseudo-neighbour: fixed ±45° extent about its compass direction,
				// normalised into [0,2π) so a range crossing 0°/360° is handled by splitWrap.
				range = new double[]{ norm(y.theta - Math.PI / 4), norm(y.theta + Math.PI / 4) };
			}

			// Scan only the still-uncommitted sub-intervals of this neighbour's range.
			for (double[] piece : splitWrap(range[0], range[1])) {
				for (double[] free : subtractResolved(piece[0], piece[1], resolvedSectors)) {
					Point[] pts = sectorBresenhamScan(cellCentre, free[0], free[1], y.dist);
					for (Point p : pts) sectorPts.add(p);
					addResolvedSector(resolvedSectors, free[0], free[1]);
				}
			}
			processed++;
		}

		// Fallback: fill any still-uncommitted arc with the cell's own baseline OWC perimeter.
		ArrayList<Point> combined = new ArrayList<Point>(sectorPts);
		if (pass1[n] != null) {
			for (Point p : pass1[n]) {
				double dx = p.x - cellCentre.x, dy = p.y - cellCentre.y;
				if (dx == 0 && dy == 0) continue;
				if (!isAngleResolved(angleOf(cellCentre, p), resolvedSectors)) combined.add(p);
			}
		}

		if (combined.isEmpty()) return pass1[n];

		// Sectors are appended in neighbour-visit order, not angular order — sort by angle before
		// refinement, or straightPerimeter would connect geometrically non-adjacent points.
		final Point centre = cellCentre;
		Collections.sort(combined, new Comparator<Point>() {
			@Override public int compare(Point a, Point b) {
				return Double.compare(angleOf(centre, a), angleOf(centre, b));
			}
		});

		Point[] raw = combined.toArray(new Point[0]);
		Point[] densified = GeometricCalculations.straightPerimeter(raw);
		int searchDistance = GeometricCalculations.scaledSearchDistance(densified.length, searchFraction, searchCeiling);
		return GeometricCalculations.straightPerimeter(
				GeometricCalculations.douglasPeucker(
				GeometricCalculations.removeLoops(
				densified, searchDistance,
				GeometricCalculations.LOOP_REMOVAL_RANGE,
				GeometricCalculations.LOOP_REMOVAL_SMOOTHING),
				epsilon));
	}

	/**
	 * Builds the distance-ordered neighbour list for a cell: every other segment centre (as a real
	 * neighbour carrying its pass-1 perimeter), plus — in non-recursive mode only — the four cardinal
	 * image-edge points as pseudo-neighbours.
	 */
	private List<Neighbour> buildNeighbours(int n, Point cellCentre, Point[][] pass1) {
		List<Neighbour> neighbours = new ArrayList<Neighbour>();

		for (int j = 0; j < segments.size(); j++) {
			if (j == n) continue;
			Point c = segments.get(j).getCenterPoint();
			if (c == null) continue;
			// A real neighbour must carry a usable perimeter; a null/empty one has no angular
			// range and must not be added (it would be misread as a cardinal at theta=NaN).
			if (pass1[j] == null || pass1[j].length == 0) continue;
			double dx = c.x - cellCentre.x, dy = c.y - cellCentre.y;
			double d = Math.sqrt(dx * dx + dy * dy);
			if (d == 0) continue;
			neighbours.add(new Neighbour(d, pass1[j], Double.NaN));
		}

		// Cardinal image-edge pseudo-neighbours are meaningful only in non-recursive mode; in
		// recursive mode the masked sub-image edges are not real boundaries, so rely on the
		// parent-boundary fallback instead (matching how Sarn.run() special-cases recursive outer constraints).
		if (parentPerimeterMap == null) {
			Point[] cardinals = {
					new Point(cellCentre.x, 0),
					new Point(cellCentre.x, processor.getHeight() - 1),
					new Point(0, cellCentre.y),
					new Point(processor.getWidth() - 1, cellCentre.y)
			};
			for (Point cp : cardinals) {
				double d = Math.sqrt(Math.pow(cp.x - cellCentre.x, 2) + Math.pow(cp.y - cellCentre.y, 2));
				if (d == 0) continue;
				neighbours.add(new Neighbour(d, null, angleOf(cellCentre, cp)));
			}
		}

		Collections.sort(neighbours, new Comparator<Neighbour>() {
			@Override public int compare(Neighbour a, Neighbour b) {
				return Double.compare(a.dist, b.dist);
			}
		});
		return neighbours;
	}

	// ── Sector helpers ───────────────────────────────────────────────────────

	/**
	 * Computes the angular range [θ_min, θ_max] subtended by {@code neighbourPerimeter} as seen from
	 * {@code cellCentre}, using atan2 and the largest-circular-gap technique to handle 0°/360°
	 * wraparound.
	 *
	 * @return {@code {θ_min, θ_max}} in radians (θ_min may exceed θ_max, meaning the range wraps
	 *         through 0); {@code null} if the perimeter is null or empty
	 */
	private double[] angularPrePass(Point cellCentre, Point[] neighbourPerimeter) {
		if (neighbourPerimeter == null || neighbourPerimeter.length == 0) return null;

		ArrayList<Double> angleList = new ArrayList<Double>();
		for (int i = 0; i < neighbourPerimeter.length; i++) {
			double dx = neighbourPerimeter[i].x - cellCentre.x;
			double dy = neighbourPerimeter[i].y - cellCentre.y;
			if (dx == 0 && dy == 0) continue; // degenerate — skip
			double a = Math.atan2(dy, dx);
			if (a < 0) a += TWO_PI;
			angleList.add(a);
		}
		if (angleList.isEmpty()) return null;

		double[] angles = new double[angleList.size()];
		for (int i = 0; i < angles.length; i++) angles[i] = angleList.get(i);
		java.util.Arrays.sort(angles);

		// Largest gap between consecutive sorted angles (wrapping last→first+2π). The subtended
		// range is the complement of that gap. If the neighbour straddles 0°, the largest gap is
		// elsewhere, so θ_min comes out > θ_max (signalling "wraps through 0").
		int n = angles.length;
		double maxGap = -1;
		int gapStartIdx = 0;
		for (int i = 0; i < n; i++) {
			double next = (i == n - 1) ? angles[0] + TWO_PI : angles[i + 1];
			double gap = next - angles[i];
			if (gap > maxGap) { maxGap = gap; gapStartIdx = i; }
		}
		double thetaMin = angles[(gapStartIdx + 1) % n];
		double thetaMax = angles[gapStartIdx];
		return new double[]{ thetaMin, thetaMax };
	}

	/**
	 * Casts Bresenham rays from {@code cellCentre} outward across [θstart, θend] (non-wrapping),
	 * out to {@code outerConstraintDist}, and returns the darkest-pixel restriction point of each ray.
	 * The angular step is ≈ 1/outerConstraintDist rad so adjacent rays are ≤~1px apart at the outer
	 * radius (the "overlap/thickness" guarantee for hitting a 1-pixel boundary).
	 */
	private Point[] sectorBresenhamScan(Point cellCentre, double thetaStart, double thetaEnd,
	                                     double outerConstraintDist) {
		ArrayList<Point> resultPts = new ArrayList<Point>();
		double step = (outerConstraintDist >= 1.0) ? 1.0 / outerConstraintDist : 1.0;
		// Epsilon so the trailing ray at thetaEnd is not skipped by float accumulation
		// (the sub-interval is marked fully resolved afterwards, so a skipped edge ray
		// would leave an unsampled notch at the sector seam).
		for (double theta = thetaStart; theta <= thetaEnd + 1e-9; theta += step) {
			int outerX = cellCentre.x + (int) Math.round(outerConstraintDist * Math.cos(theta));
			int outerY = cellCentre.y + (int) Math.round(outerConstraintDist * Math.sin(theta));
			Point[] ray = bresenham(cellCentre, new Point(outerX, outerY));
			// Keep only the in-bounds prefix — oblique rays can exit the image before the outer point.
			int valid = 0;
			while (valid < ray.length && inBounds(ray[valid])) valid++;
			if (valid == 0) continue;
			Point[] clipped = (valid == ray.length) ? ray : java.util.Arrays.copyOfRange(ray, 0, valid);
			resultPts.add(getThreasholdPoint(clipped));
		}
		return resultPts.toArray(new Point[0]);
	}

	/**
	 * Tests whether any point of {@code candidatePerimeter} falls in the uncommitted arc (outside all
	 * resolved sectors). Checks the whole perimeter rather than only the centre-angle, so an elongated
	 * neighbour whose centre lies behind a resolved sector but whose perimeter protrudes into
	 * uncommitted territory still contributes.
	 */
	private boolean perimeterIntersectsUncommittedArc(Point cellCentre, Point[] candidatePerimeter,
	                                                    ArrayList<double[]> resolvedSectors) {
		if (candidatePerimeter == null) return false;
		for (int i = 0; i < candidatePerimeter.length; i++) {
			double dx = candidatePerimeter[i].x - cellCentre.x;
			double dy = candidatePerimeter[i].y - cellCentre.y;
			if (dx == 0 && dy == 0) continue;
			double a = Math.atan2(dy, dx);
			if (a < 0) a += TWO_PI;
			if (!isAngleResolved(a, resolvedSectors)) return true;
		}
		return false;
	}

	/**
	 * Returns the fraction of the full 360° arc covered by {@code resolvedSectors}, merging
	 * overlapping ranges first so overlap is not double-counted.
	 */
	private double resolvedFraction(ArrayList<double[]> resolvedSectors) {
		if (resolvedSectors.isEmpty()) return 0.0;
		List<double[]> sorted = new ArrayList<double[]>(resolvedSectors);
		Collections.sort(sorted, new Comparator<double[]>() {
			@Override public int compare(double[] a, double[] b) { return Double.compare(a[0], b[0]); }
		});
		double covered = 0.0;
		double curStart = sorted.get(0)[0], curEnd = sorted.get(0)[1];
		for (int i = 1; i < sorted.size(); i++) {
			double[] r = sorted.get(i);
			if (r[0] <= curEnd) {
				curEnd = Math.max(curEnd, r[1]);
			} else {
				covered += curEnd - curStart;
				curStart = r[0]; curEnd = r[1];
			}
		}
		covered += curEnd - curStart;
		return covered / TWO_PI;
	}

	// ── Small angular utilities ──────────────────────────────────────────────

	/** Angle of {@code p} about {@code centre}, normalised to [0, 2π). */
	private static double angleOf(Point centre, Point p) {
		double a = Math.atan2(p.y - centre.y, p.x - centre.x);
		if (a < 0) a += TWO_PI;
		return a;
	}

	/** Normalises an arbitrary angle into [0, 2π). */
	private static double norm(double a) {
		a = a % TWO_PI;
		if (a < 0) a += TWO_PI;
		return a;
	}

	/** True if {@code p} lies within the current frame processor's bounds. */
	private boolean inBounds(Point p) {
		return p.x >= 0 && p.x < processor.getWidth() && p.y >= 0 && p.y < processor.getHeight();
	}

	/** True if {@code angle} falls in any (non-wrapping) resolved sector. */
	private boolean isAngleResolved(double angle, ArrayList<double[]> resolvedSectors) {
		for (int i = 0; i < resolvedSectors.size(); i++) {
			double[] r = resolvedSectors.get(i);
			if (angle >= r[0] && angle <= r[1]) return true;
		}
		return false;
	}

	/**
	 * Records [start, end] as resolved, splitting a range that wraps through 0 (start &gt; end) into
	 * two non-wrapping pairs so all downstream comparisons stay simple.
	 */
	private void addResolvedSector(ArrayList<double[]> resolvedSectors, double start, double end) {
		if (start <= end) {
			resolvedSectors.add(new double[]{ start, end });
		} else {
			resolvedSectors.add(new double[]{ start, TWO_PI });
			resolvedSectors.add(new double[]{ 0, end });
		}
	}

	/** Splits a possibly-wrapping [start, end] into 1 or 2 non-wrapping [start, end] pieces. */
	private List<double[]> splitWrap(double start, double end) {
		List<double[]> pieces = new ArrayList<double[]>();
		if (start <= end) {
			pieces.add(new double[]{ start, end });
		} else {
			pieces.add(new double[]{ start, TWO_PI });
			pieces.add(new double[]{ 0, end });
		}
		return pieces;
	}

	/**
	 * Returns the sub-intervals of the non-wrapping [start, end] that are not covered by any
	 * (non-wrapping) resolved sector — the still-uncommitted portions to actually scan.
	 */
	private List<double[]> subtractResolved(double start, double end, ArrayList<double[]> resolved) {
		List<double[]> free = new ArrayList<double[]>();
		free.add(new double[]{ start, end });
		for (int k = 0; k < resolved.size(); k++) {
			double[] r = resolved.get(k);
			List<double[]> next = new ArrayList<double[]>();
			for (int f = 0; f < free.size(); f++) {
				double[] iv = free.get(f);
				if (r[1] <= iv[0] || r[0] >= iv[1]) { next.add(iv); continue; } // no overlap
				if (r[0] > iv[0]) next.add(new double[]{ iv[0], Math.min(r[0], iv[1]) });
				if (r[1] < iv[1]) next.add(new double[]{ Math.max(r[1], iv[0]), iv[1] });
			}
			free = next;
		}
		return free;
	}
}
