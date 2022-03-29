package util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import dataStructure.DataSet;
import dataStructure.FrameSet;
import dataStructure.LinkSet;
import dataStructure.Segment;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.FloatPolygon;

public class FileLoadFromOverlay {
	
	JPanel panel;
	File file;
	String path;
	
	ImagePlus img;
	ImageStack imgStack;
	Overlay overlay;
	
	DataSet dataSet;
	Color dataSetColor = new Color(255, 0, 0);
	Roi[] roiList;
	
	public FileLoadFromOverlay (JPanel panel) {
		this.panel = panel;
	}
	
	public FileLoadFromOverlay (File file) {
		this.file = file;
	}
	
	public DataSet run() {
		openImage();
		createDataSet();
		fillDataSet();
		return dataSet;
	}
	
	//Gets the path of the image to load
	public void openImage() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setDialogTitle("Please select the overlayed image file to load");
		int result = jfc.showDialog(panel, "Choose");
		jfc.setVisible(true);
		if (result == JFileChooser.APPROVE_OPTION) {
			path = jfc.getSelectedFile().getAbsolutePath();
		}
		jfc.setVisible(false);	
		img = IJ.openImage(path); //TODO: virtual?
		overlay = img.getOverlay();
		imgStack = img.getImageStack();
	}
	
	public void createDataSet() {
		dataSet = new DataSet(imgStack.getWidth(), imgStack.getHeight(), imgStack.getSize());
		dataSet.setColor(dataSetColor);
		//dataSet.setDataSetName("noname");
		dataSet.setFrameSetList(new FrameSet[dataSet.getSize()]);
		for (int i = 0; i < imgStack.size(); i ++) {
			dataSet.addFrameSet(new FrameSet(i, dataSet), i);
		}
		dataSet.setLinkSetList(new ArrayList<LinkSet>());
	}
	
	
	//Reconstruct a dataSet from Image; 
	public void fillDataSet() {
		roiList = overlay.toArray();
		for (Roi roi : roiList) {
			
			//Create linkSet if not already created/ 
			int frame = roi.getTPosition(); 
			if (roi.getZPosition() > frame) frame = roi.getZPosition();
			
			//name
			String name = roi.getName(); //TODO: get Roi name
			
			FloatPolygon floatPolygon = roi.getFloatPolygon();
			
			//get perimeter
			Point[] perimeter = new Point[floatPolygon.npoints];	
			for (int j = 0; j < floatPolygon.npoints; j ++) {
				perimeter[j] = new Point((int) floatPolygon.xpoints[j], (int) floatPolygon.ypoints[j]);
			}
			
			//Construct geometric centerpoint //TODO: more sophisticated centerpoint identificaiton. 
			Rectangle rect = floatPolygon.getBounds();
			Point centerpoint = new Point (rect.x + (rect.width / 2), rect.y + (rect.height / 2));
			//System.out.println("CenterPoint is... x:" + centerpoint.x + "   y:"  + centerpoint.y);
			
			Segment segment = new Segment(frame -1, centerpoint);
			segment.setColor(null);
			segment.setExternalPerimeter(perimeter);
		
			//Add to linkSet list
			boolean addedToLinkSet = false;
			int roiName = Integer.parseInt(roi.getName().replaceAll("\\D+","")); //Test this?
			System.out.println("Roi Name: " + roiName); //XXX: TEST
			
			for (LinkSet linkSet : dataSet.getLinkSetList()) {
				//dSystem.out.println("Loop LinkSet:  " + linkSet.getName());
				if (linkSet.getName() == roiName) {
					linkSet.add(segment);
					segment.setLinkSet(linkSet);
					linkSet.add(segment);
					System.out.println("Added to LinkSet " + linkSet.getName());
					addedToLinkSet = true;
				}
			}
			
			if (!addedToLinkSet) {
				LinkSet linkSet  = new LinkSet(segment, dataSet);
				segment.setLinkSet(linkSet);
				linkSet.setName(roiName);
				//dataSet.getLinkSetList().add(linkSet);
				System.out.println("New LinkSet named: " + linkSet.getName());
			}
	
			//Add to frameSet list
			dataSet.getFrameSet(frame - 1).add(segment);
		}
		
		//Rename LinkSets
		for (int i = 0; i < dataSet.getLinkSetList().size(); i++) {
			dataSet.getLinkSet(i).setName(i);
		}
		
		//Modify dataset
		dataSet.setExternalSegmentationExists(true);	
		System.out.println("LinkSet size is: " + dataSet.getLinkSetList().size());
	}
	
	
	
	

	

			//inputStack = IJ.openVirtual(controller.getInputFilePath()).getImageStack();
	
	
	
	
	
	/*
	 * NOTE: Only neccessary for manual segmentation with bad serialization or conversion from other programs. 
	 * 
	 * 1. Transfer all the Rois from the overlay to the Roi Manager. 
	 * 2. Create a SegmentModel for each Roi
	 * 		- Calculate CenterPoint for each Roi in the same way as the manual
	 * 		- All roi's with the same name are in a LinkSet
	 * 		- All Roi's with the same frame are a FrameSet
	 * 		- Add all to dataSet.
	 * Fill in all gaps for manual. 
	 */
}
