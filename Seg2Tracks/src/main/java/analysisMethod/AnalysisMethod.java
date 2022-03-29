package analysisMethod;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

import org.apache.poi.ss.usermodel.Workbook;

import calculations.Data;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.ResultWorkbook;
import dataStructure.Segment;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

public abstract class AnalysisMethod {

	String methodName;
	String description;
	String[] channels;
	int numberOfCalculations;
	boolean mergedCalculation;
	
	DataSet [] dataSets;
	DataSet [] outputDataSets;
	
	ImagePlus target;
	ImageStack stack;
	
	ResultWorkbook workbook;
	Object[][][][] data;
	boolean override;
	
	
	public AnalysisMethod() {
		methodName = "Method Name";
		description = "How this Method Works";
		channels = new String[] {"Empty"};
		numberOfCalculations = 1;
		mergedCalculation = false;
		workbook = new ResultWorkbook();
	}
	
	//TODO: needs to be able to take multiple segment models depending on number of channels used
	public void initialize(ImagePlus target, DataSet[] dataSets, boolean override) {
		this.target = target;
		this.stack = target.getImageStack();
		this.dataSets = dataSets;
		this.override = override;
	}
	
	public final String toString() {
		return methodName;
	}
	
	public final String getDescription() {
		return description;
	}
	
	//Keep to generating the workbook. 
	//TODO expand to allow multiple calculations per overlay
	public final Workbook getWorkbook() {
		
		//Sets the override
		workbook.setOverride(override);
		
		//Generates the defined Sheets
		defineSheets();
		
		//Sets the 
		setCalculations();
		
		//Basic calculations 
		retrieveCalculations();
		
		//Returns outcome
		return workbook.getWorkbook();
	}

	//Gets the overlays. Runs via "Generate Results"
	public final ImagePlus getOverlay() {
		
		//Specify particular overlay features //TODO: Make part of AnalysisModel settings - need to deal with color somehow
		Overlay overlay = new Overlay();
		overlay.drawNames(true);
		overlay.drawLabels(true);
		overlay.setLabelColor(Color.BLACK);
		overlay.drawBackgrounds(true);
		overlay.setLabelFont(new Font ("TimesRoman", Font.BOLD, 15)); //TODO: User control over these settings. 
	
		//Creates headless ROI manager for creating overlays
		RoiManager manager = RoiManager.getInstance();
		if (manager == null) manager = new RoiManager(false);
	
		//Get overlay from dataset
		dataSetToOverlay(overlay, manager);
		target.setOverlay(overlay);
		return target;
	}
	
	//Generates a polygonRoi from a list of points //TODO: maybe part of an auxiliary package of calculations?
	public final PolygonRoi getPolygonRoi(Point[] pointList) {
		float[] xPoints = new float[pointList.length];
		float[] yPoints = new float[pointList.length];
		for (int i = 0; i < pointList.length; i++) {
			xPoints[i] = pointList[i].x;
			yPoints[i] = pointList[i].y;
		}
		return new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
	}
	
		
	//basic color method
	public Color getColor() {
		Random random = new Random();
		int red = random.nextInt(256);
		int green = random.nextInt(256);
		int blue = random.nextInt(256);
		return new Color (red, green, blue);
	}
	
	//add a sheet
	void addSheet(String name, String[] headers) {
		workbook.addSheet(name, headers);
	}
	
	//Retrieves color with segment > linkset > dataSet. Default is DataSet Color. 
	//TODO: Probably should just set to one color unless otherwise required. 
	Color getColor (Segment segment) {
		if (segment.getColor() != null) return segment.getColor(); 
		if (segment.getLinkSet().getColor() != null) return segment.getLinkSet().getColor();
		if (segment.getLinkSet().getDataSet().getColor() != null) return segment.getLinkSet().getDataSet().getColor();
		return null; //Never happens
	}
	
	//Workbook classes
	public abstract String[] getChannels();
	public abstract Data[] getCalculations();
	abstract void defineSheets();
	abstract void setCalculations();
	abstract void retrieveCalculations();
	public abstract void analyze();
	abstract void dataSetToOverlay(Overlay overlay, RoiManager manager);
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//---------
	
	
	/*
	///XXX: TEMPORARY HACK: Only for segmentation comparer
	double [] calculation(SegmentModel seg1, SegmentModel seg2) {

		//Retrieve Shapes
		ShapeRoi seg1Shape = new ShapeRoi (getPolygonRoi(seg1.getInternalPerimeter()).getPolygon());
		ShapeRoi seg2Shape = new ShapeRoi (getPolygonRoi(seg2.getInternalPerimeter()).getPolygon());
		
		double shape1Size = (double) seg1Shape.getContainedPoints().length;
		double shape2Size = (double) seg2Shape.getContainedPoints().length;
		double min = Math.min(shape1Size, shape2Size);
		
		ShapeRoi overlapShape = (ShapeRoi) seg1Shape.clone();
		ShapeRoi unionShape = (ShapeRoi) seg1Shape.clone();
		overlapShape = overlapShape.and(seg2Shape);
		unionShape = unionShape.or(seg2Shape);

		System.out.println("Shape1Size: " + shape1Size + "  shape2Size: " + shape2Size);
		
		//Roi overlapShape = seg1Shape.or(seg2Shape);
		//calculations
		//Roi overlapShape = seg1Shape.and(seg2Shape);
		//Roi unionShape = seg1Shape.or(seg2Shape);//seg1Shape.or(seg2Shape);
		
		double overlap = (double) overlapShape.getContainedPoints().length;
		double union = (double) unionShape.getContainedPoints().length;
	
		System.out.println("Overlap1: " + overlap + "  Union1: " + union + "  Min:" + min);
	
		//Segmentation calculator
		double jaccard = overlap/union;
		double dice = (2 * overlap) / (shape1Size + shape2Size);
		double oCoeff = overlap / min;
		
		System.out.println("Jaccard: " + jaccard + "  Dice: " + dice + "  oCoeff: " + oCoeff);
		
		
		return new double [] {jaccard, dice, oCoeff, shape1Size, shape2Size};
	}
	*/
	
	
	
	
	
		
	
	/** Data Acquisition:
	 * 
	 * Core Delegation:
	 * 	1. Single-Channel Access: Nested sequential access to DataSet --> LinkSet/FrameSet --> SegmentModel for calculations
	 * 	2. Multi-Channel Access: Merging FrameSet data from different dataSets
	 * 		* All dataSets should have the same number of frameSets --> Merging frameSet information should be relatively easy. 
	 * 		* Scanning data 
	 *  
	 * 		- Define overlapping data
	 * 		- Define 
	 * 		
	 * 
	 *  2. Filtering:
	 *  	- Allow object removal based on threashold value
	 *  	- Create tab for threasholded-only values
	 *  	- Allow color reassignment based on threashold value
	 * 
	 * 
	 * 	2. Multi-Channel Calculations: 
	 * 
	 * Plugin Delegation:
	 *  1. Single-Channel Calculations:
	 *  	- Specific morphological/intensity calculations for the Segment Model
	 *  	- Specific parameters for the FrameSet
	 *  	- Specific parameters for the LinkSet
	 *  	- Specific parameters for the DataSet
	 *  2. Single-Channel Filtering:
	 *  	- Define filtering parameter and threshold
	 *  	- Define filtering effect: removal, color change
	 *  3. Multi-Channel Filtering
	 *  	- Define desired inter
	 *  
	 *  3. Multi-Channel Calculations
	 *  	-
	 *  
	 *  Methods Library: 
	 *   1. Static calculations for SegmentModels.
	 *   2. Static calculations for FrameSet
	 *   3. Static calculations for LinkSet
	 *   4. Static calculations for DataSets
	 *   5. Combo calcuations
	 *   
	 *  
	 *  2. MultiChanne
	 *  
	 *  
	 * 
	 *
	 *   
	 */
		
	

		/*
		public void assessFrameSet() {
			
		}
		*/
		
		// Access all dataSets 
		
		
		
		
		
		
		/** Data Acquisition:
		 * 
		 * Core Delegation:
		 * 	1. Single-Channel Access: Nested sequential access to DataSet --> LinkSet/FrameSet --> SegmentModel for calculations
		 * 	2. Multi-Channel Access: Merging FrameSet data from different dataSets
		 * 		* All dataSets should have the same number of frameSets --> Merging frameSet information should be relatively easy. 
		 * 		* Scanning data 
		 *  
		 * 		- Define overlapping data
		 * 		- Define 
		 * 		
		 * 
		 *  2. Filtering:
		 *  	- Allow object removal based on threashold value
		 *  	- Create tab for threasholded-only values
		 *  	- Allow color reassignment based on threashold value
		 * 
		 * 
		 * 	2. Multi-Channel Calculations: 
		 * 
		 * Plugin Delegation:
		 *  1. Single-Channel Calculations:
		 *  	- Specific morphological/intensity calculations for the Segment Model
		 *  	- Specific parameters for the FrameSet
		 *  	- Specific parameters for the LinkSet
		 *  	- Specific parameters for the DataSet
		 *  2. Single-Channel Filtering:
		 *  	- Define filtering parameter and threshold
		 *  	- Define filtering effect: removal, color change
		 *  3. Multi-Channel Filtering
		 *  	- Define desired inter
		 *  
		 *  3. Multi-Channel Calculations
		 *  	-
		 *  
		 *  Methods Library: 
		 *   1. Static calculations for SegmentModels.
		 *   2. Static calculations for FrameSet
		 *   3. Static calculations for LinkSet
		 *   4. Static calculations for DataSets
		 *   5. Combo calcuations
		 *   
		 *  
		 *  2. MultiChanne
		 *  
		 *  
		 * 
		 *
		 *   
		 */
		
		
		
		
		
		
		
		/**
		 * 1. FrameSet --> SegmentModel --> Data 
		 * INPUT: SegmentModel
		 * OUPTPUT: Fluorescent/Morphological data on that individual segment on target output
		 * 
		 * DESCRIPTION: Run through each dataSet in order. Access framsets sequentially, then access each dataSet and asses.
		 * Faster than linkSet method. 
		 * 
		 */
		
		
		/** DataSet --> LinkSet --> SegmentModel Path
		 * 2. Calculations access for individual LinkSets
		 * INPUT: LinkSet
		 * OUTPUT: Length and variance parameters for Segment Models. 
		 *   1. Interactivity of the dataSets
		 *   
		 */
		
		
		
		
		
		
		
		
		/**
		 * 2a. Calculations for Segment interaction
		 * INPUT: FrameSet
		 * OUTPUT: Interaction data, interaction of specific 
		 *   1. Interactivity of the DataSet
		 * 
		 */
		
		/**
		 * 2b. Calculations for Segment interaction
		 * INPUT: Multiple FrameSets
		 * OUTPUT: Interaction data, interaction of specific 
		 *   1. Interactivity of the DataSet
		 * 
		 */
		
		/**
		 * 3. Calculations for LinkSets
		 * INPUT: Single LinkSet
		 * OUTPUT: Compiled Intensity-Morphology data on the LinkSet
		 * OUTPUT: LinkSet Interactivity
		 */
		
		/**
		 * 3a. Calculations for LinkSet associations.
		 * INPUT: Compare LinkSet associations
		 * OUTPUT: Compiled Intensity-Morphology data on the LinkSet
		 * OUTPUT: LinkSet Interactivity
		 */
		
		
		/**
		 * 4. Calculations for FrameSets
		 * INPUT: Single FrameSet
		 * OUTPUT: Compiled Intensity-Morphology data on the LinkSet
		 * OUTPUT: Spatial distribution data on LinkSet
		 */


	
	/*
	//TEST for area
	public final void testArea() {
		ImagePlus target2 = target.duplicate();
		ImageProcessor tempProc;
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) { 
			target2.setSlice(i);
			tempProc = target2.getProcessor();
			for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) { 
				SegmentModel segment = dataSet.getFrameSet(i).get(j);
				Point[] pts = getAreaPts(segment);
				for (Point pt: pts) {
					tempProc.set(pt.x, pt.y, 60000);
				}
			}
		}
		//target2.show();
	}
	
	
	public Point[] getAreaPts(SegmentModel segment) {
		return null;
	}
	*/
	
	/*
	//XXX:Temporary Hack
	if (mergedCalculation) {
		for (int i = 0; i < dataSets[0].getLinkSetList().size(); i++) { 
			for (int j = 0; j < dataSets[0].getLinkSet(i).size(); j++) { 
				SegmentModel seg1 = dataSets[0].getLinkSet(i).get(j);
				SegmentModel seg2 = dataSets[1].getLinkSet(i).get(j); //TODO: No good, requires alignment. 
				double [] calculations = calculation(seg1, seg2);
				for (int k = 0; k < calculations.length; k ++) {
					output.addLine(seg1.getLinkSet().getDataSet().getName(),
							name, seg1.getLinkSet().getName(), seg1.getFrame() + 1, 
							calculationNames[k], calculations[k]); //TODO: dataSet name
				}
			}
		}
		return output.getWorkbook();
	}
	
	if (!mergedCalculation) {
		for (DataSet dataSet : dataSets) {
			for (int i = 0; i < dataSet.getFrameSetList().length; i++) { 
				for (int j = 0; j < dataSet.getFrameSet(i).size(); j++) {
					SegmentModel segment = dataSet.getFrameSet(i).get(j);
					double [] calculations = calculation(segment);
					for (int k = 0; k < calculations.length; k ++) {
						output.addLine(segment.getLinkSet().getDataSet().getName(),
							name, segment.getLinkSet().getName(), segment.getFrame() + 1, 
							calculationNames[k], calculations[k]); //TODO: dataSet name
					}
				}
			}
		}
		return output.getWorkbook();
	}
	
	
	boolean tempTest = false;
	
	if (tempTest) {
		for (int i = 0; i < dataSets[0].getSize(); i++) { //iterates through all frameSets, should be equivalent for all dataSets
			
			
				
			}
		}
	return workbook;
	}

/DEPRECIATED	
	public void calculateDEPRECIATED() {
	
		//runs calculations
		for (int n = 0; n < outputDataSets.length; n++) {	// n = DataSet
			
			//TODO: dataset calculations
			
			//LinkSet data [segment][calculation] //TODO: initialize with -1 flag for segment if not there
			double[][][] linkData = new double[outputDataSets[n].getLinkSetList().size()]
					[outputDataSets[n].getFrameSetList().length][segmentModelCalculationNames().length];
					
			int[][] linkDataRange = new int[outputDataSets[n].getLinkSetList().size()][2];
			for (int i = 0; i < linkDataRange.length; i++) {
				linkDataRange[i][0] = Integer.MAX_VALUE;
				linkDataRange[i][1] = Integer.MIN_VALUE;
			}
			
			
			for (int i = 0; i < outputDataSets[n].getFrameSetList().length; i++) { // i = FrameSet
			
				//FrameSet data, [segment][calculation]
				double [][] frameData = new double [outputDataSets[n].getFrameSet(i).size()][segmentModelCalculationNames().length];
			
				
				for (int j = 0; j < outputDataSets[n].getFrameSet(i).size(); j++) { // j =  SegmentModel
					SegmentModel segment = outputDataSets[n].getFrameSet(i).get(j);
					double [] calculations = segmentModelCalculations(segment); // k = calculation
					
					for (int k = 0; k < calculations.length; k++) {
						
						//print to workbook //TODO --> make this part of the main
						Object[] segmentCalculations = new Object[] {
							outputDataSets[n].getName(),
							methodName,
							segment.getLinkSet().getName(),
							segment.getFrame() + 1,
							segmentModelCalculationNames()[k],
							calculations[k],
						};
						
						//adds to sheet
						workbook.addLine(segmentModelSheet, segmentCalculations);
						
						System.out.println("FrameSet Name: " + segment.getFrame());
						//adds to FrameSet Data
						frameData[j][k] = calculations[k];
						
						System.out.println("LinkSet Name: " + segment.getLinkSet().getName());
						System.out.println("LinkData length: " + linkData.length);
						System.out.println("LinkData[j] length: " + linkData[j].length);
						//adds to LinkSet Data
						linkData[segment.getLinkSet().getName()][segment.getFrame()][k] = calculations[k];
						System.out.println("Added");
						
					}
					
					
					if (i < linkDataRange[segment.getLinkSet().getName()][0]) {
						linkDataRange[segment.getLinkSet().getName()][0] = i;
					}
					
					if (i > linkDataRange[segment.getLinkSet().getName()][1]) {
						linkDataRange[segment.getLinkSet().getName()][1] = i;
					}
				}
				
				//Calculate for frame Set
				double[] frameCalculations = frameSetCalculations(frameData);
				
				for (int k = 0; k < frameCalculations.length; k++) {
					Object[] frameOutput = new Object[] {
						outputDataSets[n].getName(), //DataSetName
						methodName, //Name of method
						i + 1,//FrameSet Name (aka: frame)
						frameSetCalculationNames()[k],
						frameCalculations[k],
					};
					workbook.addLine(frameSetSheet, frameOutput);
				}
			}
				
			//TODO: Implement
			//Calculate for link set
			for (int i = 0; i < linkData.length; i++) {
				
				//based on segment model calculations
				double[] linkCalculations = linkSetCalculations(
						linkData[i],linkDataRange[i][0], linkDataRange[i][1]);
			
				
				for (int k = 0; k < linkCalculations.length; k++) {
					Object[] linkOutput = new Object[] {	
						outputDataSets[n].getName(), //DataSetName
						methodName, //Name of method
						i,    //LinkSet Name
						linkSetCalculationNames()[k],
						linkCalculations[k],
					};
					workbook.addLine(linkSetSheet, linkOutput);
				}
			}	
		}
	}



	 */
	
	
	
	
	
	
	
	
}

