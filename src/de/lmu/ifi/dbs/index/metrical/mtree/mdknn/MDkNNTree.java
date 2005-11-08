package de.lmu.ifi.dbs.index.metrical.mtree.mdknn;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.util.DistanceEntry;
import de.lmu.ifi.dbs.index.metrical.mtree.util.ParentInfo;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;

/**
 * MDkNNTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MDkNNTree<O extends MetricalObject, D extends Distance> extends MTree<O, D> {
  /**
   * The parameter k.
   */
  private int k;

  /**
   * Creates a new MDkNNTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the MTree
   * @param cacheSize the size of the cache in bytes
   */
  public MDkNNTree(String fileName, int cacheSize) {
    init(new MDkNNTreeHeader(), fileName, cacheSize);
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
   * @param k                the parameter k
   * @param objects
   */
  public MDkNNTree(String fileName, int pageSize, int cacheSize,
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
   * @param k                the parameter k
   */
  public MDkNNTree(String fileName, int pageSize, int cacheSize,
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

    // find insertion node
    ParentInfo<O, D> placeToInsert = findInsertionNode(getRoot(), object.getID(), null);
    D parentDistance = placeToInsert.getRoutingObjectID() != null ?
                       distanceFunction.distance(object.getID(), placeToInsert.getRoutingObjectID()) :
                       null;
    MDkNNTreeNode<O, D> node = (MDkNNTreeNode<O, D>) placeToInsert.getNode();
    System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                       "\ninsert " + object.getID() + " " + object + " into " + node + "\n");

    // do preInsert
    KNNList<D> knns = new KNNList<D>(k, distanceFunction.infiniteDistance());
    MDkNNTreeNode<O, D> root = (MDkNNTreeNode<O, D>) getRoot();
    MDkNNLeafEntry<D> newEntry = new MDkNNLeafEntry<D>(object.getID(), parentDistance,
                                                       distanceFunction.undefinedDistance());
    knns.add(new QueryResult<D>(object.getID(), distanceFunction.nullDistance()));
    preInsert(newEntry, root, knns);

    // add the entry
    node.addLeafEntry(newEntry);

    // adjust knn distances for path of q
    node = (MDkNNTreeNode<O, D>) placeToInsert.getNode();
    D knnDist = newEntry.getKnnDistance();
    while (node.getNodeID() != ROOT_NODE_ID.value()) {
      MDkNNTreeNode<O, D> parent = (MDkNNTreeNode<O, D>) getNode(node.getParentID());
      MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) parent.getEntry(node.getIndex());
      if (entry.getKnnDistance().compareTo(knnDist) < 0) {
        entry.setKnnDistance(knnDist);
        node = parent;
        knnDist = node.kNNDistance(distanceFunction);
      }
      else
        break;
    }

    // split the node if necessary
    node = (MDkNNTreeNode<O, D>) placeToInsert.getNode();
    while (hasOverflow(node)) {
      node = split(node);
    }

//    System.out.println(this.toString());
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
    if (k != this.k) {
      throw new IllegalArgumentException("Parameter k has to have the same value as " +
                                         "parameter k of the MDkNN-Tree!");
    }

    MDkNNTreeNode<O, D> root = (MDkNNTreeNode<O, D>) getRoot();
    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    doReverseKNNQuery(object.getID(), null, root, result);

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
        LeafEntry e = (LeafEntry) id;
        System.out.println("  obj = " + e.getObjectID());
        System.out.println("  pd  = " + e.getParentDistance());
        System.out.println("  knn = " + ((MDkNNLeafEntry<D>) id).getKnnDistance());
      }
      else {
        node = file.readPage(id.value());
        System.out.println(node + ", numEntries = " + node.getNumEntries());

        if (id instanceof DirectoryEntry) {
          DirectoryEntry e = (DirectoryEntry) id;
          System.out.println("  r_obj = " + e.getObjectID());
          System.out.println("  pd = " + e.getParentDistance());
          System.out.println("  cr = " + ((MDkNNDirectoryEntry<D>) id).getCoveringRadius());
          System.out.println("  knn = " + ((MDkNNDirectoryEntry<D>) id).getKnnDistance());
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
  protected MDkNNTreeNode<O, D> createEmptyRoot() {
    return new MDkNNTreeNode<O, D>(file, leafCapacity, true);
  }

  /**
   * Creates a header for this M-Tree.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected MTreeHeader createHeader(int pageSize) {
    return new MDkNNTreeHeader(pageSize, dirCapacity, leafCapacity, k);
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
                                 MDkNNDirectoryEntry<D> node_entry,
                                 MDkNNTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> entry = (MDkNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance()) <= 0)
          result.add(new QueryResult<D>(entry.getObjectID(), distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) node.getEntry(i);
        D node_knnDist = node_entry != null ?
                         node_entry.getKnnDistance() : distanceFunction.infiniteDistance();

        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(node_knnDist) <= 0) {
          MDkNNTreeNode<O, D> childNode = (MDkNNTreeNode<O, D>) getNode(entry.getNodeID());
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
  private D preInsert(MDkNNLeafEntry<D> q,
                      MDkNNTreeNode<O, D> node,
                      KNNList<D> knns_q) {

    D maxDist = distanceFunction.nullDistance();

    D knnDist_q = knns_q.size() == k ?
                  knns_q.getMaximumDistance() :
                  distanceFunction.infiniteDistance();

    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> p = (MDkNNLeafEntry<D>) node.getEntry(i);
        D dist_pq = distanceFunction.distance(p.getObjectID(), q.getObjectID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist_pq.compareTo(knnDist_q) <= 0) {
          QueryResult<D> knn = new QueryResult<D>(p.getObjectID(), dist_pq);
          knns_q.add(knn);
          if (knns_q.size() == k) {
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
        MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if (distEntry.getDistance().compareTo(entry_knnDist) < 0 ||
            distEntry.getDistance().compareTo(knnDist_q) < 0) {
          MDkNNTreeNode<O, D> childNode = (MDkNNTreeNode<O, D>) getNode(entry.getNodeID());
          D entry_knnDist1 = preInsert(q, childNode, knns_q);
          entry.setKnnDistance(entry_knnDist1);
          knnDist_q = knns_q.size() == k ?
                      knns_q.getMaximumDistance() :
                      distanceFunction.infiniteDistance();
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
  private List<DistanceEntry<D>> getSortedEntries(MDkNNTreeNode<O, D> node, Integer q) {
    List<DistanceEntry<D>> result = new ArrayList<DistanceEntry<D>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) node.getEntry(i);
      D distance = distanceFunction.distance(entry.getObjectID(), q);
      D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                  distanceFunction.nullDistance() :
                  distance.minus(entry.getCoveringRadius());

      result.add(new DistanceEntry<D>(entry, minDist));
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
  private List<DistanceEntry<D>> getSortedEntries(MDkNNTreeNode<O, D> node, Integer[] ids) {
    List<DistanceEntry<D>> result = new ArrayList<DistanceEntry<D>>();

    for (int i = 0; i < node.getNumEntries(); i++) {
      MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) node.getEntry(i);

      D minMinDist = distanceFunction.infiniteDistance();
      for (Integer q : ids) {
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                    distanceFunction.nullDistance() :
                    distance.minus(entry.getCoveringRadius());
        if (minDist.compareTo(minMinDist) < 0)
          minMinDist = minDist;
      }
      result.add(new DistanceEntry<D>(entry, minMinDist));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void test(Identifier rootID) {
    BreadthFirstEnumeration<MTreeNode<O, D>> bfs = new BreadthFirstEnumeration<MTreeNode<O, D>>(file, rootID);

    while (bfs.hasMoreElements()) {
      Identifier id = bfs.nextElement();

      if (id.isNodeID()) {
        MDkNNTreeNode<O, D> node = (MDkNNTreeNode<O, D>) getNode(id.value());
        node.test();

        if (id instanceof Entry) {
          MDkNNDirectoryEntry<D> e = (MDkNNDirectoryEntry<D>) id;
          node.testParentDistance(e.getObjectID(), distanceFunction);
          testCR(e);
          testKNNDist(e);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }

        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MDkNNLeafEntry<D> entry = (MDkNNLeafEntry<D>) node.getEntry(i);
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
  private void testKNNDist(MDkNNDirectoryEntry<D> rootID) {
    D knnDist_ist = rootID.getKnnDistance();

    MDkNNTreeNode<O, D> node = (MDkNNTreeNode<O, D>) getNode(rootID.value());
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
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private MDkNNTreeNode<O, D> split(MDkNNTreeNode<O, D> node) {
    Integer routingObjectID = null;
    if (node.getNodeID() != ROOT_NODE_ID.value()) {
      MDkNNTreeNode<O, D> parent = (MDkNNTreeNode<O, D>) getNode(node.getParentID());
      routingObjectID = parent.getEntry(node.getIndex()).getObjectID();
    }
    Split<D> split = new MLBDistSplit<O, D>(node, routingObjectID, distanceFunction);

    MDkNNTreeNode<O, D> newNode = node.splitEntries(split.assignmentsToFirst, split.assignmentsToSecond);
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
    MDkNNTreeNode<O, D> parent = (MDkNNTreeNode<O, D>) getNode(node.getParentID());
    MDkNNTreeNode<O, D> grandParent;
    D parentDistance1 = null, parentDistance2 = null;

    if (parent.getID() != ROOT_NODE_ID.value()) {
      grandParent = (MDkNNTreeNode<O, D>) getNode(parent.getParentID());
      Integer parentObject = grandParent.getEntry(parent.getIndex()).getObjectID();
      parentDistance1 = distanceFunction.distance(split.firstPromoted, parentObject);
      parentDistance2 = distanceFunction.distance(split.secondPromoted, parentObject);
    }

    // add the newNode to parent
    parent.addNode(newNode,
                   split.secondPromoted,
                   parentDistance2,
                   split.secondCoveringRadius,
                   newNode.kNNDistance(distanceFunction));

    // set the first promotion object, parentDistance and covering radius for node in parent
    MDkNNDirectoryEntry<D> entry1 = (MDkNNDirectoryEntry<D>) parent.getEntry(node.getIndex());
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
  private MDkNNTreeNode<O, D> createNewRoot(final MDkNNTreeNode<O, D> oldRoot,
                                            final MDkNNTreeNode<O, D> newNode,
                                            Integer firstPromoted, Integer secondPromoted,
                                            D firstCoveringRadius, D secondCoveringRadius) {
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");

    MDkNNTreeNode<O, D> root = new MDkNNTreeNode<O, D>(file, dirCapacity, false);
    file.writePage(root);

    oldRoot.setID(root.getID());
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        MTreeNode<O, D> node = getNode(((DirectoryEntry) oldRoot.getEntry(i)).getNodeID());
        node.setParentID(oldRoot.getNodeID());
        file.writePage(node);
      }
    }

    root.setID(ROOT_NODE_ID.value());
    root.addNode(oldRoot, firstPromoted, null, firstCoveringRadius, oldRoot.kNNDistance(distanceFunction));
    root.addNode(newNode, secondPromoted, null, secondCoveringRadius, newNode.kNNDistance(distanceFunction));

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

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    msg.append("New Root-ID ").append(root.getNodeID()).append("\n");
    logger.info(msg.toString());

    return root;
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
//    System.out.println("knns(" + objectID + ") = " +knns);

    if (knns.size() < k) return distanceFunction.undefinedDistance();

    return knns.getMaximumDistance();
  }

  /**
   * Inserts the specified objects into this MDkNNTree-Tree.
   *
   * @param objects the object to be inserted
   */
  @SuppressWarnings({"unchecked"})
  private void insert(List<O> objects) {
    logger.info("insert " + objects + "\n");

    // insert first
    for (O object : objects) {
      // find insertion node
      ParentInfo placeToInsert = findInsertionNode(getRoot(), object.getID(), null);
      D parentDistance = placeToInsert.getRoutingObjectID() != null ?
                         distanceFunction.distance(object.getID(), placeToInsert.getRoutingObjectID()) :
                         null;
      MDkNNTreeNode<O, D> node = (MDkNNTreeNode<O, D>) placeToInsert.getNode();

      // add the entry
      MDkNNLeafEntry<D> newEntry = new MDkNNLeafEntry<D>(object.getID(), parentDistance,
                                                         distanceFunction.undefinedDistance());
      node.addLeafEntry(newEntry);

      // split the node if necessary
      node = (MDkNNTreeNode<O, D>) placeToInsert.getNode();
      while (hasOverflow(node)) {
        node = split(node);
      }
    }

    MDkNNTreeNode<O, D> root = (MDkNNTreeNode<O, D>) getRoot();
    for (int i = 0; i < root.getNumEntries(); i++) {
      MDkNNEntry<D> entry = (MDkNNEntry<D>) root.getEntry(i);
      adjustKNNDistance(entry);
    }

//    System.out.println(this.toString());
    test(ROOT_NODE_ID);
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchAdjustKNNDistance(MDkNNEntry<D> entry) {
    if (entry.isLeafEntry()) {
      D knnDist = knnDistance(entry.getObjectID(), k);
      entry.setKnnDistance(knnDist);
      return;
    }

    MDkNNTreeNode<O, D> node = (MDkNNTreeNode<O, D>) getNode(((MDkNNDirectoryEntry<D>) entry).getNodeID());
    D knnDist = distanceFunction.nullDistance();

    if (node.isLeaf()) {
      Integer[] ids = new Integer[node.getNumEntries()];
      Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
      for (int i = 0; i < node.getNumEntries(); i++) {
        ids[i] = node.getEntry(i).getObjectID();
        knnLists.put(ids[i], new KNNList<D>(k, distanceFunction.infiniteDistance()));
      }
      batchNN((MDkNNTreeNode<O, D>) getRoot(), ids, knnLists);


      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> e = (MDkNNLeafEntry<D>) node.getEntry(i);
        Integer id = ids[i];
        KNNList<D> knn = knnLists.get(id);
        if (knn.size() != k)
          throw new RuntimeException("snh!");

        KNNList<D> knn_soll = new KNNList<D>(k, distanceFunction.infiniteDistance());
        doKNNQuery(id, knn_soll);

        if (! knn.equals(knn_soll)) {
          System.out.println("knn     [" + id + "] = " + knn);
          System.out.println("knn_soll[" + id + "] = " + knn_soll);
          throw new RuntimeException("snh!");
        }

        e.setKnnDistance(knn.getMaximumDistance());
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNEntry<D> e = (MDkNNEntry<D>) node.getEntry(i);
        adjustKNNDistance(e);
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void adjustKNNDistance(MDkNNEntry<D> entry) {
    if (entry.isLeafEntry()) {
      D knnDist = knnDistance(entry.getObjectID(), k);
      entry.setKnnDistance(knnDist);
    }
    else {
      MDkNNTreeNode<O, D> node = (MDkNNTreeNode<O, D>) getNode(((MDkNNDirectoryEntry<D>) entry).getNodeID());
      D knnDist = distanceFunction.nullDistance();
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNEntry<D> e = (MDkNNEntry<D>) node.getEntry(i);
        adjustKNNDistance(e);
        knnDist = Util.max(knnDist, e.getKnnDistance());
      }
      entry.setKnnDistance(knnDist);
    }
  }

  private void batchNN(MDkNNTreeNode<O, D> node, Integer[] ids, Map<Integer, KNNList<D>> knnLists) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MDkNNLeafEntry<D> p = (MDkNNLeafEntry<D>) node.getEntry(i);
        for (Integer q : ids) {
          KNNList<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.size() == k ?
                            knns_q.getMaximumDistance() :
                            distanceFunction.infiniteDistance();

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
          D knn_q_maxDist = knns_q.size() == k ?
                            knns_q.getMaximumDistance() :
                            distanceFunction.infiniteDistance();

          if (minDist.compareTo(knn_q_maxDist) <= 0) {
            MDkNNDirectoryEntry<D> entry = (MDkNNDirectoryEntry<D>) distEntry.getEntry();
            MDkNNTreeNode<O, D> child = (MDkNNTreeNode<O, D>) getNode(entry.getNodeID());
            batchNN(child, ids, knnLists);
            break;
          }
        }
      }
    }
  }

}
