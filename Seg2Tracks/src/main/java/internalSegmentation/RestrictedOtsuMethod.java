package internalSegmentation;

import geometricTools.ModifiedAutoThresholder;
import geometricTools.ModifiedAutoThresholder.Method;

public class RestrictedOtsuMethod extends InternalSegmentation {

	public RestrictedOtsuMethod() {
		this.name = "Restricted Otsu's Method";
		this.description = " "; //TODO
		this.externalDependence = true;
	}
	
	public boolean isExternallyDependent() {
		return true;
	}
	
	Method getMethod() {
		return Method.Otsu;
	}

	/**
	 * Modification to prevent breakdown of Otsu with high population of background
	 * pixels to object pixels ratio. Utilizes criteria established by Kittler & 
	 * Illingworth1985 in "On Threshold Selection Using Clustering Criteria"
	 */
	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		int mean1 = 0;
		int mean2 = 0;
		int countMean1 = 0;
		int countMean2 = 0;
		
		for (int i = 0; i < histogram.length; i++) {
			if (i < threshold) {
				mean1 += i * histogram[i];
				countMean1 += histogram[i];
			}
			if (i > threshold) {
				mean2 += i * histogram[i];
				countMean2 += histogram[i];
			}
		}
		
		if (countMean1 == 0 || countMean2 == 0) return false;
		mean1 = mean1 / countMean1;
		mean2 = mean2 / countMean2;
		
		if (histogram[threshold] > mean1 && histogram[threshold] > mean2) {
			return false;
		}
		return true;
	}	
}

			