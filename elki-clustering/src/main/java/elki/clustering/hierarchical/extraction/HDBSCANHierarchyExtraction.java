/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.hierarchical.extraction;

import java.util.ArrayList;
import java.util.Collection;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.hierarchical.*;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.data.model.PrototypeDendrogramModel;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Extraction of simplified cluster hierarchies, as proposed in HDBSCAN,
 * and additionally also compute the GLOSH outlier scores.
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
 * R. J. G. B. Campello, D. Moulavi, A. Zimek, J. Sander<br>
 * Hierarchical Density Estimates for Data Clustering, Visualization, and
 * Outlier Detection<br>
 * ACM Trans. Knowl. Discov. Data 10(1)
 * <p>
 * Note: some of the code is rather complex because we delay the creation of
 * one-element clusters to reduce garbage collection overhead.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - HierarchicalClusteringAlgorithm
 * @assoc - - - PointerHierarchyResult
 */
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, J. Sander", //
    title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
    booktitle = "Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)", //
    url = "https://doi.org/10.1007/978-3-642-37456-2_14", //
    bibkey = "DBLP:conf/pakdd/CampelloMS13")
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, A. Zimek, J. Sander", //
    title = "Hierarchical Density Estimates for Data Clustering, Visualization, and Outlier Detection", //
    booktitle = "ACM Trans. Knowl. Discov. Data 10(1)", //
    url = "https://doi.org/10.1145/2733381", //
    bibkey = "DBLP:journals/tkdd/CampelloMZS15")
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
  public Clustering<DendrogramModel> autorun(Database database) {
    return run(algorithm.autorun(database));
  }

  /**
   * Process an existing result.
   * 
   * @param merges Existing result in pointer representation.
   * @return Clustering
   */
  public Clustering<DendrogramModel> run(ClusterMergeHistory merges) {
    Clustering<DendrogramModel> result = new Instance(merges).run();
    Metadata.hierarchyOf(result).addChild(merges);
    return result;
  }

  /**
   * Instance for a single data set.
   * 
   * @author Erich Schubert
   */
  protected class Instance {
    /**
     * The hierarchical result to process.
     */
    protected ClusterMergeHistory merges;

    /**
     * Core distances, if available.
     */
    protected DoubleDataStore coredist;

    /**
     * Constructor.
     *
     * @param merges Hierarchical result
     */
    public Instance(ClusterMergeHistory merges) {
      this.merges = merges;
      if(merges instanceof ClusterDensityMergeHistory) {
        this.coredist = ((ClusterDensityMergeHistory) merges).getCoreDistanceStore();
      }
    }

    /**
     * Extract all clusters from the pi-lambda-representation.
     *
     * @return Hierarchical clustering
     */
    public Clustering<DendrogramModel> run() {
      Int2ObjectMap<TempCluster> cluster_map = new Int2ObjectOpenHashMap<>(merges.size() >> 1);
      DBIDVar tmp = DBIDUtil.newVar();
      final int n = merges.size();

      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", merges.numMerges(), LOG) : null;
      // Perform one join at a time, in increasing order
      for(int i = 0, m = merges.numMerges(); i < m; i++) {
        final double dist = merges.getMergeHeight(i); // Join distance
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        // Original cluster (may be null):
        TempCluster cclus = cluster_map.remove(a); // Take out
        final double cdist = (coredist != null && a < n) ? coredist.doubleValue(merges.assignVar(a, tmp)) : dist;
        final boolean cSpurious = isSpurious(cclus, cdist <= dist);
        // Other cluster (cluster of successor)
        TempCluster oclus = cluster_map.remove(b);
        final double odist = (coredist != null && b < n) ? coredist.doubleValue(merges.assignVar(b, tmp)) : dist;
        final boolean oSpurious = isSpurious(oclus, odist <= dist);

        final TempCluster nclus; // Resulting cluster.
        if(!cSpurious && !oSpurious) {
          // Full merge: both not spurious, new parent.
          assert cclus != null || a < n;
          assert oclus != null || b < n;
          cclus = cclus != null ? cclus : new TempCluster(a, cdist, merges.assignVar(a, tmp));
          oclus = oclus != null ? oclus : new TempCluster(b, odist, merges.assignVar(b, tmp));
          nclus = new TempCluster(i, dist, oclus, cclus);
        }
        else {
          // Prefer recycling a non-spurious cluster (could have children!)
          if(!oSpurious && oclus != null) {
            nclus = a < n ? oclus.grow(i, dist, merges.assignVar(a, tmp)) : oclus.grow(i, dist, cclus);
          }
          else if(!cSpurious && cclus != null) {
            nclus = b < n ? cclus.grow(i, dist, merges.assignVar(b, tmp)) : cclus.grow(i, dist, oclus);
          }
          // ospurious, but recycle the existing cluster, but reset
          else if(oclus != null) {
            nclus = (a < n ? oclus.grow(i, dist, merges.assignVar(a, tmp)) : oclus.grow(i, dist, cclus)).resetAggregate();
          }
          else if(cclus != null) {
            nclus = (b < n ? cclus.grow(i, dist, merges.assignVar(b, tmp)) : cclus.grow(i, dist, oclus)).resetAggregate();
          }
          else { // new 2-element cluster, but which may still be spurious
            assert a < n && b < n;
            nclus = new TempCluster(i, dist, merges.assignVar(a, tmp));
            nclus.members.add(merges.assignVar(b, tmp));
          }
        }
        assert nclus != null;
        cluster_map.put(i + n, nclus);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      // Build final dendrogram, and compute GLOSH scores:
      WritableDoubleDataStore glosh = DataStoreUtil.makeDoubleStorage(merges.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
      final Clustering<DendrogramModel> dendrogram = new Clustering<>();
      Metadata.of(dendrogram).setLongName("Hierarchical Clustering");
      for(TempCluster clus : cluster_map.values()) {
        finalizeCluster(clus, dendrogram, glosh, null, false);
      }
      // Also store GLOSH scores.
      Metadata.hierarchyOf(dendrogram).addChild(glosh);
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
     * @param glosh GLOSH scores output
     * @param parent Parent cluster (for hierarchical output)
     * @param flatten Flag to flatten all clusters below
     * @return smallest distance when the cluster exists
     */
    private double finalizeCluster(TempCluster temp, Clustering<DendrogramModel> clustering, WritableDoubleDataStore glosh, Cluster<DendrogramModel> parent, boolean flatten) {
      final String name = "C_" + FormatUtil.NF6.format(temp.dist);
      DendrogramModel model;
      if(temp.members != null && !temp.members.isEmpty() && merges instanceof ClusterPrototypeMergeHistory) {
        model = new PrototypeDendrogramModel(temp.dist, ((ClusterPrototypeMergeHistory) merges).prototype(temp.seq));
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
      double dmin = collectChildren(temp, clustering, glosh, temp, clus, flatten);
      for(DBIDIter it = temp.members.iter(); it.valid(); it.advance()) {
        // Note: we work with dists = 1/f.
        double cdist = coredist != null ? coredist.doubleValue(it) : temp.dist;
        cdist = cdist >= dmin ? cdist : dmin;
        assert minClSize > 2 || dmin <= cdist : dmin + " > " + cdist;
        glosh.put(it, cdist > 0 ? 1. - dmin / cdist : 0.);
      }
      temp.members = null;
      temp.children = null;
      return dmin;
    }

    /**
     * Recursive flattening of clusters.
     *
     * @param clustering Output clustering
     * @param glosh GLOSH scores output
     * @param cur Current temporary cluster
     * @param clus Output cluster
     * @param flatten Flag to indicate everything below should be flattened
     * @return smallest distance when the cluster exists
     */
    private double collectChildren(TempCluster temp, Clustering<DendrogramModel> clustering, WritableDoubleDataStore glosh, TempCluster cur, Cluster<DendrogramModel> clus, boolean flatten) {
      double dmin = cur.dmin;
      for(TempCluster child : cur.children) {
        double cdmin;
        if(flatten || child.totalStability() < 0) {
          temp.members.addDBIDs(child.members);
          cdmin = collectChildren(temp, clustering, glosh, child, clus, flatten);
        }
        else {
          cdmin = finalizeCluster(child, clustering, glosh, clus, true);
        }
        dmin = dmin < cdmin ? dmin : cdmin;
      }
      return dmin;
    }
  }

  /**
   * Temporary cluster.
   *
   * @author Erich Schubert
   */
  private static class TempCluster {
    /**
     * Merge id of the cluster for prototype identification.
     */
    protected int seq;

    /**
     * New ids, not yet in child clusters.
     */
    protected ModifiableDBIDs members = DBIDUtil.newArray();

    /**
     * Current height.
     */
    protected double dist = 0.;

    /**
     * Minimum height (densest object).
     */
    protected double dmin = 0.;

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
     * @param seq Cluster generation sequence
     * @param dist Distance
     * @param a Object reference
     */
    public TempCluster(int seq, double dist, DBIDRef a) {
      this.seq = seq;
      this.dist = this.dmin = dist;
      this.members.add(a);
      this.aggregate = 1. / dist;
    }

    /**
     * Cluster containing two existing clusters.
     *
     * @param seq Cluster generation sequence
     * @param dist Distance
     * @param a First cluster
     * @param b Second cluster
     */
    public TempCluster(int seq, double dist, TempCluster a, TempCluster b) {
      this.seq = seq;
      this.dist = dist;
      this.dmin = a.dmin < b.dmin ? a.dmin : b.dmin;
      this.children.add(a);
      this.children.add(b);
      this.childrenTotal = a.totalElements() + b.totalElements();
      this.aggregate = this.childrenTotal / dist;
    }

    /**
     * Join the contents of another cluster.
     *
     * @param seq Cluster generation sequence
     * @param dist Join distance
     * @param other Other cluster (may be {@code null})
     * @return {@code this}
     */
    public TempCluster grow(int seq, double dist, TempCluster other) {
      this.seq = seq;
      this.dist = dist;
      this.dmin = this.dmin < other.dmin ? this.dmin : other.dmin;
      assert other.children.isEmpty();
      this.members.addDBIDs(other.members);
      this.aggregate += other.members.size() / dist;
      other.members = null; // Invalidate
      other.children = null; // Invalidate
      return this;
    }

    /**
     * Join the contents of another cluster.
     *
     * @param seq Cluster generation sequence
     * @param dist Join distance
     * @param id Cluster lead, for 1-element clusters.
     * @return {@code this}
     */
    public TempCluster grow(int seq, double dist, DBIDRef id) {
      this.seq = seq;
      this.dist = this.dmin = dist;
      this.members.add(id);
      this.aggregate += 1. / dist;
      return this;
    }

    /**
     * Reset the aggregate (for spurious clusters).
     *
     * @return {@code this}
     */
    public TempCluster resetAggregate() {
      aggregate = totalElements() / dist;
      this.dmin = dist;
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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new ObjectParameter<HierarchicalClusteringAlgorithm>(Algorithm.Utils.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class, HDBSCANLinearMemory.class) //
          .grab(config, x -> algorithm = x);
      new IntParameter(MINCLUSTERSIZE_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minClSize = x);
      new Flag(HIERARCHICAL_ID).grab(config, x -> hierarchical = x);
    }

    @Override
    public HDBSCANHierarchyExtraction make() {
      return new HDBSCANHierarchyExtraction(algorithm, minClSize, hierarchical);
    }
  }
}
