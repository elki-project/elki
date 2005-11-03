package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.persistent.LRUCache;
import de.lmu.ifi.dbs.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.persistent.PersistentPageFile;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeapNode;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.Identifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search for certain object in the
 * structure. Persistence is not yet ensured.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class MTree<O extends MetricalObject, D extends Distance> implements MetricalIndex<O, D> {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  private static Level loggerLevel = Level.ALL;

  /**
   * The id of the root node.
   */
  private static Identifier ROOT_NODE_ID = new Identifier() {
    /**
     * Returns the ROOT ID.
     *
     * @return the ROOT ID
     */
    public Integer value() {
      return 0;
    }

    /**
     * Returns false.
     *
     * @return false
     */
    public boolean isNodeID() {
      return true;
    }
  };

  /**
   * The file storing the entries of this M-Tree.
   */
  private final PageFile<MTreeNode<O, D>> file;

  /**
   * The capacity of a directory node (= 1 + maximum number of entries in a directory node).
   */
  protected int dirCapacity;

  /**
   * The capacity of a leaf node (= 1 + maximum number of entries in a leaf node).
   */
  protected int leafCapacity;

  /**
   * The distance function.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * Creates a new MTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the MTree
   * @param cacheSize the size of the cache in bytes
   */
  public MTree(String fileName, int cacheSize) {
    initLogger();

    // init the file
    MTreeHeader header = new MTreeHeader();
    this.file = new PersistentPageFile<MTreeNode<O, D>>(header,
                                                        cacheSize,
                                                        new LRUCache<MTreeNode<O, D>>(),
                                                        fileName);
    this.dirCapacity = header.getDirCapacity();
    this.leafCapacity = header.getLeafCapacity();

    StringBuffer msg = new StringBuffer();
    msg.append(getClass());
    msg.append("\n file = ").append(file.getClass());
    logger.info(msg.toString());
  }

  /**
   * Creates a new MTree with the specified parameters.
   * The MTree will be hold in main memory.
   *
   * @param fileName         the name of the file for storing the entries,
   *                         if this parameter is null all entries will be hold in
   *                         main memory
   * @param pageSize         the size of a page in Bytes
   * @param cacheSize        the size of the cache in Bytes
   * @param distanceFunction the distance function
   */
  public MTree(String fileName, int pageSize, int cacheSize, DistanceFunction<O, D> distanceFunction) {
    initLogger();
    this.distanceFunction = distanceFunction;

    // determine minimum and maximum entries in an node
    initCapacity(pageSize);

    // init the file
    if (fileName == null) {
      this.file = new MemoryPageFile<MTreeNode<O, D>>(pageSize,
                                                      cacheSize,
                                                      new LRUCache<MTreeNode<O, D>>());
    }
    else {
      MTreeHeader header = new MTreeHeader(pageSize, dirCapacity, leafCapacity);
      this.file = new PersistentPageFile<MTreeNode<O, D>>(header,
                                                          cacheSize,
                                                          new LRUCache<MTreeNode<O, D>>(),
                                                          fileName);
    }


    String msg = getClass() + "\n" +
                 " file    = " + file.getClass() + "\n" +
                 " maximum number of dir entries = " + (dirCapacity - 1) + "\n" +
                 " maximum number of leaf entries = " + (leafCapacity - 1) + "\n";

    // create empty root
    MTreeNode<O, D> root = new MTreeNode<O, D>(file, leafCapacity, true);
    file.writePage(root);
    msg += " root    = " + getRoot() + "\n";

    logger.info(msg);
  }

  /**
   * Inserts the specified object into this M-Tree.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    logger.info("insert " + object.getID() + " " + object + "\n");
//    System.out.println("insert " + object.getID() + " " + object + "\n");

    ParentInfo placeToInsert = findInsertionNode(getRoot(), object.getID(), null);

    D parentDistance = null;

    if (placeToInsert.routingObjectID != null) {
      parentDistance = distanceFunction.distance(object.getID(), placeToInsert.routingObjectID);
    }

    LeafEntry<D> newEntry = new LeafEntry<D>(object.getID(), parentDistance);
    MTreeNode<O, D> node = placeToInsert.node;
    node.addLeafEntry(newEntry);

    while (hasOverflow(node)) {
      node = split(node);
    }

    // test
//    System.out.println(this);
    test(ROOT_NODE_ID);
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  public boolean delete(O o) {
    throw new UnsupportedOperationException("Delete is not supported in M-Tree!");
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
  public List<QueryResult<D>> rangeQuery(O object, String epsilon,
                                         DistanceFunction<O, D> distanceFunction) {

    D range = distanceFunction.valueOf(epsilon);
    final List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();

    doRangeQuery(null, getRoot(), object.getID(), range, distanceFunction, result);

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param object           the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  public List<QueryResult<D>> kNNQuery(O object, int k,
                                       DistanceFunction<O, D> distanceFunction) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one object has to be requested!");
    }

    // variables
    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();
    final KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
    Integer q = object.getID();

    // push root
    pq.addNode(new PQNode(distanceFunction.nullDistance(), ROOT_NODE_ID.value(), null));
    D d_k = knnList.getMaximumDistance();

    // search in tree
    while (!pq.isEmpty()) {
      PQNode pqNode = (PQNode) pq.getMinNode();

      if (pqNode.getKey().compareTo(d_k) > 0) {
        return knnList.toList();
      }

      MTreeNode<O, D> node = getNode(pqNode.getValue().getID());
      Integer o_p = pqNode.routingObjectID;

      // directory node
      if (! node.isLeaf) {
        for (int i = 0; i < node.numEntries; i++) {
          DirectoryEntry<D> entry = (DirectoryEntry<D>) node.entries[i];
          Integer o_r = entry.getObjectID();
          D r_or = entry.getCoveringRadius();
          D d1 = o_p != null? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
          D d2 = o_p != null? distanceFunction.distance(o_r, o_p): distanceFunction.nullDistance();

          D diff = d1.compareTo(d2) > 0 ?
                   d1.minus(d2) : d2.minus(d1);

          D sum = d_k.plus(r_or);

          if (diff.compareTo(sum) <= 0) {
            D d3 = distanceFunction.distance(o_r, q);
            D d_min = Util.max(d3.minus(r_or), distanceFunction.nullDistance());
            if (d_min.compareTo(d_k) <= 0) {
              pq.addNode(new PQNode(d_min, entry.getNodeID(), o_r));
            }
          }
        }

      }

      // data node
      else {
        for (int i = 0; i < node.numEntries; i++) {
        Entry<D> entry = node.entries[i];
        Integer o_j = entry.getObjectID();

        D d1 = distanceFunction.distance(o_p, q);
        D d2 = distanceFunction.distance(o_j, o_p);

        D diff = d1.compareTo(d2) > 0 ?
                 d1.minus(d2) : d2.minus(d1);

        if (diff.compareTo(d_k) <= 0) {
          D d3 = distanceFunction.distance(o_j, q);
          if (d3.compareTo(d_k) <= 0) {
            QueryResult<D> queryResult = new QueryResult<D>(o_j, d3);
            knnList.add(queryResult);
            if (knnList.size() == k)
              d_k = knnList.getMaximumDistance();
          }
        }
      }
      }
    }

    return knnList.toList();
  }

  /**
   * Returns the I/O-access of this RTree.
   *
   * @return the I/O-access of this RTree
   */
  public long getIOAccess() {
    return file.getIOAccess();
  }

  /**
   * Resets the I/O-access of this RTree.
   */
  public void resetIOAccess() {
    file.resetIOAccess();
  }

  /**
   * Returns the node with the specified id.
   *
   * @param nodeID the page id of the node to be returned
   * @return the node with the specified id
   */
  public MTreeNode<O, D> getNode(int nodeID) {
    if (nodeID == ROOT_NODE_ID.value()) return getRoot();
    else {
      return file.readPage(nodeID);
    }
  }

  /**
   * Closes this MTree and the underlying file.
   * If this MTree has a persistent file, all entries are written to disk.
   */
  public void close() {
    file.close();
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

    MTreeNode<O, D> node = getRoot();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        DirectoryEntry<D> entry = (DirectoryEntry<D>) node.entries[0];
        node = getNode(entry.getNodeID());
        levels++;
      }
    }

    BreadthFirstEnumeration<MTreeNode<O, D>> enumeration =
    new BreadthFirstEnumeration<MTreeNode<O, D>>(file, ROOT_NODE_ID);

    while (enumeration.hasMoreElements()) {
      Identifier id = enumeration.nextElement();
      if (! id.isNodeID()) {
        objects++;
//        LeafEntry e = (LeafEntry) id;
//        System.out.println("  obj = " + e.getObjectID());
//        System.out.println("  pd  = " + e.getParentDistance());
      }
      else {
        node = file.readPage(id.value());
//        System.out.println(node + ", numEntries = " + node.numEntries);

        if (id instanceof DirectoryEntry) {
//          DirectoryEntry e = (DirectoryEntry) id;
//          System.out.println("  r_obj = " + e.getObjectID());
//          System.out.println("  pd = " + e.getParentDistance());
//          System.out.println("  cr = " + ((DirectoryEntry<D>) id).getCoveringRadius());
        }

        if (node.isLeaf())
          leafNodes++;
        else {
          dirNodes++;
        }
      }
    }

    result.append(getClass().getName()).append(" hat ").append((levels + 1)).append(" Ebenen \n");
    result.append("DirCapacity = ").append(dirCapacity).append("\n");
    result.append("LeafCapacity = ").append(leafCapacity).append("\n");
    result.append(dirNodes).append(" Directory Knoten \n");
    result.append(leafNodes).append(" Daten Knoten \n");
    result.append(objects).append(" Punkte im Baum \n");
    result.append("IO-Access: ").append(file.getIOAccess()).append("\n");
    result.append("File ").append(file.getClass()).append("\n");

    return result.toString();
  }

  /**
   * Performs a range query. It starts from the root node and recursively
   * traverses all paths, which cannot be excluded from leading to
   * qualififying objects.
   *
   * @param o_p              the routing object of the specified node
   * @param node             the root of the subtree to be traversed
   * @param q                the id of the query object
   * @param r_q              the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @param result           the list holding the query results
   */
  private void doRangeQuery(Integer o_p, MTreeNode<O, D> node,
                            Integer q, D r_q,
                            DistanceFunction<O, D> distanceFunction,
                            List<QueryResult<D>> result) {

    if (! node.isLeaf) {
      for (int i = 0; i < node.numEntries; i++) {
        DirectoryEntry<D> entry = (DirectoryEntry<D>) node.entries[i];
        Integer o_r = entry.getObjectID();

        D r_or = entry.getCoveringRadius();
        D d1 = o_p != null ? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
        D d2 = o_p != null ? distanceFunction.distance(o_r, o_p) : distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ?
                 d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if (diff.compareTo(sum) <= 0) {
          D d3 = distanceFunction.distance(o_r, q);
          if (d3.compareTo(sum) <= 0) {
            MTreeNode<O, D> child = getNode(entry.getNodeID());
            doRangeQuery(o_r, child, q, r_q, distanceFunction, result);
          }
        }

      }
    }

    else {
      for (int i = 0; i < node.numEntries; i++) {
        Entry<D> entry = node.entries[i];
        Integer o_j = entry.getObjectID();

        D d1 = distanceFunction.distance(o_p, q);
        D d2 = distanceFunction.distance(o_j, o_p);

        D diff = d1.compareTo(d2) > 0 ?
                 d1.minus(d2) : d2.minus(d1);

        if (diff.compareTo(r_q) <= 0) {
          D d3 = distanceFunction.distance(o_j, q);
          if (d3.compareTo(r_q) <= 0) {
            QueryResult<D> queryResult = new QueryResult<D>(o_j, d3);
            result.add(queryResult);
          }
        }
      }
    }
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param pageSize the size of a page in Bytes
   */
  private void initCapacity(int pageSize) {
// overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

// dirCapacity = (pageSize - overhead) / (nodeID + objectID + radius + distance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + 8 + 8) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

// leafCapacity = (pageSize - overhead) / (objectID + distance) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + 8) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a leaf node = " + (leafCapacity - 1));
  }

  /**
   * Returns the root of this index.
   *
   * @return the root of this index
   */
  private MTreeNode<O, D> getRoot() {
    return file.readPage(ROOT_NODE_ID.value());
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  private boolean hasOverflow(MTreeNode node) {
    if (node.isLeaf)
      return node.numEntries == leafCapacity;
    return node.numEntries == dirCapacity;
  }

  /**
   * Return the leaf node for inserting the specified object into the subtree with the given root.
   *
   * @param node            the root of the subtree
   * @param objectID        the id of the obbject to be inserted
   * @param routingObjectID the id of the routing object
   * @return the leaf node for inserting the specified object into the given subtree
   */
  private ParentInfo findInsertionNode(MTreeNode<O, D> node, Integer objectID, Integer routingObjectID) {
// leaf
    if (node.isLeaf()) {
      return new ParentInfo(node, routingObjectID);
    }

    D nullDistance = distanceFunction.nullDistance();
    List<Enlargement> candidatesWithoutExtension = new ArrayList<Enlargement>();
    List<Enlargement> candidatesWithExtension = new ArrayList<Enlargement>();

    for (int i = 0; i < node.numEntries; i++) {
      DirectoryEntry<D> entry = (DirectoryEntry<D>) node.entries[i];
      D distance = distanceFunction.distance(objectID, entry.getObjectID());
//noinspection unchecked
      D enlrg = distance.minus(entry.getCoveringRadius());

      if (enlrg.compareTo(nullDistance) <= 0) {
        candidatesWithoutExtension.add(new Enlargement(entry, distance));
      }
      else {
        candidatesWithExtension.add(new Enlargement(entry, enlrg));
      }
    }

    Enlargement bestCandidate;
    if (!candidatesWithoutExtension.isEmpty()) {
      bestCandidate = Collections.min(candidatesWithoutExtension);
    }
    else {
      Collections.sort(candidatesWithExtension);
      bestCandidate = Collections.min(candidatesWithExtension);
      D cr = bestCandidate.entry.getCoveringRadius();

//noinspection unchecked
      bestCandidate.entry.setCoveringRadius((D) cr.plus(bestCandidate.distance));
    }

    MTreeNode<O, D> child = getNode(bestCandidate.entry.getNodeID());
    return findInsertionNode(child, objectID, bestCandidate.entry.getObjectID());
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(loggerLevel);
  }

  /**
   * Creates and returns a new root node that points to the two specified child nodes.
   *
   * @param oldRoot              the old root of this MTree
   * @param newNode              the new split node
   * @param firstPromoted        the first promotion object id
   * @param secondPromoted       the second promotion object id
   * @param firstCoveringRadius  the first covering radius
   * @param secondCoveringRadius the second covering radius
   * @return a new root node that points to the two specified child nodes
   */
  private MTreeNode<O, D> createNewRoot(final MTreeNode<O, D> oldRoot, final MTreeNode<O, D> newNode,
                                        Integer firstPromoted, Integer secondPromoted,
                                        D firstCoveringRadius, D secondCoveringRadius) {
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");

    MTreeNode<O, D> root = new MTreeNode<O, D>(file, dirCapacity, false);
    file.writePage(root);

    oldRoot.nodeID = root.getID();
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.numEntries; i++) {
        MTreeNode<O, D> node = getNode(((DirectoryEntry) oldRoot.entries[i]).getNodeID());
        node.parentID = oldRoot.nodeID;
        file.writePage(node);
      }
    }

    root.nodeID = ROOT_NODE_ID.value();
    root.addNode(oldRoot, firstPromoted, null, firstCoveringRadius);
    root.addNode(newNode, secondPromoted, null, secondCoveringRadius);

// adjust the parentDistances
    for (int i = 0; i < oldRoot.numEntries; i++) {
      D distance = distanceFunction.distance(firstPromoted, oldRoot.entries[i].getObjectID());
      oldRoot.entries[i].setParentDistance(distance);
    }
    for (int i = 0; i < newNode.numEntries; i++) {
      D distance = distanceFunction.distance(secondPromoted, newNode.entries[i].getObjectID());
      newNode.entries[i].setParentDistance(distance);
    }

    msg.append("firstCoveringRadius ").append(firstCoveringRadius).append("\n");
    msg.append("secondCoveringRadius ").append(secondCoveringRadius).append("\n");

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    msg.append("New Root-ID ").append(root.nodeID).append("\n");
    logger.info(msg.toString());

    return root;
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private MTreeNode<O, D> split(MTreeNode<O, D> node) {
// do the split
//    Split<D> split = new MRadSplit<O, D>(node, distanceFunction);
    Integer routingObjectID = null;
    if (node.nodeID != ROOT_NODE_ID.value()) {
      MTreeNode<O, D> parent = getNode(node.parentID);
      routingObjectID = parent.entries[node.index].getObjectID();
    }
    Split<D> split = new MLBDistSplit<O, D>(node, routingObjectID, distanceFunction);

    MTreeNode<O, D> newNode = node.splitEntries(split.assignmentsToFirst, split.assignmentsToSecond);

    String msg = "Split Node " + node.getID() + " (" + this.getClass() + ")\n" +
                 "      newNode " + newNode.getID() + "\n" +
                 "      firstPromoted " + split.firstPromoted + "\n" +
                 "      firstAssignments(" + node.getID() + ") " + split.assignmentsToFirst + "\n" +
                 "      firstCR " + split.firstCoveringRadius + "\n" +
                 "      secondPromoted " + split.secondPromoted + "\n" +
                 "      secondAssignments(" + newNode.getID() + ") " + split.assignmentsToSecond + "\n" +
                 "      secondCR " + split.secondCoveringRadius + "\n";

    logger.info(msg);

// write changes to file
    file.writePage(node);
    file.writePage(newNode);

// if root was split: create a new root that points the two split nodes
    if (node.getID() == ROOT_NODE_ID.value()) {
      return createNewRoot(node, newNode, split.firstPromoted, split.secondPromoted,
                           split.firstCoveringRadius, split.secondCoveringRadius);
    }

// determine the new parent distances
    MTreeNode<O, D> parent = getNode(node.parentID);
    MTreeNode<O, D> grandParent;
    D parentDistance1 = null, parentDistance2 = null;

    if (parent.getID() != ROOT_NODE_ID.value()) {
      grandParent = getNode(parent.parentID);
      Integer parentObject = grandParent.entries[parent.index].getObjectID();
      parentDistance1 = distanceFunction.distance(split.firstPromoted, parentObject);
      parentDistance2 = distanceFunction.distance(split.secondPromoted, parentObject);
    }

// add the newNode to parent
    parent.addNode(newNode, split.secondPromoted, parentDistance2, split.secondCoveringRadius);

// set the first promotion object, parentDistance and covering radius for node in parent
    DirectoryEntry<D> entry1 = (DirectoryEntry<D>) parent.entries[node.index];
    entry1.setObjectID(split.firstPromoted);
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(split.firstCoveringRadius);

// adjust the parentDistances in node
    for (int i = 0; i < node.numEntries; i++) {
      D distance = distanceFunction.distance(split.firstPromoted, node.entries[i].getObjectID());
      node.entries[i].setParentDistance(distance);
    }
// adjust the parentDistances in newNode
    for (int i = 0; i < newNode.numEntries; i++) {
      D distance = distanceFunction.distance(split.secondPromoted, newNode.entries[i].getObjectID());
      newNode.entries[i].setParentDistance(distance);
    }

// adjust the covering radii
//    if (parentDistance1 == null) parentDistance1 = distanceFunction.nullDistance();
//    if (parentDistance2 == null) parentDistance2 = distanceFunction.nullDistance();
//    D cr = (D) Util.max(split.firstCoveringRadius.plus(parentDistance1),
//                        split.secondCoveringRadius.plus(parentDistance2));
//    D cr = Util.max(parentDistance1, parentDistance2);
//    adjustCoveringRadius(parent, cr);

// write changes in parent to file
    file.writePage(parent);

    return parent;
  }

//  private void adjustCoveringRadius(MTreeNode<O, D> node, D coveringRadius) {
//
//    if (node.getID() == ROOT_NODE_ID.value()) return;
//
//
//    MTreeNode<O, D> parent = getNode(node.parentID);
//    DirectoryEntry<D> entry = (DirectoryEntry<D>) parent.entries[node.index];
//
//    if (entry.getCoveringRadius().compareTo(coveringRadius) < 0) {
//      entry.setCoveringRadius(coveringRadius);
//      adjustCoveringRadius(parent, coveringRadius.plus(entry.getParentDistance()));
//    }
//  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void test(Identifier rootID) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs = new BreadthFirstEnumeration<MTreeNode<O, D>>(file, rootID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MTreeNode<O, D> node = getNode(id.value());
        node.test();

        if (id instanceof Entry) {
          DirectoryEntry<D> e = (DirectoryEntry<D>) id;
          node.testParentDistance(e.getObjectID(), distanceFunction);
          testCR(e);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testCR(DirectoryEntry<D> rootID) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs =
    new BreadthFirstEnumeration<MTreeNode<O, D>>(file, rootID);

    Integer routingObjectID = rootID.getObjectID();
    D coveringRadius = rootID.getCoveringRadius();

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MTreeNode<O, D> node = getNode(id.value());
        node.testCoveringRadius(routingObjectID, coveringRadius, distanceFunction);
      }
    }
  }

  /**
   * Encapsulates the parameters for enlargement of nodes.
   */
  private class Enlargement implements Comparable<Enlargement> {
    /**
     * The entry.
     */
    DirectoryEntry<D> entry;

    /**
     * The distance.
     */
    D distance;

    /**
     * Creates an new Enlaregemnt object with the specified parameters.
     *
     * @param entry    the entry
     * @param distance the distance
     */
    Enlargement(DirectoryEntry<D> entry, D distance) {
      this.entry = entry;
      this.distance = distance;
    }

    /**
     * Compares this Enlargement with the specified Enlargement.
     *
     * @param other the Enlargement to be compared.
     * @return a negative integer, zero, or a positive integer as this Enlargement
     *         is less than, equal to, or greater than the specified Enlargement.
     */
    public int compareTo(Enlargement other) {
      int comp = this.distance.compareTo(other.distance);
      if (comp != 0) return comp;

      return this.entry.getNodeID() - other.entry.getNodeID();
    }
  }

  /**
   * Encapsulates the attributes for a parent leaf node of a data object.
   */
  private class ParentInfo {
    /**
     * The node.
     */
    MTreeNode<O, D> node;

    /**
     * The routing object of the node.
     */
    Integer routingObjectID;

    /**
     * Creates a new ParentInfo object with the specified parameters.
     *
     * @param node            the node
     * @param routingObjectID the routing object of the node
     */
    public ParentInfo(MTreeNode<O, D> node, Integer routingObjectID) {
      this.node = node;
      this.routingObjectID = routingObjectID;
    }
  }

  /**
   * Encapsulates the attributes for a node that can be stored in a heap.
   */
  private class PQNode extends DefaultHeapNode<Distance, Identifiable> {
    Integer routingObjectID;

    /**
     * Empty constructor for serialization purposes.
     */
    public PQNode() {
      super();
    }

    /**
     * Creates a new heap node with the specified parameters.
     *
     * @param d_min           the minimum distance of the node
     * @param nodeID          the id of the node
     * @param routingObjectID the id of the routing object of the node
     */
    public PQNode(D d_min, final Integer nodeID, final Integer routingObjectID) {
      super(d_min, new Identifiable() {
        public Integer getID() {
          return nodeID;
        }

        public int compareTo(Identifiable o) {
          return nodeID - o.getID();
        }
      });

      this.routingObjectID = routingObjectID;
    }
  }
}
