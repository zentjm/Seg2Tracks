package gui;

import java.awt.Point;
import java.util.HashMap;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.RecursiveDataSet;
import dataStructure.Segment;
import identification.Identification;
import ij.IJ;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.GaussianBlur;
import geometricTools.ModifiedMaximumFinder;
import ij.process.ImageProcessor;
import linkage.Linkage;
import sarn.Sarn;
import segmentation.Segmentation;

/**
 * Model for the recursive/subsegmentation operation panel.
 * Extends OperationModel to support multi-level segmentation: after a primary
 * segmentation pass identifies and tracks objects (e.g. macrophages), this model
 * runs a second segmentation pass on the masked interior of each primary-level
 * LinkSet to find internal structures (e.g. phagosomal voids in ADCP experiments).
 *
 * Algorithm per parent LinkSet:
 *   1. Compute a union bounding box across all frames for the parent cell.
 *   2. Build a cropped, masked ImageStack — only the bbox region is kept, with
 *      pixels outside the cell's internal perimeter zeroed.
 *   3. Run identification, segmentation, and linkage on the cropped stack.
 *   4. Translate child coordinates back to full-image space.
 *   5. Store the child DataSet on the parent LinkSet via setChildDataSet().
 *   6. Accumulate all child LinkSets into a combined RecursiveDataSet for analysis.
 */
public class RecursionOperationModel extends OperationModel {

	// Per-parent-loop state — reset at the start of each LinkSet iteration in runIt().
	// Declared as fields so the extracted pipeline methods can read them without
	// parameter threading (mirrors the pattern used in OperationModel).
	private int minX, minY, cropW, cropH;
	private ImageStack maskedStack;
	private HashMap<Integer, Point[]> parentPerimeterMap;
	private RecursiveDataSet childDataSet;
	private RecursiveDataSet combinedDataSet;

	/**
	 * Constructor for recursive operation model.
	 * @param panelNumber The panel index in the recursion hierarchy (always > 0)
	 */
	public RecursionOperationModel(int panelNumber) {
		super(panelNumber);
	}

	/**
	 * Runs the recursive segmentation pipeline.
	 * Iterates over all LinkSets in the prior panel's DataSet, masks the input image
	 * to each cell's interior, runs the appropriate pipeline steps within each masked
	 * region, and stores results.
	 *
	 * @param runType 0 = external segmentation (SARN), 1 = internal segmentation
	 */
	@Override
	public void runIt(int runType) throws Exception {

		progressBar = controller.getProgressBar();

		// Load the full stack into memory once. Stages work on per-frame duplicates
		// so this copy stays clean, and inversion (if requested) is applied here only.
		ImageStack virtualStack = IJ.openVirtual(controller.getInputFilePath()).getImageStack();
		inputStack = new ImageStack(virtualStack.getWidth(), virtualStack.getHeight());
		for (int i = 1; i <= virtualStack.getSize(); i++) {
			inputStack.addSlice(virtualStack.getProcessor(i).duplicate());
		}
		if (controller.getInvertIntensity()) {
			for (int i = 1; i <= inputStack.getSize(); i++) {
				inputStack.getProcessor(i).invert();
			}
		}

		DataSet priorDataSet = controller.getPriorDataSet();
		if (priorDataSet == null || priorDataSet.getLinkSetList().isEmpty()) {
			throw new Exception(
				"No prior segmentation found. Complete segmentation on the previous panel before running recursive segmentation.");
		}

		combinedDataSet = new RecursiveDataSet(
			inputStack.getWidth(), inputStack.getHeight(), inputStack.getSize(), priorDataSet);

		progressBar.setString("Recursive segmentation");
		progressBar.setMinimum(0);
		progressBar.setMaximum(priorDataSet.getLinkSetList().size());
		progressBar.setValue(0);
		int progress = 0;

		for (LinkSet parentLinkSet : priorDataSet.getLinkSetList()) {

			if (!computeBoundingBox(parentLinkSet)) {
				progressBar.setValue(++progress);
				continue;
			}

			maskedStack = buildCroppedMaskedStack(inputStack, parentLinkSet, minX, minY, cropW, cropH);
			buildParentPerimeterMap(parentLinkSet);

			if (runType == 0) {
				childDataSet = new RecursiveDataSet(cropW, cropH, inputStack.getSize(), priorDataSet);
				runIdentification();
				runExternalSegmentation();
			} else if (runType == 1) {
				if (controller.getInternalSegmentationMethod().isExternallyDependent()) {
					// Restricted method (e.g. RestrictedTriangleMethod): needs external perimeters
					// from a prior recursive SARN run stored on the parent LinkSet.
					if (!prepareInternalSegmentationDataSet(parentLinkSet)) {
						progressBar.setValue(++progress);
						continue;
					}
				} else {
					// Global method (e.g. TriangleMethod): no external perimeters needed —
					// run identification fresh and threshold the whole masked region.
					childDataSet = new RecursiveDataSet(cropW, cropH, inputStack.getSize(), priorDataSet);
					runIdentification();
				}
				runInternalSegmentation();
			}

			if (!childDataSet.getLinkageExists()) runLinkage();
			if (childDataSet.getInternalSegmentationExists()) runSegmentationFilters();

			parentLinkSet.setChildDataSet(childDataSet);
			translateAndAccumulate(parentLinkSet);
			progressBar.setValue(++progress);
		}

		if (runType == 0) combinedDataSet.setExternalSegmentationExists(true);
		if (runType == 1) combinedDataSet.setInternalSegmentationExists(true);
		combinedDataSet.setLinkageExists(true);

		dataSet = combinedDataSet;
		controller.setRunData(runType, dataSet);
		progressBar.setValue(progressBar.getMaximum());
	}

	/**
	 * Computes the union bounding box of the parent cell's internal perimeter across
	 * all frames and stores it in minX/minY/cropW/cropH (with 3px padding, clamped).
	 *
	 * @return false if no frame has a valid internal perimeter (parent should be skipped)
	 */
	private boolean computeBoundingBox(LinkSet parentLinkSet) {
		int bMinX = Integer.MAX_VALUE, bMinY = Integer.MAX_VALUE;
		int bMaxX = 0, bMaxY = 0;
		boolean hasPerimeter = false;
		for (Segment seg : parentLinkSet) {
			Point[] perim = seg.getInternalPerimeter();
			if (perim == null || perim.length == 0) continue;
			hasPerimeter = true;
			for (Point p : perim) {
				if (p.x < bMinX) bMinX = p.x;
				if (p.x > bMaxX) bMaxX = p.x;
				if (p.y < bMinY) bMinY = p.y;
				if (p.y > bMaxY) bMaxY = p.y;
			}
		}
		if (!hasPerimeter) return false;

		int pad = 3;
		minX = Math.max(0, bMinX - pad);
		minY = Math.max(0, bMinY - pad);
		int maxX = Math.min(inputStack.getWidth() - 1, bMaxX + pad);
		int maxY = Math.min(inputStack.getHeight() - 1, bMaxY + pad);
		cropW = maxX - minX + 1;
		cropH = maxY - minY + 1;
		return true;
	}

	/**
	 * Builds parentPerimeterMap: maps each frame index to the parent cell's internal
	 * perimeter translated into cropped coordinates (origin at minX, minY).
	 */
	private void buildParentPerimeterMap(LinkSet parentLinkSet) {
		parentPerimeterMap = new HashMap<>();
		for (Segment seg : parentLinkSet) {
			Point[] perim = seg.getInternalPerimeter();
			if (perim != null && perim.length > 0) {
				Point[] croppedPerim = new Point[perim.length];
				for (int p = 0; p < perim.length; p++) {
					croppedPerim[p] = new Point(perim[p].x - minX, perim[p].y - minY);
				}
				parentPerimeterMap.put(seg.getFrame(), croppedPerim);
			}
		}
	}

	/**
	 * Runs identification on the cropped masked stack, restricting the maxima scan
	 * to the parent cell's interior via the parentPerimeterMap.
	 * Results are stored in childDataSet.
	 */
	@Override
	protected void runIdentification() {
		Identification id = new Identification();
		id.initialize(maskedStack, childDataSet, progressBar);
		id.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		id.setFinder(new ModifiedMaximumFinder(), controller.getMaximumFinderTolerance());
		id.setRecursionPerimeterMap(parentPerimeterMap);
		id.run();
		childDataSet.setIdentificationExists(true);
	}

	/**
	 * Runs SARN on the cropped masked stack, clipping child external perimeters to
	 * the parent cell boundary via the parentPerimeterMap.
	 * Results are stored in childDataSet.
	 */
	@Override
	protected void runExternalSegmentation() {
		Sarn exSeg = controller.getExternalSegmentationMethod();
		exSeg.initialize(maskedStack, childDataSet, progressBar);
		exSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		exSeg.setParentPerimeterMap(parentPerimeterMap);
		exSeg.run();
		childDataSet.setExternalSegmentationExists(true);
	}

	/**
	 * Retrieves the SARN child DataSet stored on the parent LinkSet from a prior
	 * recursive SARN run and translates its segment coordinates to cropped space
	 * so that runInternalSegmentation() can operate on the maskedStack.
	 * <p>
	 * translateAndAccumulate() will add minX/minY back to all fields, so the
	 * double-offset for centerPoint and externalPerimeter cancels cleanly.
	 *
	 * @return false if no prior SARN child data exists for this parent (skip it)
	 */
	private boolean prepareInternalSegmentationDataSet(LinkSet parentLinkSet) {
		DataSet priorChild = parentLinkSet.getChildDataSet();
		if (priorChild == null || !priorChild.getExternalSegmentationExists()) return false;

		childDataSet = (RecursiveDataSet) priorChild;

		// Translate child segment coordinates from full-image → cropped space
		for (int fi = 0; fi < inputStack.getSize(); fi++) {
			FrameSet fs = childDataSet.getFrameSet(fi);
			if (fs == null) continue;
			for (Segment s : fs) {
				Point cp = s.getCenterPoint();
				if (cp != null) s.setCenterPoint(new Point(cp.x - minX, cp.y - minY));
				offsetPerimeter(s.getExternalPerimeter(), -minX, -minY);
			}
		}
		return true;
	}

	/**
	 * Runs restricted internal segmentation (triangle/Otsu method) on the cropped
	 * masked stack using childDataSet. Each child Segment must already have
	 * externalPerimeter set (provided by the prior recursive SARN run).
	 */
	@Override
	protected void runInternalSegmentation() {
		Segmentation inSeg = controller.getInternalSegmentationMethod();
		inSeg.initialize(maskedStack, childDataSet, progressBar);
		inSeg.setBlur(new GaussianBlur(), controller.getGaussianBlurSigma());
		inSeg.setSkipZeroBin(true);
		inSeg.run();
		childDataSet.setInternalSegmentationExists(true);
	}

	/**
	 * Runs linkage on childDataSet to track child segments across frames.
	 */
	@Override
	protected void runLinkage() {
		Linkage link = controller.getLinkageMethod();
		link.initialize(childDataSet, progressBar);
		link.run();
		childDataSet.setLinkageExists(true);
	}

	/**
	 * Runs segmentation filters on childDataSet.
	 */
	@Override
	protected void runSegmentationFilters() {
		SegmentationFilters filter = new SegmentationFilters();
		filter.initialize(childDataSet, progressBar, controller.getExcludeInternalEdges());
		filter.run();
	}

	/**
	 * Translates all child segment coordinates from cropped space back to full-image
	 * space and accumulates child LinkSets into combinedDataSet.
	 */
	private void translateAndAccumulate(LinkSet parentLinkSet) {
		for (LinkSet childLinkSet : childDataSet.getLinkSetList()) {
			for (Segment s : childLinkSet) {
				Point cp = s.getCenterPoint();
				if (cp != null) s.setCenterPoint(new Point(cp.x + minX, cp.y + minY));
				offsetPerimeter(s.getInternalPerimeter(), minX, minY);
				offsetPerimeter(s.getExternalPerimeter(), minX, minY);
			}
			combinedDataSet.addLinkSet(childLinkSet);
			combinedDataSet.addChildParentMapping(childLinkSet, parentLinkSet);
		}
	}

	/**
	 * Translates all points in a perimeter array by the given offset.
	 * Used to convert between cropped and full-image coordinates.
	 */
	private void offsetPerimeter(Point[] perimeter, int offsetX, int offsetY) {
		if (perimeter == null) return;
		for (int i = 0; i < perimeter.length; i++) {
			perimeter[i] = new Point(perimeter[i].x + offsetX, perimeter[i].y + offsetY);
		}
	}

	/**
	 * Builds a cropped, masked copy of the input ImageStack for a single parent LinkSet.
	 * Only the bounding box region is extracted from each frame. Pixels inside the bbox
	 * but outside the cell's internal perimeter are zeroed. A 1-pixel erosion is applied
	 * to prevent spurious maxima at the mask boundary.
	 *
	 * @param source  the full input ImageStack
	 * @param linkSet the parent LinkSet whose interior should be preserved
	 * @param cropX   left edge of the bounding box in full-image coordinates
	 * @param cropY   top edge of the bounding box in full-image coordinates
	 * @param cropW   width of the bounding box
	 * @param cropH   height of the bounding box
	 * @return a new ImageStack with dimensions cropW×cropH, masked to the linkSet's interior
	 */
	private ImageStack buildCroppedMaskedStack(ImageStack source, LinkSet linkSet,
			int cropX, int cropY, int cropW, int cropH) {
		ImageStack masked = new ImageStack(cropW, cropH);

		// Build frame → segment lookup (segment.getFrame() is 0-based; stack is 1-based)
		HashMap<Integer, Segment> frameToSegment = new HashMap<>();
		for (Segment seg : linkSet) {
			frameToSegment.put(seg.getFrame(), seg);
		}

		for (int stackFrame = 1; stackFrame <= source.getSize(); stackFrame++) {
			// Crop the bounding box region from the source frame.
			// Must store getProcessor() in a local variable — calling it twice returns
			// different instances; setRoi() on one instance would be lost on crop().
			ImageProcessor srcProc = source.getProcessor(stackFrame);
			srcProc.setRoi(cropX, cropY, cropW, cropH);
			ImageProcessor ip = srcProc.crop();

			Segment seg = frameToSegment.get(stackFrame - 1); // convert to 0-based

			if (seg != null && seg.getInternalPerimeter() != null
					&& seg.getInternalPerimeter().length > 0) {
				// Translate perimeter to cropped coordinates and build ROI
				Point[] perimeter = seg.getInternalPerimeter();
				int[] xPoints = new int[perimeter.length];
				int[] yPoints = new int[perimeter.length];
				for (int p = 0; p < perimeter.length; p++) {
					xPoints[p] = perimeter[p].x - cropX;
					yPoints[p] = perimeter[p].y - cropY;
				}
				PolygonRoi roi = new PolygonRoi(xPoints, yPoints, perimeter.length, Roi.POLYGON);

				ip.setValue(0);
				ip.fillOutside(roi);

				// 1-pixel erosion: zero interior pixels adjacent to the masked boundary so the
				// zero-adjacency exclusion in ModifiedMaximumFinder has a clean 1-pixel buffer.
				int w = ip.getWidth(), h = ip.getHeight();
				boolean[] toErase = new boolean[w * h];

				for (int ey = 0; ey < h; ey++) {
					for (int ex = 0; ex < w; ex++) {
						if (ip.get(ex, ey) == 0) continue;
						outer:
						for (int dy = -1; dy <= 1; dy++) {
							for (int dx = -1; dx <= 1; dx++) {
								if (dx == 0 && dy == 0) continue;
								int nx = ex + dx, ny2 = ey + dy;
								if (nx < 0 || nx >= w || ny2 < 0 || ny2 >= h || ip.get(nx, ny2) == 0) {
									toErase[ey * w + ex] = true;
									break outer;
								}
							}
						}
					}
				}

				for (int ei = 0; ei < w * h; ei++)
					if (toErase[ei]) ip.set(ei % w, ei / w, 0);
			} else {
				// No segment in this frame — zero the whole slice
				ip.setValue(0);
				ip.fill();
			}

			masked.addSlice(ip);
		}

		return masked;
	}
}
