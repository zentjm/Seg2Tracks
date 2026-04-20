package linkage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import dataStructure.LinkSet;
import dataStructure.Segment;

public class ModifiedHungarian extends Linkage{

	//int serialNumber;
	
	// TODO: Split assignments (cell division handling)
	// TODO: Merge assignments (cell merger handling)
	
	
	
	
	/**
	 * Constructor. Sets method name and description.
	 */
	public ModifiedHungarian() {
		name = "Modified Hungarian";
		description = " "; // TODO: Fill out detailed description
	}
	
	
	/**
	 * Link segments between frames step and step+1 using the Hungarian algorithm.
	 * Builds a cost matrix of pairwise distances, pads with dummy entries for
	 * asymmetric frame sizes, applies Hungarian algorithm preprocessing, and
	 * iterates through zero-cost pairs to find optimal assignments.
	 * NOTE: Implementation is incomplete; linkAssist() has known bugs detailed in that method.
	 *
	 * @param step Index of current frame; links to frame step+1
	 */
	public void setLinkage(int step) {

		//System.out.println();
		//System.out.println();
		//System.out.println("Set Linkage: " + step);

		// Get segment lists for current and next frame
		ArrayList<Segment> list1 = dataSet.getFrameSet(step); // Previous frame (workers)
		ArrayList<Segment> list2 = dataSet.getFrameSet(step + 1); // Next frame (tasks)

		// Detect size change to handle cell divisions/mergers
		int offset = list2.size() - list1.size();

		// Hungarian algorithm requires square matrices — pad with dummy rows/columns
		int dummyList1 = list1.size();
		int dummyList2 = list2.size();

		// If more cells in frame N+1, add dummy rows to frame N
		if (offset > 0) dummyList1 += offset;
		// If fewer cells in frame N+1, add dummy columns to frame N+1
		if (offset < 0) dummyList2 -= offset;

		// Cost matrix: [distance, list1_index, list2_index]
		double[][] matrix = new double[dummyList1 * dummyList2][3];

		//System.out.println("List1 Size: " + list1.size() + "     dummyList1 Size:" + dummyList1);
		//System.out.println("List2 Size: " + list2.size() + "     dummyList2 Size:" + dummyList2); // NOTE: Prints dummyList1 again (bug)
		//System.out.println("Offset: " + offset);
		//System.out.println("Matrix Size: " + matrix.length);
		
		// Compute distance for all real segment pairs
		double maxValue = 0;
		for (int i = 0; i < list1.size(); i++) {
			for (int j = 0; j < list2.size(); j++) {
				// Squared Euclidean distance between centers
				double linkCalc = linkFormula(list1.get(i), list2.get(j));
				if (linkCalc > maxValue) maxValue = linkCalc; // Track max for dummy padding
				matrix[(i * dummyList2) + j][0] = linkCalc;
				matrix[(i * dummyList2) + j][1] = i; // Index in list1
				matrix[(i * dummyList2) + j][2] = j; // Index in list2
			}
		}

		//System.out.println("Max Value: " + maxValue);

		// Fill dummy rows (list1) with maxValue so they have lowest priority for assignment
		for (int i = list1.size(); i < dummyList1; i++) {
			for (int j = 0; j < dummyList2; j++) {
				matrix[(i * dummyList2) + j][0] = maxValue;
				matrix[(i * dummyList2) + j][1] = -1; // Mark as dummy
				matrix[(i * dummyList2) + j][2] = -1;
			}
		}
		// Fill dummy columns (list2) with maxValue so they have lowest priority for assignment
		for (int i = 0; i < dummyList1; i++) {
			for (int j = list2.size(); j < dummyList2; j++) {
				matrix[(i * dummyList2) + j][0] = maxValue;
				matrix[(i * dummyList2) + j][1] = -1; // Mark as dummy
				matrix[(i * dummyList2) + j][2] = -1;
			}
		}
		
		
	
		
		
		
		
		
		
		// First step of Hungarian algorithm: reduce cost matrix
		// Choose order based on offset to handle row/column heavy situations
		if (offset < 0) {
			// More segments in frame N+1 — subtract rows first, then columns
			// Subtract the smallest value in each row from all values in that row
			for (int i = 0; i < dummyList1; i++) {
				double minRowValue = Double.MAX_VALUE;
				for (int j = 0; j < dummyList2; j++) {
					if ((matrix[i * dummyList2 + j][0]) < minRowValue) {
						minRowValue = matrix[i * dummyList2 + j][0];
					}
				}
				// Subtract minimum from all entries in this row
				for (int j = 0; j < dummyList2; j++) {
					matrix[i * dummyList2 + j][0] = matrix[i * dummyList2 + j][0] - minRowValue;
				}
			}

			// Subtract the smallest value in each column from all values in that column
			// Skip columns that already have a zero (optimization)
			for (int j = 0; j < dummyList2; j++) {
				boolean hasZero = false;
				double minColumnValue = Double.MAX_VALUE;
				// Check if column has any zeros
				for (int i = 0; i < dummyList1; i++) {
					if (matrix[i * dummyList2 + j][0] == 0) {
						hasZero = true;
						break;
					}
				}
				// If no zeros, subtract minimum
				if (!hasZero) {
					for (int i = 0; i < dummyList1; i++) {
						if ((matrix[i * dummyList2 + j][0]) < minColumnValue) {
							minColumnValue = matrix[i * dummyList2 + j][0];
						}
					}
					// Subtract minimum from all entries in this column
					for (int i = 0; i < dummyList1; i++) {
						matrix[i * dummyList2 + j][0] = matrix[i * dummyList2 + j][0] - minColumnValue;
					}
				}
			}
		} else {
			// Fewer or equal segments in frame N+1 — subtract columns first, then rows
			// Subtract the smallest value in each column from all values in that column
			for (int j = 0; j < dummyList2; j++) {
				double minColumnValue = Double.MAX_VALUE;
				for (int i = 0; i < dummyList1; i++) {
					if ((matrix[i * dummyList2 + j][0]) < minColumnValue) {
						minColumnValue = matrix[i * dummyList2 + j][0];
					}
				}
				// Subtract minimum from all entries in this column
				for (int i = 0; i < dummyList1; i++) {
					matrix[i * dummyList2 + j][0] = matrix[i * dummyList2 + j][0] - minColumnValue;
				}
			}

			// Subtract the smallest value in each row from all values in that row
			// Skip rows that already have a zero (optimization)
			for (int i = 0; i < dummyList1; i++) {
				boolean hasZero = false;
				double minRowValue = Double.MAX_VALUE;
				// Check if row has any zeros
				for (int j = 0; j < dummyList2; j++) {
					if (matrix[i * dummyList2 + j][0] == 0) {
						hasZero = true;
						break;
					}
				}
				// If no zeros, subtract minimum
				if (!hasZero) {
					for (int j = 0; j < dummyList2; j++) {
						if ((matrix[i * dummyList2 + j][0]) < minRowValue) {
							minRowValue = matrix[i * dummyList2 + j][0];
						}
					}
					// Subtract minimum from all entries in this row
					for (int j = 0; j < dummyList2; j++) {
						matrix[i * dummyList2 + j][0] = matrix[i * dummyList2 + j][0] - minRowValue;
					}
				}
			}
		}
			

		
		// Apply Hungarian algorithm refinement (incomplete)
		matrix = linkAssist(matrix, dummyList1, dummyList2);

		
		
		// Sort cost matrix by distance (ascending) to prioritize optimal assignments
		Arrays.sort(matrix, Comparator.comparingDouble(x -> x[0]));
	
		
		// Find zero-cost pairs (optimal assignments) and process them first
	
		
		// Note: This loop appears incomplete — no actual assignment logic implemented
		for (int i = 0; i < dummyList1; i++) {
			for (int j = 0; j < dummyList2; j++) {
				if (matrix[(i * dummyList2) + j][1] == -1) continue; // Skip dummy entries

				// Found a zero-cost assignment
				if (matrix[(i * dummyList2) + j][0] == 0) {
					// TODO: Implement zero-cost assignment logic
				}
				
				
				
				
			}
		}
		
		// Main assignment loop: iterate through sorted cost matrix and assign linked pairs
		for (int i = 0; i < matrix.length; i++) {
			if (matrix[i][1] == -1) continue; // Skip dummy entries (cell loss/gain)

			// Extract segments from lists
			Segment sm1 = list1.get((int) matrix[i][1]); // Segment from frame N
			Segment sm2 = list2.get((int) matrix[i][2]); // Segment from frame N+1

			// Guard: sm1 may have no LinkSet if it was an unmatched sm2 in the
			// previous step (its dummy row was skipped then). Retroactively start
			// a new track for it before any case logic that calls getLinkSet().
			if (sm1.getIsLastFrame() && !sm1.hasLinkSet()) {
				LinkSet linkSet = new LinkSet(sm1, dataSet);
				sm1.setLinkSet(linkSet);
				linkSet.setStart(sm1);
				linkSet.setName(linkSet.getDataSet().getLinkSetNameIterator());
				serialNumber++;
			}

			// Case 1: sm1 is the last segment in its track (needs to link forward)
			if (sm1.getIsLastFrame()) {

				// sm2 already matched to another segment — sm1 track ends here as orphan.
				// Note: merge naming (appending sm1's name to sm2's track) was removed because
				// the Hungarian cost-reduction creates many zero-cost pairs, causing multiple
				// orphaning sm1 tracks to all accumulate onto the same continuing track's name,
				// producing runaway name growth ("A_B_C_D_...").
				if (sm2.hasLinkSet()) {
					sm1.getLinkSet().setEnd(sm1);
					sm1.getLinkSet().setOrphan(true);
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
				}
			}
		}

	}
	
	
	
	
	
	/**
	 * Hungarian algorithm auxiliary method (incomplete). Attempts to find the minimum
	 * number of lines needed to cover all zeros in the reduced cost matrix. Returns null
	 * instead of a refined matrix — implementation is non-functional.
	 *
	 * STUB: Returns null after complex logic. The Hungarian algorithm recursion is
	 * incomplete and does not actually refine the cost matrix.
	 *
	 * @param matrix Cost matrix with indices
	 * @param dummyList1 Number of rows
	 * @param dummyList2 Number of columns
	 * @return null (non-functional)
	 */
	public double[][] linkAssist(double[][] matrix, int dummyList1, int dummyList2) {
		// Line marking arrays for identifying covered zeros
		boolean markMatrixColumn[] = new boolean[dummyList1]; // Tracks marked columns (i)
		boolean markMatrixRow[] = new boolean[dummyList2]; // Tracks marked rows (j)

		// Algorithm state
		boolean proceed = false;
		boolean finished = false;

		// Phase 1: Find minimum number of lines covering all zeros
		while (!finished) {
			int lineCount = 0;
			proceed = false;
			markMatrixColumn = new boolean[dummyList1];
			markMatrixRow = new boolean[dummyList2];

			while (!proceed) {
				int rowMax = -1;
				int columnMax = -1;

				int rowMaxCount = -1;
				int columnMaxCount = -1;

				// Count uncovered zeros in each column
				for (int i = 0; i < dummyList1; i++) { // Iterate over columns
					int count = 0;
					for (int j = 0; j < dummyList2; j++) {
						// Count zero entries that are not yet covered
						if (matrix[(i * dummyList2) + j][0] == 0 &&
							markMatrixColumn[i] == false &&
							markMatrixRow[j] == false) {
							count++;
						}
					}
					if (count > columnMaxCount) {
						columnMaxCount = count;
						columnMax = i;
					}
				}

				// Count uncovered zeros in each row
				for (int j = 0; j < dummyList2; j++) { // Iterate over rows
					int count = 0;
					for (int i = 0; i < dummyList1; i++) {
						if (matrix[(i * dummyList2) + j][0] == 0 &&
							markMatrixColumn[i] == false &&
							markMatrixRow[j] == false) {
							count++;
						}
					}
					if (count > rowMaxCount) {
						rowMaxCount = count;
						rowMax = j;
					}
				}

				// No more uncovered zeros — matrix is refined
				if (rowMaxCount == 0 && columnMaxCount == 0) {
					proceed = true;
				}
				// Mark row if it has more uncovered zeros than any column
				else if (rowMaxCount > columnMaxCount) {
					markMatrixRow[rowMax] = true;
					lineCount++;
				}
				// Mark column if it has more uncovered zeros than any row
				else {
					markMatrixColumn[columnMax] = true;
					lineCount++;
				}
			}

			// Success: can cover all zeros with exactly dummyList1 lines
			if (lineCount == dummyList1) return matrix;

			// Phase 2: Refine matrix for next iteration

			// Find minimum uncovered value
			double minValue = Double.MAX_VALUE;
			for (int i = 0; i < dummyList1; i++) {
				for (int j = 0; j < dummyList2; j++) {
					// Only consider uncovered cells
					if (matrix[(i * dummyList2) + j][0] < minValue &&
							markMatrixColumn[i] == false &&
							markMatrixRow[j] == false) {
						minValue = matrix[(i * dummyList2) + j][0];
					}
				}
			}

			// Apply Hungarian algorithm matrix transformations
			for (int i = 0; i < dummyList1; i++) {
				for (int j = 0; j < dummyList2; j++) {
					// Subtract minimum from uncovered cells
					if (markMatrixColumn[i] == false &&
							markMatrixRow[j] == false) {
						matrix[(i * dummyList2) + j][0] -= minValue;
					}
					// Add minimum to doubly-covered cells (covered by both row and column lines)
					if (markMatrixColumn[i] == true &&
							markMatrixRow[j] == true) {
						matrix[(i * dummyList2) + j][0] += minValue;
					}
				}
			}
		}

		// Should never reach here — the while loop only exits via return matrix above.
		// Throwing here is preferable to returning null, which would cause a silent NPE.
		throw new IllegalStateException("Hungarian algorithm failed to converge");
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
