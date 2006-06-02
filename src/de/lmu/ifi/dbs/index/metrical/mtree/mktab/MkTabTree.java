package de.lmu.ifi.dbs.index.metrical.mtree.mktab;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.TreePath;
import de.lmu.ifi.dbs.index.TreePathComponent;
import de.lmu.ifi.dbs.index.metrical.mtree.*;
import de.lmu.ifi.dbs.index.metrical.mtree.mkmax.MkMaxTreeHeader;
import de.lmu.ifi.dbs.index.metrical.mtree.util.Assignments;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k < kmax. All knn distances for k <= kmax are stored in each entry
 * of a node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkTabTree<O extends DatabaseObject, D extends Distance<D>> extends MTree<O, D> {
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
   * Creates a new MkNNTabTree.
   */
  public MkTabTree() {
    super();
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
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
      knnLists.put(object.getID(), new KNNList<D>(k_max, distanceFunction
      .infiniteDistance()));

      // find insertion path
      TreePath rootPath = new TreePath(new TreePathComponent(
      ROOT_NODE_ID, null));
      TreePath path = findInsertionPath(object.getID(), rootPath);

      // determine parent distance
      MTreeNode<O, D> node = getNode(path.getLastPathComponent()
      .getIdentifier());
      D parentDistance = null;
      if (path.getPathCount() > 1) {
        MTreeNode<O, D> parent = getNode(path.getParentPath()
        .getLastPathComponent().getIdentifier());
        Integer index = path.getLastPathComponent().getIndex();
        parentDistance = distanceFunction.distance(object.getID(),
                                                   parent.getEntry(index).getObjectID());
      }

      // add the object
      List<D> knnDistances = new ArrayList<D>();
      for (int i = 0; i < k_max; i++) {
        knnDistances.add(distanceFunction.undefinedDistance());
      }
      MkTabLeafEntry<D> newEntry = new MkTabLeafEntry<D>(object.getID(),
                                                         parentDistance, knnDistances);
      node.addLeafEntry(newEntry);

      // split the node if necessary
      while (hasOverflow(path)) {
        path = split(path);
      }
    }

    // do batch nn
    MkTabTreeNode<O, D> root = (MkTabTreeNode<O, D>) getRoot();
    batchNN(root, ids, knnLists);

    // adjust the knn distances
    for (int i = 0; i < root.getNumEntries(); i++) {
      MkTabEntry<D> entry = (MkTabEntry<D>) root.getEntry(i);
      batchAdjustKNNDistances(entry, knnLists);
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
      "Parameter k has to be less or equal than "
      + "parameter kmax of the MkNNTab-Tree!");
    }

    MkTabTreeNode<O, D> root = (MkTabTreeNode<O, D>) getRoot();
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
      TreePath path = enumeration.nextElement();
      Identifier id = path.getLastPathComponent().getIdentifier();
      if (!id.isNodeID()) {
        objects++;
        // MkMaxLeafEntry<D> e = (MkMaxLeafEntry<D>) id;
        // System.out.println(" obj = " + e.getObjectID());
        // System.out.println(" pd = " + e.getParentDistance());
        // System.out.println(" knns = " +
        // Arrays.asList(e.getKnnDistances()));
      }
      else {
        node = file.readPage(id.value());
        // System.out.println(node + ", numEntries = " +
        // node.getNumEntries());

        if (id instanceof MTreeDirectoryEntry) {
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
   * Creates a new leaf node with the specified capacity.
   *
   * @return a new leaf node
   */
  protected MTreeNode<O, D> createEmptyRoot() {
    return new MkTabTreeNode<O, D>(file, leafCapacity, true);
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
   * Determines the maximum and minimum number of entries in a node.
   */
  protected void initCapacity() {
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
      logger.severe("Page size is choosen too small! Maximum number of entries "
                    + "in a directory node = " + (dirCapacity - 1));

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + +
    // kmax + kmax * knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead)
                   / (4 + distanceSize + 4 + k_max * distanceSize) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    if (leafCapacity < 10)
      logger.severe("Page size is choosen too small! Maximum number of entries "
                    + "in a leaf node = " + (leafCapacity - 1));

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
        MkTabTreeNode<O, D> node = (MkTabTreeNode<O, D>) getNode(id
        .value());
        node.test();

        if (id instanceof MTreeEntry) {
          MkTabDirectoryEntry<D> e = (MkTabDirectoryEntry<D>) id;
          node.testParentDistance(e.getObjectID(), distanceFunction);
          testCoveringRadius(path);
          testKNNDistances(e);
        }
        else {
          node.testParentDistance(null, distanceFunction);
        }

        if (node.isLeaf()) {
          for (int i = 0; i < node.getNumEntries(); i++) {
            MkTabLeafEntry<D> entry = (MkTabLeafEntry<D>) node
            .getEntry(i);
            List<D> knnDistances = knnDistances(entry.getObjectID());

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
                                 MkTabDirectoryEntry<D> node_entry, MkTabTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabLeafEntry<D> entry = (MkTabLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance(k)) <= 0)
          result
          .add(new QueryResult<D>(entry.getObjectID(),
                                  distance));
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabDirectoryEntry<D> entry = (MkTabDirectoryEntry<D>) node
        .getEntry(i);
        D node_knnDist = node_entry != null ? node_entry
        .getKnnDistance(k) : distanceFunction
        .infiniteDistance();

        D distance = distanceFunction.distance(entry.getObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? distanceFunction
        .nullDistance()
                    : distance.minus(entry.getCoveringRadius());

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkTabTreeNode<O, D> childNode = (MkTabTreeNode<O, D>) getNode(entry
          .getNodeID());
          doReverseKNNQuery(k, q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Test the specified node (for debugging purpose)
   */
  private void testKNNDistances(MkTabDirectoryEntry<D> rootID) {
    MkTabTreeNode<O, D> node = (MkTabTreeNode<O, D>) getNode(rootID.value());
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
  private TreePath split(TreePath path) {
    MkTabTreeNode<O, D> node = (MkTabTreeNode<O, D>) getNode(path
    .getLastPathComponent().getIdentifier());
    Integer nodeIndex = path.getLastPathComponent().getIndex();

    // do split
    Split<O, D> split = new MLBDistSplit<O, D>(node, distanceFunction);
    Assignments<D> assignments = split.getAssignments();

    MkTabTreeNode<O, D> newNode = (MkTabTreeNode<O, D>) node.splitEntries(
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
    parent.addDirectoryEntry(new MkTabDirectoryEntry<D>(assignments.getSecondRoutingObject(),
                                                        parentDistance2,
                                                        newNode.getNodeID(),
                                                        assignments.getSecondCoveringRadius(),
                                                        newNode.kNNDistances(distanceFunction)));

    // set the first promotion object, parentDistance and covering radius
    // for node in parent
    MkTabDirectoryEntry<D> entry1 = (MkTabDirectoryEntry<D>) parent.getEntry(nodeIndex);
    entry1.setObjectID(assignments.getFirstRoutingObject());
    entry1.setParentDistance(parentDistance1);
    entry1.setCoveringRadius(assignments.getFirstCoveringRadius());
    entry1.setKnnDistances(node.kNNDistances(distanceFunction));

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
  private TreePath createNewRoot(final MkTabTreeNode<O, D> oldRoot,
                                 final MkTabTreeNode<O, D> newNode, Integer firstPromoted,
                                 Integer secondPromoted, D firstCoveringRadius,
                                 D secondCoveringRadius) {
    // create new root
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("create new root \n");
    }

    MkTabTreeNode<O, D> root = new MkTabTreeNode<O, D>(file, dirCapacity,
                                                       false);
    file.writePage(root);

    // change id in old root and set id in new root
    oldRoot.setID(root.getID());
    root.setID(ROOT_NODE_ID.value());

    // add entries to new root
    root.addDirectoryEntry(new MkTabDirectoryEntry<D>(firstPromoted, null,
                                                      oldRoot.getNodeID(), firstCoveringRadius,
                                                      oldRoot.kNNDistances(distanceFunction)));

    root.addDirectoryEntry(new MkTabDirectoryEntry<D>(secondPromoted, null,
                                                      newNode.getNodeID(), secondCoveringRadius,
                                                      newNode.kNNDistances(distanceFunction)));

    // adjust the parentDistances
    for (int i = 0; i < oldRoot.getNumEntries(); i++) {
      D distance = distanceFunction.distance(firstPromoted, oldRoot
      .getEntry(i).getObjectID());
      oldRoot.getEntry(i).setParentDistance(distance);
    }
    for (int i = 0; i < newNode.getNumEntries(); i++) {
      D distance = distanceFunction.distance(secondPromoted, newNode
      .getEntry(i).getObjectID());
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
      logger.fine(msg.toString());
    }

    return new TreePath(new TreePathComponent(ROOT_NODE_ID, null));
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
      KNNList<D> knns = knnLists.get(entry.getObjectID());
      entry.setKnnDistances(knns.distancesToList());
      return;
    }

    MkTabTreeNode<O, D> node = (MkTabTreeNode<O, D>) getNode(((MkTabDirectoryEntry<D>) entry)
    .getNodeID());
    List<D> knnDistances = initKnnDistanceList();

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabLeafEntry<D> e = (MkTabLeafEntry<D>) node.getEntry(i);
        KNNList<D> knns = knnLists.get(e.getObjectID());
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

}
