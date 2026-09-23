package gui;

import java.util.ArrayList;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Top-level model for Seg2Tracks. Manages application state including the number of operation and
 * analysis panels, and maintains lists of corresponding model objects. Uses Observable pattern to
 * notify listeners of state changes. Handles persistence of settings via Java Preferences.
 */
public class Seg2TracksModel extends Observable {
	
	//Settings
	Preferences preferences;
	int operationPanels;
	int analysisPanels;

	// Panel topology — parallel arrays indexed by display position.
	// Populated by loadSettings() from Preferences written by Seg2TracksController.exitProgram().
	int[]     panelOrderedPanelNums; // panelNumber used for per-panel Preferences keys
	boolean[] panelIsRecursive;      // true → RecursionOperationModel; false → OperationModel
	int[]     panelParentPositions;  // display-order index of parent panel (-1 if not recursive)
	
	ArrayList<OperationModel> operationModelList;
	ArrayList<AnalysisModel> analysisModelList;
	//Seg2TracksSettingsLoader loader;
	
	public Seg2TracksModel() {
		preferences = Preferences.userRoot().node("/seg2tracks");
		loadSettings();
		initialize();	
	}
	
	// Load panel count, analysis count, and panel topology.
	// Topology is written by Seg2TracksController.exitProgram(); defaults produce a single
	// regular panel (correct behaviour on a clean first run).
	public void loadSettings() {
		operationPanels = preferences.getInt("OPERATION_PANEL_NUMBER", 1);
		analysisPanels  = preferences.getInt("ANALYSIS_PANEL_NUMBER", 1);

		panelOrderedPanelNums = new int[operationPanels];
		panelIsRecursive      = new boolean[operationPanels];
		panelParentPositions  = new int[operationPanels];
		for (int i = 0; i < operationPanels; i++) {
			panelOrderedPanelNums[i] = preferences.getInt("PANEL_ORDER_PANELNUM" + i, i);
			panelIsRecursive[i]      = preferences.getBoolean("PANEL_RECURSIVE" + i, false);
			panelParentPositions[i]  = preferences.getInt("PANEL_PARENT_POS" + i, -1);
		}
	}
	
	//For saving all settings
	public void saveSettings() {
		preferences.putInt("OPERATION_PANEL_NUMBER", operationPanels);
		preferences.putInt("ANALYSIS_PANEL_NUMBER", analysisPanels);	
	}
		
	public void initialize() {

		operationModelList = new ArrayList<OperationModel>();
		analysisModelList  = new ArrayList<AnalysisModel>();

		// Create operation models in saved display order, using the saved panelNumber as
		// the Preferences key so per-panel settings (input path, method selections, name
		// etc.) are restored correctly.  Recursive panels get RecursionOperationModel;
		// the parent-child wiring is done later by Seg2TracksController.loadOperationControllers().
		for (int i = 0; i < operationPanels; i++) {
			int pNum = panelOrderedPanelNums[i];
			if (panelIsRecursive[i]) {
				operationModelList.add(new RecursionOperationModel(pNum));
			} else {
				operationModelList.add(new OperationModel(pNum));
			}
		}

		for (int i = 0; i < analysisPanels; i++) {
			analysisModelList.add(new AnalysisModel(i));
		}
	}
	
	public ArrayList<OperationModel> getOperationModels() {
		return operationModelList;
	}
	
	public ArrayList<AnalysisModel> getAnalysisModels() {
		return analysisModelList;
	}
	
	public OperationModel addOperationModel() {
		operationPanels ++;
		OperationModel tempMod = new OperationModel(operationPanels - 1);
		operationModelList.add(tempMod);
		return tempMod;
	}

	/**
	 * Creates a new RecursionOperationModel for a panel that was opened via the
	 * Subsegment button.  The panel is already linked to its parent — no checkbox
	 * or combobox selection is needed.
	 */
	public RecursionOperationModel addRecursionOperationModel() {
		operationPanels++;
		RecursionOperationModel tempMod = new RecursionOperationModel(operationPanels - 1);
		operationModelList.add(tempMod);
		return tempMod;
	}
	
	public void removeOperationModel() {
		operationPanels --;
		operationModelList.remove(operationModelList.size()-1);
	}
	
	
	//Save the settings 
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
