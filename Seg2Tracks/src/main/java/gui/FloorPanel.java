package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The bottom control panel of the main Seg2Tracks window. Displays progress bar, navigation buttons
 * for switching between operation and analysis views, panel management buttons (add/remove), and
 * a button for exporting final results (overlays and measurement data).
 */
public class FloorPanel extends JPanel implements ActionListener, ChangeListener {

	GridBagConstraints constraints = new GridBagConstraints();
	
	Seg2TracksController controller;
	
	// ── Status line + progress bar ────────────────────────────────────────────
	// One bar serves every panel. The bar itself carries no text; the status label beside it
	// shows what is running ("Set 2 — Segmentation", mirrored from the bar's string, which the
	// pipeline stages set) and the final result in colour ("✓ Set 2: Operation Complete"),
	// which stays until the next button press in a Seg2Tracks panel; then "Ready".

	/** How a finished run ended; sets the status line's colour and symbol. */
	public enum Outcome { COMPLETE, FAILED, CANCELLED }

	private static final Color COMPLETE_COLOR = new Color(0, 150, 0);
	private static final Color FAILED_COLOR   = new Color(200, 0, 0);
	/** Longest expected status text; sizes the label so the layout doesn't jump as text changes. */
	private static final String WIDEST_STATUS = "\u2713 Set 10 — Retrieving segment aggregation data";

	JLabel statusLabel = new JLabel("Ready");

	JProgressBar progressBar = new JProgressBar();

	/** Panel whose run is in progress (prefixes the stage text), or "" when idle. */
	private String runningPanel = "";
	/** True while the status line shows a finished result rather than a run or "Ready". */
	private boolean showingResult = false;
	
	//Help Button
	JButton buttonAddPanel = new JButton("Add Panel");
	JButton buttonRemovePanel = new JButton("Remove Panel");
	JButton buttonAnalyzeMenu = new JButton ("DATA ANALYSIS >>");
	JButton buttonSegmentationMenu = new JButton ("<< SEGMENTATION");
	JButton buttonGenerateResults  = new JButton("GENERATE RESULTS");
	JLabel  resultsExportedLabel  = new JLabel("");

	public FloorPanel(Seg2TracksController controller) {
		this.controller = controller;
		// GridBag, which the constraints below were always written for (the panel used to fall
		// back to FlowLayout, which ignores them, so the progress bar could not stretch).
		setLayout(new GridBagLayout());
		constraints.insets = new Insets(4, 5, 4, 5);
		initialize();
		createView();
	}

	//initializes necessary functions
	public void initialize() {
		progressBar.setValue(0);
		progressBar.setStringPainted(false); // text lives in statusLabel
		FontMetrics fm = statusLabel.getFontMetrics(statusLabel.getFont());
		Dimension size = new Dimension(fm.stringWidth(WIDEST_STATUS) + 12, statusLabel.getPreferredSize().height);
		statusLabel.setPreferredSize(size);
		statusLabel.setMinimumSize(size);
		// Pipeline stages report progress by setting the bar's string (often off the EDT).
		progressBar.addPropertyChangeListener("string", e -> {
			String stage = (String) e.getNewValue();
			SwingUtilities.invokeLater(() -> showStage(stage));
		});
		showIdle();
	}

	// ── Status line ───────────────────────────────────────────────────────────

	/** Clears the bar and names the panel whose run is starting. Call on the EDT. */
	public void startRun(String panelName) {
		runningPanel = (panelName == null) ? "" : panelName;
		showingResult = false;
		progressBar.setValue(progressBar.getMinimum());
		progressBar.setString("");
		setStatus(runningPanel.isEmpty() ? "Starting" : runningPanel + " \u2014 Starting", null);
	}

	/** Shows a stage of the running job ("Set 2 — Segmentation"). Ignored once a result is shown. */
	private void showStage(String stage) {
		if (showingResult || stage == null || stage.trim().isEmpty()) return;
		setStatus(runningPanel.isEmpty() ? stage : runningPanel + " \u2014 " + stage, null);
	}

	/** Shows a finished run's result in colour; it stays until {@link #clearResult()}. Call on the EDT. */
	public void showResult(String panelName, String result, Outcome outcome) {
		showingResult = true;
		runningPanel = "";
		if (outcome == Outcome.COMPLETE) progressBar.setValue(progressBar.getMaximum());
		String text = panelName + ": " + result;
		switch (outcome) {
			case COMPLETE:  setStatus(symbol('\u2713') + text, COMPLETE_COLOR); break;
			case FAILED:    setStatus(symbol('\u2715') + text, FAILED_COLOR); break;
			default:        setStatus(text, UIManager.getColor("Label.disabledForeground")); break;
		}
	}

	/** Called on a button press in any Seg2Tracks panel: a shown result gives way to "Ready". */
	public void clearResult() {
		if (showingResult) showIdle();
	}

	/** Empty bar, "Ready". */
	public void showIdle() {
		showingResult = false;
		runningPanel = "";
		progressBar.setValue(progressBar.getMinimum());
		progressBar.setString("");
		setStatus("Ready", UIManager.getColor("Label.disabledForeground"));
	}

	private void setStatus(String text, Color color) {
		statusLabel.setText(text);
		statusLabel.setToolTipText(text); // full text if it is ever truncated
		statusLabel.setForeground(color != null ? color : UIManager.getColor("Label.foreground"));
	}

	/** The symbol plus a space, or "" if the label's font cannot draw it (some Windows fonts). */
	private String symbol(char c) {
		return statusLabel.getFont().canDisplay(c) ? c + " " : "";
	}
	
	public JProgressBar getProgressBar() {
		return progressBar; 
	}
	
	public void createView() {
		
		//Constraint constants
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
	    
        //Add action listeners
        buttonAnalyzeMenu.addActionListener(this);	
        buttonSegmentationMenu.addActionListener(this);	
        buttonGenerateResults.addActionListener(this);	
        buttonAddPanel.addActionListener(this);
        buttonRemovePanel.addActionListener(this);
        progressBar.addChangeListener(this);
        switchToOperation();
	}
	
	public void allowAnalysisButton(boolean allow) {
		buttonAnalyzeMenu.setEnabled(allow);
	}
	
	public void allowResultsButton(boolean allow) {
		buttonGenerateResults.setEnabled(allow);
	}
	
	public void switchToOperation() {
		removeAll();
		
		//ROW 0
		constraints.gridy = 0;
		
		//Add operation panel
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(buttonAddPanel, constraints);
		
		//Remove operation panel
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(buttonRemovePanel, constraints);
		
		//Progress bar
		constraints.gridx = 2;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(statusLabel, constraints);
		
		//Progress bar: takes the spare width
        constraints.gridx = 3;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        add(progressBar, constraints);
        constraints.weightx = 0;
        
        //Help button
        constraints.gridx = 4;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonAnalyzeMenu, constraints);
        
        //Reset analysis menu button
        buttonAnalyzeMenu.setEnabled(false);
        
        //Reset progress bar and status line
        showIdle();
	}

	public void switchToAnalysis() {
		
		removeAll();
		
		//ROW 0
		constraints.gridy = 0;
		 
		//Progress bar
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.LINE_START;
	    add(statusLabel, constraints);
		
		//Progress bar: takes the spare width
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        add(progressBar, constraints);
        constraints.weightx = 0;
        
        //Help button
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonSegmentationMenu, constraints);
       
        constraints.gridx = 3;
        constraints.anchor = GridBagConstraints.LINE_END;
        add(buttonGenerateResults, constraints);

        constraints.gridx = 4;
        add(resultsExportedLabel, constraints);

        //Reset generate results menu button
        buttonGenerateResults.setEnabled(false);
        resultsExportedLabel.setText("");
        
        //Reset progress bar and status line
        showIdle();
        
        repaint();
        revalidate();
        
	}
	
	//Allows the generate-results only if acceptable conditions.
	
	public void allowPanelRemoval(boolean allow) {
		buttonRemovePanel.setEnabled(allow);
	}
	
	@Override
	public void actionPerformed(ActionEvent e ) {
		clearResult(); // a button press starts a new action
		if (e.getSource() == buttonAnalyzeMenu) controller.switchToAnalysis();	
		if (e.getSource() == buttonSegmentationMenu) controller.switchToOperation();
		if (e.getSource() == buttonGenerateResults) {
			controller.exportResults();
			buttonGenerateResults.setEnabled(false);
			resultsExportedLabel.setText("Results exported.");
		}
		if (e.getSource() == buttonAddPanel) controller.addOperationPanel();
		if (e.getSource() == buttonRemovePanel) controller.removeOperationPanel();
		
		//get source progressbar
	}

	@Override
	public void stateChanged(ChangeEvent e) {
	}
	
	
}
