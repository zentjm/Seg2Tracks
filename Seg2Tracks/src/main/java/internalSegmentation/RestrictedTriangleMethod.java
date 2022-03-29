package internalSegmentation;

import geometricTools.ModifiedAutoThresholder.Method;

public class RestrictedTriangleMethod extends InternalSegmentation {

	public RestrictedTriangleMethod() {
		this.name = "Restricted Triangle Method";
		this.description = " "; //TODO
		this.externalDependence = true;
	}
	
	public boolean isExternallyDependent() {
		return true;
	}
	
	Method getMethod() {
		return Method.Triangle;
	}
	
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}

}
