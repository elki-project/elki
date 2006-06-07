package de.lmu.ifi.dbs.index.metrical.mtree.mkmax;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.TreePath;
import de.lmu.ifi.dbs.index.TreePathComponent;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.util.Assignments;
import de.lmu.ifi.dbs.index.metrical.mtree.util.DistanceEntry;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * MkNNTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkMaxTree<O extends DatabaseObject, D extends Distance<D>> extends MTree<O, D> {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>positive integer specifying the maximal number k of reverse" +
                                   "k nearest neighbors to be supported.";

  /**
   * Parameter k.
   */
  int k_max;

  /**
   * Provides some statistics about performed reverse knn-queries.
   */
  private RkNNStatistic rkNNStatistics = new RkNNStatistic();

  /**
   * Creates a new MkMaxTree.
   */
  public MkMaxTree() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    if (! initialized) {
      init();
    }
    if (DEBUG) {
      logger.info("insert " + object.getID() + " " + object + "\n");
    }

    // find insertion path
    TreePath rootPath = new TreePath(new TreePathComponent(ROOT_NODE_ID, null));
    TreePath path = findInsertionPath(object.getID(), rootPath);
    MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) path.getLastPathComponent().getIdentifier();

    // determine parent distance
    D parentDistance = null;
    if (path.getPathCount() > 1) {
      MTreeNode<O, D> parent = getNode(path.getParentPath().getLastPathComponent().getIdentifier());
      Integer index = path.getLastPathComponent().getIndex();
      parentDistance = distanceFunction.distance(object.getID(), parent.getEntry(index).getObjectID());
    }

    // do preInsert
    KNNList<D> knns = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
    MkMaxTreeNode<O, D> root = (MkMaxTreeNode<O, D>) getRoot();
    MkMaxLeafEntry<D> newEntry = new MkMaxLeafEntry<D>(object.getID(),
                                                       parentDistance, distanceFunction.undefinedDistance());
    knns.add(new QueryResult<D>(object.getID(), distanceFunction.nullDistance()));
    preInsert(newEntry, root, knns);

    // add the object
    node.addLeafEntry(newEntry);

    // adjust knn distances for path of q
    D knnDist = newEntry.getKnnDistance();
    TreePath currentPath = path;
    while (currentPath.getPathCount() > 1) {
      node = (MkMaxTreeNode<O, D>) currentPath.getLastPathComponent().getIdentifier();
      Integer nodeIndex = currentPath.getLastPathComponent().getIndex();
      MkMaxTreeNode<O, D> parent = (MkMaxTreeNode<O, D>) currentPath.getParentPath().getLastPathComponent().getIdentifier();

      MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) parent.getEntry(nodeIndex);
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
//    test(new TreePath(new TreePathComponent(ROOT_NODE_ID, null)));
  }

  /**
   * Inserts the specified objects into this MDkNNTree-Tree.
   *
   * @param objects the object to be inserted
   */
  public void insert(List<O> objects) {
    if (! initialized) {
      init();
    }

    if (DEBUG) {
      logger.fine("insert " + objects + "\n");
    }

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    // insert first
    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(k_max, distanceFunction.infiniteDistance()));

      // find insertion path
      TreePath rootPath = new TreePath(new TreePathComponent(
      ROOT_NODE_ID, null));
      TreePath path = findInsertionPath(object.getID(), rootPath);

      // determine parent distance
      MTreeNode<O, D> node = getNode(path.getLastPathComponent().getIdentifier());
      D parentDistance = null;
      if (path.getPathCount() > 1) {
        MTreeNode<O, D> parent = getNode(path.getParentPath().getLastPathComponent().getIdentifier());
        Integer index = path.getLastPathComponent().getIndex();
        parentDistance = distanceFunction.distance(object.getID(),
                                                   parent.getEntry(index).getObjectID());
      }

      // add the object
      MkMaxLeafEntry<D> newEntry = new MkMaxLeafEntry<D>(object.getID(),
                                                         parentDistance, distanceFunction.undefinedDistance());
      node.addLeafEntry(newEntry);

      // split the node if necessary
      while (hasOverflow(path)) {
        path = split(path);
      }
    }

    // do batch nn
    MkMaxTreeNode<O, D> root = (MkMaxTreeNode<O, D>) getRoot();
    batchNN(root, ids.toArray(new Integer[objects.size()]), knnLists);

    // adjust the knn distances
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkMaxEntry<D> entry = (MkMaxEntry<D>) root.getEntry(i);
      batchAdjustKNNDistance(entry, knnLists);
    }

    // test
    test(new TreePath(new TreePathComponent(ROOT_NODE_ID, null)));
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
      throw new IllegalArgumentException(
      "Parameter k has to be equal or less than "
      + "parameter k of the MDkNN-Tree!");
    }

    MkMaxTreeNode<O, D> root = (MkMaxTreeNode<O, D>) getRoot();
    List<QueryResult<D>> candidates = new ArrayList<QueryResult<D>>();

    doReverseKNNQuery(object.getID(), null, root, candidates);

    if (k == this.k_max) {
      Collections.sort(candidates);
      rkNNStatistics.numberResults += candidates.size();
      return candidates;
    }

    // refinement of candidates
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
    List<Integer> candidateIDs = new ArrayList<Integer>();
    for (QueryResult<D> candidate : candidates) {
      KNNList<D> knns = new KNNList<D>(k, distanceFunction
      .infiniteDistance());
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
        MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) node
        .getEntry(0);
        node = getNode(entry.getNodeID());
        levels++;
      }
    }

    TreePath rootPath = new TreePath(new TreePathComponent(ROOT_NODE_ID,
                                                           null));
    BreadthFirstEnumeration<MTreeNode<O, D>> enumeration = new BreadthFirstEnumeration<MTreeNode<O, D>>(
    file, rootPath);

    while (enumeration.hasMoreElements()) {
      Identifier id = enumeration.nextElement().getLastPathComponent()
      .getIdentifier();
      if (!id.isNodeID()) {
        objects++;
        // LeafEntry e = (LeafEntry) id;
        // System.out.println(" obj = " + e.getObjectID());
        // System.out.println(" pd = " + e.getParentDistance());
        // System.out.println(" knn = " + ((MkNNLeafEntry<D>)
        // id).getKnnDistance());
      }
      else {
        node = file.readPage(id.value());
        // System.out.println(node + ", numEntries = " +
        // node.getNumEntries());

        if (id instanceof MTreeDirectoryEntry) {
          // DirectoryEntry e = (DirectoryEntry) id;
          // System.out.println(" r_obj = " + e.getObjectID());
          // System.out.println(" pd = " + e.getParentDistance());
          // System.out.println(" cr = " + ((MkNNDirectoryEntry<D>)
          // id).getCoveringRadius());
          // System.out.println(" knn = " + ((MkNNDirectoryEntry<D>)
          // id).getKnnDistance());
        }

        if (node.isLeaf()) {
          leafNodes++;
          // for (int i = 0; i < node.getNumEntries(); i++) {
          // Entry e = node.getEntry(i);
          //
          // if (e.getObjectID() == 351) {
          // System.out.print("351 --> node " + node.getNodeID());
          // MkNNTreeNode<O, D> n = (MkNNTreeNode<O, D>) node;
          // while (n.getParentID() != null) {
          // MkNNTreeNode<O, D> parent = (MkNNTreeNode<O, D>)
          // getNode(n.getParentID());
          // System.out.print(" -> " + parent.getNodeID());
          // n = parent;
          // }
          // System.out.println("");
          // }
          // }
        }
        else {
          dirNodes++;
        }
      }
    }

    result.append(getClass().getName()).append(" hat ")
    .append((levels + 1)).append(" Ebenen \n");
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
  protected MkMaxTreeNode<O, D> createEmptyRoot() {
    return new MkMaxTreeNode<O, D>(file, leafCapacity, true);
  }

  /**
   * Creates a header for this M-Tree.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected MTreeHeader createHeader(int pageSize) {
    return new MkMaxTreeHeader(pageSize, dirCapacity, leafCapacity, k_max);
  }

  /**
   * Performs a k-nearest neighbor query for the given NumberVector with the
   * given parameter k and the according distance function. The query result
   * is in ascending order to the distance to the query object.
   *
   * @param q
   * @param node_entry
   * @param node
   * @param result
   */
  private void doReverseKNNQuery(Integer q,
                                 MkMaxDirectoryEntry<D> node_entry, MkMaxTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {

    // System.out.println("NODE " + node);

    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxLeafEntry<D> entry = (MkMaxLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance()) <= 0)
          result
          .add(new QueryResult<D>(entry.getObjectID(),
                                  distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) node
        .getEntry(i);
        D node_knnDist = node_entry != null ? node_entry
        .getKnnDistance() : distanceFunction.infiniteDistance();

        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? distanceFunction
        .nullDistance()
                    : distance.minus(entry.getCoveringRadius());

        // System.out.println(" node " + node + " entry " + entry + "
        // node_knnDist " + node_knnDist + " distance " + distance);

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkMaxTreeNode<O, D> childNode = (MkMaxTreeNode<O, D>) getNode(entry
          .getNodeID());
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
  private D preInsert(MkMaxLeafEntry<D> q, MkMaxTreeNode<O, D> node,
                      KNNList<D> knns_q) {

    D maxDist = distanceFunction.nullDistance();
    D knnDist_q = knns_q.getKNNDistance();

    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxLeafEntry<D> p = (MkMaxLeafEntry<D>) node.getEntry(i);
        D dist_pq = distanceFunction.distance(p.getObjectID(), q.getObjectID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist_pq.compareTo(knnDist_q) <= 0) {
          QueryResult<D> knn = new QueryResult<D>(p.getObjectID(),
                                                  dist_pq);
          knns_q.add(knn);
          if (knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNList<D> knns_p = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
          knns_p.add(new QueryResult<D>(q.getObjectID(), dist_pq));
          doKNNQuery(p.getObjectID(), knns_p);

          if (knns_p.size() < k_max)
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
        MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if (distEntry.getDistance().compareTo(entry_knnDist) < 0
            || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          MkMaxTreeNode<O, D> childNode = (MkMaxTreeNode<O, D>) getNode(entry.getNodeID());
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
   * Sorts the entries of the specified node according to their minimum
   * distance to the specified object.
   *
   * @param node the node
   * @param q    the id of the object
   * @return a list of the sorted entries
   */
  private List<DistanceEntry<D>> getSortedEntries(MkMaxTreeNode<O, D> node,
                                                  Integer q) {
    List<DistanceEntry<D>> result = new ArrayList<DistanceEntry<D>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) node.getEntry(i);
      D distance = distanceFunction.distance(entry.getObjectID(), q);
      D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                  distanceFunction.nullDistance()
                  : distance.minus(entry.getCoveringRadius());

      result.add(new DistanceEntry<D>(entry, minDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Sorts the entries of the specified node according to their minimum
   * distance to the specified objects.
   *
   * @param node the node
   * @param ids  the ids of the objects
   * @return a list of the sorted entries
   */
  private List<DistanceEntry<D>> getSortedEntries(MkMaxTreeNode<O, D> node,
                                                  Integer[] ids) {
    List<DistanceEntry<D>> result = new ArrayList<DistanceEntry<D>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) node
      .getEntry(i);

      D minMinDist = distanceFunction.infiniteDistance();
      for (Integer q : ids) {
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? distanceFunction
        .nullDistance()
                    : distance.minus(entry.getCoveringRadius());
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
  protected void test(TreePath rootPath) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs = new BreadthFirstEnumeration<MTreeNode<O, D>>(
    file, rootPath);

    while (bfs.hasMoreElements()) {
      TreePath path = bfs.nextElement();
      Identifier id = path.getLastPathComponent().getIdentifier();

      if (id.isNodeID()) {
        MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(id
        .value());
        node.test();

        if (id instanceof MTreeEntry) {
          MkMaxDirectoryEntry<D> e = (MkMaxDirectoryEntry<D>) id;
          node.testParentDistance(e.getObjectID(), distanceFunction);
          testCoveringRadius(path);
          testKNNDist(e);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }

        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkMaxLeafEntry<D> entry = (MkMaxLeafEntry<D>) node
            .getEntry(i);
            D knnDist_ist = entry.getKnnDistance();
            D knnDist_soll = knnDistance(entry.getObjectID(), k_max);

            if (!entry.getKnnDistance().equals(knnDist_soll)) {
              String msg = "\nknnDist_ist != knnDist_soll \n"
                           + knnDist_ist + " != " + knnDist_soll
                           + "\n" + "in " + node + " at entry "
                           + entry;

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
  private void testKNNDist(MkMaxDirectoryEntry<D> rootID) {
    D knnDist_ist = rootID.getKnnDistance();

    MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(rootID.value());
    D knnDist_soll = node.kNNDistance(distanceFunction);

    if (!knnDist_ist.equals(knnDist_soll)) {
      String msg = "\nknnDist_ist != knnDist_soll \n" + knnDist_ist
                   + " != " + knnDist_soll + "\n" + "in " + node;

      System.out.println(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Splits the last node in the specified path and returns a path containing
   * at last element the parent of the newly created split node.
   *
   * @param path the path containing at last element the node to be splitted
   * @return a path containing at last element the parent of the newly created
   *         split node
   */
  private TreePath split(TreePath path) {
    MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(path
    .getLastPathComponent().getIdentifier());
    Integer nodeIndex = path.getLastPathComponent().getIndex();

    // do split
    MTreeSplit<O, D> split = new MLBDistSplit<O, D>(node, distanceFunction);
    Assignments<D> assignments = split.getAssignments();

    MkMaxTreeNode<O, D> newNode = (MkMaxTreeNode<O, D>) node.splitEntries(
    assignments.getFirstAssignments(), assignments.getSecondAssignments());

    if (DEBUG) {
      String msg = "Split Node " + node.getID() + " (" + this.getClass()
                   + ")\n" + "      newNode " + newNode.getID() + "\n"
                   + "      firstPromoted " + assignments.getFirstRoutingObject()
                   + "\n" + "      firstAssignments(" + node.getID() + ") "
                   + assignments.getFirstAssignments() + "\n" + "      firstCR "
                   + assignments.getFirstCoveringRadius() + "\n"
                   + "      secondPromoted "
                   + assignments.getSecondRoutingObject() + "\n"
                   + "      secondAssignments(" + newNode.getID() + ") "
                   + assignments.getSecondAssignments() + "\n" + "      secondCR "
                   + assignments.getSecondCoveringRadius() + "\n";
      logger.fine(msg);
    }

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    // if root was split: create a new root that points the two split nodes
    if (node.getID() == ROOT_NODE_ID.value()) {
      return createNewRoot(node, newNode, assignments.getFirstRoutingObject(),
                           assignments.getSecondRoutingObject(),
                           assignments.getFirstCoveringRadius(),
                           assignments.getSecondCoveringRadius());
    }

    // determine the new parent distances
    MTreeNode<O, D> parent = getNode(path.getParentPath().getLastPathComponent().getIdentifier());
    Integer parentIndex = path.getParentPath().getLastPathComponent().getIndex();
    MTreeNode<O, D> grandParent;
    D parentDistance1 = null, parentDistance2 = null;

    if (parent.getID() != ROOT_NODE_ID.value()) {
      grandParent = getNode(path.getParentPath().getParentPath().getLastPathComponent().getIdentifier());
      Integer parentObject = grandParent.getEntry(parentIndex).getObjectID();
      parentDistance1 = distanceFunction.distance(assignments.getFirstRoutingObject(), parentObject);
      parentDistance2 = distanceFunction.distance(assignments.getSecondRoutingObject(), parentObject);
    }

    // add the newNode to parent
    parent.addDirectoryEntry(new MkMaxDirectoryEntry<D>(assignments.getSecondRoutingObject(),
                                                        parentDistance2,
                                                        newNode.getNodeID(), assignments.getSecondCoveringRadius(),
                                                        newNode.kNNDistance(distanceFunction)));

    // set the first promotion object, parentDistance and covering radius
    // for node in parent
    MkMaxDirectoryEntry<D> entry1 = (MkMaxDirectoryEntry<D>) parent.getEntry(nodeIndex);
    entry1.setObjectID(assignments.getFirstRoutingObject());
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(assignments.getFirstCoveringRadius());
    entry1.setKnnDistance(node.kNNDistance(distanceFunction));

    // adjust the parentDistances in node
    for (int i = 0; i < node.getNumEntries(); i++) {
      D distance = distanceFunction.distance(assignments.getFirstRoutingObject(), node.getEntry(i).getObjectID());
      node.getEntry(i).setParentDistance(distance);
    }

    // adjust the parentDistances in newNode
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(assignments.getSecondRoutingObject(), newNode.getEntry(i).getObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }

    // write changes in parent to file
    file.writePage(parent);

    return path.getParentPath();
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
  private TreePath createNewRoot(final MkMaxTreeNode<O, D> oldRoot,
                                 final MkMaxTreeNode<O, D> newNode, Integer firstPromoted,
                                 Integer secondPromoted, D firstCoveringRadius,
                                 D secondCoveringRadius) {
    // create new root
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("create new root \n");
    }

    MkMaxTreeNode<O, D> root = new MkMaxTreeNode<O, D>(file, dirCapacity,
                                                       false);
    file.writePage(root);

    // change id in old root and set id in new root
    oldRoot.setID(root.getID());
    root.setID(ROOT_NODE_ID.value());

    // add entries to new root
    root.addDirectoryEntry(new MkMaxDirectoryEntry<D>(firstPromoted, null,
                                                      oldRoot.getNodeID(), firstCoveringRadius,
                                                      oldRoot.kNNDistance(distanceFunction)));

    root.addDirectoryEntry(new MkMaxDirectoryEntry<D>(secondPromoted, null,
                                                      newNode.getNodeID(), secondCoveringRadius,
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
    if (DEBUG) {
      msg.append("firstCoveringRadius ").append(firstCoveringRadius).append("\n");
      msg.append("secondCoveringRadius ").append(secondCoveringRadius).append("\n");
    }

    // write the changes
    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);

    if (DEBUG) {
      msg.append("New Root-ID ").append(root.getNodeID()).append("\n");
      logger.info(msg.toString());
    }

    return new TreePath(new TreePathComponent(ROOT_NODE_ID, null));
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
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchAdjustKNNDistance(MkMaxEntry<D> entry,
                                      Map<Integer, KNNList<D>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      KNNList<D> knns = knnLists.get(entry.getObjectID());
      entry.setKnnDistance(knns.getKNNDistance());
      return;
    }

    MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(((MkMaxDirectoryEntry<D>) entry).getNodeID());
    D knnDist = distanceFunction.nullDistance();

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>) node.getEntry(i);
        KNNList<D> knn = knnLists.get(e.getObjectID());
        e.setKnnDistance(knn.getKNNDistance());
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> e = (MkMaxEntry<D>) node.getEntry(i);
        batchAdjustKNNDistance(e, knnLists);
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }
  }

  private void batchNN(MkMaxTreeNode<O, D> node, Integer[] ids,
                       Map<Integer, KNNList<D>> knnLists) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxLeafEntry<D> p = (MkMaxLeafEntry<D>) node.getEntry(i);
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();
          D dist_pq = distanceFunction.distance(p.getObjectID(), q);
          if (dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q
            .add(new QueryResult<D>(p.getObjectID(),
                                    dist_pq));
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
            MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) distEntry
            .getEntry();
            MkMaxTreeNode<O, D> child = (MkMaxTreeNode<O, D>) getNode(entry
            .getNodeID());
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
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + knnDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + 3 * distanceSize)
                  + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries "
                    + "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance +
    // knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + 2 * distanceSize) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries "
                    + "in a leaf node = " + (leafCapacity - 1));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    try {
      k_max = Integer.parseInt(optionHandler.getOptionValue(K_P));
      if (k_max <= 0)
        throw new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D, e);
    }
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
    mySettings.addSetting(K_P, Integer.toString(k_max));
    return attributeSettings;
  }

}
