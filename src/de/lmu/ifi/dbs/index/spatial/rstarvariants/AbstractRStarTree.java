package de.lmu.ifi.dbs.index.spatial.rstarvariants;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.index.*;
import de.lmu.ifi.dbs.index.spatial.*;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.util.Enlargement;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.heap.*;

import java.util.*;

/**
 * Abstract superclass for index structures based on a R*-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractRStarTree<O extends NumberVector, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> extends SpatialIndex<O, N, E> {

  /**
   * Contains a boolean for each level of this R*-Tree that indicates
   * if there was already a reinsert operation in this level
   * during the current insert / delete operation.
   */
  private final Map<Integer, Boolean> reinsertions = new HashMap<Integer, Boolean>();

  /**
   * The height of this R*-Tree.
   */
  private int height;

  public AbstractRStarTree() {
    super();
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  public void insert(O o) {
    if (this.debug) {
      debugFine("insert object " + o.getID() + "\n");
    }

    if (!initialized) {
      initialize(o);
    }

    reinsertions.clear();
    insertLeafEntry(createNewLeafEntry(o));

    if (debug) {
      N root = getRoot();
      root.test();
    }
  }

  /**
   * Inserts the specified objects into this index. If a bulk load mode
   * is implemented, the objects are inserted in one bulk.
   *
   * @param objects the objects to be inserted
   */
  public void insert(List<O> objects) {
    if (objects.isEmpty()) return;

    if (bulk && !initialized) {
      initialize(objects.get(0));
      bulkLoad(objects);
      if (this.debug) {
        StringBuffer msg = new StringBuffer();
        msg.append(" height  = ").append(height).append("\n");
        msg.append(" root    = ").append(getRoot());
        debugFine(msg.toString());
      }
    }
    else {
      if (!initialized)
        initialize(objects.get(0));
      for (O object : objects) {
        insert(object);
      }
    }
  }

  /**
   * Inserts the specified leaf entry into this R*-Tree.
   *
   * @param entry the leaf entry to be inserted
   */
  protected void insertLeafEntry(E entry) {
    // choose node for insertion
    MBR mbr = entry.getMBR();
    IndexPath<E> subtree = choosePath(getRootPath(), mbr, 1);

    if (this.debug) {
      debugFine("\ninsertion-subtree " + subtree + "\n");
    }

    N parent = getNode(subtree.getLastPathComponent().getEntry());
    parent.addLeafEntry(entry);
    file.writePage(parent);

    // adjust the tree from subtree to root
    adjustTree(subtree);
  }

  /**
   * Inserts the specified directory entry at the specified level into this R*-Tree.
   *
   * @param entry the directory entry to be inserted
   * @param level the level at which the directory entry is to be inserted
   */
  protected void insertDirectoryEntry(E entry, int level) {
    // choose node for insertion of o
    MBR mbr = entry.getMBR();
    IndexPath<E> subtree = choosePath(getRootPath(), mbr, level);
    if (this.debug) {
      debugFine("\nsubtree " + subtree);
    }

    N parent = getNode(subtree.getLastPathComponent().getEntry());
    parent.addDirectoryEntry(entry);
    file.writePage(parent);

    // adjust the tree from subtree to root
    adjustTree(subtree);
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object with the specified id,
   *         false otherwise
   */
  public boolean delete(O o) {
    if (this.debug) {
      debugFine("delete " + o.getID() + "\n");
    }

    // find the leaf node containing o
    double[] values = getValues(o);
    MBR mbr = new MBR(values, values);
    IndexPath<E> deletionPath = findPathToObject(getRootPath(), mbr, o.getID());
    if (deletionPath == null) return false;

    N leaf = getNode(deletionPath.getParentPath().getLastPathComponent().getEntry());
    int index = deletionPath.getLastPathComponent().getIndex();

    // delete o
    leaf.deleteEntry(index);
    file.writePage(leaf);

    // condense the tree
    Stack<N> stack = new Stack<N>();
    condenseTree(deletionPath.getParentPath(), stack);

    // reinsert underflow nodes
    while (!stack.empty()) {
      N node = stack.pop();
      if (node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          reinsertions.clear();
          this.insertLeafEntry(node.getEntry(i));
        }
      }
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          stack.push(getNode(node.getEntry(i)));
        }
      }
      file.deletePage(node.getID());
    }

    if (debug) {
      N root = getRoot();
      root.test();
    }

    return true;
  }

  /**
   * Performs a range query for the given spatial objec with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object           the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public <D extends Distance<D>> List<QueryResult<D>> rangeQuery(O object, String epsilon,
                                                                 DistanceFunction<O, D> distanceFunction) {

    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    SpatialDistanceFunction<O, D> df = (SpatialDistanceFunction<O, D>) distanceFunction;

    D range = distanceFunction.valueOf(epsilon);
    final List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    final Heap<D, Identifiable> pq = new DefaultHeap<D, Identifiable>();

    // push root
    pq.addNode(new DefaultHeapNode<D, Identifiable>(distanceFunction.nullDistance(), new DefaultIdentifiable(getRootEntry().getID())));

    // search in tree
    while (!pq.isEmpty()) {
      HeapNode<D, Identifiable> pqNode = pq.getMinNode();
      if (pqNode.getKey().compareTo(range) > 0) break;

      N node = getNode(pqNode.getValue().getID());
      final int numEntries = node.getNumEntries();

      for (int i = 0; i < numEntries; i++) {
        D distance = df.minDist(node.getEntry(i).getMBR(), object);
        if (distance.compareTo(range) <= 0) {
          E entry = node.getEntry(i);
          if (node.isLeaf()) {
            result.add(new QueryResult<D>(entry.getID(), distance));
          }
          else {
            pq.addNode(new DefaultHeapNode<D, Identifiable>(distance, new DefaultIdentifiable(entry.getID())));
          }
        }
      }
    }

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  /**
   * Performs a k-nearest neighbor query for the given NumberVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object           the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public <D extends Distance<D>> List<QueryResult<D>> kNNQuery(O object, int k,
                                                               DistanceFunction<O, D> distanceFunction) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    final KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
    doKNNQuery(object, distanceFunction, knnList);
    return knnList.toList();
  }

  /**
   * Performs a bulk k-nearest neighbor query for the given object IDs. The
   * query result is in ascending order to the distance to the query objects.
   *
   * @param ids              the query objects
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public <D extends Distance<D>>List<List<QueryResult<D>>> bulkKNNQueryForID(List<Integer> ids, int k, DistanceFunction<O, D> distanceFunction) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one enumeration has to be requested!");
    }

    final Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>(ids.size());
    for (Integer id : ids) {
      knnLists.put(id, new KNNList<D>(k, distanceFunction.infiniteDistance()));
    }

    batchNN(getRoot(), ids, distanceFunction, knnLists);

    List<List<QueryResult<D>>> result = new ArrayList<List<QueryResult<D>>>();
    for (Integer id : ids) {
      result.add(knnLists.get(id).toList());
    }
    return result;
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object           the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public <D extends Distance<D>>List<QueryResult<D>> reverseKNNQuery(O object, int k, DistanceFunction<O, D> distanceFunction) {
    throw new UnsupportedOperationException("Not yet supported!");
  }

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   *
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public List<E> getLeaves() {
    List<E> result = new ArrayList<E>();

    if (height == 1) {
      result.add(getRootEntry());
      return result;
    }

    getLeafNodes(getRoot(), result, height);
    return result;
  }

  /**
   * Returns the height of this R*-Tree.
   *
   * @return the height of this R*-Tree
   */
  public int getHeight() {
    return height;
  }

  /**
   * Returns a string representation of this RTree.
   *
   * @return a string representation of this RTree
   */
  public String toString() {
    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;

    N node = getRoot();
    int dim = node.getDimensionality();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        E entry = node.getEntry(0);
        node = getNode(entry);
        levels++;
      }
    }

    BreadthFirstEnumeration<N, E> enumeration = new BreadthFirstEnumeration<N, E>(file, getRootPath());
    while (enumeration.hasMoreElements()) {
      E entry = enumeration.nextElement().getLastPathComponent().getEntry();
      if (entry.isLeafEntry())
        objects++;
      else {
        node = getNode(entry);
        if (node.isLeaf())
          leafNodes++;
        else {
          dirNodes++;
        }
      }
    }

    result.append(getClass().getName()).append(" has ").append((levels + 1)).append(" levels.\n");
    result.append(dirNodes).append(" Directory Knoten (max = ").append(dirCapacity - 1).append(", min = ").append(dirMinimum).append(")\n");
    result.append(leafNodes).append(" Daten Knoten (max = ").append(leafCapacity - 1).append(", min = ").append(leafMinimum).append(")\n");
    result.append(objects).append(" ").append(dim).append("-dim. Punkte im Baum \n");
    result.append("Read I/O-Access: ").append(file.getPhysicalReadAccess()).append("\n");
    result.append("Write I/O-Access: ").append(file.getPhysicalWriteAccess()).append("\n");
    result.append("Logical Page-Access: ").append(file.getLogicalPageAccess()).append("\n");
    result.append("File ").append(file.getClass()).append("\n");

    return result.toString();
  }

  /**
   * Initializes this R*-Tree from an existing persistent file.
   */
  protected void initializeFromFile() {
    super.initializeFromFile();
    // compute height
    this.height = computeHeight();

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append(getClass());
      msg.append("\n height = ").append(height);
      debugFine(msg.toString());
    }
  }

  /**
   * @see Index#initializeCapacities(de.lmu.ifi.dbs.data.DatabaseObject)
   */
  protected void initializeCapacities(O object) {
    int dimensionality = object.getDimensionality();

    // overhead = numEntries(4), id(4), isLeaf(0.125)
    double overhead = 8.125;
    if (pageSize - overhead < 0)
      throw new IllegalArgumentException("Node size of " + pageSize + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (childID + childMBR) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 16 * dimensionality) + 1;

    if (dirCapacity <= 1)
      throw new IllegalArgumentException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10) {
      if (this.debug) {
        warning("Page size is choosen very small! Maximum number of entries " +
                "in a directory node = " + (dirCapacity - 1));
      }
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if (dirMinimum < 2)
      dirMinimum = 2;

    // leafCapacity = (pageSize - overhead) / (childID + childValues) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + 8 * dimensionality) + 1;

    if (leafCapacity <= 1)
      throw new IllegalArgumentException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10) {
      if (this.debug) {
        warning("Page size is choosen very small! Maximum number of entries " +
                "in a leaf node = " + (leafCapacity - 1));
      }
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if (leafMinimum < 2)
      leafMinimum = 2;
  }

  /**
   * Performs a k-nearest neighbor query for the given NumberVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object           the query object
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @param knnList          the knn list containing the result
   */
  protected <D extends Distance<D>> void doKNNQuery(Object object,
                                                    DistanceFunction<O, D> distanceFunction,
                                                    KNNList<D> knnList) {

    if (!(distanceFunction instanceof SpatialDistanceFunction))
      throw new IllegalArgumentException("Distance function must be an instance of SpatialDistanceFunction!");
    SpatialDistanceFunction<O, D> df = (SpatialDistanceFunction<O, D>) distanceFunction;

    // variables
    final Heap<D, Identifiable> pq = new DefaultHeap<D, Identifiable>();

    // push root
    pq.addNode(new DefaultHeapNode<D, Identifiable>(distanceFunction.nullDistance(), new DefaultIdentifiable(getRootEntry().getID())));
    D maxDist = distanceFunction.infiniteDistance();

    // search in tree
    while (!pq.isEmpty()) {
      HeapNode<D, Identifiable> pqNode = pq.getMinNode();

      if (pqNode.getKey().compareTo(maxDist) > 0) {
        return;
      }

      N node = getNode(pqNode.getValue().getID());
      // data node
      if (node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          E entry = node.getEntry(i);
          //noinspection unchecked
          D distance = object instanceof Integer ?
                       df.minDist(entry.getMBR(), (Integer) object) :
                       df.minDist(entry.getMBR(), (O) object);

          if (distance.compareTo(maxDist) <= 0) {
            knnList.add(new QueryResult<D>(entry.getID(), distance));
            maxDist = knnList.getKNNDistance();
          }
        }
      }
      // directory node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          E entry = node.getEntry(i);
          //noinspection unchecked
          D distance = object instanceof Integer ?
                       df.minDist(entry.getMBR(), (Integer) object) :
                       df.minDist(entry.getMBR(), (O) object);
          if (distance.compareTo(maxDist) <= 0) {
            pq.addNode(new DefaultHeapNode<D, Identifiable>(distance, new DefaultIdentifiable(entry.getID())));
          }
        }
      }
    }
  }

  /**
   * Performs a batch knn query.
   *
   * @param node     the node for which the query should be performed
   * @param ids      the ids of th query objects
   * @param knnLists the knn lists of the query objcets
   */
  protected <D extends Distance<D>> void batchNN(N node, List<Integer> ids, DistanceFunction<O, D> distanceFunction, Map<Integer, KNNList<D>> knnLists) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        SpatialEntry p = node.getEntry(i);
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          D dist_pq = distanceFunction.distance(p.getID(), q);
          if (dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q.add(new QueryResult<D>(p.getID(), dist_pq));
          }
        }
      }
    }
    else {
      /*
      List<DistanceEntry<D>> entries = getSortedEntries(node, ids, distanceFunction);
      for (DistanceEntry<D> distEntry : entries) {
        D minDist = distEntry.getDistance();
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          if (minDist.compareTo(knn_q_maxDist) <= 0) {
            MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) distEntry.getEntry();
            MTreeNode<O, D> child = getNode(entry);
            batchNN(child, ids, knnLists);
            break;
          }
        }
      }  */
    }
  }

  /**
   * Returns the path to the leaf entry in the specified subtree
   * that represents the data object with the specified mbr and id.
   *
   * @param subtree the subtree to be tested
   * @param mbr     the mbr to look for
   * @param id      the id to look for
   * @return the path to the leaf entry of the specified subtree
   *         that represents the data object with the specified mbr and id
   */
  protected IndexPath<E> findPathToObject(IndexPath<E> subtree, MBR mbr, int id) {
    N node = getNode(subtree.getLastPathComponent().getEntry());
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        if (node.getEntry(i).getID() == id) {
          return subtree.pathByAddingChild(new IndexPathComponent<E>(node.getEntry(i), i));
        }
      }
    }
    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        if (node.getEntry(i).getMBR().intersects(mbr)) {
          IndexPath<E> childSubtree = subtree.pathByAddingChild(new IndexPathComponent<E>(node.getEntry(i), i));
          IndexPath<E> path = findPathToObject(childSubtree, mbr, id);
          if (path != null) return path;
        }
      }
    }
    return null;
  }

  /**
   * Creates and returns the leaf nodes for bulk load.
   *
   * @param objects the objects to be inserted
   * @return the array of leaf nodes containing the objects
   */
  protected List<N> createLeafNodes(List<O> objects) {
    int minEntries = leafMinimum;
    int maxEntries = leafCapacity - 1;

    ArrayList<N> result = new ArrayList<N>();
    BulkSplit split = new BulkSplit();
    List<SpatialObject> spatialObjects = new ArrayList<SpatialObject>(objects);
    List<List<SpatialObject>> partitions = split.partition(spatialObjects,
                                                           minEntries,
                                                           maxEntries,
                                                           bulkLoadStrategy);

    for (List<SpatialObject> partition : partitions) {
      StringBuffer msg = new StringBuffer();

      // create leaf node
      N leafNode = createNewLeafNode(leafCapacity);
      file.writePage(leafNode);
      result.add(leafNode);

      // insert data
      for (SpatialObject o : partition) {
        //noinspection unchecked
        leafNode.addLeafEntry(createNewLeafEntry((O) o));
      }

      // write to file
      file.writePage(leafNode);

      if (this.debug) {
        msg.append("\npageNo ").append(leafNode.getID()).append("\n");
        debugFine(msg.toString());

      }
    }

    if (this.debug) {
      debugFine("numDataPages = " + result.size());
    }
    return result;
  }

  /**
   * Sorts the entries of the specified node according to their minimum
   * distance to the specified object.
   *
   * @param node the node
   * @param q    the id of the object
   * @return a list of the sorted entries
   */
  protected <D extends Distance<D>> List<DistanceEntry<D, E>> getSortedEntries(N node,
                                                                               Integer q,
                                                                               SpatialDistanceFunction<O, D> distanceFunction) {
    List<DistanceEntry<D, E>> result = new ArrayList<DistanceEntry<D, E>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      D minDist = distanceFunction.minDist(entry.getMBR(), q);
      result.add(new DistanceEntry<D, E>(entry, minDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Returns a double array consisting of the values of the specified real vector.
   *
   * @param object the real vector
   * @return a double array consisting of the values of the specified real vector
   */
  protected double[] getValues(O object) {
    int dim = object.getDimensionality();
    double[] values = new double[dim];
    for (int i = 0; i < dim; i++) {
      values[i] = object.getValue(i + 1).doubleValue();
    }
    return values;
  }

  /**
   * Sets the height of this R*-Tree.
   * @param height the height to be set
   */
  protected void setHeight(int height) {
    this.height = height;
  }

  /**
   * Clears the reinsertions.
   */
  protected void clearReinsertions() {
    reinsertions.clear();
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  abstract protected boolean hasOverflow(N node);

  /**
   * Returns true if in the specified node an underflow occured, false otherwise.
   *
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occured, false otherwise
   */
  abstract protected boolean hasUnderflow(N node);

  /**
   * Computes the height of this RTree. Is called by the constructor.
   *
   * @return the height of this RTree
   */
  abstract protected int computeHeight();

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  abstract protected N createNewLeafNode(int capacity);

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  abstract protected N createNewDirectoryNode(int capacity);

  /**
   * Creates a new leaf entry representing the specified data object.
   *
   * @param object the data object to be represented by the new entry
   */
  abstract protected E createNewLeafEntry(O object);

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   */
  abstract protected E createNewDirectoryEntry(N node);

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructor.
   *
   * @param objects the data objects to be indexed
   */
  abstract protected void bulkLoad(List<O> objects);

  /**
   * Creates a new root node that points to the two specified child nodes
   * and return the path to the new root.
   *
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @return the path to the new root node that points to the two specified child nodes
   */
  private IndexPath<E> createNewRoot(final N oldRoot, final N newNode) {
    N root = createNewDirectoryNode(dirCapacity);
    file.writePage(root);

    // switch the ids
    oldRoot.setID(root.getID());
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        N node = getNode(oldRoot.getEntry(i));
        file.writePage(node);
      }
    }

    root.setID(getRootEntry().getID());
    root.addDirectoryEntry(createNewDirectoryEntry(oldRoot));
    root.addDirectoryEntry(createNewDirectoryEntry(newNode));
    getRootEntry().setMBR(root.mbr());

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    if (this.debug) {
      String msg = "\nCreate new Root: ID=" + root.getID();
      msg += "\nchild1 " + oldRoot;
      msg += "\nchild2 " + newNode;
      msg += "\n";
      debugFine(msg);
    }

    height++;

    return new IndexPath<E>(new IndexPathComponent<E>(getRootEntry(), null));
  }

  /**
   * Chooses the best path of the specified subtree for insertion of
   * the given mbr at the specified level.
   *
   * @param subtree the subtree to be tested for insertion
   * @param mbr     the mbr to be inserted
   * @param level   the level at which the mbr should be inserted (level 1 indicates leaf-level)
   * @return the path of the appropriate subtree to insert the given mbr
   */
  private IndexPath<E> choosePath(IndexPath<E> subtree, MBR mbr, int level) {
    if (this.debug) {
      debugFiner("node " + subtree + ", level " + level);
//      logger.finer("node " + subtree + ", level " + level);
    }

    N node = getNode(subtree.getLastPathComponent().getEntry());
    if (node.isLeaf()) return subtree;

    N childNode = getNode(node.getEntry(0));
    // children are leafs
    if (childNode.isLeaf()) {
      if (height - subtree.getPathCount() == level) {
        return subtree.pathByAddingChild(getChildWithLeastOverlap(node, mbr));
      }
      else
        throw new IllegalArgumentException("childNode is leaf, but currentLevel != level: " +
                                           (height - subtree.getPathCount()) + " != " + level);
    }
    // children are directory nodes
    else {
      IndexPath<E> newSubtree = subtree.pathByAddingChild(getLeastEnlargement(node, mbr));
      // desired level is reached
      if (height - subtree.getPathCount() == level) {
        return newSubtree;
      }
      else {
        return choosePath(newSubtree, mbr, level);
      }
    }
  }

  /**
   * Returns the path information of the entry of the specified node
   * with the least enlargement if the given mbr would be inserted into.
   *
   * @param node the node which children have to be tested
   * @param mbr  the mbr of the node to be inserted
   * @return the path information of the entry with the least enlargement
   *         if the given mbr would be inserted into
   */
  private IndexPathComponent<E> getLeastEnlargement(N node, MBR mbr) {
    Enlargement<E> min = null;

    for (int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      double volume = entry.getMBR().volume();
      MBR newMBR = entry.getMBR().union(mbr);
      double inc = newMBR.volume() - volume;
      Enlargement<E> enlargement = new Enlargement<E>(new IndexPathComponent<E>(entry, i), volume, inc, 0);

      if (min == null || min.compareTo(enlargement) > 0)
        min = enlargement;
    }

    assert min != null;
    return min.getPathComponent();
  }

  /**
   * Returns the path information of the entry of the specified node
   * which needs least overlap enlargement if the
   * given mbr would be inserted into.
   *
   * @param node the node of which the children should be tested
   * @param mbr  the mbr to be inserted into the children
   * @return the path information of the entry which needs least overlap
   *         enlargement if the given mbr would be inserted into
   */
  private IndexPathComponent<E> getChildWithLeastOverlap(N node, MBR mbr) {
    Enlargement<E> min = null;

    for (int i = 0; i < node.getNumEntries(); i++) {
      E entry_i = node.getEntry(i);
      MBR newMBR = entry_i.getMBR().union(mbr);

      double currOverlap = 0;
      double newOverlap = 0;
      for (int k = 0; k < node.getNumEntries(); k++) {
        if (i != k) {
          E entry_k = node.getEntry(k);
          currOverlap += entry_i.getMBR().overlap(entry_k.getMBR());
          newOverlap += newMBR.overlap(entry_k.getMBR());
        }
      }

      double volume = entry_i.getMBR().volume();
      double inc_volume = newMBR.volume() - volume;
      double inc_overlap = newOverlap - currOverlap;
      Enlargement<E> enlargement = new Enlargement<E>(new IndexPathComponent<E>(entry_i, i),
                                                      volume,
                                                      inc_volume,
                                                      inc_overlap);

      if (min == null || min.compareTo(enlargement) > 0)
        min = enlargement;
    }

    assert min != null;
    return min.getPathComponent();
  }

  /**
   * Treatment of overflow in the specified node:
   * if the node is not the root node and this is the first call of overflowTreatment
   * in the given level during insertion the specified node will be reinserted,
   * otherwise the node will be splitted.
   *
   * @param node the node where an overflow occured
   * @param path the path to the specified node
   * @return the path to the newly created split node in case of split, null in case of reinsertion
   */
  private N overflowTreatment(N node, IndexPath<E> path) {
    int level = height - path.getPathCount() + 1;
    Boolean reInsert = reinsertions.get(level);

    // there was still no reinsert operation at this level
    if (node.getID() != 0 && (reInsert == null || ! reInsert)) {
      reinsertions.put(level, true);
      if (this.debug) {
        debugFine("REINSERT " + reinsertions + "\n");
      }
      reInsert(node, level, path);
      return null;
    }

    // there was already a reinsert operation at this level
    else {
      return split(node);
    }
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private N split(N node) {
    // choose the split dimension and the split point
    int minimum = node.isLeaf() ? leafMinimum : dirMinimum;
    TopologicalSplit<E> split = new TopologicalSplit<E>(node.getEntries(), minimum);

    // do the split
    N newNode;
    if (split.getBestSorting() == SpatialComparator.MIN) {
      newNode = node.splitEntries(split.getMinSorting(), split.getSplitPoint());
    }
    else if (split.getBestSorting() == SpatialComparator.MAX) {
      newNode = node.splitEntries(split.getMaxSorting(), split.getSplitPoint());
    }
    else
      throw new IllegalStateException("split.bestSort is undefined!");

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    if (this.debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("Split Node ").append(node.getID()).append(" (").append(getClass()).append(")\n");
      msg.append("      splitAxis ").append(split.getSplitAxis()).append("\n");
      msg.append("      splitPoint ").append(split.getSplitPoint()).append("\n");
      msg.append("      newNode ").append(newNode.getID()).append("\n");
      debugFine(msg.toString());
    }

    return newNode;
  }

  /**
   * Reinserts the specified node at the specified level.
   *
   * @param node  the node to be reinserted
   * @param level the level of the node
   * @param path  the path to the node
   */
  private void reInsert(N node, int level, IndexPath<E> path) {
    MBR mbr = node.mbr();
    EuklideanDistanceFunction distFunction = new EuklideanDistanceFunction();
    //noinspection unchecked
    DistanceEntry<DoubleDistance, E>[] reInsertEntries = new DistanceEntry[node.getNumEntries()];

    // compute the center distances of entries to the node and sort it
    // in decreasing order to their distances
    for (int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      DoubleDistance dist = distFunction.centerDistance(mbr, entry.getMBR());
      reInsertEntries[i] = new DistanceEntry<DoubleDistance, E>(entry, dist, i);
    }
    Arrays.sort(reInsertEntries);

    // define, how many entries will be reinserted
    int start = (int) (0.3 * (double) node.getNumEntries());

    // initialize the reinsertion operation: move the remaining entries forward
    node.initReInsert(start, reInsertEntries);
    file.writePage(node);

    // and adapt the mbrs
    IndexPath<E> childPath = path;
    N child = node;
    while (childPath.getParentPath() != null) {
      N parent = getNode(childPath.getParentPath().getLastPathComponent().getEntry());
      int indexOfChild = childPath.getLastPathComponent().getIndex();
      parent.getEntry(indexOfChild).setMBR(child.mbr());
      file.writePage(parent);
      childPath = childPath.getParentPath();
      child = parent;
    }

    // reinsert the first entries
    for (int i = 0; i < start; i++) {
      DistanceEntry<DoubleDistance, E> re = reInsertEntries[i];
      if (node.isLeaf()) {
        if (this.debug) {
          debugFine("reinsert " + re.getEntry());
        }
        insertLeafEntry(re.getEntry());
      }
      else {
        if (this.debug) {
          debugFine("reinsert " + re.getEntry() + " at " + level);
        }
        insertDirectoryEntry(re.getEntry(), level);
      }
    }
  }

  /**
   * Adjusts the tree after insertion of some nodes.
   *
   * @param subtree the subtree to be adjusted
   */
  private void adjustTree(IndexPath<E> subtree) {
    if (this.debug) {
      debugFine("\nAdjust tree " + subtree + "\n");
    }

    // get the root of the subtree
    N node = getNode(subtree.getLastPathComponent().getEntry());

    // overflow in node
    if (hasOverflow(node)) {
      // treatment of overflow: reinsertion or split
      N split = overflowTreatment(node, subtree);

      // node was splitted
      if (split != null) {
        // if root was split: create a new root that points the two split nodes
        if (node.getID() == getRootEntry().getID()) {
          adjustTree(createNewRoot(node, split));
        }
        // node is not root
        else {
          // get the parent and add the new split node
          N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
          if (this.debug) {
            debugFine("\nparent " + parent);
//            logger.fine("\nparent " + parent);
          }
          int index2 = parent.addDirectoryEntry(createNewDirectoryEntry(split));

          // adjust the mbrs in the parent node
          int index1 = subtree.getLastPathComponent().getIndex();
          E entry1 = parent.getEntry(index1);
          MBR mbr1 = node.mbr();
          entry1.setMBR(mbr1);
          E entry2 = parent.getEntry(index2);
          MBR mbr2 = split.mbr();
          entry2.setMBR(mbr2);

          // write changes in parent to file
          file.writePage(parent);
          adjustTree(subtree.getParentPath());
        }
      }
    }
    // no overflow, only adjust mbr of node in parent
    else {
      if (node.getID() != getRootEntry().getID()) {
        N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
        int index = subtree.getLastPathComponent().getIndex();
        E entry = parent.getEntry(index);
        MBR newMbr = node.mbr();
        entry.setMBR(newMbr);
        // write changes in parent to file
        file.writePage(parent);
        adjustTree(subtree.getParentPath());
      }
      // root level is reached
      else {
        MBR newMbr = node.mbr();
        getRootEntry().setMBR(newMbr);
      }
    }
  }

  /**
   * Condenses the tree after deletion of some nodes.
   *
   * @param subtree the subtree to be condensed
   * @param stack   the stack holding the nodes to be reinserted
   *                after the tree has been condensed
   */
  private void condenseTree(IndexPath<E> subtree, Stack<N> stack) {
    N node = getNode(subtree.getLastPathComponent().getEntry());
    // node is not root
    if (node.getID() != getRootEntry().getID()) {
      N parent = getNode(subtree.getParentPath().getLastPathComponent().getEntry());
      int index = subtree.getLastPathComponent().getIndex();
      if (hasUnderflow(node)) {
        parent.deleteEntry(index);
        stack.push(node);
      }
      else {
        parent.getEntry(index).setMBR(node.mbr());
      }
      file.writePage(parent);
      // get subtree to parent
      condenseTree(subtree.getParentPath(), stack);
    }

    // node is root
    else {
      if (node.getNumEntries() == 1 && !node.isLeaf()) {
        N child = getNode(node.getEntry(0));
        N newRoot;
        if (child.isLeaf()) {
          newRoot = createNewLeafNode(leafCapacity);
          newRoot.setID(getRootEntry().getID());
          for (int i = 0; i < child.getNumEntries(); i++) {
            newRoot.addLeafEntry(child.getEntry(i));
          }
        }
        else {
          newRoot = createNewDirectoryNode(dirCapacity);
          newRoot.setID(getRootEntry().getID());
          for (int i = 0; i < child.getNumEntries(); i++) {
            newRoot.addDirectoryEntry(child.getEntry(i));
          }
        }
        file.writePage(newRoot);
        height--;
      }
    }
  }

  /**
   * Determines the entries pointing to the leaf nodes of the specified subtree
   *
   * @param node   the subtree
   * @param result the result to store the ids in
   */
  private void getLeafNodes(N node, List<E>result, int currentLevel) {
    if (currentLevel == 2) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        result.add((E) node.getEntry(i));
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        N child = file.readPage(node.getEntry(i).getID());
        getLeafNodes(child, result, (currentLevel - 1));
      }
    }
  }
}
