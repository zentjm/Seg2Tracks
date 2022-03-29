package gui;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.awt.*;

public class SegmentationFilters {
	
	OperationController controller;
	ImageStack inputStack;
	

	public SegmentationFilters(OperationController controller, ImageStack inputStack) {
		this.controller = controller;
		this.inputStack = inputStack;
	}
	
	
	
	public DataSet excludeEdges(DataSet dataSet, boolean filterInternal, boolean filterExternal) {
		

		if (filterInternal && dataSet.getInternalSegmentationExists()) {
			Segment segment;
			for (FrameSet frameSet: dataSet.getFrameSetList()) {
				for (int i = 0; i < frameSet.size(); i ++) {
					segment = frameSet.get(i);
					if (segment.getLinkSet().getInternalBoundaryContact()) {
						segment.getLinkSet().getDataSet().removeSegment(segment);
						
					}
				}	
			}
			
		}
		
		if (filterExternal && dataSet.getExternalSegmentationExists()) {
			
		
			Segment segment;
			ImageProcessor processor;
			for (FrameSet frameSet: dataSet.getFrameSetList()) {
				for (int i = 0; i < frameSet.size(); i ++) {
					System.out.println("TEST: " + i);
					segment = frameSet.get(i);
					if (segment.getLinkSet().getExternalBoundaryContact()) {
						segment.getLinkSet().getDataSet().removeSegment(segment);
						System.out.println("Removed External Segment... linkSet: " + 
								segment.getLinkSet().getName() + "   frameSet: " + segment.getFrame());
					}
				}	
			}
		}
		return dataSet;
	}
	
	boolean edgeContact(Point [] ptsList, ImageProcessor processor) {
		//Determine if any points are making contact with bounds
		for (int i = 0; i < ptsList.length; i ++) {
			if (ptsList[i].x == 0) return true;
			if (ptsList[i].x == processor.getWidth() - 1) 	return true;
			if (ptsList[i].y == 0) return true;
			if (ptsList[i].y == processor.getHeight() - 1) 	return true;
		}
		return false;
	}
	
	
	

}
