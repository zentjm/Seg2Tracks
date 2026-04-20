package analysisMethod;

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
 * Handles sheet management, calculation setup, result retrieval, and overlay generation.
 */
public abstract class OperationMethod extends AnalysisMethod {

	int segmentModelSheet;
	int frameSetSheet;
	int linkSetSheet;

	String segmentCalculationNames[];
	String linkSetCalculationNames[];
	String linkSetStatisticNames[];
	String frameSetCalculationNames[];
	String frameSetStatisticNames[];

	String segmentSheetName = "SegmentModel Calculations";
	String linkSetSheetName = "LinkSet Calculations";
	String frameSetSheetName = "FrameSet Calculations";


	/**
	 * Execute the primary analysis workflow.
	 * Transforms input datasets using dataOperation() and stores as outputDataSets.
	 */
	//Takes an input DataSet[] and returns it with or without modifications
	public final void analyze() {
		outputDataSets = dataOperation(dataSets);
	}

	/**
	 * Define result workbook sheets with appropriate column headers.
	 * //TODO: Have it only add new sheets if they do not exist, otherwise just add to them
	 * Creates three standard sheets: SegmentModel, LinkSet, and FrameSet calculations.
	 */
	//TODO: Have it only add new sheets if they do not exist, otherwise just add to them
	//default sheet definition
	void defineSheets() {
		segmentModelSheet = workbook.addSheet(segmentSheetName,
				new String[] {"DataSet", "Analysis Method", "LinkSet", "Frame", "Calculation", "Value"});
		frameSetSheet = workbook.addSheet(frameSetSheetName,
				new String[] {"DataSet", "Analysis Method", "FrameSet", " FrameSet Calculation", "Segment Calculation", "Value"});
		linkSetSheet = workbook.addSheet(linkSetSheetName,
				new String[] {"DataSet", "Analysis Method", "LinkSet", "LinkSet Calculation", "Segment Calculation", "Value"});
	}



	/**
	 * Configure all segment, LinkSet, and FrameSet calculations for execution.
	 * Assigns calculation objects to their respective data structures and sets up target image stack.
	 * Called during workflow to prepare calculations before results are retrieved.
	 */
	//dataSet calculation setter
	public void setCalculations() {
		for(DataSet dataSet : outputDataSets) {

			progressBar.setString("Planning Calculations");
			progressBar.setMaximum(dataSet.getLinkSetList().size());
			progressBar.setValue(0);
			int progress = 0;

			for (LinkSet linkSet : dataSet.getLinkSetList()) {

				//Set segment calculations
				int segmentCount = 0;
				for (Segment segment : linkSet) {
					segmentCount ++;
					//System.out.println("Linkset " + linkSet.getDisplayName() +" Segment: " + segmentCount);
					SegmentCalculation[] segCalcs = segmentCalculations();
					if (segCalcs != null) {
						for (SegmentCalculation segCalc: segCalcs) {
							segCalc.setTargetStackSlice(stack, segment.getFrame() + 1);
						}
						if (segmentCalculationNames == null) {
							segmentCalculationNames = new String[segCalcs.length];
							for (int i = 0; i < segCalcs.length; i ++) {
								segmentCalculationNames[i] = segCalcs[i].getName();
							}
						}
						for (SegmentCalculation calc: segCalcs) {
							//System.out.println("Segment calculation: " + segment.getName());
							segment.setCalculation(calc);
							calc.setSegments(segment);
						}
					}
				}

				//Set LinkSet Calculations/Statistics
				LinkSetCalculation[] linkCalcs = linkSetCalculations();
				if (linkCalcs != null) {
					if (linkSetCalculationNames == null) {
						linkSetCalculationNames = new String[linkCalcs.length];
						for (int i = 0; i < linkCalcs.length; i ++) {
							linkSetCalculationNames[i] = linkCalcs[i].getName();
						}
					}
					for (LinkSetCalculation calc: linkCalcs) {
						linkSet.setCalculation(calc);
						calc.setLinkSet(linkSet);
					}
				}

				LinkSetStatistic[] linkStats = linkSetStatistics();
				if (linkStats != null) {
					if (linkSetStatisticNames == null) {
						linkSetStatisticNames = new String[linkStats.length];
						for (int i = 0; i < linkStats.length; i ++) {
							linkSetStatisticNames[i] = linkStats[i].getName();
						}
					}
					for (LinkSetStatistic stat : linkStats) {
						linkSet.setStatistic(stat);
						stat.setLinkSet(linkSet);
					}
				}
				progress ++;
				progressBar.setValue(progress);
			}

			//Set FrameSet Calculations/Statistics
			for (FrameSet frameSet : dataSet.getFrameSetList()) {

				FrameSetCalculation[] frameCalcs = frameSetCalculations();
				if (frameCalcs != null) {
					if (frameSetCalculationNames == null) {
						frameSetCalculationNames = new String[frameCalcs.length];
						for (int i = 0; i < frameCalcs.length; i ++) {
							frameSetCalculationNames[i] = frameCalcs[i].getName();
						}
					}
					for (FrameSetCalculation calc: frameCalcs) {
						frameSet.setCalculation(calc);
						calc.setFrameSet(frameSet);
					}
				}

				FrameSetStatistic[] frameStats = frameSetStatistics();
				if (frameStats != null) {
					if (frameSetStatisticNames == null) {
						frameSetStatisticNames = new String[frameStats.length];
						for (int i = 0; i < frameStats.length; i ++) {
							frameSetStatisticNames[i] = frameStats[i].getName();
						}
					}
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
	 * Retrieve all calculation results and populate the result workbook.
	 * Iterates through all datasets, linksets, segments, and framesets, collecting computed metrics.
	 */
	//dataSet calculation getter
	public void retrieveCalculations() {


		for(DataSet dataSet : outputDataSets) {

			progressBar.setString("Calculating Functions");
			progressBar.setMinimum(0);
			progressBar.setMaximum(dataSet.getLinkSetList().size());
			progressBar.setValue(0);
			int progress = 0;

			for (LinkSet linkSet : dataSet.getLinkSetList()) {
				for (Segment segment : linkSet) {
					//SegmentCalculation[] calcs = segmentCalculations(); //TODO: this necessary?
					if (segmentCalculationNames != null) {
						for (String name: segmentCalculationNames) {
						//for (SegmentCalculation calc : calcs) { //TODO: Can refine to get only specified calculations or include all underlying calculations as well

							Object[] segmentCalculations = new Object[] {
								dataSet.getName(), 			//dataset
								methodName,					//method
								linkSet.getDisplayName(),			//linkSet
								segment.getFrame() + 1,		//frame
								name,
								segment.getCalculation(name)
							};
							workbook.addLine(segmentModelSheet, segmentCalculations);
						}
					}
				}
				if (linkSetCalculationNames != null) {
					for (String name: linkSetCalculationNames) {
						Object[] linkSetCalculations = new Object[] {
							dataSet.getName(), 				//dataset
							methodName,						//method
							linkSet.getDisplayName(),				//linkSet
							name,
							"N/A",
							linkSet.getCalculation(name)	//calculation
						};
						workbook.addLine(linkSetSheet, linkSetCalculations);
					}
				}
				if (linkSetStatisticNames != null) {
					for (String statistic: linkSetStatisticNames) {
						for (String calculation: segmentCalculationNames) {
							//TODO: if calculation is a statistic
							Object[] linkSetStatistics = new Object[] {
								dataSet.getName(), 				//dataset
								methodName,						//method
								linkSet.getDisplayName(),				//linkSet
								statistic, 							//calculation name
								calculation,
								linkSet.getStatistic(statistic, calculation)	//calculation
							};
							workbook.addLine(linkSetSheet, linkSetStatistics);
						}
					}
				}
				progress ++;
				progressBar.setValue(progress);
				//System.out.println("Progress is: " + progress + "/" + dataSet.getLinkSetList().size());

				/*TEST
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/


			}

			for (FrameSet frameSet : dataSet.getFrameSetList()) {

				if (frameSetCalculationNames != null) {
					for (String name: frameSetCalculationNames) {
						Object[] frameSetCalculations = new Object[] {
							dataSet.getName(), 				//dataset
							methodName,						//method
							frameSet.getFrame(),				//frame
							name,
							"N/A",
							frameSet.getCalculation(name)	//calculation
						};
						workbook.addLine(frameSetSheet, frameSetCalculations);
					}
				}

				if (frameSetStatisticNames != null) {
					for (String statistic: frameSetStatisticNames) {
						for (String calculation: segmentCalculationNames) {
							//TODO: if calculation is a statistic
							Object[] frameSetStatistics = new Object[] {
								dataSet.getName(), 				//dataset
								methodName,						//method
								frameSet.getFrame(),				//linkSet
								statistic, 							//calculation name
								calculation,
								frameSet.getStatistic(statistic, calculation)	//calculation
							};
							workbook.addLine(frameSetSheet, frameSetStatistics);
						}
					}		//TODO: get
				}

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

		//Iterate dataSets
		for (DataSet dataSet: outputDataSets) {

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
					//if (roi.getContainedPoints().length < 0) continue; //TODO: does this do anything?
					roi.setStrokeColor(getColor(segment));
					roi.setFillColor(getColor(segment));
					roi.setPosition(segment.getFrame() + 1);
					roi.setStrokeWidth(2);
					manager.add(target, roi, segment.getFrame() + 1);
					overlay.add(roi, "Set:" + dataSet.getName() + ", Seg #" + segment.getLinkSet().getDisplayName());
				}

				progress ++;
				progressBar.setValue(progress);
			}
		}
	}


	/**
	 * Collect all Data objects from this analysis method.
	 * Returns combined array of segment, linkset, and frameset calculations/statistics.
	 * //TODO --
	 *
	 * @return array of all Data calculation objects
	 */
	//TODO --
	public Data[] getCalculations() {

		Data[][] dataArrays = new Data[][] {
			segmentCalculations(),
			linkSetCalculations(),
			linkSetStatistics(),
			frameSetCalculations(),
			frameSetStatistics()
		};

		int length = 0;
		for (Data [] array: dataArrays) {
			if (array!= null) length += array.length;
		}
		int k = 0;
		Data [] ar = new Data [length];
		for (Data [] array: dataArrays) {
			if (array == null) continue;
			for (Data data : array) {
				//System.out.println("Returning Calculation: " + data);
				ar[k] = data;
				k++;
			}
		}

		return ar;


			//TODO:
			//Data[] linkCalcs = linkSetCalculations();
			//Data[] linkStats = linkSetStatistics();
			//Data[] frameCalcs = frameSetCalculations();
			//Data[] framceStats = frameSetStatistics();
	}



	//ABSTRACT CLASSES

	/**
	 * Transform or manipulate input DataSets before analysis.
	 * Override to perform preprocessing, filtering, or merging operations.
	 *
	 * @param inputSets array of input DataSets
	 * @return array of processed DataSets for analysis
	 */
	//Performs operations on DataSets
	abstract DataSet[] dataOperation(DataSet[] inputSets);

	/**
	 * Extract visualization ROI from a single segment.
	 * Used to create overlay visualization of the segment boundary.
	 *
	 * @param segment the Segment to extract ROI for
	 * @return Roi representing the segment boundary
	 */
	//Defines returned Overlay
	abstract Roi getOverlayParameter(Segment segment);

	/**
	 * Returns segment-level calculation objects for this analysis.
	 *
	 * @return array of SegmentCalculation objects
	 */
	//Calculates workbook data
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
