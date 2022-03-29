package gui;

import java.util.ArrayList;
import java.util.Observable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Seg2TracksModel extends Observable {
	
	//Settings
	Preferences preferences;
	int operationPanels; 
	int analysisPanels;
	
	ArrayList<OperationModel> operationModelList;
	ArrayList<AnalysisModel> analysisModelList;
	//Seg2TracksSettingsLoader loader;
	
	public Seg2TracksModel() {
		preferences = Preferences.userRoot().node("/seg2tracks");
		loadSettings();
		initialize();	
	}
	
	//TODO: handle setting, loading, and saving multiple operation models.
	public void loadSettings() {
		operationPanels = preferences.getInt("OPERATION_PANEL_NUMBER", 1);
		analysisPanels = preferences.getInt("ANALYSIS_PANEL_NUMBER", 1);	
	}
	
	//For saving all settings
	public void saveSettings() {
		preferences.putInt("OPERATION_PANEL_NUMBER", operationPanels);
		preferences.putInt("ANALYSIS_PANEL_NUMBER", analysisPanels);	
	}
		
	public void initialize() {

		System.out.println("loading settings");
		operationModelList = new ArrayList<OperationModel>();
		analysisModelList = new ArrayList<AnalysisModel>();
		
		//load the operationPanels
		for (int i = 0; i < operationPanels; i++) {
			operationModelList.add(new OperationModel(i));	
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
	
	public void removeOperationModel() {
		operationPanels --;
		operationModelList.remove(operationModelList.size()-1);
	}
	
	
	//Save the settings 
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
