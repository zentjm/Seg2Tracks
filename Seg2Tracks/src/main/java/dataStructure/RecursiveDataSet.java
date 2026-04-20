package dataStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecursiveDataSet extends DataSet to support nested/hierarchical segmentation.
 * Used by the recursive operation pipeline to hold the results of segmenting the
 * interior of each parent-level LinkSet (e.g. tracking voids inside macrophages).
 *
 * Architecture:
 *   - parentDataSet: the DataSet from the prior segmentation pass whose LinkSet
 *     interiors were used as masks when building this dataset.
 *   - All child LinkSets found during recursive segmentation are stored in the
 *     inherited linkSetList, and also stored per-parent via LinkSet.childDataSet.
 *   - childToParentMap: maps each child LinkSet in this dataset to the parent
 *     LinkSet in parentDataSet whose masked region it was found inside.
 */
public class RecursiveDataSet extends DataSet {

	private static final long serialVersionUID = 1L;

	/** The prior-level DataSet whose LinkSet interiors were segmented to build this one. */
	DataSet parentDataSet;

	/** Maps each child void LinkSet → the parent cell LinkSet it was found inside. */
	HashMap<LinkSet, LinkSet> childToParentMap = new HashMap<>();

	/**
	 * Constructs a RecursiveDataSet derived from a parent segmentation.
	 * @param width image width in pixels
	 * @param height image height in pixels
	 * @param depth number of frames
	 * @param parentDataSet the DataSet whose LinkSet interiors are being segmented
	 */
	public RecursiveDataSet(int width, int height, int depth, DataSet parentDataSet) {
		super(width, height, depth);
		this.parentDataSet = parentDataSet;
	}

	/**
	 * Gets the parent DataSet this recursive dataset was derived from.
	 * @return parent DataSet
	 */
	public DataSet getParentDataSet() {
		return parentDataSet;
	}

	/**
	 * Records that a child void LinkSet was found inside a particular parent cell LinkSet.
	 * Called once per child LinkSet during the recursive pipeline accumulation loop.
	 * @param child  the void/subsegment LinkSet
	 * @param parent the cell LinkSet whose masked interior produced this child
	 */
	public void addChildParentMapping(LinkSet child, LinkSet parent) {
		childToParentMap.put(child, parent);
	}

	/**
	 * Returns the parent cell LinkSet that a given child void LinkSet was found inside.
	 * O(1) lookup.
	 * @param child a child void LinkSet in this dataset
	 * @return the parent cell LinkSet, or null if not found
	 */
	public LinkSet getParentLinkSet(LinkSet child) {
		return childToParentMap.get(child);
	}

	/**
	 * Returns all child void LinkSets that were found inside a given parent cell LinkSet.
	 * O(n) scan — suitable for typical dataset sizes.
	 * @param parent a parent cell LinkSet from the prior segmentation pass
	 * @return list of child LinkSets belonging to that parent (empty if none)
	 */
	public List<LinkSet> getChildLinkSets(LinkSet parent) {
		List<LinkSet> children = new ArrayList<>();
		for (Map.Entry<LinkSet, LinkSet> entry : childToParentMap.entrySet()) {
			if (entry.getValue() == parent) children.add(entry.getKey());
		}
		return children;
	}
}
