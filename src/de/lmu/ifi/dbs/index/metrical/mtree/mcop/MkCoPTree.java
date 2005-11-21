package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.mknn.MkNNTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtree.util.PQNode;
import de.lmu.ifi.dbs.index.metrical.mtree.util.ParentInfo;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
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
public class MkCoPTree<O extends MetricalObject> extends MTree<O, NumberDistance> {

  /**
   * The parameter kmax.
   */
  private int k_max;

  /**
   * The values of log(1),..,log(k_max)
   */
  private double[] log_k;

  /**
   * Provides some statistics about performed reverse nn queries.
   */
  private RkNNStatistic rkNNStatistics = new RkNNStatistic();

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
                   DistanceFunction<O, NumberDistance> distanceFunction, int kmax,
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
                   DistanceFunction<O, NumberDistance> distanceFunction, int kmax) {
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
  public List<QueryResult<NumberDistance>> reverseKNNQuery(O object, int k) {
    if (k > this.k_max) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " +
                                         "parameter kmax of the MCop-Tree!");
    }

    List<QueryResult<NumberDistance>> result = new ArrayList<QueryResult<NumberDistance>>();
    List<Integer> candidates = new ArrayList<Integer>();
    doReverseKNNQuery(k, object.getID(), result, candidates);

    // refinement of candidates
    Map<Integer, KNNList<NumberDistance>> knnLists = new HashMap<Integer, KNNList<NumberDistance>>();
    for (Integer id : candidates)
      knnLists.put(id, new KNNList<NumberDistance>(k, distanceFunction.infiniteDistance()));
    batchNN(getRoot(), candidates, knnLists);

    Collections.sort(result);
    Collections.sort(candidates);

    rkNNStatistics.noCandidates += candidates.size();
    rkNNStatistics.noTrueHits += result.size();

    for (Integer id : candidates) {
      List<QueryResult<NumberDistance>> knns = knnLists.get(id).toList();
      for (QueryResult<NumberDistance> qr : knns) {
        if (qr.getID() == object.getID()) {
          result.add(new QueryResult<NumberDistance>(id, qr.getDistance()));
          break;
        }
      }

    }
    Collections.sort(result);

    rkNNStatistics.noResults += result.size();
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

    MTreeNode<O, NumberDistance> node = getRoot();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        DirectoryEntry entry = (DirectoryEntry) node.getEntry(0);
        node = getNode(entry.getNodeID());
        levels++;
      }
    }

    BreadthFirstEnumeration<MTreeNode<O, NumberDistance>> enumeration =
    new BreadthFirstEnumeration<MTreeNode<O, NumberDistance>>(file, ROOT_NODE_ID);

    int counter = 0;
    while (enumeration.hasMoreElements()) {
      Identifier id = enumeration.nextElement();
      if (! id.isNodeID()) {
        objects++;
//        MkCoPLeafEntry e = (MkCoPLeafEntry) id;
//        System.out.println(counter++ + " Object " + e.getObjectID());
//        System.out.println("  pd  = " + e.getParentDistance());
//        System.out.println("  consApprox  = " + Arrays.asList(e.getConservativeKnnDistanceApproximation()));
//        System.out.println("  progrApprox = " + Arrays.asList(e.getProgressiveKnnDistanceApproximation()));
      }
      else {
        node = file.readPage(id.value());
//        System.out.println(node + ", numEntries = " + node.getNumEntries());

        if (id instanceof DirectoryEntry) {
//          MkCoPDirectoryEntry e = (MkCoPDirectoryEntry) id;

//          System.out.println("  r_obj = " + e.getObjectID());
//          System.out.println("  pd = " + e.getParentDistance());
//          System.out.println("  cr = " + e.getCoveringRadius());
//          System.out.println("  consApprox  = " + Arrays.asList(e.getConservativeKnnDistanceApproximation()));
        }

        if (node.isLeaf()) {
//          for (int i = 0; i < node.getNumEntries(); i++) {
//            MkCoPLeafEntry e = (MkCoPLeafEntry) node.getEntry(i);
//            if (e.getObjectID() == 57 || e.getObjectID() == 7)
//              System.out.println("Xxxxxxxxxxxxxx " + e.getObjectID() + " parent = " + node);
//          }
          leafNodes++;
        }
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
   * Returns the statistic for performed rknn queries.
   *
   * @return the statistic for performed rknn queries
   */
  public RkNNStatistic getRkNNStatistics() {
    return rkNNStatistics;
  }

  /**
   * Clears the values of the statistic for performed rknn queries
   */
  public void clearRkNNStatistics() {
    rkNNStatistics.clear();
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  protected MTreeNode<O, NumberDistance> createEmptyRoot() {
    return new MkCoPTreeNode<O>(file, leafCapacity, true);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected void initCapacity(int pageSize) {
    NumberDistance dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID + coveringRadius + parentDistance + consApprox) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 18) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + consApprox + progrApprox) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize + 18 + 18) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a leaf node = " + (leafCapacity - 1));

  }

  /**
   * Performs a reverse knn query.
   *
   * @param k          the parametr k of the rknn query
   * @param q          the id of the query object
   * @param result     holds the true results (they need not to be refined)
   * @param candidates holds possible candidates for the result (they need a refinement)
   */
  private void doReverseKNNQuery(int k,
                                 Integer q,
                                 List<QueryResult<NumberDistance>> result,
                                 List<Integer> candidates) {

    final Heap<NumberDistance, Identifiable> pq = new DefaultHeap<NumberDistance, Identifiable>();

    // push root
    pq.addNode(new PQNode<NumberDistance>(distanceFunction.nullDistance(), ROOT_NODE_ID.value(), null));

    // search in tree
    while (!pq.isEmpty()) {
      PQNode<NumberDistance> pqNode = (PQNode<NumberDistance>) pq.getMinNode();

      MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(pqNode.getValue().getID());

      // directory node
      if (! node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkCoPDirectoryEntry entry = (MkCoPDirectoryEntry) node.getEntry(i);
          NumberDistance distance = distanceFunction.distance(entry.getObjectID(), q);
//          System.out.println("\nro " + entry.getObjectID());
//          System.out.println("cr " + entry.getCoveringRadius());
//          System.out.println("distance(" + entry.getObjectID()+","+q+") = " +distance);
          NumberDistance minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                                   distanceFunction.nullDistance() :
                                   (NumberDistance) distance.minus(entry.getCoveringRadius());
          NumberDistance approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k, distanceFunction);
//          System.out.println("minDist(n_" + entry.getNodeID()+","+q+") = " +minDist);
//          System.out.println("approximatedKnnDist_cons(n_" + entry.getNodeID() + ") = " +approximatedKnnDist_cons);

          if (minDist.compareTo(approximatedKnnDist_cons) <= 0)
            pq.addNode(new PQNode<NumberDistance>(minDist, entry.getNodeID(), entry.getObjectID()));
        }
      }
      // data node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkCoPLeafEntry entry = (MkCoPLeafEntry) node.getEntry(i);
          NumberDistance distance = distanceFunction.distance(entry.getObjectID(), q);
          NumberDistance approximatedKnnDist_prog = entry.approximateProgressiveKnnDistance(k, distanceFunction);

          if (distance.compareTo(approximatedKnnDist_prog) <= 0) {
            result.add(new QueryResult<NumberDistance>(entry.getObjectID(), distance));
//            System.out.println("\nObject " + entry.getObjectID() + " - " + q);
//            KNNList<DoubleDistance> knn = new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance());
//            doKNNQuery(entry.getObjectID(), knn);
//            System.out.println("  knns " + knn);
//            System.out.println("  prog " + approximatedKnnDist_prog);
//            System.out.println("  dist " + distance);
          }
          else {
            NumberDistance approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k, distanceFunction);
//            System.out.println("\nObject " + entry.getObjectID() + " - " + q);
//            KNNList<DoubleDistance> knn = new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance());
//            doKNNQuery(entry.getObjectID(), knn);
//            System.out.println("  knns " + knn);
//            System.out.println("  prog " + approximatedKnnDist_prog);
//            System.out.println("  cons " + approximatedKnnDist_cons);
//            System.out.println("  dist " + distance);

            double diff = distance.getDoubleValue() - approximatedKnnDist_cons.getDoubleValue();
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
    BreadthFirstEnumeration<MTreeNode<O, NumberDistance>> bfs = new BreadthFirstEnumeration<MTreeNode<O, NumberDistance>>(file, rootID);

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
  protected void test(Map<Integer, KNNList<NumberDistance>> knnLists) {
    BreadthFirstEnumeration<MTreeNode<O, NumberDistance>> bfs =
    new BreadthFirstEnumeration<MTreeNode<O, NumberDistance>>(file, ROOT_NODE_ID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(id.value());
        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkCoPLeafEntry entry = (MkCoPLeafEntry) node.getEntry(i);
            List<NumberDistance> knnDistances = getKNNList(entry.getObjectID(), knnLists);

            for (int k = 1; k <= k_max; k++) {
              NumberDistance knnDist_cons = entry.approximateConservativeKnnDistance(k, distanceFunction);
              NumberDistance knnDist_prog = entry.approximateProgressiveKnnDistance(k, distanceFunction);
              NumberDistance knnDist_soll = knnDistances.get(k - 1);

//              System.out.println("\nObject " + entry.getObjectID());
//              System.out.println("knnDist_soll["+k+"]" + knnDist_soll);
//              System.out.println("knnDist_cons["+k+"]" + knnDist_cons);
//              System.out.println("knnDist_prog["+k+"]" + knnDist_prog);

              if (knnDist_cons.compareTo(knnDist_soll) < 0) {
                if (Math.abs(knnDist_soll.getDoubleValue() - knnDist_cons.getDoubleValue()) > 0.000000001) {
                  String msg = ("\nkDist[" + entry.getObjectID() + "] = " + knnDistances);
                  msg += "\nknnDist_cons[" + k + "] < knnDist_soll[" + k + "] \n" +
                         knnDist_cons + " < " + knnDist_soll + "\n" +
                         "in " + node + " at entry " + entry;

                  throw new RuntimeException(msg);
                }
              }

              if (knnDist_prog.compareTo(knnDist_soll) > 0) {
                if (Math.abs(knnDist_soll.getDoubleValue() - knnDist_prog.getDoubleValue()) > 0.000000001) {
                  String msg = ("\nkDist[" + entry.getObjectID() + "] = " + knnDistances);
                  msg += "\nknnDist_prog[" + k + "] > knnDist_soll[" + k + "] \n" +
                         knnDist_prog + " > " + knnDist_soll + "\n" +
                         "in " + node + " at entry " + entry;

                  throw new RuntimeException(msg);
                }
              }

            }
            testKNNDistances(node, entry, knnDistances);
          }


        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkCoPTreeNode<O> node, MkCoPLeafEntry entry, List<NumberDistance> knnDistances) {

    ApproximationLine knnDistances_node = node.conservativeKnnDistanceApproximation(k_max);

    for (int k = 1; k <= k_max; k++) {
      NumberDistance knnDistance_node = knnDistances_node.getApproximatedKnnDistance(k, distanceFunction);
      NumberDistance knnDistance = knnDistances.get(k-1);

      String msg1 = "\nknnDistance[" + k + "] -- knnDistance_node[" + k + "] \n" +
                       knnDistance + " -- " + knnDistance_node + "\n" +
                       "in " + node + " (entry " + entry.getObjectID() + ")";
      System.out.println(msg1);

      if (knnDistance.compareTo(knnDistance_node) > 0) {
        if (Math.abs(knnDistance.getDoubleValue()) - knnDistance_node.getDoubleValue() > 0.000000001) {
          String msg = "\nknnDistance[" + k + "] > knnDistance_node[" + k + "] \n" +
                       knnDistance + " > " + knnDistance_node + "\n" +
                       "in " + node + " (entry " + entry + ")";

          throw new RuntimeException(msg);
        }
      }
    }
    if (node.getNodeID() != ROOT_NODE_ID.value()) {
      MkCoPTreeNode<O> parent = (MkCoPTreeNode<O>) getNode(node.getParentID());
      testKNNDistances(parent, entry, knnDistances);
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkCoPDirectoryEntry rootID) {
    MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(rootID.value());
    ApproximationLine knnDistances_soll_cons = node.conservativeKnnDistanceApproximation(k_max);

    for (int k = 1; k <= k_max; k++) {
      NumberDistance knnDist_ist_cons = rootID.approximateConservativeKnnDistance(k, distanceFunction);
      NumberDistance knnDist_soll_cons = knnDistances_soll_cons.getApproximatedKnnDistance(k, distanceFunction);

      if (! knnDist_ist_cons.equals(knnDist_soll_cons)) {
        if (Math.abs(knnDist_ist_cons.getDoubleValue() - knnDist_soll_cons.getDoubleValue()) > 0.000000001) {
          String msg = "\nknnDist_ist_cons[" + k + "] != knnDist_soll_cons[" + k + "] \n" +
                       knnDist_ist_cons + " != " + knnDist_soll_cons + "\n" +
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
    Split<NumberDistance> split = new MLBDistSplit<O, NumberDistance>(node, routingObjectID, distanceFunction);

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
    NumberDistance parentDistance1 = null, parentDistance2 = null;

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
                                                     newNode.conservativeKnnDistanceApproximation(k_max)));

    // set the first promotion object, parentDistance, covering radius
    // and knn distance approximation for node in parent
    MkCoPDirectoryEntry entry1 = (MkCoPDirectoryEntry) parent.getEntry(node.getIndex());
    entry1.setObjectID(split.firstPromoted);
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(split.firstCoveringRadius);
    entry1.setConservativeKnnDistanceApproximation(node.conservativeKnnDistanceApproximation(k_max));

    // adjust the parentDistances in node
    for (int i = 0; i < node.getNumEntries(); i++) {
      NumberDistance distance = distanceFunction.distance(split.firstPromoted, node.getEntry(i).getObjectID());
      node.getEntry(i).setParentDistance(distance);
    }

    // adjust the parentDistances in newNode
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      NumberDistance distance = distanceFunction.distance(split.secondPromoted, newNode.getEntry(i).getObjectID());
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
                                         NumberDistance firstCoveringRadius, NumberDistance secondCoveringRadius) {
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");

    MkCoPTreeNode<O> root = new MkCoPTreeNode<O>(file, dirCapacity, false);
    file.writePage(root);

    oldRoot.setID(root.getID());
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        MTreeNode<O, NumberDistance> node = getNode(((DirectoryEntry) oldRoot.getEntry(i)).getNodeID());
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
                                                   oldRoot.conservativeKnnDistanceApproximation(k_max)));

    root.addDirectoryEntry(new MkCoPDirectoryEntry(secondPromoted,
                                                   null,
                                                   newNode.getNodeID(),
                                                   secondCoveringRadius,
                                                   newNode.conservativeKnnDistanceApproximation(k_max)));

    // adjust the parentDistances
    for (int i = 0; i < oldRoot.getNumEntries(); i++) {
      NumberDistance distance = distanceFunction.distance(firstPromoted, oldRoot.getEntry(i).getObjectID());
      oldRoot.getEntry(i).setParentDistance(distance);
    }
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      NumberDistance distance = distanceFunction.distance(secondPromoted, newNode.getEntry(i).getObjectID());
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
    Map<Integer, KNNList<NumberDistance>> knnLists = new HashMap<Integer, KNNList<NumberDistance>>();

    // insert first
    System.out.println("insert");
    for (O object : objects) {
//      System.out.println("insert " + object.getID());
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<NumberDistance>(k_max + 1, distanceFunction.infiniteDistance()));

      // find insertion node
      ParentInfo placeToInsert = findInsertionNode(getRoot(), object.getID(), null);
      NumberDistance parentDistance = placeToInsert.getRoutingObjectID() != null ?
                                      distanceFunction.distance(object.getID(), placeToInsert.getRoutingObjectID()) :
                                      null;
      MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) placeToInsert.getNode();

      // add the entry
      MkCoPLeafEntry newEntry = new MkCoPLeafEntry(object.getID(),
                                                   parentDistance,
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
    System.out.println("batch nn");
    MkCoPTreeNode<O> root = (MkCoPTreeNode<O>) getRoot();
    batchNN(root, ids, knnLists);

    // adjust the knn distances
    System.out.println("adjust the knn distances");
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkCoPEntry entry = (MkCoPEntry) root.getEntry(i);
      batchApproximateKNNDistances(entry, knnLists);
    }

//    test(knnLists);
//    test(ROOT_NODE_ID);
  }

  private List<NumberDistance> getKNNList(Integer id, Map<Integer, KNNList<NumberDistance>> knnLists) {
    KNNList<NumberDistance> knns = knnLists.get(id);
    List<NumberDistance> result = knns.distancesToList();
//    result.remove(0);
    return result;
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchApproximateKNNDistances(MkCoPEntry entry, Map<Integer, KNNList<NumberDistance>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      approximateKnnDistances((MkCoPLeafEntry) entry, getKNNList(entry.getObjectID(), knnLists));
      return;
    }

    MkCoPTreeNode<O> node = (MkCoPTreeNode<O>) getNode(((MkCoPDirectoryEntry) entry).getNodeID());
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPLeafEntry e = (MkCoPLeafEntry) node.getEntry(i);
        approximateKnnDistances(e, getKNNList(e.getObjectID(), knnLists));
      }
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPEntry e = (MkCoPEntry) node.getEntry(i);
        batchApproximateKNNDistances(e, knnLists);
      }
    }
    entry.setConservativeKnnDistanceApproximation(node.conservativeKnnDistanceApproximation(k_max));
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
   * @param knnDistances TODO: Spezialbehandlung fuer identische Punkte in DB (insbes. Distanz 0)
   */
  private void approximateKnnDistances(MkCoPLeafEntry entry, List<NumberDistance> knnDistances) {
    // count the zero distances
    int k_0 = 0;
    for (int i = 0; i < k_max; i++) {
      double dist = knnDistances.get(i).getDoubleValue();
      if (dist == 0) k_0++;
      else
        break;
    }

    // init variables
    double sum_log_kDist = 0;
    double sum_log_k_kDist = 0;
    double[] log_kDist = new double[k_max - k_0];

    for (int i = 0; i < k_max - k_0; i++) {
      double dist = knnDistances.get(i + k_0).getDoubleValue();
      log_kDist[i] = Math.log(dist);
      sum_log_kDist += log_kDist[i];
      sum_log_k_kDist += log_kDist[i] * log_k[i];
    }

    double[] log_k = new double[k_max - k_0];
    System.arraycopy(this.log_k, k_0, log_k, 0, k_max - k_0);
    double sum_log_k = 0;
    double sum_log_k2 = 0;
    for (int i = 0; i < log_k.length; i++) {
      sum_log_k += log_k[i];
      sum_log_k += log_k[i] * log_k[i];
    }

    // lower and upper hull
    ConvexHull convexHull = new ConvexHull(log_k, log_kDist);

    // approximate lower hull
    ApproximationLine conservative = approximateUpperHull(convexHull, log_k, sum_log_k, sum_log_k2,
                                                          log_kDist, sum_log_kDist, sum_log_k_kDist);
    // approximate upper hull
    ApproximationLine progressive = approximateLowerHull(convexHull, log_k, sum_log_k, sum_log_k2,
                                                         log_kDist, sum_log_kDist, sum_log_k_kDist);

    entry.setConservativeKnnDistanceApproximation(conservative);
    entry.setProgressiveKnnDistanceApproximation(progressive);
  }

  /**
   * Initilaizes the log_k values.
   */
  private void init() {
    log_k = new double[k_max];
    for (int k = 1; k <= k_max; k++) {
      log_k[k - 1] = Math.log(k);
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
  private ApproximationLine approximateLowerHull(ConvexHull convexHull,
                                                 double[] log_k, double sum_log_k, double sum_log_k2,
                                                 double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuffer msg = new StringBuffer();
    int[] lowerHull = convexHull.getLowerHull();
    int l = convexHull.getNumberOfPointsInLowerHull();
    int k_0 = k_max - lowerHull.length + 1;

    // linear search on all line segments on the lower convex hull
    msg.append("lower hull l = " + l + "\n");
    double low_error = Double.MAX_VALUE;
    double low_m = 0.0;
    double low_t = 0.0;

    for (int i = 1; i < l; i++) {
      double cur_m = (log_kDist[lowerHull[i]] - log_kDist[lowerHull[i - 1]]) / (log_k[lowerHull[i]] - log_k[lowerHull[i - 1]]);
      double cur_t = log_kDist[lowerHull[i]] - cur_m * log_k[lowerHull[i]];
      double cur_error = ssqerr(k_0, k_max, log_k, log_kDist, cur_m, cur_t);
      msg.append("  Segment = " + i + " m = " + cur_m + " t = " + cur_t + " lowerror = " + cur_error + "\n");
      if (cur_error < low_error) {
        low_error = cur_error;
        low_m = cur_m;
        low_t = cur_t;
      }
    }
    ApproximationLine lowerApproximation = new ApproximationLine(k_0, low_m, low_t);

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
        msg.append("ERROR lower: The bisection search will not work properly !");
      if (!(i < l - 1 && log_kDist[lowerHull[i + 1]] < log_kDist[lowerHull[i]] + cur_m * (log_k[lowerHull[i + 1]] - log_k[lowerHull[i]])))
        is_right = false;
    }

    lowerApproximation = new ApproximationLine(k_0, low_m, low_t);
    return lowerApproximation;
  }

  private ApproximationLine approximateUpperHull(ConvexHull convexHull,
                                                 double[] log_k, double sum_log_k, double sum_log_k2,
                                                 double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    StringBuffer msg = new StringBuffer();
    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = k_max - upperHull.length + 1;

    // linear search on all line segments on the upper convex hull
    msg.append("upper hull:").append(u);
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
        msg.append("ERROR upper: The bisection search will not work properly !");
      if (!(i < u - 1 && log_kDist[upperHull[i + 1]] > log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]])))
        is_left = false;
    }

    ApproximationLine upperApproximation = new ApproximationLine(k_0, upp_m, upp_t);
    return upperApproximation;
  }
}
