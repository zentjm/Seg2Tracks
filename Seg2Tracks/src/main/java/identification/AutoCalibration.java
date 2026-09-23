package identification;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedMaximumFinder;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 * Utility class for automatically estimating Object Identification parameters
 * (Gaussian blur sigma and intensity threshold) either from the image alone or
 * from an existing segmentation {@link DataSet} used as ground truth.
 *
 * <h3>Available methods</h3>
 * <dl>
 *   <dt>{@link #estimateSigmaLoG(ImageStack)}</dt>
 *   <dd>Laplacian-of-Gaussian (LoG) scale-space analysis on a representative
 *       frame.  No prior segmentation required.  Returns a sigma estimate based
 *       on the dominant blob scale present in the image.</dd>
 *
 *   <dt>{@link #calibrateFromDataSet(DataSet, ImageStack, boolean)}</dt>
 *   <dd>Supervised calibration using an existing segmentation as ground truth.
 *       Runs three sequential sub-methods:
 *       <ol>
 *         <li><b>Object size → sigma</b>: mean equivalent-circle radius from
 *             each segment's external perimeter bounding box, mapped to
 *             σ = r / √2 (LoG-optimal for Gaussian blobs).</li>
 *         <li><b>Inter-object spacing → sigma ceiling</b>: minimum
 *             nearest-neighbour distance between known centers, capping σ at
 *             d_min / 4 to prevent adjacent objects from merging.</li>
 *         <li><b>Center-point intensity → threshold floor</b>: blurs each
 *             frame at the chosen sigma, computes the kernel-averaged intensity
 *             at each known center (matching {@link Identification#filterLowPoints}),
 *             and sets threshold = (minimum effective threshold − margin) so
 *             that every ground-truth object is guaranteed to be detected.</li>
 *       </ol>
 *       Returns {@code double[]{sigma, threshold}} where threshold is a fraction
 *       [0–1].  Returns {@code null} if the DataSet contains no usable perimeter
 *       data.
 *   </dd>
 * </dl>
 *
 * <p>All methods are static and safe to call from a background thread.
 */
public class AutoCalibration {

	/**
	 * Kernel half-size used by {@link Identification#filterLowPoints}.
	 * Must stay in sync with {@link Identification#kernelSize}.
	 */
	private static final int KERNEL_SIZE = 3;

	/**
	 * Safety margin (as a fraction of the intensity range) subtracted from
	 * the minimum center-point effective threshold in
	 * {@link #calibrateFromDataSet}.  Ensures the recommended threshold is
	 * slightly below the dimmest known object so that objects fractionally
	 * dimmer in subsequent stacks are still detected.
	 */
	private static final double THRESHOLD_MARGIN = 0.02;

	/**
	 * Sigma values (in pixels) sampled for LoG scale-space analysis.
	 * Approximately log-spaced from 2 to 50 px; covers the typical range of
	 * object radii expected in fluorescence microscopy at common magnifications.
	 */
	private static final double[] LOG_SIGMAS = {
		2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 18, 22, 26, 30, 35, 40, 50
	};

	/**
	 * Maximum number of candidate blob locations used in
	 * {@link #estimateSigmaLoG}.  Caps memory and computation for images
	 * with many local maxima.
	 */
	private static final int MAX_LOG_CANDIDATES = 200;

	private AutoCalibration() {} // non-instantiable utility class

	// ── LoG scale-space sigma estimation ─────────────────────────────────────

	/**
	 * Estimates the recommended Gaussian blur sigma using Laplacian-of-Gaussian
	 * (LoG) scale-space analysis on the middle frame of the input stack.
	 *
	 * <h3>Algorithm</h3>
	 * <ol>
	 *   <li>Detect up to {@value #MAX_LOG_CANDIDATES} candidate blob locations at
	 *       an initial scale (σ = 10 px) using {@link ModifiedMaximumFinder}.</li>
	 *   <li>For each sigma in {@link #LOG_SIGMAS}: Gaussian-blur the frame and
	 *       compute the normalized LoG response
	 *       L(x,y,σ) = σ² × ∇²(G_σ∗I)(x,y) at each candidate location using
	 *       the 4-connected discrete Laplacian.</li>
	 *   <li>For each candidate, record the sigma that maximises |L|
	 *       (the scale at which the blob is sharpest).</li>
	 *   <li>Return the response-weighted mean of those per-candidate sigmas,
	 *       divided by √2 to convert from LoG-optimal σ to the blur σ used in
	 *       the identification pipeline.</li>
	 * </ol>
	 *
	 * @param stack the input image stack (not mutated)
	 * @return recommended Gaussian blur sigma in pixels; 10.0 as a safe fallback
	 *         if no blob structure is detected
	 */
	public static double estimateSigmaLoG(ImageStack stack) {
		int midFrame = Math.max(1, (stack.getSize() + 1) / 2);
		ImageProcessor frame = stack.getProcessor(midFrame).duplicate();

		// Step 1: find candidate blob locations at a moderate initial sigma
		ImageProcessor initBlurred = frame.duplicate();
		new GaussianBlur().blurGaussian(initBlurred, 10.0);
		java.awt.Polygon candidates = new ModifiedMaximumFinder().getMaxima(initBlurred, 0, true);
		int nCandidates = Math.min(candidates.npoints, MAX_LOG_CANDIDATES);
		if (nCandidates == 0) return 10.0; // no structure found

		// Step 2: pre-compute blurred images and LoG responses at each scale.
		// logAtScale[si][pi] = normalized LoG at sigma LOG_SIGMAS[si], candidate pi.
		// Processing sigma-by-sigma (one blur per sigma) is far cheaper than
		// re-blurring the image once per (sigma, candidate) pair.
		float[][] logAtScale = new float[LOG_SIGMAS.length][nCandidates];
		for (int si = 0; si < LOG_SIGMAS.length; si++) {
			ImageProcessor blurred = frame.duplicate();
			new GaussianBlur().blurGaussian(blurred, LOG_SIGMAS[si]);
			for (int pi = 0; pi < nCandidates; pi++) {
				logAtScale[si][pi] = normalizedLoGAt(
						blurred, LOG_SIGMAS[si],
						candidates.xpoints[pi], candidates.ypoints[pi]);
			}
		}

		// Step 3: for each candidate, find the sigma that maximises |LoG|
		double weightedSigmaSum = 0;
		double totalWeight = 0;
		for (int pi = 0; pi < nCandidates; pi++) {
			float bestResponse = 0;
			double bestSigma  = LOG_SIGMAS[LOG_SIGMAS.length / 2]; // default mid-scale
			for (int si = 0; si < LOG_SIGMAS.length; si++) {
				float r = Math.abs(logAtScale[si][pi]);
				if (r > bestResponse) { bestResponse = r; bestSigma = LOG_SIGMAS[si]; }
			}
			if (bestResponse > 0) {
				weightedSigmaSum += bestSigma * bestResponse;
				totalWeight      += bestResponse;
			}
		}

		if (totalWeight == 0) return 10.0;
		double sigmaLoG = weightedSigmaSum / totalWeight;
		// Convert LoG-optimal sigma to blur sigma: σ_blur ≈ σ_LoG / √2
		return sigmaLoG / Math.sqrt(2);
	}

	// ── DataSet-supervised calibration ───────────────────────────────────────

	/**
	 * Estimates sigma and threshold from an existing segmentation DataSet used
	 * as ground truth, running three sequential methods described in the class
	 * Javadoc.
	 *
	 * @param ds              DataSet with external (SARN envelope) perimeters and
	 *                        center points already populated
	 * @param stack           image stack corresponding to the DataSet (not mutated)
	 * @param invertIntensity {@code true} if the stack is processed with intensity
	 *                        inversion (must match the controller setting so the
	 *                        threshold is computed on the same intensities the
	 *                        pipeline will see)
	 * @return {@code double[]{sigma, threshold}} where {@code threshold} is a
	 *         fraction in [0,1], or {@code null} if no usable perimeter data is
	 *         found in the DataSet
	 */
	public static double[] calibrateFromDataSet(DataSet ds,
	                                             ImageStack stack,
	                                             boolean invertIntensity) {
		FrameSet[] frameSets = ds.getFrameSetList();
		if (frameSets == null) return null;

		// ── Method 1: Object size → sigma ────────────────────────────────────
		// Estimate each segment's equivalent-circle radius from its perimeter
		// bounding box (fast: O(perim.length) per segment, no area allocation).
		double totalRadius = 0;
		int    radiusCount = 0;
		for (FrameSet fs : frameSets) {
			for (Segment seg : fs) {
				Point[] perim = seg.getExternalPerimeter();
				if (perim == null || perim.length < 3) continue;
				int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
				int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
				for (Point p : perim) {
					if (p.x < minX) minX = p.x;  if (p.x > maxX) maxX = p.x;
					if (p.y < minY) minY = p.y;  if (p.y > maxY) maxY = p.y;
				}
				// Mean of half-width and half-height as radius estimate
				double r = ((maxX - minX) + (maxY - minY)) / 4.0;
				if (r > 0) { totalRadius += r; radiusCount++; }
			}
		}
		if (radiusCount == 0) return null;
		double meanRadius = totalRadius / radiusCount;
		double sigmaSize  = meanRadius / Math.sqrt(2); // LoG-optimal conversion

		// ── Method 2: Inter-object spacing → sigma ceiling ───────────────────
		// Minimum nearest-neighbour distance between centers across all frames.
		double minDist = Double.MAX_VALUE;
		for (FrameSet fs : frameSets) {
			List<Point> centers = new ArrayList<>();
			for (Segment seg : fs) {
				Point cp = seg.getCenterPoint();
				if (cp != null) centers.add(cp);
			}
			for (int i = 0; i < centers.size(); i++) {
				for (int j = i + 1; j < centers.size(); j++) {
					double d = centers.get(i).distance(centers.get(j));
					if (d < minDist) minDist = d;
				}
			}
		}
		double sigmaFinal;
		if (minDist < Double.MAX_VALUE) {
			double sigmaMax = minDist / 4.0;
			sigmaFinal = Math.min(sigmaSize, sigmaMax);
		} else {
			sigmaFinal = sigmaSize; // single object or no spacing data
		}

		// ── Method 3: Center-point intensity → threshold floor ───────────────
		// Blur each frame at the chosen sigma, compute the kernel-averaged
		// intensity at each known center (same kernel as filterLowPoints),
		// and find the minimum effective threshold across all centers/frames.
		double minEffectiveThreshold = Double.MAX_VALUE;
		for (FrameSet fs : frameSets) {
			if (fs.size() == 0) continue;
			int frameNo = fs.getFrame();
			if (frameNo < 0 || frameNo >= stack.getSize()) continue;

			ImageProcessor proc = stack.getProcessor(frameNo + 1).duplicate();
			if (invertIntensity) proc.invert();
			new GaussianBlur().blurGaussian(proc, sigmaFinal);

			// Per-frame intensity range (matches filterLowPoints min/max scan)
			int fMin = Integer.MAX_VALUE, fMax = Integer.MIN_VALUE;
			for (int x = 0; x < proc.getWidth(); x++) {
				for (int y = 0; y < proc.getHeight(); y++) {
					int v = proc.get(x, y);
					if (v < fMin) fMin = v;
					if (v > fMax) fMax = v;
				}
			}
			if (fMax <= fMin) continue; // flat frame — skip

			for (Segment seg : fs) {
				Point cp = seg.getCenterPoint();
				if (cp == null) continue;
				int kAvg = kernelAverage(cp.x, cp.y, proc);
				if (kAvg < 0) continue; // center at image edge — skip
				double effThresh = (double)(kAvg - fMin) / (fMax - fMin);
				if (effThresh < minEffectiveThreshold) minEffectiveThreshold = effThresh;
			}
		}

		double threshold;
		if (minEffectiveThreshold < Double.MAX_VALUE) {
			// Subtract safety margin so objects fractionally dimmer in subsequent
			// stacks are still detected.
			threshold = Math.max(0.0, minEffectiveThreshold - THRESHOLD_MARGIN);
		} else {
			threshold = 0.05; // fallback: 5% if no center data available
		}

		return new double[]{ sigmaFinal, threshold };
	}

	// ── Boundary Cleanup search-distance estimation ──────────────────────────

	/** Clamp bounds for the fraction returned by {@link #estimateSearchFraction}. Not exposed —
	 * an estimate outside this range would either barely search anything (too small) or approach
	 * the cost of always searching the full available half of the contour (too large). */
	private static final double SEARCH_FRACTION_MIN = 0.05;
	private static final double SEARCH_FRACTION_MAX = 0.35;

	/** Target search window (in points) for a *median*-sized boundary in the dataset, used to
	 * back-solve {@code searchFraction}. A modest multiple of {@link
	 * GeometricCalculations#SEARCH_DISTANCE_FLOOR} — enough to search meaningfully past the
	 * floor, not an attempt to guess a "correct" absolute loop size (there's no ground truth for
	 * that; the user's own visual verification in Guided Calibration is what confirms it). */
	private static final double TARGET_MEDIAN_SEARCH_WINDOW = GeometricCalculations.SEARCH_DISTANCE_FLOOR * 1.5;

	/**
	 * Estimates a starting {@code searchFraction}/{@code searchCeiling} pair for Boundary Cleanup
	 * from the currently loaded {@link DataSet}'s own external-perimeter point-count distribution.
	 * <p>
	 * Unlike {@link #calibrateFromDataSet}, there is no separate ground truth to calibrate
	 * against here — the boundaries in {@code ds} <em>are</em> the thing being cleaned, not a
	 * reference to match. This is a simpler self-referential estimate: it scales the fraction so
	 * a median-sized object in this dataset gets a reasonably sized (not floor-dominated, not
	 * excessive) search window, and sets the ceiling from the largest object actually present so
	 * typical-size boundaries in this dataset aren't truncated by an arbitrary cap. It is a
	 * starting point for the user's own visual verification via Guided Calibration, not a
	 * validated final answer.
	 *
	 * @param ds DataSet with external (SARN envelope) perimeters already populated
	 * @return {@code double[]{searchFraction, searchCeiling}} (searchCeiling as a whole-number
	 *         double, round before use), or {@code null} if the DataSet contains no usable
	 *         perimeter data
	 */
	public static double[] estimateSearchFraction(DataSet ds) {
		FrameSet[] frameSets = ds.getFrameSetList();
		if (frameSets == null) return null;

		List<Integer> pointCounts = new ArrayList<>();
		for (FrameSet fs : frameSets) {
			for (Segment seg : fs) {
				Point[] perim = seg.getExternalPerimeter();
				if (perim != null && perim.length >= 3) pointCounts.add(perim.length);
			}
		}
		if (pointCounts.isEmpty()) return null;

		Collections.sort(pointCounts);
		int median = pointCounts.get(pointCounts.size() / 2);
		int max = pointCounts.get(pointCounts.size() - 1);

		double fraction = TARGET_MEDIAN_SEARCH_WINDOW / median;
		fraction = Math.max(SEARCH_FRACTION_MIN, Math.min(SEARCH_FRACTION_MAX, fraction));

		// Half the largest observed contour: searching further would mostly be truncated anyway
		// by removeLoops's own size/2 wraparound-safety clamp.
		double ceiling = Math.max(GeometricCalculations.SEARCH_DISTANCE_FLOOR * 2, max * 0.5);

		return new double[]{ fraction, ceiling };
	}

	// ── Internal helpers ─────────────────────────────────────────────────────

	/**
	 * Computes the normalized LoG response at pixel (cx, cy) in an
	 * already-blurred image using the 4-connected discrete Laplacian:
	 * <pre>σ² × (I[x+1,y] + I[x-1,y] + I[x,y+1] + I[x,y-1] − 4·I[x,y])</pre>
	 * Returns 0 for border pixels where the stencil would fall outside the image.
	 */
	private static float normalizedLoGAt(ImageProcessor blurred,
	                                      double sigma, int cx, int cy) {
		int w = blurred.getWidth(), h = blurred.getHeight();
		if (cx <= 0 || cx >= w - 1 || cy <= 0 || cy >= h - 1) return 0;
		float lap = blurred.getPixelValue(cx + 1, cy)
		          + blurred.getPixelValue(cx - 1, cy)
		          + blurred.getPixelValue(cx, cy + 1)
		          + blurred.getPixelValue(cx, cy - 1)
		          - 4 * blurred.getPixelValue(cx, cy);
		return (float)(sigma * sigma * lap);
	}

	/**
	 * Computes the kernel-averaged intensity around (cx, cy) using a
	 * (2·{@value #KERNEL_SIZE}+1) × (2·{@value #KERNEL_SIZE}+1) square
	 * neighbourhood, matching the kernel used in
	 * {@link Identification#filterLowPoints}.
	 *
	 * @return average pixel intensity in the kernel, or −1 if any kernel
	 *         pixel falls outside the image boundary
	 */
	private static int kernelAverage(int cx, int cy, ImageProcessor proc) {
		int w = proc.getWidth(), h = proc.getHeight();
		int sum = 0, count = 0;
		for (int dx = -KERNEL_SIZE; dx <= KERNEL_SIZE; dx++) {
			int x = cx + dx;
			if (x < 0 || x >= w) return -1;
			for (int dy = -KERNEL_SIZE; dy <= KERNEL_SIZE; dy++) {
				int y = cy + dy;
				if (y < 0 || y >= h) return -1;
				sum += proc.get(x, y);
				count++;
			}
		}
		return (count > 0) ? sum / count : -1;
	}
}
