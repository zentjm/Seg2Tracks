package manualSegmentation;

import java.awt.BasicStroke;
import java.awt.Color;

import ij.gui.Overlay;
import ij.gui.Roi;

/**
 * The old outline shown during a Redraw Segment edit: a dashed, non-editable copy in a
 * colour that stands out from both the normal ROI colour and the selection colour.
 * <p>
 * The real outline is taken out of the overlay while the redraw is armed and only the copy is
 * shown, so nothing the user does can alter the stored outline. On Apply the copy is simply
 * removed; on Cancel the real outline is put back.
 * <p>
 * Overlay membership is checked by object identity, never with {@code Overlay.contains()} /
 * {@code Overlay.remove(Roi)}: those use {@code Roi.equals()}, which only compares type,
 * bounds and length, so the reference copy is "equal" to the original.
 */
public class RedrawReference {

	private static final Color[] CANDIDATES = {
		Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.ORANGE, Color.WHITE,
	};
	private static final BasicStroke DASHED = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 10f, new float[] { 6f, 4f }, 0f);

	private Overlay overlay;
	private Roi original;
	private Roi reference;

	/**
	 * Replaces {@code original} in {@code overlay} with a dashed reference copy on the given
	 * 1-based slice.
	 *
	 * @param avoid colours the reference should contrast with (ROI colour, selection colour)
	 */
	public void show(Overlay overlay, Roi original, int slice, Color... avoid) {
		this.overlay = overlay;
		this.original = original;
		removeExact(overlay, original);
		reference = (Roi) original.clone();
		reference.setStroke(DASHED);
		reference.setStrokeColor(contrasting(avoid));
		reference.setPosition(slice);
		overlay.add(reference);
	}

	/** Removes the reference copy, leaving the original out of the overlay (Apply). */
	public void clear() {
		if (overlay != null && reference != null) removeExact(overlay, reference);
		overlay = null;
		original = null;
		reference = null;
	}

	/** Removes the reference copy and puts the original outline back (Cancel). */
	public void restore() {
		if (overlay != null && reference != null) removeExact(overlay, reference);
		if (overlay != null && original != null && indexOfExact(overlay, original) < 0) overlay.add(original);
		overlay = null;
		original = null;
		reference = null;
	}

	/** Index of this exact Roi object in the overlay, or -1. */
	static int indexOfExact(Overlay overlay, Roi roi) {
		for (int i = 0; i < overlay.size(); i++) {
			if (overlay.get(i) == roi) return i;
		}
		return -1;
	}

	/** Removes this exact Roi object (not an "equal" one) from the overlay, if present. */
	static void removeExact(Overlay overlay, Roi roi) {
		int i = indexOfExact(overlay, roi);
		if (i >= 0) overlay.remove(i);
	}

	/** The candidate colour farthest (in RGB) from the nearest of the colours to avoid. */
	private static Color contrasting(Color... avoid) {
		Color best = CANDIDATES[0];
		double bestScore = -1;
		for (Color c : CANDIDATES) {
			double nearest = Double.MAX_VALUE;
			for (Color a : avoid) {
				if (a == null) continue;
				double dr = c.getRed() - a.getRed(), dg = c.getGreen() - a.getGreen(), db = c.getBlue() - a.getBlue();
				nearest = Math.min(nearest, dr * dr + dg * dg + db * db);
			}
			if (nearest > bestScore) { bestScore = nearest; best = c; }
		}
		return best;
	}
}
