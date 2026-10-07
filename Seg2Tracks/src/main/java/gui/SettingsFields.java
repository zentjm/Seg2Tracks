package gui;

import java.awt.Color;
import java.math.BigDecimal;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Shared helpers for settings dialogs whose text boxes are the single path into the stored
 * settings (Object Identification Settings, Boundary Cleanup Settings).
 * <p>
 * The pattern: the boxes show the stored values; editing them (by hand, or by a Guided
 * Calibration sending its slider values) only changes the boxes; <b>Apply</b> is enabled only
 * while the boxes differ from what is stored and hold valid numbers, and pushes them to the
 * controller. A status line says whether there are unapplied changes or an invalid value.
 */
final class SettingsFields {

	static final String PENDING = "Changes not applied yet";
	private static final Color PENDING_COLOR = new Color(0x99, 0x66, 0x00);

	private SettingsFields() { }

	/** Formats a value for a text box without floating-point noise (0.07*100 → "7"). */
	static String format(double value) {
		double rounded = Math.round(value * 1e6) / 1e6;
		return BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString();
	}

	/** The box's value as a number, or {@code fallback} if it does not parse. */
	static double parseOr(JTextField field, double fallback) {
		try { return Double.parseDouble(field.getText().trim()); }
		catch (NumberFormatException e) { return fallback; }
	}

	/** True if two settings values are equal to within display precision. */
	static boolean same(double a, double b) {
		return Math.abs(a - b) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
	}

	/** Calls {@code onChange} whenever the text of any of the fields changes. */
	static void onEdit(Runnable onChange, JTextField... fields) {
		DocumentListener listener = new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e)  { onChange.run(); }
			@Override public void removeUpdate(DocumentEvent e)  { onChange.run(); }
			@Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
		};
		for (JTextField f : fields) f.getDocument().addDocumentListener(listener);
	}

	static void showPending(JLabel label) {
		label.setForeground(PENDING_COLOR);
		label.setText(PENDING);
	}

	static void showError(JLabel label, String message) {
		label.setForeground(Color.RED);
		label.setText(message);
	}

	static void clear(JLabel label) {
		label.setText(" ");
	}
}
