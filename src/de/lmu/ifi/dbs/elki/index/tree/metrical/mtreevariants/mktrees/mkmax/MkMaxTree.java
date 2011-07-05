package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnified;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.QueryStatistic;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k <= k_max. The k-nearest neigbor distance for k = k_max is stored
 * in each entry of a node.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has MkMaxTreeNode oneway - - contains
 * 
 * @param <O> the type of DatabaseObject to be stored in the MkMaxTree
 * @param <D> the type of Distance used in the MkMaxTree
 */
public class MkMaxTree<O, D extends Distance<D>> extends AbstractMkTreeUnified<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MkMaxTree.class);

  /**
   * Provides some statistics about performed reverse knn-queries.
   */
  private QueryStatistic rkNNStatistics = new QueryStatistic();

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   * @param k_max Maximum value for k
   */
  public MkMaxTree(PageFile<MkMaxTreeNode<O, D>> pagefile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(pagefile, distanceQuery, distanceFunction, k_max);
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. In the
   * first step the candidates are chosen by performing a reverse k-nearest
   * neighbor query with k = {@link #k_max}. Then these candidates are refined
   * in a second step.
   */
  @Override
  public List<DistanceResultPair<D>> reverseKNNQuery(DBID id, int k) {
    if(k > this.getKmax()) {
      throw new IllegalArgumentException("Parameter k has to be equal or less than " + "parameter k of the MkMax-Tree!");
    }

    // get the candidates
    List<DistanceResultPair<D>> candidates = new ArrayList<DistanceResultPair<D>>();
    doReverseKNNQuery(id, getRoot(), null, candidates);

    if(k == this.getKmax()) {
      Collections.sort(candidates);
      rkNNStatistics.addTrueHits(candidates.size());
      rkNNStatistics.addResults(candidates.size());
      return candidates;
    }

    // refinement of candidates
    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();
    ModifiableDBIDs candidateIDs = DBIDUtil.newArray();
    for(DistanceResultPair<D> candidate : candidates) {
      KNNHeap<D> knns = new KNNHeap<D>(k, getDistanceQuery().infiniteDistance());
      knnLists.put(candidate.getDBID(), knns);
      candidateIDs.add(candidate.getDBID());
    }
    batchNN(getRoot(), candidateIDs, knnLists);

    List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();
    for(DBID cid : candidateIDs) {
      for(DistanceResultPair<D> qr : knnLists.get(cid)) {
        if(id.equals(qr.getDBID())) {
          result.add(new GenericDistanceResultPair<D>(qr.getDistance(), cid));
          break;
        }
      }
    }

    rkNNStatistics.addResults(result.size());
    rkNNStatistics.addCandidates(candidates.size());
    Collections.sort(result);
    return result;
  }

  /**
   * Returns the statistic for performed rknn queries.
   * 
   * @return the statistic for performed rknn queries
   */
  public QueryStatistic getRkNNStatistics() {
    return rkNNStatistics;
  }

  /**
   * Clears the values of the statistic for performed rknn queries
   */
  public void clearRkNNStatistics() {
    rkNNStatistics.clear();
  }

  /**
   * Adapts the knn distances before insertion of the specified entry.
   * 
   */
  @Override
  protected void preInsert(MkMaxEntry<D> entry) {
    KNNHeap<D> knns_o = new KNNHeap<D>(getKmax(), getDistanceQuery().infiniteDistance());
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   */
  @Override
  protected void kNNdistanceAdjustment(MkMaxEntry<D> entry, Map<DBID, KNNHeap<D>> knnLists) {
    MkMaxTreeNode<O, D> node = getNode(entry);
    D knnDist_node = getDistanceQuery().nullDistance();
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> leafEntry = node.getEntry(i);
        leafEntry.setKnnDistance(knnLists.get(getPageID(leafEntry)).getKNNDistance());
        knnDist_node = DistanceUtil.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> dirEntry = node.getEntry(i);
        kNNdistanceAdjustment(dirEntry, knnLists);
        knnDist_node = DistanceUtil.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    entry.setKnnDistance(knnDist_node);
  }

  /**
   * Performs a reverse k-nearest neighbor query in the specified subtree for
   * the given query object with k = {@link #k_max}. It recursively traverses
   * all paths from the specified node, which cannot be excluded from leading to
   * qualififying objects.
   * 
   * @param q the id of the query object
   * @param node the node of the subtree on which the query is performed
   * @param node_entry the entry representing the node
   * @param result the list for the query result
   */
  private void doReverseKNNQuery(DBID q, MkMaxTreeNode<O, D> node, MkMaxEntry<D> node_entry, List<DistanceResultPair<D>> result) {
    // data node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> entry = node.getEntry(i);
        D distance = getDistanceQuery().distance(entry.getRoutingObjectID(), q);
        if(distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new GenericDistanceResultPair<D>(distance, entry.getRoutingObjectID()));
        }
      }
    }

    // directory node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> entry = node.getEntry(i);
        D node_knnDist = node_entry != null ? node_entry.getKnnDistance() : getDistanceQuery().infiniteDistance();

        D distance = getDistanceQuery().distance(entry.getRoutingObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? getDistanceQuery().nullDistance() : distance.minus(entry.getCoveringRadius());

        if(minDist.compareTo(node_knnDist) <= 0) {
          MkMaxTreeNode<O, D> childNode = getNode(entry);
          doReverseKNNQuery(q, childNode, entry, result);
        }
      }
    }
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   * 
   * @param q the entry to be inserted
   * @param nodeEntry the entry representing the root of thge current subtree
   * @param knns_q the knns of q
   */
  private void preInsert(MkMaxEntry<D> q, MkMaxEntry<D> nodeEntry, KNNHeap<D> knns_q) {
    if(logger.isDebugging()) {
      logger.debugFine("preInsert " + q + " - " + nodeEntry + "\n");
    }

    D knnDist_q = knns_q.getKNNDistance();
    MkMaxTreeNode<O, D> node = getNode(nodeEntry);
    D knnDist_node = getDistanceQuery().nullDistance();

    // leaf node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MkMaxEntry<D> p = node.getEntry(i);
        D dist_pq = getDistanceQuery().distance(p.getRoutingObjectID(), q.getRoutingObjectID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if(dist_pq.compareTo(knnDist_q) <= 0) {
          DistanceResultPair<D> knn = new GenericDistanceResultPair<D>(dist_pq, p.getRoutingObjectID());
          knns_q.add(knn);
          if(knns_q.size() >= getKmax()) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNHeap<D> knns_p = new KNNHeap<D>(getKmax(), getDistanceQuery().infiniteDistance());
          knns_p.add(new GenericDistanceResultPair<D>(dist_pq, q.getRoutingObjectID()));
          doKNNQuery(p.getRoutingObjectID(), knns_p);

          if(knns_p.size() < getKmax()) {
            p.setKnnDistance(getDistanceQuery().undefinedDistance());
          }
          else {
            D knnDist_p = knns_p.getMaximumDistance();
            p.setKnnDistance(knnDist_p);
          }
        }
        knnDist_node = DistanceUtil.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      List<DistanceEntry<D, MkMaxEntry<D>>> entries = getSortedEntries(node, q.getRoutingObjectID());
      for(DistanceEntry<D, MkMaxEntry<D>> distEntry : entries) {
        MkMaxEntry<D> dirEntry = distEntry.getEntry();
        D entry_knnDist = dirEntry.getKnnDistance();

        if(distEntry.getDistance().compareTo(entry_knnDist) < 0 || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          preInsert(q, dirEntry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = DistanceUtil.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    if(logger.isDebugging()) {
      logger.debugFine(nodeEntry + "set knn dist " + knnDist_node);
    }
    nodeEntry.setKnnDistance(knnDist_node);
  }

  @Override
  protected void initializeCapacities(MkMaxEntry<D> exampleLeaf) {
    int distanceSize = exampleLeaf.getParentDistance().externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if(getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (file.getPageSize() - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + knnDistance) + 1
    dirCapacity = (int) (getPageSize() - overhead) / (4 + 4 + 3 * distanceSize) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (file.getPageSize() - overhead) / (objectID +
    // parentDistance +
    // knnDistance) + 1
    leafCapacity = (int) (getPageSize() - overhead) / (4 + 2 * distanceSize) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }
  }

  /**
   * @return a new MkMaxTreeNode which is a leaf node
   */
  @Override
  protected MkMaxTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkMaxTreeNode<O, D>(capacity, true);
  }

  /**
   * @return a new MkMaxTreeNode which is a directory node
   */
  @Override
  protected MkMaxTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkMaxTreeNode<O, D>(capacity, false);
  }

  /**
   * @return a new MkMaxDirectoryEntry representing the specified node
   */
  @Override
  protected MkMaxEntry<D> createNewDirectoryEntry(MkMaxTreeNode<O, D> node, DBID routingObjectID, D parentDistance) {
    return new MkMaxDirectoryEntry<D>(routingObjectID, parentDistance, node.getPageID(), node.coveringRadius(routingObjectID, this), node.kNNDistance(getDistanceQuery()));
  }

  /**
   * @return a new MkMaxDirectoryEntry by calling
   *         <code>new MkMaxDirectoryEntry<D>(null, null, 0, null)</code>
   */
  @Override
  protected MkMaxEntry<D> createRootEntry() {
    return new MkMaxDirectoryEntry<D>(null, null, 0, null, null);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}