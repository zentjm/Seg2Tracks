package dataStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecursiveDataSet extends DataSet to support nested/hierarchical segmentation.
 * Used by the recursive operation pipeline to hold the results of segmenting the
 * interior of each parent-level LinkSet (e.g. tracking subsegments inside segments).
 *
 * Architecture:
 *   - parentDataSet: the DataSet from the prior segmentation pass whose LinkSet
 *     interiors were used as masks when building this dataset.
 *   - All child (subsegment) LinkSets found during recursive segmentation are stored
 *     in the inherited linkSetList, and also stored per-parent via LinkSet.childDataSet.
 *   - childToParentMap: maps each child subsegment LinkSet in this dataset to the
 *     parent segment LinkSet in parentDataSet whose masked region it was found inside.
 */
public class RecursiveDataSet extends DataSet {

	private static final long serialVersionUID = 1L;

	/** The prior-level DataSet whose LinkSet interiors were segmented to build this one. */
	DataSet parentDataSet;

	/** Maps each child subsegment LinkSet → the parent segment LinkSet it was found inside. */
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
	 * Records that a child subsegment LinkSet was found inside a particular parent segment LinkSet.
	 * Called once per child LinkSet during the recursive pipeline accumulation loop.
	 * @param child  the subsegment LinkSet
	 * @param parent the segment LinkSet whose masked interior produced this child
	 */
	public void addChildParentMapping(LinkSet child, LinkSet parent) {
		childToParentMap.put(child, parent);
	}

	/**
	 * Returns the parent segment LinkSet that a given child subsegment LinkSet was found inside.
	 * O(1) lookup.
	 * @param child a child subsegment LinkSet in this dataset
	 * @return the parent segment LinkSet, or null if not found
	 */
	public LinkSet getParentLinkSet(LinkSet child) {
		return childToParentMap.get(child);
	}

	/**
	 * Returns all child subsegment LinkSets that were found inside a given parent segment LinkSet.
	 * O(n) scan — suitable for typical dataset sizes.
	 * @param parent a parent segment LinkSet from the prior segmentation pass
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
