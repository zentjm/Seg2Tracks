package pipeline;

import dataStructure.DataSet;
import util.UserInputException;

/**
 * Checks shared by the GUI analyses and batch sample matching.
 */
public final class InputChecks {

	private InputChecks() { }

	/**
	 * Requires every dataset to exist and to share width, height and frame count, which any
	 * analysis that combines panels (e.g. two channels of the same experiment) relies on.
	 *
	 * @param dataSets the inputs, in channel order
	 * @throws UserInputException describing each input's size if they differ, or naming an empty input
	 */
	public static void requireSameDimensions(DataSet... dataSets) {
		if (dataSets == null || dataSets.length == 0) return;
		for (int i = 0; i < dataSets.length; i++) {
			if (dataSets[i] == null) {
				throw new UserInputException("Input " + (i + 1) + " has no data. Run or load a "
						+ "segmentation for that panel first.");
			}
		}
		DataSet first = dataSets[0];
		boolean same = true;
		for (DataSet d : dataSets) {
			if (d.getWidth() != first.getWidth() || d.getHeight() != first.getHeight()
					|| d.getSize() != first.getSize()) {
				same = false;
				break;
			}
		}
		if (same) return;
		StringBuilder sb = new StringBuilder("The inputs must have the same width, height and number "
				+ "of frames, but they differ:\n");
		for (int i = 0; i < dataSets.length; i++) {
			DataSet d = dataSets[i];
			sb.append("\n  ").append(d.getName() != null ? d.getName() : "Input " + (i + 1))
			  .append(": ").append(d.getWidth()).append(" × ").append(d.getHeight())
			  .append(", ").append(d.getSize()).append(" frames");
		}
		throw new UserInputException(sb.toString());
	}
}
