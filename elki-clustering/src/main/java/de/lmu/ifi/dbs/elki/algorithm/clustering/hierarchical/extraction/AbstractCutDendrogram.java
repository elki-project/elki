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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerPrototypeHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.model.PrototypeDendrogramModel;
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for extracting clusters from dendrograms.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - runs - HierarchicalClusteringAlgorithm
 * @assoc - processes - PointerHierarchyRepresentationResult
 * @navassoc - produces - Clustering
 * @navassoc - produces - DendrogramModel
 */
public abstract class AbstractCutDendrogram implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Include empty clusters in the hierarchy produced.
   */
  protected final boolean hierarchical;

  /**
   * Clustering algorithm to run to obtain the hierarchy.
   */
  protected final HierarchicalClusteringAlgorithm algorithm;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm
   * @param hierarchical Extract hierarchical result
   */
  public AbstractCutDendrogram(HierarchicalClusteringAlgorithm algorithm, boolean hierarchical) {
    super();
    this.algorithm = algorithm;
    this.hierarchical = hierarchical;
  }

  @Override
  public Clustering<DendrogramModel> run(Database database) {
    PointerHierarchyRepresentationResult pointerresult = algorithm.run(database);
    return run(pointerresult);
  }

  /**
   * Process a pointer hierarchy result.
   * 
   * @param pointerresult Hierarchical result in pointer representation.
   * @return Clustering
   */
  abstract public Clustering<DendrogramModel> run(PointerHierarchyRepresentationResult pointerresult);

  /**
   * Instance for a single data set.
   * 
   * @author Erich Schubert
   */
  abstract public class Instance {
    /**
     * Unordered IDs
     */
    protected ArrayDBIDs ids;

    /**
     * Parent pointer
     */
    protected DBIDDataStore pi;

    /**
     * Merge distance
     */
    protected DoubleDataStore lambda;

    /**
     * The hierarchical result to process.
     */
    protected PointerHierarchyRepresentationResult pointerresult;

    /**
     * Map clusters to integer cluster numbers.
     */
    protected WritableIntegerDataStore cluster_map;

    /**
     * Storage for cluster contents
     */
    protected ArrayList<ModifiableDBIDs> cluster_dbids;

    /**
     * Cluster distances
     */
    protected DoubleArray clusterHeight;

    /**
     * Cluster lead objects
     */
    protected ArrayModifiableDBIDs cluster_leads;

    /**
     * Constructor.
     *
     * @param pointerresult Hierarchical result
     */
    public Instance(PointerHierarchyRepresentationResult pointerresult) {
      this.ids = pointerresult.topologicalSort();
      this.pi = pointerresult.getParentStore();
      this.lambda = pointerresult.getParentDistanceStore();
      this.pointerresult = pointerresult;
    }

    /**
     * Extract all clusters from the pi-lambda-representation.
     *
     * @return Hierarchical clustering
     */
    public Clustering<DendrogramModel> extractClusters() {
      final Logging log = getLogger();
      // Sort DBIDs topologically.
      DBIDArrayIter it = pointerresult.topologicalSort().iter();

      final int split = findSplit(it);

      // Extract the child clusters
      FiniteProgress progress = log.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), log) : null;
      // Initialize data structures:
      final int expcnum = ids.size() - split;
      cluster_map = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, -1);
      cluster_dbids = new ArrayList<>(expcnum + 10);
      clusterHeight = new DoubleArray(expcnum + 10);
      cluster_leads = DBIDUtil.newArray(expcnum + 10);

      buildLeafClusters(it, split, progress);
      Clustering<DendrogramModel> dendrogram = hierarchical ? //
          buildHierarchical(it, split, progress) : buildFlat(it, split, progress);
      log.ensureCompleted(progress);
      return dendrogram;
    }

    /**
     * Prepare the leaf clusters by executing the first (size - 1 - split)
     * merges.
     * 
     * @param it Iterator
     * @param split Splitting point
     * @param progress Progress for logging (may be {@code null})
     */
    private void buildLeafClusters(DBIDArrayIter it, final int split, FiniteProgress progress) {
      final Logging log = getLogger();
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
          if(clusterHeight.get(clusterid) < dist) {
            clusterHeight.set(clusterid, dist);
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
          clusterHeight.add(dist);
        }

        // Decrement counter
        log.incrementProcessed(progress);
      }
    }

    /**
     * Build a flat clustering.
     * 
     * @param it Ordered iterator
     * @param split Splitting point
     * @param progress Progress for logging (may be {@code null})
     * @return Clustering
     */
    private Clustering<DendrogramModel> buildFlat(DBIDArrayIter it, final int split, FiniteProgress progress) {
      final Logging log = getLogger();
      Clustering<DendrogramModel> dendrogram = new Clustering<>("Flattened Hierarchical Clustering", "flattened-hierarchical-clustering");
      // Convert initial clusters to cluster objects
      {
        int i = 0;
        for(DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
          dendrogram.addToplevelCluster(makeCluster(it2, clusterHeight.get(i), cluster_dbids.get(i)));
        }
        clusterHeight = null; // Invalidate
      }
      cluster_dbids = null; // Invalidate
      // Process the upper part, bottom-up.
      for(it.seek(split); it.valid(); it.advance()) {
        int clusterid = cluster_map.intValue(it);
        if(clusterid < 0) {
          dendrogram.addToplevelCluster(makeCluster(it, Double.NaN, DBIDUtil.deref(it)));
        }
        log.incrementProcessed(progress);
      }
      cluster_map = null; // Invalidate
      return dendrogram;
    }

    /**
     * Build a hierarchical clustering.
     * 
     * @param it Iterator
     * @param split Splitting point
     * @param progress Progress for logging (may be {@code null})
     * @return Clustering
     */
    private Clustering<DendrogramModel> buildHierarchical(DBIDArrayIter it, int split, FiniteProgress progress) {
      final int expcnum = ids.size() - split;
      final Logging log = getLogger();
      final Clustering<DendrogramModel> dendrogram;
      // Build a hierarchy out of these clusters.
      dendrogram = new Clustering<>("Hierarchical Clustering", "hierarchical-clustering");
      Cluster<DendrogramModel> root = null;
      ArrayList<Cluster<DendrogramModel>> clusters = new ArrayList<>(expcnum);
      // Convert initial clusters to cluster objects
      {
        int i = 0;
        for(DBIDIter it2 = cluster_leads.iter(); it2.valid(); it2.advance(), i++) {
          clusters.add(makeCluster(it2, clusterHeight.get(i), cluster_dbids.get(i)));
        }
        clusterHeight = null; // Invalidate
        cluster_dbids = null; // Invalidate
      }
      // Process the upper part, bottom-up.
      DBIDVar succ = DBIDUtil.newVar(); // Variable for successor.
      for(it.seek(split); it.valid(); it.advance()) {
        int clusterid = cluster_map.intValue(it);
        // The current cluster led by the current element:
        final Cluster<DendrogramModel> clus;
        if(clusterid >= 0) {
          clus = clusters.get(clusterid);
        }
        else {
          clus = makeCluster(it, Double.NaN, DBIDUtil.deref(it));
        }
        // The successor to join:
        pi.assignVar(it, succ); // succ = pi(it)
        if(DBIDUtil.equal(it, succ)) {
          assert (root == null);
          root = clus;
          log.incrementProcessed(progress);
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
          // Create a new, one-element cluster for parent, and a merged cluster
          // above it.
          pclus = makeCluster(succ, depth, DBIDUtil.EMPTYDBIDS);
          dendrogram.addChildCluster(pclus, makeCluster(succ, Double.NaN, DBIDUtil.deref(succ)));
          if(clus != null) {
            dendrogram.addChildCluster(pclus, clus);
          }
          // Store cluster:
          parentid = clusters.size();
          clusters.add(pclus); // Remember parent cluster
          cluster_map.putInt(succ, parentid); // Reference
        }

        // Decrement counter
        log.incrementProcessed(progress);
      }
      assert (root != null);
      cluster_map = null; // Invalidate
      // attach root
      dendrogram.addToplevelCluster(root);
      return dendrogram;
    }

    /**
     * Find the splitting point in the ordered DBIDs list.
     *
     * @param it Iterator on this list (reused)
     * @return Splitting point
     */
    abstract protected int findSplit(DBIDArrayIter it);

    /**
     * Make the cluster for the given object
     *
     * @param lead Leading object
     * @param depth Linkage depth
     * @param members Member objects
     * @return Cluster
     */
    protected Cluster<DendrogramModel> makeCluster(DBIDRef lead, double depth, DBIDs members) {
      final String name;
      if(members == null || (members.size() == 1 && members.contains(lead))) {
        name = "obj_" + DBIDUtil.toString(lead);
      }
      else if(members.size() == 0) {
        name = "mrg_" + DBIDUtil.toString(lead) + "_" + depth;
      }
      else if(depth < Double.POSITIVE_INFINITY) {
        name = "clu_" + DBIDUtil.toString(lead) + "_" + depth;
      }
      else {
        // Complete data set only?
        name = "top_" + DBIDUtil.toString(lead);
      }

      DendrogramModel model;
      if(members != null && !members.isEmpty() && pointerresult instanceof PointerPrototypeHierarchyRepresentationResult) {
        model = new PrototypeDendrogramModel(depth, ((PointerPrototypeHierarchyRepresentationResult) pointerresult).findPrototype(members));
      }
      else {
        model = new DendrogramModel(depth);
      }
      return new Cluster<>(name, members, model);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  abstract protected Logging getLogger();

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  abstract public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to configure the output mode (nested or truncated clusters).
     */
    public static final OptionID HIERARCHICAL_ID = new OptionID("hierarchical.hierarchy", "Generate a truncated hierarchical clustering result (or strict partitions).");

    /**
     * Flag to generate a hierarchical result.
     */
    boolean hierarchical = false;

    /**
     * The hierarchical clustering algorithm to run.
     */
    HierarchicalClusteringAlgorithm algorithm;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<HierarchicalClusteringAlgorithm> algorithmP = new ObjectParameter<>(AbstractAlgorithm.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class);
      if(config.grab(algorithmP)) {
        algorithm = algorithmP.instantiateClass(config);
      }
      Flag hierarchicalF = new Flag(HIERARCHICAL_ID);
      if(config.grab(hierarchicalF)) {
        hierarchical = hierarchicalF.isTrue();
      }
    }
  }
}
