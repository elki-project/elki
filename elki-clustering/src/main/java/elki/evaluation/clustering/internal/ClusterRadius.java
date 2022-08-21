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
import elki.data.NumberVector;
import elki.data.model.ModelUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a clustering by the (weighted) cluster radius.
 * This is based on a MiniMax kind of cluster model.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
public class ClusterRadius implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(ClusterRadius.class);

  /**
   * Handling of Noise clusters
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private NumberVectorDistance<?> distance;

  /**
   * Key for logging statistics.
   */
  private String key = ClusterRadius.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function to use.
   * @param noiseOption Control noise handling.
   */
  public ClusterRadius(NumberVectorDistance<?> distance, NoiseHandling noiseOption) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOption;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return ssq
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    double weighted = 0, unweighted = 0;
    int cnum = 0;
    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          continue;
        case TREAT_NOISE_AS_SINGLETONS:
          break;
        case MERGE_NOISE:
          break; // Treat as cluster below:
        }
      }
      NumberVector center = ModelUtil.getPrototypeOrCentroid(cluster.getModel(), rel, cluster.getIDs());
      double max = 0;
      for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
        final double d = distance.distance(center, rel.get(it1));
        max = Math.max(max, d);
      }
      cnum += 1;
      weighted += max * cluster.size();
      unweighted += max;
    }
    weighted /= rel.size();
    unweighted /= cnum;
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(key + ".weighted", weighted));
      LOG.statistics(new DoubleStatistic(key + ".unweighted", unweighted));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Weighted radius sum", weighted, 0., Double.POSITIVE_INFINITY, true);
    g.addMeasure("Radius sum", unweighted, 0., Double.POSITIVE_INFINITY, true);
    Metadata.hierarchyOf(c).addChild(ev);
    return weighted;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<NumberVector> rel = db.getRelation(distance.getInputTypeRestriction());
    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("ssq.distance", "Distance function to use for computing the SSQ.");

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID NOISE_ID = new OptionID("ssq.noisehandling", "Control how noise should be treated.");

    /**
     * Distance function to use.
     */
    private NumberVectorDistance<?> distance;

    /**
     * Handling of noise clusters.
     */
    private NoiseHandling noiseOption;

    @Override
    public void configure(Parameterization config) {
      ObjectParameter<NumberVectorDistance<?>> distP = new ObjectParameter<>(DISTANCE_ID, NumberVectorDistance.class, SquaredEuclideanDistance.class);
      if(config.grab(distP)) {
        distance = distP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    public ClusterRadius make() {
      return new ClusterRadius(distance, noiseOption);
    }
  }
}
