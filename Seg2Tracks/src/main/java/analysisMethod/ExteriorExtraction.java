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

/**
 * Extracts morphological and intensity data from pericellular regions surrounding cell segmentations.
 * Defines exterior region as the difference between external and internal perimeter boundaries.
 * Used for measuring extracellular phenomena like matrix degradation and pericellular activity.
 */
public class ExteriorExtraction extends OperationMethod {

	/**
	 * Constructor initializing exterior extraction analysis settings.
	 */
	public ExteriorExtraction()  {
		methodName = "Exterior Extraction";
		description = "Morphological-intensity data of pericellular environment (Area = SARN Region - segmentation";
	}

	/**
	 * Returns channel names for the input dataset.
	 *
	 * @return array with single element "DataSet:"
	 */
	@Override
	public String[] getChannels() {
		return new String[] { "DataSet:"};
	}

	/**
	 * Perform any necessary transformations on input datasets.
	 * Currently returns input unchanged; exterior region extraction happens in overlayParameter and calculations.
	 * //TODO: this changes the output of segmentation - need to CLONE so only output of analysis
	 *
	 * @param inputSet array of input DataSets
	 * @return same input DataSet array
	 */
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
	}

	/**
	 * Extract the exterior (pericellular) region ROI from a segment.
	 * Defined as external perimeter minus internal perimeter (the pericellular annulus).
	 *
	 * @param segment the Segment to extract exterior region for
	 * @return Roi representing the pericellular space
	 */
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




	/**
	 * Returns segment-level calculations for exterior region analysis.
	 * Computes perimeter and area differences between external and internal boundaries.
	 *
	 * @return array of exterior SegmentCalculation objects
	 */
	@Override
	SegmentCalculation[] segmentCalculations() {
		Area area = new Area();
		DifferenceIntensity differenceIntensity = new DifferenceIntensity();
		return new SegmentCalculation[] {
			new Perimeter(),
			area,
			new DifferenceArea(),
			differenceIntensity,
			new DifferenceIntensityMean(area, differenceIntensity)
		};
	}

	/**
	 * Returns LinkSet-level calculations for exterior region analysis.
	 *
	 * @return array containing AreaDistribution (Area Standard Deviation) calculation
	 */
	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return new LinkSetCalculation[] {
			new AreaDistribution()
		};
	}

	/**
	 * Returns LinkSet-level statistics for exterior region analysis.
	 *
	 * @return array containing LinkSetMean statistic
	 */
	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return new LinkSetStatistic[] {
			new LinkSetMean()
		};
	}

	/**
	 * Returns FrameSet-level calculations for exterior region analysis.
	 *
	 * @return array containing Confluency calculation
	 */
	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return new FrameSetCalculation[] {
			new Confluency()
		};
	}

	/**
	 * Returns FrameSet-level statistics for exterior region analysis.
	 *
	 * @return array containing FrameSetMean statistic
	 */
	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return new FrameSetStatistic[] {
			new FrameSetMean()
		};
	}


}
