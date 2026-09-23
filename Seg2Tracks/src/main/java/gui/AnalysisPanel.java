package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import util.FileSelectionPanel;
import util.FileType;

/**
 * View component for the analysis panel. Displays UI controls for selecting analysis methods,
 * specifying target files, mapping input channels to datasets, and running analysis on segmented images.
 * Observes FileSelectionPanel changes and notifies the controller of user actions.
 *
 * Layout (column alignment is shared with the main GridBagLayout):
 *   ROW 0  — panel title
 *   ROW 1  — target file selection
 *   ROW 2  — [Operation:] [method combo] [Settings]
 *   ROW 3+ — one row per channel: [channel name] [dataset combo] (Run + status on the last row)
 *
 * Channel rows are rebuilt dynamically by updateChannelPanel() whenever the selected method
 * changes or new datasets become available.  The parent window is re-packed automatically.
 */
public class AnalysisPanel extends JPanel implements ActionListener, Observer {

	GridBagConstraints constraints = new GridBagConstraints();

	AnalysisController controller;
	AnalysisModel model;
	int panelNumber;

	//Buttons
	JButton buttonTarget;
	JButton buttonRun = new JButton("Run");
	JButton buttonSettings = new JButton("Settings");

	//Channel names supplied by the selected AnalysisMethod
	String[] channelNames = {"empty"};

	//ComboBox components
	JComboBox<String[]> comboBoxAnalysisMethod;
	ArrayList<JComboBox> comboBoxChannelSelections = new ArrayList<JComboBox>();

	//Names of available DataSets (populated when segmentation results are loaded)
	String[] channelList = {"empty"};

	//Target file selection
	JTextField textFieldTarget;
	JLabel targetMessage = new JLabel(" ");
	FileSelectionPanel targetSelection;

	//Status label (shown to the right of Run)
	JLabel labelInfoRun = new JLabel(" ");

	// Components added dynamically by updateChannelPanel(); tracked so they can be
	// cleanly removed before each rebuild.
	ArrayList<Component> channelRowComponents = new ArrayList<Component>();

	// Main-grid row at which channel rows begin (rows 0-2 are static)
	private static final int CHANNEL_START_ROW = 3;

	public AnalysisPanel(AnalysisController controller, AnalysisModel model, int panelNumber) {
		this.panelNumber = panelNumber;
		this.controller = controller;
		this.model = model;
		setLayout(new GridBagLayout()); // must be set before createPanel adds components
		initialize();
		model.addObserver(this);
		createPanel();
	}

	public void initialize() {
		comboBoxAnalysisMethod = new JComboBox(controller.getAnalysisMethodNames());
		comboBoxAnalysisMethod.setSelectedIndex(controller.getAnalysisMethodSelection());

		targetSelection = new FileSelectionPanel("Target", controller.getTargetFilePath(), this);
		buttonTarget = targetSelection.getButton();
		textFieldTarget = targetSelection.getField();
		targetSelection.addObserver(this);

		channelNames = controller.getChannels();
		targetSelection.forceTimerUpdate();
		updateAnalysisLoaded(0, 0);
		// Channel rows are built at the end of createPanel() once the layout is active.
	}

	public void createPanel() {

		//Constraint constants
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.insets = new Insets(2, 4, 2, 4);

		//Adjust fonts
		targetMessage.setFont(new Font(targetMessage.getFont().getName(), Font.ITALIC + Font.BOLD, targetMessage.getFont().getSize()));
		labelInfoRun.setFont(new Font(labelInfoRun.getFont().getName(), Font.ITALIC + Font.BOLD, labelInfoRun.getFont().getSize()));
		labelInfoRun.setPreferredSize(new Dimension(200, labelInfoRun.getPreferredSize().height));

		//ROW 0 — panel title (1-based index)
		constraints.gridy = 0;
		constraints.gridx = 0;
		add(new JLabel("Analysis " + (panelNumber + 1)), constraints);

		//ROW 1 — target file
		constraints.gridy = 1;
		constraints.gridx = 0;
		add(buttonTarget, constraints);
		constraints.gridx = 1;
		constraints.gridwidth = 4;
		add(textFieldTarget, constraints);
		constraints.gridwidth = 1;
		constraints.gridx = 5;
		constraints.gridwidth = 3;
		add(targetMessage, constraints);
		constraints.gridwidth = 1;

		//ROW 2 — analysis method
		//   col 0: "Operation:"  col 1-2: method combo  col 3: Settings
		constraints.gridy = 2;
		constraints.gridx = 0;
		add(new JLabel("Operation:"), constraints);
		constraints.gridx = 1;
		constraints.gridwidth = 2;
		add(comboBoxAnalysisMethod, constraints);
		constraints.gridwidth = 1;
		constraints.gridx = 3;
		add(buttonSettings, constraints);
		buttonSettings.setEnabled(false);
		buttonSettings.setToolTipText("Implementation pending soon");

		//OBSERVERS — listeners on static components
		comboBoxAnalysisMethod.addActionListener(this);
		buttonSettings.addActionListener(this);
		buttonRun.addActionListener(this);

		//ROW 3+ — dynamic channel rows (built by updateChannelPanel)
		//   col 0: channel name    col 1-2: dataset combo
		//   col 3: Run (last row)  col 4+:  status (last row)
		updateChannelPanel();
	}

	/**
	 * Rebuilds the per-channel dataset-selection rows in the main panel grid.
	 * Called once during construction and again whenever the selected analysis method changes
	 * or the set of available DataSets changes.  Automatically re-packs the parent window.
	 *
	 * Columns align directly with the "Operation:" row above:
	 *   col 0 = channel name label  (same as "Operation:")
	 *   col 1-2 = dataset combobox  (same as method combobox)
	 *   col 3 = Run button          (same as Settings button)
	 *   col 4+ = status label
	 */
	public void updateChannelPanel() {

		// Ensure constraints are initialised (this can be called before createPanel finishes)
		constraints.anchor = GridBagConstraints.BASELINE_LEADING;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(2, 4, 2, 4);

		// Remove previous channel rows, Run, and status from the main panel
		for (Component c : channelRowComponents) remove(c);
		channelRowComponents.clear();
		comboBoxChannelSelections.clear();

		channelNames = controller.getChannels();

		// Build one combobox per channel input
		for (int i = 0; i < channelNames.length; i++) {
			JComboBox combo = new JComboBox(channelList);
			combo.addActionListener(this);
			comboBoxChannelSelections.add(combo);
		}

		// Add channel rows to the main panel grid
		for (int i = 0; i < channelNames.length; i++) {
			constraints.gridy = CHANNEL_START_ROW + i;

			JLabel nameLabel = new JLabel(channelNames[i]);
			constraints.gridx = 0;
			add(nameLabel, constraints);
			channelRowComponents.add(nameLabel);

			constraints.gridx = 1;
			constraints.gridwidth = 2;
			add(comboBoxChannelSelections.get(i), constraints);
			constraints.gridwidth = 1;
			channelRowComponents.add(comboBoxChannelSelections.get(i));
		}

		// Run + status live on the last channel row
		int lastRow = CHANNEL_START_ROW + Math.max(0, channelNames.length - 1);
		constraints.gridy = lastRow;

		constraints.gridx = 3;
		add(buttonRun, constraints);
		channelRowComponents.add(buttonRun);

		constraints.gridx = 4;
		constraints.gridwidth = 4;
		add(labelInfoRun, constraints);
		constraints.gridwidth = 1;
		channelRowComponents.add(labelInfoRun);

		revalidate();
		repaint();

		// Auto-resize the parent window to fit the updated row count
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null) window.pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == comboBoxAnalysisMethod) {
			controller.setAnalysisMethodSelection(comboBoxAnalysisMethod.getSelectedIndex());
			updateChannelPanel();
		}

		for (int i = 0; i < comboBoxChannelSelections.size(); i++) {
			if (e.getSource() == comboBoxChannelSelections.get(i)) {
				controller.setChannelMethodSelection(i, comboBoxChannelSelections.get(i).getSelectedIndex());
			}
		}

		if (e.getSource() == buttonSettings) {
			controller.openAnalysisSettings();
		}

		if (e.getSource() == buttonRun) {
			controller.runAnalysis();
		}
	}

	@Override
	public void update(Observable obs, Object arg) {
		if (obs instanceof FileSelectionPanel) {
			updateTargetFile();
		}
	}

	public void updateTargetFile() {
		if (targetSelection.getFileType() == FileType.NOT_DIRECTORY) {
			targetMessage.setText("Not a valid directory");
			targetMessage.setForeground(Color.RED);
		} else if (targetSelection.getFileType() == FileType.NO_FILES) {
			targetMessage.setText("Directory has no files");
			targetMessage.setForeground(Color.RED);
		} else if (targetSelection.getFileType() == FileType.MULTIPLE_FILES) {
			targetMessage.setText("Directory has more than one file");
			targetMessage.setForeground(Color.RED);
		} else if (targetSelection.getFileType() == FileType.SINGLE_FILE) {
			targetMessage.setText("Target File Selected");
			targetMessage.setForeground(Color.BLACK);
			controller.setTargetFilePath(targetSelection.getFile().getAbsolutePath());
		}
		controller.setTargetField(textFieldTarget.getText());
		repaint();
		revalidate();
	}

	// Setting 0: unloaded. 1: overlay+data. 2: overlay only. 3: data only. 4: nothing generated.
	// label param is reserved for future expansion; currently only labelInfoRun is shown.
	public void updateAnalysisLoaded(int label, int setting) {
		if (label != 0) return;

		final Color LOADED_COLOR  = new Color(0, 150, 0);
		final Color WARNING_COLOR = new Color(200, 140, 0);

		if (setting == 0) { labelInfoRun.setText(" ");                                    labelInfoRun.setForeground(Color.BLACK);    }
		if (setting == 1) { labelInfoRun.setText("Overlay constructed and data analyzed"); labelInfoRun.setForeground(LOADED_COLOR);  }
		if (setting == 2) { labelInfoRun.setText("Overlay constructed");                  labelInfoRun.setForeground(LOADED_COLOR);  }
		if (setting == 3) { labelInfoRun.setText("Data Analyzed");                        labelInfoRun.setForeground(LOADED_COLOR);  }
		if (setting == 4) { labelInfoRun.setText("No data generated");                    labelInfoRun.setForeground(WARNING_COLOR); }
	}

	public void setChannelList(String[] channelList) {
		this.channelList = channelList;
		updateChannelPanel();
	}
}
