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
package elki.evaluation.clustering.internal;

import java.util.Iterator;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ModelUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.math.MeanVariance;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the simplified silhouette of a data set.
 * <p>
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
public class SimplifiedSilhouette implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(SimplifiedSilhouette.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private NumberVectorDistance<?> distance;

  /**
   * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  private boolean penalize = true;

  /**
   * Key for logging statistics.
   */
  private String key = SimplifiedSilhouette.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   * @param penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  public SimplifiedSilhouette(NumberVectorDistance<?> distance, NoiseHandling noiseOpt, boolean penalize) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOpt;
    this.penalize = penalize;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param rel Data relation
   * @param c Clustering
   * @return Mean simplified silhouette
   */
  public double evaluateClustering(Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    NumberVector[] centroids = new NumberVector[clusters.size()];
    int ignorednoise = centroids(rel, clusters, centroids, noiseOption);

    MeanVariance mssil = new MeanVariance();
    WritableDoubleDataStore silhouettes = DataStoreFactory.FACTORY.makeDoubleStorage(rel.getDBIDs(), DataStoreFactory.HINT_DB);

    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      if(cluster.size() <= 1) {
        // As suggested in Rousseeuw, we use 0 for singletons.
        mssil.put(0., cluster.size());
        for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
          silhouettes.putDouble(it, 0);
        }
        continue;
      }
      if(cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          continue; // Ignore elements
        case TREAT_NOISE_AS_SINGLETONS:
          // As suggested in Rousseeuw, we use 0 for singletons.
          mssil.put(0., cluster.size());
          for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
            silhouettes.putDouble(it, 0);
          }
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

        double s = (min - a) / (min > a ? min : a);
        mssil.put(s);
        silhouettes.putDouble(it, s);
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

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("Simp. Silhouette +-" + FormatUtil.NF2.format(stdssil), meanssil, -1., 1., 0., false);
    if(!Metadata.hierarchyOf(c).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    Metadata.hierarchyOf(c).addChild(new MaterializedDoubleRelation(Silhouette.SILHOUETTE_NAME, rel.getDBIDs(), silhouettes));
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
          centroids[i] = null;
          continue;
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
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Relation<? extends NumberVector> rel = ResultUtil.findDatabase(result).getRelation(this.distance.getInputTypeRestriction());
    for(Clustering<?> c : crs) {
      evaluateClustering(rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Distance function to use.
     */
    private NumberVectorDistance<?> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption;

    /**
     * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
     */
    private boolean penalize = true;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<NumberVectorDistance<?>>(Silhouette.Par.DISTANCE_ID, NumberVectorDistance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<NoiseHandling>(Silhouette.Par.NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseOption = x);
      if(noiseOption == NoiseHandling.IGNORE_NOISE) {
        new Flag(Silhouette.Par.NO_PENALIZE_ID).grab(config, x -> penalize = !x);
      }
    }

    @Override
    public SimplifiedSilhouette make() {
      return new SimplifiedSilhouette(distance, noiseOption, penalize);
    }
  }
}
