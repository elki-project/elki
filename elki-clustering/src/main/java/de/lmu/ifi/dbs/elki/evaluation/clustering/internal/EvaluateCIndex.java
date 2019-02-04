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
package de.lmu.ifi.dbs.elki.evaluation.clustering.internal;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMinHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the C-index of a data set.
 * <p>
 * Note: This requires pairwise distance computations, so it is not recommended
 * to use this on larger data sets.
 * <p>
 * Reference:
 * <p>
 * L. J. Hubert, J. R. Levin<br>
 * A general statistical framework for assessing categorical clustering in free
 * recall<br>
 * Psychological Bulletin, Vol. 83(6)
 *
 * @author Stephan Baier
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
@Reference(authors = "L. J. Hubert, J. R. Levin", //
    title = "A general statistical framework for assessing categorical clustering in free recall", //
    booktitle = "Psychological Bulletin, Vol. 83(6)", //
    url = "https://doi.org/10.1037/0033-2909.83.6.1072", //
    bibkey = "doi:10.1037/0033-2909.83.6.1072")
public class EvaluateCIndex<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateCIndex.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private DistanceFunction<? super O> distance;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateCIndex.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   */
  public EvaluateCIndex(DistanceFunction<? super O> distance, NoiseHandling noiseOpt) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return C-Index
   */
  public double evaluateClustering(Database db, Relation<? extends O> rel, DistanceQuery<O> dq, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();

    // Count ignored noise, and within-cluster distances
    int ignorednoise = 0, w = 0;
    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          ignorednoise += cluster.size();
          continue; // Ignore
        case TREAT_NOISE_AS_SINGLETONS:
          continue; // No within-cluster distances!
        case MERGE_NOISE:
          break; // Treat like a cluster
        default:
          LOG.warning("Unknown noise handling option: " + noiseOption);
        }
      }
      w += (cluster.size() * (cluster.size() - 1)) >>> 1;
    }

    // TODO: for small k=2, and balanced clusters, it may be more efficient to
    // just build a long array with all distances, and select the quantiles.
    // The heaps used below pay off in memory consumption for k > 2

    // Yes, maxDists is supposed to be a min heap, and the other way.
    // Because we want to replace the smallest of the current k-largest
    // distances.
    DoubleHeap maxDists = new DoubleMinHeap(w);
    DoubleHeap minDists = new DoubleMaxHeap(w);
    double theta = 0.; // Sum of within-cluster distances

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Processing clusters for C-Index", clusters.size(), LOG) : null;
    for(int i = 0; i < clusters.size(); i++) {
      Cluster<?> cluster = clusters.get(i);
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          LOG.incrementProcessed(prog);
          continue; // Ignore
        case TREAT_NOISE_AS_SINGLETONS:
          processSingleton(cluster, rel, dq, maxDists, minDists, w);
          LOG.incrementProcessed(prog);
          continue;
        case MERGE_NOISE:
          break; // Treat like a cluster, below
        }
      }
      theta += processCluster(cluster, clusters, i, dq, maxDists, minDists, w);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    // Simulate best and worst cases:
    double min = 0, max = 0; // Sum of largest and smallest
    assert (minDists.size() == w);
    assert (maxDists.size() == w);
    for(DoubleHeap.UnsortedIter it = minDists.unsortedIter(); it.valid(); it.advance()) {
      min += it.get();
    }
    for(DoubleHeap.UnsortedIter it = maxDists.unsortedIter(); it.valid(); it.advance()) {
      max += it.get();
    }
    assert (max >= min);

    double cIndex = (max > min) ? (theta - min) / (max - min) : 1.;

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".c-index.noise-handling", noiseOption.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".c-index.ignored", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".c-index", cIndex));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("C-Index", cIndex, 0., 1., 0., true);
    db.getHierarchy().resultChanged(ev);
    return cIndex;
  }

  protected double processCluster(Cluster<?> cluster, List<? extends Cluster<?>> clusters, int i, DistanceQuery<O> dq, DoubleHeap maxDists, DoubleHeap minDists, int w) {
    double theta = 0.;
    for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
      // Compare object to every cluster, but only once
      for(int j = i; j < clusters.size(); j++) {
        Cluster<?> ocluster = clusters.get(j);
        if(ocluster.size() <= 1 || ocluster.isNoise()) {
          switch(noiseOption){
          case IGNORE_NOISE:
            continue; // Ignore this cluster.
          case TREAT_NOISE_AS_SINGLETONS:
            break; // Treat like a cluster
          case MERGE_NOISE:
            break; // Treat like a cluster
          }
        }
        for(DBIDIter it2 = ocluster.getIDs().iter(); it2.valid(); it2.advance()) {
          if(DBIDUtil.compare(it1, it2) <= 0) { // Only once.
            continue;
          }
          double dist = dq.distance(it1, it2);
          minDists.add(dist, w);
          maxDists.add(dist, w);
          if(ocluster == cluster) { // Within-cluster distances.
            theta += dist;
          }
        }
      }
    }
    return theta;
  }

  protected void processSingleton(Cluster<?> cluster, Relation<? extends O> rel, DistanceQuery<O> dq, DoubleHeap maxDists, DoubleHeap minDists, int w) {
    // All other objects are in other clusters!
    for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
      for(DBIDIter it2 = rel.iterDBIDs(); it2.valid(); it2.advance()) {
        if(DBIDUtil.compare(it1, it2) <= 0) { // Only once.
          continue;
        }
        double dist = dq.distance(it1, it2);
        minDists.add(dist, w);
        maxDists.add(dist, w);
      }
    }
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(hier);
    Relation<O> rel = db.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> dq = db.getDistanceQuery(rel, distance);

    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, dq, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("c-index.distance", "Distance function to use for computing the c-index.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("c-index.noisehandling", "Control how noise should be treated.");

    /**
     * Distance function to use.
     */
    private DistanceFunction<? super O> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    protected EvaluateCIndex<O> makeInstance() {
      return new EvaluateCIndex<>(distance, noiseOption);
    }
  }

}
