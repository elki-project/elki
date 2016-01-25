package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.set.TIntSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.KNNJoin;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTreeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * DeliClu: Density-Based Hierarchical Clustering, a hierarchical algorithm to
 * find density-connected sets in a database.
 * <p>
 * Reference: <br>
 * E. Achtert, C. Böhm, P. Kröger: DeLiClu: Boosting Robustness, Completeness,
 * Usability, and Efficiency of Hierarchical Clustering by a Closest Pair
 * Ranking. <br>
 * In Proc. 10th Pacific-Asia Conference on Knowledge Discovery and Data Mining
 * (PAKDD 2006), Singapore, 2006.
 * </p>
 *
 * @author Elke Achtert
 * @since 0.7.0
 * @param <NV> the type of NumberVector handled by this Algorithm
 */
@Title("DeliClu: Density-Based Hierarchical Clustering")
@Description("Hierachical algorithm to find density-connected sets in a database based on the parameter 'minpts'.")
@Reference(authors = "E. Achtert, C. Böhm, P. Kröger", //
title = "DeLiClu: Boosting Robustness, Completeness, Usability, and Efficiency of Hierarchical Clustering by a Closest Pair Ranking", //
booktitle = "Proc. 10th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD 2006), Singapore, 2006", //
url = "http://dx.doi.org/10.1007/11731139_16")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.clustering.DeLiClu" })
public class DeLiClu<NV extends NumberVector> extends AbstractDistanceBasedAlgorithm<NV, ClusterOrder>implements OPTICSTypeAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DeLiClu.class);

  /**
   * The priority queue for the algorithm.
   */
  private UpdatableHeap<SpatialObjectPair> heap;

  /**
   * Holds the knnJoin algorithm.
   */
  private KNNJoin<NV, DeLiCluNode, DeLiCluEntry> knnJoin;

  /**
   * Density threshold in number of objects.
   */
  private int minpts;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param minpts MinPts
   */
  public DeLiClu(DistanceFunction<? super NV> distanceFunction, int minpts) {
    super(distanceFunction);
    this.knnJoin = new KNNJoin<>(distanceFunction, minpts);
    this.minpts = minpts;
  }

  public ClusterOrder run(Database database, Relation<NV> relation) {
    Collection<DeLiCluTreeIndex<NV>> indexes = ResultUtil.filterResults(database.getHierarchy(), relation, DeLiCluTreeIndex.class);
    if(indexes.size() != 1) {
      throw new AbortException("DeLiClu found " + indexes.size() + " DeLiCluTree indexes. DeLiClu needs a special index to operate, therefore you need to add this index to your database.");
    }
    DeLiCluTreeIndex<NV> index = indexes.iterator().next();
    // FIXME: check that the index matches the relation!

    if(!(getDistanceFunction() instanceof SpatialPrimitiveDistanceFunction<?>)) {
      throw new IllegalArgumentException("Distance Function must be an instance of " + SpatialPrimitiveDistanceFunction.class.getName());
    }
    @SuppressWarnings("unchecked")
    SpatialPrimitiveDistanceFunction<NV> distFunction = (SpatialPrimitiveDistanceFunction<NV>) getDistanceFunction();

    // first do the knn-Join
    if(LOG.isVerbose()) {
      LOG.verbose("knnJoin...");
    }
    DataStore<KNNList> knns = knnJoin.run(relation);
    DBIDs ids = relation.getDBIDs();
    final int size = ids.size();

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("DeLiClu", size, LOG) : null;

    ClusterOrder clusterOrder = new ClusterOrder(ids, "DeLiClu Clustering", "deliclu-clustering");
    heap = new UpdatableHeap<>();

    // add start object to cluster order and (root, root) to priority queue
    DBID startID = DBIDUtil.deref(ids.iter());
    clusterOrder.add(startID, Double.POSITIVE_INFINITY, null);
    int numHandled = 1;
    index.setHandled(startID, relation.get(startID));
    SpatialDirectoryEntry rootEntry = (SpatialDirectoryEntry) index.getRootEntry();
    SpatialObjectPair spatialObjectPair = new SpatialObjectPair(0., rootEntry, rootEntry, true);
    heap.add(spatialObjectPair);

    while(numHandled < size) {
      if(heap.isEmpty()) {
        throw new AbortException("DeLiClu heap was empty when it shouldn't have been.");
      }
      SpatialObjectPair dataPair = heap.poll();

      // pair of nodes
      if(dataPair.isExpandable) {
        expandNodes(index, distFunction, dataPair, knns);
      }
      // pair of objects
      else {
        // set handled
        LeafEntry e1 = (LeafEntry) dataPair.entry1;
        LeafEntry e2 = (LeafEntry) dataPair.entry2;
        final DBID e1id = e1.getDBID();
        IndexTreePath<DeLiCluEntry> path = index.setHandled(e1id, relation.get(e1id));
        if(path == null) {
          throw new RuntimeException("snh: parent(" + e1id + ") = null!!!");
        }
        // add to cluster order
        clusterOrder.add(e1id, dataPair.distance, e2.getDBID());
        numHandled++;
        // reinsert expanded leafs
        reinsertExpanded(distFunction, index, path, knns);

        if(progress != null) {
          progress.setProcessed(numHandled, LOG);
        }
      }
    }
    LOG.ensureCompleted(progress);
    return clusterOrder;
  }

  /**
   * Expands the spatial nodes of the specified pair.
   *
   * @param index the index storing the objects
   * @param distFunction the spatial distance function of this algorithm
   * @param nodePair the pair of nodes to be expanded
   * @param knns the knn list
   */
  private void expandNodes(DeLiCluTree index, SpatialPrimitiveDistanceFunction<NV> distFunction, SpatialObjectPair nodePair, DataStore<KNNList> knns) {
    DeLiCluNode node1 = index.getNode(((SpatialDirectoryEntry) nodePair.entry1).getPageID());
    DeLiCluNode node2 = index.getNode(((SpatialDirectoryEntry) nodePair.entry2).getPageID());

    if(node1.isLeaf()) {
      expandLeafNodes(distFunction, node1, node2, knns);
    }
    else {
      expandDirNodes(distFunction, node1, node2);
    }

    index.setExpanded(nodePair.entry2, nodePair.entry1);
  }

  /**
   * Expands the specified directory nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param node1 the first node
   * @param node2 the second node
   */
  private void expandDirNodes(SpatialPrimitiveDistanceFunction<NV> distFunction, DeLiCluNode node1, DeLiCluNode node2) {
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("ExpandDirNodes: " + node1.getPageID() + " + " + node2.getPageID());
    }
    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of unhandled - handled children of
    // node1-node2 into pq
    for(int i = 0; i < numEntries_1; i++) {
      DeLiCluEntry entry1 = node1.getEntry(i);
      if(!entry1.hasUnhandled()) {
        continue;
      }
      for(int j = 0; j < numEntries_2; j++) {
        DeLiCluEntry entry2 = node2.getEntry(j);

        if(!entry2.hasHandled()) {
          continue;
        }
        double distance = distFunction.minDist(entry1, entry2);

        SpatialObjectPair nodePair = new SpatialObjectPair(distance, entry1, entry2, true);
        heap.add(nodePair);
      }
    }
  }

  /**
   * Expands the specified leaf nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param node1 the first node
   * @param node2 the second node
   * @param knns the knn list
   */
  private void expandLeafNodes(SpatialPrimitiveDistanceFunction<NV> distFunction, DeLiCluNode node1, DeLiCluNode node2, DataStore<KNNList> knns) {
    if(LOG.isDebuggingFinest()) {
      LOG.debugFinest("ExpandLeafNodes: " + node1.getPageID() + " + " + node2.getPageID());
    }
    int numEntries_1 = node1.getNumEntries();
    int numEntries_2 = node2.getNumEntries();

    // insert all combinations of unhandled - handled children of
    // node1-node2 into pq
    for(int i = 0; i < numEntries_1; i++) {
      DeLiCluEntry entry1 = node1.getEntry(i);
      if(!entry1.hasUnhandled()) {
        continue;
      }
      for(int j = 0; j < numEntries_2; j++) {
        DeLiCluEntry entry2 = node2.getEntry(j);
        if(!entry2.hasHandled()) {
          continue;
        }

        double distance = distFunction.minDist(entry1, entry2);
        double reach = MathUtil.max(distance, knns.get(((LeafEntry) entry2).getDBID()).getKNNDistance());
        SpatialObjectPair dataPair = new SpatialObjectPair(reach, entry1, entry2, false);
        heap.add(dataPair);
      }
    }
  }

  /**
   * Reinserts the objects of the already expanded nodes.
   *
   * @param distFunction the spatial distance function of this algorithm
   * @param index the index storing the objects
   * @param path the path of the object inserted last
   * @param knns the knn list
   */
  private void reinsertExpanded(SpatialPrimitiveDistanceFunction<NV> distFunction, DeLiCluTree index, IndexTreePath<DeLiCluEntry> path, DataStore<KNNList> knns) {
    int l = 0; // Count the number of components.
    for(IndexTreePath<DeLiCluEntry> it = path; it != null; it = it.getParentPath()) {
      l++;
    }
    ArrayList<IndexTreePath<DeLiCluEntry>> p = new ArrayList<>(l - 1);
    // All except the last (= root).
    IndexTreePath<DeLiCluEntry> it = path;
    for(; it.getParentPath() != null; it = it.getParentPath()) {
      p.add(it);
    }
    assert(p.size() == l - 1);
    DeLiCluEntry rootEntry = it.getEntry();
    reinsertExpanded(distFunction, index, p, l - 2, rootEntry, knns);
  }

  private void reinsertExpanded(SpatialPrimitiveDistanceFunction<NV> distFunction, DeLiCluTree index, List<IndexTreePath<DeLiCluEntry>> path, int pos, DeLiCluEntry parentEntry, DataStore<KNNList> knns) {
    DeLiCluNode parentNode = index.getNode(parentEntry);
    SpatialEntry entry2 = path.get(pos).getEntry();

    if(entry2.isLeafEntry()) {
      assert(pos == 0);
      for(int i = 0; i < parentNode.getNumEntries(); i++) {
        DeLiCluEntry entry1 = parentNode.getEntry(i);
        if(entry1.hasHandled()) {
          continue;
        }
        double distance = distFunction.minDist(entry1, entry2);
        double reach = MathUtil.max(distance, knns.get(((LeafEntry) entry2).getDBID()).getKNNDistance());
        SpatialObjectPair dataPair = new SpatialObjectPair(reach, entry1, entry2, false);
        heap.add(dataPair);
      }
      return;
    }
    TIntSet expanded = index.getExpanded(entry2);
    for(int i = 0; i < parentNode.getNumEntries(); i++) {
      DeLiCluDirectoryEntry entry1 = (DeLiCluDirectoryEntry) parentNode.getEntry(i);

      // not yet expanded
      if(!expanded.contains(entry1.getPageID())) {
        double distance = distFunction.minDist(entry1, entry2);
        SpatialObjectPair nodePair = new SpatialObjectPair(distance, entry1, entry2, true);
        heap.add(nodePair);
      }

      // already expanded
      else {
        reinsertExpanded(distFunction, index, path, pos - 1, entry1, knns);
      }
    }
  }

  @Override
  public int getMinPts() {
    return minpts;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Encapsulates an entry in the cluster order.
   *
   * @apiviz.exclude
   */
  public class SpatialObjectPair implements Comparable<SpatialObjectPair> {
    /**
     * The first entry of this pair.
     */
    SpatialEntry entry1;

    /**
     * The second entry of this pair.
     */
    SpatialEntry entry2;

    /**
     * Indicates whether this pair is expandable or not.
     */
    boolean isExpandable;

    /**
     * The current distance.
     */
    double distance;

    /**
     * Creates a new entry with the specified parameters.
     *
     * @param entry1 the first entry of this pair
     * @param entry2 the second entry of this pair
     * @param isExpandable if true, this pair is expandable (a pair of nodes),
     *        otherwise this pair is not expandable (a pair of objects)
     */
    public SpatialObjectPair(double distance, SpatialEntry entry1, SpatialEntry entry2, boolean isExpandable) {
      this.distance = distance;
      this.entry1 = entry1;
      this.entry2 = entry2;
      this.isExpandable = isExpandable;
    }

    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p/>
     *
     * @param other the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(SpatialObjectPair other) {
      /*
       * if(this.entry1.getEntryID().compareTo(other.entry1.getEntryID()) > 0) {
       * return -1; }
       * if(this.entry1.getEntryID().compareTo(other.entry1.getEntryID()) < 0) {
       * return 1; }
       * if(this.entry2.getEntryID().compareTo(other.entry2.getEntryID()) > 0) {
       * return -1; }
       * if(this.entry2.getEntryID().compareTo(other.entry2.getEntryID()) < 0) {
       * return 1; } return 0;
       */
      // FIXME: inverted?
      return Double.compare(this.distance, other.distance);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
      if(!isExpandable) {
        return entry1 + " - " + entry2;
      }
      return "n_" + entry1 + " - n_" + entry2;
    }

    /** equals is used in updating the heap! */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      if(!(SpatialObjectPair.class.isInstance(obj))) {
        return false;
      }
      SpatialObjectPair other = (SpatialObjectPair) obj;
      if(!isExpandable) {
        return this.entry1.equals(other.entry1);
      }
      else {
        return this.entry1.equals(other.entry1) && this.entry2.equals(other.entry2);
      }
    }

    /** hashCode is used in updating the heap! */
    @Override
    public int hashCode() {
      final long prime = 2654435761L;
      if(!isExpandable) {
        return entry1.hashCode();
      }
      long result = 0;
      result = prime * result + ((entry1 == null) ? 0 : entry1.hashCode());
      result = prime * result + ((entry2 == null) ? 0 : entry2.hashCode());
      return (int) result;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<NV extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<NV> {
    /**
     * Parameter to specify the threshold for minimum number of points within a
     * cluster, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("deliclu.minpts", "Threshold for minimum number of points within a cluster.");

    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter minptsP = new IntParameter(MINPTS_ID);
      minptsP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected DeLiClu<NV> makeInstance() {
      return new DeLiClu<>(distanceFunction, minpts);
    }
  }
}