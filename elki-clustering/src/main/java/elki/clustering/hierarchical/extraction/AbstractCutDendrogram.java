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

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.clustering.hierarchical.ClusterPrototypeMergeHistory;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.data.model.PrototypeDendrogramModel;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.result.Metadata;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Abstract base class for extracting clusters from dendrograms.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @composed - runs - HierarchicalClusteringAlgorithm
 * @assoc - processes - PointerHierarchyResult
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
   * Produce a simpler result by adding single points directly into the merge
   * cluster.
   */
  protected final boolean simplify;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm
   * @param hierarchical Extract hierarchical result
   * @param simplify Simplify by putting single points into merge clusters
   */
  public AbstractCutDendrogram(HierarchicalClusteringAlgorithm algorithm, boolean hierarchical, boolean simplify) {
    super();
    this.algorithm = algorithm;
    this.hierarchical = hierarchical;
    this.simplify = simplify;
  }

  /**
   * Run the algorithms on a database.
   * 
   * @param database Database to process
   * @return Clustering
   */
  public Clustering<DendrogramModel> run(Database database) {
    assert algorithm != null : "To auto-run on a database, the algorithm must be configured.";
    return run(algorithm.autorun(database));
  }

  /**
   * Process a pointer hierarchy result.
   * 
   * @param pointerresult Hierarchical result in pointer representation.
   * @return Clustering
   */
  public abstract Clustering<DendrogramModel> run(ClusterMergeHistory pointerresult);

  /**
   * Instance for a single data set.
   * 
   * @author Erich Schubert
   */
  public abstract class Instance {
    /**
     * The hierarchical result to process.
     */
    protected ClusterMergeHistory merges;

    /**
     * Map clusters to integer cluster numbers.
     */
    protected Int2IntOpenHashMap leafMap;

    /**
     * Topmost merge of each leaf.
     */
    protected IntegerArray leafTop;

    /**
     * Collected cluster members
     */
    protected ArrayList<ModifiableDBIDs> clusterMembers;

    /**
     * Constructor.
     *
     * @param merges Cluster merge history
     */
    public Instance(ClusterMergeHistory merges) {
      this.merges = merges;
    }

    /**
     * Extract all clusters from the pi-lambda-representation.
     *
     * @return Hierarchical clustering
     */
    public Clustering<DendrogramModel> extractClusters() {
      final Logging log = getLogger();
      final int split = findSplit();

      // Extract the child clusters
      FiniteProgress progress = log.isVerbose() ? new FiniteProgress("Extracting clusters", merges.numMerges(), log) : null;
      // Initialize data structures:
      final int expcnum = merges.size() - split + 1;
      leafMap = new Int2IntOpenHashMap(merges.size());
      leafMap.defaultReturnValue(-1);
      clusterMembers = new ArrayList<>(expcnum);
      leafTop = new IntegerArray(expcnum);

      buildLeafClusters(split, progress);
      Clustering<DendrogramModel> dendrogram = hierarchical ? //
          buildHierarchical(split, progress) : buildFlat(split, progress);
      log.ensureCompleted(progress);
      return dendrogram;
    }

    /**
     * Prepare the leaf clusters by executing the first (size - 1 - split)
     * merges.
     * 
     * @param split Splitting point
     * @param progress Progress for logging (may be {@code null})
     */
    private void buildLeafClusters(int split, FiniteProgress progress) {
      final Logging log = getLogger();
      final DBIDVar tmp = DBIDUtil.newVar();
      final int n = merges.size();
      // Process merges backwards, starting at the split point (less
      // allocations). We build a map (merge number -> cluster number), and add
      // singleton objects to their clusters.
      for(int i = split - 1; i >= 0; --i) {
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        int c = leafMap.remove(i + n); // no longer needed
        ModifiableDBIDs ci = null;
        if(c < 0) { // not yet created
          c = clusterMembers.size(); // next index
          ci = DBIDUtil.newArray();
          clusterMembers.add(ci);
          leafTop.add(i);
        }
        else {
          ci = clusterMembers.get(c);
        }
        if(b < n) { // b is singleton, add
          ci.add(merges.assignVar(b, tmp));
        }
        else { // ca = c
          leafMap.put(b, c);
        }
        if(a < n) { // a is singleton, add
          ci.add(merges.assignVar(a, tmp));
        }
        else { // ca = c
          leafMap.put(a, c);
        }
        log.incrementProcessed(progress);
      }
    }

    /**
     * Build a flat clustering.
     * 
     * @param split Splitting point
     * @param progress Progress for logging (may be {@code null})
     * @return Clustering
     */
    private Clustering<DendrogramModel> buildFlat(int split, FiniteProgress progress) {
      final Logging log = getLogger();
      final int n = merges.size();
      Clustering<DendrogramModel> dendrogram = new Clustering<>();
      Metadata.of(dendrogram).setLongName("Flattened Hierarchical Clustering");
      // Convert initial clusters to cluster objects
      for(int i = 0; i < leafTop.size; i++) {
        dendrogram.addToplevelCluster(makeCluster(leafTop.get(i) + n, clusterMembers.get(i)));
      }
      // Make one-element clusters for singletons at the top, if necessary:
      DBIDVar tmp = DBIDUtil.newVar();
      for(int i = split, m = merges.numMerges(); i < m; i++) {
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        if(a < m) {
          dendrogram.addToplevelCluster(makeCluster(a, DBIDUtil.deref(merges.assignVar(a, tmp))));
        }
        if(b < m) {
          dendrogram.addToplevelCluster(makeCluster(b, DBIDUtil.deref(merges.assignVar(b, tmp))));
        }
        log.incrementProcessed(progress);
      }
      return dendrogram;
    }

    /**
     * Build a hierarchical clustering.
     * 
     * @param split Splitting point
     * @param progress Progress for logging (may be {@code null})
     * @return Clustering
     */
    private Clustering<DendrogramModel> buildHierarchical(int split, FiniteProgress progress) {
      final Logging log = getLogger();
      final Clustering<DendrogramModel> dendrogram = new Clustering<>();
      Metadata.of(dendrogram).setLongName("Hierarchical Clustering");
      Cluster<DendrogramModel> root = null;
      final int n = merges.size();
      ArrayList<Cluster<DendrogramModel>> clusters = new ArrayList<>(n - split);
      // Convert initial clusters to cluster objects
      Int2IntOpenHashMap cmap = new Int2IntOpenHashMap(leafTop.size() << 1);
      cmap.defaultReturnValue(-1);
      for(int i = 0; i < leafTop.size; i++) {
        final int id = leafTop.get(i);
        clusters.add(makeCluster(id + n, clusterMembers.get(i)));
        cmap.put(id + n, i);
      }
      // Process the upper part, bottom-up.
      DBIDVar tmp = DBIDUtil.newVar();
      for(int i = split, m = merges.numMerges(); i < m; i++) {
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        final double depth = merges.getMergeHeight(i);
        final int ca = a < n ? -1 : cmap.remove(a);
        final Cluster<DendrogramModel> clu; // Cluster resulting from this merge
        if(ca >= 0) {
          Cluster<DendrogramModel> cla = clusters.get(ca);
          if(cla.getModel().getDistance() == depth) {
            if(b < n) { // single object at same height, simplify
              if(simplify) {
                cmap.put(i + n, ca);
                ((ModifiableDBIDs) (clu = cla).getIDs()).add(merges.assignVar(b, tmp));
              }
              else {
                cmap.put(i + n, clusters.size() + 1);
                clusters.add(clu = makeCluster(i + n, DBIDUtil.EMPTYDBIDS));
                dendrogram.addChildCluster(clu, cla);
                dendrogram.addChildCluster(clu, makeCluster(b, DBIDUtil.newArray(merges.assignVar(b, tmp))));
              }
            }
            else { // additional cluster at same height, add as child
              cmap.put(i + n, ca);
              dendrogram.addChildCluster(clu = cla, clusters.get(cmap.remove(b)));
            }
          }
          else { // new height, need merge cluster
            ModifiableDBIDs ci = DBIDUtil.newArray(b < n ? 1 : 0);
            if(b < n) { // add singleton to merge cluster directly:
              ci.add(merges.assignVar(b, tmp));
            }
            cmap.put(i + n, clusters.size());
            clusters.add(clu = makeCluster(i + n, ci));
            dendrogram.addChildCluster(clu, cla);
            if(b >= n) {
              dendrogram.addChildCluster(clu, clusters.get(cmap.remove(b)));
            }
          }
        }
        else { // a single object
          if(b < n) { // two single objects
            ModifiableDBIDs ci = DBIDUtil.newArray(2);
            ci.add(merges.assignVar(a, tmp));
            ci.add(merges.assignVar(b, tmp));
            cmap.put(i + n, clusters.size());
            clusters.add(clu = makeCluster(i + n, ci));
          }
          else {
            final int cb = cmap.get(b);
            Cluster<DendrogramModel> clb = clusters.get(cb);
            if(clb.getModel().getDistance() == depth) {
              ((ModifiableDBIDs) (clu = clb).getIDs()).add(merges.assignVar(a, tmp));
              cmap.put(i + n, cb);
            }
            else { // new height, need merge cluster
              cmap.put(i + n, clusters.size());
              if(!simplify) {
                clusters.add(clu = makeCluster(i + n, DBIDUtil.EMPTYDBIDS));
                dendrogram.addChildCluster(clu, makeCluster(a, DBIDUtil.newArray(merges.assignVar(a, tmp))));
              }
              else {
                clusters.add(clu = makeCluster(i + n, DBIDUtil.newArray(merges.assignVar(a, tmp))));
              }
              dendrogram.addChildCluster(clu, clb);
            }
          }
        }
        root = clu;
        // Decrement counter
        log.incrementProcessed(progress);
      }
      dendrogram.addToplevelCluster(root);
      return dendrogram;
    }

    /**
     * Find the splitting point in the merge history.
     *
     * @return Splitting point
     */
    protected abstract int findSplit();

    /**
     * Make the cluster for the given object
     *
     * @param seq Cluster sequence number
     * @param members Member objects
     * @return Cluster
     */
    protected Cluster<DendrogramModel> makeCluster(int seq, DBIDs members) {
      double depth = seq >= merges.size() ? merges.getMergeHeight(seq - merges.size()) : Double.NaN;
      final String name; // TODO: how useful are these names after all?
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

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  protected abstract Logging getLogger();

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Parameter to configure the output mode (nested or truncated clusters).
     */
    public static final OptionID HIERARCHICAL_ID = new OptionID("hierarchical.hierarchy", "Generate a truncated hierarchical clustering result (or strict partitions).");

    /**
     * Disable the simplification that puts points into merge clusters.
     */
    public static final OptionID NOSIMPLIFY_ID = new OptionID("hierarchical.nosimplify", "Do not put single points directly into merge clusters.");

    /**
     * Flag to generate a hierarchical result.
     */
    boolean hierarchical = false;

    /**
     * The hierarchical clustering algorithm to run.
     */
    HierarchicalClusteringAlgorithm algorithm;

    /**
     * Produce a simpler result by adding single points directly into the merge
     * cluster.
     */
    boolean simplify;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<HierarchicalClusteringAlgorithm>(Algorithm.Utils.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class) //
          .grab(config, x -> algorithm = x);
      new Flag(HIERARCHICAL_ID).grab(config, x -> hierarchical = x);
      if(hierarchical) {
        new Flag(NOSIMPLIFY_ID).grab(config, x -> simplify = !x);
      }
    }
  }
}
