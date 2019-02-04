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

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ModelUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the simplified silhouette of a data set.
 *
 * The simplified silhouette does not use pairwise distances, but distances to
 * centroids only.
 *
 * @author Stephan Baier
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
public class EvaluateSimplifiedSilhouette implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSimplifiedSilhouette.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private NumberVectorDistanceFunction<?> distance;

  /**
   * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  private boolean penalize = true;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateSimplifiedSilhouette.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   * @param penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  public EvaluateSimplifiedSilhouette(NumberVectorDistanceFunction<?> distance, NoiseHandling noiseOpt, boolean penalize) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOpt;
    this.penalize = penalize;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return Mean simplified silhouette
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    NumberVector[] centroids = new NumberVector[clusters.size()];
    int ignorednoise = centroids(rel, clusters, centroids, noiseOption);

    MeanVariance mssil = new MeanVariance();

    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      if(cluster.size() <= 1) {
        // As suggested in Rousseeuw, we use 0 for singletons.
        mssil.put(0., cluster.size());
        continue;
      }
      if(cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          continue; // Ignore elements
        case TREAT_NOISE_AS_SINGLETONS:
          // As suggested in Rousseeuw, we use 0 for singletons.
          mssil.put(0., cluster.size());
          continue;
        case MERGE_NOISE:
          break; // Treat as cluster below
        }
      }

      // Cluster center:
      final NumberVector center = centroids[i];
      assert (center != null);
      for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
        NumberVector obj = rel.get(it);
        // a: Distance to own centroid
        double a = distance.distance(center, obj);

        // b: Distance to other clusters centroids:
        double min = Double.POSITIVE_INFINITY;
        Iterator<? extends Cluster<?>> cj = clusters.iterator();
        for(int j = 0; cj.hasNext(); j++) {
          Cluster<?> ocluster = cj.next();
          if(i == j) {
            continue;
          }
          NumberVector other = centroids[j];
          if(other == null) { // Noise!
            switch(noiseOption){
            case IGNORE_NOISE:
              continue;
            case TREAT_NOISE_AS_SINGLETONS:
              // Treat each object like a centroid!
              for(DBIDIter it2 = ocluster.getIDs().iter(); it2.valid(); it2.advance()) {
                double dist = distance.distance(rel.get(it2), obj);
                min = dist < min ? dist : min;
              }
              continue;
            case MERGE_NOISE:
              break; // Treat as cluster below, but should not be reachable.
            }
          }
          // Clusters: use centroid.
          double dist = distance.distance(other, obj);
          min = dist < min ? dist : min;
        }

        // One 'real' cluster only?
        min = min < Double.POSITIVE_INFINITY ? min : a;
        mssil.put((min - a) / (min > a ? min : a));
      }
    }

    double penalty = 1.;
    // Only if {@link NoiseHandling#IGNORE_NOISE}:
    if(penalize && ignorednoise > 0) {
      penalty = (rel.size() - ignorednoise) / (double) rel.size();
    }
    final double meanssil = penalty * mssil.getMean();
    final double stdssil = penalty * mssil.getSampleStddev();
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".simplified-silhouette.noise-handling", noiseOption.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".simplified-silhouette.ignored", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".simplified-silhouette.mean", meanssil));
      LOG.statistics(new DoubleStatistic(key + ".simplified-silhouette.stddev", stdssil));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Simp. Silhouette +-" + FormatUtil.NF2.format(stdssil), meanssil, -1., 1., 0., false);
    db.getHierarchy().resultChanged(ev);
    return meanssil;
  }

  /**
   * Compute centroids.
   *
   * @param rel Data relation
   * @param clusters Clusters
   * @param centroids Output array for centroids
   * @return Number of ignored noise elements.
   */
  public static int centroids(Relation<? extends NumberVector> rel, List<? extends Cluster<?>> clusters, NumberVector[] centroids, NoiseHandling noiseOption) {
    assert (centroids.length == clusters.size());
    int ignorednoise = 0;
    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          ignorednoise += cluster.size();
        case TREAT_NOISE_AS_SINGLETONS:
          centroids[i] = null;
          continue;
        case MERGE_NOISE:
          break; // Treat as cluster below
        }
      }
      centroids[i] = ModelUtil.getPrototypeOrCentroid(cluster.getModel(), rel, cluster.getIDs());
    }
    return ignorednoise;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(hier);
    Relation<? extends NumberVector> rel = db.getRelation(this.distance.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Distance function to use.
     */
    private NumberVectorDistanceFunction<?> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption;

    /**
     * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
     */
    private boolean penalize = true;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<NumberVectorDistanceFunction<?>> distanceFunctionP = new ObjectParameter<>(EvaluateSilhouette.Parameterizer.DISTANCE_ID, NumberVectorDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(EvaluateSilhouette.Parameterizer.NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }

      if(noiseOption == NoiseHandling.IGNORE_NOISE) {
        Flag penalizeP = new Flag(EvaluateSilhouette.Parameterizer.NO_PENALIZE_ID);
        if(config.grab(penalizeP)) {
          penalize = penalizeP.isFalse();
        }
      }
    }

    @Override
    protected EvaluateSimplifiedSilhouette makeInstance() {
      return new EvaluateSimplifiedSilhouette(distance, noiseOption, penalize);
    }
  }
}
