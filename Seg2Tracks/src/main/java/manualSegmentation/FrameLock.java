package manualSegmentation;

import javax.swing.SwingUtilities;

import ij.ImageListener;
import ij.ImagePlus;

/**
 * Keeps an image on one slice while the user is drawing, so a stray scroll, arrow key or
 * mouse-wheel turn cannot move the outline being drawn onto the wrong frame.
 * <p>
 * Every slice change in ImageJ goes through {@code ImagePlus.setSlice()} →
 * {@code updateAndDraw()}, which notifies {@link ImageListener}s, so listening for updates
 * catches all navigation routes (window scrollbar, keyboard, wheel, other plugins). When the
 * locked image lands on another slice it is moved straight back.
 * <p>
 * The controller itself moves between frames with {@link #moveTo(int)}, which updates the
 * locked slice before navigating.
 */
public class FrameLock implements ImageListener {

	private ImagePlus image;
	private int lockedSlice;

	/** Locks {@code imp} to the given 1-based slice. Replaces any existing lock. */
	public void lock(ImagePlus imp, int slice) {
		unlock();
		image = imp;
		lockedSlice = slice;
		ImagePlus.addImageListener(this);
	}

	/** Moves the locked image to a new 1-based slice (e.g. Next/Previous Frame). */
	public void moveTo(int slice) {
		lockedSlice = slice;
		if (image != null) image.setSlice(slice);
	}

	/** Releases the lock. Safe to call when not locked. */
	public void unlock() {
		if (image != null) ImagePlus.removeImageListener(this);
		image = null;
	}

	public boolean isLocked() {
		return image != null;
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp != image || imp.getCurrentSlice() == lockedSlice) return;
		// Defer: we are inside ImageJ's own setSlice() call.
		SwingUtilities.invokeLater(() -> {
			if (image == imp && imp.getCurrentSlice() != lockedSlice) imp.setSlice(lockedSlice);
		});
	}

	@Override
	public void imageOpened(ImagePlus imp) { }

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == image) unlock();
	}
}
