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
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerDensityHierarchyRepresentationResult;
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
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Extraction of simplified cluster hierarchies, as proposed in HDBSCAN.
 *
 * In contrast to the authors top-down approach, we use a bottom-up approach
 * based on the more efficient pointer representation introduced in SLINK.
 *
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, and J. Sander<br />
 * Density-Based Clustering Based on Hierarchical Density Estimates<br />
 * Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining,
 * PAKDD
 * </p>
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @apiviz.uses HierarchicalClusteringAlgorithm
 * @apiviz.uses PointerHierarchyRepresentationResult
 */
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, and J. Sander", //
title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
booktitle = "Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining, PAKDD", //
url = "http://dx.doi.org/10.1007/978-3-642-37456-2_14")
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
    DBIDs ids = pointerresult.getDBIDs();
    DBIDDataStore pi = pointerresult.getParentStore();
    DoubleDataStore lambda = pointerresult.getParentDistanceStore();
    DoubleDataStore coredist = null;
    if(pointerresult instanceof PointerDensityHierarchyRepresentationResult) {
      coredist = ((PointerDensityHierarchyRepresentationResult) pointerresult).getCoreDistanceStore();
    }

    Clustering<DendrogramModel> result = extractClusters(ids, pi, lambda, coredist);
    result.addChildResult(pointerresult);
    return result;
  }

  /**
   * Extract all clusters from the pi-lambda-representation.
   *
   * @param ids Object ids to process
   * @param pi Pi store
   * @param lambda Lambda store
   * @param coredist Core distances
   * @return Hierarchical clustering
   */
  public Clustering<DendrogramModel> extractClusters(DBIDs ids, DBIDDataStore pi, DoubleDataStore lambda, DoubleDataStore coredist) {
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", ids.size(), LOG) : null;

    // Sort DBIDs by lambda, to process merges in increasing order.
    ArrayDBIDs order = PointerHierarchyRepresentationResult.topologicalSort(ids, pi, lambda);

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
            toplevel.add(cclus.toCluster(dendrogram, it));
          }
          else {
            noise.addDBIDs(cclus.newids);
          }
          cluster_map.put(it, null);
        }
        else if(minClSize <= 1 && cIsCore) { // Non-spurious singleton
          toplevel.add(makeSingletonCluster(it, dist));
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
          oclus.addChild(oclus.toCluster(dendrogram, it));
          oclus.addChild(cclus.toCluster(dendrogram, it));
          assert (oclus.children.size() == 2);
          oclus.depth = dist; // Update height
        }
        else if(cNotSpurious) {
          // Fully merge oclus into cclus
          cclus.addDBIDs(oclus.newids);
          assert (oclus.children.size() == 0);
          cclus.depth = dist; // Update height
          cluster_map.put(succ, cclus);
        }
        else {
          // Fully merge cclus into oclus
          oclus.addDBIDs(cclus.newids);
          assert (cclus.children.size() == 0);
          oclus.depth = dist; // Update height
        }
      }
      else if(cclus != null) { // Current exists
        if(cNotSpurious && oNotSpurious) {
          cclus.addChild(cclus.toCluster(dendrogram, it));
        }
        addSingleton(cclus, succ, dist, oNotSpurious);
        cluster_map.put(succ, cclus);
      }
      else if(oclus != null) { // Other exists
        if(cNotSpurious && oNotSpurious) {
          oclus.addChild(oclus.toCluster(dendrogram, it));
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
      clus.addChild(makeSingletonCluster(id, dist));
    }
    else {
      clus.add(id); // Add current object
    }
    clus.depth = dist; // Update height
  }

  /**
   * Temporary cluster.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  protected static class TempCluster {
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
      return children.size() > 0 || newids.size() >= minClSize;
    }

    /**
     * Make the cluster for the given object
     *
     * @param clustering Parent clustering
     * @param lead Leading object
     * @return Cluster
     */
    private Cluster<DendrogramModel> toCluster(Clustering<DendrogramModel> clustering, DBIDRef lead) {
      final String name;
      if(children.size() > 1) {
        name = "mrg_" + DBIDUtil.toString(lead) + "_" + depth;
      }
      else if(newids.size() == 1) {
        name = "obj_" + DBIDUtil.toString(lead);
      }
      else if(!Double.isNaN(depth)) {
        name = "clu_" + DBIDUtil.toString(lead) + "_" + depth;
      }
      else {
        // Complete data set only?
        name = "clu_" + DBIDUtil.toString(lead);
      }
      Cluster<DendrogramModel> cluster = new Cluster<>(name, DBIDUtil.newArray(newids), //
      new DendrogramModel(depth));
      for(Cluster<DendrogramModel> child : children) {
        clustering.addChildCluster(cluster, child);
      }
      newids.clear();
      children.clear();
      return cluster;
    }
  }

  /**
   * Make the cluster for the given object
   *
   * @param lead Leading object
   * @param depth Linkage depth
   * @return Cluster
   */
  private Cluster<DendrogramModel> makeSingletonCluster(DBIDRef lead, double depth) {
    final String name = "obj_" + DBIDUtil.toString(lead);
    return new Cluster<>(name, DBIDUtil.deref(lead), new DendrogramModel(depth));
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
      ObjectParameter<HierarchicalClusteringAlgorithm> algorithmP = new ObjectParameter<>(AlgorithmStep.Parameterizer.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class);
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
