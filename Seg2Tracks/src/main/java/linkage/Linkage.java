package linkage;

import java.util.ArrayList;

import dataStructure.DataSet;
import dataStructure.Segment;

public abstract class Linkage {

	DataSet dataSet;
	String name;
	String description;
	

	public Linkage() {
		name = "Method Name";
		description = "How this Method Works";
	}
	
	public void initialize(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	public String toString() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public abstract void run();

}
