package de.lmu.ifi.dbs.index.metrical.mtreevariants;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.index.*;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.util.Assignments;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.util.PQNode;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.Identifiable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure. Persistence is not yet ensured.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTree<O extends DatabaseObject, D extends Distance<D>, N extends MTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends MetricalIndex<O, D,N,E> {
  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "<class>the distance function to determine the distance between database objects " +
                                                   Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) +
                                                   ". Default: " + DEFAULT_DISTANCE_FUNCTION;

  /**
   * Holds the class specific debug status.
   */
  private static boolean DEBUG = LoggingConfiguration.DEBUG;
//  protected static boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The distance function.
   */
  protected DistanceFunction<O, D> distanceFunction;

  /**
   * Empty constructor.
   */
  public MTree() {
    super();
    parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Inserts the specified object into this M-Tree.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    if (DEBUG) {
      logger.fine("insert " + object.getID() + " " + object + "\n");
    }

    if (!initialized) {
      initialize(object);
    }

    // find insertion path
    IndexPath<E> path = choosePath(object.getID(), getRootPath());
    N node = getNode(path.getLastPathComponent().getEntry());

    // determine parent distance
    D parentDistance = null;
    if (path.getParentPath() != null) {
      N parent = getNode(path.getParentPath().getLastPathComponent().getEntry());
      Integer index = path.getLastPathComponent().getIndex();
      parentDistance = distanceFunction.distance(object.getID(), parent.getEntry(index).getRoutingObjectID());
    }

    // add object
    //noinspection unchecked
    node.addLeafEntry((E) new MTreeLeafEntry<D>(object.getID(), parentDistance));

    // do split if necessary
    while (hasOverflow(path)) {
      path = split(path);
    }

    //test
//    test(new TreePath(new TreePathComponent(ROOT_NODE_ID, null)));
  }

  /**
   * Inserts the specified objects into this index sequentially.
   *
   * @param objects the objects to be inserted
   */
  public void insert(List<O> objects) {
    for (O object : objects) {
      insert(object);
    }
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  public boolean delete(O o) {
    throw new UnsupportedOperationException("Deletion of objects is not supported by a M-Tree!");
  }

  /**
   * Performs a range query for the given spatial objec with the given epsilon
   * range and the according distance function. The query result is in
   * ascending order to the distance to the query object.
   *
   * @param object  the query object
   * @param epsilon the string representation of the query range
   * @return a List of the query results
   */
  public List<QueryResult<D>> rangeQuery(O object, String epsilon) {

    D range = distanceFunction.valueOf(epsilon);
    final List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();

    doRangeQuery(null, getRoot(), object.getID(), range, result);

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  /**
   * Performs a k-nearest neighbor query for the given NumberVector with the
   * given parameter k and the according distance function. The query result
   * is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public List<QueryResult<D>> kNNQuery(O object, int k) {
    if (k < 1) {
      throw new IllegalArgumentException("At least one object has to be requested!");
    }

    final KNNList<D> knnList = new KNNList<D>(k, distanceFunction.infiniteDistance());
    doKNNQuery(object.getID(), knnList);
    return knnList.toList();
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public List<QueryResult<D>> reverseKNNQuery(O object, int k) {
    throw new UnsupportedOperationException("Not yet supported!");
  }

  /**
   * Returns the distance function.
   *
   * @return the distance function
   */
  public DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
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

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        E entry = node.getEntry(0);
        node = getNode(entry);
        levels++;
      }
    }

    BreadthFirstEnumeration<N, E> enumeration = new BreadthFirstEnumeration<N, E>(file, getRootPath());
    while (enumeration.hasMoreElements()) {
      IndexPath path = enumeration.nextElement();
      Entry entry = path.getLastPathComponent().getEntry();
      if (entry.isLeafEntry()) {
        objects++;
//        MTreeLeafEntry e = (MTreeLeafEntry) id;
//        System.out.println("  obj = " + e.getObjectID());
//        System.out.println("  pd  = " + e.getParentDistance());
      }
      else {
        node = file.readPage(entry.getID());
//        System.out.println(node + ", numEntries = " + node.numEntries);

        if (entry instanceof MTreeDirectoryEntry) {
//          MTreeDirectoryEntry e = (MTreeDirectoryEntry) id;
//          System.out.println("  r_obj = " + e.getObjectID());
//          System.out.println("  pd = " + e.getParentDistance());
//          System.out.println("  cr = " + ((MTreeDirectoryEntry<D>) id).getCoveringRadius());
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
    // todo
    result.append("IO-Access: ").append(file.getPhysicalReadAccess()).append("\n");
    result.append("File ").append(file.getClass()).append("\n");

    return result.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    if (optionHandler.isSet(DISTANCE_FUNCTION_P)) {
      String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
      try {
        // noinspection unchecked
        distanceFunction = Util.instantiate(DistanceFunction.class, className);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(DISTANCE_FUNCTION_P,
                                               className, DISTANCE_FUNCTION_D, e);
      }
    }
    else {
      try {
        // noinspection unchecked
        distanceFunction = Util.instantiate(DistanceFunction.class,
                                            DEFAULT_DISTANCE_FUNCTION);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(DISTANCE_FUNCTION_P,
                                               DEFAULT_DISTANCE_FUNCTION, DISTANCE_FUNCTION_D, e);
      }
    }

    remainingParameters = distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(DISTANCE_FUNCTION_P, distanceFunction.getClass().getName());

    attributeSettings.addAll(distanceFunction.getAttributeSettings());

    return attributeSettings;
  }

  /**
   * @see Index#createEmptyRoot(DatabaseObject)
   */
  protected void createEmptyRoot(O object) {
    N root = createNewLeafNode(leafCapacity);
    file.writePage(root);
  }

  /**
   * Performs a range query. It starts from the root node and recursively
   * traverses all paths, which cannot be excluded from leading to
   * qualififying objects.
   *
   * @param o_p    the routing object of the specified node
   * @param node   the root of the subtree to be traversed
   * @param q      the id of the query object
   * @param r_q    the query range
   * @param result the list holding the query results
   */
  private void doRangeQuery(Integer o_p, N node, Integer q, D r_q, List<QueryResult<D>> result) {

    if (!node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) node.getEntry(i);
        Integer o_r = entry.getRoutingObjectID();

        D r_or = entry.getCoveringRadius();
        D d1 = o_p != null ? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
        D d2 = o_p != null ? distanceFunction.distance(o_r, o_p) : distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if (diff.compareTo(sum) <= 0) {
          D d3 = distanceFunction.distance(o_r, q);
          if (d3.compareTo(sum) <= 0) {
            N child = getNode((E) entry);
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }

      }
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        Integer o_j = entry.getRoutingObjectID();

        D d1 = o_p != null ? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
        D d2 = o_p != null ? distanceFunction.distance(o_j, o_p) : distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

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
   * Performs a k-nearest neighbor query for the given NumberVector with the
   * given parameter k and the according distance function. The query result
   * is in ascending order to the distance to the query object.
   *
   * @param q       the id of the query object
   * @param knnList the query result list
   */
  protected void doKNNQuery(Integer q, KNNList<D> knnList) {
    final Heap<D, Identifiable> pq = new DefaultHeap<D, Identifiable>();

    // push root
    pq.addNode(new PQNode<D>(distanceFunction.nullDistance(), getRootEntry().getID(), null));
    D d_k = knnList.getKNNDistance();

    // search in tree
    while (!pq.isEmpty()) {
      PQNode<D> pqNode = (PQNode<D>) pq.getMinNode();

      if (pqNode.getKey().compareTo(d_k) > 0) {
        return;
      }

      N node = getNode(pqNode.getValue().getID());
      Integer o_p = pqNode.getRoutingObjectID();

      // directory node
      if (!node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) node.getEntry(i);
          Integer o_r = entry.getRoutingObjectID();
          D r_or = entry.getCoveringRadius();
          D d1 = o_p != null ? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
          D d2 = o_p != null ? distanceFunction.distance(o_r, o_p) : distanceFunction.nullDistance();

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          D sum = d_k.plus(r_or);

          if (diff.compareTo(sum) <= 0) {
            D d3 = distanceFunction.distance(o_r, q);
            D d_min = Util.max(d3.minus(r_or), distanceFunction.nullDistance());
            if (d_min.compareTo(d_k) <= 0) {
              pq.addNode(new PQNode<D>(d_min, entry.getID(), o_r));
            }
          }
        }

      }

      // data node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MTreeEntry<D> entry = node.getEntry(i);
          Integer o_j = entry.getRoutingObjectID();

          D d1 = o_p != null ? distanceFunction.distance(o_p, q) : distanceFunction.nullDistance();
          D d2 = o_p != null ? distanceFunction.distance(o_j, o_p) : distanceFunction.nullDistance();

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          if (diff.compareTo(d_k) <= 0) {
            D d3 = distanceFunction.distance(o_j, q);
            if (d3.compareTo(d_k) <= 0) {
              QueryResult<D> queryResult = new QueryResult<D>(o_j, d3);
              knnList.add(queryResult);
              d_k = knnList.getKNNDistance();
            }
          }
        }
      }
    }
  }

  /**
   * Returns true if in the last component of the specified path an overflow has occured, false
   * otherwise.
   *
   * @param path the path to be tested for overflow
   * @return true if in the last component of the specified path an overflow has occured,
   *         false otherwise
   */
  protected boolean hasOverflow(IndexPath<E> path) {
    N node = getNode(path.getLastPathComponent().getEntry());
    if (node.isLeaf())
      return node.getNumEntries() == leafCapacity;

    return node.getNumEntries() == dirCapacity;
  }

  /**
   * Chooses the best path of the specified subtree for insertion of
   * the given object.
   *
   * @param subtree the subtree to be tested for insertion
   * @param objectID the id of the obbject to be inserted
   * @return the path of the appropriate subtree to insert the given object
   */
  protected IndexPath<E> choosePath(Integer objectID, IndexPath<E> subtree) {
    N node = getNode(subtree.getLastPathComponent().getEntry());

    // leaf
    if (node.isLeaf()) {
      return subtree;
    }

    D nullDistance = distanceFunction.nullDistance();
    List<DistanceEntry<D, E>> candidatesWithoutExtension = new ArrayList<DistanceEntry<D, E>>();
    List<DistanceEntry<D, E>> candidatesWithExtension = new ArrayList<DistanceEntry<D, E>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) node.getEntry(i);
      D distance = distanceFunction.distance(objectID, entry.getRoutingObjectID());
      D enlrg = distance.minus(entry.getCoveringRadius());

      if (enlrg.compareTo(nullDistance) <= 0) {
        candidatesWithoutExtension.add(new DistanceEntry<D, E>((E) entry, distance, i));
      }
      else {
        candidatesWithExtension.add(new DistanceEntry<D, E>((E) entry, enlrg, i));
      }
    }

    DistanceEntry<D, E> bestCandidate;
    if (!candidatesWithoutExtension.isEmpty()) {
      bestCandidate = Collections.min(candidatesWithoutExtension);
    }
    else {
      Collections.sort(candidatesWithExtension);
      bestCandidate = Collections.min(candidatesWithExtension);
      MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) bestCandidate.getEntry();
      D cr = entry.getCoveringRadius();
      entry.setCoveringRadius((D) cr.plus(bestCandidate.getDistance()));
    }

    return choosePath(objectID, subtree.pathByAddingChild(new IndexPathComponent<E>(bestCandidate.getEntry(),
                                                                                 bestCandidate.getIndex())));
  }

  /**
   * Performs a batch knn query.
   *
   * @param node     the node for which the query should be performed
   * @param ids      the ids of th query objects
   * @param knnLists the knn lists of the query objcets
   */
  protected void batchNN(N node, List<Integer> ids, Map<Integer, KNNList<D>> knnLists) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MTreeLeafEntry<D> p = (MTreeLeafEntry<D>) node.getEntry(i);
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          D dist_pq = distanceFunction.distance(p.getRoutingObjectID(), q);
          if (dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q.add(new QueryResult<D>(p.getRoutingObjectID(), dist_pq));
          }
        }
      }
    }
    else {
      List<DistanceEntry<D, E>> entries = getSortedEntries(node, ids);
      for (DistanceEntry<D, E> distEntry : entries) {
        D minDist = distEntry.getDistance();
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          if (minDist.compareTo(knn_q_maxDist) <= 0) {
            E entry = (E) distEntry.getEntry();
            N child = getNode(entry);
            batchNN(child, ids, knnLists);
            break;
          }
        }
      }
    }
  }

  /**
   * Test the covering radius of specified node (for debugging purpose).
   */
  protected void testCoveringRadius(IndexPath<E> rootPath) {
    BreadthFirstEnumeration<N, E> bfs = new BreadthFirstEnumeration<N, E>(file, rootPath);

    MTreeDirectoryEntry<D> rootID = (MTreeDirectoryEntry<D>) rootPath.getLastPathComponent().getEntry();
    Integer routingObjectID = rootID.getRoutingObjectID();
    D coveringRadius = rootID.getCoveringRadius();

    while (bfs.hasMoreElements()) {
      IndexPath<E> path = bfs.nextElement();
      E entry = path.getLastPathComponent().getEntry();

      if (! entry.isLeafEntry()) {
        N node = getNode(entry);
        node.testCoveringRadius(routingObjectID, coveringRadius, distanceFunction);
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  protected void test(IndexPath<E> rootPath) {
    BreadthFirstEnumeration<N, E> bfs = new BreadthFirstEnumeration<N, E>(file, rootPath);

    while (bfs.hasMoreElements()) {
      IndexPath<E> path = bfs.nextElement();
      E entry = path.getLastPathComponent().getEntry();

      if (! entry.isLeafEntry()) {
        N node = getNode(entry);
        node.test();

        if (entry instanceof MTreeEntry) {
          MTreeDirectoryEntry<D> e = (MTreeDirectoryEntry<D>) entry;
          node.testParentDistance(e.getRoutingObjectID(), distanceFunction);
          testCoveringRadius(path);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }
      }
    }
  }

  /**
   * @see Index#initializeCapacities(DatabaseObject)
   */
  protected void initializeCapacities(O object) {
    D dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance) +
    // 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
  }

  /**
   * Creates and returns a new root node that points to the two specified
   * child nodes.
   *
   * @param oldRoot              the old root of this MTree
   * @param newNode              the new split node
   * @param firstPromoted        the first promotion object id
   * @param secondPromoted       the second promotion object id
   * @param firstCoveringRadius  the first covering radius
   * @param secondCoveringRadius the second covering radius
   * @return a new root node that points to the two specified child nodes
   */
  private IndexPath createNewRoot(final N oldRoot, final N newNode,
                                  Integer firstPromoted, Integer secondPromoted,
                                  D firstCoveringRadius, D secondCoveringRadius) {

    StringBuffer msg = new StringBuffer();

    // create new root
    if (DEBUG) {
      msg.append("create new root \n");
    }
    N root = (N) new MTreeNode<O, D, N, E>(file, dirCapacity, false);
    file.writePage(root);

    // change id in old root and set id in new root
    oldRoot.setID(root.getID());
    root.setID(getRootEntry().getID());

    // add entries to new root
    root.addDirectoryEntry((E) new MTreeDirectoryEntry<D>(firstPromoted, null, oldRoot.getID(), firstCoveringRadius));
    root.addDirectoryEntry((E) new MTreeDirectoryEntry<D>(secondPromoted, null, newNode.getID(), secondCoveringRadius));

    // adjust the parentDistances
    for (int i = 0; i < oldRoot.getNumEntries(); i++) {
      D distance = distanceFunction.distance(firstPromoted, oldRoot.getEntry(i).getRoutingObjectID());
      oldRoot.getEntry(i).setParentDistance(distance);
    }
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(secondPromoted, newNode.getEntry(i).getRoutingObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }
    if (DEBUG) {
      msg.append("firstCoveringRadius ").append(firstCoveringRadius).append("\n");
      msg.append("secondCoveringRadius ").append(secondCoveringRadius).append("\n");
    }

    // write the changes
    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);

    if (DEBUG) {
      msg.append("New Root-ID ").append(root.getID()).append("\n");
      logger.fine(msg.toString());
    }

    return new IndexPath<E>(new IndexPathComponent<E>(getRootEntry(), null));
  }

  /**
   * Splits the last node in the specified path and returns a path
   * containing at last element the parent of the newly created split node.
   *
   * @param path the path containing at last element the node to be splitted
   * @return a path containing at last element the parent of the newly created split node
   */
  private IndexPath<E> split(IndexPath<E> path) {
    N node = getNode(path.getLastPathComponent().getEntry());
    Integer nodeIndex = path.getLastPathComponent().getIndex();

    // do split
    MTreeSplit<O, D, N, E> split = new MLBDistSplit<O, D, N, E>(node, distanceFunction);
    Assignments<D,E> assignments = split.getAssignments();
    N newNode = node.splitEntries(assignments.getFirstAssignments(),
                                  assignments.getSecondAssignments());

    if (DEBUG) {
      String msg = "Split Node " + node.getID() + " (" + this.getClass() + ")\n" +
                   "      newNode " + newNode.getID() + "\n" +
                   "      firstPromoted " + assignments.getFirstRoutingObject() + "\n" +
                   "      firstAssignments(" + node.getID() + ") " + assignments.getFirstAssignments() + "\n" +
                   "      firstCR " + assignments.getFirstCoveringRadius() + "\n" +
                   "      secondPromoted " + assignments.getSecondRoutingObject() + "\n" +
                   "      secondAssignments(" + newNode.getID() + ") " + assignments.getSecondAssignments() + "\n" +
                   "      secondCR " + assignments.getSecondCoveringRadius() + "\n";
      logger.fine(msg);
    }

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    // if root was split: create a new root that points the two split nodes
    if (node.getID() == getRootEntry().getID()) {
      return createNewRoot(node, newNode,
                           assignments.getFirstRoutingObject(), assignments.getSecondRoutingObject(),
                           assignments.getFirstCoveringRadius(), assignments.getSecondCoveringRadius());
    }

    // determine the new parent distances
    N parent = getNode(path.getParentPath().getLastPathComponent().getEntry());
    Integer parentIndex = path.getParentPath().getLastPathComponent().getIndex();
    N grandParent;
    D parentDistance1 = null, parentDistance2 = null;

    if (parent.getID() != getRootEntry().getID()) {
      grandParent = getNode(path.getParentPath().getParentPath().getLastPathComponent().getEntry());
      Integer parentObject = grandParent.getEntry(parentIndex).getRoutingObjectID();
      parentDistance1 = distanceFunction.distance(assignments.getFirstRoutingObject(), parentObject);
      parentDistance2 = distanceFunction.distance(assignments.getSecondRoutingObject(), parentObject);
    }

    // add the newNode to parent
    parent.addDirectoryEntry((E) new MTreeDirectoryEntry<D>(assignments.getSecondRoutingObject(), parentDistance2, newNode.getID(), assignments.getSecondCoveringRadius()));

    // set the first promotion object, parentDistance and covering radius
    // for node in parent
    MTreeDirectoryEntry<D> entry1 = (MTreeDirectoryEntry<D>) parent.getEntry(nodeIndex);
    entry1.setRoutingObjectID(assignments.getFirstRoutingObject());
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(assignments.getFirstCoveringRadius());

    // adjust the parentDistances in node
    for (int i = 0; i < node.getNumEntries(); i++) {
      D distance = distanceFunction.distance(assignments.getFirstRoutingObject(), node.getEntry(i).getRoutingObjectID());
      node.getEntry(i).setParentDistance(distance);
    }

    // adjust the parentDistances in newNode
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(assignments.getSecondRoutingObject(), newNode.getEntry(i).getRoutingObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }

    // write changes in parent to file
    file.writePage(parent);

    return path.getParentPath();
  }

  /**
   * Sorts the entries of the specified node according to their minimum
   * distance to the specified objects.
   *
   * @param node the node
   * @param ids  the ids of the objects
   * @return a list of the sorted entries
   */
  private List<DistanceEntry<D, E>> getSortedEntries(N node, List<Integer> ids) {
    List<DistanceEntry<D, E>> result = new ArrayList<DistanceEntry<D, E>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) node.getEntry(i);

      D minMinDist = distanceFunction.infiniteDistance();
      for (Integer q : ids) {
        D distance = distanceFunction.distance(entry.getRoutingObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? distanceFunction.nullDistance() : distance.minus(entry.getCoveringRadius());
        if (minDist.compareTo(minMinDist) < 0) {
          minMinDist = minDist;
        }
      }
      result.add(new DistanceEntry<D, E>((E) entry, minMinDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Creates an entry representing the root node.
   */
  protected E createRootEntry() {
    //noinspection unchecked
    return (E) new MTreeDirectoryEntry<D>(null, null, 0, null);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected N createNewLeafNode(int capacity) {
    return (N) new MTreeNode<O,D,N,E>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected N createNewDirectoryNode(int capacity) {
    return (N) new MTreeNode<O,D,N,E>(file, capacity, false);
  }
}
