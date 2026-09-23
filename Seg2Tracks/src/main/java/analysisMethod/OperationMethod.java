package analysisMethod;

import java.util.ArrayList;
import java.util.List;

import calculations.Data;
import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import dataStructure.SegmentModel;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

/**
 * Abstract base class for analysis methods that perform data operations followed by hierarchical quantification.
 * Provides framework for transforming DataSets and computing multi-level statistics (Segment -> LinkSet/FrameSet -> DataSet).
 *
 * <h3>Output format</h3>
 * Wide format (one row per entity, one column per calculation/statistic),
 * optimised for Excel pivot chart integration.  Three sheets are produced:
 * <ol>
 *   <li>Segment Data  — one row per (LinkSet × frame/segment)
 *   <li>Track Data    — one row per LinkSet, with direct LinkSet calculations and
 *                       per-statistic × per-calculation stat columns
 *   <li>Frame Data    — one row per frame, with direct FrameSet calculations and
 *                       per-statistic × per-calculation stat columns
 * </ol>
 * Statistic columns follow the pattern {@code StatName_CalcName} (e.g. {@code LinkSetMean_Area}).
 */
public abstract class OperationMethod extends AnalysisMethod {

	int segmentModelSheet;
	int frameSetSheet;
	int linkSetSheet;

	// ── Cached calculation name arrays ────────────────────────────────────────
	// Populated in defineSheets() by harvesting names from fresh instances.
	// Reused in setCalculations() (to skip re-creation) and retrieveCalculations()
	// (to identify which names to look up on stored data objects).

	String[] segmentCalculationNames;

	/**
	 * Subset of {@link #segmentCalculationNames} containing only the names of
	 * segment calculations whose {@link calculations.SegmentCalculation#isStatistic()}
	 * returns {@code true}.  These drive LinkSet-level and FrameSet-level aggregate
	 * statistic column headers (e.g. {@code LinkSetMean_Area}).
	 */
	String[] segmentStatisticCalculationNames;

	String[] linkSetCalculationNames;
	String[] linkSetStatisticNames;
	String[] frameSetCalculationNames;
	String[] frameSetStatisticNames;


	/**
	 * Execute the primary analysis workflow.
	 * Transforms input datasets using dataOperation() and stores as outputDataSets.
	 */
	public final void analyze() {
		outputDataSets = dataOperation(dataSets);
	}

	/**
	 * Define result workbook sheets with wide-format column headers.
	 * Names are harvested from fresh calculation instances so adding a new
	 * calculation automatically extends the relevant sheet.
	 *
	 * Three sheets are created:
	 * <ul>
	 *   <li><b>Segment Data</b>  — one column per segment calculation
	 *   <li><b>Track Data</b>    — LinkSet calculation columns + stat×calc columns
	 *   <li><b>Frame Data</b>    — FrameSet calculation columns + stat×calc columns
	 * </ul>
	 */
	@Override
	void defineSheets() {
		// Harvest names from fresh instances (thrown away — real instances are
		// created fresh per data object in setCalculations()).
		SegmentCalculation[] segCalcs = segmentCalculations();
		segmentCalculationNames = names(segCalcs);

		// Stat-eligible subset: isStatistic() == true (default is true for all).
		int statCount = 0;
		if (segCalcs != null) {
			for (SegmentCalculation c : segCalcs) if (c.isStatistic()) statCount++;
		}
		segmentStatisticCalculationNames = new String[statCount];
		int si = 0;
		if (segCalcs != null) {
			for (SegmentCalculation c : segCalcs)
				if (c.isStatistic()) segmentStatisticCalculationNames[si++] = c.getName();
		}

		linkSetCalculationNames  = names(linkSetCalculations());
		linkSetStatisticNames    = names(linkSetStatistics());
		frameSetCalculationNames = names(frameSetCalculations());
		frameSetStatisticNames   = names(frameSetStatistics());

		// 1. Segment Data — one row per (LinkSet × segment)
		segmentModelSheet = workbook.addSheet("Segment Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "LinkSet", "Frame"},
				segmentCalculationNames));

		// 2. Track Data — one row per LinkSet
		//    Stat columns: StatName_CalcName (e.g. LinkSetMean_Area)
		linkSetSheet = workbook.addSheet("Track Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "LinkSet"},
				linkSetCalculationNames,
				statCalcHeaders(linkSetStatisticNames, segmentStatisticCalculationNames)));

		// 3. Frame Data — one row per frame
		//    Stat columns: StatName_CalcName (e.g. FrameSetMean_Area)
		frameSetSheet = workbook.addSheet("Frame Data",
			wideHeaders(
				new String[]{"DataSet", "Method", "Frame"},
				frameSetCalculationNames,
				statCalcHeaders(frameSetStatisticNames, segmentStatisticCalculationNames)));
	}


	/**
	 * Configure all segment, LinkSet, and FrameSet calculations for execution.
	 * Assigns calculation objects to their respective data structures and sets up target image stack.
	 * Called during workflow to prepare calculations before results are retrieved.
	 *
	 * Name arrays are already populated by {@link #defineSheets()} — this method
	 * only wires fresh calculation instances to their data objects.
	 */
	public void setCalculations() {
		for (DataSet dataSet : outputDataSets) {

			progressBar.setString("Planning Calculations");
			progressBar.setMaximum(dataSet.getLinkSetList().size());
			progressBar.setValue(0);
			int progress = 0;

			for (LinkSet linkSet : dataSet.getLinkSetList()) {

				// ── Segment calculations ──────────────────────────────────────────
				for (Segment segment : linkSet) {
					SegmentCalculation[] segCalcs = segmentCalculations();
					if (segCalcs != null) {
						for (SegmentCalculation segCalc : segCalcs) {
							segCalc.setTargetStackSlice(stack, segment.getFrame() + 1);
						}
						for (SegmentCalculation calc : segCalcs) {
							segment.setCalculation(calc);
							calc.setSegment(segment);
						}
					}
				}

				// ── LinkSet calculations ──────────────────────────────────────────
				LinkSetCalculation[] linkCalcs = linkSetCalculations();
				if (linkCalcs != null) {
					for (LinkSetCalculation calc : linkCalcs) {
						linkSet.setCalculation(calc);
						calc.setLinkSet(linkSet);
					}
				}

				// ── LinkSet statistics ────────────────────────────────────────────
				LinkSetStatistic[] linkStats = linkSetStatistics();
				if (linkStats != null) {
					for (LinkSetStatistic stat : linkStats) {
						linkSet.setStatistic(stat);
						stat.setLinkSet(linkSet);
					}
				}

				progress++;
				progressBar.setValue(progress);
			}

			// ── FrameSet calculations and statistics ──────────────────────────────
			for (FrameSet frameSet : dataSet.getFrameSetList()) {
				if (frameSet == null) continue; // frames with no segments leave a null slot

				FrameSetCalculation[] frameCalcs = frameSetCalculations();
				if (frameCalcs != null) {
					for (FrameSetCalculation calc : frameCalcs) {
						frameSet.setCalculation(calc);
						calc.setFrameSet(frameSet);
					}
				}

				FrameSetStatistic[] frameStats = frameSetStatistics();
				if (frameStats != null) {
					for (FrameSetStatistic stat : frameStats) {
						frameSet.setStatistic(stat);
						stat.setFrameSet(frameSet);
					}
				}
			}

			//TODO: Set DataSet Calculations/Statistics
		}
	}


	/**
	 * Retrieve all calculation results and populate the result workbook with wide-format rows.
	 *
	 * <ul>
	 *   <li>Sheet 1 (<b>Segment Data</b>)  — one row per (LinkSet × segment)
	 *   <li>Sheet 2 (<b>Track Data</b>)    — one row per LinkSet
	 *   <li>Sheet 3 (<b>Frame Data</b>)    — one row per frame
	 * </ul>
	 */
	public void retrieveCalculations() {

		for (DataSet dataSet : outputDataSets) {

			// ── Sheet 1: Segment Data ─────────────────────────────────────────────
			progressBar.setString("Calculating Segment Data");
			progressBar.setMinimum(0);
			progressBar.setMaximum(dataSet.getLinkSetList().size());
			progressBar.setValue(0);
			int progress = 0;

			for (LinkSet linkSet : dataSet.getLinkSetList()) {
				for (Segment segment : linkSet) {
					List<Object> row = new ArrayList<>();
					row.add(dataSet.getName());
					row.add(methodName);
					row.add(linkSet.getDisplayName());
					row.add(segment.getFrame() + 1);  // 1-based frame number
					if (segmentCalculationNames != null) {
						for (String name : segmentCalculationNames) {
							row.add(segment.getCalculation(name));
						}
					}
					workbook.addLine(segmentModelSheet, row.toArray());
				}
				progress++;
				progressBar.setValue(progress);
			}

			// ── Sheet 2: Track Data ───────────────────────────────────────────────
			progressBar.setString("Calculating Track Data");
			progressBar.setValue(0);
			progress = 0;

			for (LinkSet linkSet : dataSet.getLinkSetList()) {
				List<Object> row = new ArrayList<>();
				row.add(dataSet.getName());
				row.add(methodName);
				row.add(linkSet.getDisplayName());

				// Direct LinkSet calculations (e.g. AreaDistribution)
				if (linkSetCalculationNames != null) {
					for (String name : linkSetCalculationNames) {
						row.add(linkSet.getCalculation(name));
					}
				}
				// Stat columns: one per (stat × statistic-eligible segment calc)
				if (linkSetStatisticNames != null && segmentStatisticCalculationNames != null) {
					for (String stat : linkSetStatisticNames) {
						for (String calc : segmentStatisticCalculationNames) {
							row.add(linkSet.getStatistic(stat, calc));
						}
					}
				}
				workbook.addLine(linkSetSheet, row.toArray());
				progress++;
				progressBar.setValue(progress);
			}

			// ── Sheet 3: Frame Data ───────────────────────────────────────────────
			progressBar.setString("Calculating Frame Data");
			progressBar.setMinimum(0);
			progressBar.setMaximum(dataSet.getFrameSetList().length);
			progressBar.setValue(0);
			progress = 0;

			for (FrameSet frameSet : dataSet.getFrameSetList()) {
				if (frameSet == null) continue; // frames with no segments leave a null slot
				List<Object> row = new ArrayList<>();
				row.add(dataSet.getName());
				row.add(methodName);
				row.add(frameSet.getFrame() + 1);  // 1-based frame number

				// Direct FrameSet calculations
				if (frameSetCalculationNames != null) {
					for (String name : frameSetCalculationNames) {
						row.add(frameSet.getCalculation(name));
					}
				}
				// Stat columns: one per (stat × statistic-eligible segment calc)
				if (frameSetStatisticNames != null && segmentStatisticCalculationNames != null) {
					for (String stat : frameSetStatisticNames) {
						for (String calc : segmentStatisticCalculationNames) {
							row.add(frameSet.getStatistic(stat, calc));
						}
					}
				}
				workbook.addLine(frameSetSheet, row.toArray());
				progress++;
				progressBar.setValue(progress);
			}
		}
	}


	/**
	 * Convert output datasets to overlay visualizations on the target image.
	 * Creates ROIs for all segments and adds them to the overlay with appropriate colors and labels.
	 *
	 * @param overlay the Overlay to populate with segment ROIs
	 * @param manager RoiManager for managing ROI additions
	 */
	void dataSetToOverlay(Overlay overlay, RoiManager manager) {

		for (DataSet dataSet : outputDataSets) {

			progressBar.setString("Generating Overlay");
			progressBar.setMinimum(0);
			progressBar.setMaximum(dataSet.getFrameSetList().length);
			progressBar.setValue(0);
			int progress = 0;

			Segment segment;
			//TODO: show cell linkage (here only segmentation)
			for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
				for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
					segment = dataSet.getFrameSet(i).get(j);
					Roi roi = getOverlayParameter(segment);
					roi.setStrokeColor(getColor(segment));
					roi.setFillColor(getColor(segment));
					roi.setPosition(segment.getFrame() + 1);
					roi.setStrokeWidth(2);
					manager.add(target, roi, segment.getFrame() + 1);
					overlay.add(roi, "Set:" + dataSet.getName() + ", Seg #" + segment.getLinkSet().getDisplayName());
				}
				progress++;
				progressBar.setValue(progress);
			}
		}
	}


	/**
	 * Collect all Data objects from this analysis method.
	 * Returns combined array of segment, linkset, and frameset calculations/statistics.
	 * //TODO —
	 *
	 * @return array of all Data calculation objects
	 */
	//TODO —
	public Data[] getCalculations() {

		Data[][] dataArrays = new Data[][] {
			segmentCalculations(),
			linkSetCalculations(),
			linkSetStatistics(),
			frameSetCalculations(),
			frameSetStatistics()
		};

		int length = 0;
		for (Data[] array : dataArrays) {
			if (array != null) length += array.length;
		}
		int k = 0;
		Data[] ar = new Data[length];
		for (Data[] array : dataArrays) {
			if (array == null) continue;
			for (Data data : array) {
				ar[k] = data;
				k++;
			}
		}
		return ar;
	}


	// ── Private helpers ───────────────────────────────────────────────────────

	/**
	 * Extracts {@link Data#getName()} from every element of a Data array.
	 * Returns an empty array (never null) if the input is null or empty.
	 */
	private String[] names(Data[] calcs) {
		if (calcs == null || calcs.length == 0) return new String[0];
		String[] out = new String[calcs.length];
		for (int i = 0; i < calcs.length; i++) out[i] = calcs[i].getName();
		return out;
	}

	/**
	 * Concatenates a fixed prefix array with one or more additional name arrays
	 * into a single flat String[] suitable for {@link dataStructure.ResultWorkbook#addSheet}.
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
	 * Pattern: {@code statName + "_" + calcName} (e.g. "LinkSetMean_Area").
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


	// ── Abstract methods ──────────────────────────────────────────────────────

	/**
	 * Transform or manipulate input DataSets before analysis.
	 * Override to perform preprocessing, filtering, or merging operations.
	 *
	 * @param inputSets array of input DataSets
	 * @return array of processed DataSets for analysis
	 */
	abstract DataSet[] dataOperation(DataSet[] inputSets);

	/**
	 * Extract visualization ROI from a single segment.
	 * Used to create overlay visualization of the segment boundary.
	 *
	 * @param segment the Segment to extract ROI for
	 * @return Roi representing the segment boundary
	 */
	abstract Roi getOverlayParameter(Segment segment);

	/**
	 * Returns segment-level calculation objects for this analysis.
	 *
	 * @return array of SegmentCalculation objects
	 */
	abstract SegmentCalculation[] segmentCalculations();

	/**
	 * Returns LinkSet-level calculation objects for this analysis.
	 *
	 * @return array of LinkSetCalculation objects
	 */
	abstract LinkSetCalculation[] linkSetCalculations();

	/**
	 * Returns LinkSet-level statistic objects for this analysis.
	 *
	 * @return array of LinkSetStatistic objects
	 */
	abstract LinkSetStatistic[] linkSetStatistics();

	/**
	 * Returns FrameSet-level calculation objects for this analysis.
	 *
	 * @return array of FrameSetCalculation objects
	 */
	abstract FrameSetCalculation[] frameSetCalculations();

	/**
	 * Returns FrameSet-level statistic objects for this analysis.
	 *
	 * @return array of FrameSetStatistic objects
	 */
	abstract FrameSetStatistic[] frameSetStatistics();

}
