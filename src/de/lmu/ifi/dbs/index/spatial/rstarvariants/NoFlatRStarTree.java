package de.lmu.ifi.dbs.index.spatial.rstarvariants;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.BulkSplit;
import de.lmu.ifi.dbs.index.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialObject;

/**
 * Abstract superclass for all non-flat R*-Tree variants.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class NoFlatRStarTree<O extends NumberVector, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry>
		extends AbstractRStarTree<O, N, E> {

	/**
	 * Creates a new RTree.
	 */
	public NoFlatRStarTree() {
		super();
	}

	/**
	 * Returns true if in the specified node an overflow occured, false
	 * otherwise.
	 * 
	 * @param node
	 *            the node to be tested for overflow
	 * @return true if in the specified node an overflow occured, false
	 *         otherwise
	 */
	protected boolean hasOverflow(N node) {
		if (node.isLeaf())
			return node.getNumEntries() == leafCapacity;
		else
			return node.getNumEntries() == dirCapacity;
	}

	/**
	 * Returns true if in the specified node an underflow occured, false
	 * otherwise.
	 * 
	 * @param node
	 *            the node to be tested for underflow
	 * @return true if in the specified node an underflow occured, false
	 *         otherwise
	 */
	protected boolean hasUnderflow(N node) {
		if (node.isLeaf())
			return node.getNumEntries() < leafMinimum;
		else
			return node.getNumEntries() < dirMinimum;
	}

	/**
	 * Computes the height of this RTree. Is called by the constructur. and
	 * should be overwritten by subclasses if necessary.
	 * 
	 * @return the height of this RTree
	 */
	protected int computeHeight() {
		N node = getRoot();
		int height = 1;

		// compute height
		while (!node.isLeaf() && node.getNumEntries() != 0) {
			E entry = node.getEntry(0);
			node = getNode(entry.getID());
			height++;
		}
		return height;
	}

	/**
	 * @see de.lmu.ifi.dbs.index.Index#createEmptyRoot(de.lmu.ifi.dbs.data.DatabaseObject)
	 */
	protected void createEmptyRoot(O object) {
		N root = createNewLeafNode(leafCapacity);
		file.writePage(root);
		this.height = 1;
	}

	/**
	 * Performs a bulk load on this RTree with the specified data. Is called by
	 * the constructur and should be overwritten by subclasses if necessary.
	 * 
	 * @param objects
	 *            the data objects to be indexed
	 */
	protected void bulkLoad(List<O> objects) {
		StringBuffer msg = new StringBuffer();
		List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(
				objects);

		// root is leaf node
		double size = objects.size();
		if (size / (leafCapacity - 1.0) <= 1) {
			N root = createNewLeafNode(leafCapacity);
			root.setID(getRootEntry().getID());
			file.writePage(root);
			createRoot(root, spatialObjects);
			height = 1;
			if (this.debug) {
				msg.append("\n  numNodes = 1");
			}
		}

		// root is directory node
		else {
			N root = createNewDirectoryNode(dirCapacity);
			root.setID(getRootEntry().getID());
			file.writePage(root);

			// create leaf nodes
			List<N> nodes = createLeafNodes(objects);

			int numNodes = nodes.size();
			if (this.debug) {
				msg.append("\n  numLeafNodes = ").append(numNodes);
			}
			height = 1;

			// create directory nodes
			while (nodes.size() > (dirCapacity - 1)) {
				nodes = createDirectoryNodes(nodes);
				numNodes += nodes.size();
				height++;
			}

			// create root
			createRoot(root, new ArrayList<SpatialObject>(nodes));
			numNodes++;
			height++;
			if (this.debug) {
				msg.append("\n  numNodes = ").append(numNodes);
			}
		}
		if (this.debug) {
			msg.append("\n  height = ").append(height);
			msg.append("\n  root " + getRoot());
			debugFine(msg.toString() + "\n");
		}
	}

	/**
	 * Creates and returns the directory nodes for bulk load.
	 * 
	 * @param nodes
	 *            the nodes to be inserted
	 * @return the directory nodes containing the nodes
	 */
	private List<N> createDirectoryNodes(List<N> nodes) {
		int minEntries = dirMinimum;
		int maxEntries = dirCapacity - 1;

		ArrayList<N> result = new ArrayList<N>();
		BulkSplit split = new BulkSplit();
		List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(nodes);
		List<List<SpatialObject>> partitions = split.partition(spatialObjects,
				minEntries, maxEntries, bulkLoadStrategy);

		for (List<SpatialObject> partition : partitions) {
			StringBuffer msg = new StringBuffer();

			// create node
			N dirNode = createNewDirectoryNode(dirCapacity);
			file.writePage(dirNode);
			result.add(dirNode);

			// insert nodes
			for (SpatialObject o : partition) {
				// noinspection unchecked
				N node = (N) o;
				dirNode.addDirectoryEntry(createNewDirectoryEntry(node));
			}

			// write to file
			file.writePage(dirNode);
			if (this.debug) {
				msg.append("\npageNo ").append(dirNode.getID());
				debugFiner(msg.toString() + "\n");
			}
		}

		return result;
	}

	/**
	 * Returns a root node for bulk load. If the objects are data objects a leaf
	 * node will be returned, if the objects are nodes a directory node will be
	 * returned.
	 * 
	 * @param root
	 *            the new root node
	 * @param objects
	 *            the spatial objects to be inserted
	 * @return the root node
	 */
	private N createRoot(N root, List<SpatialObject> objects) {
		// insert data
		for (SpatialObject object : objects) {
			if (object instanceof NumberVector) {
				// noinspection unchecked
				root.addLeafEntry(createNewLeafEntry((O) object));
			} else {
				// noinspection unchecked
				N node = (N) object;
				root.addDirectoryEntry(createNewDirectoryEntry(node));
			}
		}

		// write to file
		file.writePage(root);
		if (this.debug) {
			StringBuffer msg = new StringBuffer();
			msg.append("\npageNo ").append(root.getID());
			debugFiner(msg.toString() + "\n");
		}

		return root;
	}
}
