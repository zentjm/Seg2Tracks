package dataStructure;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import calculations.LinkSetCalculation;
import calculations.LinkSetStatistic;
import calculations.SegmentCalculation;
import pairedSegmentCalculations.PairedListCalculation;
import pairedSegmentCalculations.PairedSegmentCalculation;

/**
 * LinkSet represents a single cell tracked across multiple frames.
 * Extends LinkSetModel to act as a list of Segments, with one Segment per frame for that cell.
 * Supports hierarchical tracking relationships (parent/child LinkSets).
 */
public class LinkSet extends LinkSetModel<Segment> {

	private static final long serialVersionUID = 1L;

	int name; // Unique identifier for this cell (used for analysis output)
	String displayName; // Human-readable name for display; null for primary tracks (falls back to String.valueOf(name)); dot-notation for children (e.g. "10.1", "10.2")
	String splitBaseName; // Display name captured just before first split; used to derive consistent dot-notation for all children
	int length; // UNCLEAR: appears unused—number of frames or segments?
	int startFrame; // UNCLEAR: appears unused—first frame containing this cell?
	int endFrame; // UNCLEAR: appears unused—last frame containing this cell?

	boolean orphan; // True if cell has no valid tracking lineage

	HashMap<String, LinkSetCalculation> calculationMap; // Cached cell-level calculations
	HashMap<String, LinkSetStatistic> statisticMap; // Cell-level statistics

	// Display color (null for backwards compatibility as of September 24th)
	Color color = null;

	Segment end; // UNCLEAR: last Segment or terminal Segment in tracking?
	Segment start; // UNCLEAR: first Segment or initial Segment in tracking?

	DataSet dataSet; // Reference to parent dataset
	DataSet childDataSet; // Results of recursive segmentation run on this cell's interior (cross-dataset link; null if not yet run)

	LinkSet parent; // Parent LinkSet if this is a child tracking (hierarchical, within same DataSet)
	ArrayList<LinkSet> children; // Child LinkSets for hierarchical tracking (within same DataSet)

	/**
	 * Constructs an empty LinkSet within a dataset.
	 * Automatically registers itself with the parent DataSet.
	 * @param dataSet parent DataSet
	 */
	public LinkSet(DataSet dataSet) {
		this.dataSet = dataSet;
		this.dataSet.addLinkSet(this); // Register this LinkSet with dataset (added Jul 6th)
		children = new ArrayList<LinkSet>();
		orphan = false;
	}

	/**
	 * Constructs a LinkSet with an initial Segment.
	 * @param segment first Segment in this cell's track
	 * @param dataSet parent DataSet
	 */
	public LinkSet(Segment segment, DataSet dataSet) {
		this (dataSet);
		add(segment);
	}
	
	// Setters for adding and modifying Segments

	/**
	 * Adds a Segment to this LinkSet (appends to the tracked cell).
	 * @param segment Segment to add
	 */
	public void addSegment(Segment segment) {
		add(segment);
	}

	/**
	 * Removes the last Segment from this LinkSet.
	 * Clears the removed Segment's LinkSet reference.
	 */
	public void removeLastSegment() {
		get(this.size()-1).setLinkSet(null); // Unlink the last Segment
		remove(this.size() - 1); // Remove from ArrayList
	}

	/**
	 * Sets the unique name/ID for this tracked cell.
	 * @param name unique identifier
	 */
	public void setName (int name) {
		this.name = name;
	}

	/**
	 * Sets the terminal Segment.
	 * @param end end Segment
	 */
	public void setEnd (Segment end) {
		this.end = end;
	}

	/**
	 * Sets the initial Segment.
	 * @param start start Segment
	 */
	public void setStart (Segment start) {
		this.start = start;
	}

	/**
	 * Sets the display color for visualization.
	 * @param color Color for this cell
	 */
	public void setColor (Color color) {
		this.color = color;
	}

	/**
	 * Adds a child LinkSet for hierarchical tracking (e.g. after cell division).
	 * The parent track keeps its existing display name; each child is assigned
	 * dot-notation derived from the parent: first daughter → "10.1", second → "10.2".
	 * Works recursively: a child named "10.1" produces "10.1.1", "10.1.2", etc.
	 * @param child child LinkSet
	 */
	public void addChild (LinkSet child) {
		children.add(child);
		child.setDisplayName(getDisplayName() + "." + children.size());
		child.setParent(this);
	}

	/**
	 * Sets the parent LinkSet (called automatically by addChild).
	 * @param parent parent LinkSet
	 */
	public void setParent (LinkSet parent) {
		this.parent = parent;
	}

	/**
	 * Gets the child LinkSets (daughter tracks after division).
	 * @return list of child LinkSets
	 */
	public ArrayList<LinkSet> getChildren() {
		return children;
	}

	/**
	 * Gets the parent LinkSet, or null if this is a primary track.
	 * @return parent LinkSet, or null
	 */
	public LinkSet getParent() {
		return parent;
	}

	/**
	 * Marks this LinkSet as orphan (no valid tracking lineage).
	 * @param orphan true if orphaned
	 */
	public void setOrphan (boolean orphan) {
		this.orphan = orphan;
	}

	// Getters for retrieving data

	/**
	 * Gets the parent DataSet.
	 * @return parent DataSet
	 */
	public DataSet getDataSet() {
		return dataSet;
	}

	/**
	 * Attaches the RecursiveDataSet produced by running the recursive segmentation
	 * pipeline on this cell's interior. Null until that pipeline has been run.
	 * @param childDataSet the DataSet of child (e.g. void) segmentation results
	 */
	public void setChildDataSet(DataSet childDataSet) {
		this.childDataSet = childDataSet;
	}

	/**
	 * Returns the RecursiveDataSet for this cell's interior segmentation, or null
	 * if the recursive pipeline has not been run for this cell yet.
	 */
	public DataSet getChildDataSet() {
		return childDataSet;
	}

	/**
	 * Gets the unique name/ID of this cell.
	 * @return cell identifier
	 */
	public int getName() {
		return name;
	}

	/**
	 * Gets the display name for this track.
	 * Primary tracks return their integer ID as a string (e.g. "10").
	 * Child tracks created by cell division return dot-notation (e.g. "10.1", "10.2").
	 * @return display name string
	 */
	public String getDisplayName() {
		return (displayName != null) ? displayName : String.valueOf(name);
	}

	/**
	 * Sets the display name override for this track.
	 * Used to assign dot-notation names to child tracks at cell division.
	 * @param displayName display name string (e.g. "10.1")
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Gets the display color.
	 * @return Color for visualization
	 */
	public Color getColor () {
		return color;
	}

	/**
	 * Checks if this LinkSet has been marked as orphan.
	 * @return true if orphaned
	 */
	public boolean isOrphan() {
		return orphan;
	}
	
	/**
	 * Checks if any Segment in this cell touches the external boundary.
	 * Iterates through all Segments in the tracked cell.
	 * @return true if any Segment has external boundary contact
	 */
	public boolean getExternalBoundaryContact() {
		for (Segment segment : this) {
			//System.out.println("Segment " + segment.getExternalBoundaryContact()); // DEBUG output
			if (segment.getExternalBoundaryContact()) return true;
		}
		return false;
	}

	/**
	 * Checks if any Segment in this cell touches the internal boundary.
	 * @return true if any Segment has internal boundary contact
	 */
	public boolean getInternalBoundaryContact() {
		for (Segment segment : this) {
			if (segment.getInternalBoundaryContact()) return true;
		}
		return false;
	}

	// Calculation and statistic caching methods

	/**
	 * Stores a cell-level calculation result.
	 * @param calculation LinkSetCalculation to cache
	 */
	public void setCalculation(LinkSetCalculation calculation) {
		if (calculationMap == null) calculationMap = new HashMap<String, LinkSetCalculation>();
		calculationMap.put(calculation.getName(), calculation);
	}

	/**
	 * Retrieves a cached cell-level calculation.
	 * @param name name of calculation
	 * @return calculated value
	 */
	public double getCalculation(String name) {
		return calculationMap.get(name).get();
	}

	/**
	 * Stores a cell-level statistic.
	 * @param statistic LinkSetStatistic to cache
	 */
	public void setStatistic(LinkSetStatistic statistic) {
		if (statisticMap == null) statisticMap = new HashMap<String, LinkSetStatistic>();
		statisticMap.put(statistic.getName(), statistic);
	}

	/**
	 * Retrieves a cached cell-level statistic.
	 * @param statistic name of statistic
	 * @param calculation name of underlying calculation
	 * @return statistical result
	 */
	public double getStatistic(String statistic, String calculation) {
		return statisticMap.get(statistic).get(calculation);
	}
	
	
	
	

}
