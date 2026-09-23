package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Help menu window that renders the bundled UserManual.md as scrollable HTML.
 *
 * The manual is loaded from the classpath resource /UserManual.md and converted
 * from Markdown to HTML by a lightweight line-by-line parser.  Anchor links
 * (e.g., Table of Contents entries) are functional via HyperlinkListener +
 * scrollToReference().
 *
 * Markdown features supported:
 *   # / ## / ###  headings (with auto-generated name anchors)
 *   **bold**  *italic*  `inline code`  [text](#anchor) links
 *   ``` fenced code blocks
 *   --- horizontal rules
 *   > blockquotes (consecutive > lines merged into one blockquote)
 *   - / * bullet lists   1. ordered lists
 *   | tables | with | header row |
 */
public class HelpMenuPanel extends JFrame implements ActionListener {

	private static final int WINDOW_WIDTH  = 860;
	private static final int WINDOW_HEIGHT = 720;

	private JEditorPane editorPane;
	private JButton     buttonClose = new JButton("Close");

	/** Classpath resource (Markdown) rendered by this window, e.g. {@code /UserManual.md}. */
	private final String resourcePath;

	/**
	 * Opens a help window rendering the given Markdown classpath resource.
	 *
	 * @param title        window title
	 * @param resourcePath classpath resource path, e.g. {@code /UserManual.md} or {@code /ChangeLog.md}
	 */
	public HelpMenuPanel(String title, String resourcePath) {
		super(title);
		this.resourcePath = resourcePath;
		createView();
	}

	public void createView() {
		setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		setMinimumSize(new Dimension(500, 400));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Use HTMLEditorKit for CSS + anchor support
		HTMLEditorKit kit = new HTMLEditorKit();
		editorPane = new JEditorPane();
		editorPane.setEditorKit(kit);
		editorPane.setContentType("text/html");
		editorPane.setText(loadManualHtml());
		editorPane.setEditable(false);
		editorPane.setCaretPosition(0);   // scroll to top after load

		// Handle #anchor links from the Table of Contents
		editorPane.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				String desc = e.getDescription();
				if (desc != null && desc.startsWith("#")) {
					editorPane.scrollToReference(desc.substring(1));
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(editorPane,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		buttonClose.addActionListener(this);
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
		bottomPanel.add(buttonClose);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	// -------------------------------------------------------------------------
	// Resource loading
	// -------------------------------------------------------------------------

	private String loadManualHtml() {
		InputStream is = HelpMenuPanel.class.getResourceAsStream(resourcePath);
		if (is == null) {
			return "<html><body><p><b>Help file not found.</b> "
				 + esc(resourcePath) + " was not found on the classpath.</p></body></html>";
		}
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			reader.close();
			return markdownToHtml(sb.toString());
		} catch (IOException ex) {
			return "<html><body><p>Error loading help: " + esc(ex.getMessage()) + "</p></body></html>";
		}
	}

	// -------------------------------------------------------------------------
	// Markdown → HTML converter
	// -------------------------------------------------------------------------

	private String markdownToHtml(String md) {
		String[] lines = md.split("\n", -1);
		StringBuilder html = new StringBuilder();

		// Stylesheet — conservative properties supported by Java's HTMLEditorKit
		html.append("<html><head><style>");
		html.append("body{font-family:sans-serif;margin:20px 28px;font-size:13px;}");
		html.append("h1{font-size:20px;}");
		html.append("h2{font-size:16px;}");
		html.append("h3{font-size:14px;}");
		html.append("pre{background:#f5f5f5;padding:10px 14px;font-size:11px;font-family:monospace;}");
		html.append("code{background:#f0f0f0;font-family:monospace;font-size:11px;}");
		html.append("table{border-collapse:collapse;margin:8px 0;}");
		html.append("th,td{border:1px solid #bbb;padding:5px 12px;}");
		html.append("th{background-color:#e8e8e8;font-weight:bold;}");
		html.append("blockquote{margin:8px 0;padding:6px 14px;background:#f5f5f5;color:#444;}");
		html.append("ul,ol{padding-left:28px;}");
		html.append("li{margin:3px 0;}");
		html.append("a{color:#0066cc;}");
		html.append("</style></head><body>");

		boolean inCodeBlock    = false;
		boolean inTable        = false;
		boolean tableHasHeader = false;
		boolean inList         = false;
		boolean inOrderedList  = false;
		boolean inBlockquote   = false;
		boolean inParagraph    = false;

		for (String line : lines) {
			String trimmed = line.trim();

			// ── Fenced code block ──────────────────────────────────────────
			if (trimmed.startsWith("```")) {
				if (!inCodeBlock) {
					if (inList)        { html.append("</ul>");           inList        = false; }
					if (inOrderedList) { html.append("</ol>");           inOrderedList = false; }
					if (inBlockquote)  { html.append("</blockquote>");   inBlockquote  = false; }
					if (inParagraph)   { html.append("</p>");            inParagraph   = false; }
					if (inTable)       { html.append("</table>");        inTable       = false; tableHasHeader = false; }
					html.append("<pre><code>");
					inCodeBlock = true;
				} else {
					html.append("</code></pre>");
					inCodeBlock = false;
				}
				continue;
			}

			if (inCodeBlock) {
				html.append(esc(line)).append("\n");
				continue;
			}

			// ── Table separator row (|---|---| etc.) — skip ───────────────
			if (trimmed.matches("\\|[-| :]+\\|")) {
				continue;
			}

			// ── Table row ────────────────────────────────────────────────
			if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
				if (inList)        { html.append("</ul>");         inList        = false; }
				if (inOrderedList) { html.append("</ol>");         inOrderedList = false; }
				if (inBlockquote)  { html.append("</blockquote>"); inBlockquote  = false; }
				if (inParagraph)   { html.append("</p>");          inParagraph   = false; }

				if (!inTable) {
					html.append("<table>");
					inTable = true;
					tableHasHeader = false;
				}
				boolean isHeader = !tableHasHeader;
				tableHasHeader = true;

				html.append("<tr>");
				String[] cols = trimmed.split("\\|", -1);
				for (int c = 1; c < cols.length - 1; c++) {
					String cell = cols[c].trim();
					if (isHeader) {
						html.append("<th>").append(inline(cell)).append("</th>");
					} else {
						html.append("<td>").append(inline(cell)).append("</td>");
					}
				}
				html.append("</tr>");
				continue;
			} else if (inTable) {
				html.append("</table>");
				inTable        = false;
				tableHasHeader = false;
			}

			// ── Blank line ────────────────────────────────────────────────
			if (trimmed.isEmpty()) {
				if (inList)        { html.append("</ul>");         inList        = false; }
				if (inOrderedList) { html.append("</ol>");         inOrderedList = false; }
				if (inBlockquote)  { html.append("</blockquote>"); inBlockquote  = false; }
				if (inParagraph)   { html.append("</p>");          inParagraph   = false; }
				continue;
			}

			// ── Horizontal rule ───────────────────────────────────────────
			if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
				if (inList)        { html.append("</ul>");         inList        = false; }
				if (inOrderedList) { html.append("</ol>");         inOrderedList = false; }
				if (inBlockquote)  { html.append("</blockquote>"); inBlockquote  = false; }
				if (inParagraph)   { html.append("</p>");          inParagraph   = false; }
				html.append("<hr/>");
				continue;
			}

			// ── Headings ─────────────────────────────────────────────────
			if (line.startsWith("### ")) {
				closeAll(html, inList, inOrderedList, inBlockquote, inParagraph);
				inList = false; inOrderedList = false; inBlockquote = false; inParagraph = false;
				String text   = line.substring(4);
				String anchor = headingToAnchor(text);
				html.append("<h3><a name=\"").append(anchor).append("\">")
				    .append(inline(text)).append("</a></h3>");
				continue;
			}
			if (line.startsWith("## ")) {
				closeAll(html, inList, inOrderedList, inBlockquote, inParagraph);
				inList = false; inOrderedList = false; inBlockquote = false; inParagraph = false;
				String text   = line.substring(3);
				String anchor = headingToAnchor(text);
				html.append("<h2><a name=\"").append(anchor).append("\">")
				    .append(inline(text)).append("</a></h2>");
				continue;
			}
			if (line.startsWith("# ")) {
				closeAll(html, inList, inOrderedList, inBlockquote, inParagraph);
				inList = false; inOrderedList = false; inBlockquote = false; inParagraph = false;
				String text   = line.substring(2);
				String anchor = headingToAnchor(text);
				html.append("<h1><a name=\"").append(anchor).append("\">")
				    .append(inline(text)).append("</a></h1>");
				continue;
			}

			// ── Blockquote (consecutive > lines merged) ───────────────────
			if (trimmed.startsWith("> ")) {
				if (inList)        { html.append("</ul>");   inList        = false; }
				if (inOrderedList) { html.append("</ol>");   inOrderedList = false; }
				if (inParagraph)   { html.append("</p>");    inParagraph   = false; }
				if (!inBlockquote) { html.append("<blockquote>"); inBlockquote = true; }
				else               { html.append(" "); }
				html.append(inline(trimmed.substring(2)));
				continue;
			} else if (inBlockquote) {
				html.append("</blockquote>");
				inBlockquote = false;
			}

			// ── Bullet list ───────────────────────────────────────────────
			if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
				if (inOrderedList) { html.append("</ol>"); inOrderedList = false; }
				if (inParagraph)   { html.append("</p>");  inParagraph   = false; }
				if (!inList)       { html.append("<ul>");  inList        = true;  }
				html.append("<li>").append(inline(trimmed.substring(2))).append("</li>");
				continue;
			}

			// ── Ordered list ──────────────────────────────────────────────
			if (trimmed.matches("\\d+\\.\\s+.*")) {
				if (inList)     { html.append("</ul>"); inList     = false; }
				if (inParagraph){ html.append("</p>");  inParagraph = false; }
				if (!inOrderedList) { html.append("<ol>"); inOrderedList = true; }
				String content = trimmed.replaceFirst("\\d+\\.\\s+", "");
				html.append("<li>").append(inline(content)).append("</li>");
				continue;
			}

			// ── Paragraph text ────────────────────────────────────────────
			if (inList)        { html.append("</ul>"); inList        = false; }
			if (inOrderedList) { html.append("</ol>"); inOrderedList = false; }
			if (!inParagraph)  { html.append("<p>");   inParagraph   = true;  }
			else               { html.append(" "); }
			html.append(inline(line));
		}

		// Close any remaining open blocks
		if (inCodeBlock)   html.append("</code></pre>");
		if (inTable)       html.append("</table>");
		if (inList)        html.append("</ul>");
		if (inOrderedList) html.append("</ol>");
		if (inBlockquote)  html.append("</blockquote>");
		if (inParagraph)   html.append("</p>");

		html.append("</body></html>");
		return html.toString();
	}

	/** Emits closing tags for any open block-level elements (called before headings). */
	private void closeAll(StringBuilder html,
			boolean inList, boolean inOrderedList,
			boolean inBlockquote, boolean inParagraph) {
		if (inList)        html.append("</ul>");
		if (inOrderedList) html.append("</ol>");
		if (inBlockquote)  html.append("</blockquote>");
		if (inParagraph)   html.append("</p>");
	}

	/**
	 * Applies inline Markdown transforms to a span of plain text.
	 * Order: HTML-escape → bold → italic → inline-code → links.
	 */
	private String inline(String text) {
		text = esc(text);
		// Bold (**text**)
		text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
		// Italic (*text*) — after bold so ** is already consumed
		text = text.replaceAll("\\*([^*<>]+?)\\*", "<i>$1</i>");
		// Inline code (`text`)
		text = text.replaceAll("`([^`]+?)`", "<code>$1</code>");
		// Links [label](#anchor) or [label](url)
		text = text.replaceAll("\\[([^\\]]+?)\\]\\(([^)]+?)\\)", "<a href=\"$2\">$1</a>");
		return text;
	}

	/** HTML-escapes &, <, > characters. */
	private String esc(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Converts a heading text to a GitHub-style anchor ID:
	 *   lowercase → remove non-[a-z0-9 -] → replace spaces with hyphens.
	 * Matches the anchor format used in the Table of Contents links.
	 */
	private String headingToAnchor(String text) {
		// Strip inline markdown formatting first
		String plain = text
			.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
			.replaceAll("\\*(.+?)\\*",        "$1")
			.replaceAll("`(.+?)`",             "$1")
			.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
		return plain.toLowerCase()
			.replaceAll("[^a-z0-9 \\-]", "")
			.replace(' ', '-');
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonClose) dispose();
	}
}
