package pipeline;

import javax.swing.JProgressBar;

import dataStructure.DataSet;
import geometricTools.ModifiedMaximumFinder;
import gui.SegmentationFilters;
import identification.Identification;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import linkage.Linkage;
import sarn.Sarn;
import segmentation.Segmentation;
import util.UserInputException;

/**
 * The automatic segmentation pipeline for one operation panel, independent of the GUI:
 * identification → SARN (external segmentation) → internal segmentation → linkage → filters.
 * <p>
 * Each step is a separate method so the GUI can keep running them one button at a time
 * ({@code OperationModel} and {@code RecursionOperationModel} call them, passing the panel's own
 * method instances), while batch runs and tests call {@link #runAll} with methods created from
 * the class names in {@link PipelineSettings}.
 * <p>
 * The algorithms report progress through a Swing {@link JProgressBar}. The GUI passes its shared
 * bar; headless callers may pass {@code null} and a private one is used.
 */
public final class Seg2TracksPipeline {

	private Seg2TracksPipeline() { }

	// ── Input ────────────────────────────────────────────────────────────────

	/**
	 * Loads the whole stack into memory (so every stage reads from RAM) and inverts it once if
	 * asked, before any stage sees it. Same as the GUI's run.
	 *
	 * @throws UserInputException if the file cannot be opened
	 */
	public static ImageStack loadStack(String path, boolean invert) {
		ImagePlus virtual = IJ.openVirtual(path);
		if (virtual == null) throw new UserInputException("Could not open input file:\n" + path);
		ImageStack virtualStack = virtual.getImageStack();
		ImageStack stack = new ImageStack(virtualStack.getWidth(), virtualStack.getHeight());
		for (int i = 1; i <= virtualStack.getSize(); i++) {
			stack.addSlice(virtualStack.getProcessor(i).duplicate());
		}
		if (invert) {
			for (int i = 1; i <= stack.getSize(); i++) stack.getProcessor(i).invert();
		}
		return stack;
	}

	/** A new, empty dataset sized to the stack. */
	public static DataSet newDataSet(ImageStack stack) {
		return new DataSet(stack.getWidth(), stack.getHeight(), stack.getSize());
	}

	// ── Steps ────────────────────────────────────────────────────────────────

	/** Finds object centres in every frame. */
	public static void identify(ImageStack stack, DataSet dataSet, PipelineSettings s, JProgressBar progress) {
		progress = bar(progress);
		progress.setMinimum(0);
		progress.setMaximum(stack.getSize());
		progress.setValue(0);
		Identification id = new Identification();
		id.initialize(stack, dataSet, progress);
		id.setBlur(new GaussianBlur(), s.getGaussianBlurSigma());
		id.setFinder(new ModifiedMaximumFinder(), s.getMaximumFinderTolerance());
		id.setRecursiveTolerancePct(s.getRecursiveTolerancePct());
		id.run();
		dataSet.setIdentificationExists(true);
	}

	/** SARN: each object's external envelope. */
	public static void segmentExternal(ImageStack stack, DataSet dataSet, PipelineSettings s,
			Sarn method, JProgressBar progress) {
		progress = bar(progress);
		method.initialize(stack, dataSet, progress);
		method.setBlur(new GaussianBlur(), s.getGaussianBlurSigma());
		method.setCleanupParams(s.getSearchFraction(), s.getSearchCeiling(), s.getSimplificationEpsilon());
		method.run();
		dataSet.setExternalSegmentationExists(true);
	}

	/**
	 * Internal (threshold) segmentation.
	 *
	 * @throws UserInputException if the method needs SARN boundaries and there are none
	 */
	public static void segmentInternal(ImageStack stack, DataSet dataSet, PipelineSettings s,
			Segmentation method, JProgressBar progress) {
		if (method.isExternallyDependent() && !dataSet.getExternalSegmentationExists()) {
			throw new UserInputException("\"" + method + "\" thresholds inside each object's SARN "
					+ "boundary, but SARN has not been run. Run SARN first, or choose a method "
					+ "that does not need it.");
		}
		progress = bar(progress);
		method.initialize(stack, dataSet, progress);
		method.setBlur(new GaussianBlur(), s.getGaussianBlurSigma());
		method.setCleanupParams(s.getSearchFraction(), s.getSearchCeiling(), s.getSimplificationEpsilon());
		method.run();
		dataSet.setInternalSegmentationExists(true);
	}

	/** Links objects across frames into tracks. */
	public static void link(DataSet dataSet, Linkage method, JProgressBar progress) {
		progress = bar(progress);
		method.initialize(dataSet, progress);
		method.run();
		dataSet.setLinkageExists(true);
	}

	/** Segmentation filters (edge exclusion), applied after internal segmentation. */
	public static void filter(DataSet dataSet, PipelineSettings s, JProgressBar progress) {
		SegmentationFilters filter = new SegmentationFilters();
		filter.initialize(dataSet, bar(progress), s.getExcludeInternalEdges());
		filter.run();
	}

	// ── Whole run (batch, tests) ─────────────────────────────────────────────

	/**
	 * Runs every requested step on a fresh dataset, in the same order the GUI does across its
	 * SARN and internal-segmentation Run buttons: identification, SARN, linkage, internal
	 * segmentation, (linkage if not yet done), filters. Methods are created from the class
	 * names in the settings.
	 *
	 * @param runExternal run SARN
	 * @param runInternal run internal segmentation
	 */
	public static DataSet runAll(ImageStack stack, PipelineSettings s,
			boolean runExternal, boolean runInternal, JProgressBar progress) {
		progress = bar(progress);
		DataSet dataSet = newDataSet(stack);
		identify(stack, dataSet, s, progress);
		if (runExternal) {
			segmentExternal(stack, dataSet, s, createSarn(s), progress);
			link(dataSet, createLinkage(s), progress);
		}
		if (runInternal) {
			segmentInternal(stack, dataSet, s, createSegmentation(s), progress);
			if (!dataSet.getLinkageExists()) link(dataSet, createLinkage(s), progress);
		}
		if (dataSet.getInternalSegmentationExists()) filter(dataSet, s, progress);
		return dataSet;
	}

	// ── Methods by class name ────────────────────────────────────────────────

	public static Sarn createSarn(PipelineSettings s) {
		return create(s.getExternalSegmentationMethod(), Sarn.class);
	}

	public static Segmentation createSegmentation(PipelineSettings s) {
		return create(s.getInternalSegmentationMethod(), Segmentation.class);
	}

	public static Linkage createLinkage(PipelineSettings s) {
		return create(s.getLinkageMethod(), Linkage.class);
	}

	private static <T> T create(String className, Class<T> type) {
		try {
			Class<?> c = Class.forName(className, true, Seg2TracksPipeline.class.getClassLoader());
			return type.cast(c.getConstructor().newInstance());
		} catch (ClassNotFoundException | ClassCastException e) {
			throw new UserInputException("Unknown " + type.getSimpleName() + " method: " + className);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not create " + className, e);
		}
	}

	private static JProgressBar bar(JProgressBar progress) {
		return (progress != null) ? progress : new JProgressBar();
	}
}
