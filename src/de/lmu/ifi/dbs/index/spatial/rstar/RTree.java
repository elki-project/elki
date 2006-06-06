package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * RTree is a spatial index structure based on the concepts of the R*-Tree. Apart from organizing the objects
 * it also provides several methods to search for certain object in the
 * structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTree<O extends NumberVector> extends AbstractRTree<O> {
  /**
   * Holds the class specific debug status.
   */
  protected static boolean DEBUG = LoggingConfiguration.DEBUG;
//  protected static boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  protected Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Creates a new RTree.
   */
  public RTree() {
    super();
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  public RTreeNode getRoot() {
    return file.readPage(ROOT_NODE_ID);
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  protected boolean hasOverflow(RTreeNode node) {
    if (node.isLeaf())
      return node.getNumEntries() == leafCapacity;
    else
      return node.getNumEntries() == dirCapacity;
  }

  /**
   * Computes the height of this RTree. Is called by the constructur.
   * and should be overwritten by subclasses if necessary.
   *
   * @return the height of this RTree
   */
  protected int computeHeight() {
    RTreeNode node = getRoot();
    int height = 1;

    // compute height
    while (!node.isLeaf() && node.getNumEntries() != 0) {
      Entry entry = node.entries[0];
      node = getNode(entry.getID());
      height++;
    }
    return height;
  }

  /**
   * Creates an empty root node and writes it to file. Is called by the constructur
   * and should be overwritten by subclasses if necessary.
   *
   * @param dimensionality the dimensionality of the data objects to be stored
   */
  protected void createEmptyRoot(int dimensionality) {
    RTreeNode root = createNewLeafNode(leafCapacity);
    file.writePage(root);
    this.height = 1;
  }

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructur
   * and should be overwritten by subclasses if necessary.
   *
   * @param objects the data objects to be indexed
   */
  protected final void bulkLoad(List<O> objects) {
    StringBuffer msg = new StringBuffer();
    List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(objects);

    // root is leaf node
    double size = objects.size();
    if (size / (leafCapacity - 1.0) <= 1) {
      RTreeNode root = createNewLeafNode(leafCapacity);
      root.setID(ROOT_NODE_ID);
      file.writePage(root);
      createRoot(root, spatialObjects);
      height = 1;
      if (DEBUG) {
        msg.append("\n  numNodes = 1");
      }
    }

    // root is directory node
    else {
      RTreeNode root = createNewDirectoryNode(dirCapacity);
      root.setID(ROOT_NODE_ID);
      file.writePage(root);

      // create leaf nodes
      List<RTreeNode> nodes = createLeafNodes(objects);

      int numNodes = nodes.size();
      if (DEBUG) {
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
      if (DEBUG) {
        msg.append("\n  numNodes = ").append(numNodes);
      }
    }
    if (DEBUG) {
      msg.append("\n  height = ").append(height);
      msg.append("\n root " + getRoot());
      logger.fine(msg.toString() + "\n");
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RTreeNode createNewLeafNode(int capacity) {
    return new RTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RTreeNode createNewDirectoryNode(int capacity) {
    return new RTreeNode(file, capacity, false);
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   *
   * @param id     the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  protected LeafEntry createNewLeafEntry(int id, double[] values) {
    return new LeafEntry(id, values);
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   *
   * @param id  the unique id of the underlying spatial object
   * @param mbr the minmum bounding rectangle of the underlying spatial object
   */
  protected DirectoryEntry createNewDirectoryEntry(int id, MBR mbr) {
    return new DirectoryEntry(id, mbr);
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   *
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private List<RTreeNode> createDirectoryNodes(List<RTreeNode> nodes) {
    int minEntries = dirMinimum;
    int maxEntries = dirCapacity - 1;

    ArrayList<RTreeNode> result = new ArrayList<RTreeNode>();
    BulkSplit split = new BulkSplit();
    List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(nodes);
    List<List<SpatialObject>> partitions = split.partition(spatialObjects,
                                                           minEntries,
                                                           maxEntries,
                                                           bulkLoadStrategy);

    for (List<SpatialObject> partition : partitions) {
      StringBuffer msg = new StringBuffer();

      // create node
      RTreeNode dirNode = createNewDirectoryNode(dirCapacity);
      file.writePage(dirNode);
      result.add(dirNode);

      // insert nodes
      for (SpatialObject o : partition) {
        dirNode.addNode((RTreeNode) o);
      }

      // write to file
      file.writePage(dirNode);
      if (DEBUG) {
        msg.append("\npageNo ").append(dirNode.getID());
        logger.finer(msg.toString() + "\n");
      }
    }

    logger.info("numDirPages " + result.size());
    return result;
  }

  /**
   * Returns a root node for bulk load, if the objects are data objects a
   * leaf node will be returned, if the pbjects are RTreeNodes a directory node
   * will be returned.
   *
   * @param root    the new root node
   * @param objects the spatial objects to be inserted
   * @return the root node
   */
  private RTreeNode createRoot(RTreeNode root, List<SpatialObject> objects) {
    // insert data
    for (SpatialObject object : objects) {
      if (object instanceof NumberVector) {
        //noinspection unchecked
        LeafEntry entry =createNewLeafEntry(object.getID(), getValues((O) object));
        root.addLeafEntry(entry);
      }
      else {
        root.addNode((RTreeNode) object);
      }
    }

    // write to file
    file.writePage(root);
    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\npageNo ").append(root.getID());
      logger.finer(msg.toString() + "\n");
    }

    return root;
  }
}
