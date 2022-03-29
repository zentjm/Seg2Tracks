package dataStructure;

import java.util.ArrayList;
import java.util.EnumMap;


public class DataOutput {

	ArrayList<Data> dataArray;
	
	class Data {
		EnumMap map;
		ArrayList list;
	}
	
	public DataOutput(int dataSets, int linkSets, int frameSets) { 
		
		dataArray = new ArrayList<Data>(dataSets);
		for (int i =0; i < dataArray.size(); i++) {
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
