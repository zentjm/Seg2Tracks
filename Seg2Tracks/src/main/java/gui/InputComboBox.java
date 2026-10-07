package gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.UIManager;

/**
 * Analysis input dropdown whose entries can be individually unavailable: unavailable entries
 * stay in the list (so the list matches the panels) but are drawn greyed out and cannot be
 * selected, by mouse or keyboard.
 * <p>
 * Used for recursive analyses, which need a subsegmentation dataset: regular datasets are
 * listed but greyed. Swing has no per-item enable flag, so this is done with a renderer and a
 * {@link #setSelectedIndex} guard.
 */
class InputComboBox extends JComboBox<String> {

	private static final long serialVersionUID = 1L;

	private final boolean[] available;

	/**
	 * @param items     entry labels
	 * @param available per-entry availability (same length as {@code items}); null = all
	 */
	InputComboBox(String[] items, boolean[] available) {
		super(items);
		this.available = available;
		setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				boolean ok = index < 0 || isAvailable(index);
				super.getListCellRendererComponent(list, value, index, isSelected && ok, cellHasFocus && ok);
				if (!ok) setForeground(UIManager.getColor("Label.disabledForeground"));
				return this;
			}
		});
	}

	boolean isAvailable(int index) {
		return available == null || index < 0 || index >= available.length || available[index];
	}

	/** Index of the first available entry, or -1 if none is available. */
	int firstAvailable() {
		for (int i = 0; i < getItemCount(); i++) if (isAvailable(i)) return i;
		return -1;
	}

	/** Ignores attempts to select an unavailable entry (mouse click or arrow keys). */
	@Override
	public void setSelectedIndex(int index) {
		if (!isAvailable(index)) return;
		super.setSelectedIndex(index);
	}

	/** Same guard for the selection paths in Swing that select by item rather than index. */
	@Override
	public void setSelectedItem(Object item) {
		for (int i = 0; i < getItemCount(); i++) {
			if (getItemAt(i) == item) {
				if (!isAvailable(i)) return;
				break;
			}
		}
		super.setSelectedItem(item);
	}
}
