package calculations;

/**
 * Counts the number of subsegment tracks (child LinkSets) found inside a parent segment.
 *
 * Each child LinkSet represents one subsegment tracked across frames, so this returns
 * the total number of distinct subsegments identified in the parent segment's interior —
 * regardless of how many frames each subsegment spans.
 *
 * No perimeter data is required; this calculation is valid at the SARN-only stage.
 */
public class VoidCount extends RecursiveLinkSetCalculation {

	@Override
	public double calculate() {
		return childLinkSets.size();
	}

	@Override
	public String getName() {
		return "Subsegment Count";
	}
}
