package calculations;

import dataStructure.Segment;
import geometricTools.GeometricCalculations;

/**
 * Computes the total pixel area occupied by all subsegment tracks inside a parent segment.
 *
 * Area per subsegment is computed as the pixel count within the segment's
 * {@code internalPerimeter} (the final constricted boundary, consistent with
 * {@link Area}).  All frames of all child subsegment tracks are summed.
 *
 * <p><strong>Pipeline requirement:</strong> {@code internalPerimeter} must be set
 * on subsegments (i.e. restricted internal segmentation must have been run after
 * SARN).  Segments whose {@code internalPerimeter} is null contribute 0 to the sum;
 * a TODO gating mechanism will block this calculation from running when only
 * SARN-stage data is available.
 */
public class TotalVoidArea extends RecursiveLinkSetCalculation {

	@Override
	public double calculate() {
		double total = 0;
		for (dataStructure.LinkSet child : childLinkSets) {
			for (Segment seg : child) {
				if (seg.getInternalPerimeter() != null) {
					total += GeometricCalculations.getAreaByRoi(seg.getInternalPerimeter()).length;
				}
			}
		}
		return total;
	}

	@Override
	public String getName() {
		return "Total Subsegment Area";
	}
}
