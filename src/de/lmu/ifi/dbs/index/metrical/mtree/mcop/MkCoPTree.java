package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.mknn.MkNNTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtree.util.PQNode;
import de.lmu.ifi.dbs.index.metrical.mtree.util.ParentInfo;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.heap.Identifiable;

import java.util.*;

/**
 * MkCopTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for parameter
 * k < kmax.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkCoPTree<O extends MetricalObject> extends MTree<O, DoubleDistance> {

  /**
   * The parameter kmax.
   */
  private int k_max;

  /**
   * The values of log(1),..,log(k_max)
   */
  private double[] log_k;

  /**
   * The sum of log(k) for k = 1..k_max
   */
  private double sum_log_k;

  /**
   * The sum of log(k)^2 for k = 1..k_max
   */
  private double sum_log_k2;


  /**
   * Creates a new MkCopTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the MCopTree
   * @param cacheSize the size of the cache in bytes
   */
  public MkCoPTree(String fileName, int cacheSize) {
    init(new MkNNTreeHeader(), fileName, cacheSize);
    init();
  }

  /**
   * Creates a new MkCopTree with the specified parameters.
   *
   * @param fileName         the name of the file for storing the entries,
   *                         if this parameter is null all entries will be hold in
   *                         main memory
   * @param pageSize         the size of a page in Bytes
   * @param cacheSize        the size of the cache in Bytes
   * @param distanceFunction the distance function
   * @param kmax             the maximal number of knn distances to be stored
   * @param objects
   */
  public MkCoPTree(String fileName, int pageSize, int cacheSize,
                   DistanceFunction<O, DoubleDistance> distanceFunction, int kmax,
                   List<O> objects) {
    super();
    this.k_max = kmax;
    init(fileName, pageSize, cacheSize, distanceFunction);
    init();

    insert(objects);
  }

  /**
   * Creates a new MkCopTree with the specified parameters.
   *
   * @param fileName         the name of the file for storing the entries,
   *                         if this parameter is null all entries will be hold in
   *                         main memory
   * @param pageSize         the size of a page in Bytes
   * @param cacheSize        the size of the cache in Bytes
   * @param distanceFunction the distance function
   * @param kmax             the maximal number of knn distances to be stored
   */
  public MkCoPTree(String fileName, int pageSize, int cacheSize,
                   DistanceFunction<O, DoubleDistance> distanceFunction, int kmax) {
    super();
    this.k_max = kmax;
    init(fileName, pageSize, cacheSize, distanceFunction);
    init();
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree.
   * This operation is not supported.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    throw new UnsupportedOperationException("Insertion of objects is not supported by a M-Tree!");
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public List<QueryResult<DoubleDistance>> reverseKNNQuery(O object, int k) {
    if (k > this.k_max) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " +
                                         "parameter kmax of the MCop-Tree!");
    }

    List<QueryResult<DoubleDistance>> result = new ArrayList<QueryResult<DoubleDistance>>();
    List<Integer> candidates = new ArrayList<Integer>();
    doReverseKNNQuery(k, object.getID(), result, candidates);

    // refinement of candidates
    Map<Integer, KNNList<DoubleDistance>> knnLists = new HashMap<Integer, KNNList<DoubleDistance>>();
    for (Integer id : candidates)
      knnLists.put(id, new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance()));
    batchNN(getRoot(), candidates, knnLists);

    Collections.sort(result);
    Collections.sort(candidates);
    System.out.println("result   (" + result.size() + ") "  + result);
    System.out.println("candidate(" + candidates.size() + ") " + candidates);

    for (Integer id : candidates) {
      List<QueryResult<DoubleDistance>> knns = knnLists.get(id).toList();
      for (QueryResult<DoubleDistance> qr : knns) {
        if (qr.getID() == object.getID()) {
          result.add(new QueryResult<DoubleDistance>(id, qr.getDistance()));
          break;
        }
      }

    }

    Collections.sort(result);


    return result;
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

    MTreeNode<O, DoubleDistance> node = getRoot();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        DirectoryEntry entry = (DirectoryEntry) node.getEntry(0);
        node = getNode(entry.getNodeID());
        levels++;
      }
    }

    BreadthFirstEnumeration<MTreeNode<O, DoubleDistance>> enumeration =
    new BreadthFirstEnumeration<MTreeNode<O, DoubleDistance>>(file, ROOT_NODE_ID);

    while (enumeration.hasMoreElements()) {
      Identifier id = enumeration.nextElement();
      if (! id.isNodeID()) {
        objects++;
        MkCoPLeafEntry e = (MkCoPLeafEntry) id;
        System.out.println("Object " + e.getObjectID());
        System.out.println("  pd  = " + e.getParentDistance());
        System.out.println("  consApprox  = " + Arrays.asList(e.getConservativeKnnDistanceApproximation()));
        System.out.println("  progrApprox = " + Arrays.asList(e.getProgressiveKnnDistanceApproximation()));
      }
      else {
        node = file.readPage(id.value());
        System.out.println(node + ", numEntries = " + node.getNumEntries());

        if (id instanceof DirectoryEntry) {
          MkCoPDirectoryEntry e = (MkCoPDirectoryEntry) id;
          System.out.println("  r_obj = " + e.getObjectID());
          System.out.println("  pd = " + e.getParentDistance());
          System.out.println("  cr = " + e.getCoveringRadius());
          System.out.println("  consApprox  = " + Arrays.asList(e.getConservativeKnnDistanceApproximation()));
          System.out.println("  progrApprox = " + Arrays.asList(e.getProgressiveKnnDistanceApproximation()));
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
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  protected MTreeNode<O, DoubleDistance> createEmptyRoot() {
    return new MkCoPTreeNode<O>(file, leafCapacity, true);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected void initCapacity(int pageSize) {
    DoubleDistance dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID + coveringRadius + parentDistance + kmax + consApprox + progrApprox) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 4 + 16 + 16) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + kmax + consApprox + progrApprox) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize + 4 + 16 + 16) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a leaf node = " + (leafCapacity - 1));

  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   * todo!
   *
   * @param q
   * @param node_entry
   * @param node
   * @param result
   */
  private void doReverseKNNQuery1(int k,
                                  Integer q,
                                  MkCoPDirectoryEntry node_entry,
                                  MkCoPTreeNode<O> node,
                                  List<QueryResult<DoubleDistance>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPLeafEntry entry = (MkCoPLeafEntry) node.getEntry(i);
        DoubleDistance distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.approximateConservativeKnnDistance(k)) <= 0)
          result.add(new QueryResult<DoubleDistance>(entry.getObjectID(), distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPDirectoryEntry entry = (MkCoPDirectoryEntry) node.getEntry(i);
        DoubleDistance node_knnDist = node_entry != null ?
                                      node_entry.approximateConservativeKnnDistance(k) :
                                      distanceFunction.infiniteDistance();

        DoubleDistance distance = distanceFunction.distance(entry.getObjectID(), q);
        DoubleDistance minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                                 distanceFunction.nullDistance() :
                                 distance.minus(entry.getCoveringRadius());

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkCoPTreeNode<O> childNode = (MkCoPTreeNode<O>) getNode(entry.getNodeID());
          doReverseKNNQuery1(k, q, entry, childNode, result);
        }
      }
    }
  }

  private void doReverseKNNQuery(int k,
                                 Integer q,
                                 List<QueryResult<DoubleDistance>> result,
                                 List<Integer> candidates) {

    final Heap<Distance, Identifiable> pq = new DefaultHeap<Distance, Identifiable>();

    // push root
    pq.addNode(new PQNode<DoubleDistance>(distanceFunction.nullDistance(), ROOT_NODE_ID.value(), null));

    // search in tree
    while (!pq.isEmpty()) {
      PQNode<DoubleDistance> pqNode = (PQNode<DoubleDistance>) pq.getMinNode();

      MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(pqNode.getValue().getID());
      Integer o_p = pqNode.getRoutingObjectID();

      // directory node
      if (! node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkCoPDirectoryEntry entry = (MkCoPDirectoryEntry) node.getEntry(i);
          DoubleDistance distance = distanceFunction.distance(entry.getObjectID(), q);
          DoubleDistance minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                                   distanceFunction.nullDistance() :
                                   distance.minus(entry.getCoveringRadius());

          DoubleDistance approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k);
          if (minDist.compareTo(approximatedKnnDist_cons) <= 0)
            pq.addNode(new PQNode<DoubleDistance>(minDist, entry.getNodeID(), entry.getObjectID()));
        }
      }
      // data node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkCoPLeafEntry entry = (MkCoPLeafEntry) node.getEntry(i);
          DoubleDistance distance = distanceFunction.distance(entry.getObjectID(), q);
          DoubleDistance approximatedKnnDist_prog = entry.approximateProgressiveKnnDistance(k);

          if (distance.compareTo(approximatedKnnDist_prog) <= 0) {
            result.add(new QueryResult<DoubleDistance>(entry.getObjectID(), distance));
//            System.out.println("\nObject " + entry.getObjectID() + " - " + q);
//            KNNList<DoubleDistance> knn = new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance());
//            doKNNQuery(entry.getObjectID(), knn);
//            System.out.println("  knns " + knn);
//            System.out.println("  prog " + approximatedKnnDist_prog);
//            System.out.println("  dist " + distance);
          }
          else {
            DoubleDistance approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k);
//            System.out.println("\nObject " + entry.getObjectID() + " - " + q);
//            KNNList<DoubleDistance> knn = new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance());
//            doKNNQuery(entry.getObjectID(), knn);
//            System.out.println("  knns " + knn);
//            System.out.println("  prog " + approximatedKnnDist_prog);
//            System.out.println("  cons " + approximatedKnnDist_cons);
//            System.out.println("  dist " + distance);

            double diff = distance.getValue() - approximatedKnnDist_cons.getValue();
//            if (distance.compareTo(approximatedKnnDist_cons) <= 0)
            if (diff <= 0.0000000001)
              candidates.add(entry.getObjectID());

          }
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  protected void test(Identifier rootID) {
    BreadthFirstEnumeration<MTreeNode<O, DoubleDistance>> bfs = new BreadthFirstEnumeration<MTreeNode<O, DoubleDistance>>(file, rootID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(id.value());
        node.test();

        if (id instanceof Entry) {
          MkCoPDirectoryEntry e = (MkCoPDirectoryEntry) id;
          node.testParentDistance(e.getObjectID(), distanceFunction);
          testCR(e);
          testKNNDistances(e);
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
  protected void test(Map<Integer, KNNList<DoubleDistance>> knnLists) {
    BreadthFirstEnumeration<MTreeNode<O, DoubleDistance>> bfs =
    new BreadthFirstEnumeration<MTreeNode<O, DoubleDistance>>(file, ROOT_NODE_ID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(id.value());
        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkCoPLeafEntry entry = (MkCoPLeafEntry) node.getEntry(i);
            List<DoubleDistance> knnDistances = getKNNList(entry.getObjectID(), knnLists);

            for (int k = 1; k <= k_max; k++) {
              DoubleDistance knnDist_cons = entry.approximateConservativeKnnDistance(k);
              DoubleDistance knnDist_prog = entry.approximateProgressiveKnnDistance(k);
              DoubleDistance knnDist_soll = knnDistances.get(k - 1);

              if (knnDist_cons.compareTo(knnDist_soll) < 0) {
                if (Math.abs(knnDist_soll.getValue() - knnDist_cons.getValue()) > 0.000000001) {
                  String msg = ("\nkDist[" + entry.getObjectID() + "] = " + knnDistances);
                  msg += "\nknnDist_cons[" + k + "] < knnDist_soll[" + k + "] \n" +
                         knnDist_cons + " < " + knnDist_soll + "\n" +
                         "in " + node + " at entry " + entry;

                  throw new RuntimeException(msg);
                }
              }

              if (knnDist_prog.compareTo(knnDist_soll) > 0) {
                if (Math.abs(knnDist_soll.getValue() - knnDist_prog.getValue()) > 0.000000001) {
                  String msg = ("\nkDist[" + entry.getObjectID() + "] = " + knnDistances);
                  msg += "\nknnDist_prog[" + k + "] > knnDist_soll[" + k + "] \n" +
                         knnDist_prog + " > " + knnDist_soll + "\n" +
                         "in " + node + " at entry " + entry;

                  throw new RuntimeException(msg);
                }
              }

            }
          }
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkCoPDirectoryEntry rootID) {
    MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(rootID.value());
    ApproximationLine knnDistances_soll_cons = node.conservativeKnnDistanceApproximation();
    ApproximationLine knnDistances_soll_prog = node.progressiveKnnDistanceApproximation();

    for (int k = 1; k <= k_max; k++) {
      DoubleDistance knnDist_ist_cons = rootID.approximateConservativeKnnDistance(k);
      DoubleDistance knnDist_ist_prog = rootID.approximateProgressiveKnnDistance(k);
      DoubleDistance knnDist_soll_cons = knnDistances_soll_cons.getApproximatedKnnDistance(k);
      DoubleDistance knnDist_soll_prog = knnDistances_soll_prog.getApproximatedKnnDistance(k);

      if (! knnDist_ist_cons.equals(knnDist_soll_cons)) {
        if (Math.abs(knnDist_ist_cons.getValue() - knnDist_soll_cons.getValue()) > 0.000000001) {
          String msg = "\nknnDist_ist_cons[" + k + "] != knnDist_soll_cons[" + k + "] \n" +
                       knnDist_ist_cons + " != " + knnDist_soll_cons + "\n" +
                       "in " + node;
          throw new RuntimeException(msg);
        }
      }

      if (! knnDist_ist_prog.equals(knnDist_soll_prog)) {
        if (Math.abs(knnDist_ist_prog.getValue() - knnDist_soll_prog.getValue()) > 0.000000001) {
          String msg = "\nknnDist_ist_prog[" + k + "] != knnDist_soll_prog[" + k + "] \n" +
                       knnDist_ist_prog + " != " + knnDist_soll_prog + "\n" +
                       "in " + node;

          throw new RuntimeException(msg);
        }
      }
    }
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private MkCoPTreeNode<O> split(MkCoPTreeNode<O> node) {
    Integer routingObjectID = null;
    if (node.getNodeID() != ROOT_NODE_ID.value()) {
      MkCoPTreeNode<O> parent = (MkCoPTreeNode<O>) getNode(node.getParentID());
      routingObjectID = parent.getEntry(node.getIndex()).getObjectID();
    }
    Split<DoubleDistance> split = new MLBDistSplit<O, DoubleDistance>(node, routingObjectID, distanceFunction);

    MkCoPTreeNode<O> newNode = (MkCoPTreeNode<O>) node.splitEntries(split.assignmentsToFirst, split.assignmentsToSecond);
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
    MkCoPTreeNode<O> parent = (MkCoPTreeNode<O>) getNode(node.getParentID());
    MkCoPTreeNode<O> grandParent;
    DoubleDistance parentDistance1 = null, parentDistance2 = null;

    if (parent.getID() != ROOT_NODE_ID.value()) {
      grandParent = (MkCoPTreeNode<O>) getNode(parent.getParentID());
      Integer parentObject = grandParent.getEntry(parent.getIndex()).getObjectID();
      parentDistance1 = distanceFunction.distance(split.firstPromoted, parentObject);
      parentDistance2 = distanceFunction.distance(split.secondPromoted, parentObject);
    }

    // add the newNode to parent
    parent.addDirectoryEntry(new MkCoPDirectoryEntry(split.secondPromoted,
                                                     parentDistance2,
                                                     newNode.getNodeID(),
                                                     split.secondCoveringRadius,
                                                     k_max,
                                                     newNode.conservativeKnnDistanceApproximation(),
                                                     newNode.progressiveKnnDistanceApproximation()));

    // set the first promotion object, parentDistance and covering radius for node in parent
    MkCoPDirectoryEntry entry1 = (MkCoPDirectoryEntry) parent.getEntry(node.getIndex());
    entry1.setObjectID(split.firstPromoted);
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(split.firstCoveringRadius);
    entry1.setConservativeKnnDistanceApproximation(node.conservativeKnnDistanceApproximation());
    entry1.setProgressiveKnnDistanceApproximation(node.progressiveKnnDistanceApproximation());

    // adjust the parentDistances in node
    for (int i = 0; i < node.getNumEntries(); i++) {
      DoubleDistance distance = distanceFunction.distance(split.firstPromoted, node.getEntry(i).getObjectID());
      node.getEntry(i).setParentDistance(distance);
    }

    // adjust the parentDistances in newNode
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      DoubleDistance distance = distanceFunction.distance(split.secondPromoted, newNode.getEntry(i).getObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }

    // write changes in parent to file
    file.writePage(parent);

    return parent;
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
  private MkCoPTreeNode<O> createNewRoot(final MkCoPTreeNode<O> oldRoot,
                                         final MkCoPTreeNode<O> newNode,
                                         Integer firstPromoted, Integer secondPromoted,
                                         DoubleDistance firstCoveringRadius, DoubleDistance secondCoveringRadius) {
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");

    MkCoPTreeNode<O> root = new MkCoPTreeNode<O>(file, dirCapacity, false);
    file.writePage(root);

    oldRoot.setID(root.getID());
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        MTreeNode<O, DoubleDistance> node = getNode(((DirectoryEntry) oldRoot.getEntry(i)).getNodeID());
        node.setParentID(oldRoot.getNodeID());
        file.writePage(node);
      }
    }
    file.writePage(oldRoot);

    root.setID(ROOT_NODE_ID.value());
    root.addDirectoryEntry(new MkCoPDirectoryEntry(firstPromoted,
                                                   null,
                                                   oldRoot.getNodeID(),
                                                   firstCoveringRadius,
                                                   k_max,
                                                   oldRoot.conservativeKnnDistanceApproximation(),
                                                   oldRoot.progressiveKnnDistanceApproximation()));

    root.addDirectoryEntry(new MkCoPDirectoryEntry(secondPromoted,
                                                   null,
                                                   newNode.getNodeID(),
                                                   secondCoveringRadius,
                                                   k_max,
                                                   newNode.conservativeKnnDistanceApproximation(),
                                                   newNode.progressiveKnnDistanceApproximation()));

    // adjust the parentDistances
    for (int i = 0; i < oldRoot.getNumEntries(); i++) {
      DoubleDistance distance = distanceFunction.distance(firstPromoted, oldRoot.getEntry(i).getObjectID());
      oldRoot.getEntry(i).setParentDistance(distance);
    }
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      DoubleDistance distance = distanceFunction.distance(secondPromoted, newNode.getEntry(i).getObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }

    msg.append("firstCoveringRadius ").append(firstCoveringRadius).append("\n");
    msg.append("secondCoveringRadius ").append(secondCoveringRadius).append("\n");

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    msg.append("New Root-ID ").append(root.getNodeID()).append("\n");

    logger.info(msg.toString());

    return root;
  }

  /**
   * Inserts the specified objects into this MDkNNTree-Tree.
   *
   * @param objects the object to be inserted
   */
  @SuppressWarnings({"unchecked"})
  private void insert(List<O> objects) {
    logger.info("insert " + objects + "\n");

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<DoubleDistance>> knnLists = new HashMap<Integer, KNNList<DoubleDistance>>();

    // insert first
    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<DoubleDistance>(k_max + 1, distanceFunction.infiniteDistance()));

      // find insertion node
      ParentInfo placeToInsert = findInsertionNode(getRoot(), object.getID(), null);
      DoubleDistance parentDistance = placeToInsert.getRoutingObjectID() != null ?
                                      distanceFunction.distance(object.getID(), placeToInsert.getRoutingObjectID()) :
                                      null;
      MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) placeToInsert.getNode();

      // add the entry
      MkCoPLeafEntry newEntry = new MkCoPLeafEntry(object.getID(), parentDistance, k_max,
                                                   new ApproximationLine(k_max, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                                                   new ApproximationLine(0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
      node.addLeafEntry(newEntry);

      // split the node if necessary
      node = (MkCoPTreeNode<O>) placeToInsert.getNode();
      while (hasOverflow(node)) {
        node = split(node);
      }
    }

    // do batch nn
    MkCoPTreeNode<O> root = (MkCoPTreeNode<O>) getRoot();
    batchNN(root, ids, knnLists);

    // adjust the knn distances
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkCoPEntry entry = (MkCoPEntry) root.getEntry(i);
      batchApproximateKNNDistances(entry, knnLists);
    }

    System.out.println(this.toString());
    test(knnLists);
    System.out.println("KNN DISTS OK");
    test(ROOT_NODE_ID);
  }

  private List<DoubleDistance> getKNNList(Integer id, Map<Integer, KNNList<DoubleDistance>> knnLists) {
    KNNList<DoubleDistance> knns = knnLists.get(id);
    List<DoubleDistance> result = knns.distancesToList();
//    result.remove(0);
    return result;
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchApproximateKNNDistances(MkCoPEntry entry, Map<Integer, KNNList<DoubleDistance>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      approximateKnnDistances(entry, getKNNList(entry.getObjectID(), knnLists));
      return;
    }

    MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(((MkCoPDirectoryEntry) entry).getNodeID());
    ApproximationLine conservative = new ApproximationLine(0, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    ApproximationLine progressive = new ApproximationLine(k_max, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPLeafEntry e = (MkCoPLeafEntry) node.getEntry(i);
        approximateKnnDistances(e, getKNNList(e.getObjectID(), knnLists));
        conservative = max(conservative, e.getConservativeKnnDistanceApproximation());
        progressive = min(progressive, e.getProgressiveKnnDistanceApproximation());
      }
      entry.setConservativeKnnDistanceApproximation(conservative);
      entry.setProgressiveKnnDistanceApproximation(progressive);
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPEntry e = (MkCoPEntry) node.getEntry(i);
        batchApproximateKNNDistances(e, knnLists);
        conservative = max(conservative, e.getConservativeKnnDistanceApproximation());
        progressive = min(progressive, e.getProgressiveKnnDistanceApproximation());
      }
      entry.setConservativeKnnDistanceApproximation(conservative);
      entry.setProgressiveKnnDistanceApproximation(progressive);
    }
  }

  /**
   * Returns a new approximation line as the maximum of the two specified lines.
   *
   * @param approx1 the first approximatation
   * @param approx2 the second approximatation
   * @return a new approximation line as the maximum of the two specified lines.
   */
  private ApproximationLine max(ApproximationLine approx1, ApproximationLine approx2) {
    // determine k_0, y_1, y_kmax
    int k_0 = Math.max(approx1.getK_0(), approx2.getK_0());
    double y_1 = Math.max(approx1.getValueAt(k_0), approx2.getValueAt(k_0));
    double y_kmax = Math.max(approx1.getValueAt(k_max), approx2.getValueAt(k_max));

    // determine m and t
    double m = (y_kmax - y_1) / (Math.log(k_max) - Math.log(k_0));
    double t = y_1 - m * Math.log(k_0);

    return new ApproximationLine(k_0, m, t);
  }

  /**
   * Returns a new approximation line as the minimum of the two specified lines.
   *
   * @param approx1 the first approximatation
   * @param approx2 the second approximatation
   * @return a new approximation line as the maximum of the two specified lines.
   */
  private ApproximationLine min(ApproximationLine approx1, ApproximationLine approx2) {
    // determine k_0, y_1, y_kmax
    int k_0 = Math.min(approx1.getK_0(), approx2.getK_0());
    double y_1 = Math.min(approx1.getValueAt(k_0), approx2.getValueAt(k_0));
    double y_kmax = Math.min(approx1.getValueAt(k_max), approx2.getValueAt(k_max));

    // determine m
    double m = (y_kmax - y_1) / (Math.log(k_max) - Math.log(k_0));
    double t = y_1 - m * Math.log(k_0);

    return new ApproximationLine(k_0, m, t);
  }

  /**
   * Computes logarithmic skew (fractal dimension ie. m) and
   * in kappx[0] and kappx[1] the non-logarithmic values of the
   * approximated first and last nearest neighbor distances
   *
   * @param logk  logs of k
   * @param nn
   * @param lower
   * @param upper TODO: Spezialbehandlung für identische Punkte in DB (insbes. Distanz 0)
   */
  public void approxKdistOLD(double sumx, double sumx2, double[] logk, double [] nn, double [] lower, double [] upper) {
    int k_0 = 0;
    StringBuffer msg = new StringBuffer();

    // sum values
    double sumy = 0;
    double sumxy = 0;
    // logs of the distances
    double[] lnn = new double[k_max];

    // fill lnn and sum values
    for (int l = 0; l < k_max; l++) {
      double h = lnn[l] = Math.log(nn[l + 1]);
      sumy += h;
      sumxy += h * logk[l];
    }

    // lower and upper hull
    int[] lhull = new int[k_max];
    int[] uhull = new int[k_max];
    // first point is always in lhull
    lhull[0] = 0;
    // number of points in lhull
    int i = 1;
    // first point is always in uhull
    uhull[0] = 0;
    // number of points in uhull
    int j = 1;

    // Determine the convex hulls (using point stack)
    for (int l = 1; l < k_max; l++) {
      // lower hull
      lhull[i] = l;
      while (i >= 2
             && (lnn[lhull[i]] - lnn[lhull[i - 1]]) / (logk[lhull[i]] - logk[lhull[i - 1]])
                <= (lnn[lhull[i - 1]] - lnn[lhull[i - 2]]) / (logk[lhull[i - 1]] - logk[lhull[i - 2]])) {
        // right curved
        lhull[i - 1] = lhull[i];
        i--;
      }
      i++;
      // upper hull
      uhull[j] = l;
      while (j >= 2
             && (lnn[uhull[j]] - lnn[uhull[j - 1]]) / (logk[uhull[j]] - logk[uhull[j - 1]])
                >= (lnn[uhull[j - 1]] - lnn[uhull[j - 2]]) / (logk[uhull[j - 1]] - logk[uhull[j - 2]])) {
        // left curved
        uhull[j - 1] = uhull[j];
        j--;
      }
      j++;
    }

    msg.append("lower and upper hull\n");
    for (int l = 0; l < i; l++) {
      msg.append("  uhull ").append(l).append("=").append(uhull[l]).append(" y=").append(lnn[uhull[l]]).append("\n");
      msg.append("  lhull ").append(l).append("=").append(lhull[l]).append(" y=").append(lnn[lhull[l]]).append("\n");
    }

    // linear search on all line segments on the lower convex hull
    msg.append("lower hull: " + i + "\n");
    double low_error = Double.MAX_VALUE;
    double low_m = 0.0;
    double low_t = 0.0;
    for (int l = 1; l < i; l++) {
      double cur_m = (lnn[lhull[l]] - lnn[lhull[l - 1]]) / (logk[lhull[l]] - logk[lhull[l - 1]]);
      double cur_t = lnn[lhull[l]] - cur_m * logk[lhull[l]];
      double cur_error = ssqerr(k_0, k_max, logk, lnn, cur_m, cur_t);
      msg.append("Segment=" + l + " m=" + cur_m + " lowerror=" + cur_error + "\n");
      if (cur_error < low_error) {
        low_error = cur_error;
        low_m = cur_m;
        low_t = cur_t;
      }
    }
    // linear search on all points of the lower convex hull
    for (int l = 0; l < i; l++) {
      double cur_m = optimize(k_0, k_max, sumx, sumx2, logk[lhull[l]], lnn[lhull[l]], sumxy, sumy);
      double cur_t = lnn[lhull[l]] - cur_m * logk[lhull[l]];
      // only valid if both neighboring points are underneath y=mx+t
      if ((l == 0 || lnn[lhull[l - 1]] >=
                     lnn[lhull[l]] - cur_m * (logk[lhull[l]] - logk[lhull[l - 1]]))
          &&
          (l == i - 1 || lnn[lhull[l + 1]] >=
                         lnn[lhull[l]] + cur_m * (logk[lhull[l + 1]] - logk[lhull[l]]))) {
        double cur_error = ssqerr(k_0, k_max, logk, lnn, cur_m, cur_t);
        if (cur_error < low_error) {
          low_error = cur_error;
          low_m = cur_m;
          low_t = cur_t;
        }
      }

      // check proof of bisection search
      boolean is_right = true;
      if (! (l > 0 && lnn[lhull[l - 1]] < lnn[lhull[l]] - cur_m * (logk[lhull[l]] - logk[lhull[l - 1]])) &&
          ! is_right)
        System.out.println("ERROR lower: The bisection search will not work properly !");
      if (!(l < i - 1 && lnn[lhull[l + 1]] < lnn[lhull[l]] + cur_m * (logk[lhull[l + 1]] - logk[lhull[l]])))
        is_right = false;
    }
    lower[0] = low_t;
    lower[1] = low_m * logk[k_max - 1] + low_t;
    msg.append("m = " + low_m + " lower0 = " + lower[0] + " lower1=" + lower[1]);

    // linear search on all line segments on the upper convex hull
    msg.append("upper hull:" + j);
    double upp_error = Double.MAX_VALUE;
    double upp_m = 0.0;
    double upp_t = 0.0;
    for (int l = 1; l < j; l++) {
      double cur_m = (lnn[uhull[l]] - lnn[uhull[l - 1]]) / (logk[uhull[l]] - logk[uhull[l - 1]]);
      double cur_t = lnn[uhull[l]] - cur_m * logk[uhull[l]];
      double cur_error = ssqerr(k_0, k_max, logk, lnn, cur_m, cur_t);
      if (cur_error < upp_error) {
        upp_error = cur_error;
        upp_m = cur_m;
        upp_t = cur_t;
      }
    }
    // linear search on all points of the upper convex hull
    for (int l = 0; l < j; l++) {
      double cur_m = optimize(k_0, k_max, sumx, sumx2, logk[uhull[l]], lnn[uhull[l]], sumxy, sumy);
      double cur_t = lnn[uhull[l]] - cur_m * logk[uhull[l]];
      // only valid if both neighboring points are underneath y=mx+t
      if ((l == 0 || lnn[uhull[l - 1]] <=
                     lnn[uhull[l]] - cur_m * (logk[uhull[l]] - logk[uhull[l - 1]]))
          &&
          (l == j - 1 || lnn[uhull[l + 1]] <=
                         lnn[uhull[l]] + cur_m * (logk[uhull[l + 1]] - logk[uhull[l]]))) {
        double cur_error = ssqerr(k_0, k_max, logk, lnn, cur_m, cur_t);
        if (cur_error < upp_error) {
          upp_error = cur_error;
          upp_m = cur_m;
          upp_t = cur_t;
        }
      }

      // check proof of bisection search
      boolean is_left = true; // NEEDED FOR PROOF CHECK
      if (! (l > 0 && lnn[uhull[l - 1]] > lnn[uhull[l]] - cur_m * (logk[uhull[l]] - logk[uhull[l - 1]])) && ! is_left)
        System.out.println("ERROR upper: The bisection search will not work properly !");
      if (!(l < j - 1 && lnn[uhull[l + 1]] > lnn[uhull[l]] + cur_m * (logk[uhull[l + 1]] - logk[uhull[l]])))
        is_left = false;
    }
    upper[0] = upp_t;
    upper[1] = upp_m * logk[k_max - 1] + upp_t;
    msg.append("m = " + upp_m + " upper0 = " + upper[0] + " upper1 = " + upper[1]);
  }

  /*
  * auxiliary function for approxKdist methods.
  */
  private double ssqerr(int k0, int kmax, double[] logk, double [] lnn, double m, double t) {
    double result = 0;
    for (int i = 0; i < kmax - k0; i++) {
      double h = lnn[i] - (m * (logk[i] - logk[0]) + t);
      result += h * h;
    }
    return result;
  }

  /*
   * auxiliary function for approxKdist methods.
   */
  private double optimize(int k0, int kmax, double sumx, double sumx2, double xp, double yp, double sumxy, double sumy) {
    int k = kmax - k0;
    return (sumxy - xp * sumy - yp * sumx + k * xp * yp) /
           (sumx2 - 2 * sumx * xp + k * xp * xp);
//    return (-xp * yp *  + yp * sumx - sumxy + xp * sumy) / (-xp * xp * kmax - sumx2 + 2 * xp * sumx);
  }

  /**
   * Computes logarithmic skew (fractal dimension ie. m) and
   * in kappx[0] and kappx[1] the non-logarithmic values of the
   * approximated first and last nearest neighbor distances
   *
   * @param knnDistances TODO: Spezialbehandlung für identische Punkte in DB (insbes. Distanz 0)
   */
  public void approximateKnnDistances(MkCoPEntry entry, List<DoubleDistance> knnDistances) {
    // count the zero distances
    int k_0 = 0;
    for (int i = 0; i < k_max; i++) {
      double dist = knnDistances.get(i).getValue();
      if (dist == 0) k_0++;
      else
        break;
    }

    // init variables
    double sum_log_kDist = 0;
    double sum_log_k_kDist = 0;
    double[] log_kDist = new double[k_max - k_0];

    for (int i = 0; i < k_max - k_0; i++) {
      double dist = knnDistances.get(i + k_0).getValue();
      log_kDist[i] = Math.log(dist);
      sum_log_kDist += log_kDist[i];
      sum_log_k_kDist += log_kDist[i] * log_k[i];
    }

    double[] log_k = new double[k_max - k_0];
    System.arraycopy(this.log_k, k_0, log_k, 0, k_max - k_0);

    // lower and upper hull
    ConvexHull convexHull = new ConvexHull(log_k, log_kDist);

    // approximate lower hull
    ApproximationLine conservative = approximateUpperHull(convexHull, log_k, log_kDist, sum_log_kDist, sum_log_k_kDist);
    // approximate upper hull
    ApproximationLine progressive = approximateLowerHull(convexHull, log_k, log_kDist, sum_log_kDist, sum_log_k_kDist);

    entry.setConservativeKnnDistanceApproximation(conservative);
    entry.setProgressiveKnnDistanceApproximation(progressive);
  }

  /**
   * Initilaizes the log_k, sum_log_k and sum_log_k2 values.
   */
  private void init() {
    log_k = new double[k_max];
    sum_log_k = 0;
    sum_log_k2 = 0;

    for (int k = 1; k <= k_max; k++) {
      double log = Math.log(k);
      log_k[k - 1] = log;
      sum_log_k += log;
      sum_log_k2 += log * log;
    }
  }

  /**
   * Approximates the lower hull.
   *
   * @param convexHull
   * @param log_kDist
   * @param sum_log_kDist
   * @param sum_log_k_kDist
   */
  private ApproximationLine approximateLowerHull(ConvexHull convexHull, double[] log_k, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuffer msg = new StringBuffer();
    int[] lowerHull = convexHull.getLowerHull();
    int l = convexHull.getNumberOfPointsInLowerHull();
    int k_0 = k_max - lowerHull.length + 1;
    double sum_log_k = 0;
    double sum_log_k2 = 0;
    for (double log : log_k) {
      sum_log_k += log;
      sum_log_k += log * log;
    }

    System.out.println("YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
    System.out.println("log_k     " + Util.format(log_k, ",", 6));
    System.out.println("log_k_dist" + Util.format(log_kDist, ",", 6));

    // linear search on all line segments on the lower convex hull
    msg.append("lower hull l = " + l + "\n");
    double low_error = Double.MAX_VALUE;
    double low_m = 0.0;
    double low_t = 0.0;

    for (int i = 1; i < l; i++) {
      double cur_m = (log_kDist[lowerHull[i]] - log_kDist[lowerHull[i - 1]]) / (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]]);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
      msg.append("  Segment = " + i + " m = " + cur_m + " t = " + cur_t +  " lowerror = " + cur_error + "\n");
      if (cur_error < low_error) {
        low_error = cur_error;
        low_m = cur_m;
        low_t = cur_t;
      }
    }
    ApproximationLine lowerApproximation = new ApproximationLine(k_0, low_m, low_t);

    System.out.println(msg);
    System.out.println("  lower Approx " + lowerApproximation);

    for (int k = 0; k <= k_max - k_0; k++) {
      double y = log_kDist[k];
      double y_ist = lowerApproximation.getValueAt(k + k_0 + 1);
      if (y_ist < y) {
        System.out.println("  " + (k + 1) + " y_ist < y : " + y_ist +" > " + y);
      }
      else
        System.out.println("  " + (k + 1) + " ok " + y_ist);
    }

    // linear search on all points of the lower convex hull
    boolean is_right = true; // NEEDED FOR PROOF CHECK
    for (int i = 0; i < l; i++) {
      double cur_m = optimize(k_0, k_max, sum_log_k, sum_log_k2, log_k[lowerHull[i]], log_kDist[lowerHull[i]], sum_log_k_kDist, sum_log_kDist);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      // only valid if both neighboring points are underneath y=mx+t
      if ((i == 0 || log_kDist[lowerHull[i - 1]] >=
                     log_kDist[lowerHull[i]] - cur_m * (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]]))
          &&
          (i == l - 1 || log_kDist[lowerHull[i + 1]] >=
                         log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]]))) {
        double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
        if (cur_error < low_error) {
          low_error = cur_error;
          low_m = cur_m;
          low_t = cur_t;
        }
      }

      // check proof of bisection search
      if (! (i > 0 && log_kDist[lowerHull[i - 1]] < log_kDist[lowerHull[i]] - cur_m * (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]])) &&
          ! is_right)
        System.out.println("ERROR lower: The bisection search will not work properly !");
      if (!(i < l - 1 && log_kDist[lowerHull[i + 1]] < log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]])))
        is_right = false;
    }

    lowerApproximation = new ApproximationLine(k_0, low_m, low_t);


    System.out.println(" lowerHull " + Util.format(log_kDist, ",", 6));
    System.out.println(" l " + convexHull.getNumberOfPointsInLowerHull());
    for (int k = 0; k <= k_max - k_0; k++) {
      double y = log_kDist[k];
      double y_ist = lowerApproximation.getValueAt(k + k_0 + 1);
      if (y_ist < y) {
        System.out.println("  " + (k+1) + " y_ist < y : " + y_ist +" > " + y);
      }
      else
        System.out.println("  " + (k+1) + " ok " + y_ist);
    }

    return lowerApproximation;
  }

  private ApproximationLine approximateUpperHull(ConvexHull convexHull, double[] log_k, double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuffer msg = new StringBuffer();
    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = k_max - upperHull.length + 1;

    double sum_log_k = 0;
    double sum_log_k2 = 0;
    for (int i = 0; i < log_k.length; i++) {
      sum_log_k += log_k[i];
      sum_log_k += log_k[i] * log_k[i];
    }

    // linear search on all line segments on the upper convex hull
    msg.append("upper hull:" + u);
    double upp_error = Double.MAX_VALUE;
    double upp_m = 0.0;
    double upp_t = 0.0;
    for (int i = 1; i < u; i++) {
      double cur_m = (log_kDist[upperHull[i]] - log_kDist[upperHull[i - 1]]) / (log_k[upperHull[i]] - log_k[upperHull[i - 1]]);
      double cur_t = log_kDist[upperHull[i]] - cur_m * log_k[upperHull[i]];
      double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
      if (cur_error < upp_error) {
        upp_error = cur_error;
        upp_m = cur_m;
        upp_t = cur_t;
      }
    }
    // linear search on all points of the upper convex hull
    boolean is_left = true; // NEEDED FOR PROOF CHECK
    for (int i = 0; i < u; i++) {
      double cur_m = optimize(k_0, k_max, sum_log_k, sum_log_k2, log_k[upperHull[i]], log_kDist[upperHull[i]], sum_log_k_kDist, sum_log_kDist);
      double cur_t = log_kDist[upperHull[i]] - cur_m * log_k[upperHull[i]];
      // only valid if both neighboring points are underneath y=mx+t
      if ((i == 0 || log_kDist[upperHull[i - 1]] <=
                     log_kDist[upperHull[i]] - cur_m * (log_k[upperHull[i]] - log_k[upperHull[i - 1]]))
          &&
          (i == u - 1 || log_kDist[upperHull[i + 1]] <=
                         log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]]))) {
        double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
        if (cur_error < upp_error) {
          upp_error = cur_error;
          upp_m = cur_m;
          upp_t = cur_t;
        }
      }

      // check proof of bisection search
      if (! (i > 0 && log_kDist[upperHull[i - 1]] > log_kDist[upperHull[i]] - cur_m * (log_k[upperHull[i]] - log_k[upperHull[i - 1]])) && ! is_left)
        System.out.println("ERROR upper: The bisection search will not work properly !");
      if (!(i < u - 1 && log_kDist[upperHull[i + 1]] > log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]])))
        is_left = false;
    }

    ApproximationLine upperApproximation = new ApproximationLine(k_0, upp_m, upp_t);

    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    for (int k = 0; k < k_max - k_0; k++) {
      double y = log_kDist[k];
      double y_ist = upperApproximation.getValueAt(k + k_0 + 1);
      if (y_ist > y) {
        System.out.println("  " + (k+1) + " y_ist > y : " + y_ist +" > " + y);
      }
      else
        System.out.println("  " + (k+1) + " ok");
    }

    return upperApproximation;
  }
}
