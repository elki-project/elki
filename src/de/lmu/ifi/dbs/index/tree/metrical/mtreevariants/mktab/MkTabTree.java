package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.mktab;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.mkmax.MkMaxTreeHeader;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k < kmax. All knn distances for k <= kmax are stored in each entry
 * of a node.
 *
 * @author Elke Achtert 
 */
public class MkTabTree<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTree<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> {

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "positive integer specifying the maximal number k of reverse" + "k nearest neighbors to be supported.";

  /**
   * Parameter k.
   */
  int k_max;

  /**
   * Creates a new MkNNTabTree.
   */
  public MkTabTree() {
    super();

    optionHandler.put(new IntParameter(K_P, K_D, new GreaterConstraint(0)));
    this.debug = true;
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(MkTabEntry<D> entry) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree. This operation is
   * not supported. todo Provide insertion of single objects
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

    if (!initialized) {
      initialize(objects.get(0));
    }

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    // insert first
    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(k_max, getDistanceFunction().infiniteDistance()));

      // insert the object
      super.insert(object, false);
    }

    // do batch nn
    batchNN(getRoot(), ids, knnLists);

    // adjust the knn distances
    adjustKNNDistances(getRootEntry(), knnLists);

    if (debug) {
      getRoot().test(this, getRootEntry());
    }
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
      throw new IllegalArgumentException("Parameter k has to be less or equal than " + "parameter kmax of the MkNNTab-Tree!");
    }

    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    doReverseKNNQuery(k, object.getID(), null, getRoot(), result);

    Collections.sort(result);
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    k_max = (Integer) optionHandler.getOptionValue(K_P);

    return remainingParameters;
  }

  /**
   * Creates a header for this index structure. Subclasses may need to
   * overwrite this method.
   */
  protected TreeIndexHeader createHeader() {
    return new MkMaxTreeHeader(pageSize, dirCapacity, leafCapacity, k_max);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param object a object that is stored in the tree
   */
  protected void initCapacity(O object) {
    D dummyDistance = getDistanceFunction().nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + kmax + kmax * knnDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + distanceSize + distanceSize + 4 + k_max * distanceSize) + 1;

    if (dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if (dirCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance + +
    // kmax + kmax * knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + distanceSize + 4 + k_max * distanceSize) + 1;

    if (leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if (leafCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

  }

  /**
   * Performs a k-nearest neighbor query in the specified subtree
   * for the given query object and the
   * given parameter k. 
   *
   * @param k          the parameter k of the knn-query
   * @param q          the id of the query object
   * @param node_entry the entry representing the node
   * @param node       the root of the subtree
   * @param result     the list holding the query result
   */
  private void doReverseKNNQuery(int k, Integer q, MkTabEntry<D> node_entry, MkTabTreeNode<O, D> node, List<QueryResult<D>> result) {
    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry<D> entry = node.getEntry(i);
        D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance(k)) <= 0) {
          result.add(new QueryResult<D>(entry.getRoutingObjectID(), distance));
        }
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry<D> entry = node.getEntry(i);
        D node_knnDist = node_entry != null ? node_entry.getKnnDistance(k) : getDistanceFunction().infiniteDistance();

        D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? getDistanceFunction().nullDistance() : distance.minus(entry
            .getCoveringRadius());

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkTabTreeNode<O, D> childNode = getNode(entry.getID());
          doReverseKNNQuery(k, q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Returns the knn distance of the object with the specified id.
   *
   * @param objectID the id of the query object
   * @return the knn distance of the object with the specified id
   */
  private List<D> knnDistances(Integer objectID) {
    KNNList<D> knns = new KNNList<D>(k_max, getDistanceFunction().infiniteDistance());
    doKNNQuery(objectID, knns);

    return knns.distancesToList();
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry    the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustKNNDistances(MkTabEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    MkTabTreeNode<O, D> node = file.readPage(entry.getID());
    List<D> knnDistances_node = initKnnDistanceList();
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry<D> leafEntry = node.getEntry(i);
        leafEntry.setKnnDistances(knnLists.get(leafEntry.getID()).distancesToList());
        knnDistances_node = max(knnDistances_node, leafEntry.getKnnDistances());
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkTabEntry<D> dirEntry = node.getEntry(i);
        adjustKNNDistances(dirEntry, knnLists);
        knnDistances_node = max(knnDistances_node, dirEntry.getKnnDistances());
      }
    }
    entry.setKnnDistances(knnDistances_node);
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
    if (distances1.size() != distances2.size()) {
      throw new RuntimeException("different lengths!");
    }

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
      knnDistances.add(getDistanceFunction().nullDistance());
    }
    return knnDistances;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkTabTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkTabTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkTabTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkTabTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   *
   * @param object         the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of the
   *                       parent node
   */
  protected MkTabEntry<D> createNewLeafEntry(O object, D parentDistance) {
    return new MkTabLeafEntry<D>(object.getID(), parentDistance, knnDistances(object.getID()));
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node            the node to be represented by the new entry
   * @param routingObjectID the id of the routing object of the node
   * @param parentDistance  the distance from the routing object of the node to the
   *                        routing object of the parent node
   */
  protected MkTabEntry<D> createNewDirectoryEntry(MkTabTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
    return new MkTabDirectoryEntry<D>(routingObjectID, parentDistance, node.getID(), node.coveringRadius(routingObjectID, this), node
        .kNNDistances(getDistanceFunction()));
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected MkTabEntry<D> createRootEntry() {
    return new MkTabDirectoryEntry<D>(null, null, 0, null, initKnnDistanceList());
	}

}
