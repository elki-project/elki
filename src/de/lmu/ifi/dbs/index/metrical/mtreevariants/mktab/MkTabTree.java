package de.lmu.ifi.dbs.index.metrical.mtreevariants.mktab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Entry;
import de.lmu.ifi.dbs.index.IndexHeader;
import de.lmu.ifi.dbs.index.IndexPath;
import de.lmu.ifi.dbs.index.IndexPathComponent;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MLBDistSplit;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTree;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeDirectoryEntry;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeSplit;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.mkmax.MkMaxTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.util.Assignments;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k < kmax. All knn distances for k <= kmax are stored in each entry
 * of a node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkTabTree<O extends DatabaseObject, D extends Distance<D>, N extends MkTabTreeNode<O, D, N, E>, E extends MkTabEntry<D>> extends MTree<O, D, N, E> {

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "positive integer specifying the maximal number k of reverse" +
                                   "k nearest neighbors to be supported.";

  /**
   * Parameter k.
   */
  int k_max;

  /**
   * Creates a new MkNNTabTree.
   */
  public MkTabTree() {
    super();
    optionHandler.put(K_P, new Parameter(K_P,K_D,Parameter.Types.INT));
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree. This operation is
   * not supported.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Inserts the specified objects into this MDkNNTree-Tree.
   *
   * @param objects the object to be inserted
   */
  public void insert(List<O> objects) {
    if (this.debug) {
    	debugFine("insert " + objects + "\n");
    }

    if (! initialized) {
      initialize(objects.get(0));
    }

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    // insert first
    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(k_max, distanceFunction.infiniteDistance()));

      // find insertion path
      IndexPath<E> path = choosePath(object.getID(), getRootPath());

      // determine parent distance
      N node = getNode(path.getLastPathComponent().getEntry());
      D parentDistance = null;
      if (path.getPathCount() > 1) {
        N parent = getNode(path.getParentPath().getLastPathComponent().getEntry());
        Integer index = path.getLastPathComponent().getIndex();
        parentDistance = distanceFunction.distance(object.getID(),
                                                   parent.getEntry(index).getRoutingObjectID());
      }

      // add the object
      List<D> knnDistances = new ArrayList<D>();
      for (int i = 0; i < k_max; i++) {
        knnDistances.add(distanceFunction.undefinedDistance());
      }
      E  newEntry = (E) new MkTabLeafEntry<D>(object.getID(),
                                              parentDistance, knnDistances);
      node.addLeafEntry(newEntry);

      // split the node if necessary
      while (hasOverflow(path)) {
        path = split(path);
      }
    }

    // do batch nn
    N root = getRoot();
    batchNN(root, ids, knnLists);

    // adjust the knn distances
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkTabEntry<D> entry = root.getEntry(i);
      batchAdjustKNNDistances(entry, knnLists);
    }

    // test
    test(getRootPath());
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
      "Parameter k has to be less or equal than "
      + "parameter kmax of the MkNNTab-Tree!");
    }

    N root = getRoot();
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

    N node = getRoot();

    while (!node.isLeaf()) {
      if (node.getNumEntries() > 0) {
        MTreeDirectoryEntry<D> entry = (MTreeDirectoryEntry<D>) node.getEntry(0);
        node = getNode(entry.getID());
        levels++;
      }
    }

    BreadthFirstEnumeration<N, E> enumeration = new BreadthFirstEnumeration<N, E>(file, getRootPath());
    while (enumeration.hasMoreElements()) {
      IndexPath path = enumeration.nextElement();
      Entry nextEntry = path.getLastPathComponent().getEntry();
      if (nextEntry.isLeafEntry()) {
        objects++;
        // MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>) id;
        // System.out.println(" obj = " + e.getObjectID());
        // System.out.println(" pd = " + e.getParentDistance());
        // System.out.println(" knns = " +
        // Arrays.asList(e.getKnnDistances()));
      }
      else {
        node = file.readPage(nextEntry.getID());
        // System.out.println(node + ", numEntries = " +
        // node.getNumEntries());

        if (nextEntry instanceof MTreeDirectoryEntry) {
          // MkMaxDirectoryEntry<D> e = (MkMaxDirectoryEntry<D>) id;
          // System.out.println(" r_obj = " + e.getObjectID());
          // System.out.println(" pd = " + e.getParentDistance());
          // System.out.println(" cr = " + e.getCoveringRadius());
          // System.out.println(" knns = " +
          // Arrays.asList(e.getKnnDistances()));
        }

        if (node.isLeaf()) {
          leafNodes++;

          // for (int i = 0; i < node.getNumEntries(); i++) {
          // MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>)
          // node.getEntry(i);
          // if (e.getObjectID() == 73 || e.getObjectID() == 88)
          // System.out.println("XXXX object " + e.getObjectID() + "
          // parent " +node);
          // }
        }
        else {
          dirNodes++;

          // for (int i = 0; i < node.getNumEntries(); i++) {
          // MkMaxDirectoryEntry<D> e = (MkMaxDirectoryEntry<D>)
          // node.getEntry(i);
          // if (e.getNodeID() == 61 || e.getNodeID() == 323)
          // System.out.println("XXXX node " + e.getNodeID() + "
          // parent " +node);
          // }
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


  /**
   * Creates a header for this index structure.
   * Subclasses may need to overwrite this method.
   */
  protected IndexHeader createHeader() {
    return new MkMaxTreeHeader(pageSize, dirCapacity, leafCapacity, k_max);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   */
  protected void initCapacity(O object) {
    D dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + kmax + kmax * knnDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 4 + k_max * distanceSize) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    if (dirCapacity < 10)
    	warning("Page size is choosen too small! Maximum number of entries "
                    + "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + +
    // kmax + kmax * knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead)
                   / (4 + distanceSize + 4 + k_max * distanceSize) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    if (leafCapacity < 10)
    	warning("Page size is choosen too small! Maximum number of entries "
                    + "in a leaf node = " + (leafCapacity - 1));

  }

  /**
   * Test the specified node (for debugging purpose)
   */
  protected void test(IndexPath rootPath) {
    BreadthFirstEnumeration<N, E> bfs = new BreadthFirstEnumeration<N, E>(
    file, rootPath);

    while (bfs.hasMoreElements()) {
      IndexPath path = bfs.nextElement();
      Entry nextEntry = path.getLastPathComponent().getEntry();

      if (! nextEntry.isLeafEntry()) {
        N node = getNode(nextEntry.getID());
        node.test();

        if (nextEntry instanceof MTreeEntry) {
          MkTabDirectoryEntry<D> e = (MkTabDirectoryEntry<D>) nextEntry;
          node.testParentDistance(e.getRoutingObjectID(), distanceFunction);
          testCoveringRadius(path);
          testKNNDistances(e);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }

        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkTabLeafEntry<D> entry = (MkTabLeafEntry<D>) node.getEntry(i);
            List<D> knnDistances = knnDistances(entry.getRoutingObjectID());

            for (int k = 1; k <= k_max; k++) {
              D knnDist_ist = entry.getKnnDistance(k);
              D knnDist_soll = knnDistances.get(k - 1);

              if (!knnDist_ist.equals(knnDist_soll)) {
                String msg = "\nknnDist_ist[" + k
                             + "] != knnDist_soll[" + k + "] \n"
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
  private void doReverseKNNQuery(int k, Integer q,
                                 MkTabDirectoryEntry<D> node_entry, N node,
                                 List<QueryResult<D>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabLeafEntry<D> entry = (MkTabLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getRoutingObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance(k)) <= 0)
          result
          .add(new QueryResult<D>(entry.getRoutingObjectID(),
                                  distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabDirectoryEntry<D> entry = (MkTabDirectoryEntry<D>) node.getEntry(i);
        D node_knnDist = node_entry != null ?
                         node_entry.getKnnDistance(k) :
                         distanceFunction.infiniteDistance();

        D distance = distanceFunction.distance(entry.getRoutingObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                    distanceFunction.nullDistance() :
                    distance.minus(entry.getCoveringRadius());

        if (minDist.compareTo(node_knnDist) <= 0) {
          N childNode = getNode(entry.getID());
          doReverseKNNQuery(k, q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkTabDirectoryEntry<D> rootID) {
    N node = getNode(rootID.getID());
    List<D> knnDistances_soll = node.kNNDistances(distanceFunction);

    for (int k = 1; k <= k_max; k++) {
      D knnDist_ist = rootID.getKnnDistance(k);
      D knnDist_soll = knnDistances_soll.get(k - 1);

      if (!knnDist_ist.equals(knnDist_soll)) {
        String msg = "\nknnDist_ist[" + k + "] != knnDist_soll[" + k
                     + "] \n" + knnDist_ist + " != " + knnDist_soll + "\n"
                     + "in " + node;

        System.out.println(msg);
        throw new RuntimeException(msg);
      }
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
  private IndexPath split(IndexPath<E> path) {
    N node = getNode(path.getLastPathComponent().getEntry());
    Integer nodeIndex = path.getLastPathComponent().getIndex();

    // do split
    MTreeSplit<O, D, N, E> split = new MLBDistSplit<O, D, N, E>(node, distanceFunction);
    Assignments<D, E> assignments = split.getAssignments();

    N newNode = node.splitEntries(assignments.getFirstAssignments(),
                                  assignments.getSecondAssignments());

    if (this.debug) {
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
      debugFine(msg);
//      logger.fine(msg);
    }

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    // if root was split: create a new root that points the two split nodes
    if (node.getID() == getRootEntry().getID()) {
      return createNewRoot(node,
                           newNode,
                           assignments.getFirstRoutingObject(),
                           assignments.getSecondRoutingObject(),
                           assignments.getFirstCoveringRadius(),
                           assignments.getSecondCoveringRadius());
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
    parent.addDirectoryEntry((E) new MkTabDirectoryEntry<D>(assignments.getSecondRoutingObject(),
                                                            parentDistance2,
                                                            newNode.getID(),
                                                            assignments.getSecondCoveringRadius(),
                                                            newNode.kNNDistances(distanceFunction)));

    // set the first promotion object, parentDistance and covering radius
    // for node in parent
    MkTabDirectoryEntry<D> entry1 = (MkTabDirectoryEntry<D>) parent.getEntry(nodeIndex);
    entry1.setRoutingObjectID(assignments.getFirstRoutingObject());
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(assignments.getFirstCoveringRadius());
    entry1.setKnnDistances(node.kNNDistances(distanceFunction));

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
    // create new root
    StringBuffer msg = new StringBuffer();
    if (this.debug) {
      msg.append("create new root \n");
    }

    N root = (N) new MkTabTreeNode<O, D, N, E>(file, dirCapacity,
                                               false);
    file.writePage(root);

    // change id in old root and set id in new root
    oldRoot.setID(root.getID());
    root.setID(getRootEntry().getID());

    // add entries to new root
    root.addDirectoryEntry((E) new MkTabDirectoryEntry<D>(firstPromoted, null,
                                                          oldRoot.getID(), firstCoveringRadius,
                                                          oldRoot.kNNDistances(distanceFunction)));

    root.addDirectoryEntry((E) new MkTabDirectoryEntry<D>(secondPromoted, null,
                                                          newNode.getID(), secondCoveringRadius,
                                                          newNode.kNNDistances(distanceFunction)));

    // adjust the parentDistances
    for (int i = 0; i < oldRoot.getNumEntries(); i++) {
      D distance = distanceFunction.distance(firstPromoted, oldRoot.getEntry(i).getRoutingObjectID());
      oldRoot.getEntry(i).setParentDistance(distance);
    }
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(secondPromoted, newNode.getEntry(i).getRoutingObjectID());
      newNode.getEntry(i).setParentDistance(distance);
    }
    if (this.debug) {
      msg.append("firstCoveringRadius ").append(firstCoveringRadius).append("\n");
      msg.append("secondCoveringRadius ").append(secondCoveringRadius).append("\n");
    }

    // write the changes
    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);

    if (this.debug) {
      msg.append("New Root-ID ").append(root.getID()).append("\n");
      debugFine(msg.toString());
    }

    return new IndexPath<E>(new IndexPathComponent<E>(getRootEntry(), null));
  }

  /**
   * Returns the knn distance of the object with the specified id.
   *
   * @param objectID the id of the query object
   * @return the knn distance of the object with the specified id
   */
  private List<D> knnDistances(Integer objectID) {
    KNNList<D> knns = new KNNList<D>(k_max, distanceFunction
    .infiniteDistance());
    doKNNQuery(objectID, knns);

    return knns.distancesToList();
  }

  /**
   * Adjusts the knn distances for the specified subtree.
   *
   * @param entry
   */
  private void batchAdjustKNNDistances(MkTabEntry<D> entry,
                                       Map<Integer, KNNList<D>> knnLists) {
    // if root is a leaf
    if (entry.isLeafEntry()) {
      KNNList<D> knns = knnLists.get(entry.getRoutingObjectID());
      entry.setKnnDistances(knns.distancesToList());
      return;
    }

    N node = getNode(((MkTabDirectoryEntry<D>) entry).getID());
    List<D> knnDistances = initKnnDistanceList();

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabLeafEntry<D> e = (MkTabLeafEntry<D>) node.getEntry(i);
        KNNList<D> knns = knnLists.get(e.getRoutingObjectID());
        List<D> entry_knnDistances = knns.distancesToList();
        e.setKnnDistances(entry_knnDistances);
        knnDistances = max(knnDistances, entry_knnDistances);
      }
      entry.setKnnDistances(knnDistances);
    }

    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry<D> e = (MkTabEntry<D>) node.getEntry(i);
        batchAdjustKNNDistances(e, knnLists);
        knnDistances = max(knnDistances, e.getKnnDistances());
      }
      entry.setKnnDistances(knnDistances);
    }
  }

  /**
   * Returns an array that holds the maximum values of the both specified
   * arrays in each index.
   *
   * @param distances1 the first array
   * @param distances2 the second array
   * @return an array that holds the maximum values of the both specified
   *         arrays in each index
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
   *
   * @return a knn distance list with all distances set to null distance
   */
  private List<D> initKnnDistanceList() {
    List<D> knnDistances = new ArrayList<D>(k_max);
    for (int i = 0; i < k_max; i++) {
      knnDistances.add(distanceFunction.nullDistance());
    }
    return knnDistances;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected N createNewLeafNode(int capacity) {
    return (N) new MkTabTreeNode<O,D,N,E>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected N createNewDirectoryNode(int capacity) {
    return (N) new MkTabTreeNode<O,D,N,E>(file, capacity, false);
  }

}
