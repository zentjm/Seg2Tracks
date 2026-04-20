package dataStructure;

import java.util.ArrayList;
import java.util.EnumMap;


/**
 * DataOutput manages hierarchical output structures for segmentation and tracking results.
 * Designed to hold nested calculation data organized by DataSets, LinkSets, and FrameSets.
 */
public class DataOutput {

	ArrayList<Data> dataArray;

	/**
	 * Inner data container combining parameter maps with list data.
	 */
	class Data {
		EnumMap map;
		ArrayList list;
	}

	/**
	 * Initializes a DataOutput structure with capacity for nested data.
	 * @param dataSets number of datasets to accommodate
	 * @param linkSets number of linksets per dataset
	 * @param frameSets number of framesets per linkset
	 */
	public DataOutput(int dataSets, int linkSets, int frameSets) {

		dataArray = new ArrayList<Data>(dataSets);
		for (int i =0; i < dataArray.size(); i++) {
			// DEAD CODE: These lines were part of the original hierarchical structure design but are now commented out
			//dataArray.get(i).map = new EnumMap(DataSetCalculations.parameter);
			//dataArray.set(i, new ArrayList<ArrayList<EnumMap>>(linkSets));
		}
		
		
		
	}
	
	
	
	
	
	
	
	/*
	
	//An ArrayList object for holding data structures. 
	public DataOutput(int dataSets, int linkSets, int frameSets) { 
		dataArray = new ArrayList<ArrayList<ArrayList<EnumMap>>>(dataSets);
		for (int i =0; i < dataArray.size(); i++) {
			dataArray.set(i, new ArrayList<ArrayList<EnumMap>>(linkSets));
			for (int j =0; j < dataArray.get(i).size(); j++) {
				dataArray.get(i).set(j, new ArrayList<EnumMap>(frameSets));
				for (int k = 0; k < dataArray.get(i).get(j).size(); k++) {
					dataArray.get(i).get(j).set(k, new 
					
				}
				
			}
		}
	}
	*/
	
	
	
	

	
	
	

}
