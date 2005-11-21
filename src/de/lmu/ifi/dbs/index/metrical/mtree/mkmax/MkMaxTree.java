package de.lmu.ifi.dbs.index.metrical.mtree.mkmax;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.mknn.MkNNTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtree.util.ParentInfo;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.*;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for parameter
 * k < kmax.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkMaxTree<O extends MetricalObject, D extends Distance<D>> extends MTree<O, D> {
  /**
   * The parameter kmax.
   */
  private int kmax;

  /**
   * Creates a new MkNNTabTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the MTree
   * @param cacheSize the size of the cache in bytes
   */
  public MkMaxTree(String fileName, int cacheSize) {
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
   * @param kmax             the maximal number of knn distances to be stored
   * @param objects
   */
  public MkMaxTree(String fileName, int pageSize, int cacheSize,
                   DistanceFunction<O, D> distanceFunction, int kmax,
                   List<O> objects) {
    super();
    this.kmax = kmax;
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
   * @param kmax             the maximal number of knn distances to be stored
   */
  public MkMaxTree(String fileName, int pageSize, int cacheSize,
                   DistanceFunction<O, D> distanceFunction, int kmax) {
    super();
    this.kmax = kmax;
    init(fileName, pageSize, cacheSize, distanceFunction);
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
  public List<QueryResult<D>> reverseKNNQuery(O object, int k) {
    if (k > this.kmax) {
      throw new IllegalArgumentException("Parameter k has to be less or equal than " +
                                         "parameter kmax of the MkNNTab-Tree!");
    }

    MkMaxTreeNode<O, D> root = (MkMaxTreeNode<O, D>) getRoot();
    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    doReverseKNNQuery(k, object.getID(), null, root, result);

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
//        MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>) id;
//        System.out.println("  obj = " + e.getObjectID());
//        System.out.println("  pd  = " + e.getParentDistance());
//        System.out.println("  knns = " + Arrays.asList(e.getKnnDistances()));
      }
      else {
        node = file.readPage(id.value());
//        System.out.println(node + ", numEntries = " + node.getNumEntries());

        if (id instanceof DirectoryEntry) {
//          MkMaxDirectoryEntry<D> e = (MkMaxDirectoryEntry<D>) id;
//          System.out.println("  r_obj = " + e.getObjectID());
//          System.out.println("  pd = " + e.getParentDistance());
//          System.out.println("  cr = " + e.getCoveringRadius());
//          System.out.println("  knns = " + Arrays.asList(e.getKnnDistances()));
        }

        if (node.isLeaf()) {
          leafNodes++;

          for (int i = 0; i < node.getNumEntries(); i++) {
            MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>) node.getEntry(i);
//            if (e.getObjectID() == 73 || e.getObjectID() == 88)
//              System.out.println("XXXX object " + e.getObjectID() + " parent  " +node);
          }
        }
        else {
          dirNodes++;

          for (int i = 0; i < node.getNumEntries(); i++) {
            MkMaxDirectoryEntry<D> e = (MkMaxDirectoryEntry<D>) node.getEntry(i);
//            if (e.getNodeID() == 61 || e.getNodeID() == 323)
//              System.out.println("XXXX node " + e.getNodeID() + " parent  " +node);
          }
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
  protected MTreeNode<O, D> createEmptyRoot() {
    return new MkMaxTreeNode<O, D>(file, leafCapacity, true);
  }

  /**
   * Creates a header for this M-Tree.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected MTreeHeader createHeader(int pageSize) {
    return new MkNNTreeHeader(pageSize, dirCapacity, leafCapacity, kmax);
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

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID + coveringRadius + parentDistance + kmax + kmax * knnDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 4 + kmax * distanceSize) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + + kmax + kmax * knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize +  4 + kmax * distanceSize) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries " +
                    "in a leaf node = " + (leafCapacity - 1));

  }

  /**
    * Test the specified node (for debugging purpose)
    */
   protected void test(Identifier rootID) {
     BreadthFirstEnumeration<MTreeNode<O, D>> bfs = new BreadthFirstEnumeration<MTreeNode<O, D>>(file, rootID);

     while (bfs.hasMoreElements()) {
       Identifier id = bfs.nextElement();

       if (id.isNodeID()) {
         MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(id.value());
         node.test();

         if (id instanceof Entry) {
           MkMaxDirectoryEntry<D> e = (MkMaxDirectoryEntry<D>) id;
           node.testParentDistance(e.getObjectID(), distanceFunction);
           testCR(e);
           testKNNDistances(e);
         }
         else {
           node.testParentDistance(null, distanceFunction);
         }

         if (node.isLeaf()) {
           for (int i = 0; i < node.getNumEntries(); i++) {
             MkMaxLeafEntry<D> entry = (MkMaxLeafEntry<D>) node.getEntry(i);
             List<D> knnDistances = knnDistances(entry.getObjectID());

             for (int k = 1; k <= kmax; k++) {
               D knnDist_ist = entry.getKnnDistance(k);
               D knnDist_soll = knnDistances.get(k-1);

               if (! knnDist_ist.equals(knnDist_soll)) {
                 String msg = "\nknnDist_ist[" + k + "] != knnDist_soll[" + k + "] \n" +
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
  private void doReverseKNNQuery(int k,
                                 Integer q,
                                 MkMaxDirectoryEntry<D> node_entry,
                                 MkMaxTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxLeafEntry<D> entry = (MkMaxLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance(k)) <= 0)
          result.add(new QueryResult<D>(entry.getObjectID(), distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxDirectoryEntry<D> entry = (MkMaxDirectoryEntry<D>) node.getEntry(i);
        D node_knnDist = node_entry != null ?
                         node_entry.getKnnDistance(k) :
                         distanceFunction.infiniteDistance();

        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = (D) (entry.getCoveringRadius().compareTo(distance) > 0 ?
                    distanceFunction.nullDistance() :
                    distance.minus(entry.getCoveringRadius()));

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkMaxTreeNode<O, D> childNode = (MkMaxTreeNode<O, D>) getNode(entry.getNodeID());
          doReverseKNNQuery(k, q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkMaxDirectoryEntry<D> rootID) {
    MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(rootID.value());
    List<D> knnDistances_soll = node.kNNDistances(distanceFunction);

    for (int k = 1; k <= kmax; k++) {
      D knnDist_ist = rootID.getKnnDistance(k);
      D knnDist_soll = knnDistances_soll.get(k - 1);

      if (! knnDist_ist.equals(knnDist_soll)) {
        String msg = "\nknnDist_ist[" + k + "] != knnDist_soll[" + k + "] \n" +
                     knnDist_ist + " != " + knnDist_soll + "\n" +
                     "in " + node;

        System.out.println(msg);
        throw new RuntimeException(msg);
      }
    }
  }

  /**
   * Splits the specified node and returns the newly created split node.
   *
   * @param node the node to be splitted
   * @return the newly created split node
   */
  private MkMaxTreeNode<O, D> split(MkMaxTreeNode<O, D> node) {
    Integer routingObjectID = null;
    if (node.getNodeID() != ROOT_NODE_ID.value()) {
      MkMaxTreeNode<O, D> parent = (MkMaxTreeNode<O, D>) getNode(node.getParentID());
      routingObjectID = parent.getEntry(node.getIndex()).getObjectID();
    }
    Split<D> split = new MLBDistSplit<O, D>(node, routingObjectID, distanceFunction);

    MkMaxTreeNode<O, D> newNode = (MkMaxTreeNode<O, D>) node.splitEntries(split.assignmentsToFirst, split.assignmentsToSecond);
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
    MkMaxTreeNode<O, D> parent = (MkMaxTreeNode<O, D>) getNode(node.getParentID());
    MkMaxTreeNode<O, D> grandParent;
    D parentDistance1 = null, parentDistance2 = null;

    if (parent.getID() != ROOT_NODE_ID.value()) {
      grandParent = (MkMaxTreeNode<O, D>) getNode(parent.getParentID());
      Integer parentObject = grandParent.getEntry(parent.getIndex()).getObjectID();
      parentDistance1 = distanceFunction.distance(split.firstPromoted, parentObject);
      parentDistance2 = distanceFunction.distance(split.secondPromoted, parentObject);
    }

    // add the newNode to parent
    parent.addDirectoryEntry(new MkMaxDirectoryEntry<D>(split.secondPromoted,
                                                        parentDistance2,
                                                        newNode.getNodeID(),
                                                        split.secondCoveringRadius,
                                                        newNode.kNNDistances(distanceFunction)));

    // set the first promotion object, parentDistance and covering radius for node in parent
    MkMaxDirectoryEntry<D> entry1 = (MkMaxDirectoryEntry<D>) parent.getEntry(node.getIndex());
    entry1.setObjectID(split.firstPromoted);
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(split.firstCoveringRadius);
    entry1.setKnnDistances(node.kNNDistances(distanceFunction));

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
  private MkMaxTreeNode<O, D> createNewRoot(final MkMaxTreeNode<O, D> oldRoot,
                                            final MkMaxTreeNode<O, D> newNode,
                                            Integer firstPromoted, Integer secondPromoted,
                                            D firstCoveringRadius, D secondCoveringRadius) {
    StringBuffer msg = new StringBuffer();
    msg.append("create new root \n");

    MkMaxTreeNode<O, D> root = new MkMaxTreeNode<O, D>(file, dirCapacity, false);
    file.writePage(root);

    oldRoot.setID(root.getID());
    if (!oldRoot.isLeaf()) {
      for (int i = 0; i < oldRoot.getNumEntries(); i++) {
        MTreeNode<O, D> node = getNode(((DirectoryEntry) oldRoot.getEntry(i)).getNodeID());
        node.setParentID(oldRoot.getNodeID());
        file.writePage(node);
      }
    }
    file.writePage(oldRoot);

    root.setID(ROOT_NODE_ID.value());
    root.addDirectoryEntry(new MkMaxDirectoryEntry<D>(firstPromoted,
                                                      null,
                                                      oldRoot.getNodeID(),
                                                      firstCoveringRadius,
                                                      oldRoot.kNNDistances(distanceFunction)));

    root.addDirectoryEntry(new MkMaxDirectoryEntry<D>(secondPromoted,
                                                      null,
                                                      newNode.getNodeID(),
                                                      secondCoveringRadius,
                                                      newNode.kNNDistances(distanceFunction)));

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
  private List<D> knnDistances(Integer objectID) {
    KNNList<D> knns = new KNNList<D>(kmax, distanceFunction.infiniteDistance());
    doKNNQuery(objectID, knns);

    return knns.distancesToList();
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
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    // insert first
    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(kmax, distanceFunction.infiniteDistance()));

      // find insertion node
      ParentInfo placeToInsert = findInsertionNode(getRoot(), object.getID(), null);
      D parentDistance = placeToInsert.getRoutingObjectID() != null ?
                         distanceFunction.distance(object.getID(), placeToInsert.getRoutingObjectID()) :
                         null;
      MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) placeToInsert.getNode();

      // add the entry
      List<D> knnDistances = new ArrayList<D>();
      for (int i = 0; i < kmax; i++)
        knnDistances.add(distanceFunction.undefinedDistance());

      MkMaxLeafEntry<D> newEntry = new MkMaxLeafEntry<D>(object.getID(), parentDistance, knnDistances);
      node.addLeafEntry(newEntry);

      // split the node if necessary
      node = (MkMaxTreeNode<O, D>) placeToInsert.getNode();
      while (hasOverflow(node)) {
        node = split(node);
      }
    }

    // do batch nn
    MkMaxTreeNode<O, D> root = (MkMaxTreeNode<O, D>) getRoot();
    batchNN(root, ids, knnLists);

    // adjust the knn distances
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkMaxEntry<D> entry = (MkMaxEntry<D>) root.getEntry(i);
      batchAdjustKNNDistances(entry, knnLists);
    }

    test(ROOT_NODE_ID);
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchAdjustKNNDistances(MkMaxEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      KNNList<D> knns = knnLists.get(entry.getObjectID());
      entry.setKnnDistances(knns.distancesToList());
      return;
    }

    MkMaxTreeNode<O, D> node = (MkMaxTreeNode<O, D>) getNode(((MkMaxDirectoryEntry<D>) entry).getNodeID());
    List<D> knnDistances = initKnnDistanceList();

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>) node.getEntry(i);
        KNNList<D> knns = knnLists.get(e.getObjectID());
        List<D> entry_knnDistances = knns.distancesToList();
        e.setKnnDistances(entry_knnDistances);
        knnDistances = max(knnDistances, entry_knnDistances);
      }
      entry.setKnnDistances(knnDistances);
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> e = (MkMaxEntry<D>) node.getEntry(i);
        batchAdjustKNNDistances(e, knnLists);
        knnDistances = max(knnDistances, e.getKnnDistances());
      }
      entry.setKnnDistances(knnDistances);
    }
  }

  /**
   * Returns an array that holds the maximum values of
   * the both specified arrays in each index.
   *
   * @param distances1 the first array
   * @param distances2 the second array
   * @return an array that holds the maximum values of
   * the both specified arrays in each index
   */
  private List<D> max(List<D> distances1, List<D> distances2) {
    if (distances1.size() != distances2.size())
      throw new RuntimeException("different lengths!");

    List<D> result = new ArrayList<D>();

    for (int i = 0; i < distances1.size(); i++) {
      D d1 = distances1.get(i);
      D d2 = distances2.get(i);
      result.add(Util.max(d1, d2));
    }
    return result;
  }

  /**
   * Retuns a knn distance list with all distances set to null distance.
   * @return a knn distance list with all distances set to null distance
   */
  private List<D> initKnnDistanceList() {
    List<D> knnDistances = new ArrayList<D>(kmax);
    for (int i = 0; i < kmax; i++) {
      knnDistances.add(distanceFunction.nullDistance());
    }
    return knnDistances;
  }

}
