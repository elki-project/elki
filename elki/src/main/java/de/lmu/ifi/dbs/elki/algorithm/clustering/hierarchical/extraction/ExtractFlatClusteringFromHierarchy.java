package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction;
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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Extract a flat clustering from a full hierarchy, represented in pointer form.
 *
 * FIXME: re-check tie handling!
 *
 * TODO: add an hierarchy simplification step.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @apiviz.uses HierarchicalClusteringAlgorithm
 * @apiviz.uses PointerHierarchyRepresentationResult
 */
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.ExtractFlatClusteringFromHierarchy")
public class ExtractFlatClusteringFromHierarchy implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ExtractFlatClusteringFromHierarchy.class);

  /**
   * Threshold mode.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static enum ThresholdMode {
    /** Cut by minimum number of clusters */
    BY_MINCLUSTERS,
    /** Cut by threshold */
    BY_THRESHOLD,
    /** No thresholding */
    NO_THRESHOLD,
  }

  /**
   * Minimum number of clusters to extract
   */
  private final int minclusters;

  /**
   * Threshold for extracting clusters.
   */
  private final double threshold;

  /**
   * Include empty clusters in the hierarchy produced.
   */
  private final boolean hierarchical;

  /**
   * Merge singleton clusters into the parent cluster.
   */
  private final boolean nosingletons;

  /**
   * Clustering algorithm to run to obtain the hierarchy.
   */
  private final HierarchicalClusteringAlgorithm algorithm;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param minclusters Minimum number of clusters
   * @param hierarchical Produce a hierarchical output
   * @param nosingletons Merge singletons into parent
   */
  public ExtractFlatClusteringFromHierarchy(HierarchicalClusteringAlgorithm algorithm, int minclusters, boolean hierarchical, boolean nosingletons) {
    super();
    this.algorithm = algorithm;
    this.threshold = Double.NaN;
    this.minclusters = minclusters;
    this.hierarchical = hierarchical;
    this.nosingletons = nosingletons;
  }

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param threshold Distance threshold
   * @param hierarchical Produce a hierarchical output
   * @param nosingletons Merge singletons into parent
   */
  public ExtractFlatClusteringFromHierarchy(HierarchicalClusteringAlgorithm algorithm, double threshold, boolean hierarchical, boolean nosingletons) {
    super();
    this.algorithm = algorithm;
    this.threshold = threshold;
    this.minclusters = -1;
    this.hierarchical = hierarchical;
    this.nosingletons = nosingletons;
  }

  @Override
  public Clustering<DendrogramModel> run(Database database) {
    PointerHierarchyRepresentationResult pointerresult = algorithm.run(database);
    DBIDs ids = pointerresult.getDBIDs();
    DBIDDataStore pi = pointerresult.getParentStore();
    DoubleDataStore lambda = pointerresult.getParentDistanceStore();

    Clustering<DendrogramModel> result = extractClusters(ids, pi, lambda);
    result.addChildResult(pointerresult);

    return result;
  }

  /**
   * Extract all clusters from the pi-lambda-representation.
   *
   * @param ids Object ids to process
   * @param pi Pi store
   * @param lambda Lambda store
   *
   * @return Hierarchical clustering
   */
  public Clustering<DendrogramModel> extractClusters(DBIDs ids, final DBIDDataStore pi, final DoubleDataStore lambda) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), LOG) : null;

    // Sort DBIDs by lambda. We need this for two things:
    // a) to determine the stop distance from "minclusters" parameter
    // b) to process arrows in decreasing / increasing order
    ArrayDBIDs order = PointerHierarchyRepresentationResult.topologicalSort(ids, pi, lambda);
    DBIDArrayIter it = order.iter(); // Used multiple times!

    final int split = findSplit(order, it, lambda);

    // Extract the child clusters
    final int expcnum = ids.size() - split;
    WritableIntegerDataStore cluster_map = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, -1);
    ArrayList<ModifiableDBIDs> cluster_dbids = new ArrayList<>(expcnum + 10);
    DoubleArray cluster_dist = new DoubleArray(expcnum + 10);
    ArrayModifiableDBIDs cluster_leads = DBIDUtil.newArray(expcnum + 10);

    DBIDVar succ = DBIDUtil.newVar(); // Variable for successor.
    // Go backwards on the lower part.
    for(it.seek(split - 1); it.valid(); it.retract()) {
      double dist = lambda.doubleValue(it); // Distance to successor
      pi.assignVar(it, succ); // succ = pi(it)
      int clusterid = cluster_map.intValue(succ);
      // Successor cluster has already been created:
      if(clusterid >= 0) {
        cluster_dbids.get(clusterid).add(it);
        cluster_map.putInt(it, clusterid);
        // Update distance to maximum encountered:
        if(cluster_dist.get(clusterid) < dist) {
          cluster_dist.set(clusterid, dist);
        }
      }
      else {
        // Need to start a new cluster:
        clusterid = cluster_dbids.size(); // next cluster number.
        ModifiableDBIDs cids = DBIDUtil.newArray();
        // Add element and successor as initial members:
        cids.add(succ);
        cluster_map.putInt(succ, clusterid);
        cids.add(it);
        cluster_map.putInt(it, clusterid);
        // Store new cluster.
        cluster_dbids.add(cids);
        cluster_leads.add(succ);
        cluster_dist.add(dist);
      }

      // Decrement counter
      LOG.incrementProcessed(progress);
    }
    final Clustering<DendrogramModel> dendrogram;
    if(hierarchical) {
      // Build a hierarchy out of these clusters.
      dendrogram = new Clustering<>("Hierarchical Clustering", "hierarchical-clustering");
      Cluster<DendrogramModel> root = null;
      ArrayList<Cluster<DendrogramModel>> clusters = new ArrayList<>(expcnum);
      // Convert initial clusters to cluster objects
      {
        int i = 0;
        for(DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
          double depth = cluster_dist.get(i);
          clusters.add(makeCluster(it2, depth, cluster_dbids.get(i)));
        }
        cluster_dist = null; // Invalidate
        cluster_dbids = null; // Invalidate
      }
      // Process the upper part, bottom-up.
      for(it.seek(split); it.valid(); it.advance()) {
        int clusterid = cluster_map.intValue(it);
        // The current cluster led by the current element:
        final Cluster<DendrogramModel> clus;
        if(clusterid >= 0) {
          clus = clusters.get(clusterid);
        }
        else if(nosingletons && ids.size() > 1) {
          clus = null;
        }
        else {
          clus = makeCluster(it, Double.NaN, DBIDUtil.deref(it));
        }
        // The successor to join:
        pi.assignVar(it, succ); // succ = pi(it)
        if(DBIDUtil.equal(it, succ)) {
          assert (root == null);
          root = clus;
          LOG.incrementProcessed(progress);
          continue;
        }
        // Parent cluster:
        int parentid = cluster_map.intValue(succ);
        double depth = lambda.doubleValue(it);
        // Parent cluster exists - merge as a new cluster:
        if(parentid >= 0) {
          final Cluster<DendrogramModel> pclus = clusters.get(parentid);
          if(pclus.getModel().getDistance() == depth) {
            if(clus == null) {
              ((ModifiableDBIDs) pclus.getIDs()).add(it);
            }
            else {
              dendrogram.addChildCluster(pclus, clus);
            }
          }
          else {
            // Merge at new depth:
            ModifiableDBIDs cids = DBIDUtil.newArray(clus == null ? 1 : 0);
            if(clus == null) {
              cids.add(it);
            }
            Cluster<DendrogramModel> npclus = makeCluster(succ, depth, cids);
            if(clus != null) {
              dendrogram.addChildCluster(npclus, clus);
            }
            dendrogram.addChildCluster(npclus, pclus);
            // Replace existing parent cluster: new depth
            clusters.set(parentid, npclus);
          }
        }
        else {
          // Merge with parent at this depth:
          final Cluster<DendrogramModel> pclus;
          if(nosingletons) {
            ModifiableDBIDs cids = DBIDUtil.newArray(clus == null ? 2 : 1);
            cids.add(succ);
            if(clus == null) {
              cids.add(it);
            }
            // New cluster for parent and/or new point
            pclus = makeCluster(succ, depth, cids);
          }
          else {
            // Create a new, one-element cluster for parent, and a merged
            // cluster on top.
            pclus = makeCluster(succ, depth, DBIDUtil.EMPTYDBIDS);
            dendrogram.addChildCluster(pclus, makeCluster(succ, Double.NaN, DBIDUtil.deref(succ)));
          }
          if(clus != null) {
            dendrogram.addChildCluster(pclus, clus);
          }
          // Store cluster:
          parentid = clusters.size();
          clusters.add(pclus); // Remember parent cluster
          cluster_map.putInt(succ, parentid); // Reference
        }

        // Decrement counter
        LOG.incrementProcessed(progress);
      }
      assert (root != null);
      // attach root
      dendrogram.addToplevelCluster(root);
    }
    else {
      // Build a hierarchy out of these clusters.
      dendrogram = new Clustering<>("Flattened Hierarchical Clustering", "flattened-hierarchical-clustering");
      // Convert initial clusters to cluster objects
      {
        int i = 0;
        for(DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
          double depth = cluster_dist.get(i);
          dendrogram.addToplevelCluster(makeCluster(it2, depth, cluster_dbids.get(i)));
        }
        cluster_dist = null; // Invalidate
      }
      if(nosingletons) {
        // Convert singletons.
        for(it.seek(split); it.valid(); it.advance()) {
          int parentid = cluster_map.intValue(succ);
          if(parentid >= 0) {
            cluster_dbids.get(parentid).add(it);
            cluster_map.put(it, parentid);
          }
        }
      }
      // Process the upper part, bottom-up.
      for(it.seek(split); it.valid(); it.advance()) {
        int clusterid = cluster_map.intValue(it);
        if(clusterid < 0) {
          dendrogram.addToplevelCluster(makeCluster(it, Double.NaN, DBIDUtil.deref(it)));
        }

        // Decrement counter
        LOG.incrementProcessed(progress);
      }
      cluster_dbids = null; // Invalidate
    }
    LOG.ensureCompleted(progress);

    return dendrogram;
  }

  /**
   * Find the splitting point in the ordered DBIDs list.
   *
   * @param order Ordered list
   * @param it Iterator on this list (reused)
   * @param lambda Join distances.
   * @return Splitting point
   */
  private int findSplit(ArrayDBIDs order, DBIDArrayIter it, DoubleDataStore lambda) {
    int split;
    if(minclusters > 0) {
      split = order.size() > minclusters ? order.size() - minclusters : 0;
      it.seek(split);
      // Stop distance:
      final double stopdist = lambda.doubleValue(it);

      // Tie handling: decrement split.
      for(it.retract(); it.valid() && stopdist <= lambda.doubleValue(it); it.retract()) {
        split--;
      }
    }
    else if(!Double.isNaN(threshold)) {
      split = order.size();
      it.seek(split - 1);
      while(it.valid() && threshold <= lambda.doubleValue(it)) {
        split--;
        it.retract();
      }
    }
    else { // full hierarchy
      split = 0;
    }
    return split;
  }

  /**
   * Make the cluster for the given object
   *
   * @param lead Leading object
   * @param depth Linkage depth
   * @param members Member objects
   * @return Cluster
   */
  private Cluster<DendrogramModel> makeCluster(DBIDRef lead, double depth, DBIDs members) {
    final String name;
    if(members.size() == 0) {
      name = "mrg_" + DBIDUtil.toString(lead) + "_" + depth;
    }
    else if(!Double.isNaN(depth) && Double.isInfinite(depth) || (members.size() == 1 && members.contains(lead))) {
      name = "obj_" + DBIDUtil.toString(lead);
    }
    else if(!Double.isNaN(depth)) {
      name = "clu_" + DBIDUtil.toString(lead) + "_" + depth;
    }
    else {
      // Complete data set only?
      name = "clu_" + DBIDUtil.toString(lead);
    }
    Cluster<DendrogramModel> cluster = new Cluster<>(name, members, new DendrogramModel(depth));
    return cluster;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Extraction mode to use.
     */
    public static final OptionID MODE_ID = new OptionID("hierarchical.threshold-mode", "The thresholding mode to use for extracting clusters: by desired number of clusters, or by distance threshold.");

    /**
     * The minimum number of clusters to extract.
     */
    public static final OptionID MINCLUSTERS_ID = new OptionID("hierarchical.minclusters", "The minimum number of clusters to extract (there may be more clusters when tied).");

    /**
     * The threshold level for which to extract the clustering.
     */
    public static final OptionID THRESHOLD_ID = new OptionID("hierarchical.threshold", "The threshold level for which to extract the clusters.");

    /**
     * Parameter to configure the output mode (nested or truncated clusters).
     */
    public static final OptionID HIERARCHICAL_ID = new OptionID("hierarchical.hierarchy", "Generate a truncated hierarchical clustering result (or strict partitions).");

    /**
     * Flag to produce singleton clusters.
     */
    public static final OptionID NO_SINGLETONS_ID = new OptionID("hierarchical.mergesingletons", "Merge singleton clusters into parent. This produces a more complex hierarchy, but that is easier to understand.");

    /**
     * Threshold mode.
     */
    ThresholdMode thresholdmode = null;

    /**
     * Number of clusters to extract.
     */
    int minclusters = -1;

    /**
     * Threshold level.
     */
    double threshold = Double.NaN;

    /**
     * Flag to generate a hierarchical result.
     */
    boolean hierarchical = false;

    /**
     * Also create singleton clusters.
     */
    boolean nosingletons = false;

    /**
     * The hierarchical clustering algorithm to run.
     */
    HierarchicalClusteringAlgorithm algorithm;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<HierarchicalClusteringAlgorithm> algorithmP = new ObjectParameter<>(AlgorithmStep.Parameterizer.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class);
      if(config.grab(algorithmP)) {
        algorithm = algorithmP.instantiateClass(config);
      }

      EnumParameter<ThresholdMode> modeP = new EnumParameter<>(MODE_ID, ThresholdMode.class, ThresholdMode.BY_MINCLUSTERS);
      if(config.grab(modeP)) {
        thresholdmode = modeP.getValue();
      }

      if(thresholdmode == null || ThresholdMode.BY_MINCLUSTERS.equals(thresholdmode)) {
        IntParameter minclustersP = new IntParameter(MINCLUSTERS_ID) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(minclustersP)) {
          minclusters = minclustersP.intValue();
        }
      }

      if(thresholdmode == null || ThresholdMode.BY_THRESHOLD.equals(thresholdmode)) {
        DoubleParameter distP = new DoubleParameter(THRESHOLD_ID);
        if(config.grab(distP)) {
          threshold = distP.getValue();
        }
      }

      if(thresholdmode == null || !ThresholdMode.NO_THRESHOLD.equals(thresholdmode)) {
        Flag hierarchicalF = new Flag(HIERARCHICAL_ID);
        if(config.grab(hierarchicalF)) {
          hierarchical = hierarchicalF.isTrue();
        }
      }
      else {
        // This becomes full hierarchy:
        minclusters = -1;
        hierarchical = true;
      }

      Flag singletonsF = new Flag(NO_SINGLETONS_ID);
      if(config.grab(singletonsF)) {
        nosingletons = singletonsF.isTrue();
      }
    }

    @Override
    protected ExtractFlatClusteringFromHierarchy makeInstance() {
      switch(thresholdmode){
      case NO_THRESHOLD:
      case BY_MINCLUSTERS:
        return new ExtractFlatClusteringFromHierarchy(algorithm, minclusters, hierarchical, nosingletons);
      case BY_THRESHOLD:
        return new ExtractFlatClusteringFromHierarchy(algorithm, threshold, hierarchical, nosingletons);
      default:
        throw new AbortException("Unknown extraction mode.");
      }
    }
  }
}
