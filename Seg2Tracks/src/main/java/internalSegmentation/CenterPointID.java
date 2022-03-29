package internalSegmentation;

import java.awt.*;

import dataStructure.FrameSet;
import geometricTools.GeometricCalculations;
import geometricTools.ModifiedAutoThresholder.Method;
import ij.gui.Roi;
import ij.gui.Wand;


public class CenterPointID extends InternalSegmentation {

	public CenterPointID() {
		this.name = "CenterPointID";
		this.description = " "; //TODO
		this.externalDependence = false;
	}
	
	public boolean isExternallyDependent() {
		return false;
	}
	
	Method getMethod() {
		return null;
	}
	
	

	@Override
	protected void binarySegmentation(FrameSet segments) {

		for (int n = 0; n < segments.size(); n++) {
			
			//Rectangle(int x, int y, int width, int height)
			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;
			
			int size = 5;
			
			Point[] rect = new Point[4];
			rect[0] = new Point(x - size, y - size);
			rect[2] = new Point(x - size, y + size);
			rect[1] = new Point(x + size, y - size);
			rect[3] = new Point(x + size, y + size);
			
	
			//Sets SegmentModel internal perimeter //TODO: some external control on geometric operations	
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(rect));
		
		}
	}
	
	@Override
	protected void dependentBinarySegmentation(FrameSet segments) {
		
		for (int n = 0; n < segments.size(); n++) {
			
			//Roi roi = getPolygonRoi(segments.get(n).getExternalPerimeter());
		
			//Rectangle(int x, int y, int width, int height)
			int x = segments.get(n).getCenterPoint().x;
			int y = segments.get(n).getCenterPoint().y;
			
			int size = 5;
			
			Point[] rect = new Point[4];
			rect[0] = new Point(x - size, y - size);
			rect[2] = new Point(x - size, y + size);
			rect[1] = new Point(x + size, y - size);
			rect[3] = new Point(x + size, y + size);
			
	
			//Sets SegmentModel internal perimeter //TODO: some external control on geometric operations	
			segments.get(n).setInternalPerimeter(
				GeometricCalculations.straightPerimeter(rect));
		
		}
	}
		//TODO: dependence on external segmentation. 

	@Override
	boolean acceptableThreshold(int threshold, int[] histogram) {
		return true;
	}
		
	

}
