package manualSegmentation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Non-interactive panel that shows the full source image scaled to fit,
 * with the current parent cell outlined for context. The displayed frame
 * is synchronised with the cropped cell image above it.
 */
public class RecursionPreviewPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private ImageStack fullStack;
	private LinkSet parentCell;
	private int currentFrame; // 0-based full-stack frame index

	private static final Color PARENT_COLOR = new Color(0, 255, 0, 200); // green

	/**
	 * @param fullStack    the full source image stack (all frames)
	 * @param parentCell   the parent LinkSet whose outline to draw
	 * @param initialFrame 0-based full-stack frame to display initially
	 * @param targetWidth  desired pixel width of the preview
	 */
	public RecursionPreviewPanel(ImageStack fullStack, LinkSet parentCell,
	                              int initialFrame, int targetWidth) {
		this.fullStack = fullStack;
		this.parentCell = parentCell;
		this.currentFrame = initialFrame;

		
		//TODO: needs dynamic target width handling. 
		double scale = (double) targetWidth / fullStack.getWidth();
		int scaledHeight = (int) (fullStack.getHeight() * scale);
		setPreferredSize(new Dimension(targetWidth, scaledHeight));
	}

	/** Update the displayed frame and repaint. */
	public void setFrame(int fullStackFrame) {
		if (fullStackFrame < 0) fullStackFrame = 0;
		if (fullStackFrame >= fullStack.getSize()) fullStackFrame = fullStack.getSize() - 1;
		this.currentFrame = fullStackFrame;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (fullStack == null || fullStack.getSize() == 0) return;

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		// Scale to fit panel while preserving aspect ratio
		double scaleX = (double) getWidth()  / fullStack.getWidth();
		double scaleY = (double) getHeight() / fullStack.getHeight();
		double sc = Math.min(scaleX, scaleY);

		int drawW = (int) (fullStack.getWidth()  * sc);
		int drawH = (int) (fullStack.getHeight() * sc);
		int offX  = (getWidth()  - drawW) / 2;
		int offY  = (getHeight() - drawH) / 2;

		// Draw the full image frame
		ImageProcessor ip = fullStack.getProcessor(currentFrame + 1); // 1-based
		java.awt.Image img = ip.createImage();
		g2.drawImage(img, offX, offY, drawW, drawH, this);

		// Draw parent cell outline for this frame
		g2.setColor(PARENT_COLOR);
		g2.setStroke(new BasicStroke(2));
		for (Segment seg : parentCell) {
			if (seg.getFrame() != currentFrame) continue;
			Point[] perim = seg.getInternalPerimeter();
			if (perim == null || perim.length < 2) continue;

			int[] xp = new int[perim.length];
			int[] yp = new int[perim.length];
			for (int i = 0; i < perim.length; i++) {
				xp[i] = offX + (int) (perim[i].x * sc);
				yp[i] = offY + (int) (perim[i].y * sc);
			}
			g2.drawPolygon(xp, yp, perim.length);
		}
	}
}
