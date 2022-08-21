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
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.ModifiableDBIDs;
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

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Extraction of a given number of clusters with a minimum size, and noise.
 * <p>
 * This will execute the highest-most cut where we retain k clusters, each with
 * a minimum size, plus noise (single points that would only merge afterwards).
 * If no such cut can be found, it returns a result with a relaxed k.
 * <p>
 * You need to specify: A) the minimum size of a cluster (it does not make much
 * sense to use 1 - then it will simply execute all but the last k merges) and
 * B) the desired number of clusters with at least minSize elements each.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Michael Gertz<br>
 * Semantic Word Clouds with Background Corpus Normalization and t-distributed
 * Stochastic Neighbor Embedding<br>
 * ArXiV preprint, 1708.03569
 * <p>
 * TODO: Also provide representatives and last merge height for clusters.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @assoc - - - HierarchicalClusteringAlgorithm
 * @assoc - - - PointerHierarchyResult
 */
@Reference(authors = "Erich Schubert, Michael Gertz", //
    title = "Semantic Word Clouds with Background Corpus Normalization and t-distributed Stochastic Neighbor Embedding", //
    booktitle = "ArXiV preprint, 1708.03569", //
    url = "http://arxiv.org/abs/1708.03569", //
    bibkey = "DBLP:journals/corr/abs-1708-03569")
@Priority(Priority.RECOMMENDED + 6) // Extraction should come before clustering
public class ClustersWithNoiseExtraction implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClustersWithNoiseExtraction.class);

  /**
   * Minimum number of clusters.
   */
  private int numCl = 1;

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
   * @param numCl Number of clusters
   * @param minClSize Minimum cluster size
   */
  public ClustersWithNoiseExtraction(HierarchicalClusteringAlgorithm algorithm, int numCl, int minClSize) {
    super();
    this.algorithm = algorithm;
    this.numCl = numCl;
    this.minClSize = minClSize;
  }

  @Override
  public Clustering<Model> autorun(Database database) {
    return run(algorithm.autorun(database));
  }

  /**
   * Process an existing result.
   * 
   * @param merges Existing result in pointer representation.
   * @return Clustering
   */
  public Clustering<Model> run(ClusterMergeHistory merges) {
    Clustering<Model> result = new Instance(merges).run();
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
     * Constructor.
     *
     * @param merges Hierarchical result
     */
    public Instance(ClusterMergeHistory merges) {
      this.merges = merges;
    }

    /**
     * Extract all clusters from the pi-lambda-representation.
     *
     * @return Hierarchical clustering
     */
    public Clustering<Model> run() {
      final int n = merges.size();
      // In a first pass, find the stop position.
      int curGood = 0, bestCl = n, bestOff = n - bestCl - 1;
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Finding best threshold", merges.numMerges(), LOG) : null;
      for(int i = 0, m = merges.numMerges(); i < m; i++) {
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        final int sa = a < n ? 1 : merges.getSize(a - n);
        final int sb = b < n ? 1 : merges.getSize(b - n);
        final int sc = merges.getSize(i);
        // Change in good clusters
        curGood += (sa >= minClSize ? -1 : 0) + (sb >= minClSize ? -1 : 0) + (sc >= minClSize ? 1 : 0);
        // Desired number of clusters, or at least new best:
        if(curGood == numCl || Math.abs(curGood - numCl) < Math.abs(bestCl - numCl)) {
          bestCl = curGood;
          bestOff = i;
        }
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      if(bestCl != numCl) {
        LOG.warning("Could not find a result with exactly " + numCl + " clusters (+ noise), generating " + bestCl + " clusters instead.");
      }

      progress = LOG.isVerbose() ? new FiniteProgress("Performing cluster merges", bestOff, LOG) : null;
      Int2IntOpenHashMap leafMap = new Int2IntOpenHashMap(merges.size());
      leafMap.defaultReturnValue(-1);
      ArrayList<ModifiableDBIDs> clusterMembers = new ArrayList<>(merges.size() - bestCl + 2);
      clusterMembers.add(DBIDUtil.newArray()); // noise

      final DBIDVar tmp = DBIDUtil.newVar();
      // Process merges backwards, starting at the split point (less
      // allocations). We build a map (merge number -> cluster number), and add
      // singleton objects to their clusters.
      for(int i = bestOff; i >= 0; --i) {
        final int a = merges.getMergeA(i), b = merges.getMergeB(i);
        int c = leafMap.remove(i + n); // no longer needed
        ModifiableDBIDs ci = null;
        if(c < 0 && merges.getSize(i) < minClSize) {
          c = 0; // put into noise
        }
        if(c < 0) { // not yet created
          c = clusterMembers.size(); // next index
          ci = DBIDUtil.newArray();
          clusterMembers.add(ci);
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
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      // Wrap as cluster objects:
      ArrayList<Cluster<Model>> toplevel = new ArrayList<>(bestCl + 1);
      // Noise cluster?
      if(!clusterMembers.get(0).isEmpty()) {
        toplevel.add(new Cluster<>("Noise", clusterMembers.get(0), true));
      }
      for(int i = 1; i < clusterMembers.size(); i++) {
        toplevel.add(new Cluster<>(clusterMembers.get(i)));
      }

      Clustering<Model> dendrogram = new Clustering<>(toplevel);
      Metadata.of(dendrogram).setLongName("Hierarchical Clustering");
      return dendrogram;
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
     * The number of clusters to extract.
     */
    public static final OptionID K_ID = new OptionID("extract.k", "The number of clusters to extract.");

    /**
     * The minimum size of clusters to extract.
     */
    public static final OptionID MINCLUSTERSIZE_ID = new OptionID("extract.minclsize", "The minimum cluster size.");

    /**
     * Minimum number of clusters.
     */
    int numCl = 1;

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
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> numCl = x);
      new IntParameter(MINCLUSTERSIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minClSize = x);
    }

    @Override
    public ClustersWithNoiseExtraction make() {
      return new ClustersWithNoiseExtraction(algorithm, numCl, minClSize);
    }
  }
}
