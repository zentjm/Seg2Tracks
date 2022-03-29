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

//This method allows vis-a-vis comparison of LinkSets, FrameSets, and SegmentModels from different DataSets. 
public abstract class CompareMethod extends AnalysisMethod {

	//TODO: NEEDS TO BE ABLE TO HANDLE COMBINATIONS OF MORE THAN JUST TWO DATASETS
	PairedDataSet pairedData;
	boolean sortByLink;
	
	int pairedSegmentSheet;
	int pairedLinkSheet;
	int pairedDataSheet;
	
	
	//Abstract classes
	abstract void calculatePairedData(DataSet[] inputSets) ; //converts to Paired data
	abstract PairedSegmentCalculation[] pairedSegmentCalculations();
	abstract PairedListCalculation[] pairedListCalculations();
	abstract Roi[] getOverlayParameter(PairedSegment segment);
	
	void initialize() {
	}
	
	public void analyze() {
		pairedData = new PairedDataSet();
		calculatePairedData(dataSets);
	}

	//TODO: Have it only add new sheets if they do not exist, otherwise just add to them
	//default sheet definition
	void defineSheets() {
		pairedSegmentSheet = workbook.addSheet("Segment Comparison", 
				new String[] {"DataSet1","DataSet2", "Analysis Method", "PairedList", "Frame", "Calculation", "Value"}
		);
		pairedLinkSheet = workbook.addSheet("Aggregated List Data", 
				new String[] {"DataSet1","DataSet2", "Analysis Method", "PairedList", " List Calculation","Segment Calculation", "Value"}
		);
		
		pairedDataSheet = workbook.addSheet("Aggregated Data", 
				new String[] {"Analysis Method", "Calculation", "Value"}
		);
		
	}

	//dataSet calculation setter
	public void setCalculations() {
		
		for (PairedList list : pairedData) {
			
			for (PairedSegment segment : list.getList()) {
				PairedSegmentCalculation[] calcs = pairedSegmentCalculations();
				for (PairedSegmentCalculation calc: calcs) {
					segment.setCalculation(calc);
					calc.setSegments(segment);
				}
			}
			
			PairedListCalculation[] listCalcs = pairedListCalculations();
			for (PairedListCalculation calc: listCalcs) {
				list.setCalculation(calc);
				calc.setList(list);
			}
		}
		
		
	}
	
				
	//dataSet calculation getter
	public void retrieveCalculations() {
		for (PairedList list : pairedData) {
			
			PairedSegmentCalculation[] calcs = pairedSegmentCalculations(); //TODO: get the list of names in the calculation setter
			
			String data1 = "None";
			String data2 = "None";
			if (list.hasSet1()) data1 = list.getDataSet1().getName();
			if (list.hasSet2()) data2 = list.getDataSet2().getName();
			
			//PairedSegmentCalculation[] calcs = pairedSegmentCalculations();
			for (PairedSegment segment : list.getList()) {
				if (segment.getSeg1() == null & segment.getSeg2() == null) continue;
				
				for (PairedSegmentCalculation calc : calcs) { //TODO: Can refine to get only specified calculations or include all underlying calculations as well.
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
		}
	}
	
	
	void dataSetToOverlay(Overlay overlay, RoiManager manager) {
		//Iterate pairedLists

	
		//TODO: show cell linkage (here only segmentation)
		for (PairedList list : pairedData) { 
			
			/*
			//XXX: TESTING:
			System.out.println("\nThis list: " + list.getName());
			for (int i = 0; i < list.getList().length; i ++) {
				System.out.println("This SegmentPair Frame: " + list.get(i).getFrame());
				if (list.get(i).getSeg1() != null) System.out.println("Has Segment 1");
				else System.out.println("NO Segment 1");
				if (list.get(i).getSeg2() != null) System.out.println("Has Segment 2");
				else System.out.println("NO Segment 2");
			}
			*/
			
			
			
			for (PairedSegment pair : list.getList()) { 
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
	
	public Data[] getCalculations() {
		return null; //TODO: implement this. 
	}
	

	
}
