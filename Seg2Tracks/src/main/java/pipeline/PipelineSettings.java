package pipeline;

import java.util.Properties;

/**
 * Everything one operation panel's automatic run depends on, as plain data: the
 * identification, boundary cleanup and filter parameters, plus the SARN, internal
 * segmentation and linkage methods by fully qualified class name.
 * <p>
 * The GUI builds one from a panel ({@code OperationController.toPipelineSettings()}); batch
 * runs and tests build one from a template file or in code. Methods are stored by class name,
 * not by their position in {@code seg2tracks.config}, so settings keep their meaning if that
 * list is reordered.
 * <p>
 * {@link #toProperties} / {@link #fromProperties} give a simple text form for batch templates
 * and run metadata. Values are fixed at construction; use the {@code with...} copies to vary
 * one parameter (e.g. in a parameter sweep).
 */
public final class PipelineSettings {

	// Identification
	private final double gaussianBlurSigma;
	/** Fraction of the intensity range, 0–1 (the GUI shows it as a percentage). */
	private final double maximumFinderTolerance;
	/** Percent of the intensity range, used in recursive (subsegmentation) mode. */
	private final double recursiveTolerancePct;
	private final boolean invertIntensity;

	// SARN boundary cleanup
	private final double searchFraction;
	private final int searchCeiling;
	private final double simplificationEpsilon;

	// Segmentation filters
	private final boolean excludeInternalEdges;

	// Methods (fully qualified class names, as in seg2tracks.config)
	private final String externalSegmentationMethod;
	private final String internalSegmentationMethod;
	private final String linkageMethod;

	public PipelineSettings(double gaussianBlurSigma, double maximumFinderTolerance,
			double recursiveTolerancePct, boolean invertIntensity,
			double searchFraction, int searchCeiling, double simplificationEpsilon,
			boolean excludeInternalEdges,
			String externalSegmentationMethod, String internalSegmentationMethod,
			String linkageMethod) {
		this.gaussianBlurSigma = gaussianBlurSigma;
		this.maximumFinderTolerance = maximumFinderTolerance;
		this.recursiveTolerancePct = recursiveTolerancePct;
		this.invertIntensity = invertIntensity;
		this.searchFraction = searchFraction;
		this.searchCeiling = searchCeiling;
		this.simplificationEpsilon = simplificationEpsilon;
		this.excludeInternalEdges = excludeInternalEdges;
		this.externalSegmentationMethod = externalSegmentationMethod;
		this.internalSegmentationMethod = internalSegmentationMethod;
		this.linkageMethod = linkageMethod;
	}

	public double getGaussianBlurSigma()        { return gaussianBlurSigma; }
	public double getMaximumFinderTolerance()   { return maximumFinderTolerance; }
	public double getRecursiveTolerancePct()    { return recursiveTolerancePct; }
	public boolean getInvertIntensity()         { return invertIntensity; }
	public double getSearchFraction()           { return searchFraction; }
	public int getSearchCeiling()               { return searchCeiling; }
	public double getSimplificationEpsilon()    { return simplificationEpsilon; }
	public boolean getExcludeInternalEdges()    { return excludeInternalEdges; }
	public String getExternalSegmentationMethod() { return externalSegmentationMethod; }
	public String getInternalSegmentationMethod() { return internalSegmentationMethod; }
	public String getLinkageMethod()            { return linkageMethod; }

	// ── Copies with one value changed (parameter sweeps, tests) ──────────────

	public PipelineSettings withGaussianBlurSigma(double v) {
		return new PipelineSettings(v, maximumFinderTolerance, recursiveTolerancePct, invertIntensity,
				searchFraction, searchCeiling, simplificationEpsilon, excludeInternalEdges,
				externalSegmentationMethod, internalSegmentationMethod, linkageMethod);
	}

	public PipelineSettings withMaximumFinderTolerance(double v) {
		return new PipelineSettings(gaussianBlurSigma, v, recursiveTolerancePct, invertIntensity,
				searchFraction, searchCeiling, simplificationEpsilon, excludeInternalEdges,
				externalSegmentationMethod, internalSegmentationMethod, linkageMethod);
	}

	public PipelineSettings withMethods(String external, String internal, String linkage) {
		return new PipelineSettings(gaussianBlurSigma, maximumFinderTolerance, recursiveTolerancePct,
				invertIntensity, searchFraction, searchCeiling, simplificationEpsilon,
				excludeInternalEdges, external, internal, linkage);
	}

	// ── Text form (batch templates, run metadata) ────────────────────────────

	/**
	 * Writes the settings as properties, each key prefixed with {@code prefix} (e.g. "panel0.")
	 * so several panels can share one file.
	 */
	public void toProperties(Properties props, String prefix) {
		props.setProperty(prefix + "gaussianBlurSigma", Double.toString(gaussianBlurSigma));
		props.setProperty(prefix + "maximumFinderTolerance", Double.toString(maximumFinderTolerance));
		props.setProperty(prefix + "recursiveTolerancePct", Double.toString(recursiveTolerancePct));
		props.setProperty(prefix + "invertIntensity", Boolean.toString(invertIntensity));
		props.setProperty(prefix + "searchFraction", Double.toString(searchFraction));
		props.setProperty(prefix + "searchCeiling", Integer.toString(searchCeiling));
		props.setProperty(prefix + "simplificationEpsilon", Double.toString(simplificationEpsilon));
		props.setProperty(prefix + "excludeInternalEdges", Boolean.toString(excludeInternalEdges));
		props.setProperty(prefix + "externalSegmentationMethod", externalSegmentationMethod);
		props.setProperty(prefix + "internalSegmentationMethod", internalSegmentationMethod);
		props.setProperty(prefix + "linkageMethod", linkageMethod);
	}

	/**
	 * Reads settings written by {@link #toProperties}.
	 *
	 * @throws IllegalArgumentException naming the first missing or malformed key
	 */
	public static PipelineSettings fromProperties(Properties props, String prefix) {
		return new PipelineSettings(
				dbl(props, prefix, "gaussianBlurSigma"),
				dbl(props, prefix, "maximumFinderTolerance"),
				dbl(props, prefix, "recursiveTolerancePct"),
				bool(props, prefix, "invertIntensity"),
				dbl(props, prefix, "searchFraction"),
				(int) dbl(props, prefix, "searchCeiling"),
				dbl(props, prefix, "simplificationEpsilon"),
				bool(props, prefix, "excludeInternalEdges"),
				str(props, prefix, "externalSegmentationMethod"),
				str(props, prefix, "internalSegmentationMethod"),
				str(props, prefix, "linkageMethod"));
	}

	private static String str(Properties p, String prefix, String key) {
		String v = p.getProperty(prefix + key);
		if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("Missing setting: " + prefix + key);
		return v.trim();
	}

	private static double dbl(Properties p, String prefix, String key) {
		String v = str(p, prefix, key);
		try { return Double.parseDouble(v); }
		catch (NumberFormatException e) { throw new IllegalArgumentException("Not a number: " + prefix + key + " = " + v); }
	}

	private static boolean bool(Properties p, String prefix, String key) {
		String v = str(p, prefix, key);
		if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false"))
			throw new IllegalArgumentException("Not true/false: " + prefix + key + " = " + v);
		return Boolean.parseBoolean(v);
	}

	@Override
	public String toString() {
		Properties p = new Properties();
		toProperties(p, "");
		return "PipelineSettings" + new java.util.TreeMap<>(p);
	}
}
