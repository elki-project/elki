package de.lmu.ifi.dbs.elki.evaluation.clustering.internal;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ModelUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the simplified silhouette of a data set.
 * 
 * @author Stephan Baier
 * @author Erich Schubert
 */
public class EvaluateSimplifiedSilhouette implements Evaluator {

  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSimplifiedSilhouette.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

  /**
   * Distance function to use.
   */
  private PrimitiveDistanceFunction<? super NumberVector> distance;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateSimplifiedSilhouette.class.getName();

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateSimplifiedSilhouette(PrimitiveDistanceFunction<? super NumberVector> distance, NoiseHandling noiseOpt) {
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
   * @return Mean simplified silhouette
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();

    // Collect cluster centroids
    int noisecount = 0;
    ArrayList<NumberVector> centroids = new ArrayList<NumberVector>();

    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
        case IGNORE_NOISE_WITH_PENALTY:
        case TREAT_NOISE_AS_SINGLETONS:
          noisecount += cluster.size();
          centroids.add(null);
          continue;
        case MERGE_NOISE:
          // Continue.
          break;
        }
      }
      NumberVector p = ModelUtil.getPrototype(cluster.getModel(), rel);
      if(p == null) {
        p = Centroid.make(rel, cluster.getIDs());
      }
      centroids.add(p);
    }
    assert (centroids.size() == clusters.size());

    MeanVariance mssil = new MeanVariance();

    int i = -1;
    for(Cluster<?> cluster : clusters) {
      i++; // Count all clusters.
      if(cluster.size() <= 1) {
        // As suggested in Rousseeuw, we use 0 for singletons.
        mssil.put(0., cluster.size());
        continue;
      }
      if(cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
        case IGNORE_NOISE_WITH_PENALTY:
          continue;
        case TREAT_NOISE_AS_SINGLETONS:
          // As suggested in Rousseeuw, we use 0 for singletons.
          mssil.put(0., cluster.size());
          continue;
        case MERGE_NOISE:
          // Continue as with clusters.
          break;
        }
      }

      // Cluster center:
      final NumberVector center = centroids.get(i);
      assert (center != null);
      for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
        NumberVector obj = rel.get(it);
        // a: Distance to own centroid
        double a = distance.distance(center, obj);

        // b: Distance to other clusters centroids:
        double min = Double.POSITIVE_INFINITY;
        for(NumberVector other : centroids) {
          if(other != null && other != center) {
            double dist = distance.distance(other, obj);
            min = dist < min ? dist : min;
          }
        }

        // One cluster only?
        min = min < Double.POSITIVE_INFINITY ? min : a;
        mssil.put((min - a) / (min > a ? min : a));
      }
    }

    double penalty = 1.;
    if(noiseOption == NoiseHandling.IGNORE_NOISE_WITH_PENALTY && noisecount > 0) {
      penalty = (rel.size() - noisecount) / (double) rel.size();
    }
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".simplified-silhouette.noise-handling", noiseOption.toString()));
      LOG.statistics(new LongStatistic(key + ".simplified-silhouette.noise", noisecount));
      LOG.statistics(new DoubleStatistic(key + ".simplified-silhouette.mean", penalty * mssil.getMean()));
      LOG.statistics(new DoubleStatistic(key + ".simplified-silhouette.stddev", penalty * mssil.getSampleStddev()));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Simplified Silhouette +-" + FormatUtil.NF2.format(penalty * mssil.getSampleStddev()), penalty * mssil.getMean(), -1., 1., 0., false);
    db.getHierarchy().resultChanged(ev);
    return penalty * mssil.getMean();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<? extends NumberVector> rel = db.getRelation(this.distance.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, c);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<? super NumberVector> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<PrimitiveDistanceFunction<? super NumberVector>> distanceFunctionP = new ObjectParameter<>(EvaluateSilhouette.Parameterizer.DISTANCE_ID, PrimitiveDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(EvaluateSilhouette.Parameterizer.NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    protected EvaluateSimplifiedSilhouette makeInstance() {
      return new EvaluateSimplifiedSilhouette(distance, noiseOption);
    }
  }
}
