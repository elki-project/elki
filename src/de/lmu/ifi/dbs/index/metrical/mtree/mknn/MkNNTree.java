package de.lmu.ifi.dbs.index.metrical.mtree.mknn;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.TreePath;
import de.lmu.ifi.dbs.index.TreePathComponent;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.util.DistanceEntry;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;

/**
 * MkNNTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries.
 * The k-nn distance is stored in each entry of a node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkNNTree<O extends MetricalObject, D extends Distance<D>> extends MTree<O, D> {
  /**
   * The parameter k.
   */
  private int k;

  /**
   * Provides some statistics about performed reverse knn-queries.
   */
  private ReversekNNStatistic rkNNStatistics = new ReversekNNStatistic();

  /**
   * Creates a new MDkNNTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the MTree
   * @param cacheSize the size of the cache in bytes
   */
  public MkNNTree(String fileName, int cacheSize) {
    init(new MkNNTreeHeader(), fileName, cacheSize);
  }

  /**
   * Creates a new MDkNNTree with the specified parameters.
   *
   * @param fileName         the name of the file for storing the entries,
   *                         if this parameter is null all entries will be hold in
   *                         main memory
   * @param pageSize         the size of a page in Bytes
   * @param cacheSize        the size of the cache in Bytes
   * @param distanceFunction the distance function
   * @param k                the parametr k of the knn distance to be stored
   * @param objects
   */
  public MkNNTree(String fileName, int pageSize, int cacheSize,
                  DistanceFunction<O, D> distanceFunction, int k,
                  List<O> objects) {
    super();
    this.k = k;
    init(fileName, pageSize, cacheSize, distanceFunction);

    insert(objects);
  }

  /**
   * Creates a new MDkNNTree with the specified parameters.
   *
   * @param fileName         the name of the file for storing the entries,
   *                         if this parameter is null all entries will be hold in
   *                         main memory
   * @param pageSize         the size of a page in Bytes
   * @param cacheSize        the size of the cache in Bytes
   * @param distanceFunction the distance function
   * @param k                the parametr k of the knn distance to be stored
   */
  public MkNNTree(String fileName, int pageSize, int cacheSize,
                  DistanceFunction<O, D> distanceFunction, int k) {
    super();
    this.k = k;
    init(fileName, pageSize, cacheSize, distanceFunction);
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    logger.info("insert " + object.getID() + " " + object + "\n");

    // find insertion path
    TreePath<MTreeNode<O, D>> rootPath = new TreePath<MTreeNode<O, D>>(new TreePathComponent<MTreeNode<O, D>>(getRoot(), null));
    TreePath<MTreeNode<O, D>> path = findInsertionPath(object.getID(), rootPath);
    MkNNTreeNode<O, D> node = (MkNNTreeNode<O, D>) path.getLastPathComponent().getNode();

    // determine parent distance
    D parentDistance = null;
    if (path.getPathCount() > 1) {
      MTreeNode<O, D> parent = path.getParentPath().getLastPathComponent().getNode();
      Integer index = path.getLastPathComponent().getIndex();
      parentDistance = distanceFunction.distance(object.getID(), parent.getEntry(index).getObjectID());
    }

    // do preInsert
    KNNList<D> knns = new KNNList<D>(k, distanceFunction.infiniteDistance());
    MkNNTreeNode<O, D> root = (MkNNTreeNode<O, D>) getRoot();
    MkNNLeafEntry<D> newEntry = new MkNNLeafEntry<D>(object.getID(), parentDistance,
                                                     distanceFunction.undefinedDistance());
    knns.add(new QueryResult<D>(object.getID(), distanceFunction.nullDistance()));
    preInsert(newEntry, root, knns);

    // add the object
    node.addLeafEntry(newEntry);

    // adjust knn distances for path of q
    D knnDist = newEntry.getKnnDistance();
    TreePath<MTreeNode<O, D>> currentPath = path;
    while (currentPath.getPathCount() > 1) {
      node = (MkNNTreeNode<O, D>) currentPath.getLastPathComponent().getNode();
      Integer nodeIndex = currentPath.getLastPathComponent().getIndex();
      MkNNTreeNode<O, D> parent = (MkNNTreeNode<O, D>) currentPath.getParentPath().getLastPathComponent().getNode();

      MkNNDirectoryEntry<D> entry = (MkNNDirectoryEntry<D>) parent.getEntry(nodeIndex);
      if (entry.getKnnDistance().compareTo(knnDist) < 0) {
        entry.setKnnDistance(knnDist);
        currentPath = currentPath.getParentPath();
        knnDist = node.kNNDistance(distanceFunction);
      }
      else
        break;
    }

    // do split if necessary
    while (hasOverflow(path)) {
      path = split(path);
    }

    // test
    test(ROOT_NODE_ID);
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
    if (k > this.k) {
      throw new IllegalArgumentException("Parameter k has to be equal or less than " +
                                         "parameter k of the MDkNN-Tree!");
    }

    MkNNTreeNode<O, D> root = (MkNNTreeNode<O, D>) getRoot();
    List<QueryResult<D>> candidates = new ArrayList<QueryResult<D>>();

    doReverseKNNQuery(object.getID(), null, root, candidates);

    if (k == this.k) {
      Collections.sort(candidates);
      rkNNStatistics.numberResults += candidates.size();
      return candidates;
    }

    // refinement of candidates
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
    List<Integer> candidateIDs = new ArrayList<Integer>();
    for (QueryResult<D> candidate : candidates) {
      KNNList<D> knns = new KNNList<D>(k, distanceFunction.infiniteDistance());
      knnLists.put(candidate.getID(), knns);
      candidateIDs.add(candidate.getID());
    }
    batchNN(getRoot(), candidateIDs, knnLists);

    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    for (Integer id : candidateIDs) {
      List<QueryResult<D>> knns = knnLists.get(id).toList();
      for (QueryResult<D> qr : knns) {
        if (qr.getID() == object.getID()) {
          result.add(new QueryResult<D>(id, qr.getDistance()));
          break;
        }
      }
    }

    rkNNStatistics.numberResults += result.size();
    rkNNStatistics.numberCandidates += candidates.size();
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

    MTreeNode<O, D> node = getRoot();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        DirectoryEntry<D> entry = (DirectoryEntry<D>) node.getEntry(0);
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
//        System.out.println("  knn = " + ((MkNNLeafEntry<D>) id).getKnnDistance());
      }
      else {
        node = file.readPage(id.value());
//        System.out.println(node + ", numEntries = " + node.getNumEntries());

        if (id instanceof DirectoryEntry) {
//          DirectoryEntry e = (DirectoryEntry) id;
//          System.out.println("  r_obj = " + e.getObjectID());
//          System.out.println("  pd = " + e.getParentDistance());
//          System.out.println("  cr = " + ((MkNNDirectoryEntry<D>) id).getCoveringRadius());
//          System.out.println("  knn = " + ((MkNNDirectoryEntry<D>) id).getKnnDistance());
        }

        if (node.isLeaf()) {
          leafNodes++;
//          for (int i = 0; i < node.getNumEntries(); i++) {
//            Entry e = node.getEntry(i);
//
//            if (e.getObjectID() == 351) {
//              System.out.print("351 --> node " + node.getNodeID());
//              MkNNTreeNode<O, D> n = (MkNNTreeNode<O, D>) node;
//              while (n.getParentID() != null) {
//                MkNNTreeNode<O, D> parent = (MkNNTreeNode<O, D>) getNode(n.getParentID());
//                System.out.print(" -> " + parent.getNodeID());
//                n = parent;
//              }
//              System.out.println("");
//            }
//          }
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
  public ReversekNNStatistic getRkNNStatistics() {
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
  protected MkNNTreeNode<O, D> createEmptyRoot() {
    return new MkNNTreeNode<O, D>(file, leafCapacity, true);
  }

  /**
   * Creates a header for this M-Tree.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected MTreeHeader createHeader(int pageSize) {
    return new MkNNTreeHeader(pageSize, dirCapacity, leafCapacity, k);
  }

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param q
   * @param node_entry
   * @param node
   * @param result
   */
  private void doReverseKNNQuery(Integer q,
                                 MkNNDirectoryEntry<D> node_entry,
                                 MkNNTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {

//    System.out.println("NODE " + node);

    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkNNLeafEntry<D> entry = (MkNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance()) <= 0)
          result.add(new QueryResult<D>(entry.getObjectID(), distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkNNDirectoryEntry<D> entry = (MkNNDirectoryEntry<D>) node.getEntry(i);
        D node_knnDist = node_entry != null ?
                         node_entry.getKnnDistance() : distanceFunction.infiniteDistance();

        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                    distanceFunction.nullDistance() :
                    distance.minus(entry.getCoveringRadius());

//        System.out.println("  node " + node + " entry " + entry + " node_knnDist " + node_knnDist + " distance " + distance);

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkNNTreeNode<O, D> childNode = (MkNNTreeNode<O, D>) getNode(entry.getNodeID());
          doReverseKNNQuery(q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Adapts the knn distances.
   *
   * @param node
   * @param q
   * @param knns_q
   */
  private D preInsert(MkNNLeafEntry<D> q,
                      MkNNTreeNode<O, D> node,
                      KNNList<D> knns_q) {

    D maxDist = distanceFunction.nullDistance();

    D knnDist_q = knns_q.getKNNDistance();

    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkNNLeafEntry<D> p = (MkNNLeafEntry<D>) node.getEntry(i);
        D dist_pq = distanceFunction.distance(p.getObjectID(), q.getObjectID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist_pq.compareTo(knnDist_q) <= 0) {
          QueryResult<D> knn = new QueryResult<D>(p.getObjectID(), dist_pq);
          knns_q.add(knn);
          if (knns_q.size() >= k) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNList<D> knns_p = new KNNList<D>(k, distanceFunction.infiniteDistance());
          knns_p.add(new QueryResult<D>(q.getObjectID(), dist_pq));
          doKNNQuery(p.getObjectID(), knns_p);

          if (knns_p.size() < k)
            p.setKnnDistance(distanceFunction.undefinedDistance());
          else {
            D knnDist_p = knns_p.getMaximumDistance();
            p.setKnnDistance(knnDist_p);
          }
        }
        maxDist = Util.max(maxDist, p.getKnnDistance());
      }
    }
    // directory node
    else {
      List<DistanceEntry<D>> entries = getSortedEntries(node, q.getObjectID());
      for (DistanceEntry<D> distEntry : entries) {
        MkNNDirectoryEntry<D> entry = (MkNNDirectoryEntry<D>) distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if (distEntry.getDistance().compareTo(entry_knnDist) < 0 ||
            distEntry.getDistance().compareTo(knnDist_q) < 0) {
          MkNNTreeNode<O, D> childNode = (MkNNTreeNode<O, D>) getNode(entry.getNodeID());
          D entry_knnDist1 = preInsert(q, childNode, knns_q);
          entry.setKnnDistance(entry_knnDist1);
          knnDist_q = knns_q.getKNNDistance();
        }
        maxDist = Util.max(maxDist, entry.getKnnDistance());
      }
    }
    return maxDist;
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified object.
   *
   * @param node the node
   * @param q    the id of the object
   * @return a list of the sorted entries
   */
  private List<DistanceEntry<D>> getSortedEntries(MkNNTreeNode<O, D> node, Integer q) {
    List<DistanceEntry<D>> result = new ArrayList<DistanceEntry<D>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MkNNDirectoryEntry<D> entry = (MkNNDirectoryEntry<D>) node.getEntry(i);
      D distance = distanceFunction.distance(entry.getObjectID(), q);
      D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                  distanceFunction.nullDistance() :
                  distance.minus(entry.getCoveringRadius());

      result.add(new DistanceEntry<D>(entry, minDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified objects.
   *
   * @param node the node
   * @param ids  the ids of the objects
   * @return a list of the sorted entries
   */
  private List<DistanceEntry<D>> getSortedEntries(MkNNTreeNode<O, D> node, Integer[] ids) {
    List<DistanceEntry<D>> result = new ArrayList<DistanceEntry<D>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MkNNDirectoryEntry<D> entry = (MkNNDirectoryEntry<D>) node.getEntry(i);

      D minMinDist = distanceFunction.infiniteDistance();
      for (Integer q : ids) {
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                    distanceFunction.nullDistance() :
                    distance.minus(entry.getCoveringRadius());
        if (minDist.compareTo(minMinDist) < 0)
          minMinDist = minDist;
      }
      result.add(new DistanceEntry<D>(entry, minMinDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  protected void test(Identifier rootID) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs = new BreadthFirstEnumeration<MTreeNode<O, D>>(file, rootID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MkNNTreeNode<O, D> node = (MkNNTreeNode<O, D>) getNode(id.value());
        node.test();

        if (id instanceof Entry) {
          MkNNDirectoryEntry<D> e = (MkNNDirectoryEntry<D>) id;
          node.testParentDistance(e.getObjectID(), distanceFunction);
          testCR(e);
          testKNNDist(e);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }

        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkNNLeafEntry<D> entry = (MkNNLeafEntry<D>) node.getEntry(i);
            D knnDist_ist = entry.getKnnDistance();
            D knnDist_soll = knnDistance(entry.getObjectID(), k);

            if (! entry.getKnnDistance().equals(knnDist_soll)) {
              String msg = "\nknnDist_ist != knnDist_soll \n" +
                           knnDist_ist + " != " + knnDist_soll + "\n" +
                           "in " + node + " at entry " + entry;

              System.out.println(msg);
              throw new RuntimeException();

            }
          }
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDist(MkNNDirectoryEntry<D> rootID) {
    D knnDist_ist = rootID.getKnnDistance();

    MkNNTreeNode<O, D> node = (MkNNTreeNode<O, D>) getNode(rootID.value());
    D knnDist_soll = node.kNNDistance(distanceFunction);

    if (! knnDist_ist.equals(knnDist_soll)) {
      String msg = "\nknnDist_ist != knnDist_soll \n" +
                   knnDist_ist + " != " + knnDist_soll + "\n" +
                   "in " + node;

      System.out.println(msg);
      throw new RuntimeException(msg);
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
    MkNNTreeNode<O, D> node = (MkNNTreeNode<O, D>) path.getLastPathComponent().getNode();
    Integer nodeIndex = path.getLastPathComponent().getIndex();

    // determine routing object in parent
    Integer routingObjectID = null;
    if (path.getPathCount() > 1) {
      MTreeNode<O, D> parent = path.getParentPath().getLastPathComponent().getNode();
      routingObjectID = parent.getEntry(nodeIndex).getObjectID();
    }

    // do split
    Split<D> split = new MLBDistSplit<O, D>(node, routingObjectID, distanceFunction);
    MkNNTreeNode<O, D> newNode = (MkNNTreeNode<O, D>) node.splitEntries(split.assignmentsToFirst, split.assignmentsToSecond);
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
      return createNewRoot(node, newNode,
                           split.firstPromoted, split.secondPromoted,
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
    parent.addDirectoryEntry(new MkNNDirectoryEntry<D>(split.secondPromoted,
                                                       parentDistance2,
                                                       newNode.getNodeID(),
                                                       split.secondCoveringRadius,
                                                       newNode.kNNDistance(distanceFunction)));

    // set the first promotion object, parentDistance and covering radius for node in parent
    MkNNDirectoryEntry<D> entry1 = (MkNNDirectoryEntry<D>) parent.getEntry(nodeIndex);
    entry1.setObjectID(split.firstPromoted);
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(split.firstCoveringRadius);
    entry1.setKnnDistance(node.kNNDistance(distanceFunction));

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
  private TreePath<MTreeNode<O, D>> createNewRoot(final MkNNTreeNode<O, D> oldRoot,
                                                  final MkNNTreeNode<O, D> newNode,
                                                  Integer firstPromoted, Integer secondPromoted,
                                                  D firstCoveringRadius, D secondCoveringRadius) {
    // create new root
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");
    MkNNTreeNode<O, D> root = new MkNNTreeNode<O, D>(file, dirCapacity, false);
    file.writePage(root);

    // change id in old root and set id in new root
    oldRoot.setID(root.getID());
    root.setID(ROOT_NODE_ID.value());

    // add entries to new root
    root.addDirectoryEntry(new MkNNDirectoryEntry<D>(firstPromoted,
                                                     null,
                                                     oldRoot.getNodeID(),
                                                     firstCoveringRadius,
                                                     oldRoot.kNNDistance(distanceFunction)));

    root.addDirectoryEntry(new MkNNDirectoryEntry<D>(secondPromoted,
                                                     null,
                                                     newNode.getNodeID(),
                                                     secondCoveringRadius,
                                                     newNode.kNNDistance(distanceFunction)));

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
   * Returns the knn distance of the object with the specified id.
   *
   * @param objectID the id of the query object
   * @return the knn distance of the object with the specified id
   */
  private D knnDistance(Integer objectID, int k) {
    KNNList<D> knns = new KNNList<D>(k, distanceFunction.infiniteDistance());
    doKNNQuery(objectID, knns);
    return knns.getKNNDistance();
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
      knnLists.put(object.getID(), new KNNList<D>(k, distanceFunction.infiniteDistance()));

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
      MkNNLeafEntry<D> newEntry = new MkNNLeafEntry<D>(object.getID(), parentDistance,
                                                       distanceFunction.undefinedDistance());
      node.addLeafEntry(newEntry);

      // split the node if necessary
      while (hasOverflow(path)) {
        path = split(path);
      }
    }

    // do batch nn
    MkNNTreeNode<O, D> root = (MkNNTreeNode<O, D>) getRoot();
    batchNN(root, ids.toArray(new Integer[objects.size()]), knnLists);

    // adjust the knn distances
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkNNEntry<D> entry = (MkNNEntry<D>) root.getEntry(i);
      batchAdjustKNNDistance(entry, knnLists);
    }

    // test
    test(ROOT_NODE_ID);
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchAdjustKNNDistance(MkNNEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      KNNList<D> knns = knnLists.get(entry.getObjectID());
      entry.setKnnDistance(knns.getKNNDistance());
      return;
    }

    MkNNTreeNode<O, D> node = (MkNNTreeNode<O, D>) getNode(((MkNNDirectoryEntry<D>) entry).getNodeID());
    D knnDist = distanceFunction.nullDistance();

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkNNLeafEntry<D> e = (MkNNLeafEntry<D>) node.getEntry(i);
        KNNList<D> knn = knnLists.get(e.getObjectID());
        e.setKnnDistance(knn.getKNNDistance());
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkNNEntry<D> e = (MkNNEntry<D>) node.getEntry(i);
        batchAdjustKNNDistance(e, knnLists);
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }
  }

  private void batchNN(MkNNTreeNode<O, D> node, Integer[] ids, Map<Integer, KNNList<D>> knnLists) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkNNLeafEntry<D> p = (MkNNLeafEntry<D>) node.getEntry(i);
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();
          D dist_pq = distanceFunction.distance(p.getObjectID(), q);
          if (dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q.add(new QueryResult<D>(p.getObjectID(), dist_pq));
          }
        }
      }
    }
    else {
      List<DistanceEntry<D>> entries = getSortedEntries(node, ids);
      for (DistanceEntry<D> distEntry : entries) {
        D minDist = distEntry.getDistance();
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          if (minDist.compareTo(knn_q_maxDist) <= 0) {
            MkNNDirectoryEntry<D> entry = (MkNNDirectoryEntry<D>) distEntry.getEntry();
            MkNNTreeNode<O, D> child = (MkNNTreeNode<O, D>) getNode(entry.getNodeID());
            batchNN(child, ids, knnLists);
            break;
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
  protected void initCapacity(int pageSize) {
    D dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID + coveringRadius + parentDistance + knnDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + 3 * distanceSize) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + 2 * distanceSize) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a leaf node = " + (leafCapacity - 1));
  }


}
