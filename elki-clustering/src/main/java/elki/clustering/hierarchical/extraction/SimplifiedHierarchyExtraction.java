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
import elki.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import elki.clustering.hierarchical.ClusterDensityMergeHistory;
import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.clustering.hierarchical.ClusterPrototypeMergeHistory;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.data.model.PrototypeDendrogramModel;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DoubleDataStore;
import elki.database.ids.*;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

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
     * Core distances (if available, may be {@code null}).
     */
    protected DoubleDataStore coredist = null;

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

      final Clustering<DendrogramModel> dendrogram = new Clustering<>();
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Extracting clusters", merges.numMerges(), LOG) : null;
      // Perform one join at a time, in increasing order
      for(int i = 0, m = merges.numMerges(); i < m; i++) {
        final double dist = merges.getMergeHeight(i); // Join distance
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        // Original cluster (may be null):
        TempCluster aclus = cluster_map.remove(a);
        final boolean aIsCore = coredist == null || a >= n || dist >= coredist.doubleValue(merges.assignVar(a, tmp));
        final boolean aNotSpurious = aclus != null ? aclus.isNotSpurious(minClSize) : (minClSize <= 1 && aIsCore);
        // Other cluster (cluster of successor)
        TempCluster bclus = cluster_map.remove(b);
        final boolean bIsCore = coredist == null || b >= n || dist <= coredist.doubleValue(merges.assignVar(b, tmp));
        final boolean bNotSpurious = bclus != null ? bclus.isNotSpurious(minClSize) : (minClSize <= 1 && bIsCore);
        final TempCluster nclus; // Resulting cluster.
        // Both exist already, and are not spurious: full merge.
        if(bclus != null && aclus != null) {
          if(bNotSpurious && aNotSpurious) {
            // Finalize both children, reuse bclus
            bclus.addChild(toCluster(bclus, dendrogram));
            bclus.addChild(toCluster(aclus, dendrogram));
            assert bclus.children.size() == 2;
            nclus = bclus; // recycle
          }
          else if(aNotSpurious) {
            // Merge bclus into aclus
            aclus.addDBIDs(bclus.newids);
            assert bclus.children.isEmpty();
            nclus = aclus;
          }
          else {
            // Merge aclus into bclus
            bclus.addDBIDs(aclus.newids);
            assert aclus.children.isEmpty();
            nclus = bclus;
          }
          nclus.depth = dist; // Update height
        }
        else if(aclus != null) { // Only A exists
          if(aNotSpurious && bNotSpurious) {
            aclus.addChild(toCluster(aclus, dendrogram));
          }
          addSingleton(aclus, b, merges.assignVar(b, tmp), dist, bNotSpurious);
          nclus = aclus;
        }
        else if(bclus != null) { // Only B exists
          if(aNotSpurious && bNotSpurious) {
            bclus.addChild(toCluster(bclus, dendrogram));
          }
          addSingleton(bclus, a, merges.assignVar(a, tmp), dist, aNotSpurious);
          nclus = bclus;
        }
        else { // Both null, make new cluster
          nclus = new TempCluster(i + n, dist);
          addSingleton(nclus, a, merges.assignVar(a, tmp), dist, aNotSpurious);
          addSingleton(nclus, b, merges.assignVar(b, tmp), dist, bNotSpurious);
        }
        assert nclus != null;
        cluster_map.put(i + n, nclus);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      // Add root structure to dendrogram:
      for(TempCluster clus : cluster_map.values()) {
        dendrogram.addToplevelCluster(toCluster(clus, dendrogram));
      }
      Metadata.of(dendrogram).setLongName("Hierarchical Clustering");
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
    private void addSingleton(TempCluster clus, int id, DBIDRef it, double dist, boolean asCluster) {
      if(asCluster) {
        clus.addChild(makeCluster(id, dist, DBIDUtil.deref(it)));
      }
      else {
        clus.add(it); // Add current object
      }
      clus.depth = dist; // Update height
    }

    /**
     * Make the cluster for the given object
     *
     * @param temp Current temporary cluster
     * @param clustering Parent clustering
     * @return Cluster
     */
    protected Cluster<DendrogramModel> toCluster(TempCluster temp, Clustering<DendrogramModel> clustering) {
      Cluster<DendrogramModel> cluster = makeCluster(temp.seq, temp.depth, DBIDUtil.newArray(temp.newids));
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
     * @param seq Cluster sequence number
     * @param depth Linkage depth
     * @param members Member objects
     * @return Cluster
     */
    protected Cluster<DendrogramModel> makeCluster(int seq, double depth, DBIDs members) {
      final String name; // TODO: how useful are these names after all?
      members = members != null ? members : DBIDUtil.EMPTYDBIDS;
      if(members.size() == 1) {
        name = "obj_" + DBIDUtil.toString(members.iter());
      }
      else if(members.isEmpty()) { // non-trivial merge cluster
        name = "mrg_" + depth;
      }
      else if(depth < Double.POSITIVE_INFINITY) {
        name = "clu_" + depth;
      }
      else {
        name = "top";
      }

      DendrogramModel model;
      if(!members.isEmpty() && merges instanceof ClusterPrototypeMergeHistory) {
        model = new PrototypeDendrogramModel(depth, ((ClusterPrototypeMergeHistory) merges).prototype(seq));
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
     * Merge id of the cluster for prototype identification.
     */
    protected int seq;

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
     * @param seq Cluster generation sequence
     * @param depth Depth
     */
    public TempCluster(int seq, double depth) {
      this.seq = seq;
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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new ObjectParameter<HierarchicalClusteringAlgorithm>(Algorithm.Utils.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class) //
          .grab(config, x -> algorithm = x);
      new IntParameter(MINCLUSTERSIZE_ID, 1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minClSize = x);
    }

    @Override
    public SimplifiedHierarchyExtraction make() {
      return new SimplifiedHierarchyExtraction(algorithm, minClSize);
    }
  }
}
