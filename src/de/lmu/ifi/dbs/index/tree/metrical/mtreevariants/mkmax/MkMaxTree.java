package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.mkmax;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.util.RkNNStatistic;
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
 * MkNNTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 *
 * @author Elke Achtert 
 */
public class MkMaxTree<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTree<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> {

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
   * Provides some statistics about performed reverse knn-queries.
   */
  private RkNNStatistic rkNNStatistics = new RkNNStatistic();

  /**
   * Creates a new MkMaxTree.
   */
  public MkMaxTree() {
    super();
    optionHandler.put(K_P, new IntParameter(K_P, K_D, new GreaterConstraint(0)));
    this.debug = true;
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(MkMaxEntry<D> entry) {
    KNNList<D> knns_o = new KNNList<D>(k_max, getDistanceFunction().infiniteDistance());
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Inserts the specified objects into this MkMaxTree-Tree.
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
    adjustKNNDistance(getRootEntry(), knnLists);

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
      throw new IllegalArgumentException("Parameter k has to be equal or less than " + "parameter k of the MkMax-Tree!");
    }

    // get the candidates
    List<QueryResult<D>> candidates = new ArrayList<QueryResult<D>>();
    doReverseKNNQuery(object.getID(), null, getRoot(), candidates);

    if (k == this.k_max) {
      Collections.sort(candidates);
      rkNNStatistics.numberResults += candidates.size();
      return candidates;
    }

    // refinement of candidates
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
    List<Integer> candidateIDs = new ArrayList<Integer>();
    for (QueryResult<D> candidate : candidates) {
      KNNList<D> knns = new KNNList<D>(k, getDistanceFunction().infiniteDistance());
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
   * Creates a header for this index structure. Subclasses may need to
   * overwrite this method.
   */
  protected TreeIndexHeader createHeader() {
    return new MkMaxTreeHeader(pageSize, dirCapacity, leafCapacity, k_max);
  }

  /**
   * Performs a k-nearest neighbor query in the specified subtree
   * for the given query object.
   *
   * @param q          the id of the query object
   * @param node_entry the entry representing the node
   * @param node       the node of the subtree on which the query is performed
   * @param result     the list for the query result
   */
  private void doReverseKNNQuery(Integer q,
                                 MkMaxEntry<D> node_entry,
                                 MkMaxTreeNode<O, D> node,
                                 List<QueryResult<D>> result) {

    // data node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> entry = node.getEntry(i);
        D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
        if (distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new QueryResult<D>(entry.getRoutingObjectID(), distance));
        }
      }
    }

    // directory node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> entry = node.getEntry(i);
        D node_knnDist = node_entry != null ? node_entry.getKnnDistance() : getDistanceFunction().infiniteDistance();

        D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? getDistanceFunction().nullDistance() : distance.minus(entry
            .getCoveringRadius());

        if (minDist.compareTo(node_knnDist) <= 0) {
          MkMaxTreeNode<O, D> childNode = getNode(entry.getID());
          doReverseKNNQuery(q, entry, childNode, result);
        }
      }
    }
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   *
   * @param q         the entry to be inserted
   * @param nodeEntry the entry representing the root of thge current subtree
   * @param knns_q    the knns of q
   */
  private void preInsert(MkMaxEntry<D> q, MkMaxEntry<D> nodeEntry, KNNList<D> knns_q) {
    if (this.debug) {
      debugFine("\npreInsert " + q + " - " + nodeEntry + "\n");
    }

    D knnDist_q = knns_q.getKNNDistance();
    MkMaxTreeNode<O, D> node = file.readPage(nodeEntry.getID());
    D knnDist_node = getDistanceFunction().nullDistance();

    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> p = node.getEntry(i);
        D dist_pq = getDistanceFunction().distance(p.getRoutingObjectID(), q.getRoutingObjectID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist_pq.compareTo(knnDist_q) <= 0) {
          QueryResult<D> knn = new QueryResult<D>(p.getRoutingObjectID(), dist_pq);
          knns_q.add(knn);
          if (knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNList<D> knns_p = new KNNList<D>(k_max, getDistanceFunction().infiniteDistance());
          knns_p.add(new QueryResult<D>(q.getRoutingObjectID(), dist_pq));
          doKNNQuery(p.getRoutingObjectID(), knns_p);

          if (knns_p.size() < k_max) {
            p.setKnnDistance(getDistanceFunction().undefinedDistance());
          }
          else {
            D knnDist_p = knns_p.getMaximumDistance();
            p.setKnnDistance(knnDist_p);
          }
        }
        knnDist_node = Util.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      List<DistanceEntry<D, MkMaxEntry<D>>> entries = getSortedEntries(node, q.getRoutingObjectID());
      for (DistanceEntry<D, MkMaxEntry<D>> distEntry : entries) {
        MkMaxEntry<D> dirEntry = distEntry.getEntry();
        D entry_knnDist = dirEntry.getKnnDistance();

        if (distEntry.getDistance().compareTo(entry_knnDist) < 0 || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          preInsert(q, dirEntry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = Util.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    if (debug) {
      debugFine(nodeEntry + "set knn dist " + knnDist_node);
    }
    nodeEntry.setKnnDistance(knnDist_node);
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry    the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustKNNDistance(MkMaxEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    MkMaxTreeNode<O, D> node = file.readPage(entry.getID());
    D knnDist_node = getDistanceFunction().nullDistance();
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> leafEntry = node.getEntry(i);
        leafEntry.setKnnDistance(knnLists.get(leafEntry.getID()).getKNNDistance());
        knnDist_node = Util.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> dirEntry = node.getEntry(i);
        adjustKNNDistance(dirEntry, knnLists);
        knnDist_node = Util.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    entry.setKnnDistance(knnDist_node);
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param pageSize the size of a page in Bytes
   */
  protected void initCapacity(int pageSize) {
    D dummyDistance = getDistanceFunction().nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + knnDistance) + 1
    dirCapacity = (int) (pageSize - overhead) / (4 + 4 + 3 * distanceSize) + 1;

    if (dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if (dirCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance +
    // knnDistance) + 1
    leafCapacity = (int) (pageSize - overhead) / (4 + 2 * distanceSize) + 1;

    if (leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if (leafCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    k_max = (Integer) optionHandler.getOptionValue(K_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkMaxTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkMaxTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkMaxTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkMaxTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   *
   * @param object         the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of the
   *                       parent node
   */
  protected MkMaxEntry<D> createNewLeafEntry(O object, D parentDistance) {
    KNNList<D> knns = new KNNList<D>(k_max - 1, getDistanceFunction().infiniteDistance());
    doKNNQuery(object.getID(), knns);
    D knnDistance = knns.getKNNDistance();
    return new MkMaxLeafEntry<D>(object.getID(), parentDistance, knnDistance);
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node            the node to be represented by the new entry
   * @param routingObjectID the id of the routing object of the node
   * @param parentDistance  the distance from the routing object of the node to the
   *                        routing object of the parent node
   */
  protected MkMaxEntry<D> createNewDirectoryEntry(MkMaxTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
    return new MkMaxDirectoryEntry<D>(routingObjectID, parentDistance, node.getID(), node.coveringRadius(routingObjectID, this), node
        .kNNDistance(getDistanceFunction()));
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected MkMaxEntry<D> createRootEntry() {
    return new MkMaxDirectoryEntry<D>(null, null, 0, null, null);
  }
}
