package analysisMethod;

import java.awt.Color;
import java.util.ArrayList;

import calculations.FrameSetCalculation;
import calculations.FrameSetStatistic;
import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.SegmentCalculation;
import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.gui.Roi;
import geometricTools.GeometricCalculations;

/**
 * Merges segmentations from two DataSets into a single overlay visualization.
 * Calculates objects separately but displays them on the same image with different colors.
 */
public class ChannelMerger extends OperationMethod {

	/**
	 * Constructor initializing channel merger settings.
	 */
	public ChannelMerger() {
		methodName = "Overlay Channels";
		description = "Provides one overlay for two channels and but calculates objects separately";
		numberOfCalculations = 1;
	}

	/**
	 * Returns channel labels for the two input datasets being merged.
	 *
	 * @return array with labels for DataSet 1 and DataSet 2
	 */
	@Override
	public String[] getChannels() {
		return new String[] {
				"DataSet 1: ",
				"DataSet 2: "
		};
	}


	/**
	 * Merge two input DataSets into a single output DataSet with combined LinkSets and FrameSets.
	 * Colors LinkSets with semi-transparent red and green for visual distinction.
	 *
	 * @param inputSet array of two DataSets to merge
	 * @return array with single merged DataSet
	 */
	@Override
	DataSet[] dataOperation(DataSet[] inputSet) {

		//Generate output dataSet
		DataSet mergedSet =  new DataSet(inputSet[0].getWidth(), inputSet[0].getHeight(), inputSet[0].getSize());

		//Modify DataSet colors
		Color colorOne = new Color (255, 0, 0, 127);
		Color colorTwo = new Color (0, 255, 0, 127);

		for (LinkSet linkSet: dataSets[0].getLinkSetList()) {
			linkSet.setColor(colorOne);
		}

		for (LinkSet linkSet: dataSets[1].getLinkSetList()) {
			linkSet.setColor(colorTwo);
		}

		//Merge DataSets
		ArrayList <LinkSet> mergedLinkSetList = new ArrayList<LinkSet>();
		mergedLinkSetList.addAll(dataSets[0].getLinkSetList());
		mergedLinkSetList.addAll(dataSets[1].getLinkSetList());

		FrameSet[] mergedFrameSetList = new FrameSet[dataSets[0].getFrameSetList().length];
		for (int i = 0; i < mergedFrameSetList.length; i ++) {
			mergedFrameSetList[i] = new FrameSet(i, mergedSet);
			mergedFrameSetList[i].addAll(dataSets[0].getFrameSet(i));
			mergedFrameSetList[i].addAll(dataSets[1].getFrameSet(i));
		}

		mergedSet.setLinkSetList(mergedLinkSetList);
		mergedSet.setFrameSetList(mergedFrameSetList);

		DataSet outputSets[] = {mergedSet};
		return outputSets;
	}

	/**
	 * Extract overlay ROI from segment using straight line perimeter representation.
	 *
	 * @param segment the Segment to extract ROI for
	 * @return Roi using straightened perimeter
	 */
	//@Override
	Roi getOverlayParameter(Segment segment) {
		return getPolygonRoi(
				GeometricCalculations.straightPerimeter(
						segment.getInternalPerimeter())); //STRAIGHT LINE
	}


	/**
	 * Segment-level calculations for merged channel analysis.
	 * ChannelMerger is a visualization-only method — quantitative comparison between
	 * channels is handled by SegmentationComparer. Returns null intentionally;
	 * OperationMethod.setCalculations() handles null safely.
	 *
	 * @return null (no segment calculations for this method)
	 */
	@Override
	SegmentCalculation[] segmentCalculations() {
		return null;
	}

	/**
	 * No workbook sheets are defined for ChannelMerger, as it produces no quantitative output.
	 * Quantitative cross-channel metrics (Jaccard, Dice, Coverage, etc.) are computed by
	 * SegmentationComparer instead.
	 */
	@Override
	void defineSheets() {
		// Intentionally empty — ChannelMerger produces no Excel output.
	}

	/**
	 * LinkSet-level calculations for merged channel analysis.
	 * Returns null intentionally — ChannelMerger is visualization-only.
	 *
	 * @return null (no LinkSet calculations for this method)
	 */
	@Override
	LinkSetCalculation[] linkSetCalculations() {
		return null;
	}

	/**
	 * LinkSet-level statistics for merged channel analysis.
	 * Returns null intentionally — ChannelMerger is visualization-only.
	 *
	 * @return null (no LinkSet statistics for this method)
	 */
	@Override
	LinkSetStatistic[] linkSetStatistics() {
		return null;
	}

	/**
	 * FrameSet-level calculations for merged channel analysis.
	 * Returns null intentionally — ChannelMerger is visualization-only.
	 *
	 * @return null (no FrameSet calculations for this method)
	 */
	@Override
	FrameSetCalculation[] frameSetCalculations() {
		return null;
	}

	/**
	 * FrameSet-level statistics for merged channel analysis.
	 * Returns null intentionally — ChannelMerger is visualization-only.
	 *
	 * @return null (no FrameSet statistics for this method)
	 */
	@Override
	FrameSetStatistic[] frameSetStatistics() {
		return null;
	}
}
