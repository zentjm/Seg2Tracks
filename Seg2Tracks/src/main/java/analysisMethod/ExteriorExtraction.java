package analysisMethod;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import calculations.Area;
import calculations.AreaDistribution;
import calculations.Confluency;
import calculations.Data;
import calculations.DifferenceArea;
import calculations.DifferenceIntensity;
import calculations.DifferenceIntensityMean;
import calculations.FrameSetCalculation;
import calculations.FrameSetMean;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetMean;
import calculations.LinkSetStatistic;
import calculations.Perimeter;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import geometricTools.GeometricCalculations;

public class ExteriorExtraction extends OperationMethod {

	public ExteriorExtraction()  {
		methodName = "Exterior Extraction";
		description = "Morphological-intensity data of pericellular environment";
	}

	@Override
	public String[] getChannels() {
		return new String[] { "DataSet:"};
	}

	@Override
	DataSet[] dataOperation(DataSet[] inputSet) {
		
		//TODO: this changes the output of segmentation - need to CLONE so only output of analysis
		
		/*
		for (DataSet dataSet: inputSet) {
			for (FrameSet frameSet : dataSet.getFrameSetList()) {
				for (SegmentModel segment : frameSet) {
				
					ShapeRoi external = new ShapeRoi (getPolygonRoi(segment.getExternalPerimeter()).getPolygon());
					ShapeRoi internal = new ShapeRoi (getPolygonRoi(segment.getInternalPerimeter()).getPolygon());
					external.not(internal);
					
					//Convert shapeRoi to a perimeter
					FloatPolygon polyRoi = new PolygonRoi(external.getPolygon(), Roi.POLYLINE).getFloatPolygon();
					
					float[] xPts = polyRoi.xpoints;
					float[] yPts = polyRoi.ypoints;
					
					Point[]	pts = new Point[polyRoi.npoints];

					for (int k = 0; k < polyRoi.npoints; k++) {
						pts[k] = new Point((int) xPts[k], (int) yPts[k]);
					}
					
					segment.setInternalPerimeter(pts);

				}
			}
		}
		*/
			
		
		return inputSet;
		
		
		
		
		
		
		/*
		//Generate new output DataSets
		DataSet newSet =  new DataSet(inputSet[0].getWidth(), inputSet[0].getHeight(), inputSet[0].getSize());
		
		//Name dataSets
		newSet.setDataSetName("Pericellular Segmentation");
		
		//Define overlays colors for each DataSet
		Color colorOne = new Color (0, 255, 0, 127);
	
		//Set up new LinkedList/FrameSet data holders
		ArrayList<LinkSet> linkSetList = new ArrayList<LinkSet>();
		FrameSet[] frameSetList = new FrameSet[dataSets[0].getFrameSetList().length];
		
		for (DataSet dataSet: inputSet) {
			for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
				frameSetList[i] = new FrameSet(i, newSet);
				for (int j = 0; j < dataSet.getFrameSetList()[i].size(); j++) {
					
					//Subtract internal from external segmentation
					FrameSet frameSet = dataSet.getFrameSetList()[i];
					SegmentModel segment = frameSet.get(j);
					ShapeRoi external = new ShapeRoi (getPolygonRoi(segment.getExternalPerimeter()).getPolygon());
					ShapeRoi internal = new ShapeRoi (getPolygonRoi(segment.getInternalPerimeter()).getPolygon());
					external.not(internal);
					
					//Convert shapeRoi to a perimeter
					FloatPolygon polyRoi = new PolygonRoi(external.getPolygon(), Roi.POLYLINE).getFloatPolygon();
					
					float[] xPts = polyRoi.xpoints;
					float[] yPts = polyRoi.ypoints;
					
					Point[]	pts = new Point[polyRoi.npoints];

					for (int k = 0; k < polyRoi.npoints; k++) {
						pts[k] = new Point((int) xPts[k], (int) yPts[k]);
					}
					
					SegmentModel newSeg = new SegmentModel(frameSet.getFrame(), frameSet.get(j).getCenterPoint());
					newSeg.setInternalPerimeter(pts);
				}
			}	
			for (int j = 0; j < dataSet.getLinkSetList().size(); j++) {
			
				
			}
		}
		*/
	}

	@Override
	Roi getOverlayParameter(Segment segment) {
		//Subtract internal from external segmentation
		ShapeRoi external = new ShapeRoi (getPolygonRoi(
				segment.getExternalPerimeter()).getPolygon());
		ShapeRoi internal = new ShapeRoi (getPolygonRoi(
				segment.getInternalPerimeter()).getPolygon());
		Roi roi = external.not(internal);
		return roi;	
	}
	
	
	
	
	
	

	@Override
	SegmentCalculation[] segmentCalculations() {
		return new SegmentCalculation[] {
			new Perimeter(),
			new DifferenceArea(),
			new DifferenceIntensity(),
			new DifferenceIntensityMean()
		};
	}

	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
			new AreaDistribution()
		};
	}

	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return new LinkSetStatistic[] {
			new LinkSetMean()
		};
	}

	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return new FrameSetCalculation[] {
			new Confluency()
		};
	}

	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
			new FrameSetMean()
		};
	}

	
}
