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
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

//This method allows operations on dataset, then hierarchical quantification
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
	
	
	
	//Takes an input DataSet[] and returns it with or without modifications
	public final void analyze() {
		outputDataSets = dataOperation(dataSets);	
	}
	
	//TODO: Have it only add new sheets if they do not exist, otherwise just add to them
	//default sheet definition
	void defineSheets() {
		segmentModelSheet = workbook.addSheet(segmentSheetName, 
				new String[] {"DataSet", "Analysis Method", "LinkSet", "Frame", "Calculation", "Value"});
		frameSetSheet = workbook.addSheet(frameSetSheetName,
				new String[] {"DataSet", "Analysis Method", "FrameSet", " FrameSet Calculation", "Segment Calculation", "Value"});
		linkSetSheet = workbook.addSheet(linkSetSheetName,
				new String[] {"DataSet", "Analysis Method", "LinkSet","LinkSet Calculation", "Segment Calculation", "Value"});
	}


	//dataSet calculation setter
	public void setCalculations() {
		for(DataSet dataSet : outputDataSets) {
			for (LinkSet linkSet : dataSet.getLinkSetList()) {
				
				//Set segment calculations
				for (Segment segment : linkSet) {
					SegmentCalculation[] segCalcs = segmentCalculations();
					if (segCalcs != null) {
						for (SegmentCalculation segCalc: segCalcs) {
							segCalc.setTargetProcessor(stack.getProcessor(segment.getFrame() + 1));
						}
						if (segmentCalculationNames == null) {
							segmentCalculationNames = new String[segCalcs.length];
							for (int i = 0; i < segCalcs.length; i ++) {
								segmentCalculationNames[i] = segCalcs[i].getName();
							}
						}
						for (SegmentCalculation calc: segCalcs) {
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
	
	
	//dataSet calculation getter
	public void retrieveCalculations() {
		for(DataSet dataSet : outputDataSets) {
			for (LinkSet linkSet : dataSet.getLinkSetList()) {
				for (Segment segment : linkSet) {
					//SegmentCalculation[] calcs = segmentCalculations(); //TODO: this necessary?
					if (segmentCalculationNames != null) {
						for (String name: segmentCalculationNames) {
						//for (SegmentCalculation calc : calcs) { //TODO: Can refine to get only specified calculations or include all underlying calculations as well.
							Object[] segmentCalculations = new Object[] {
								dataSet.getName(), 			//dataset
								methodName,					//method		
								linkSet.getName(),			//linkSet
								segment.getFrame() + 1,		//frame
								name, //calc.getName(),
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
							linkSet.getName(),				//linkSet
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
								linkSet.getName(),				//linkSet
								statistic, 							//calculation name
								calculation,
								linkSet.getStatistic(statistic, calculation)	//calculation
							};
							workbook.addLine(linkSetSheet, linkSetStatistics);
						}
					}
				}
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
	
	
	void dataSetToOverlay(Overlay overlay, RoiManager manager) {
		//Iterate dataSets
		for (DataSet dataSet: outputDataSets) {
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
					overlay.add(roi, "Set:" + dataSet.getName() + ", Seg #" + segment.getLinkSet().getName());
				}
			}
		}
	}
	
	
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
	
	//Performs operations on DataSets
	abstract DataSet[] dataOperation(DataSet[] inputSets); 
	
	//Defines returned Overlay
	abstract Roi getOverlayParameter(Segment segment);
	
	//Calculates workbook data
	abstract SegmentCalculation[] segmentCalculations();
	abstract LinkSetCalculation[] linkSetCalculations();
	abstract LinkSetStatistic[] linkSetStatistics();
	abstract FrameSetCalculation[] frameSetCalculations();
	abstract FrameSetStatistic[] frameSetStatistics();
	
	
	
	
	
	
	
	
}
