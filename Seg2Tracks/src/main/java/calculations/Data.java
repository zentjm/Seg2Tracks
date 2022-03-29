package calculations;

import dataStructure.Segment;

public abstract class Data {
	
	boolean active;
	
	public Data() {
		active = false;
	}
	
	//Name of Calculation
	public abstract String getName();
	
	//Type of data calculation is applied to 
	public abstract DataType getType();
	
	//Sets whether calculation will be implemented
	public void setActive(boolean active) {
		this.active = active;
	}
	
	//Gets active state
	public boolean isActive() {
		return active;
	}
	
}

	
	
	
