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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Extraction of simplified cluster hierarchies, as proposed in HDBSCAN.
 * <p>
 * In contrast to the authors top-down approach, we use a bottom-up approach
 * based on the more efficient pointer representation introduced in SLINK.
 * <p>
 * In particular, it can also be used to extract a hierarchy from a hierarchical
 * agglomerative clustering.
 * <p>
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, J. Sander<br>
 * Density-Based Clustering Based on Hierarchical Density Estimates<br>
 * Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)
 * <p>
 * Note: some of the code is rather complex because we delay the creation of
 * one-element clusters to reduce garbage collection overhead.
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
public class HDBSCANHierarchyExtraction implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(HDBSCANHierarchyExtraction.class);

  /**
   * Minimum cluster size.
   */
  private int minClSize = 1;

  /**
   * Clustering algorithm to run to obtain the hierarchy.
   */
  private HierarchicalClusteringAlgorithm algorithm;

  /**
   * Return a hierarchical result.
   */
  private boolean hierarchical = true;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param minClSize Minimum cluster size
   * @param hierarchical Produce a hierarchical result
   */
  public HDBSCANHierarchyExtraction(HierarchicalClusteringAlgorithm algorithm, int minClSize, boolean hierarchical) {
    super();
    this.algorithm = algorithm;
    this.minClSize = minClSize;
    this.hierarchical = hierarchical;
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
      ArrayList<TempCluster> toplevel = new ArrayList<>();

      DBIDVar olead = DBIDUtil.newVar(); // Variable for successor.
      // Perform one join at a time, in increasing order
      for(DBIDArrayIter clead = order.iter(); clead.valid(); clead.advance()) {
        final double dist = lambda.doubleValue(clead); // Join distance
        final double cdist = (coredist != null) ? coredist.doubleValue(clead) : dist;
        // Original cluster (may be null):
        TempCluster cclus = cluster_map.get(clead);
        // Per definition of pointer hierarchy, must not be referenced anymore:
        cluster_map.put(clead, null); // Forget previous assignment.
        final boolean cSpurious = isSpurious(cclus, cdist <= dist);

        // Successor object/cluster:
        pi.assignVar(clead, olead); // Find successor
        // If we don't have a (different) successor, we are at the root level.
        if(DBIDUtil.equal(clead, olead) || olead.isEmpty()) {
          if(cclus != null) {
            if(cclus.isSpurious(minClSize)) {
              noise.addDBIDs(cclus.members);
            }
            else {
              toplevel.add(cclus);
            }
            cluster_map.put(clead, null);
          }
          else if(cSpurious) { // Spurious singleton
            noise.add(clead);
          }
          else { // Non-spurious singleton
            toplevel.add(new TempCluster(dist, clead));
          }
          LOG.incrementProcessed(progress);
          continue; // top level cluster.
        }
        // Other cluster (cluster of successor)
        TempCluster oclus = cluster_map.get(olead);
        final double odist = (coredist != null) ? coredist.doubleValue(olead) : dist;
        final boolean oSpurious = isSpurious(oclus, odist <= dist);

        final TempCluster nclus; // Resulting cluster.
        if(!oSpurious && !cSpurious) {
          // Full merge: both not spurious, new parent.
          cclus = cclus != null ? cclus : new TempCluster(cdist, clead);
          oclus = oclus != null ? oclus : new TempCluster(odist, olead);
          nclus = new TempCluster(dist, oclus, cclus);
        }
        else {
          // Prefer recycling a non-spurious cluster (could have children!)
          if(!oSpurious && oclus != null) {
            nclus = oclus.grow(dist, cclus, clead);
          }
          else if(!cSpurious && cclus != null) {
            nclus = cclus.grow(dist, oclus, olead);
          }
          // Then recycle, but reset
          else if(oclus != null) {
            nclus = oclus.grow(dist, cclus, clead).resetAggregate();
          }
          else if(cclus != null) {
            nclus = cclus.grow(dist, oclus, olead).resetAggregate();
          }
          // Last option: a new 2-element cluster.
          else {
            nclus = new TempCluster(dist, clead, olead);
          }
        }
        assert (nclus != null);
        cluster_map.put(olead, nclus);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      // Build final dendrogram:
      final Clustering<DendrogramModel> dendrogram = new Clustering<>("Hierarchical Clustering", "hierarchical-clustering");
      Cluster<DendrogramModel> nclus = null;
      if(noise.size() > 0) {
        nclus = new Cluster<>("Noise", noise, true, new DendrogramModel(Double.POSITIVE_INFINITY));
        dendrogram.addToplevelCluster(nclus);
      }
      for(TempCluster clus : toplevel) {
        finalizeCluster(clus, dendrogram, nclus, false);
      }
      return dendrogram;
    }

    /**
     * Spurious, also for non-materialized clusters.
     *
     * @param clus Cluster, may be {@code null} for 1-element clusters.
     * @param isCore Core property
     * @return {@code true} if spurious.
     */
    private boolean isSpurious(TempCluster clus, boolean isCore) {
      return clus != null ? clus.isSpurious(minClSize) : (minClSize > 1 || !isCore);
    }

    /**
     * Make the cluster for the given object
     *
     * @param temp Current temporary cluster
     * @param clustering Parent clustering
     * @param parent Parent cluster (for hierarchical output)
     * @param flatten Flag to flatten all clusters below.
     */
    private void finalizeCluster(TempCluster temp, Clustering<DendrogramModel> clustering, Cluster<DendrogramModel> parent, boolean flatten) {
      final String name = "C_" + FormatUtil.NF6.format(temp.dist);
      DendrogramModel model;
      if(temp.members != null && !temp.members.isEmpty() && pointerresult instanceof PointerPrototypeHierarchyRepresentationResult) {
        model = new PrototypeDendrogramModel(temp.dist, ((PointerPrototypeHierarchyRepresentationResult) pointerresult).findPrototype(temp.members));
      }
      else {
        model = new DendrogramModel(temp.dist);
      }
      Cluster<DendrogramModel> clus = new Cluster<>(name, temp.members, model);
      if(hierarchical && parent != null) { // Hierarchical output
        clustering.addChildCluster(parent, clus);
      }
      else {
        clustering.addToplevelCluster(clus);
      }
      collectChildren(temp, clustering, temp, clus, flatten);
      temp.members = null;
      temp.children = null;
    }

    /**
     * Recursive flattening of clusters.
     *
     * @param clustering Output clustering
     * @param cur Current temporary cluster
     * @param clus Output cluster
     * @param flatten Flag to indicate everything below should be flattened.
     */
    private void collectChildren(TempCluster temp, Clustering<DendrogramModel> clustering, TempCluster cur, Cluster<DendrogramModel> clus, boolean flatten) {
      for(TempCluster child : cur.children) {
        if(flatten || child.totalStability() < 0) {
          temp.members.addDBIDs(child.members);
          collectChildren(temp, clustering, child, clus, flatten);
        }
        else {
          finalizeCluster(child, clustering, clus, true);
        }
      }
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
    protected ModifiableDBIDs members = DBIDUtil.newArray();

    /**
     * Current height.
     */
    protected double dist = 0.;

    /**
     * Mass aggregate.
     */
    protected double aggregate = 0.;

    /**
     * Number of objects in children.
     */
    protected int childrenTotal = 0;

    /**
     * (Finished) child clusters
     */
    protected Collection<TempCluster> children = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param dist Distance
     * @param a Object reference
     */
    public TempCluster(double dist, DBIDRef a) {
      this.dist = dist;
      this.members.add(a);
      this.aggregate = 1. / dist;
    }

    /**
     * Constructor.
     *
     * @param dist Distance
     */
    public TempCluster(double dist, DBIDRef a, DBIDRef b) {
      this.dist = dist;
      this.members.add(a);
      this.members.add(b);
      this.aggregate = 2. / dist;
    }

    /**
     * Cluster containing two existing clusters.
     *
     * @param dist Distance
     * @param a First cluster
     * @param b Second cluster
     */
    public TempCluster(double dist, TempCluster a, TempCluster b) {
      this.dist = dist;
      this.children.add(a);
      this.children.add(b);
      this.childrenTotal = a.totalElements() + b.totalElements();
      this.aggregate = this.childrenTotal / dist;
    }

    /**
     * Join the contents of another cluster.
     *
     * @param dist Join distance
     * @param other Other cluster (may be {@code null})
     * @param id Cluster lead, for 1-element clusters.
     * @return {@code this}
     */
    public TempCluster grow(double dist, TempCluster other, DBIDRef id) {
      this.dist = dist;
      if(other == null) {
        this.members.add(id);
        this.aggregate += 1. / dist;
      }
      else {
        assert (other.children.isEmpty());
        this.members.addDBIDs(other.members);
        this.aggregate += other.members.size() / dist;
        other.members = null; // Invalidate
        other.children = null; // Invalidate
      }
      return this;
    }

    /**
     * Reset the aggregate (for spurious clusters).
     *
     * @return {@code this}
     */
    public TempCluster resetAggregate() {
      aggregate = totalElements() / dist;
      return this;
    }

    /**
     * Total number of elements in this subtree.
     *
     * @return Total
     */
    public int totalElements() {
      return childrenTotal + members.size();
    }

    /**
     * Excess of mass measure.
     *
     * @return Excess of mass
     */
    public double excessOfMass() {
      return aggregate - totalElements() / dist;
    }

    /**
     * Excess of mass measure.
     *
     * @return Excess of mass
     */
    public double totalStability() {
      double stability = excessOfMass();
      double cstab = 0.;
      for(TempCluster child : children) {
        cstab += Math.abs(child.totalStability());
      }
      return stability > cstab ? stability : -cstab;
    }

    /**
     * Test if a cluster is spurious.
     *
     * @param minClSize Minimum cluster size
     * @return {@code true} if spurious
     */
    public boolean isSpurious(int minClSize) {
      return children.isEmpty() && members.size() < minClSize;
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
     * Produce a hierarchical result.
     */
    public static final OptionID HIERARCHICAL_ID = new OptionID("hdbscan.hierarchical", "Produce a hierarchical output.");

    /**
     * Minimum cluster size.
     */
    int minClSize = 1;

    /**
     * The hierarchical clustering algorithm to run.
     */
    HierarchicalClusteringAlgorithm algorithm;

    /**
     * Return a hierarchical result.
     */
    boolean hierarchical = true;

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

      Flag hierarchicalF = new Flag(HIERARCHICAL_ID);
      if(config.grab(hierarchicalF)) {
        hierarchical = hierarchicalF.isTrue();
      }
    }

    @Override
    protected HDBSCANHierarchyExtraction makeInstance() {
      return new HDBSCANHierarchyExtraction(algorithm, minClSize, hierarchical);
    }
  }
}
