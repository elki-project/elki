/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.index.tree.spatial.rstarvariants.rdknn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.database.ids.*;
import elki.database.query.distance.DistancePrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.distance.SpatialDistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.query.rknn.RKNNQuery;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
import elki.index.DistancePriorityIndex;
import elki.index.DynamicIndex;
import elki.index.RKNNIndex;
import elki.index.tree.IndexTreePath;
import elki.index.tree.LeafEntry;
import elki.index.tree.TreeIndexHeader;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil;
import elki.logging.Logging;
import elki.persistent.PageFile;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.pairs.DoubleObjPair;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 * <p>
 * TODO: noch nicht fertig!!!
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @has - - - RdKNNNode
 * @has - - - RdKNNTreeHeader
 * @composed - - - RdkNNSettings
 *
 * @param <O> Object type
 */
// FIXME: currently does not yet return RKNNQuery objects!
public class RdKNNTree<O extends NumberVector> extends NonFlatRStarTree<RdKNNNode, RdKNNEntry, RdkNNSettings> implements DistancePriorityIndex<O>, RKNNIndex<O>, DynamicIndex {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RdKNNTree.class);

  /**
   * The distance function.
   */
  private SpatialDistanceQuery<O> distanceQuery;

  /**
   * Internal knn query object, for updating the rKNN.
   */
  protected KNNQuery<O> knnQuery;

  /**
   * The relation we query.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   *
   * @param relation Relation to index
   * @param pagefile Data storage
   * @param settings Tree settings
   */
  public RdKNNTree(Relation<O> relation, PageFile<RdKNNNode> pagefile, RdkNNSettings settings) {
    super(pagefile, settings);
    this.relation = relation;
    this.distanceQuery = settings.distanceFunction.instantiate(relation);
    this.knnQuery = relation.getKNNQuery(distanceQuery);
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  @Override
  protected void preInsert(RdKNNEntry entry) {
    KNNHeap knns_o = DBIDUtil.newHeap(settings.k_max);
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Performs necessary operations after deleting the specified object.
   */
  @Override
  protected void postDelete(RdKNNEntry entry) {
    // reverse knn of o
    ModifiableDoubleDBIDList rnns = DBIDUtil.newDistanceDBIDList();
    doReverseKNN(getRoot(), ((RdKNNLeafEntry) entry).getDBID(), rnns);
    // knn of rnn
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(rnns);
    ids.sort(); // Sort by ID, not by distance!
    double[] kdists = new double[ids.size()];
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      kdists[iter.getOffset()] = knnQuery.getKNNForDBID(iter, settings.k_max).getKNNDistance();
    }

    adjustKNNDistances(getRootEntry(), ids, kdists);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(List<RdKNNEntry> entries) {
    super.bulkLoad(entries);

    // adjust all knn distances
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(entries.size());
    for(RdKNNEntry entry : entries) {
      ids.add(((RdKNNLeafEntry) entry).getDBID());
    }
    ids.sort();
    double[] kdists = new double[ids.size()];
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      kdists[iter.getOffset()] = knnQuery.getKNNForDBID(iter, settings.k_max).getKNNDistance();
    }

    adjustKNNDistances(getRootEntry(), ids, kdists);
    // Optional test
    doExtraIntegrityChecks();
  }

  public DoubleDBIDList reverseKNNQuery(DBID oid, int k, SpatialPrimitiveDistance<? super O> distanceFunction, KNNQuery<O> knnQuery) {
    checkDistance(distanceFunction);
    if(k > settings.k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + settings.k_max);
    }

    // get candidates
    ModifiableDoubleDBIDList candidates = DBIDUtil.newDistanceDBIDList();
    doReverseKNN(getRoot(), oid, candidates);

    if(k == settings.k_max) {
      return candidates.sort();
    }

    // refinement of candidates, if k < k_max
    ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = candidates.iter(); iter.valid(); iter.advance()) {
      for(DoubleDBIDListIter qr = knnQuery.getKNNForDBID(iter, k).iter(); qr.valid(); qr.advance()) {
        if(DBIDUtil.equal(oid, qr)) {
          result.add(qr.doubleValue(), iter);
          break;
        }
      }
    }

    return result.sort();
  }

  @Override
  protected TreeIndexHeader createHeader() {
    return new RdKNNTreeHeader(getPageSize(), dirCapacity, leafCapacity, dirMinimum, leafCapacity, settings.k_max);
  }

  @Override
  protected void initializeCapacities(RdKNNEntry exampleLeaf) {
    int dimensionality = exampleLeaf.getDimensionality();
    int distanceSize = ByteArrayUtil.SIZE_DOUBLE;

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
  protected List<DoubleObjPair<RdKNNEntry>> getSortedEntries(AbstractRStarTreeNode<?, ?> node, SpatialComparable q, SpatialPrimitiveDistance<?> distanceFunction) {
    List<DoubleObjPair<RdKNNEntry>> result = new ArrayList<>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      RdKNNEntry entry = (RdKNNEntry) node.getEntry(i);
      double minDist = distanceFunction.minDist(entry, q);
      result.add(new DoubleObjPair<>(minDist, entry));
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
  private void preInsert(RdKNNEntry q, RdKNNEntry nodeEntry, KNNHeap knns_q) {
    double knnDist_q = knns_q.getKNNDistance();
    RdKNNNode node = getNode(nodeEntry);
    double knnDist_node = 0.;

    // leaf node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry p = (RdKNNLeafEntry) node.getEntry(i);
        double dist_pq = distanceQuery.distance(p.getDBID(), ((LeafEntry) q).getDBID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if(dist_pq <= knnDist_q) {
          knns_q.insert(dist_pq, p.getDBID());
          if(knns_q.size() >= settings.k_max) {
            knnDist_q = knns_q.getKNNDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq <= p.getKnnDistance()) {
          KNNList knns_without_q = knnQuery.getKNNForObject(relation.get(p.getDBID()), settings.k_max);
          p.setKnnDistance(knns_without_q.size() + 1 < settings.k_max ? Double.NaN : //
              Math.min(knns_without_q.doubleValue(knns_without_q.size() - 1), dist_pq));
        }
        knnDist_node = Math.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      O obj = relation.get(((LeafEntry) q).getDBID());
      List<DoubleObjPair<RdKNNEntry>> entries = getSortedEntries(node, obj, settings.distanceFunction);
      for(DoubleObjPair<RdKNNEntry> distEntry : entries) {
        RdKNNEntry entry = distEntry.second;
        double entry_knnDist = entry.getKnnDistance();

        if(distEntry.first < entry_knnDist || distEntry.first < knnDist_q) {
          preInsert(q, entry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = Math.max(knnDist_node, entry.getKnnDistance());
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
  private void doReverseKNN(RdKNNNode node, DBID oid, ModifiableDoubleDBIDList result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry entry = (RdKNNLeafEntry) node.getEntry(i);
        double distance = distanceQuery.distance(entry.getDBID(), oid);
        if(distance <= entry.getKnnDistance()) {
          result.add(distance, entry.getDBID());
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry entry = (RdKNNDirectoryEntry) node.getEntry(i);
        double minDist = distanceQuery.minDist(entry, oid);
        if(minDist <= entry.getKnnDistance()) {
          doReverseKNN(getNode(entry), oid, result);
        }
      }
    }
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry the root entry of the current subtree
   * @param ids <em>Sorted</em> list of IDs
   * @param kdists knn distances
   */
  private void adjustKNNDistances(RdKNNEntry entry, ArrayDBIDs ids, double[] kdists) {
    RdKNNNode node = getNode(entry);
    double knnDist_node = 0.;
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry leafEntry = node.getEntry(i);
        DBID id = ((LeafEntry) leafEntry).getDBID();
        int pos = ids.binarySearch(id);
        if(pos >= 0) {
          leafEntry.setKnnDistance(kdists[pos]);
        }
        knnDist_node = Math.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry dirEntry = node.getEntry(i);
        adjustKNNDistances(dirEntry, ids, kdists);
        knnDist_node = Math.max(knnDist_node, dirEntry.getKnnDistance());
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
  protected RdKNNNode createNewLeafNode() {
    return new RdKNNNode(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @return a new directory node
   */
  @Override
  protected RdKNNNode createNewDirectoryNode() {
    return new RdKNNNode(dirCapacity, false);
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   */
  @Override
  protected RdKNNEntry createNewDirectoryEntry(RdKNNNode node) {
    return new RdKNNDirectoryEntry(node.getPageID(), node.computeMBR(), node.kNNDistance());
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  @Override
  protected RdKNNEntry createRootEntry() {
    return new RdKNNDirectoryEntry(0, null, Double.NaN);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not an instance of the distance function used by this index.
   *
   * @throws IllegalArgumentException
   * @param distanceFunction the distance function to be checked
   */
  private void checkDistance(SpatialPrimitiveDistance<? super O> distanceFunction) {
    if(!settings.distanceFunction.equals(distanceFunction)) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + this.distanceQuery.getClass() + ", but is " + distanceFunction.getClass());
    }
  }

  protected RdKNNLeafEntry createNewLeafEntry(DBID id) {
    return new RdKNNLeafEntry(id, relation.get(id), Double.NaN);
  }

  @Override
  public void initialize() {
    super.initialize();
    insertAll(relation.getDBIDs());
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
   * @param ids the objects to be inserted
   */
  @Override
  public final void insertAll(DBIDs ids) {
    if(ids.isEmpty() || (ids.size() == 1)) {
      return;
    }

    // Make an example leaf
    if(canBulkLoad()) {
      List<RdKNNEntry> leafs = new ArrayList<>(ids.size());
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        leafs.add(createNewLeafEntry(DBIDUtil.deref(iter)));
      }
      bulkLoad(leafs);
    }
    else {
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
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
    IndexTreePath<RdKNNEntry> deletionPath = findPathToObject(getRootPath(), obj, id);
    if(deletionPath == null) {
      return false;
    }
    deletePath(deletionPath);
    return true;
  }

  @Override
  public void deleteAll(DBIDs ids) {
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      delete(DBIDUtil.deref(iter));
    }
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    return RStarTreeUtil.getRangeQuery(this, (SpatialDistanceQuery<O>) distanceQuery, hints);
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    return RStarTreeUtil.getKNNQuery(this, (SpatialDistanceQuery<O>) distanceQuery, hints);
  }

  @Override
  public DistancePrioritySearcher<O> getPriorityQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    // Can we support this distance function - spatial distances only!
    if(!(distanceQuery instanceof SpatialDistanceQuery)) {
      return null;
    }
    SpatialDistanceQuery<O> dq = (SpatialDistanceQuery<O>) distanceQuery;
    return RStarTreeUtil.getDistancePrioritySearcher(this, dq, hints);
  }

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
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
