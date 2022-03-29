package dataStructure;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;

public class DataSet implements Serializable {
	private static final long serialVersionUID = 1L;
	
	//Metadata
	String name;
	boolean identificationExists;
	boolean linkageExists;
	boolean internalSegmentationExists;
	boolean externalSegmentationExists;
	boolean loadedFile;
	
	//Data Collections
	FrameSet [] frameSetList;
	ArrayList <LinkSet> linkSetList;
	
	//Name
	String dataSetName = "noname";
	
	//Color
	Color color = Color.WHITE;
	
	//Dimensions of Associated Input Image
	int width;
	int height;
	int depth;
	
	//Constructor
	public DataSet(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		frameSetList = new FrameSet[depth];
		linkSetList = new ArrayList <LinkSet>();
		
		identificationExists = false;
		linkageExists = false;
		internalSegmentationExists = false;
		externalSegmentationExists = false;
		loadedFile = false;
	}
	//Getter methods
	public FrameSet getFrameSet(int frame) {
		return frameSetList[frame];
	}
	
	public FrameSet[] getFrameSetList() {
		return frameSetList;
	}
	
	public LinkSet getLinkSet(int number) {
		return linkSetList.get(number);
	}
	
	public ArrayList<LinkSet> getLinkSetList() {
		return linkSetList;
	}
	
	public String getName() {
		return dataSetName;
	}
	
	public Color getColor() {
		return color;
	}
	
	//Determines if these operations have been run on this dataSet
	public boolean getIdentificationExists() {
		return identificationExists;
	}
	
	public boolean getLinkageExists() {
		return linkageExists;
	}
	
	public boolean getInternalSegmentationExists() {
		return internalSegmentationExists;
	}
	
	public boolean getExternalSegmentationExists() {
		return externalSegmentationExists;
	}
	
	//Gets the Dimensions of Associated Input Image
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getSize() {
		return depth;
	}
	
	//Setter methods
	public void addLinkSet(LinkSet linkSet) {
		linkSetList.add(linkSet);
	}
	
	public void addFrameSet(FrameSet frameSet, int frame) {
		frameSetList[frame] = frameSet;
	}
	
	public void setFrameSetList(FrameSet[] frameSetList) {
		this.frameSetList = frameSetList;
	}
	
	public void setLinkSetList(ArrayList<LinkSet> linkSetList) {
		this.linkSetList = linkSetList;
	}	
	
	public void setIdentificationExists(boolean identificationExists) {
		this.identificationExists = identificationExists;
	}
	
	public void setLinkageExists(boolean linkageExists) {
		this.linkageExists = linkageExists;
	}
	
	public void setInternalSegmentationExists(boolean internalSegmentationExists) {
		this.internalSegmentationExists = internalSegmentationExists;
	}
	
	public void setExternalSegmentationExists(boolean externalSegmentationExists) {
		this.externalSegmentationExists = externalSegmentationExists;
	}
	
	public void setDataSetName(String dataSetName) {
		this.dataSetName = dataSetName;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	//Operation methods
	
	public void removeSegment(SegmentModel deleteSegment) {
		

		
		for (int i = 0; i < this.getFrameSetList().length; i++) {
			for (int j = 0; j < this.getFrameSet(i).size(); j++) {
				LinkSet linkSet = deleteSegment.getLinkSet();
				for (Segment segment: linkSet) {
					segment.getLinkSet().getDataSet().getFrameSet(segment.getFrame()).remove(segment);
				}
				linkSet.getDataSet().getLinkSetList().remove(linkSet);
			}
		}	
		

		
		
		
		/*
		//Remove segment from frameSets
		for (Segment seg : segment.getLinkSet()) {
			seg.getLinkSet().getDataSet().getFrameSet(seg.getFrame()).remove(seg);
		}
			
		//Remove segment from linkSets
		linkSetList.remove(segment.getLinkSet());
		*/
		//Alternative Remove segment from linkSets
		/*
		for (int i = 0 ; i < linkSetList.size(); i ++) {
			if (segment.getLinkSet().getName() == linkSetList.get(i).getName()) {
				linkSetList.remove(i);
			}	
		}
		*/
	}
	
	

}
