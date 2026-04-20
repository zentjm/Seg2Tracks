package manualSegmentation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

import ij.ImagePlus;

/**
 * Composite JFrame that docks the segment-list sidebar to the left of the
 * ImageJ image display for recursive manual segmentation. The sidebar stays
 * pinned when cells are switched; only the image panel contents change.
 *
 * Layout:
 *   ┌──────────┬────────────────────────────────────┐
 *   │ Sidebar  │   Image Display (StackWindow)      │
 *   │ (scroll) │                                    │
 *   │          │                                    │
 *   │  [Prev]  │                                    │
 *   │  [Next]  │                                    │
 *   └──────────┴────────────────────────────────────┘
 */
public class RecursionCompositeWindow extends JFrame {

	private static final long serialVersionUID = 1L;

	private RecursionSegmentListPanel sidebar;
	private JPanel imagePanel;

	/**
	 * @param title   window title
	 * @param sidebar the segment list sidebar
	 * @param imageWidth  pixel width of the image canvas area
	 * @param imageHeight pixel height of the image canvas area
	 */
	public RecursionCompositeWindow(String title,
	                                 RecursionSegmentListPanel sidebar,
	                                 int imageWidth, int imageHeight) {
		super(title);
		this.sidebar = sidebar;

		setLayout(new BorderLayout());

		// Sidebar on the left
		add(sidebar, BorderLayout.WEST);

		// Image panel on the right — canvas, scrollbar, and preview are embedded
		// here when a cell is loaded. No fixed preferredSize: pack() sizes from content.
		imagePanel = new JPanel(new BorderLayout());
		add(imagePanel, BorderLayout.CENTER);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Returns the panel into which the ImageJ StackWindow's content should
	 * be placed.
	 */
	public JPanel getImagePanel() {
		return imagePanel;
	}

	/**
	 * Returns the sidebar so the controller can update the active-cell
	 * highlight.
	 */
	public RecursionSegmentListPanel getSidebar() {
		return sidebar;
	}
}
