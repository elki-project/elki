package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 * <p/>
 * TODO: noch nicht fertig!!!
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has RdKNNNode oneway - - contains
 * @apiviz.has RdKNNTreeHeader
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
// FIXME: currently does not yet return RKNNQuery objects!
public class RdKNNTree<O extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends NonFlatRStarTree<RdKNNNode<D>, RdKNNEntry<D>> implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RdKNNTree.class);

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * The distance function.
   */
  private SpatialPrimitiveDistanceFunction<O, D> distanceFunction;

  /**
   * The distance function.
   */
  private SpatialDistanceQuery<O, D> distanceQuery;

  /**
   * Internal knn query object, for updating the rKNN.
   */
  protected KNNQuery<O, D> knnQuery;

  /**
   * The realtion we query.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param k_max max k
   * @param distanceFunction distance function
   * @param distanceQuery distance query
   */
  public RdKNNTree(Relation<O> relation, PageFile<RdKNNNode<D>> pagefile, int k_max, SpatialPrimitiveDistanceFunction<O, D> distanceFunction, SpatialDistanceQuery<O, D> distanceQuery) {
    super(pagefile);
    this.relation = relation;
    this.k_max = k_max;
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceQuery;
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   * 
   * @param entry the entry to be inserted
   */
  @Override
  protected void preInsert(RdKNNEntry<D> entry) {
    KNNHeap<D> knns_o = new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance());
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Performs necessary operations after deleting the specified object.
   */
  @Override
  protected void postDelete(RdKNNEntry<D> entry) {
    // reverse knn of o
    List<DistanceResultPair<D>> rnns = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), ((RdKNNLeafEntry<D>) entry).getDBID(), rnns);

    // knn of rnn
    ModifiableDBIDs ids = DBIDUtil.newArray();
    for(DistanceResultPair<D> rnn : rnns) {
      ids.add(rnn.getDBID());
    }

    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(ids.size());
    for(DBID id : ids) {
      knnLists.put(id, new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance()));
    }
    knnQuery.getKNNForBulkHeaps(knnLists);

    // adjust knn distances
    adjustKNNDistance(getRootEntry(), knnLists);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(List<RdKNNEntry<D>> entries) {
    super.bulkLoad(entries);

    // adjust all knn distances
    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(entries.size());
    for(RdKNNEntry<D> entry : entries) {
      DBID id = ((RdKNNLeafEntry<D>) entry).getDBID();
      knnLists.put(id, new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance()));
    }
    knnQuery.getKNNForBulkHeaps(knnLists);
    adjustKNNDistance(getRootEntry(), knnLists);

    // test
    doExtraIntegrityChecks();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T extends Distance<T>> List<DistanceResultPair<T>> reverseKNNQuery(DBID oid, int k, SpatialPrimitiveDistanceFunction<? super O, T> distanceFunction, KNNQuery<O, D> knnQuery) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    List candidates = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), oid, candidates);

    if(k == k_max) {
      Collections.sort(candidates);
      return candidates;
    }

    // refinement of candidates, if k < k_max
    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();
    ModifiableDBIDs candidateIDs = DBIDUtil.newArray();
    for(int i = 0; i < candidates.size(); i++) {
      DistanceResultPair<D> candidate = (DistanceResultPair<D>) candidates.get(i);
      KNNHeap<T> knns = new KNNHeap<T>(k, distanceFunction.getDistanceFactory().infiniteDistance());
      knnLists.put(candidate.getDBID(), (KNNHeap<D>) knns);
      candidateIDs.add(candidate.getDBID());
    }
    knnQuery.getKNNForBulkHeaps(knnLists);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DBID id : candidateIDs) {
      for(DistanceResultPair<D> qr : knnLists.get(id)) {
        if(oid.equals(qr.getDBID())) {
          result.add(new GenericDistanceResultPair<T>((T) qr.getDistance(), id));
          break;
        }
      }
    }

    Collections.sort(result);
    return result;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkReverseKNNQueryForID(DBIDs ids, int k, SpatialPrimitiveDistanceFunction<? super O, T> distanceFunction, KNNQuery<O, D> knnQuery) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    Map<DBID, List<DistanceResultPair<D>>> candidateMap = new HashMap<DBID, List<DistanceResultPair<D>>>();
    for(DBID id : ids) {
      candidateMap.put(id, new ArrayList<DistanceResultPair<D>>());
    }
    doBulkReverseKNN(getRoot(), ids, candidateMap);

    if(k == k_max) {
      List<List<DistanceResultPair<T>>> resultList = new ArrayList<List<DistanceResultPair<T>>>();
      for(List candidates : candidateMap.values()) {
        Collections.sort(candidates);
        resultList.add(candidates);
      }
      return resultList;
    }

    // refinement of candidates, if k < k_max
    // perform a knn query for the candidates
    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();
    for(List<DistanceResultPair<D>> candidates : candidateMap.values()) {
      for(DistanceResultPair<D> candidate : candidates) {
        if(!knnLists.containsKey(candidate.getDBID())) {
          KNNHeap<T> knns = new KNNHeap<T>(k, distanceFunction.getDistanceFactory().infiniteDistance());
          knnLists.put(candidate.getDBID(), (KNNHeap<D>) knns);
        }
      }
    }
    knnQuery.getKNNForBulkHeaps(knnLists);

    // and add candidate c to the result if o is a knn of c
    List<List<DistanceResultPair<T>>> resultList = new ArrayList<List<DistanceResultPair<T>>>();
    for(DBID id : candidateMap.keySet()) {
      List<DistanceResultPair<D>> candidates = candidateMap.get(id);
      List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
      for(DistanceResultPair<D> candidate : candidates) {
        for(DistanceResultPair<D> qr : knnLists.get(candidate.getDBID())) {
          if(qr.getDBID() == id) {
            result.add(new GenericDistanceResultPair<T>((T) qr.getDistance(), id));
            break;
          }
        }
      }
      resultList.add(result);
    }
    return resultList;
  }

  @Override
  protected TreeIndexHeader createHeader() {
    return new RdKNNTreeHeader(getPageSize(), dirCapacity, leafCapacity, dirMinimum, leafCapacity, k_max);
  }

  @Override
  protected void initializeCapacities(RdKNNEntry<D> exampleLeaf) {
    int dimensionality = exampleLeaf.getDimensionality();
    D dummyDistance = distanceQuery.getDistanceFactory().nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if(getPageSize() - overhead < 0) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (childID + childMBR + knnDistance)
    // + 1
    dirCapacity = (int) ((getPageSize() - overhead) / (4 + 16 * dimensionality + distanceSize)) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if(dirMinimum < 2) {
      dirMinimum = 2;
    }

    // leafCapacity = (pageSize - overhead) / (childID + childValues +
    // knnDistance) + 1
    leafCapacity = (int) ((getPageSize() - overhead) / (4 + 8 * dimensionality + distanceSize)) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + getPageSize() + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if(leafMinimum < 2) {
      leafMinimum = 2;
    }

    if(logger.isVerbose()) {
      logger.verbose("Directory Capacity: " + dirCapacity + "\nLeaf Capacity: " + leafCapacity);
    }
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified object.
   * 
   * @param node the node
   * @param q the query object
   * @param distanceFunction the distance function for computing the distances
   * @return a list of the sorted entries
   */
  // TODO: move somewhere else?
  protected List<DistanceEntry<D, RdKNNEntry<D>>> getSortedEntries(AbstractRStarTreeNode<?, ?> node, SpatialComparable q, SpatialPrimitiveDistanceFunction<?, D> distanceFunction) {
    List<DistanceEntry<D, RdKNNEntry<D>>> result = new ArrayList<DistanceEntry<D, RdKNNEntry<D>>>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      @SuppressWarnings("unchecked")
      RdKNNEntry<D> entry = (RdKNNEntry<D>) node.getEntry(i);
      D minDist = distanceFunction.minDist(entry, q);
      result.add(new DistanceEntry<D, RdKNNEntry<D>>(entry, minDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   * 
   * @param q the entry to be inserted
   * @param nodeEntry the entry representing the root of the current subtree
   * @param knns_q the knns of q
   */
  private void preInsert(RdKNNEntry<D> q, RdKNNEntry<D> nodeEntry, KNNHeap<D> knns_q) {
    D knnDist_q = knns_q.getKNNDistance();
    RdKNNNode<D> node = getNode(nodeEntry);
    D knnDist_node = distanceQuery.getDistanceFactory().nullDistance();

    // leaf node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> p = (RdKNNLeafEntry<D>) node.getEntry(i);
        D dist_pq = distanceQuery.distance(p.getDBID(), ((LeafEntry) q).getDBID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if(dist_pq.compareTo(knnDist_q) <= 0) {
          knns_q.add(dist_pq, p.getDBID());
          if(knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          O obj = relation.get(p.getDBID());
          List<DistanceResultPair<D>> knns_without_q = knnQuery.getKNNForObject(obj, k_max);

          if(knns_without_q.size() + 1 < k_max) {
            p.setKnnDistance(distanceQuery.getDistanceFactory().undefinedDistance());
          }
          else {
            D knnDist_p = DistanceUtil.min(knns_without_q.get(knns_without_q.size() - 1).getDistance(), dist_pq);
            p.setKnnDistance(knnDist_p);
          }
        }
        knnDist_node = DistanceUtil.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      O obj = relation.get(((LeafEntry) q).getDBID());
      List<DistanceEntry<D, RdKNNEntry<D>>> entries = getSortedEntries(node, obj, distanceFunction);
      for(DistanceEntry<D, RdKNNEntry<D>> distEntry : entries) {
        RdKNNEntry<D> entry = distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if(distEntry.getDistance().compareTo(entry_knnDist) < 0 || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          preInsert(q, entry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = DistanceUtil.max(knnDist_node, entry.getKnnDistance());
      }
    }
    nodeEntry.setKnnDistance(knnDist_node);
  }

  /**
   * Performs a reverse knn query in the specified subtree.
   * 
   * @param node the root node of the current subtree
   * @param oid the id of the object for which the rknn query is performed
   * @param result the list containing the query results
   */
  private void doReverseKNN(RdKNNNode<D> node, DBID oid, List<DistanceResultPair<D>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceQuery.distance(entry.getDBID(), oid);
        if(distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new GenericDistanceResultPair<D>(distance, entry.getDBID()));
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D> entry = (RdKNNDirectoryEntry<D>) node.getEntry(i);
        D minDist = distanceQuery.minDist(entry, oid);
        if(minDist.compareTo(entry.getKnnDistance()) <= 0) {
          doReverseKNN(getNode(entry), oid, result);
        }
      }
    }
  }

  /**
   * Performs a bulk reverse knn query in the specified subtree.
   * 
   * @param node the root node of the current subtree
   * @param ids the object ids for which the rknn query is performed
   * @param result the map containing the query results for each object
   */
  private void doBulkReverseKNN(RdKNNNode<D> node, DBIDs ids, Map<DBID, List<DistanceResultPair<D>>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) node.getEntry(i);
        for(DBID id : ids) {
          D distance = distanceQuery.distance(entry.getDBID(), id);
          if(distance.compareTo(entry.getKnnDistance()) <= 0) {
            result.get(id).add(new GenericDistanceResultPair<D>(distance, entry.getDBID()));
          }
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D> entry = (RdKNNDirectoryEntry<D>) node.getEntry(i);
        ModifiableDBIDs candidates = DBIDUtil.newArray();
        for(DBID id : ids) {
          D minDist = distanceQuery.minDist(entry, id);
          if(minDist.compareTo(entry.getKnnDistance()) <= 0) {
            candidates.add(id);
          }
          if(!candidates.isEmpty()) {
            doBulkReverseKNN(getNode(entry), candidates, result);
          }
        }
      }
    }
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   * 
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustKNNDistance(RdKNNEntry<D> entry, Map<DBID, KNNHeap<D>> knnLists) {
    RdKNNNode<D> node = getNode(entry);
    D knnDist_node = distanceQuery.getDistanceFactory().nullDistance();
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D> leafEntry = node.getEntry(i);
        KNNHeap<D> knns = knnLists.get(((LeafEntry) leafEntry).getDBID());
        if(knns != null) {
          leafEntry.setKnnDistance(knnLists.get(((LeafEntry) leafEntry).getDBID()).getKNNDistance());
        }
        knnDist_node = DistanceUtil.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D> dirEntry = node.getEntry(i);
        adjustKNNDistance(dirEntry, knnLists);
        knnDist_node = DistanceUtil.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    entry.setKnnDistance(knnDist_node);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @return a new leaf node
   */
  @Override
  protected RdKNNNode<D> createNewLeafNode() {
    return new RdKNNNode<D>(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @return a new directory node
   */
  @Override
  protected RdKNNNode<D> createNewDirectoryNode() {
    return new RdKNNNode<D>(dirCapacity, false);
  }

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   */
  @Override
  protected RdKNNEntry<D> createNewDirectoryEntry(RdKNNNode<D> node) {
    return new RdKNNDirectoryEntry<D>(node.getPageID(), node.computeMBR(), node.kNNDistance());
  }

  /**
   * Creates an entry representing the root node.
   * 
   * @return an entry representing the root node
   */
  @Override
  protected RdKNNEntry<D> createRootEntry() {
    return new RdKNNDirectoryEntry<D>(0, null, null);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not an instance of the distance function used by this index.
   * 
   * @throws IllegalArgumentException
   * @param <T> distance type
   * @param distanceFunction the distance function to be checked
   */
  private <T extends Distance<T>> void checkDistanceFunction(SpatialPrimitiveDistanceFunction<? super O, T> distanceFunction) {
    // todo: the same class does not necessarily indicate the same
    // distancefunction!!! (e.g.dim selecting df!)
    if(!distanceFunction.getClass().equals(this.distanceFunction.getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + this.distanceQuery.getClass() + ", but is " + distanceFunction.getClass());
    }
  }

  protected RdKNNLeafEntry<D> createNewLeafEntry(DBID id) {
    return new RdKNNLeafEntry<D>(id, relation.get(id), null);
  }

  /**
   * Inserts the specified real vector object into this index.
   * 
   * @param id the object id that was inserted
   */
  @Override
  public final void insert(DBID id) {
    insertLeaf(createNewLeafEntry(id));
  }

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param objects the objects to be inserted
   */
  @Override
  public final void insertAll(DBIDs ids) {
    if(ids.isEmpty() || (ids.size() == 1)) {
      return;
    }

    // Make an example leaf
    if(canBulkLoad()) {
      List<RdKNNEntry<D>> leafs = new ArrayList<RdKNNEntry<D>>(ids.size());
      for(DBID id : ids) {
        leafs.add(createNewLeafEntry(id));
      }
      bulkLoad(leafs);
    }
    else {
      for(DBID id : ids) {
        insert(id);
      }
    }

    doExtraIntegrityChecks();
  }

  /**
   * Deletes the specified object from this index.
   * 
   * @return true if this index did contain the object with the specified id,
   *         false otherwise
   */
  @Override
  public final boolean delete(DBID id) {
    // find the leaf node containing o
    O obj = relation.get(id);
    IndexTreePath<RdKNNEntry<D>> deletionPath = findPathToObject(getRootPath(), obj, id);
    if(deletionPath == null) {
      return false;
    }
    deletePath(deletionPath);
    return true;
  }

  @Override
  public void deleteAll(DBIDs ids) {
    for(DBID id : ids) {
      delete(id);
    }
  }

  @Override
  public <T extends Distance<T>> RangeQuery<O, T> getRangeQuery(DistanceQuery<O, T> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    SpatialDistanceQuery<O, T> dq = (SpatialDistanceQuery<O, T>) distanceQuery;
    return RStarTreeUtil.getRangeQuery(this, dq, hints);
  }

  @Override
  public <T extends Distance<T>> KNNQuery<O, T> getKNNQuery(DistanceQuery<O, T> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    SpatialDistanceQuery<O, T> dq = (SpatialDistanceQuery<O, T>) distanceQuery;
    return RStarTreeUtil.getKNNQuery(this, dq, hints);
  }

  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // FIXME: re-add
    return null;
  }

  @Override
  public String getLongName() {
    return "RdKNNTree";
  }

  @Override
  public String getShortName() {
    return "rdknntree";
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}