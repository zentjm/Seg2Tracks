package analysisMethod;

import java.awt.Color;
import java.util.ArrayList;

import calculations.Data;
import calculations.SegmentCalculation;
import pairedDataStructure.PairedDataSet;
import pairedDataStructure.PairedList;
import pairedDataStructure.PairedSegment;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import pairedSegmentCalculations.PairedDataCalculation;
import pairedSegmentCalculations.PairedListCalculation;
import pairedSegmentCalculations.PairedSegmentCalculation;

/**
 * Abstract base class for comparative analysis of two DataSets.
 * Enables side-by-side comparison of LinkSets, FrameSets, and Segments from different conditions
 * using paired data structures and comparative metrics (Jaccard, Dice, Overlap indices).
 */
public abstract class CompareMethod extends AnalysisMethod {

	//TODO: NEEDS TO BE ABLE TO HANDLE COMBINATIONS OF MORE THAN JUST TWO DATASETS
	PairedDataSet pairedData;
	boolean sortByLink;

	int pairedSegmentSheet;
	int pairedLinkSheet;
	int pairedDataSheet;

	/**
	 * Convert input DataSets to paired data structure for comparison.
	 * Aligns segments from different DataSets for pairwise analysis.
	 *
	 * @param inputSets array of DataSets to pair and compare
	 */
	abstract void calculatePairedData(DataSet[] inputSets) ;

	/**
	 * Returns array of paired segment calculation objects.
	 *
	 * @return array of PairedSegmentCalculation
	 */
	abstract PairedSegmentCalculation[] pairedSegmentCalculations();

	/**
	 * Returns array of paired list (LinkSet/FrameSet) calculation objects.
	 *
	 * @return array of PairedListCalculation
	 */
	abstract PairedListCalculation[] pairedListCalculations();

	/**
	 * Extract overlay ROI(s) from a paired segment pair for visualization.
	 *
	 * @param segment the PairedSegment to extract ROIs for
	 * @return array of Roi objects (may contain multiple ROIs per pair)
	 */
	abstract Roi[] getOverlayParameter(PairedSegment segment);

	/**
	 * Initialize comparison method settings.
	 */
	void initialize() {
	}

	/**
	 * Execute the comparison analysis workflow.
	 * Converts input datasets to paired data structure ready for calculations.
	 */
	public void analyze() {
		pairedData = new PairedDataSet();
		calculatePairedData(dataSets);
	}

	/**
	 * Define spreadsheet sheets for comparative results.
	 * //TODO: Have it only add new sheets if they do not exist, otherwise just add to them
	 */
	//TODO: Have it only add new sheets if they do not exist, otherwise just add to them
	//default sheet definition
	void defineSheets() {
		pairedSegmentSheet = workbook.addSheet("Segment Comparison",
				new String[] {"DataSet1","DataSet2", "Analysis Method", "PairedList", "Frame", "Calculation", "Value"}
		);
		pairedLinkSheet = workbook.addSheet("Aggregated List Data",
				new String[] {"DataSet1","DataSet2", "Analysis Method", "PairedList", " List Calculation", "Segment Calculation", "Value"}
		);

		pairedDataSheet = workbook.addSheet("Aggregated Data",
				new String[] {"Analysis Method", "Calculation", "Value"}
		);
	}

	/**
	 * Configure paired segment and list calculations for execution.
	 * Assigns calculations to each paired segment and list in the pairedData structure.
	 */
	//dataSet calculation setter
	public void setCalculations() {


		//System.out.println("Set calculations");

		for (PairedList list : pairedData) {

			if (list == null) {
				//System.out.println("Detected null List");
			}



			for (PairedSegment segment : list.getList()) {

				//System.out.println("Running segment");

				if (segment== null) {
					//System.out.println("Detected null sgement");
					continue;

				}
				if (segment.getSeg1() == null || segment.getSeg2() == null) {
					//System.out.println("Detected null segement subsegment");
					continue;
				}
				//System.out.println("Segments are present");



				PairedSegmentCalculation[] calcs = pairedSegmentCalculations();
				for (PairedSegmentCalculation calc: calcs) {

					//System.out.println("Step 1");

					segment.setCalculation(calc);

					//System.out.println("Step 2");

					calc.setSegments(segment);

					//System.out.println("Step 3");

				}
			}
			//System.out.println("Calculating list");
			PairedListCalculation[] listCalcs = pairedListCalculations();
			for (PairedListCalculation calc: listCalcs) {
				list.setCalculation(calc);
				calc.setList(list);
			}
		}
	}


	/**
	 * Retrieve paired calculation results and populate the workbook.
	 * Iterates through all paired segments and lists, collecting comparison metrics.
	 */
	//dataSet calculation getter
	public void retrieveCalculations() {

		//System.out.println("Retrieve calculations XX");

		progressBar.setMinimum(0);
		progressBar.setMaximum(pairedData.size());
		progressBar.setValue(0);

		int n = 0;
		for (PairedList list : pairedData) {

			n++;
			//System.out.println("List " + n);

			PairedSegmentCalculation[] calcs = pairedSegmentCalculations(); //TODO: get the list of names in the calculation setter

			//System.out.println("List ran " + n);

			String data1 = "None";
			String data2 = "None";
			if (list.hasSet1()) data1 = list.getDataSet1().getName();
			if (list.hasSet2()) data2 = list.getDataSet2().getName();

			//PairedSegmentCalculation[] calcs = pairedSegmentCalculations();
			for (PairedSegment segment : list.getList()) {
				if (segment == null ) continue; //do not analyze if the segment does not exist
				if (segment.getSeg1() == null && segment.getSeg2() == null) continue;

				//System.out.println("Segment running");

				for (PairedSegmentCalculation calc : calcs) { //TODO: Can refine to get only specified calculations or include all underlying calculations as well
					Object[] segmentCalculations = new Object[] {
						data1, 									//dataset 1
						data2, 									//dataset 2
						methodName,								//method
						list.getName(),							//Pairedlist
						segment.getFrame() + 1,					//frame
						calc.getName(),							//calculation
						segment.getCalculation(calc.getName()) //calculation value
					};
					workbook.addLine(pairedSegmentSheet, segmentCalculations);
				}
			}

			//System.out.println("Segment calculations retrieved");

			PairedListCalculation[] listCalcs = pairedListCalculations();
			for (PairedListCalculation listCalc : listCalcs) {
				for (PairedSegmentCalculation calc : calcs) { //TODO: Want to change this for all calculations that are calc-based
					Object[] listCalculations = new Object[] {
						data1, 									//dataset 1
						data2, 									//dataset 2
						methodName,								//method
						list.getName(),							//Pairedlist
						listCalc.getName(),						//list calculation
						calc.getName(),							//segment calculation
						list.getCalculation(listCalc.getName(), calc) //calculation value
					};
					workbook.addLine(pairedLinkSheet, listCalculations);
				}
			}
			progressBar.setValue(progressBar.getValue() + 1);
		}

		//System.out.println("Calculations retrieved");
	}


	/**
	 * Convert paired data structures to overlay visualizations for comparison display.
	 * Generates ROIs for both segments in each pair, colored distinctly for easy comparison.
	 *
	 * @param overlay the Overlay to populate with comparison ROIs
	 * @param manager RoiManager for managing ROI additions
	 */
	void dataSetToOverlay(Overlay overlay, RoiManager manager) {
		//Iterate pairedLists

		//System.out.println("DataToOverlay");

		//TODO: show cell linkage (here only segmentation)
		for (PairedList list : pairedData) {


			//XXX: TESTING:
			//System.out.println("\nThis list: " + list.getName());
			for (int i = 0; i < list.getList().length; i ++) {

				if (list.get(i) == null) continue; //Do not analyze if the segment does not exist.

				//System.out.println("This SegmentPair Frame: " + list.get(i).getFrame());
				if (list.get(i).getSeg1() != null) System.out.println("Has Segment 1");
				else System.out.println("NO Segment 1");
				if (list.get(i).getSeg2() != null) System.out.println("Has Segment 2");
				else System.out.println("NO Segment 2");
			}


			for (PairedSegment pair : list.getList()) {

				if (pair == null ) continue;
				if (pair.getSeg1() == null && pair.getSeg2() == null) continue;

				//System.out.println("Getting paired segment");

				Roi[] rois = getOverlayParameter(pair);

				for (Roi roi : rois) {
					roi.setPosition(pair.getFrame() + 1);
					roi.setStrokeWidth(2);
					manager.add(target, roi, pair.getFrame() + 1);
					overlay.add(roi, "Pair #" + pair.getPairedList().getName() + " Seg:" + roi.getName());
				}
			}
		}
	}

	/**
	 * Returns array of Data calculation objects for this comparison analysis.
	 * //TODO: implement this.
	 *
	 * @return null (not yet implemented)
	 */
	public Data[] getCalculations() {
		return null; //TODO: implement this.
	}



}
