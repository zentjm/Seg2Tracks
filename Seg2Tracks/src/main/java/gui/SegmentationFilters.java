package gui;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.Segment;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.awt.*;

import javax.swing.JProgressBar;

/**
 * Post-segmentation filter for cleaning up segmented regions. Currently implements edge exclusion
 * filtering to remove segments that contact image boundaries (incomplete/partial objects).
 * Can be extended with additional filtering criteria (min/max area, solidity, etc.).
 */
public class SegmentationFilters {
	
	DataSet dataSet;
	JProgressBar progressBar;
	boolean excludeEdges;

	public SegmentationFilters() {}
	
	public void initialize(DataSet dataSet, JProgressBar progressBar, boolean excludeEdges) {
		this.dataSet = dataSet;
		this.progressBar = progressBar;
		this.excludeEdges = excludeEdges;
	}
	
	public void run() {
		if (excludeEdges) excludeEdges();
		
		
		
		
	}
	
	
	
	
	public DataSet excludeEdges() {
		progressBar.setString("Edge Filter");
		Segment segment;
		for (FrameSet frameSet: dataSet.getFrameSetList()) {
			for (int i = 0; i < frameSet.size(); i ++) {
				segment = frameSet.get(i);
				if (segment.getLinkSet().getInternalBoundaryContact()) {
					segment.getLinkSet().getDataSet().removeSegment(segment);
					
				}
				progressBar.setValue(i);
			}	
		}
		return dataSet;
	}
		
		
		/*
		if (filterExternal && dataSet.getExternalSegmentationExists()) {
			Segment segment;
			ImageProcessor processor;
			for (FrameSet frameSet: dataSet.getFrameSetList()) {
				for (int i = 0; i < frameSet.size(); i ++) {
					//System.out.println("TEST: " + i);
					segment = frameSet.get(i);
					if (segment.getLinkSet().getExternalBoundaryContact()) {
						segment.getLinkSet().getDataSet().removeSegment(segment);
						//System.out.println("Removed External Segment... linkSet: " + 
								segment.getLinkSet().getName() + "   frameSet: " + segment.getFrame());
					}
				}	
			}
		}
		*/
		
	
	/*
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
	*/
	
	
	

}
