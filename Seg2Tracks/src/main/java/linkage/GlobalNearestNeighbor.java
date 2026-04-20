package linkage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import dataStructure.LinkSet;
import dataStructure.Segment;

/**
 * Simple greedy linkage using nearest-neighbor distance. Computes Euclidean
 * distance between all pairs of segments in consecutive frames, sorts by distance,
 * and greedily assigns each segment to its nearest unmatched partner. Fast but
 * suboptimal compared to global optimization like the Hungarian algorithm.
 */
public class GlobalNearestNeighbor extends Linkage{

	/**
	 * Constructor. Sets method name and description.
	 */
	public GlobalNearestNeighbor() {
		name = "Global Nearest Neighbor";
		description = " "; // TODO: Fill out detailed description
	}
	
	/**
	 * Link segments between frame step and step+1 using greedy nearest-neighbor.
	 * Computes pairwise distances, sorts by distance (ascending), then iterates
	 * through sorted pairs, assigning each unmatched pair. Handles cell divisions
	 * (one cell → two in next frame) and mergers (two cells → one) by checking if
	 * segments are already matched.
	 *
	 * @param step Index of current frame; links to frame step+1
	 */
	public void setLinkage(int step) {

		//System.out.println("Set Linkage: " + step);

		// Get segment lists for current and next frame
		ArrayList<Segment> list1 = dataSet.getFrameSet(step); // Previous frame
		ArrayList<Segment> list2 = dataSet.getFrameSet(step + 1); // Next frame

		// Distance matrix: [distance, list1_index, list2_index]
		double[][] matrix = new double[list1.size() * list2.size()][3];

		//System.out.println("List1 Size: " + list1.size());
		//System.out.println("List2 Size: " + list2.size());
		//System.out.println("Matrix Size: " + matrix.length);

		// Compute pairwise distances between all segments in consecutive frames
		for (int i = 0; i < list1.size(); i++) {
			for (int j = 0; j < list2.size(); j++) {
				// Distance is squared Euclidean distance
				matrix[(i * list2.size()) + j][0] = linkFormula(list1.get(i), list2.get(j));
				matrix[(i * list2.size()) + j][1] = i; // Index in list1
				matrix[(i * list2.size()) + j][2] = j; // Index in list2
			}
		}

		// Sort by distance (ascending) — process shortest distances first
		Arrays.sort(matrix, Comparator.comparingDouble(x -> x[0]));

		// Greedy assignment: iterate through sorted pairs, match if both unmatched
		for (int i = 0; i < matrix.length; i++) {
			Segment sm1 = list1.get((int) matrix[i][1]); // Segment from frame N
			Segment sm2 = list2.get((int) matrix[i][2]); // Segment from frame N+1

			// Guard: sm1 may have no LinkSet if it was unmatched in the previous step.
			if (sm1.getIsLastFrame() && !sm1.hasLinkSet()) {
				LinkSet linkSet = new LinkSet(sm1, dataSet);
				sm1.setLinkSet(linkSet);
				linkSet.setStart(sm1);
				linkSet.setName(linkSet.getDataSet().getLinkSetNameIterator());
				serialNumber++;
			}

			// Case 1: sm1 is the last segment in its track (needs to link forward)
			if (sm1.getIsLastFrame()) {
				// sm2 already matched to another segment — possible cell division
				if (sm2.hasLinkSet()) {
					// sm1 track ends here; mark as orphan
					sm1.getLinkSet().setEnd(sm1);
					sm1.getLinkSet().setOrphan(true);
					//System.out.println("Already Matched - Split?");
				}

				// sm2 unmatched — link it to sm1's track
				if (!sm2.hasLinkSet()) {
					sm2.setLinkSet(sm1.getLinkSet()); // Assign to existing track
					sm1.getLinkSet().add(sm2);
					sm1.setIsLastFrame(false); // sm1 is no longer the end
				}
			}

			// Case 2: sm1 is already linked to a track from a previous frame
			if (!sm1.getIsLastFrame()) {
				// Both already matched — skip (no need to reassign)
				if (sm2.hasLinkSet()) {
					// Do nothing
				}

				// sm1 matched, sm2 unmatched — possible new object (cell division)
				if (!sm2.hasLinkSet()) {
					LinkSet linkSet = new LinkSet(sm2, dataSet); // Create new track
					sm2.setLinkSet(linkSet);
					sm2.getLinkSet().setStart(sm2);
					sm1.getLinkSet().addChild(sm2.getLinkSet()); // Track parent-child relationship
					sm2.getLinkSet().setName(linkSet.getDataSet().getLinkSetNameIterator());
					serialNumber++;
					//System.out.println("New Object");
				}
			}
		}

	}
	
	/**
	 * Compute squared Euclidean distance between two segment centers.
	 * Uses squared distance (no sqrt) for efficiency; relative ordering is preserved.
	 *
	 * @param one First segment
	 * @param two Second segment
	 * @return Squared Euclidean distance between their center points
	 */
	public double linkFormula(Segment one, Segment two) {

		// Extract center coordinates for clarity
		int x1 = one.getCenterPoint().x;
		int y1 = one.getCenterPoint().y;
		int x2 = two.getCenterPoint().x;
		int y2 = two.getCenterPoint().y;

		// Squared Euclidean distance (avoids expensive sqrt; order preserved)
		return (Math.pow(y2-y1, 2) + Math.pow(x2-x1, 2));
	}
}
		

		
		

		 
		

