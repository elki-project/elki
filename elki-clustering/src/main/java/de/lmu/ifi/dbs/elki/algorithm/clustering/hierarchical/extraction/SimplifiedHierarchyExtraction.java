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
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerDensityHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerPrototypeHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.model.PrototypeDendrogramModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.*;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Extraction of simplified cluster hierarchies, as proposed in HDBSCAN.
 * <p>
 * In contrast to the authors top-down approach, we use a bottom-up approach
 * based on the more efficient pointer representation introduced in SLINK.
 * <p>
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, J. Sander<br>
 * Density-Based Clustering Based on Hierarchical Density Estimates<br>
 * Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)
 * F
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - HierarchicalClusteringAlgorithm
 * @assoc - - - PointerHierarchyRepresentationResult
 */
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, J. Sander", //
    title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
    booktitle = "Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)", //
    url = "https://doi.org/10.1007/978-3-642-37456-2_14", //
    bibkey = "DBLP:conf/pakdd/CampelloMS13")
@Priority(Priority.RECOMMENDED + 5) // Extraction should come before clustering
public class SimplifiedHierarchyExtraction implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SimplifiedHierarchyExtraction.class);

  /**
   * Minimum cluster size.
   */
  private int minClSize = 1;

  /**
   * Clustering algorithm to run to obtain the hierarchy.
   */
  private HierarchicalClusteringAlgorithm algorithm;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param minClSize Minimum cluster size
   */
  public SimplifiedHierarchyExtraction(HierarchicalClusteringAlgorithm algorithm, int minClSize) {
    super();
    this.algorithm = algorithm;
    this.minClSize = minClSize;
  }

  @Override
  public Clustering<DendrogramModel> run(Database database) {
    PointerHierarchyRepresentationResult pointerresult = algorithm.run(database);
    return run(pointerresult);
  }

  /**
   * Process an existing result.
   * 
   * @param pointerresult Existing result in pointer representation.
   * @return Clustering
   */
  public Clustering<DendrogramModel> run(PointerHierarchyRepresentationResult pointerresult) {
    Clustering<DendrogramModel> result = new Instance(pointerresult).run();
    result.addChildResult(pointerresult);
    return result;
  }

  /**
   * Instance for a single data set.
   * 
   * @author Erich Schubert
   */
  protected class Instance {
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
     * Core distances (if available, may be {@code null}).
     */
    protected DoubleDataStore coredist = null;

    /**
     * The hierarchical result to process.
     */
    protected PointerHierarchyRepresentationResult pointerresult;

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
      if(pointerresult instanceof PointerDensityHierarchyRepresentationResult) {
        this.coredist = ((PointerDensityHierarchyRepresentationResult) pointerresult).getCoreDistanceStore();
      }
    }

    /**
     * Extract all clusters from the pi-lambda-representation.
     *
     * @return Hierarchical clustering
     */
    public Clustering<DendrogramModel> run() {
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), LOG) : null;

      // Sort DBIDs by lambda, to process merges in increasing order.
      ArrayDBIDs order = pointerresult.topologicalSort();

      WritableDataStore<TempCluster> cluster_map = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP, TempCluster.class);

      ArrayModifiableDBIDs noise = DBIDUtil.newArray();
      ArrayList<Cluster<DendrogramModel>> toplevel = new ArrayList<>();

      final Clustering<DendrogramModel> dendrogram = new Clustering<>("Hierarchical Clustering", "hierarchical-clustering");

      DBIDVar succ = DBIDUtil.newVar(); // Variable for successor.
      // Perform one join at a time, in increasing order
      for(DBIDArrayIter it = order.iter(); it.valid(); it.advance()) {
        final double dist = lambda.doubleValue(it); // Join distance
        final boolean cIsCore = (coredist == null) || dist >= coredist.doubleValue(it);

        // Original cluster (may be null):
        TempCluster cclus = cluster_map.get(it);
        final boolean cNotSpurious = cclus != null ? cclus.isNotSpurious(minClSize) : (minClSize <= 1 && cIsCore);

        // Successor cluster:
        pi.assignVar(it, succ); // succ = pi(it)
        // If we don't have a (different) successor, we are at the root level.
        if(DBIDUtil.equal(it, succ)) {
          // TODO: is this the proper handling?
          if(cclus != null) {
            if(cclus.isNotSpurious(minClSize)) {
              toplevel.add(toCluster(cclus, dendrogram, it));
            }
            else {
              noise.addDBIDs(cclus.newids);
            }
            cluster_map.put(it, null);
          }
          else if(minClSize <= 1 && cIsCore) { // Non-spurious singleton
            toplevel.add(makeCluster(it, dist, null));
          }
          else { // Spurious singleton
            noise.add(it);
          }
          LOG.incrementProcessed(progress);
          continue; // top level cluster.
        }
        // Other cluster (cluster of successor)
        TempCluster oclus = cluster_map.get(succ);
        final boolean oIsCore = (coredist == null || dist <= coredist.doubleValue(succ));
        final boolean oNotSpurious = oclus != null ? oclus.isNotSpurious(minClSize) : (minClSize <= 1 && oIsCore);
        // Both exist already, and are not spurious: full merge.
        if(oclus != null && cclus != null) {
          if(oNotSpurious && cNotSpurious) {
            // Finalize both children:
            oclus.addChild(toCluster(oclus, dendrogram, it));
            oclus.addChild(toCluster(cclus, dendrogram, it));
            assert (oclus.children.size() == 2);
            oclus.depth = dist; // Update height
          }
          else if(cNotSpurious) {
            // Fully merge oclus into cclus
            cclus.addDBIDs(oclus.newids);
            assert (oclus.children.isEmpty());
            cclus.depth = dist; // Update height
            cluster_map.put(succ, cclus);
          }
          else {
            // Fully merge cclus into oclus
            oclus.addDBIDs(cclus.newids);
            assert (cclus.children.isEmpty());
            oclus.depth = dist; // Update height
          }
        }
        else if(cclus != null) { // Current exists
          if(cNotSpurious && oNotSpurious) {
            cclus.addChild(toCluster(cclus, dendrogram, it));
          }
          addSingleton(cclus, succ, dist, oNotSpurious);
          cluster_map.put(succ, cclus);
        }
        else if(oclus != null) { // Other exists
          if(cNotSpurious && oNotSpurious) {
            oclus.addChild(toCluster(oclus, dendrogram, it));
          }
          addSingleton(oclus, it, dist, cNotSpurious);
        }
        else { // Both null, make new
          oclus = new TempCluster(dist);
          addSingleton(oclus, it, dist, cNotSpurious);
          addSingleton(oclus, succ, dist, oNotSpurious);
          cluster_map.put(succ, oclus);
        }
        cluster_map.put(it, null);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      // Add root structure to dendrogram:
      if(noise.size() > 0) {
        Cluster<DendrogramModel> nclus = new Cluster<>("Noise", noise, true, new DendrogramModel(Double.POSITIVE_INFINITY));
        dendrogram.addToplevelCluster(nclus);
        for(Cluster<DendrogramModel> clus : toplevel) {
          dendrogram.addChildCluster(nclus, clus);
        }
      }
      else {
        for(Cluster<DendrogramModel> clus : toplevel) {
          dendrogram.addToplevelCluster(clus);
        }
      }
      return dendrogram;
    }

    /**
     * Add a singleton object, as point or cluster.
     *
     * @param clus Current cluster.
     * @param id Object to add
     * @param dist Distance
     * @param asCluster Add as cluster (or only as id)
     */
    private void addSingleton(TempCluster clus, DBIDRef id, double dist, boolean asCluster) {
      if(asCluster) {
        clus.addChild(makeCluster(id, dist, null));
      }
      else {
        clus.add(id); // Add current object
      }
      clus.depth = dist; // Update height
    }

    /**
     * Make the cluster for the given object
     *
     * @param temp Current temporary cluster
     * @param clustering Parent clustering
     * @param lead Leading object
     * @return Cluster
     */
    protected Cluster<DendrogramModel> toCluster(TempCluster temp, Clustering<DendrogramModel> clustering, DBIDRef lead) {
      Cluster<DendrogramModel> cluster = makeCluster(lead, temp.depth, DBIDUtil.newArray(temp.newids));
      for(Cluster<DendrogramModel> child : temp.children) {
        clustering.addChildCluster(cluster, child);
      }
      temp.newids.clear();
      temp.children.clear();
      return cluster;
    }

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
      if(members == null || members.size() == 1 && members.contains(lead)) {
        name = "obj_" + DBIDUtil.toString(lead);
        if(members == null) {
          ArrayModifiableDBIDs m = DBIDUtil.newArray(1);
          m.add(lead);
          members = m;
        }
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

  /**
   * Temporary cluster.
   *
   * @author Erich Schubert
   */
  private static class TempCluster {
    /**
     * New ids, not yet in child clusters.
     */
    protected ModifiableDBIDs newids = DBIDUtil.newArray();

    /**
     * Current height.
     */
    protected double depth = 0.;

    /**
     * (Finished) child clusters
     */
    protected Collection<Cluster<DendrogramModel>> children = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param depth Depth
     */
    public TempCluster(double depth) {
      this.depth = depth;
    }

    /**
     * Add new objects to the cluster.
     *
     * @param id ID to add.
     */
    public void add(DBIDRef id) {
      this.newids.add(id);
    }

    /**
     * Add new objects to the cluster.
     *
     * @param ids IDs to add.
     */
    public void addDBIDs(DBIDs ids) {
      this.newids.addDBIDs(ids);
    }

    /**
     * Add a child cluster.
     *
     * @param clu Child cluster.
     */
    public void addChild(Cluster<DendrogramModel> clu) {
      children.add(clu);
    }

    /**
     * Test if a cluster is spurious.
     *
     * @param minClSize Minimum cluster size
     * @return {@code true} if spurious
     */
    public boolean isNotSpurious(int minClSize) {
      return !children.isEmpty() || newids.size() >= minClSize;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The minimum size of clusters to extract.
     */
    public static final OptionID MINCLUSTERSIZE_ID = new OptionID("hdbscan.minclsize", "The minimum cluster size.");

    /**
     * Minimum cluster size.
     */
    int minClSize = 1;

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

      IntParameter minclustersP = new IntParameter(MINCLUSTERSIZE_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minclustersP)) {
        minClSize = minclustersP.intValue();
      }
    }

    @Override
    protected SimplifiedHierarchyExtraction makeInstance() {
      return new SimplifiedHierarchyExtraction(algorithm, minClSize);
    }
  }
}
