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
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerDensityHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
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
 * @assoc - - - PointerHierarchyRepresentationResult
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
  public Clustering<Model> run(Database database) {
    return run(algorithm.run(database));
  }

  /**
   * Process an existing result.
   * 
   * @param pointerresult Existing result in pointer representation.
   * @return Clustering
   */
  public Clustering<Model> run(PointerHierarchyRepresentationResult pointerresult) {
    Clustering<Model> result = new Instance(pointerresult).run();
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
    public Clustering<Model> run() {
      // Sort DBIDs by lambda, to process merges in increasing order.
      ArrayDBIDs order = pointerresult.topologicalSort();

      // int numMerges = ids.size() - numCl;
      DBIDVar succ = DBIDUtil.newVar(); // Variable for successor.

      // In a first pass, find the stop position.
      // Cluster sizes, initial size: 1
      WritableIntegerDataStore clustersizes = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, 1);
      int curGood = 0, bestCl = ids.size(), bestOff = ids.size() - bestCl - 1;
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Finding best threshold", ids.size(), LOG) : null;
      DBIDArrayIter it = order.iter();
      for(; it.valid(); it.advance()) {
        curGood += mergeClusterSizes(clustersizes, it, pi.assignVar(it, succ));
        // Desired number of clusters, or at least new best:
        if(curGood == numCl || Math.abs(curGood - numCl) < Math.abs(bestCl - numCl)) {
          bestCl = curGood;
          bestOff = it.getOffset() + 1;
        }
        LOG.incrementProcessed(progress);
      }
      if(progress != null) {
        progress.setProcessed(ids.size(), LOG);
      }
      LOG.ensureCompleted(progress);
      if(bestCl != numCl) {
        LOG.warning("Could not find a result with exactly " + numCl + " clusters (+ noise), generating " + bestCl + " clusters instead.");
      }

      // Now perform the necessary merges:
      WritableDataStore<ArrayModifiableDBIDs> clusters = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP, ArrayModifiableDBIDs.class);
      progress = LOG.isVerbose() ? new FiniteProgress("Performing cluster merges", bestOff, LOG) : null;
      for(it.seek(0); it.getOffset() < bestOff; it.advance()) {
        mergeClusters(clusters, it, pi.assignVar(it, succ));
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      ArrayModifiableDBIDs noise = DBIDUtil.newArray();
      Cluster<Model> nclus = new Cluster<>("Noise", noise, true);
      ArrayList<Cluster<Model>> toplevel = new ArrayList<>(bestCl + 1);
      toplevel.add(nclus); // We eventually remove this later on.

      // Third pass: wrap clusters into output format, collect noise.
      for(it.seek(0); it.valid(); it.advance()) {
        ArrayModifiableDBIDs c = clusters.get(it);
        if(c == null) {
          if(it.getOffset() >= bestOff) {
            noise.add(it);
          }
        }
        else if(c.size() < minClSize) {
          noise.addDBIDs(c);
        }
        else {
          toplevel.add(new Cluster<>(c));
        }
      }

      // No noise cluster?
      if(noise.isEmpty()) {
        toplevel.remove(0);
      }
      Clustering<Model> dendrogram = new Clustering<>("Hierarchical Clustering", "hierarchical-clustering", toplevel);
      return dendrogram;
    }

    /**
     * Merge two clusters, size only.
     * 
     * @param clustersizes Cluster sizes
     * @param it Source object (disappears)
     * @param succ Target object
     * @return Change in the number of good clusters (-1,0,+1)
     */
    private int mergeClusterSizes(WritableIntegerDataStore clustersizes, DBIDRef it, DBIDRef succ) {
      int c1 = clustersizes.intValue(it), c2 = clustersizes.intValue(succ);
      int c12 = c1 + c2;
      clustersizes.put(succ, c12);
      return (c12 >= minClSize ? 1 : 0) - (c1 >= minClSize ? 1 : 0) - (c2 >= minClSize ? 1 : 0);
    }

    /**
     * Merge two clusters
     * 
     * @param clusters Temporary clusters
     * @param it Source object (disappears)
     * @param succ Target object
     */
    private void mergeClusters(WritableDataStore<ArrayModifiableDBIDs> clusters, DBIDRef it, DBIDRef succ) {
      ArrayModifiableDBIDs c1 = clusters.get(it), c2 = clusters.get(succ);
      if(c1 == null) {
        if(c2 == null) {
          clusters.put(succ, c2 = DBIDUtil.newArray());
          c2.add(succ);
        }
        c2.add(it);
      }
      else {
        if(c2 == null) {
          c1.add(succ);
          clusters.put(succ, c2 = c1);
        }
        else {
          c2.addDBIDs(c1);
        }
        clusters.put(it, null);
      }
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<HierarchicalClusteringAlgorithm> algorithmP = new ObjectParameter<>(AbstractAlgorithm.ALGORITHM_ID, HierarchicalClusteringAlgorithm.class);
      if(config.grab(algorithmP)) {
        algorithm = algorithmP.instantiateClass(config);
      }

      IntParameter numClP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(numClP)) {
        numCl = numClP.intValue();
      }

      IntParameter minclustersP = new IntParameter(MINCLUSTERSIZE_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minclustersP)) {
        minClSize = minclustersP.intValue();
      }
    }

    @Override
    protected ClustersWithNoiseExtraction makeInstance() {
      return new ClustersWithNoiseExtraction(algorithm, numCl, minClSize);
    }
  }
}
