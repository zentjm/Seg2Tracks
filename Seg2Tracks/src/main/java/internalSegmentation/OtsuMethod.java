package internalSegmentation;

import geometricTools.ModifiedAutoThresholder.Method;;

public class OtsuMethod extends InternalSegmentation {

	
	public OtsuMethod() {
		this.name = "Otsu's Method";
		this.description = " "; //TODO
		this.externalDependence = false;
	}
	
	public boolean isExternallyDependent() {
		return false;
	}
	
	Method getMethod() {
		return Method.Otsu;
	}
	
	
	/*
	Method getMethod() {
		return Method.Otsu
	}
	*/
	
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}
}
