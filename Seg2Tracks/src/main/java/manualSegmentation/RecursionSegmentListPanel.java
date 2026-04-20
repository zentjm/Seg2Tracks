package manualSegmentation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dataStructure.LinkSet;

/**
 * Sidebar panel listing all parent LinkSets as clickable buttons, docked to the
 * left side of the recursive manual segmentation image display. Buttons are
 * arranged vertically in scroll-safe layout. At the bottom, fixed "Previous" /
 * "Next" navigation buttons allow frame-by-frame iteration.
 *
 * The currently loaded cell's button is highlighted so the user always knows
 * which cell they are working on.
 */
public class RecursionSegmentListPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private RecursionManualController controller;
	private List<JButton> cellButtons;
	private JButton buttonPrevious;
	private JButton buttonNext;

	/** Width of the sidebar in pixels. */
	static final int SIDEBAR_WIDTH = 150;

	/** Background colour for the active cell button. */
	private static final Color ACTIVE_BG = new Color(100, 200, 100);
	private static final Color DEFAULT_BG = null; // system default

	private int activeCellIndex = -1;

	/**
	 * @param controller  parent controller for callbacks
	 * @param linkSets    ordered list of parent LinkSets (one button each)
	 * @param imageHeight the height of the image display — used to size this panel
	 */
	public RecursionSegmentListPanel(RecursionManualController controller,
	                                  List<LinkSet> linkSets, int imageHeight) {
		this.controller = controller;
		setLayout(new BorderLayout());

		// ── Scrollable button list ───────────────────────────────────────────
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

		cellButtons = new ArrayList<>();
		for (int i = 0; i < linkSets.size(); i++) {
			String label = "Cell " + linkSets.get(i).getDisplayName();
			JButton btn = new JButton(label);
			btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
			btn.setAlignmentX(CENTER_ALIGNMENT);
			btn.setMaximumSize(new Dimension(SIDEBAR_WIDTH - 10, 28));
			btn.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 10, 28));
			final int idx = i;
			btn.addActionListener(e -> controller.loadCellByIndex(idx));
			cellButtons.add(btn);
			listPanel.add(btn);
		}

		JScrollPane scrollPane = new JScrollPane(listPanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		// ── Previous / Next buttons (fixed at bottom) ────────────────────────
		JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
		buttonPrevious = new JButton("Prev");
		buttonNext     = new JButton("Next");
		buttonPrevious.setFont(new Font("SansSerif", Font.PLAIN, 11));
		buttonNext.setFont(new Font("SansSerif", Font.PLAIN, 11));
		buttonPrevious.addActionListener(this);
		buttonNext.addActionListener(this);
		navPanel.add(buttonPrevious);
		navPanel.add(buttonNext);
		add(navPanel, BorderLayout.SOUTH);

		// Size the sidebar to match image height
		int panelHeight = Math.max(imageHeight, 200);
		setPreferredSize(new Dimension(SIDEBAR_WIDTH, panelHeight));
		setMinimumSize(new Dimension(SIDEBAR_WIDTH, 200));
	}

	/**
	 * Enables or disables all cell-switching controls in the sidebar.
	 *
	 * Call with {@code false} when the user enters segmentation mode
	 * (the "Segmentation" button is pressed) to prevent navigating away while
	 * a void is being drawn.  Call with {@code true} when the segmentation is
	 * completed ("End Object") or aborted ("Main Menu").
	 *
	 * The Previous / Next navigation buttons respect the same gate.
	 * When re-enabling, Previous/Next are restored to their normal
	 * enabled state based on the active cell index rather than unconditionally.
	 */
	public void setCellSwitchingEnabled(boolean enabled) {
		for (JButton btn : cellButtons) {
			btn.setEnabled(enabled);
		}
		if (enabled) {
			// Restore nav-button state that setActiveCell() would have set
			buttonPrevious.setEnabled(activeCellIndex > 0);
			buttonNext.setEnabled(activeCellIndex < cellButtons.size() - 1);
		} else {
			buttonPrevious.setEnabled(false);
			buttonNext.setEnabled(false);
		}
	}

	/**
	 * Highlights the button for the given cell index and dims the rest.
	 */
	public void setActiveCell(int index) {
		activeCellIndex = index;
		for (int i = 0; i < cellButtons.size(); i++) {
			JButton btn = cellButtons.get(i);
			if (i == index) {
				btn.setBackground(ACTIVE_BG);
				btn.setOpaque(true);
			} else {
				btn.setBackground(DEFAULT_BG);
				btn.setOpaque(false);
			}
			btn.repaint();
		}
		// Enable/disable nav buttons
		buttonPrevious.setEnabled(index > 0);
		buttonNext.setEnabled(index < cellButtons.size() - 1);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonPrevious && activeCellIndex > 0) {
			controller.loadCellByIndex(activeCellIndex - 1);
		}
		if (e.getSource() == buttonNext && activeCellIndex < cellButtons.size() - 1) {
			controller.loadCellByIndex(activeCellIndex + 1);
		}
	}
}
