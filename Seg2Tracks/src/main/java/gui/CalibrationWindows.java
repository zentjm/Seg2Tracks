package gui;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ij.ImagePlus;

/**
 * Window placement for the Guided Calibration dialogs (Object Identification and Boundary
 * Cleanup), which each pair a control panel with an ImageJ preview window.
 * <p>
 * ImageJ opens the preview at its own default location after the control panel is shown, so it
 * used to cover the controls. Here the control panel goes on the left and the preview to its
 * right, top-aligned; if the screen is too narrow for both, the preview is staggered down and
 * right of the panel instead. Either way the control panel is brought to the front.
 */
final class CalibrationWindows {

	private static final int GAP = 12;
	private static final int STAGGER = 60;
	private static final int MARGIN = 20;

	private CalibrationWindows() { }

	/** Places the control panel on the left of the usable screen area, vertically centred. */
	static void placeControls(JFrame controls) {
		Rectangle screen = screen();
		int y = screen.y + Math.max(MARGIN, (screen.height - controls.getHeight()) / 3);
		controls.setLocation(screen.x + MARGIN, y);
	}

	/**
	 * Positions the preview window beside (or staggered from) the control panel and brings the
	 * control panel to the front. Call once, right after the preview is first shown.
	 */
	static void arrange(JFrame controls, ImagePlus preview) {
		SwingUtilities.invokeLater(() -> {
			Window win = preview.getWindow();
			if (win != null && controls.isShowing()) {
				Rectangle screen = screen();
				Rectangle c = controls.getBounds();
				int beside = c.x + c.width + GAP;
				if (beside + win.getWidth() <= screen.x + screen.width) {
					win.setLocation(beside, c.y);
				} else {
					int x = Math.min(c.x + STAGGER, screen.x + screen.width - win.getWidth());
					int y = Math.min(c.y + STAGGER, screen.y + screen.height - win.getHeight());
					win.setLocation(Math.max(screen.x, x), Math.max(screen.y, y));
				}
			}
			controls.toFront();
		});
	}

	private static Rectangle screen() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	}
}
