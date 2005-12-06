package de.lmu.ifi.dbs.index.metrical.mtree.mkcop;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.TreePath;
import de.lmu.ifi.dbs.index.TreePathComponent;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.mkmax.MkMaxTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtree.util.PQNode;
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
public class MkCoPTree<O extends MetricalObject, D extends NumberDistance<D>> extends MTree<O, D> {
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
    init(new MkMaxTreeHeader(), fileName, cacheSize);
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
                   DistanceFunction<O, D> distanceFunction, int kmax,
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
                   DistanceFunction<O, D> distanceFunction, int kmax) {
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
    throw new UnsupportedOperationException("Insertion of dingle objects is not supported!");
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
    if (k > this.k_max) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " +
                                         "parameter kmax of the MCop-Tree!");
    }

    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    List<Integer> candidates = new ArrayList<Integer>();
    doReverseKNNQuery(k, object.getID(), result, candidates);

    // refinement of candidates
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
    for (Integer id : candidates)
      knnLists.put(id, new KNNList<D>(k, distanceFunction.infiniteDistance()));
    batchNN(getRoot(), candidates, knnLists);

    Collections.sort(result);
    Collections.sort(candidates);

    rkNNStatistics.numberCandidates += candidates.size();
    rkNNStatistics.numberTrueHits += result.size();

    for (Integer id : candidates) {
      List<QueryResult<D>> knns = knnLists.get(id).toList();
      for (QueryResult<D> qr : knns) {
        if (qr.getID() == object.getID()) {
          result.add(new QueryResult<D>(id, qr.getDistance()));
          break;
        }
      }

    }
    Collections.sort(result);

    rkNNStatistics.numberResults += result.size();
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

    MTreeNode<O, D> node = getRoot();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        MTreeDirectoryEntry entry = (MTreeDirectoryEntry) node.getEntry(0);
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
//        MkCoPLeafEntry e = (MkCoPLeafEntry) id;
//        System.out.println(counter++ + " Object " + e.getObjectID());
//        System.out.println("  pd  = " + e.getParentDistance());
//        System.out.println("  consApprox  = " + Arrays.asList(e.getConservativeKnnDistanceApproximation()));
//        System.out.println("  progrApprox = " + Arrays.asList(e.getProgressiveKnnDistanceApproximation()));
      }
      else {
        node = file.readPage(id.value());
//        System.out.println(node + ", numEntries = " + node.getNumEntries());

        if (id instanceof MTreeDirectoryEntry) {
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
  protected MTreeNode<O, D> createEmptyRoot() {
    return new MkCoPTreeNode<O, D>(file, leafCapacity, true);
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
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 10) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + consApprox + progrApprox) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize + 2 * 10) + 1;

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
                                 List<QueryResult<D>> result,
                                 List<Integer> candidates) {

    final Heap<D, Identifiable> pq = new DefaultHeap<D, Identifiable>();

    // push root
    pq.addNode(new PQNode<D>(distanceFunction.nullDistance(), ROOT_NODE_ID.value(), null));

    // search in tree
    while (!pq.isEmpty()) {
      PQNode<D> pqNode = (PQNode<D>) pq.getMinNode();

      MkCoPTreeNode<O, D> node = (MkCoPTreeNode<O, D>) getNode(pqNode.getValue().getID());

      // directory node
      if (! node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkCoPDirectoryEntry<D> entry = (MkCoPDirectoryEntry<D>) node.getEntry(i);
          D distance = distanceFunction.distance(entry.getObjectID(), q);
//          System.out.println("\nro " + entry.getObjectID());
//          System.out.println("cr " + entry.getCoveringRadius());
//          System.out.println("distance(" + entry.getObjectID()+","+q+") = " +distance);
          D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                      distanceFunction.nullDistance() :
                      distance.minus(entry.getCoveringRadius());
          D approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k, distanceFunction);
//          System.out.println("minDist(n_" + entry.getNodeID()+","+q+") = " +minDist);
//          System.out.println("approximatedKnnDist_cons(n_" + entry.getNodeID() + ") = " +approximatedKnnDist_cons);

          if (minDist.compareTo(approximatedKnnDist_cons) <= 0)
            pq.addNode(new PQNode<D>(minDist, entry.getNodeID(), entry.getObjectID()));
        }
      }
      // data node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkCoPLeafEntry<D> entry = (MkCoPLeafEntry<D>) node.getEntry(i);
          D distance = distanceFunction.distance(entry.getObjectID(), q);
          D approximatedKnnDist_prog = entry.approximateProgressiveKnnDistance(k, distanceFunction);

          if (distance.compareTo(approximatedKnnDist_prog) <= 0) {
            result.add(new QueryResult<D>(entry.getObjectID(), distance));
//            System.out.println("\nObject " + entry.getObjectID() + " - " + q);
//            KNNList<DoubleDistance> knn = new KNNList<DoubleDistance>(k, distanceFunction.infiniteDistance());
//            doKNNQuery(entry.getObjectID(), knn);
//            System.out.println("  knns " + knn);
//            System.out.println("  prog " + approximatedKnnDist_prog);
//            System.out.println("  dist " + distance);
          }
          else {
            NumberDistance approximatedKnnDist_cons = entry.approximateConservativeKnnDistance(k, distanceFunction);
            double diff = distance.getDoubleValue() - approximatedKnnDist_cons.getDoubleValue();
//            if (distance.compareTo(approximatedKnnDist_cons) <= 0)
            if (diff <= 0.0000000001) {
              candidates.add(entry.getObjectID());
//              System.out.println("\nObject " + entry.getObjectID() + " - " + q);
//              KNNList knn = new KNNList(k, distanceFunction.infiniteDistance());
//              doKNNQuery(entry.getObjectID(), knn);
//              System.out.println("  knns " + knn);
//              System.out.println("  prog " + approximatedKnnDist_prog);
//              System.out.println("  cons " + approximatedKnnDist_cons);
//              System.out.println("  dist " + distance);
            }

          }
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  protected void test(Identifier rootID) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs = new BreadthFirstEnumeration<MTreeNode<O, D>>(file, rootID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MkCoPTreeNode<O, D> node = (MkCoPTreeNode<O, D>) getNode(id.value());
        node.test();

        if (id instanceof MTreeEntry) {
          MkCoPDirectoryEntry<D> e = (MkCoPDirectoryEntry<D>) id;
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
  protected void test(Map<Integer, KNNList<D>> knnLists) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs =
    new BreadthFirstEnumeration<MTreeNode<O, D>>(file, ROOT_NODE_ID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MkCoPTreeNode<O, D> node = (MkCoPTreeNode<O, D>) getNode(id.value());
        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkCoPLeafEntry<D> entry = (MkCoPLeafEntry<D>) node.getEntry(i);
            List<D> knnDistances = getKNNList(entry.getObjectID(), knnLists);

            for (int k = 1; k <= k_max; k++) {
              D knnDist_cons = entry.approximateConservativeKnnDistance(k, distanceFunction);
              D knnDist_prog = entry.approximateProgressiveKnnDistance(k, distanceFunction);
              D knnDist_soll = knnDistances.get(k - 1);

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
                  System.out.println(Math.abs(knnDist_soll.getDoubleValue() - knnDist_cons.getDoubleValue()));

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
            // todo
//            testKNNDistances(node, entry, knnDistances);
          }
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(TreePath<MkCoPTreeNode<O, D>> path, MkCoPLeafEntry entry, List<D> knnDistances) {
    MkCoPTreeNode<O, D> node = path.getLastPathComponent().getNode();
    ApproximationLine knnDistances_node = node.conservativeKnnDistanceApproximation(k_max);

    for (int k = 1; k <= k_max; k++) {
      D knnDistance_node = knnDistances_node.getApproximatedKnnDistance(k, distanceFunction);
      D knnDistance = knnDistances.get(k - 1);

//      String msg1 = "\nknnDistance[" + k + "] -- knnDistance_node[" + k + "] \n" +
//                    knnDistance + " -- " + knnDistance_node + "\n" +
//                    "in " + node + " (entry " + entry.getObjectID() + ")";
//      System.out.println(msg1);

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
      testKNNDistances(path.getParentPath(), entry, knnDistances);
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkCoPDirectoryEntry<D> rootID) {
    MkCoPTreeNode<O, D> node = (MkCoPTreeNode<O, D>) getNode(rootID.value());
    ApproximationLine knnDistances_soll_cons = node.conservativeKnnDistanceApproximation(k_max);

    for (int k = 1; k <= k_max; k++) {
      D knnDist_ist_cons = rootID.approximateConservativeKnnDistance(k, distanceFunction);
      D knnDist_soll_cons = knnDistances_soll_cons.getApproximatedKnnDistance(k, distanceFunction);

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
   * Splits the last node in the specified path and returns a path
   * containing at last element the parent of the newly created split node.
   *
   * @param path the path containing at last element the node to be splitted
   * @return a path containing at last element the parent of the newly created split node
   */
  private TreePath<MTreeNode<O, D>> split(TreePath<MTreeNode<O, D>> path) {
    MkCoPTreeNode<O, D> node = (MkCoPTreeNode<O, D>) path.getLastPathComponent().getNode();
    Integer nodeIndex = path.getLastPathComponent().getIndex();

    // determine routing object in parent
    Integer routingObjectID = null;
    if (path.getPathCount() > 1) {
      MkCoPTreeNode<O, D> parent = (MkCoPTreeNode<O, D>) path.getParentPath().getLastPathComponent().getNode();
      routingObjectID = parent.getEntry(nodeIndex).getObjectID();
    }

    // do split
    Split<D> split = new MLBDistSplit<O, D>(node, routingObjectID, distanceFunction);
    MkCoPTreeNode<O, D> newNode = (MkCoPTreeNode<O, D>) node.splitEntries(split.assignmentsToFirst, split.assignmentsToSecond);
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
    MTreeNode<O, D> parent = path.getParentPath().getLastPathComponent().getNode();
    Integer parentIndex = path.getParentPath().getLastPathComponent().getIndex();
    MTreeNode<O, D> grandParent;
    D parentDistance1 = null, parentDistance2 = null;
    if (parent.getID() != ROOT_NODE_ID.value()) {
      grandParent = path.getParentPath().getParentPath().getLastPathComponent().getNode();
      Integer parentObject = grandParent.getEntry(parentIndex).getObjectID();
      parentDistance1 = distanceFunction.distance(split.firstPromoted, parentObject);
      parentDistance2 = distanceFunction.distance(split.secondPromoted, parentObject);
    }

    // add the newNode to parent
    parent.addDirectoryEntry(new MkCoPDirectoryEntry<D>(split.secondPromoted,
                                                        parentDistance2,
                                                        newNode.getNodeID(),
                                                        split.secondCoveringRadius,
                                                        newNode.conservativeKnnDistanceApproximation(k_max)));

    // set the first promotion object, parentDistance, covering radius
    // and knn distance approximation for node in parent
    MkCoPDirectoryEntry<D> entry1 = (MkCoPDirectoryEntry<D>) parent.getEntry(nodeIndex);
    entry1.setObjectID(split.firstPromoted);
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(split.firstCoveringRadius);
    entry1.setConservativeKnnDistanceApproximation(node.conservativeKnnDistanceApproximation(k_max));

    // adjust the parentDistances in node
    for (int i = 0; i < node.getNumEntries(); i++) {
      D distance = distanceFunction.distance(split.firstPromoted, node.getEntry(i).getObjectID());
      node.getEntry(i).setParentDistance(distance);
    }

    // adjust the parentDistances in newNode
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(split.secondPromoted, newNode.getEntry(i).getObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }

    // write changes in parent to file
    file.writePage(parent);

    return path.getParentPath();
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
  private TreePath<MTreeNode<O, D>>  createNewRoot(final MkCoPTreeNode<O, D> oldRoot,
                                            final MkCoPTreeNode<O, D> newNode,
                                            Integer firstPromoted, Integer secondPromoted,
                                            D firstCoveringRadius, D secondCoveringRadius) {
    // create new root
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");
    MkCoPTreeNode<O, D> root = new MkCoPTreeNode<O, D>(file, dirCapacity, false);
    file.writePage(root);

    // change id in old root and set id in new root
    oldRoot.setID(root.getID());
    root.setID(ROOT_NODE_ID.value());

    // add entries to new root
    root.addDirectoryEntry(new MkCoPDirectoryEntry<D>(firstPromoted,
                                                      null,
                                                      oldRoot.getNodeID(),
                                                      firstCoveringRadius,
                                                      oldRoot.conservativeKnnDistanceApproximation(k_max)));

    root.addDirectoryEntry(new MkCoPDirectoryEntry<D>(secondPromoted,
                                                      null,
                                                      newNode.getNodeID(),
                                                      secondCoveringRadius,
                                                      newNode.conservativeKnnDistanceApproximation(k_max)));

    // adjust the parentDistances
    for (int i = 0; i < oldRoot.getNumEntries(); i++) {
      D distance = distanceFunction.distance(firstPromoted, oldRoot.getEntry(i).getObjectID());
      oldRoot.getEntry(i).setParentDistance(distance);
    }
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(secondPromoted, newNode.getEntry(i).getObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }
    msg.append("firstCoveringRadius ").append(firstCoveringRadius).append("\n");
    msg.append("secondCoveringRadius ").append(secondCoveringRadius).append("\n");

    // write the changes
    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    msg.append("New Root-ID ").append(root.getNodeID()).append("\n");

    logger.info(msg.toString());

    return new TreePath<MTreeNode<O, D>>(new TreePathComponent<MTreeNode<O, D>>(root, null));
  }

  /**
   * Inserts the specified objects into this MDkNNTree-Tree.
   *
   * @param objects the object to be inserted
   */
  private void insert(List<O> objects) {
    logger.info("insert " + objects + "\n");

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    // insert first
    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(k_max + 1, distanceFunction.infiniteDistance()));

      // find insertion path
      TreePath<MTreeNode<O, D>> rootPath = new TreePath<MTreeNode<O, D>>(new TreePathComponent<MTreeNode<O, D>>(getRoot(), null));
      TreePath<MTreeNode<O, D>> path = findInsertionPath(object.getID(), rootPath);

      // determine parent distance
      MTreeNode<O, D> node = path.getLastPathComponent().getNode();
      D parentDistance = null;
      if (path.getPathCount() > 1) {
        MTreeNode<O, D> parent = path.getParentPath().getLastPathComponent().getNode();
        Integer index = path.getLastPathComponent().getIndex();
        parentDistance = distanceFunction.distance(object.getID(), parent.getEntry(index).getObjectID());
      }

      // add the object
      MkCoPLeafEntry<D> newEntry = new MkCoPLeafEntry<D>(object.getID(),
                                                         parentDistance,
                                                         new ApproximationLine(k_max, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                                                         new ApproximationLine(0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
      node.addLeafEntry(newEntry);

     // split the node if necessary
      while (hasOverflow(path)) {
        path = split(path);
      }
    }

    // do batch nn
    logger.info("batch nn");
    MkCoPTreeNode<O, D> root = (MkCoPTreeNode<O, D>) getRoot();
    batchNN(root, ids, knnLists);

    // adjust the knn distances
    logger.info("adjust the knn distances");
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkCoPEntry<D> entry = (MkCoPEntry<D>) root.getEntry(i);
      batchApproximateKNNDistances(entry, knnLists);
    }

    // test
    test(knnLists);
    test(ROOT_NODE_ID);
  }

  private List<D> getKNNList(Integer id, Map<Integer, KNNList<D>> knnLists) {
    KNNList<D> knns = knnLists.get(id);
    List<D> result = knns.distancesToList();
//    result.remove(0);
    return result;
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchApproximateKNNDistances(MkCoPEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      approximateKnnDistances((MkCoPLeafEntry<D>) entry, getKNNList(entry.getObjectID(), knnLists));
//      System.out.println("batchApproximateKNNDistances object " + entry.getObjectID() + ": " + entry.getConservativeKnnDistanceApproximation());
      return;
    }

    MkCoPTreeNode<O, D> node = (MkCoPTreeNode<O, D>) getNode(((MkCoPDirectoryEntry) entry).getNodeID());
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPLeafEntry<D> e = (MkCoPLeafEntry<D>) node.getEntry(i);
        approximateKnnDistances(e, getKNNList(e.getObjectID(), knnLists));
//        System.out.println("batchApproximateKNNDistances object " + e.getObjectID() + ": " + e.getConservativeKnnDistanceApproximation());
      }
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkCoPDirectoryEntry<D> e = (MkCoPDirectoryEntry<D>) node.getEntry(i);
        batchApproximateKNNDistances(e, knnLists);
//        System.out.println("batchApproximateKNNDistances node " + e.getNodeID() + ": " + e.getConservativeKnnDistanceApproximation());
      }
    }
    ApproximationLine approx = node.conservativeKnnDistanceApproximation(k_max);
//    System.out.println(node.getID() + " approx " + approx);
    entry.setConservativeKnnDistanceApproximation(approx);
  }

  /*
  * auxiliary function for approxKdist methods.
  */
  private double ssqerr(int k0, int kmax, double[] logk, double [] log_kDist, double m, double t) {
    int k = kmax - k0;
    double result = 0;
    for (int i = 0; i < k; i++) {
//      double h = log_kDist[i] - (m * (logk[i] - logk[0]) + t);  ???
      double h = log_kDist[i] - m * logk[i] - t;
      result += h * h;
    }
    return result;
  }

  /*
   * auxiliary function for approxKdist methods.
   */
  private double optimize(int k0, int kmax, double sumx, double sumx2, double xp, double yp, double sumxy, double sumy) {
    int k = kmax - k0 + 1;
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
  private void approximateKnnDistances(MkCoPLeafEntry entry, List<D> knnDistances) {
    // count the zero distances
    int k_0 = 0;
    for (int i = 0; i < k_max; i++) {
      double dist = knnDistances.get(i).getDoubleValue();
      if (dist == 0) k_0++;
      else
        break;
    }

    // init variables
    double[] log_k = new double[k_max - k_0];
    System.arraycopy(this.log_k, k_0, log_k, 0, k_max - k_0);

    double sum_log_kDist = 0;
    double sum_log_k_kDist = 0;
    double[] log_kDist = new double[k_max - k_0];

    for (int i = 0; i < k_max - k_0; i++) {
      double dist = knnDistances.get(i + k_0).getDoubleValue();
      log_kDist[i] = Math.log(dist);
      sum_log_kDist += log_kDist[i];
      sum_log_k_kDist += log_kDist[i] * log_k[i];
    }

    double sum_log_k = 0;
    double sum_log_k2 = 0;
    for (int i = 0; i < log_k.length; i++) {
      sum_log_k += log_k[i];
      sum_log_k2 += (log_k[i] * log_k[i]);
    }

//    System.out.println("k_0 " + k_0);
//    System.out.println("k_max " + k_max);
//    System.out.println("log_k("+log_k.length+") " + Util.format(log_k));
//    System.out.println("sum_log_k " + sum_log_k);
//    System.out.println("sum_log_k^2 " + sum_log_k2);
//    System.out.println("kDists " + knnDistances);
//    System.out.println("log_kDist(" + log_kDist.length + ") " + Util.format(log_kDist));
//    System.out.println("sum_log_kDist " + sum_log_kDist);
//    System.out.println("sum_log_k_kDist " + sum_log_k_kDist);

    // lower and upper hull
    ConvexHull convexHull = new ConvexHull(log_k, log_kDist);

    // approximate upper hull
    ApproximationLine conservative = approximateUpperHull(convexHull, log_k, sum_log_k, sum_log_k2,
                                                          log_kDist, sum_log_kDist, sum_log_k_kDist);

    ApproximationLine c2 = approximateUpperHull_PAPER(convexHull, log_k, sum_log_k, sum_log_k2, log_kDist,
                                                      sum_log_kDist, sum_log_k_kDist);

    double err1 = ssqerr(k_0, k_max, log_k, log_kDist, conservative.getM(), conservative.getT());
    double err2 = ssqerr(k_0, k_max, log_k, log_kDist, c2.getM(), c2.getT());

//    System.out.println("err1 " + err1);
//    System.out.println("err2 " + err2);

    if (err1 > err2 && err1 - err2 > 0.000000001 && false) {
//    if (err1 > err2) {
      int u = convexHull.getNumberOfPointsInUpperHull();
      int[] upperHull = convexHull.getUpperHull();
      System.out.println("");
      System.out.println("entry " + entry.getObjectID());
      System.out.println("lower Hull " + convexHull.getNumberOfPointsInLowerHull() + " " + Util.format(convexHull.getLowerHull()));
      System.out.println("upper Hull " + convexHull.getNumberOfPointsInUpperHull() + " " + Util.format(convexHull.getUpperHull()));
      System.out.println("err1 " + err1);
      System.out.println("err2 " + err2);
      System.out.println("conservative1 " + conservative);
      System.out.println("conservative2 " + c2);

      for (int i = 0; i < u; i++) {
        System.out.println("log_k[" + upperHull[i] + "] = " + log_k[upperHull[i]]);
        System.out.println("log_kDist[" + upperHull[i] + "] = " + log_kDist[upperHull[i]]);
      }
//      if (entry.getObjectID() == 153) System.exit(1);
    }

    // approximate lower hull
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

    ApproximationLine lowerApproximation = new ApproximationLine(k_0, low_m, low_t);
    return lowerApproximation;
  }

  private ApproximationLine approximateUpperHull(ConvexHull convexHull,
                                                 double[] log_k, double sum_log_k, double sum_log_k2,
                                                 double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();
    int k_0 = k_max - upperHull.length + 1;

    ApproximationLine approx = null;
    double error = Double.POSITIVE_INFINITY;
    for (int i = 0; i < u - 1; i++) {
      int ii = upperHull[i];
      int jj = upperHull[i + 1];
      double current_m = (log_kDist[jj] - log_kDist[ii]) / (log_k[jj] - log_k[ii]);
      double current_t = log_kDist[ii] - current_m * log_k[ii];
      ApproximationLine current_approx = new ApproximationLine(k_0, current_m, current_t);

      boolean ok = true;
      double currentError = 0;
      for (int k = k_0; k <= k_max; k++) {
        double appDist = current_approx.getValueAt(k);
        if (appDist < log_kDist[k - k_0] && log_kDist[k - k_0] - appDist > 0.000000001) {
          ok = false;
          break;
        }
        currentError += (appDist - log_kDist[k - k_0]);
      }
//        System.out.println("  " + i + "-" + j + "  " + ok + " ce " + currentError + " e " + error);
      if (ok && currentError < error) {
        approx = current_approx;
        error = currentError;
//          System.out.println("i = "+ i + " j = " + j);
      }
    }
//      System.out.println("upper Approx " + approx);
    return approx;
  }

  private ApproximationLine approximateUpperHull_PAPER(ConvexHull convexHull,
                                                       double[] log_k, double sum_log_k, double sum_log_k2,
                                                       double[] log_kDist, double sum_log_kDist, double sum_log_k_kDist) {
    int[] upperHull = convexHull.getUpperHull();
    int u = convexHull.getNumberOfPointsInUpperHull();

    List<Integer> marked = new ArrayList<Integer>();

    int k_0 = k_max - upperHull.length + 1;

    int a = u / 2;
//    System.out.println("");
    while (marked.size() != u) {
      marked.add(a);
      double x_a = log_k[upperHull[a]];
      double y_a = log_kDist[upperHull[a]];

      double m_a = optimize(k_0, k_max, sum_log_k, sum_log_k2, x_a, y_a, sum_log_k_kDist, sum_log_kDist);
      double t_a = y_a - m_a * x_a;

//      System.out.println("a=" + a + " m_a="+m_a + ", t_a=" + t_a);
//      System.out.println("            err " + ssqerr(k_0, k_max, log_k, log_kDist, m_a, m_a));

      double x_p = a == 0 ? Double.NaN : log_k[upperHull[a - 1]];
      double y_p = a == 0 ? Double.NaN : log_kDist[upperHull[a - 1]];
      double x_s = a == u ? Double.NaN : log_k[upperHull[a + 1]];
      double y_s = a == u ? Double.NaN : log_kDist[upperHull[a + 1]];

      boolean lessThanPre = a == 0 || y_p <= m_a * x_p + t_a;
      boolean lessThanSuc = a == u || y_s <= m_a * x_s + t_a;

      if (lessThanPre && lessThanSuc) {
        ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);
//        System.out.println("1 anchor = " + a);
        return appr;
      }

      else if (! lessThanPre) {
        if (marked.contains(a - 1)) {
          m_a = (y_a - y_p) / (x_a - x_p);
          if (y_a == y_p) m_a = 0;
          t_a = y_a - m_a * x_a;

          ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);
//          System.out.println("2 anchor = " + a);
//          System.out.println("appr1 " + appr);
//          System.out.println("x_a " + x_a + ", y_a " + y_a);
//          System.out.println("x_p " + x_p + ", y_p " + y_p);
//          System.out.println("a " + a);
//          System.out.println("upperHull " + Util.format(upperHull));
          return appr;
        }
        else a = a - 1;
      }
      else {
        if (marked.contains(a + 1)) {
          m_a = (y_a - y_s) / (x_a - x_s);
          if (y_a == y_p) m_a = 0;
          t_a = y_a - m_a * x_a;
//          System.out.println("3 anchor = " + a + " -- " + (a+1));
          ApproximationLine appr = new ApproximationLine(k_0, m_a, t_a);
//          System.out.println("appr2 " + appr);
          return appr;
        }
        else a = a + 1;
      }
    }
//    System.out.println("snh");
    return null;
  }

  private ApproximationLine approximateUpperHull_OLD(ConvexHull convexHull,
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
      {
//        System.out.println("ERROR upper: The bisection search will not work properly !");
//        System.out.println(Util.format(log_kDist));
      }
      if (!(i < u - 1 && log_kDist[upperHull[i + 1]] > log_kDist[upperHull[i]] + cur_m * (log_k[upperHull[i + 1]] - log_k[upperHull[i]])))
        is_left = false;
    }

    ApproximationLine upperApproximation = new ApproximationLine(k_0, upp_m, upp_t);
    return upperApproximation;
  }
}
