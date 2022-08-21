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
package elki.evaluation.clustering.internal;

import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.datastructures.heap.DoubleHeap;
import elki.utilities.datastructures.heap.DoubleMaxHeap;
import elki.utilities.datastructures.heap.DoubleMinHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class CIndex<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(CIndex.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private Distance<? super O> distance;

  /**
   * Key for logging statistics.
   */
  private String key = CIndex.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   */
  public CIndex(Distance<? super O> distance, NoiseHandling noiseOpt) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   * 
   * @param rel Data relation
   * @param c Clustering
   * @return C-Index
   */
  public double evaluateClustering(Relation<? extends O> rel, DistanceQuery<O> dq, Clustering<?> c) {
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

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("C-Index", cIndex, 0., 1., 0., true);
    if(!Metadata.hierarchyOf(c).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
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
          // Careful: we don't want duplicate distances, but we already do the
          // same trick on the clusters; so on different clusters we need to
          // look at all pairs, within a cluster only half.
          if(i == j && DBIDUtil.compare(it1, it2) <= 0) {
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
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<O> relation = db.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    for(Clustering<?> c : crs) {
      evaluateClustering(relation, dq, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
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
    private Distance<? super O> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseOption = x);
    }

    @Override
    public CIndex<O> make() {
      return new CIndex<>(distance, noiseOption);
    }
  }

}
