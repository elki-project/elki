package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract superclass for all non-flat R*-Tree variants.
 * 
 * @author Elke Achtert
 * @param <O> Object type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class NonFlatRStarTree<O extends NumberVector<O, ?>, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends AbstractRStarTree<O, N, E> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public NonFlatRStarTree(Parameterization config) {
    super(config);
  }

  /**
   * Returns true if in the specified node an overflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  @Override
  protected boolean hasOverflow(N node) {
    if(node.isLeaf()) {
      return node.getNumEntries() == leafCapacity;
    }
    else {
      return node.getNumEntries() == dirCapacity;
    }
  }

  /**
   * Returns true if in the specified node an underflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occurred, false
   *         otherwise
   */
  @Override
  protected boolean hasUnderflow(N node) {
    if(node.isLeaf()) {
      return node.getNumEntries() < leafMinimum;
    }
    else {
      return node.getNumEntries() < dirMinimum;
    }
  }

  /**
   * Computes the height of this RTree. Is called by the constructor. and should
   * be overwritten by subclasses if necessary.
   * 
   * @return the height of this RTree
   */
  @Override
  protected int computeHeight() {
    N node = getRoot();
    int height = 1;

    // compute height
    while(!node.isLeaf() && node.getNumEntries() != 0) {
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
   * the constructor and should be overwritten by subclasses if necessary.
   * 
   * @param objects the data objects to be indexed
   */
  @Override
  protected void bulkLoad(List<O> objects) {
    StringBuffer msg = new StringBuffer();
    List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(objects);

    // root is leaf node
    double size = objects.size();
    if(size / (leafCapacity - 1.0) <= 1) {
      N root = createNewLeafNode(leafCapacity);
      root.setID(getRootEntry().getID());
      file.writePage(root);
      createRoot(root, spatialObjects);
      setHeight(1);
      if(logger.isDebugging()) {
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
      if(logger.isDebugging()) {
        msg.append("\n  numLeafNodes = ").append(numNodes);
      }
      setHeight(1);

      // create directory nodes
      while(nodes.size() > (dirCapacity - 1)) {
        nodes = createDirectoryNodes(nodes);
        numNodes += nodes.size();
        setHeight(getHeight() + 1);
      }

      // create root
      createRoot(root, new ArrayList<SpatialObject>(nodes));
      numNodes++;
      setHeight(getHeight() + 1);
      if(logger.isDebugging()) {
        msg.append("\n  numNodes = ").append(numNodes);
      }
    }
    if(logger.isDebugging()) {
      msg.append("\n  height = ").append(getHeight());
      msg.append("\n  root " + getRoot());
      logger.debugFine(msg.toString() + "\n");
    }
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   * 
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private List<N> createDirectoryNodes(List<N> nodes) {
    int minEntries = dirMinimum;
    int maxEntries = dirCapacity - 1;

    ArrayList<N> result = new ArrayList<N>();
    BulkSplit<N> split = new BulkSplit<N>();
    List<List<N>> partitions = split.partition(nodes, minEntries, maxEntries, bulkLoadStrategy);

    for(List<N> partition : partitions) {
      // create node
      N dirNode = createNewDirectoryNode(dirCapacity);
      file.writePage(dirNode);
      result.add(dirNode);

      // insert nodes
      for(N o : partition) {
        dirNode.addDirectoryEntry(createNewDirectoryEntry(o));
      }

      // write to file
      file.writePage(dirNode);
      if(logger.isDebuggingFiner()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\npageNo ").append(dirNode.getID());
        logger.debugFiner(msg.toString() + "\n");
      }
    }

    return result;
  }

  /**
   * Returns a root node for bulk load. If the objects are data objects a leaf
   * node will be returned, if the objects are nodes a directory node will be
   * returned.
   * 
   * @param root the new root node
   * @param objects the spatial objects to be inserted
   * @return the root node
   */
  @SuppressWarnings("unchecked")
  private N createRoot(N root, List<SpatialObject> objects) {
    // insert data
    for(SpatialObject object : objects) {
      if(object instanceof NumberVector) {
        root.addLeafEntry(createNewLeafEntry((O) object));
      }
      else {
        root.addDirectoryEntry(createNewDirectoryEntry((N) object));
      }
    }

    // set root mbr
    getRootEntry().setMBR(root.mbr());

    // write to file
    file.writePage(root);
    if(logger.isDebuggingFiner()) {
      StringBuffer msg = new StringBuffer();
      msg.append("pageNo ").append(root.getID());
      logger.debugFiner(msg.toString() + "\n");
    }

    return root;
  }
}