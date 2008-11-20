package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract superclass for all non-flat R*-Tree variants.
 * 
 * @author Elke Achtert 
 */
public abstract class NonFlatRStarTree<O extends NumberVector<O,?>, N extends AbstractRStarTreeNode<N, E>, E
    extends SpatialEntry> extends AbstractRStarTree<O, N, E> {

	/**
	 * Creates a new RTree.
	 */
	public NonFlatRStarTree() {
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
	@Override
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
	@Override
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
	@Override
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

	@Override
  protected void createEmptyRoot(@SuppressWarnings("unused") O object) {
		N root = createNewLeafNode(leafCapacity);
		file.writePage(root);
    setHeight(1);
	}

	/**
	 * Performs a bulk load on this RTree with the specified data. Is called by
	 * the constructur and should be overwritten by subclasses if necessary.
	 * 
	 * @param objects
	 *            the data objects to be indexed
	 */
	@Override
  protected void bulkLoad(List<O> objects) {
		StringBuffer msg = new StringBuffer();
		List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(objects);

		// root is leaf node
		double size = objects.size();
		if (size / (leafCapacity - 1.0) <= 1) {
			N root = createNewLeafNode(leafCapacity);
			root.setID(getRootEntry().getID());
			file.writePage(root);
			createRoot(root, spatialObjects);
			setHeight(1);
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
			setHeight(1);

			// create directory nodes
			while (nodes.size() > (dirCapacity - 1)) {
				nodes = createDirectoryNodes(nodes);
				numNodes += nodes.size();
        setHeight(getHeight()+1);
			}

			// create root
			createRoot(root, new ArrayList<SpatialObject>(nodes));
			numNodes++;
      setHeight(getHeight()+1);
			if (this.debug) {
				msg.append("\n  numNodes = ").append(numNodes);
			}
		}
		if (this.debug) {
			msg.append("\n  height = ").append(getHeight());
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
		BulkSplit<N> split = new BulkSplit<N>();
		List<List<N>> partitions = split.partition(nodes,
				minEntries, maxEntries, bulkLoadStrategy);

		for (List<N> partition : partitions) {
			StringBuffer msg = new StringBuffer();

			// create node
			N dirNode = createNewDirectoryNode(dirCapacity);
			file.writePage(dirNode);
			result.add(dirNode);

			// insert nodes
			for (N o : partition) {
				dirNode.addDirectoryEntry(createNewDirectoryEntry(o));
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
  @SuppressWarnings("unchecked")
  private N createRoot(N root, List<SpatialObject> objects) {
		// insert data
		for (SpatialObject object : objects) {
			if (object instanceof NumberVector) {
				root.addLeafEntry(createNewLeafEntry((O) object));
			} else {
				root.addDirectoryEntry(createNewDirectoryEntry((N) object));
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
