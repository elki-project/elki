package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
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
  private static final Logging LOG = Logging.getLogger(RdKNNTree.class);

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
    this.initialize();
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   * 
   * @param entry the entry to be inserted
   */
  @Override
  protected void preInsert(RdKNNEntry<D> entry) {
    KNNHeap<D> knns_o = KNNUtil.newHeap(distanceFunction, k_max);
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Performs necessary operations after deleting the specified object.
   */
  @Override
  protected void postDelete(RdKNNEntry<D> entry) {
    // reverse knn of o
    GenericDistanceDBIDList<D> rnns = new GenericDistanceDBIDList<D>();
    doReverseKNN(getRoot(), ((RdKNNLeafEntry<D>) entry).getDBID(), rnns);

    // knn of rnn
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(rnns);
    ids.sort();
    List<? extends KNNResult<D>> knnLists = knnQuery.getKNNForBulkDBIDs(ids, k_max);

    // adjust knn distances
    adjustKNNDistance(getRootEntry(), ids, knnLists);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(List<RdKNNEntry<D>> entries) {
    super.bulkLoad(entries);

    // adjust all knn distances
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(entries.size());
    for(RdKNNEntry<D> entry : entries) {
      DBID id = ((RdKNNLeafEntry<D>) entry).getDBID();
      ids.add(id);
    }
    ids.sort();
    List<? extends KNNResult<D>> knnLists = knnQuery.getKNNForBulkDBIDs(ids, k_max);
    adjustKNNDistance(getRootEntry(), ids, knnLists);

    // test
    doExtraIntegrityChecks();
  }

  public DistanceDBIDResult<D> reverseKNNQuery(DBID oid, int k, SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction, KNNQuery<O, D> knnQuery) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    GenericDistanceDBIDList<D> candidates = new GenericDistanceDBIDList<D>();
    doReverseKNN(getRoot(), oid, candidates);

    if(k == k_max) {
      candidates.sort();
      return candidates;
    }

    // refinement of candidates, if k < k_max
    ArrayModifiableDBIDs candidateIDs = DBIDUtil.newArray(candidates);
    for(int i = 0; i < candidates.size(); i++) {
      DistanceDBIDPair<D> candidate = (DistanceDBIDPair<D>) candidates.get(i);
      candidateIDs.add(candidate);
    }
    candidateIDs.sort();
    List<? extends KNNResult<D>> knnLists = knnQuery.getKNNForBulkDBIDs(candidateIDs, k);

    GenericDistanceDBIDList<D> result = new GenericDistanceDBIDList<D>();
    int i = 0;
    for (DBIDIter iter = candidateIDs.iter(); iter.valid(); iter.advance(), i++) {
      for (DistanceDBIDResultIter<D> qr = knnLists.get(i).iter(); qr.valid(); qr.advance()) {
        if(DBIDUtil.equal(oid, qr)) {
          result.add(qr.getDistance(), iter);
          break;
        }
      }
    }

    result.sort();
    return result;
  }

  public List<GenericDistanceDBIDList<D>> bulkReverseKNNQueryForID(DBIDs ids, int k, SpatialPrimitiveDistanceFunction<? super O, D> distanceFunction, KNNQuery<O, D> knnQuery) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    Map<DBID, GenericDistanceDBIDList<D>> candidateMap = new HashMap<DBID, GenericDistanceDBIDList<D>>();
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      DBID id = DBIDUtil.deref(iter);
      candidateMap.put(id, new GenericDistanceDBIDList<D>());
    }
    doBulkReverseKNN(getRoot(), ids, candidateMap);

    if(k == k_max) {
      List<GenericDistanceDBIDList<D>> resultList = new ArrayList<GenericDistanceDBIDList<D>>();
      for(GenericDistanceDBIDList<D> candidates : candidateMap.values()) {
        candidates.sort();
        resultList.add(candidates);
      }
      return resultList;
    }

    // refinement of candidates, if k < k_max
    // perform a knn query for the candidates
    ArrayModifiableDBIDs candidateIDs = DBIDUtil.newArray();
    for(GenericDistanceDBIDList<D> candidates : candidateMap.values()) {
      for(int i = 0; i < candidates.size(); i++) {
        DistanceDBIDPair<D> candidate = (DistanceDBIDPair<D>) candidates.get(i);
        candidateIDs.add(candidate);
      }
    }
    candidateIDs.sort();
    List<? extends KNNResult<D>> knnLists = knnQuery.getKNNForBulkDBIDs(candidateIDs, k);

    // and add candidate c to the result if o is a knn of c
    List<GenericDistanceDBIDList<D>> resultList = new ArrayList<GenericDistanceDBIDList<D>>();
    for(DBID id : candidateMap.keySet()) {
      GenericDistanceDBIDList<D> candidates = candidateMap.get(id);
      GenericDistanceDBIDList<D> result = new GenericDistanceDBIDList<D>();
      for (DistanceDBIDResultIter<D> candidate = candidates.iter(); candidate.valid(); candidate.advance()) {
        int pos = candidateIDs.binarySearch(candidate);
        assert(pos >= 0);
        for (DistanceDBIDResultIter<D> qr = knnLists.get(pos).iter(); qr.valid(); qr.advance()) {
          if(DBIDUtil.equal(id, qr)) {
            result.add(qr.getDistance(), candidate);
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
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
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
      LOG.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if(leafMinimum < 2) {
      leafMinimum = 2;
    }

    if(LOG.isVerbose()) {
      LOG.verbose("Directory Capacity: " + dirCapacity + "\nLeaf Capacity: " + leafCapacity);
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
            knnDist_q = knns_q.getKNNDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          O obj = relation.get(p.getDBID());
          KNNResult<D> knns_without_q = knnQuery.getKNNForObject(obj, k_max);

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
  private void doReverseKNN(RdKNNNode<D> node, DBID oid, GenericDistanceDBIDList<D> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceQuery.distance(entry.getDBID(), oid);
        if(distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(distance, entry.getDBID());
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
  private void doBulkReverseKNN(RdKNNNode<D> node, DBIDs ids, Map<DBID, GenericDistanceDBIDList<D>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) node.getEntry(i);
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          DBID id = DBIDUtil.deref(iter);
          D distance = distanceQuery.distance(entry.getDBID(), id);
          if(distance.compareTo(entry.getKnnDistance()) <= 0) {
            result.get(id).add(distance, entry.getDBID());
          }
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D> entry = (RdKNNDirectoryEntry<D>) node.getEntry(i);
        ModifiableDBIDs candidates = DBIDUtil.newArray();
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          DBID id = DBIDUtil.deref(iter);
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
   * @param ids <em>Sorted</em> list of IDs
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustKNNDistance(RdKNNEntry<D> entry, ArrayDBIDs ids, List<? extends KNNResult<D>> knnLists) {
    RdKNNNode<D> node = getNode(entry);
    D knnDist_node = distanceQuery.getDistanceFactory().nullDistance();
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D> leafEntry = node.getEntry(i);
        DBID id = ((LeafEntry) leafEntry).getDBID();
        int pos = ids.binarySearch(id);
        if (pos >= 0) {
          leafEntry.setKnnDistance(knnLists.get(pos).getKNNDistance());
        }
        knnDist_node = DistanceUtil.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D> dirEntry = node.getEntry(i);
        adjustKNNDistance(dirEntry, ids, knnLists);
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
  public final void insert(DBIDRef id) {
    insertLeaf(createNewLeafEntry(DBIDUtil.deref(id)));
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
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        leafs.add(createNewLeafEntry(DBIDUtil.deref(iter)));
      }
      bulkLoad(leafs);
    }
    else {
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        insert(iter);
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
  public final boolean delete(DBIDRef id) {
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
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      delete(DBIDUtil.deref(iter));
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
    return LOG;
  }
}