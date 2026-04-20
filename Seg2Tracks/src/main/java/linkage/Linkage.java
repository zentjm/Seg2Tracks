package linkage;

import javax.swing.JProgressBar;

import dataStructure.DataSet;
import dataStructure.LinkSet;
import dataStructure.Segment;

/**
 * Abstract base class for all cell-tracking linkage methods. Handles linking cells
 * across frames in a time-lapse sequence. Subclasses implement different algorithms
 * (e.g., nearest-neighbor, Hungarian algorithm) to assign segments in frame N to
 * segments in frame N+1.
 */
public abstract class Linkage {

	DataSet dataSet; // Container for all frames and segments
	String name; // Name of this linkage method
	String description; // Description of how this method works

	// Status tracking UI
	JProgressBar progressBar; // Progress bar for visual feedback

	// Variables for tracking
	int serialNumber; // Counter for assigning unique LinkSet identifiers
	
	/**
	 * Constructor. Initializes name and description to placeholder values.
	 * Subclasses should override these with actual method names and descriptions.
	 */
	public Linkage() {
		name = "Method Name";
		description = "How this Method Works";
	}
	
	/**
	 * Initialize linkage with data and progress bar.
	 * @param dataSet Container of all frames and segments
	 * @param progressBar UI progress bar for user feedback
	 */
	public void initialize(DataSet dataSet, JProgressBar progressBar) {
		this.dataSet = dataSet;
		this.progressBar = progressBar;
	}

	/**
	 * Return this method's name for display in UI selection menus.
	 * @return Method name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Return a description of how this linkage method works.
	 * @return Method description
	 */
	public String getDescription() {
		return description;
	}
		
	/**
	 * Main entry point: initialize LinkSet objects for the first frame with detections,
	 * then iteratively call setLinkage() to link segments across consecutive frames.
	 * This orchestrates the entire tracking pipeline.
	 */
	public void run() {


		//System.out.println("Running " + name);
		serialNumber = 0;
		progressBar.setString("Linkage");

		// Check if first frame has any detections
		if (dataSet.getFrameSet(0).size() == 0) {
			//System.out.println("The first frameset is size 0");
		}

		// Find the first frame that actually contains segments
		int firstSegment = dataSet.getFrameSetList().length - 1;
		for (int i = 0; i < dataSet.getFrameSetList().length; i++) {
			if(dataSet.getFrameSet(i).size() > 0) {
				firstSegment = i;
				break;
			}
		}

		// Initialize LinkSet for each segment in the first frame (all start new tracks)
		for (Segment segment: dataSet.getFrameSet(firstSegment)) {
			LinkSet linkSet = new LinkSet(segment, dataSet);
			segment.setLinkSet(linkSet);
			segment.getLinkSet().setStart(segment);
			segment.getLinkSet().setName(linkSet.getDataSet().getLinkSetNameIterator());
			serialNumber++;
		}

		// Link segments across consecutive frames
		for (int i = firstSegment; i < dataSet.getFrameSetList().length - 1; i++) {

			// Skip entirely if both frames are empty — avoids degenerate 0×0 matrices
			// in subclass implementations (e.g. recursive mode with sparse parent cells).
			if (dataSet.getFrameSet(i).isEmpty() && dataSet.getFrameSet(i + 1).isEmpty()) {
				progressBar.setValue(i);
				continue;
			}

			// Delegate frame-by-frame linking to subclass implementation
			setLinkage(i);

			// Initialize any frame i+1 segments that the subclass left unmatched.
			// This handles genuine new appearances (segments with no counterpart in frame i).
			for (Segment seg : dataSet.getFrameSet(i + 1)) {
				if (!seg.hasLinkSet()) {
					LinkSet linkSet = new LinkSet(seg, dataSet);
					seg.setLinkSet(linkSet);
					linkSet.setStart(seg);
					linkSet.setName(linkSet.getDataSet().getLinkSetNameIterator());
					serialNumber++;
				}
			}

			progressBar.setValue(i);
		}

	}
	
	/**
	 * Link segments between frames step and step+1. This abstract method is
	 * implemented by subclasses to define the specific algorithm (nearest neighbor,
	 * Hungarian, etc.) used to match segments across frames.
	 *
	 * @param step Index of the current frame; linking is to frame step+1
	 */
	public abstract void setLinkage(int step);
	
	

}
